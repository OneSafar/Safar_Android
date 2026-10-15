package com.safarparmar.app.ui.ekagra

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.safarparmar.app.performance.adaptiveBlur
import com.safarparmar.app.performance.decorativeFloat

/** Shared by the real timer and its practice presentation. */
@Composable
internal fun EkagraTimerBackground(
    theme: VisualTheme,
    isDarkTheme: Boolean,
    isBreakActive: Boolean,
    timerImmersiveActive: Boolean = false,
) {
    Box(Modifier.fillMaxSize()) {
        val colors = theme.gradientColors
        if (colors != null) {
            val targetTopColor = if (isBreakActive) {
                if (isDarkTheme) Color(0xFF1E2024) else Color(0xFFD4D8DF)
            } else colors[0]
            val targetBottomColor = if (isBreakActive) {
                if (isDarkTheme) Color(0xFF0D0E11) else Color(0xFFECEFF3)
            } else colors[1]

            // Cross-fade palette swaps when the user picks a new
            // visual theme instead of snapping to the new colours.
            val topColor by animateColorAsState(
                targetValue = targetTopColor,
                animationSpec = tween(1200),
                label = "bgTopColor",
            )
            val bottomColor by animateColorAsState(
                targetValue = targetBottomColor,
                animationSpec = tween(1200),
                label = "bgBottomColor",
            )

            val infiniteTransition = rememberInfiniteTransition(label = "gradientAnimation")
            val angle by infiniteTransition.decorativeFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 30000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "gradientAngle"
            )

            // Read animation state while drawing, not while composing
            // the whole timer screen on every background frame.
            Box(modifier = Modifier.fillMaxSize().drawBehind {
                val radians = (angle * Math.PI / 180f).toFloat()
                val centerX = size.width * (0.5f + 0.25f * kotlin.math.cos(radians))
                val centerY = size.height * (0.5f + 0.25f * kotlin.math.sin(radians))
                drawRect(androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(topColor, bottomColor),
                    radius = maxOf(size.width, size.height).coerceAtLeast(1f) * 1.5f,
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                ))
            })
        } else {
            // Unreachable while the video themes are hidden — every
            // selectable theme now has a gradient. Kept so restoring
            // them is just a matter of clearing their gradientColors.
            // Blur only the moving backdrop so the timer stays
            // readable; gradient themes remain sharp.
            EkagraVideoBackground(
                videoUrl = theme.videoUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.06f)
                    .adaptiveBlur(10.dp),
            )
        }
        val scrimAlpha by animateFloatAsState(
            targetValue = when {
                timerImmersiveActive -> 0.12f
                isDarkTheme -> 0.55f
                theme.gradientColors != null -> 0.08f
                else -> 0.38f
            },
            animationSpec = tween(600),
            label = "scrimAlpha",
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
    }
}
