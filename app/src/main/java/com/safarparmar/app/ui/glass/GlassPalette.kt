package com.safarparmar.app.ui.glass

/**
 * SAFAR LIQUID GLASS — PALETTE, MODIFIER & BACKDROP
 * ───────────────────────────────────────────────────────────────────────────
 * - SafarGlassPalette  : color tokens for dark AND light modes
 * - Modifier.liquidGlass : frosted-panel look (transparent tint + rim border)
 * - LiquidGlassBackdrop  : solid white canvas (light) / solid black canvas (dark)
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

    // ── Light mode ──
    val LightViolet   = Color(0xFF9575CD)  // medium purple
    val LightPink     = Color(0xFFEC407A)  // medium rose
    val LightCoral    = Color(0xFFE07AAE)  // dusty rose
    val LightLavender = Color(0xFFBA9FE4)  // soft purple

    val LightTextPrimary   = Color(0xFF1A1A2E)
    val LightTextSecondary = Color(0xFF5A5A7A)
}

// ─────────────────────────────────────────────────────────────────────────────
// MODIFIER: liquidGlass
// The "frosted panel" look. Removes standard shadow completely to avoid the ugly
// Material 3 shadow rectangle from showing through the transparent design.
// ─────────────────────────────────────────────────────────────────────────────

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    surfaceTint: Color = Color.White,
    tintAlpha: Float = 0.06f, // Mostly transparent, a little bit translucent
    isLight: Boolean = false,
): Modifier {
    val borderBrush = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.16f),
                Color.Black.copy(alpha = 0.03f),
                Color.Black.copy(alpha = 0.10f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(260f, 260f),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.12f),
            ),
            start = Offset(0f, 0f),
            end   = Offset(260f, 260f),
        )
    }

    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    surfaceTint.copy(alpha = (tintAlpha * 1.2f).coerceAtMost(1f)),
                    surfaceTint.copy(alpha = tintAlpha * 0.8f),
                ),
            ),
        )
        .border(
            width = 1.dp,
            brush = borderBrush,
            shape = shape,
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// COMPOSABLE: LiquidGlassBackdrop
// Pure solid canvas backdrops for dark and light themes (no gradients).
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    isLight: Boolean = false,
) {
    val baseColor = if (isLight) Color.White else Color.Black
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    )
}
