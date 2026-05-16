package com.safar.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.tan

/**
 * Adds a modern shimmer highlight effect that moves across the component.
 * Perfect for premium primary call-to-action buttons.
 */
fun Modifier.shimmer(
    durationMillis: Int = 2500,
    shimmerWidth: Float = 600f,
    angle: Float = 25f
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "button_shimmer")
    
    // We animate from a negative offset (left of button) to a large positive offset (right of button)
    val translateAnim by transition.animateFloat(
        initialValue = -shimmerWidth,
        targetValue = 1200f + shimmerWidth, // Assumes max button width around 1000f; works for most screen widths
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.25f), // Subtle white highlight
            Color.White.copy(alpha = 0.0f),
        ),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + shimmerWidth, shimmerWidth * tan(Math.toRadians(angle.toDouble())).toFloat())
    )

    this.drawWithContent {
        drawContent()
        drawRect(brush = shimmerBrush)
    }
}
