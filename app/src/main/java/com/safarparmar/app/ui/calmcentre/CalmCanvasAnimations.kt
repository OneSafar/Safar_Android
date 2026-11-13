package com.safarparmar.app.ui.calmcentre

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Organic breathing character blob extracted from Headspace launch-breathe.
 * Features organic squash/stretch, synchronized eye scaling, breathing O-mouth, and ambient glow rings.
 */
@Composable
fun CalmBreathingBlob(
    progress: Float,
    modifier: Modifier = Modifier,
    baseRadius: Dp = 80.dp,
    characterColor: Color = Color(0xFFF47D31),
    glowColor: Color = Color(0x33F47D31),
    eyeColor: Color = Color(0xFF262626),
) {
    Canvas(modifier = modifier.size(baseRadius * 3.4f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radiusPx = baseRadius.toPx()
        val currentRadius = radiusPx * (1f + 0.32f * progress)

        // 1. Ambient glow rings
        drawCircle(
            color = glowColor.copy(alpha = 0.16f * (1f - progress * 0.35f)),
            radius = currentRadius * 1.35f,
            center = center,
        )
        drawCircle(
            color = glowColor.copy(alpha = 0.08f * (1f - progress * 0.5f)),
            radius = currentRadius * 1.6f,
            center = center,
        )

        // 2. Character body with organic squash and stretch
        val squashX = 1f + 0.04f * sin(progress * PI.toFloat())
        val squashY = 1f - 0.04f * sin(progress * PI.toFloat())

        scale(scaleX = squashX, scaleY = squashY, pivot = center) {
            drawCircle(
                color = characterColor,
                radius = currentRadius,
                center = center,
            )

            // 3. Eyes with blinking/relaxing scale
            val eyeOffsetX = currentRadius * 0.28f
            val eyeOffsetY = -currentRadius * 0.08f
            val eyeRadius = currentRadius * 0.075f
            val eyeScaleY = 0.35f + 0.65f * progress

            // Left Eye
            scale(scaleX = 1f, scaleY = eyeScaleY, pivot = Offset(center.x - eyeOffsetX, center.y + eyeOffsetY)) {
                drawCircle(
                    color = eyeColor,
                    radius = eyeRadius,
                    center = Offset(center.x - eyeOffsetX, center.y + eyeOffsetY),
                )
            }

            // Right Eye
            scale(scaleX = 1f, scaleY = eyeScaleY, pivot = Offset(center.x + eyeOffsetX, center.y + eyeOffsetY)) {
                drawCircle(
                    color = eyeColor,
                    radius = eyeRadius,
                    center = Offset(center.x + eyeOffsetX, center.y + eyeOffsetY),
                )
            }

            // 4. Morphing mouth (Gentle smile -> Circular breathing mouth)
            val mouthY = center.y + currentRadius * 0.24f
            if (progress < 0.25f) {
                val mouthWidth = currentRadius * 0.24f
                val mouthPath = Path().apply {
                    moveTo(center.x - mouthWidth / 2f, mouthY)
                    quadraticTo(center.x, mouthY + currentRadius * 0.08f, center.x + mouthWidth / 2f, mouthY)
                }
                drawPath(
                    path = mouthPath,
                    color = eyeColor,
                    style = Stroke(width = currentRadius * 0.045f, cap = StrokeCap.Round),
                )
            } else {
                val mouthWidth = currentRadius * 0.16f * progress
                val mouthHeight = currentRadius * 0.22f * progress
                drawOval(
                    color = eyeColor,
                    topLeft = Offset(center.x - mouthWidth / 2f, mouthY - mouthHeight / 2f),
                    size = Size(mouthWidth, mouthHeight),
                )
            }
        }
    }
}

/**
 * Drifting parallax cloud canvas layer extracted from Headspace HSSkyAnimation.
 */
@Composable
fun CalmParallaxClouds(
    modifier: Modifier = Modifier,
    cloudColor: Color = Color.White.copy(alpha = 0.28f),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CalmCloudDrift")
    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "CloudDriftProgress",
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Cloud Layer 1 (Upper foreground)
        val cloud1X = ((cloudOffset * width * 1.3f) % (width + 300f)) - 150f
        val cloud1Y = height * 0.18f
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(cloud1X, cloud1Y),
            size = Size(130.dp.toPx(), 44.dp.toPx()),
            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
        )

        // Cloud Layer 2 (Lower midground with parallax factor 0.65)
        val cloud2X = (((cloudOffset * 0.65f + 0.45f) * width) % (width + 400f)) - 200f
        val cloud2Y = height * 0.38f
        drawRoundRect(
            color = cloudColor.copy(alpha = cloudColor.alpha * 0.75f),
            topLeft = Offset(cloud2X, cloud2Y),
            size = Size(170.dp.toPx(), 52.dp.toPx()),
            cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx()),
        )
    }
}

