package com.safarparmar.app.ui.leaderboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium-Grade Vector Badge: 1st Place Imperial Sovereign Crown
 * Features 5 jeweled arches, pearl finials, ruby-embossed headband,
 * ambient breathing golden halo, floating starlight sparkles, and specular shimmer.
 */
@Composable
fun ImperialGoldCrownBadge(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "imperialCrownAnim")

    val floatBob by if (animated) {
        transition.animateFloat(
            initialValue = -2.2f,
            targetValue = 2.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "crownBob",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val glowPulse by if (animated) {
        transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "crownGlow",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.8f) }
    }

    val sparkleRotation by if (animated) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "sparkleRotate",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val shimmerX by if (animated) {
        transition.animateFloat(
            initialValue = -30f,
            targetValue = 70f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = LinearEasing, delayMillis = 400),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shimmerSweep",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .graphicsLayer { translationY = floatBob.dp.toPx() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Radial Golden Ambient Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFEF08A).copy(alpha = 0.45f * glowPulse),
                        Color(0xFFF59E0B).copy(alpha = 0.20f * glowPulse),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = w * 0.75f,
                ),
            )

            // 2. Floating Diamond Sparkles (+ shape starbursts)
            drawSparkleStar(Offset(w * 0.12f, h * 0.20f), 2.5.dp.toPx(), sparkleRotation, Color(0xFFFEF9C3).copy(alpha = glowPulse))
            drawSparkleStar(Offset(w * 0.88f, h * 0.22f), 2.8.dp.toPx(), -sparkleRotation * 1.2f, Color(0xFFFEF9C3).copy(alpha = 1f - (glowPulse * 0.3f)))
            drawSparkleStar(Offset(w * 0.50f, h * 0.05f), 3.2.dp.toPx(), sparkleRotation * 0.8f, Color(0xFFFFFFFF))

            // 3. Velvet Crimson Cap Arc (Back of Crown)
            val capPath = Path().apply {
                moveTo(w * 0.22f, h * 0.74f)
                cubicTo(w * 0.22f, h * 0.42f, w * 0.78f, h * 0.42f, w * 0.78f, h * 0.74f)
                close()
            }
            drawPath(
                path = capPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF991B1B), Color(0xFF450A0A)),
                    startY = h * 0.4f,
                    endY = h * 0.74f,
                ),
            )

            // 4. Crown Gold Body & Arches (5 peaks with sculpted curves)
            val crownBodyPath = Path().apply {
                moveTo(w * 0.14f, h * 0.74f)
                lineTo(w * 0.86f, h * 0.74f)
                // Right outer peak
                cubicTo(w * 0.88f, h * 0.60f, w * 0.90f, h * 0.45f, w * 0.86f, h * 0.36f)
                // Right inner valley
                lineTo(w * 0.72f, h * 0.56f)
                // Right inner peak
                lineTo(w * 0.66f, h * 0.26f)
                // Center right valley
                lineTo(w * 0.56f, h * 0.50f)
                // Main Sovereign Center Peak
                lineTo(w * 0.50f, h * 0.16f)
                // Center left valley
                lineTo(w * 0.44f, h * 0.50f)
                // Left inner peak
                lineTo(w * 0.34f, h * 0.26f)
                // Left inner valley
                lineTo(w * 0.28f, h * 0.56f)
                // Left outer peak
                cubicTo(w * 0.10f, h * 0.45f, w * 0.12f, h * 0.60f, w * 0.14f, h * 0.74f)
                close()
            }

            // Rich multi-stop metallic gold gradient
            drawPath(
                path = crownBodyPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFBEB), // Highlight
                        Color(0xFFFDE047), // Pale Gold
                        Color(0xFFF59E0B), // Vibrant Gold
                        Color(0xFFD97706), // Deep Gold
                        Color(0xFF92400E), // Shadow Gold
                    ),
                    start = Offset(shimmerX.dp.toPx(), 0f),
                    end = Offset((shimmerX + 30f).dp.toPx(), h),
                ),
            )

            // Crisp Gold Outline
            drawPath(
                path = crownBodyPath,
                color = Color(0xFF78350F),
                style = Stroke(width = 1.3.dp.toPx(), join = StrokeJoin.Round),
            )

            // 5. Embossed Headband Base (Gold Plate + Ruby & Diamond Bezels)
            val bandRect = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = androidx.compose.ui.geometry.Rect(w * 0.12f, h * 0.72f, w * 0.88f, h * 0.88f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    ),
                )
            }
            drawPath(
                path = bandRect,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFEF08A), Color(0xFFD97706), Color(0xFF78350F)),
                    startY = h * 0.72f,
                    endY = h * 0.88f,
                ),
            )
            drawPath(
                path = bandRect,
                color = Color(0xFF78350F),
                style = Stroke(width = 1.dp.toPx()),
            )

            // Headband Jewels (Center Oval Ruby + Flanking Diamond Quadrants)
            // Center Ruby
            drawOval(
                brush = Brush.radialGradient(listOf(Color(0xFFF87171), Color(0xFFDC2626), Color(0xFF7F1D1D))),
                topLeft = Offset(w * 0.44f, h * 0.76f),
                size = Size(w * 0.12f, h * 0.09f),
            )
            // Flanking Emerald/Diamond Gems
            drawCircle(Color(0xFFFFFFFF), 1.3.dp.toPx(), Offset(w * 0.26f, h * 0.80f))
            drawCircle(Color(0xFF78350F), 1.3.dp.toPx(), Offset(w * 0.26f, h * 0.80f), style = Stroke(0.5.dp.toPx()))
            drawCircle(Color(0xFFFFFFFF), 1.3.dp.toPx(), Offset(w * 0.74f, h * 0.80f))
            drawCircle(Color(0xFF78350F), 1.3.dp.toPx(), Offset(w * 0.74f, h * 0.80f), style = Stroke(0.5.dp.toPx()))

            // 6. Lustrous Spherical Pearls on the 5 Crest Tips
            drawLustrousPearl(Offset(w * 0.50f, h * 0.16f), 2.4.dp.toPx())
            drawLustrousPearl(Offset(w * 0.34f, h * 0.26f), 2.0.dp.toPx())
            drawLustrousPearl(Offset(w * 0.66f, h * 0.26f), 2.0.dp.toPx())
            drawLustrousPearl(Offset(w * 0.12f, h * 0.38f), 1.8.dp.toPx())
            drawLustrousPearl(Offset(w * 0.88f, h * 0.38f), 1.8.dp.toPx())
        }
    }
}

