package com.safarparmar.app.ui.dhyan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Dhyan flat-hairline + pink accent palette.
 * Surfaces/ink key off Material background luminance (same signal as glass tiles).
 */
object DhyanFlatColors {
    private val isLight: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.isLightBackground()

    private val isDark: Boolean
        @Composable get() = !isLight

    val Bg @Composable get() = if (isDark) Color(0xFF131316) else Color(0xFFFFF9F0)
    val Text @Composable get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    val Muted @Composable get() = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val Hairline @Composable get() = if (isDark) Color(0xFF3F3F46) else Color(0xFFE2DDF0)

    val OnGlassText @Composable get() = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val OnGlassMuted @Composable get() = if (isDark) Color(0xFFD1D1D6) else Color(0xFF3A3A3C)

    /** Primary Dhyan accent — action pink */
    val Primary @Composable get() = if (isDark) Color(0xFFE86B96) else Color(0xFFF04880)
    val PrimarySoft @Composable get() = if (isDark) Color(0xFFE86B96).copy(alpha = 0.22f) else Color(0xFFFF7AA8).copy(alpha = 0.18f)

    val Rose @Composable get() = if (isDark) Color(0xFFE05282) else Color(0xFFF49BB7)
    val Lotus @Composable get() = if (isDark) Color(0xFFE05282) else Color(0xFFFFCDE0)
    val Calm @Composable get() = if (isDark) Color(0xFF8A133B) else Color(0xFFE37A9A)
    val Sky @Composable get() = if (isDark) Color(0xFF7CB9E8) else Color(0xFF5B9BD5)

    /** Pink gradient canvas behind the session screen. */
    val CanvasBrush @Composable get() = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF1A1016),
                Color(0xFF2A1520),
                Color(0xFF1A1016),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFFFF0F5),
                Color(0xFFFFD6E7),
                Color(0xFFF8C8DC),
            ),
        )
    }

    fun glassBody(isLight: Boolean): Color =
        if (isLight) Color(0xFFF9F9FB) else Color(0xFF2C2C2E)

    fun onGlassText(isLight: Boolean): Color =
        if (isLight) Color(0xFF1C1C1E) else Color(0xFFF5F5F7)

    fun onGlassMuted(isLight: Boolean): Color =
        if (isLight) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
}
