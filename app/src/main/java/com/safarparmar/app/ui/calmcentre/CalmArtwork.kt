package com.safarparmar.app.ui.calmcentre

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.safarparmar.app.performance.rememberDecorationPhase
import kotlin.math.PI
import kotlin.math.sin

/** Local practice colors extend SAFAR's ivory/plum language; navigation keeps its brand accent. */
internal data class CalmColors(
    val background: Color, val surface: Color, val ink: Color, val muted: Color,
    val plum: Color, val onPlum: Color, val line: Color,
    val peach: Color, val rose: Color, val sage: Color, val night: Color,
) {
    fun stage(practice: CalmPractice) = when (practice) {
        CalmPractice.BREATHE -> peach
        CalmPractice.KINDNESS -> rose
        CalmPractice.GROUND -> sage
        CalmPractice.REST -> night
    }

    companion object {
        fun forTheme(dark: Boolean) = if (dark) CalmColors(
            Color(0xFF19151D), Color(0xFF271F2B), Color(0xFFF7EAF1), Color(0xFFC7B4C3),
            Color(0xFFE9B7DA), Color(0xFF391C35), Color(0xFF493A4B),
            Color(0xFF493026), Color(0xFF422B3C), Color(0xFF253D37), Color(0xFF252D4C),
        ) else CalmColors(
            Color(0xFFFFF8F3), Color(0xFFFFFCFA), Color(0xFF36192F), Color(0xFF75616F),
            Color(0xFF542249), Color(0xFFFFF8F3), Color(0xFFE8DBE3),
            Color(0xFFFBE4D5), Color(0xFFF3E1E9), Color(0xFFE3EDE3), Color(0xFFE3E6F3),
        )
    }
}

// Illustration pigments are independent of text tokens and work on either practice surface.
private object Pigment {
    val apricot = Color(0xFFF4944F)
    val terracotta = Color(0xFFD85C30)
    val petalLight = Color(0xFFFFB575)
    val seed = Color(0xFFFFBC77)
    val face = Color(0xFF633C3C)
    val pink = Color(0xFFD58CA6)
    val blush = Color(0xFFEDB8C8)
    val sage = Color(0xFF7F9E83)
    val moss = Color(0xFF577D69)
    val stone = Color(0xFFB7C9A9)
    val moon = Color(0xFFF6DBB5)
    val hill = Color(0xFF7F8DAE)
    val cloud = Color(0xFFC4CBE1)
}

/**
 * Original vector choreography using the existing materials' visual vocabulary: petals,
 * expressive curves, soft pebbles and drifting sky. Frame state is read only while drawing.
 * All shapes use a 1000 x 760 coordinate space so the composition scales without clipping.
 */
