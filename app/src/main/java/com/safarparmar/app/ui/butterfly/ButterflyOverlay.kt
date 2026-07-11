package com.safarparmar.app.ui.butterfly

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.sqrt
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.BlendMode
import com.safarparmar.app.R
import kotlin.math.atan2

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ButterflyOverlay(
    state: ButterflyTourState,
    wingColor: Color = Color(0xFFF082DC),
    bodyColor: Color = Color(0xFF500F50),
    butterflySize: Dp = 52.8.dp,
    dimColor: Color = Color(0x55000000),
) {
    if (!state.isVisible) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val W = constraints.maxWidth.toFloat()
        val H = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // ── Butterfly position ─────────────────────────────────────
        val bfX = remember { mutableFloatStateOf(W * (state.currentStep?.anchorX ?: 0.5f)) }
        val bfY = remember { mutableFloatStateOf(H * (state.currentStep?.anchorY ?: 0.5f)) }

        // ── Glitter system ─────────────────────────────────────────
        val glitter = remember { GlitterSystem() }
        var showTooltip by remember { mutableStateOf(false) }
        var trailMs by remember { mutableLongStateOf(0L) }
        var flightAngleDeg by remember { mutableFloatStateOf(0f) }

        data class TrailPoint(val position: Offset, val createdAtMs: Long)
        val trailHistory = remember { mutableStateListOf<TrailPoint>() }
        var frameMs by remember { mutableLongStateOf(0L) }
        val trailLifetimeMs = 1_000L

        // ── S-curve / arc Bézier state ─────────────────────────────
        data class CurveState(
            val startX: Float, val startY: Float,
            val cp1X: Float,   val cp1Y: Float,
            val cp2X: Float,   val cp2Y: Float,
            val endX: Float,   val endY: Float,
        )

        var curve by remember { mutableStateOf<CurveState?>(null) }
        var tProgress by remember { mutableFloatStateOf(0f) }
        var flightDurationMs by remember { mutableLongStateOf(500L) }

        var settled by remember { mutableStateOf(false) }
        var settledMs by remember { mutableLongStateOf(0L) }
        var tooltipAnchorX by remember { mutableFloatStateOf(0f) }
        var tooltipAnchorY by remember { mutableFloatStateOf(0f) }

        // ── Cubic Bézier evaluator ─────────────────────────────────
        fun cubicBezier(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
            val u = 1f - t
            return u*u*u*p0 + 3f*u*u*t*p1 + 3f*u*t*t*p2 + t*t*t*p3
        }

        // ── FAR distance threshold ─────────────────────────────────
        val farThreshold = W * 0.50f   // below = gentle arc, above = full S

        // ── Step change: build curve for this step ─────────────────
        LaunchedEffect(state.currentStepIndex, state.isVisible) {
            if (!state.isVisible) return@LaunchedEffect
            showTooltip = false
            settled = false
            settledMs = 0L
            tProgress = 0f

            val startX = bfX.floatValue
            val startY = bfY.floatValue
            val endX   = W * (state.currentStep?.anchorX ?: 0.5f)
            val endY   = H * (state.currentStep?.anchorY ?: 0.5f)

            val dx   = endX - startX
            val dy   = endY - startY
            val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)

            // Normalize duration: constant speed of 2.1 pixels/ms (30% slower than 3.0f).
            // Capped between 350ms and 700ms to maintain a fluid, smooth transition.
            flightDurationMs = (dist / 2.1f).toLong().coerceIn(350L, 700L)

            // Perpendicular unit vector (left of travel direction)
            val perpX = -dy / dist
            val perpY =  dx / dist

            val side = if (Math.random() > 0.5) 1f else -1f

            // cp offset scales with distance, capped reasonably
            val cpOffset = (dist * 0.75f).coerceIn(W * 0.08f, W * 0.70f)

            val cp1X: Float
            val cp1Y: Float
            val cp2X: Float
            val cp2Y: Float

            if (dist >= farThreshold) {
                // ── FAR: full S-curve ──────────────────────────────
                // cp1 bulges one side, cp2 bulges the opposite → S shape
                cp1X = startX + dx * 0.33f + perpX * cpOffset * side
                cp1Y = startY + dy * 0.33f + perpY * cpOffset * side
                cp2X = startX + dx * 0.66f - perpX * cpOffset * side
                cp2Y = startY + dy * 0.66f - perpY * cpOffset * side
            } else {
                // ── SHORT: gentle single arc ───────────────────────
                // Both control points on the same side → soft bow, no S
                val arcOffset = (dist * 0.30f).coerceIn(W * 0.04f, W * 0.15f)
                cp1X = startX + dx * 0.33f + perpX * arcOffset * side
                cp1Y = startY + dy * 0.33f + perpY * arcOffset * side
                cp2X = startX + dx * 0.66f + perpX * arcOffset * side
                cp2Y = startY + dy * 0.66f + perpY * arcOffset * side
            }

            curve = CurveState(startX, startY, cp1X, cp1Y, cp2X, cp2Y, endX, endY)
        }

        // ── Master tick loop ───────────────────────────────────────
        LaunchedEffect(state.isVisible) {
            var prevX = bfX.floatValue
            var prevY = bfY.floatValue
            var stepStartMs = -1L
            var lastCurve: CurveState? = null   // detect when curve changes → reset timer

            while (state.isVisible) {
                withFrameMillis { ms ->
                    frameMs = ms
                    val c = curve ?: return@withFrameMillis

                    while (
                        trailHistory.isNotEmpty() &&
                        ms - trailHistory.first().createdAtMs >= trailLifetimeMs
                    ) {
                        trailHistory.removeAt(0)
                    }

                    // New step arrived → reset timer so t always starts from 0
                    if (c !== lastCurve) {
                        lastCurve = c
                        stepStartMs = ms
                    }
                    if (stepStartMs < 0L) stepStartMs = ms

                    // Smoothstep ease-in-out: 3t²-2t³
                    val rawT = ((ms - stepStartMs) / flightDurationMs.toFloat()).coerceIn(0f, 1f)
                    tProgress = rawT * rawT * (3f - 2f * rawT)

                    // Evaluate cubic Bézier position
                    val nx = cubicBezier(tProgress, c.startX, c.cp1X, c.cp2X, c.endX)
                    val ny = cubicBezier(tProgress, c.startY, c.cp1Y, c.cp2Y, c.endY)

                    bfX.floatValue = nx
                    bfY.floatValue = ny

                    // ── Rotation ──
                    val dx = nx - prevX
                    val dy = ny - prevY
                    val speed = sqrt(dx * dx + dy * dy)
                    if (speed > 0.3f) {
                        flightAngleDeg =
                            Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    }

                    // ── Short-lived trail & glitter ──
                    glitter.tick()
                    if (ms - trailMs > 22L) {
                        if (speed > 0.3f) {
                            trailHistory.add(TrailPoint(Offset(nx, ny), ms))
                            glitter.spawn(nx, ny, 5)
                            glitter.spawnTail(prevX, prevY, dx, dy, 4)
                        } else {
                            glitter.spawn(nx, ny, 2)
                        }
                        prevX = nx
                        prevY = ny
                        trailMs = ms
                    }

                    // ── Settle detection ──
                    val distToEnd = sqrt(
                        (nx - c.endX) * (nx - c.endX) +
                                (ny - c.endY) * (ny - c.endY)
                    )
                    val closeToDest = tProgress > 0.92f || distToEnd < W * 0.05f
                    if (closeToDest && !settled) {
                        if (settledMs == 0L) settledMs = ms
                        if (ms - settledMs > 500L) {
                            settled = true
                            showTooltip = true
                            tooltipAnchorX = bfX.floatValue
                            tooltipAnchorY = bfY.floatValue
                        }
                    } else if (!closeToDest) {
                        settledMs = 0L
                    }
                }
            }
        }

        // ── Trail ribbon + sparkle canvas ─────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.99f }
                .pointerInput(state.currentStep?.isInteractive) {
                    val interactive = state.currentStep?.isInteractive == true
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val cx = bfX.floatValue
                            val cy = bfY.floatValue
                            val radius = state.currentStep?.cutoutRadius?.toPx() ?: 0f
                            event.changes.forEach { change ->
                                if (change.isConsumed) return@forEach

                                if (interactive) {
                                    val dx = change.position.x - cx
                                    val dy = change.position.y - cy
                                    if (sqrt(dx * dx + dy * dy) > radius) {
                                        change.consume()
                                    }
                                } else {
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            // Draw dim background
            drawRect(dimColor)
            if (state.currentStep?.isInteractive == true) {
                drawCircle(
                    color = Color.Transparent,
                    radius = state.currentStep?.cutoutRadius?.toPx() ?: 0f,
                    center = Offset(bfX.floatValue, bfY.floatValue),
                    blendMode = BlendMode.Clear
                )
            }

            // 0. Ambient radial glow around butterfly
            val glowBrush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF64C8).copy(alpha = 0.15f),
                    Color(0xFF9632FF).copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                center = Offset(bfX.floatValue, bfY.floatValue),
                radius = 180f,
            )
            drawCircle(
                brush = glowBrush,
                radius = 180f,
                center = Offset(bfX.floatValue, bfY.floatValue),
            )

            // 1. Smooth rounded ribbon. Sampled flight points are joined through
            // quadratic control points rather than straight segments, and the entire
            // ribbon fades away within one second after the butterfly settles.
            if (trailHistory.size > 1) {
                val path = Path()
                val first = trailHistory.first().position
                path.moveTo(first.x, first.y)
                for (i in 1 until trailHistory.lastIndex) {
                    val control = trailHistory[i].position
                    val next = trailHistory[i + 1].position
                    val midpoint = Offset((control.x + next.x) / 2f, (control.y + next.y) / 2f)
                    path.quadraticTo(control.x, control.y, midpoint.x, midpoint.y)
                }
                val last = trailHistory.last().position
                path.lineTo(last.x, last.y)

                val newestAge = frameMs - trailHistory.last().createdAtMs
                val fade = (1f - newestAge / trailLifetimeMs.toFloat()).coerceIn(0f, 1f)
                drawPath(
                    path = path,
                    color = Color(0xFFF082DC).copy(alpha = 0.20f * fade),
                    style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
                drawPath(
                    path = path,
                    color = Color(0xFFFFB0F8).copy(alpha = 0.58f * fade),
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            // 2. Sparkles
            for (p in glitter.particles) {
                val alpha = p.life.coerceIn(0f, 1f)
                val currentSize = p.size * alpha.coerceAtLeast(0f)
                if (currentSize <= 0f) continue

                val baseColor = when {
                    p.isGold -> Color(1f, 0.88f, 0.30f, alpha)
                    p.isTail -> Color(1f, 0.70f, 0.98f, alpha)
                    else     -> Color(1f, 0.59f, 0.98f, alpha)
                }

                drawCircle(color = baseColor, radius = currentSize, center = Offset(p.x, p.y))

                if (p.isStar && currentSize > 1.2f) {
                    val arm = currentSize * 2.0f
                    val starAlpha = alpha * 0.75f
                    drawLine(
                        color = baseColor.copy(alpha = starAlpha),
                        start = Offset(p.x - arm, p.y),
                        end = Offset(p.x + arm, p.y),
                        strokeWidth = 0.9f,
                    )
                    drawLine(
                        color = baseColor.copy(alpha = starAlpha),
                        start = Offset(p.x, p.y - arm),
                        end = Offset(p.x, p.y + arm),
                        strokeWidth = 0.9f,
                    )
                    if (p.isTail) {
                        val diag = arm * 0.65f
                        drawLine(
                            color = baseColor.copy(alpha = starAlpha * 0.6f),
                            start = Offset(p.x - diag, p.y - diag),
                            end = Offset(p.x + diag, p.y + diag),
                            strokeWidth = 0.7f,
                        )
                        drawLine(
                            color = baseColor.copy(alpha = starAlpha * 0.6f),
                            start = Offset(p.x + diag, p.y - diag),
                            end = Offset(p.x - diag, p.y + diag),
                            strokeWidth = 0.7f,
                        )
                    }
                }
            }
        }

        // ── Tooltip card ───────────────────────────────────────────
        AnimatedVisibility(
            visible = showTooltip,
            enter = fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.88f),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.88f),
        ) {
            val step = state.currentStep
            if (step != null) {
                val cardW = 220.dp
                val cardH = 160.dp
                val cardWPx = with(density) { cardW.toPx() }
                val cardHPx = with(density) { cardH.toPx() }
                val margin = with(density) { 12.dp.toPx() }
                val navBarH = with(density) { 80.dp.toPx() }

                var tx = tooltipAnchorX + 64f
                var ty = tooltipAnchorY - 60f

                if (tx + cardWPx > W - margin) tx = tooltipAnchorX - cardWPx - 18f
                if (tx < margin) tx = margin
                if (ty < margin) ty = tooltipAnchorY + 64f
                if (ty + cardHPx > H - navBarH - margin) ty = H - navBarH - cardHPx - margin
                tx = tx.coerceIn(margin, maxOf(margin, W - cardWPx - margin))
                ty = ty.coerceIn(margin, maxOf(margin, H - navBarH - cardHPx - margin))

                TooltipCard(
                    step = step,
                    stepIndex = state.currentStepIndex,
                    totalSteps = state.steps.size,
                    isFirst = state.isFirstStep,
                    isLast = state.isLastStep,
                    offsetX = tx.roundToInt(),
                    offsetY = ty.roundToInt(),
                    onPrevious = { state.previous() },
                    onNext = { state.next() },
                    onSkip = { state.dismiss() },
                )
            }
        }

        // ── Butterfly ──────────────────────────────────────────────
        val bfPx = with(density) { butterflySize.toPx() }
        val bfOffX = (bfX.floatValue - bfPx / 2f).roundToInt()
        val bfOffY = (bfY.floatValue - bfPx / 2f).roundToInt()
        Image(
            painter = painterResource(id = R.drawable.ic_butterfly_tour),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(butterflySize)
                .offset { IntOffset(bfOffX, bfOffY) }
                .graphicsLayer { rotationZ = flightAngleDeg },
        )
    }
}