/**
 * Audio player ambient pulse disc and particle orbit.
 * Extracted from Headspace audio-player-dark-mode-bg-meditate.
 */
@Composable
fun CalmVisualizerDisc(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF6C47FF),
    glowColor: Color = Color(0xFF9E7BFF),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerDisc")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseProgress",
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "OrbitProgress",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = 70.dp.toPx()
        val pulseExpansion = if (isPlaying) pulse * 14.dp.toPx() else 0f
        val orbRadius = baseRadius + pulseExpansion

        // Outer glow layers
        drawCircle(
            color = glowColor.copy(alpha = 0.12f),
            radius = orbRadius * 1.55f,
            center = center,
        )
        drawCircle(
            color = glowColor.copy(alpha = 0.22f),
            radius = orbRadius * 1.25f,
            center = center,
        )
        drawCircle(
            color = primaryColor,
            radius = orbRadius,
            center = center,
        )

        // Orbital particle ring
        if (isPlaying) {
            val particleCount = 8
            val orbitRadius = orbRadius * 1.45f
            for (i in 0 until particleCount) {
                val angle = rotation + (i * 2 * PI / particleCount).toFloat()
                val particleX = center.x + orbitRadius * cos(angle)
                val particleY = center.y + orbitRadius * sin(angle)
                val particleSize = 3.5.dp.toPx() * (0.8f + 0.4f * sin(angle * 2))

                drawCircle(
                    color = Color.White.copy(alpha = 0.65f),
                    radius = particleSize,
                    center = Offset(particleX, particleY),
                )
            }
        }
    }
}

/**
 * Interactive Headspace Completion Checkmark.
 * Extracted from completion_checkmark.riv with spring-scale pop and trim-path drawing.
 */
@Composable
fun CalmCompletionCheckmark(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    checkmarkSize: Dp = 56.dp,
    activeColor: Color = Color(0xFF6C47FF),
    inactiveColor: Color = Color(0xFFE2E2E2),
) {
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "CheckmarkPopScale",
    )

    val checkProgress by animateFloatAsState(
        targetValue = if (isChecked) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "CheckmarkPathTrim",
    )

    val circleColor by animateColorAsState(
        targetValue = if (isChecked) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 250),
        label = "CheckmarkCircleColor",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(checkmarkSize)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                onCheckedChange(!isChecked)
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(checkmarkSize)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) * 0.9f

            scale(scale, pivot = center) {
                drawCircle(
                    color = circleColor,
                    radius = radius,
                    center = center,
                )

                if (checkProgress > 0f) {
                    val checkmarkPath = Path().apply {
                        val startX = center.x - radius * 0.42f
                        val startY = center.y - radius * 0.02f
                        val midX = center.x - radius * 0.10f
                        val midY = center.y + radius * 0.30f
                        val endX = center.x + radius * 0.45f
                        val endY = center.y - radius * 0.32f

                        moveTo(startX, startY)
                        lineTo(midX, midY)
                        lineTo(endX, endY)
                    }

                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(checkmarkPath, false)
                    val trimmedPath = Path()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = pathMeasure.length * checkProgress,
                        destination = trimmedPath,
                        startWithMoveTo = true,
                    )

                    drawPath(
                        path = trimmedPath,
                        color = Color.White,
                        style = Stroke(
                            width = radius * 0.18f,
                            cap = StrokeCap.Round,
                        ),
                    )
                }
            }
        }
    }
}
