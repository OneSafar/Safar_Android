package com.example.liquidglass

/**
 * LIQUID GLASS — THEME
 * ---------------------------------------------------------------------------
 * Color palette, dark color scheme and shape scale for the design system.
 * Drop this file into your project (adjust the package name above), then
 * wrap your app's content in `LiquidGlassTheme { ... }`.
 */

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Named palette — deep "night sky" base with four aurora accent colors
 *  that drift behind the frosted panels (see GlassEffects.kt). */
object GlassPalette {
    val Ink900 = Color(0xFF0A0E1A)
    val Ink800 = Color(0xFF11162A)
    val Ink700 = Color(0xFF1A2036)

    val Violet = Color(0xFF7C6CF0)
    val Cyan = Color(0xFF4FD8EB)
    val Coral = Color(0xFFFF6B81)
    val Amber = Color(0xFFFFC46B)

    val TextPrimary = Color(0xFFF5F7FA)
    val TextSecondary = Color(0xFFA9B1C6)
}

private val LiquidGlassColorScheme = darkColorScheme(
    primary = GlassPalette.Violet,
    secondary = GlassPalette.Cyan,
    tertiary = GlassPalette.Amber,
    background = GlassPalette.Ink900,
    surface = GlassPalette.Ink800,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = GlassPalette.TextPrimary,
    onSurface = GlassPalette.TextPrimary,
    error = GlassPalette.Coral,
    onError = Color.White,
)

/** Rounded, soft shape scale — glass reads best with generous corner radii. */
val LiquidGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun LiquidGlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiquidGlassColorScheme,
        shapes = LiquidGlassShapes,
        content = content,
    )
}