@Composable
internal fun CalmArtwork(
    practice: CalmPractice,
    modifier: Modifier = Modifier,
    elapsedMillis: (() -> Long)? = null,
    durationMillis: Long? = null,
    technique: CalmBreathTechnique = CalmBreathTechnique.GENTLE,
    motionEnabled: Boolean = true,
) {
    // The common multiple closes every 20s preview breath / 8s ripple / 24s orbit.
    // Active sessions use their own functional clock, with no second decorative ticker.
    val ambient = if (elapsedMillis == null && motionEnabled) rememberDecorationPhase(120_000) else ({ 0f })
    val petal = remember {
        Path().apply {
            moveTo(0f, 22f)
            cubicTo(-88f, -12f, -135f, -133f, -58f, -207f)
            cubicTo(-6f, -259f, 114f, -199f, 104f, -112f)
            cubicTo(98f, -43f, 38f, 6f, 0f, 22f)
            close()
        }
    }
    val pebble = remember {
        Path().apply {
            moveTo(-139f, 65f)
            cubicTo(-184f, 2f, -110f, -133f, -19f, -143f)
            cubicTo(73f, -164f, 158f, -41f, 146f, 37f)
            cubicTo(132f, 123f, -85f, 137f, -139f, 65f)
            close()
        }
    }
    Canvas(modifier) {
        val fittingScale = minOf(size.width / 1000f, size.height / 760f)
        val time = if (!motionEnabled) 0L else elapsedMillis?.invoke() ?: (ambient() * 120_000).toLong()
        val phase = time / 12_000f * (2f * PI.toFloat())
        translate((size.width - 1000f * fittingScale) / 2f, (size.height - 760f * fittingScale) / 2f) {
            scale(fittingScale, fittingScale, pivot = Offset.Zero) {
                when (practice) {
                    CalmPractice.BREATHE -> {
                        // The small preview breathes at half speed; functional guidance keeps its 4/6 rhythm.
                        val breathTime = if (elapsedMillis == null) time / 2L else time
                        val expansion = if (motionEnabled) calmBreathFrame(breathTime, technique).expansion else 0.55f
                        val open = 0.72f + expansion * 0.24f
                        drawOval(Pigment.terracotta.copy(alpha = 0.07f), Offset(265f, 603f), Size(470f, 43f))
                        drawCircle(Pigment.terracotta.copy(alpha = 0.12f), 288f, Offset(500f, 365f), style = Stroke(2f))
                        drawArc(Pigment.terracotta.copy(alpha = 0.22f), -58f, 112f, false,
                            Offset(186f, 51f), Size(628f, 628f), style = Stroke(2f, cap = StrokeCap.Round))
                        translate(500f, 365f) {
                            for (i in 0..4) {
                                rotate(i * 72f - 16f + expansion * 5f, Offset.Zero) {
                                    translate(0f, -18f - expansion * 18f) {
                                        scale(open, open, Offset.Zero) {
                                            drawPath(petal, lerp(Pigment.apricot, Pigment.terracotta, i / 8f))
                                            drawPath(petal, Pigment.petalLight.copy(alpha = 0.25f), style = Stroke(2.5f))
                                        }
                                    }
                                }
                            }
                            drawCircle(Pigment.seed, 87f + expansion * 6f, center = Offset.Zero)
                            peacefulFace(Offset(0f, 1f), 1f)
                        }
                        // Two quiet leaves anchor the blossom instead of free-floating particles.
                        translate(728f, 570f) {
                            rotate(48f, Offset.Zero) { scale(0.24f, 0.38f, Offset.Zero) { drawPath(petal, Pigment.sage) } }
                            rotate(110f, Offset.Zero) { scale(0.20f, 0.32f, Offset.Zero) { drawPath(petal, Pigment.stone) } }
                        }
                    }
                    CalmPractice.KINDNESS -> {
                        val first = calmCompanionPose(time, opposite = false)
                        val second = calmCompanionPose(time, opposite = true)
                        drawCircle(Pigment.pink.copy(alpha = 0.10f), 249f, Offset(500f, 342f), style = Stroke(2f))
                        // Shadows follow the bodies; front/back order swaps only when separated.
                        for (pose in listOf(first, second)) {
                            val width = 238f * pose.scale
                            drawOval(Pigment.pink.copy(alpha = 0.07f + 0.025f * (pose.depth + 1f)),
                                Offset(pose.x - width / 2f, 576f + pose.depth * 8f), Size(width, 29f * pose.scale))
                        }
                        if (first.depth <= second.depth) {
                            kindnessCompanion(pebble, first, Pigment.pink)
                            kindnessCompanion(pebble, second, Pigment.blush)
                        } else {
                            kindnessCompanion(pebble, second, Pigment.blush)
                            kindnessCompanion(pebble, first, Pigment.pink)
                        }
                        // The small folded petal follows through more slowly than the orbit.
                        translate(500f + sin(phase * 0.5f) * 16f, 190f + sin(phase) * 10f) {
                            rotate(sin(phase * 0.5f) * 12f, Offset.Zero) {
                                val open = 1f + sin(phase) * 0.08f
                                scale(open, open, Offset.Zero) {
                                    rotate(-24f, Offset.Zero) { scale(0.18f, 0.20f, Offset.Zero) { drawPath(petal, Pigment.pink) } }
                                    rotate(48f, Offset.Zero) { scale(0.18f, 0.20f, Offset.Zero) { drawPath(petal, Pigment.blush) } }
                                }
                            }
                        }
                    }
                    CalmPractice.GROUND -> {
                        for (i in 0..2) {
                            val wave = ((time % 8000L) / 8000f + i / 3f) % 1f
                            val width = 380f + wave * 430f
                            drawOval(Pigment.moss.copy(alpha = sin(PI.toFloat() * wave) * 0.20f),
                                Offset(500f - width / 2f, 530f - width * 0.10f), Size(width, width * 0.23f), style = Stroke(2f))
                        }
                        translate(500f, 491f) { scale(1.4f, 0.54f, Offset.Zero) { drawPath(pebble, Pigment.moss) } }
                        translate(480f, 394f + sin(phase) * 3f) {
                            rotate(-8f, Offset.Zero) { scale(1.02f, 0.60f, Offset.Zero) { drawPath(pebble, Pigment.sage) } }
                        }
                        translate(510f, 295f + sin(phase) * 5f) {
                            rotate(8f, Offset.Zero) { scale(0.66f, 0.68f, Offset.Zero) { drawPath(pebble, Pigment.stone) } }
                            peacefulFace(Offset(0f, 0f), 0.6f)
                        }
                        translate(516f, 202f) {
                            rotate(30f + sin(phase) * 3f, Offset.Zero) { scale(0.18f, 0.35f, Offset.Zero) { drawPath(petal, Pigment.moss) } }
                        }
                    }
                    CalmPractice.REST -> {
                        val moonY = if (!motionEnabled) 315f else if (durationMillis != null) {
                            calmRestMoonY(time, durationMillis) + sin(phase) * 5f
                        } else 315f + sin(phase * 0.5f) * 55f
                        val moonX = 520f + sin(phase * 0.5f) * 22f
                        val haloPulse = sin(phase) * 7f
                        drawCircle(Pigment.moon.copy(alpha = 0.07f), 245f + haloPulse, Offset(moonX, moonY))
                        drawCircle(Pigment.moon.copy(alpha = 0.12f), 191f + haloPulse * 0.6f, Offset(moonX, moonY))
                        for (i in 0..2) {
                            val x = 260f + i * 220f + sin(phase * 0.5f + i) * 52f
                            val y = 412f + i * 45f + sin(phase * 0.5f + i * 0.7f) * 9f
                            drawLine(Pigment.cloud.copy(alpha = 0.75f), Offset(x - 78f, y),
                                Offset(x + 78f, y), 26f - i * 5f, StrokeCap.Round)
                        }
                        // Keep the face readable as the moon passes through the cloud layers.
                        translate(moonX, moonY) {
                            rotate(sin(phase * 0.5f) * 6f, Offset.Zero) {
                                drawCircle(Pigment.moon, 119f, Offset.Zero)
                                peacefulFace(Offset(0f, 6f), 1f)
                            }
                        }
                        drawArc(Pigment.hill.copy(alpha = 0.32f), 186f, 168f, false,
                            Offset(170f, 539f), Size(660f, 220f), style = Stroke(3f, cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}

private fun DrawScope.kindnessCompanion(path: Path, pose: CalmCompanionPose, color: Color) {
    translate(pose.x, pose.y) {
        rotate(pose.tilt, Offset.Zero) {
            scale(pose.scale * pose.turn, pose.scale, Offset.Zero) {
                scale(1.05f, 1.2f, Offset.Zero) { drawPath(path, color) }
                peacefulFace(Offset(0f, 0f), 0.8f)
            }
        }
    }
}

private fun DrawScope.peacefulFace(center: Offset, factor: Float) {
    for (direction in listOf(-1f, 1f)) {
        drawArc(Pigment.face, 12f, 156f, false,
            Offset(center.x + direction * 27f * factor - 12f * factor, center.y - 8f * factor),
            Size(24f * factor, 15f * factor), style = Stroke(4f * factor, cap = StrokeCap.Round))
    }
    drawArc(Pigment.face.copy(alpha = 0.8f), 20f, 140f, false,
        Offset(center.x - 11f * factor, center.y + 16f * factor), Size(22f * factor, 12f * factor),
        style = Stroke(3f * factor, cap = StrokeCap.Round))
}
