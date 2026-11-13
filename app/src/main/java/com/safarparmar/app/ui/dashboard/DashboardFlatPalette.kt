package com.safarparmar.app.ui.dashboard

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

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

    val Bg @Composable get() = if (isDark) BgDark else Color(0xFFFAF8FF)
    // Keep dialogs and sheets at their existing elevation color.
    val ComponentBackground @Composable get() = if (isDark) Color(0xFF211921) else Color(0xFFFAF8FF)
    val Text @Composable get() = if (isDark) Color(0xFFFFF0F6) else Color(0xFF241544)
    /** Stronger muted ink for captions on cream / charcoal. */
    val Muted @Composable get() = if (isDark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Hairline @Composable get() = if (isDark) Color(0xFF654D66) else Color(0xFFEADFE6)

    val Accent @Composable get() = if (isDark) RoyalPurpleDark else RoyalPurple

    val Paper @Composable get() = if (isDark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val Soft @Composable get() = if (isDark) RoyalPurpleSoftDark else RoyalPurpleSoft

    fun glassBody(isDark: Boolean): Color =
        if (isDark) Color(0xFF342735) else Color(0xFFFFFDFC)

    fun onGlassText(isDark: Boolean): Color =
        if (isDark) Color(0xFFFFF0F6) else Color(0xFF241544)

    /** High-contrast muted for text on opaque glass tiles. */
    fun onGlassMuted(isDark: Boolean): Color =
        if (isDark) Color(0xFFCFBECB) else Color(0xFF75616F)
}