@Composable
private fun TooltipCard(
    step: ButterflyTourStep,
    stepIndex: Int,
    totalSteps: Int,
    isFirst: Boolean,
    isLast: Boolean,
    offsetX: Int,
    offsetY: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    Surface(
        modifier =
            Modifier.offset { IntOffset(offsetX, offsetY) }
                .widthIn(min = 180.dp, max = 240.dp),
        shape = RoundedCornerShape(16.dp),
        color = ButterflyCardBg,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Step indicator dots
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isLast) "✦ done!" else "stop ${stepIndex + 1} / $totalSteps",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ButterflyGoldDark,
                    letterSpacing = 0.08.sp,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                step.iconRes?.let { res ->
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(id = res),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ButterflyTextDark
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = step.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ButterflyTextDark,
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = step.message,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = ButterflyTextDark,
                lineHeight = 17.sp,
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.heightIn(min = 30.dp),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 8.dp,
                            vertical = 0.dp
                        ),
                ) {
                    Text(
                        "Skip",
                        fontSize = 11.sp,
                        color = ButterflyGold,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!step.isInteractive) {
                    TextButton(
                        onClick = onPrevious,
                        enabled = !isFirst,
                        modifier = Modifier.heightIn(min = 30.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 0.dp,
                        ),
                    ) {
                        Text(
                            "←",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = onNext,
                        modifier = Modifier.width(64.dp).height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = ButterflyGold,
                                contentColor = Color.White,
                            ),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 0.dp,
                                vertical = 0.dp,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = if (isLast) "Finish tour" else "Next step",
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }
            }
        }
    }
}
