package com.safarparmar.app.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    if (!com.safarparmar.app.performance.decorativeMotionEnabled()) return@composed this
    val phase = com.safarparmar.app.performance.rememberDecorationPhase(durationMillis)
    val colors = androidx.compose.runtime.remember { listOf(Color.Transparent, Color.White.copy(alpha = 0.25f), Color.Transparent) }
    val slope = androidx.compose.runtime.remember(angle) { tan(Math.toRadians(angle.toDouble())).toFloat() }
    this.drawWithContent {
        drawContent()
        val x = -shimmerWidth + phase() * (size.width + 2 * shimmerWidth)
        drawRect(Brush.linearGradient(colors, Offset(x, 0f), Offset(x + shimmerWidth, shimmerWidth * slope)))
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
    val enabled = com.safarparmar.app.performance.decorativeMotionEnabled()
    val phase = com.safarparmar.app.performance.rememberDecorationPhase(2800)

    Text(
        text = text,
        modifier = if (!enabled) modifier else modifier
            .graphicsLayer(alpha = 0.99f)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.linearGradient(
                        colors = RainbowShimmerColors,
                        start = Offset(-360f + phase() * (size.width + 720f), 0f),
                        end = Offset(phase() * (size.width + 720f), 0f),
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
    if (!com.safarparmar.app.performance.decorativeMotionEnabled()) return@composed this
    val phase = com.safarparmar.app.performance.rememberDecorationPhase(durationMillis)
    val colors = androidx.compose.runtime.remember { RainbowShimmerColors.map { it.copy(alpha = 0.4f) } }
    val slope = androidx.compose.runtime.remember(angle) { tan(Math.toRadians(angle.toDouble())).toFloat() }
    this.drawWithContent {
        drawContent()
        val x = -shimmerWidth + phase() * (size.width + 2 * shimmerWidth)
        drawRect(Brush.linearGradient(colors, Offset(x, 0f), Offset(x + shimmerWidth, shimmerWidth * slope)))
    }
}
