package com.example.liquidglass

/**
 * LIQUID GLASS — EFFECTS
 * ---------------------------------------------------------------------------
 * The heart of the design language:
 *
 *  1. `Modifier.liquidGlass(...)` — the reusable "frosted panel" look
 *     (translucent tint + soft rim border + drop shadow) that every
 *     component in GlassComponents.kt / GlassScaffold.kt is built on.
 *
 *  2. `LiquidGlassBackdrop` — a slowly drifting, blurred aurora of color
 *     that sits behind your screen content. Because it's blurred and the
 *     panels above it are translucent, the colors bleed softly through
 *     the glass, which is what sells the "liquid glass" effect. On API 31+
 *     it uses a real RenderEffect blur; below that it gracefully falls back
 *     to soft radial gradients (no blur, but still looks intentional).
 */

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader

/**
 * The signature "frosted glass panel" modifier. Apply it to any Box/Row/
 * Column to turn it into a liquid-glass surface: shadow -> clip -> tinted
 * translucent fill -> soft diagonal rim-light border.
 *
 * @param shape corner shape of the panel
 * @param surfaceTint base tint color of the glass (white = neutral frost)
 * @param tintAlpha how strong the tint/fill is (0.08–0.20 reads as glass;
 *   higher values read as a solid tinted card)
 * @param elevation drop shadow strength
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    surfaceTint: Color = Color.White,
    tintAlpha: Float = 0.14f,
    elevation: Dp = 16.dp,
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.35f),
        spotColor = Color.Black.copy(alpha = 0.45f),
        clip = false,
    )
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                surfaceTint.copy(alpha = (tintAlpha * 1.4f).coerceAtMost(1f)),
                surfaceTint.copy(alpha = tintAlpha * 0.6f),
            ),
        ),
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.14f),
            ),
            start = Offset(0f, 0f),
            end = Offset(260f, 260f),
        ),
        shape = shape,
    )

/**
 * Full-bleed animated background: a handful of soft color blobs drifting
 * in a slow loop over an ink-dark base, blurred so they read as ambient
 * light rather than shapes. Place this as the very first child of a root
 * Box, with your glass panels stacked on top of it.
 */
@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    blobColors: List<Color> = listOf(
        GlassPalette.Violet, GlassPalette.Cyan, GlassPalette.Coral, GlassPalette.Amber,
    ),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_backdrop_drift")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
        ),
        label = "drift_angle",
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(GlassPalette.Ink900)
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = AndroidRenderEffect
                        .createBlurEffect(140f, 140f, Shader.TileMode.CLAMP)
                        .asComposeRenderEffect()
                }
            },
    ) {
        val w = size.width
        val h = size.height
        blobColors.forEachIndexed { index, color ->
            val angleDeg = drift + index * (360f / blobColors.size)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cx = w / 2f + cos(angleRad).toFloat() * w * 0.32f
            val cy = h / 2f + sin(angleRad).toFloat() * h * 0.22f
            val radius = w * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
    }
}