/**
 * Premium-Grade Vector Badge: 2nd Place Heraldic Silver Laurel Shield
 * Features dual-tone beveled knight shield, polished crossed olive laurel wreath,
 * 8-pointed star of distinction, and platinum specular light sweep.
 */
@Composable
fun HeraldicSilverLaurelShieldBadge(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "silverShieldAnim")

    val shimmerX by if (animated) {
        transition.animateFloat(
            initialValue = -35f,
            targetValue = 65f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing, delayMillis = 200),
                repeatMode = RepeatMode.Restart,
            ),
            label = "shieldShimmer",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val glowPulse by if (animated) {
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "shieldGlow",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.7f) }
    }

    Box(
        modifier = modifier.size(34.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Cool Platinum Radial Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0).copy(alpha = 0.40f * glowPulse),
                        Color(0xFF94A3B8).copy(alpha = 0.15f * glowPulse),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = w * 0.70f,
                ),
            )

            // 2. Sculpted Dual Laurel Wreath (Left & Right Branches)
            drawLaurelBranch(center = Offset(w * 0.50f, h * 0.52f), radiusX = w * 0.44f, radiusY = h * 0.42f, isLeft = true)
            drawLaurelBranch(center = Offset(w * 0.50f, h * 0.52f), radiusX = w * 0.44f, radiusY = h * 0.42f, isLeft = false)

            // 3. Knight Heater Shield Geometry
            val shieldLeft = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.24f, h * 0.22f)
                lineTo(w * 0.24f, h * 0.58f)
                cubicTo(w * 0.24f, h * 0.76f, w * 0.45f, h * 0.88f, w * 0.50f, h * 0.92f)
                close()
            }
            val shieldRight = Path().apply {
                moveTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.76f, h * 0.22f)
                lineTo(w * 0.76f, h * 0.58f)
                cubicTo(w * 0.76f, h * 0.76f, w * 0.55f, h * 0.88f, w * 0.50f, h * 0.92f)
                close()
            }

            // Left Facet (Direct Light: High Specular Silver)
            drawPath(
                path = shieldLeft,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF94A3B8)),
                    start = Offset(shimmerX.dp.toPx(), 0f),
                    end = Offset((shimmerX + 25f).dp.toPx(), h),
                ),
            )
            // Right Facet (Beveled Shadow: Polished Steel Silver)
            drawPath(
                path = shieldRight,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF475569)),
                    start = Offset(shimmerX.dp.toPx(), 0f),
                    end = Offset((shimmerX + 25f).dp.toPx(), h),
                ),
            )

            // Shield Bevel Border Stroke
            val fullShield = Path().apply {
                moveTo(w * 0.24f, h * 0.22f)
                lineTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.76f, h * 0.22f)
                lineTo(w * 0.76f, h * 0.58f)
                cubicTo(w * 0.76f, h * 0.76f, w * 0.50f, h * 0.92f, w * 0.50f, h * 0.92f)
                cubicTo(w * 0.50f, h * 0.92f, w * 0.24f, h * 0.76f, w * 0.24f, h * 0.58f)
                close()
            }
            drawPath(
                path = fullShield,
                color = Color(0xFF334155),
                style = Stroke(width = 1.3.dp.toPx(), join = StrokeJoin.Round),
            )
            // Vertical Spine Highlight
            drawLine(
                color = Color(0xFFFFFFFF),
                start = Offset(w * 0.50f, h * 0.18f),
                end = Offset(w * 0.50f, h * 0.88f),
                strokeWidth = 1.1.dp.toPx(),
            )

            // 4. Center 8-Point Faceted Star of Distinction
            drawEightPointStar(center = Offset(w * 0.50f, h * 0.48f), outerRadius = w * 0.17f, innerRadius = w * 0.07f)
        }
    }
}

