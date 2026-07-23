package com.safarparmar.app.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Dashboard flat-hairline + accent palette.
 * Ink keys off Material background luminance so light/dark stay readable.
 */
object DashboardFlatColors {
    private val isLight: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.isLightBackground()

    private val isDark: Boolean
        @Composable get() = !isLight

    val Bg @Composable get() = if (isDark) Color(0xFF131316) else Color(0xFFFFF9F0)
    val Text @Composable get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    /** Stronger muted ink for captions on cream / charcoal. */
    val Muted @Composable get() = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val Hairline @Composable get() = if (isDark) Color(0xFF3F3F46) else Color(0xFFE2DDF0)

    val Accent @Composable get() = if (isDark) Color(0xFF64B5FF) else Color(0xFF0A84FF)

    fun glassBody(isDark: Boolean): Color =
        if (isDark) Color(0xFF2C2C2E) else Color(0xFFF9F9FB)

    fun onGlassText(isDark: Boolean): Color =
        if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)

    /** High-contrast muted for text on opaque glass tiles. */
    fun onGlassMuted(isDark: Boolean): Color =
        if (isDark) Color(0xFFD1D1D6) else Color(0xFF3A3A3C)
}
