package com.safarparmar.app.ui.dashboard

import com.safarparmar.app.ui.theme.BgDark
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

    val Bg @Composable get() = if (isDark) BgDark else Color(0xFFFFF5F0)
    // Keep dialogs and sheets at their existing elevation color.
    val ComponentBackground @Composable get() = if (isDark) Color(0xFF211921) else Color(0xFFFFF5F0)
    val Text @Composable get() = if (isDark) Color(0xFFFFF0F6) else Color(0xFF39132E)
    /** Stronger muted ink for captions on cream / charcoal. */
    val Muted @Composable get() = if (isDark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Hairline @Composable get() = if (isDark) Color(0xFF654D66) else Color(0xFFEADFE6)

    val Accent @Composable get() = if (isDark) Color(0xFFF0BEDC) else Color(0xFF491438)

    val Paper @Composable get() = if (isDark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val Soft @Composable get() = if (isDark) Color(0xFF513551) else Color(0xFFEDE0F4)

    fun glassBody(isDark: Boolean): Color =
        if (isDark) Color(0xFF342735) else Color(0xFFFFFDFC)

    fun onGlassText(isDark: Boolean): Color =
        if (isDark) Color(0xFFFFF0F6) else Color(0xFF39132E)

    /** High-contrast muted for text on opaque glass tiles. */
    fun onGlassMuted(isDark: Boolean): Color =
        if (isDark) Color(0xFFCFBECB) else Color(0xFF75616F)
}
