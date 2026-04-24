package com.safar.app.ui.butterfly

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Enhanced butterfly drawn using Temple H. Fay's Butterfly Curve. Improvements over v1:
 * - Dual-layer wings: soft base fill + vivid gradient shimmer on top
 * - Wing venation lines radiating from body center
 * - Iridescent edge highlight that pulses with the shimmer phase
 * - Glitter aura: 6 tiny sparkle dots orbiting the body
 * - Richer body: segmented abdomen + thorax + larger head
 * - Thicker, curvier antennae with heart-shaped tips
 */
@Composable
fun ButterflyDrawing(
        wingColor: Color = Color(0xFFF082DC),
        bodyColor: Color = Color(0xFF500F50),
        modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bf")

    // Wing flap — full sin cycle so wings flip through centre
    val flapPhase by
            infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * Math.PI).toFloat(),
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(durationMillis = 460, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                            ),
                    label = "flap",
            )

    // Shimmer phase — slower, offset from flap for organic feel
    val shimmerPhase by
            infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * Math.PI).toFloat(),
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                            ),
                    label = "shimmer",
            )

    // Aura orbit phase
    val auraPhase by
            infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2 * Math.PI).toFloat(),
                    animationSpec =
                            infiniteRepeatable(
                                    animation = tween(durationMillis = 900, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart,
                            ),
                    label = "aura",
            )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = size.width * 0.068f

        val flapScaleX = sin(flapPhase)
        val shimmer = (sin(shimmerPhase) * 0.5f + 0.5f) // 0..1

        // ── Layer 1: soft base wings ───────────────────────────────
        withTransform({
            translate(cx, cy)
            scale(scaleX = flapScaleX, scaleY = 1f, pivot = Offset.Zero)
        }) { drawFayWings(scale = scale, wingColor = wingColor, alpha = 0.82f, strokeWidth = 1.2f) }

        // ── Layer 2: shimmer / iridescent overlay ──────────────────
        val shimmerColor =
                Color(
                        red = 1f,
                        green = 0.55f + shimmer * 0.25f,
                        blue = 0.90f + shimmer * 0.10f,
                        alpha = 0.28f + shimmer * 0.22f,
                )
        withTransform({
            translate(cx, cy)
            scale(scaleX = flapScaleX, scaleY = 1f, pivot = Offset.Zero)
        }) {
            drawFayWings(
                    scale = scale * 0.96f,
                    wingColor = shimmerColor,
                    alpha = 1f,
                    strokeWidth = 0f
            )
        }

        // ── Layer 3: bright edge highlight on leading edges ────────
        val edgeAlpha = 0.18f + shimmer * 0.32f
        withTransform({
            translate(cx, cy)
            scale(scaleX = flapScaleX, scaleY = 1f, pivot = Offset.Zero)
        }) { drawFayEdgeHighlight(scale = scale, alpha = edgeAlpha) }

        // ── Layer 4: wing venation ─────────────────────────────────
        withTransform({
            translate(cx, cy)
            scale(scaleX = flapScaleX, scaleY = 1f, pivot = Offset.Zero)
        }) { drawVenation(scale = scale, shimmer = shimmer) }

        // ── Glitter aura orbiting body ─────────────────────────────
        drawGlitterAura(cx = cx, cy = cy, scale = scale, auraPhase = auraPhase, shimmer = shimmer)

        // ── Body on top ───────────────────────────────────────────
        drawFayBody(cx = cx, cy = cy, bodyColor = bodyColor, scale = scale, shimmer = shimmer)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Wing fill
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawFayWings(
        scale: Float,
        wingColor: Color,
        alpha: Float,
        strokeWidth: Float,
) {
    val path = buildFayPath(scale)

    drawPath(path, color = wingColor.copy(alpha = alpha), style = Fill)

    if (strokeWidth > 0f) {
        drawPath(
                path,
                color = wingColor.copy(alpha = alpha * 0.65f),
                style = Stroke(width = strokeWidth),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bright edge / iridescent border
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawFayEdgeHighlight(scale: Float, alpha: Float) {
    val path = buildFayPath(scale)
    drawPath(
            path,
            color = Color(1f, 0.95f, 1f, alpha),
            style = Stroke(width = 2.8f, cap = StrokeCap.Round),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Wing venation — radiating lines from body centre
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawVenation(scale: Float, shimmer: Float) {
    val veins =
            listOf(
                    // angle in radians, length multiplier
                    Pair(-0.6f, 2.2f),
                    Pair(-1.1f, 2.6f),
                    Pair(-1.7f, 2.4f),
                    Pair(0.4f, 1.8f),
                    Pair(0.9f, 2.3f),
                    Pair(1.5f, 2.0f),
                    Pair(2.1f, 1.6f),
            )
    val veinColor = Color(0xFFE060C8).copy(alpha = 0.18f + shimmer * 0.14f)
    val baseY = scale * 0.2f // start slightly below centre (thorax)

    for ((angle, lengthMul) in veins) {
        val len = scale * lengthMul
        val endX = cos(angle) * len
        val endY = baseY + sin(angle) * len
        // simple straight vein from body centre
        drawLine(
                color = veinColor,
                start = Offset(0f, baseY),
                end = Offset(endX, endY),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round,
        )
        // secondary branching vein at 60% along
        val branchX = cos(angle + 0.4f) * len * 0.55f
        val branchY = baseY + sin(angle + 0.4f) * len * 0.55f
        drawLine(
                color = veinColor.copy(alpha = veinColor.alpha * 0.65f),
                start = Offset(cos(angle) * len * 0.45f, baseY + sin(angle) * len * 0.45f),
                end = Offset(branchX, branchY),
                strokeWidth = 0.7f,
                cap = StrokeCap.Round,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Glitter aura — small sparkles orbiting the body
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawGlitterAura(
        cx: Float,
        cy: Float,
        scale: Float,
        auraPhase: Float,
        shimmer: Float,
) {
    val count = 7
    val radius = scale * 1.05f
    for (i in 0 until count) {
        val angle = auraPhase + (i.toFloat() / count) * (2 * Math.PI).toFloat()
        val ax = cx + cos(angle) * radius
        val ay = cy + sin(angle) * radius * 0.7f // slightly oval orbit

        // Twinkle: each dot pulses out of phase
        val twinkle = (sin(auraPhase * 2f + i * 1.1f) * 0.5f + 0.5f)
        val dotSize = (1.4f + twinkle * 2.2f)
        val dotAlpha = 0.45f + twinkle * 0.55f

        // Alternate pink / gold / white for variety
        val dotColor =
                when (i % 3) {
                    0 -> Color(1f, 0.59f, 0.98f, dotAlpha) // pink
                    1 -> Color(1f, 0.88f, 0.40f, dotAlpha) // gold
                    else -> Color(1f, 1f, 1f, dotAlpha * 0.8f) // white
                }

        drawCircle(color = dotColor, radius = dotSize, center = Offset(ax, ay))

        // Star cross on larger dots
        if (twinkle > 0.6f) {
            val arm = dotSize * 1.6f
            val crossAlpha = (twinkle - 0.6f) / 0.4f * dotAlpha
            drawLine(
                    color = dotColor.copy(alpha = crossAlpha),
                    start = Offset(ax - arm, ay),
                    end = Offset(ax + arm, ay),
                    strokeWidth = 0.8f,
            )
            drawLine(
                    color = dotColor.copy(alpha = crossAlpha),
                    start = Offset(ax, ay - arm),
                    end = Offset(ax, ay + arm),
                    strokeWidth = 0.8f,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Body — segmented abdomen + thorax + head + antennae
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawFayBody(
        cx: Float,
        cy: Float,
        bodyColor: Color,
        scale: Float,
        shimmer: Float,
) {
    val aTopX = scale * 1.4f
    val aTopY = scale * 1.9f

    // ── Antennae ───────────────────────────────────────────────────
    for (side in listOf(-1f, 1f)) {
        val antennaPath =
                Path().apply {
                    moveTo(cx, cy - scale * 1.15f)
                    cubicTo(
                            cx + side * scale * 0.5f,
                            cy - scale * 1.9f,
                            cx + side * scale * 1.2f,
                            cy - scale * 2.2f,
                            cx + side * aTopX,
                            cy - aTopY,
                    )
                }
        drawPath(
                antennaPath,
                color = Color(0xFF7A1A7A).copy(alpha = 0.92f),
                style = Stroke(width = 2.0f, cap = StrokeCap.Round),
        )
        // Heart-shaped tip: two tiny overlapping circles
        val tipX = cx + side * aTopX
        val tipY = cy - aTopY
        drawCircle(
                color = Color(0xFFFF60E8).copy(alpha = 0.9f),
                radius = 3.2f,
                center = Offset(tipX - 1.3f, tipY)
        )
        drawCircle(
                color = Color(0xFFFF60E8).copy(alpha = 0.9f),
                radius = 3.2f,
                center = Offset(tipX + 1.3f, tipY)
        )
        drawCircle(color = Color(0xFF9B009B), radius = 1.8f, center = Offset(tipX, tipY + 1.5f))
    }

    // ── Abdomen — segmented (5 oval segments tapering downward) ───
    val abdomenSegments = 5
    for (seg in 0 until abdomenSegments) {
        val frac = seg.toFloat() / abdomenSegments.toFloat()
        val segW = scale * 0.27f * (1f - frac * 0.55f)
        val segH = scale * 0.32f
        val segCy = cy + scale * 0.55f + seg * scale * 0.42f
        val segColor =
                if (seg % 2 == 0) bodyColor else Color(0xFF6A1A6A) // alternating darker stripe
        drawOval(
                color = segColor,
                topLeft = Offset(cx - segW, segCy - segH / 2f),
                size = androidx.compose.ui.geometry.Size(segW * 2f, segH),
        )
        // Shimmer highlight strip on each segment
        drawOval(
                color = Color(1f, 0.7f, 1f, 0.12f + shimmer * 0.08f),
                topLeft = Offset(cx - segW * 0.5f, segCy - segH * 0.35f),
                size = androidx.compose.ui.geometry.Size(segW, segH * 0.4f),
        )
    }

    // ── Thorax ────────────────────────────────────────────────────
    val thoraxW = scale * 0.28f
    val thoraxH = scale * 0.60f
    val thoraxTop = cy - thoraxH * 0.3f
    drawOval(
            color = bodyColor,
            topLeft = Offset(cx - thoraxW, thoraxTop),
            size = androidx.compose.ui.geometry.Size(thoraxW * 2f, thoraxH),
    )
    // Thorax highlight
    drawOval(
            color = Color(1f, 0.75f, 1f, 0.22f + shimmer * 0.15f),
            topLeft = Offset(cx - thoraxW * 0.5f, thoraxTop + thoraxH * 0.1f),
            size = androidx.compose.ui.geometry.Size(thoraxW, thoraxH * 0.4f),
    )

    // ── Head ──────────────────────────────────────────────────────
    val headR = scale * 0.30f
    val headCy = cy - thoraxH * 0.3f - headR * 0.7f
    drawCircle(
            color = bodyColor,
            radius = headR,
            center = Offset(cx, headCy),
    )
    // Eye dots
    for (side in listOf(-1f, 1f)) {
        drawCircle(
                color = Color(0xFFFFB0F8).copy(alpha = 0.9f),
                radius = headR * 0.28f,
                center = Offset(cx + side * headR * 0.42f, headCy - headR * 0.15f),
        )
        drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = headR * 0.10f,
                center = Offset(cx + side * headR * 0.46f, headCy - headR * 0.20f),
        )
    }
    // Head shimmer
    drawCircle(
            color = Color(1f, 0.8f, 1f, 0.18f + shimmer * 0.18f),
            radius = headR * 0.55f,
            center = Offset(cx - headR * 0.18f, headCy - headR * 0.22f),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Fay curve path builder (shared)
// ─────────────────────────────────────────────────────────────────────────────

private fun buildFayPath(scale: Float): Path {
    val path = Path()
    val steps = 480
    val tMax = (Math.PI * 24).toFloat()
    val dt = tMax / steps
    var first = true
    for (i in 0..steps) {
        val t = i * dt
        val r = exp(cos(t)) - 2f * cos(4f * t) - sin(t / 12f).pow(5)
        val x = sin(t) * r * scale
        val y = -cos(t) * r * scale
        if (first) {
            path.moveTo(x, y)
            first = false
        } else path.lineTo(x, y)
    }
    path.close()
    return path
}