/**
 * Premium-Grade Vector Badge: 3rd Place Radiant Bronze Flame Medallion
 * Features 12-point radiant sunburst medallion base, multi-layered sculpted 3D eternal flame,
 * molten amber core, and breathing thermal pulse.
 */
@Composable
fun RadiantBronzeFlameMedallionBadge(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "bronzeFlameAnim")

    val thermalPulse by if (animated) {
        transition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "thermalScale",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    val heatGlow by if (animated) {
        transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "heatGlow",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.75f) }
    }

    Box(
        modifier = modifier
            .size(34.dp)
            .graphicsLayer {
                scaleX = thermalPulse
                scaleY = thermalPulse
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Radiant Amber-Copper Heat Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFDBA74).copy(alpha = 0.45f * heatGlow),
                        Color(0xFFEA580C).copy(alpha = 0.20f * heatGlow),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = w * 0.72f,
                ),
            )

            // 2. 12-Point Sunburst Medallion Base
            val starburstPath = Path().apply {
                val points = 12
                val outerR = w * 0.44f
                val innerR = w * 0.36f
                for (i in 0 until points * 2) {
                    val angle = (i * PI / points).toFloat()
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = w * 0.50f + r * cos(angle)
                    val y = h * 0.54f + r * sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(
                path = starburstPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDBA74), Color(0xFFD97706), Color(0xFF78350F), Color(0xFF451A03)),
                    center = Offset(w * 0.50f, h * 0.54f),
                    radius = w * 0.45f,
                ),
            )
            drawPath(
                path = starburstPath,
                color = Color(0xFF451A03),
                style = Stroke(width = 1.1.dp.toPx()),
            )

            // 3. Layer 1: Outer Antique Bronze Flame Petals
            val outerFlamePath = Path().apply {
                moveTo(w * 0.50f, h * 0.10f)
                cubicTo(w * 0.68f, h * 0.26f, w * 0.82f, h * 0.46f, w * 0.76f, h * 0.68f)
                cubicTo(w * 0.70f, h * 0.86f, w * 0.30f, h * 0.86f, w * 0.24f, h * 0.68f)
                cubicTo(w * 0.18f, h * 0.46f, w * 0.32f, h * 0.26f, w * 0.50f, h * 0.10f)
                close()
            }
            drawPath(
                path = outerFlamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFEF3C7), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF78350F)),
                    startY = h * 0.10f,
                    endY = h * 0.86f,
                ),
            )
            drawPath(
                path = outerFlamePath,
                color = Color(0xFF451A03),
                style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round),
            )

            // 4. Layer 2: Molten Core Incandescent Flame
            val innerFlamePath = Path().apply {
                moveTo(w * 0.50f, h * 0.32f)
                cubicTo(w * 0.62f, h * 0.44f, w * 0.68f, h * 0.58f, w * 0.62f, h * 0.72f)
                cubicTo(w * 0.56f, h * 0.80f, w * 0.44f, h * 0.80f, w * 0.38f, h * 0.72f)
                cubicTo(w * 0.32f, h * 0.58f, w * 0.38f, h * 0.44f, w * 0.50f, h * 0.32f)
                close()
            }
            drawPath(
                path = innerFlamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFFEF08A), Color(0xFFF97316)),
                    startY = h * 0.32f,
                    endY = h * 0.80f,
                ),
            )
        }
    }
}

