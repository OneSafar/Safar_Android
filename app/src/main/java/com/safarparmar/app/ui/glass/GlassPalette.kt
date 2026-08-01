package com.safarparmar.app.ui.glass

/**
 * SAFAR LIQUID GLASS — PALETTE, MODIFIER & BACKDROP
 * ───────────────────────────────────────────────────────────────────────────
 * Light mode (Mac Control Center recipe):
 *   cool grey frosted tint + white rim highlight + soft lift shadow
 * Dark mode: translucent white tint on dark canvas
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// PALETTE
// ─────────────────────────────────────────────────────────────────────────────

object SafarGlassPalette {

    // ── Colors ──
    val Violet   = Color(0xFF7C5AD9)  // Safar deep purple
    val Pink     = Color(0xFFFF69B4)  // Safar signature pink
    val Coral    = Color(0xFFE47AB5)  // warm rose-pink
    val Lavender = Color(0xFFB39DDB)  // soft lavender

    val TextPrimary   = Color(0xFFF5F7FA)
    val TextSecondary = Color(0xFFA9B1C6)

    // ── Light mode decorative lavender (titles/links — not primary button fill) ──
    val LightViolet   = Color(0xFFA78BFA)
    val LightPink     = Color(0xFFEC407A)  // medium rose
    val LightCoral    = Color(0xFFE07AAE)  // dusty rose
    val LightLavender = Color(0xFFBA9FE4)  // soft purple

    val LightTextPrimary   = Color(0xFF1A1A2E)
    val LightTextSecondary = Color(0xFF5A5A7A)

    /** Cool grey glass fill — readable on light canvases (not pure white). */
    val LightGlassTint = Color(0xFFD6DAE2)
    /** Soft lift shadow color (Mac-style cool grey). */
    val LightGlassShadow = Color(0xFF7A8498)
}

// ─────────────────────────────────────────────────────────────────────────────
// MODIFIER: liquidGlass
// Light = Mac Control Center frosted grey; dark = translucent white sheen.
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    surfaceTint: Color = Color.White,
    tintAlpha: Float = 0.06f,
    isLight: Boolean = false,
): Modifier {
    // Light mode: never use pure white — cool grey is what keeps glass visible.
    val isNearWhite = surfaceTint.red > 0.95f && surfaceTint.green > 0.95f && surfaceTint.blue > 0.95f
    val tint = if (isLight && isNearWhite) SafarGlassPalette.LightGlassTint else surfaceTint
    // Light panels need enough body to read, but stay translucent so the canvas shows through.
    val alpha = if (isLight) tintAlpha.coerceIn(0.28f, 0.62f) else tintAlpha

    val borderBrush = if (isLight) {
        // Bright rim highlight (Mac), not a dark stroke.
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.55f),
            ),
            start = Offset(0f, 0f),
            end = Offset(260f, 260f),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.12f),
            ),
            start = Offset(0f, 0f),
            end = Offset(260f, 260f),
        )
    }

    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    tint.copy(alpha = (alpha * 1.15f).coerceAtMost(0.75f)),
                    tint.copy(alpha = alpha * 0.85f),
                ),
            ),
        )
        .drawBehind {
            // Top specular highlight — glass catching light.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isLight) 0.55f else 0.12f),
                        Color.Transparent,
                    ),
                    endY = size.height * 0.45f,
                ),
            )
        }
        .border(
            width = if (isLight) 0.9.dp else 1.dp,
            brush = borderBrush,
            shape = shape,
        )
}

/** Soft cool-grey drop shadow that lifts glass off a light canvas (Mac Control Center). */
fun Modifier.glassLiftShadow(
    shape: Shape,
    isLight: Boolean,
    elevation: Dp = if (isLight) 14.dp else 6.dp,
): Modifier = if (isLight) {
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = SafarGlassPalette.LightGlassShadow.copy(alpha = 0.32f),
        spotColor = SafarGlassPalette.LightGlassShadow.copy(alpha = 0.24f),
    )
} else {
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x22000000),
        spotColor = Color(0x18000000),
    )
}

/** Dhyan-parity corner radius for shared Safar glass chrome. */
val SafarGlassChromeRadius = 14.dp

/**
 * Canonical frosted panel stack (Dhyan recipe):
 * cool-grey lift shadow + liquidGlass with LightGlassTint @ ~0.48 (light) / white @ ~0.10 (dark).
 */
fun Modifier.safarFrostedPanel(
    isLight: Boolean,
    shape: Shape = RoundedCornerShape(SafarGlassChromeRadius),
    tintAlpha: Float? = null,
    elevation: Dp = if (isLight) 14.dp else 6.dp,
): Modifier {
    val surfaceTint = if (isLight) SafarGlassPalette.LightGlassTint else Color.White
    val alpha = tintAlpha ?: if (isLight) 0.48f else 0.10f
    return this
        .glassLiftShadow(shape = shape, isLight = isLight, elevation = elevation)
        .liquidGlass(
            shape = shape,
            surfaceTint = surfaceTint,
            tintAlpha = alpha,
            isLight = isLight,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPOSABLE: LiquidGlassBackdrop / SafarGlassBackdrop
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
) {
    SafarGlassBackdrop(modifier = modifier, isLight = isLight)
}

/** Cool grey wall (light) / black canvas (dark) — Dhyan calm backdrop. */
@Composable
fun SafarGlassBackdrop(
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLight) Color(0xFFE9EBF0) else Color.Black),
    )
}
