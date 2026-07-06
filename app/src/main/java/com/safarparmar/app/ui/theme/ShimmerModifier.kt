package com.safarparmar.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

private val RainbowShimmerColors = listOf(
    Color(0xFFFF1744),
    Color(0xFFFF9100),
    Color(0xFFFFEA00),
    Color(0xFF00E676),
    Color(0xFF2979FF),
    Color(0xFFD500F9),
    Color(0xFFFF1744),
)

@Composable
fun RainbowShimmerText(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    style: TextStyle = LocalTextStyle.current,
) {
    val transition = rememberInfiniteTransition(label = "rainbow_text_shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = -360f,
        targetValue = 800f + 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rainbow_text_shimmer_translation",
    )
    
    Text(
        text = text,
        modifier = modifier
            .graphicsLayer(alpha = 0.99f)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.linearGradient(
                        colors = RainbowShimmerColors,
                        start = Offset(translateAnim.value, 0f),
                        end = Offset(translateAnim.value + 360f, 0f),
                    ),
                    blendMode = androidx.compose.ui.graphics.BlendMode.SrcIn
                )
            },
        style = style.copy(
            fontWeight = fontWeight,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Animated rainbow gradient overlay (use RainbowShimmerText for label text instead).
 */
fun Modifier.rainbowShimmer(
    durationMillis: Int = 2800,
    shimmerWidth: Float = 480f,
    angle: Float = 20f,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "rainbow_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -shimmerWidth,
        targetValue = 1200f + shimmerWidth,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rainbow_shimmer_translation",
    )

    val rainbowBrush = Brush.linearGradient(
        colors = RainbowShimmerColors.map { it.copy(alpha = 0.4f) },
        start = Offset(translateAnim, 0f),
        end = Offset(
            translateAnim + shimmerWidth,
            shimmerWidth * tan(Math.toRadians(angle.toDouble())).toFloat(),
        ),
    )

    this.drawWithContent {
        drawContent()
        drawRect(brush = rainbowBrush)
    }
}