// ── Private Drawing Helpers ──────────────────────────────────────────────────

private fun DrawScope.drawSparkleStar(center: Offset, radius: Float, rotationDegrees: Float, color: Color) {
    rotate(rotationDegrees, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticTo(center.x, center.y, center.x + radius, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + radius)
            quadraticTo(center.x, center.y, center.x - radius, center.y)
            quadraticTo(center.x, center.y, center.x - radius, center.y)
            close()
        }
        drawPath(path, color)
    }
}

private fun DrawScope.drawLustrousPearl(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFFBEB), Color(0xFFE2E8F0), Color(0xFF94A3B8)),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 1.2f,
        ),
        radius = radius,
        center = center,
    )
    drawCircle(
        color = Color(0xFF78350F),
        radius = radius,
        center = center,
        style = Stroke(width = 0.6.dp.toPx()),
    )
}

private fun DrawScope.drawEightPointStar(center: Offset, outerRadius: Float, innerRadius: Float) {
    val starPath = Path()
    val numPoints = 8
    for (i in 0 until numPoints * 2) {
        val angle = (i * PI / numPoints - PI / 2).toFloat()
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()

    drawPath(
        path = starPath,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFF94A3B8)),
            center = center,
            radius = outerRadius,
        ),
    )
    drawPath(
        path = starPath,
        color = Color(0xFF334155),
        style = Stroke(width = 0.9.dp.toPx()),
    )
}

private fun DrawScope.drawLaurelBranch(center: Offset, radiusX: Float, radiusY: Float, isLeft: Boolean) {
    val sign = if (isLeft) -1f else 1f
    val stemPath = Path().apply {
        moveTo(center.x, center.y + radiusY * 0.85f)
        cubicTo(
            center.x + sign * radiusX * 0.6f, center.y + radiusY * 0.7f,
            center.x + sign * radiusX * 1.0f, center.y + radiusY * 0.1f,
            center.x + sign * radiusX * 0.75f, center.y - radiusY * 0.45f,
        )
    }
    drawPath(stemPath, color = Color(0xFF64748B), style = Stroke(width = 1.4.dp.toPx()))

    // 5 Pairs of sculpted laurel leaves
    val leafAngles = listOf(0.70f, 0.45f, 0.20f, -0.05f, -0.30f)
    for (progress in leafAngles) {
        val leafCenter = Offset(
            center.x + sign * radiusX * (0.85f - (progress * 0.2f)),
            center.y + radiusY * progress,
        )
        drawOval(
            brush = Brush.linearGradient(listOf(Color(0xFFF8FAFC), Color(0xFF94A3B8))),
            topLeft = Offset(leafCenter.x - 3.dp.toPx(), leafCenter.y - 5.dp.toPx()),
            size = Size(6.dp.toPx(), 10.dp.toPx()),
        )
        drawOval(
            color = Color(0xFF475569),
            topLeft = Offset(leafCenter.x - 3.dp.toPx(), leafCenter.y - 5.dp.toPx()),
            size = Size(6.dp.toPx(), 10.dp.toPx()),
            style = Stroke(0.6.dp.toPx()),
        )
    }
}
