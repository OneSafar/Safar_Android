package com.safarparmar.app.ui.dhyan

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

    val Bg @Composable get() = if (isDark) BgDark else Color(0xFFFFF5F0)
    // Preserve component fills independently of the screen canvas.
    val ComponentBackground @Composable get() = if (isDark) Color(0xFF211921) else Color(0xFFFFF5F0)
    val Text @Composable get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF39132E)
    val Muted @Composable get() = if (isDark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Hairline @Composable get() = if (isDark) Color(0xFF3F3F46) else Color(0xFFE8DEE5)

    val OnGlassText @Composable get() = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val OnGlassMuted @Composable get() = if (isDark) Color(0xFFD1D1D6) else Color(0xFF3A3A3C)

    /** Primary Dhyan accent — exact web Deep Pink (#BE185D light, #F472B6 dark) */
    val Primary @Composable get() = if (isDark) Color(0xFFF0BEDC) else Color(0xFF542347)
    val PrimaryDeep = Color(0xFF421735)
    val PrimarySoft @Composable get() = if (isDark) Color(0xFFF0BEDC).copy(alpha = 0.20f) else Color(0xFF542347).copy(alpha = 0.12f)
    val PrimaryContainer @Composable get() = if (isDark) Color(0xFF513551) else Color(0xFFEDE0F4)
    val BorderHairline @Composable get() = if (isDark) Color(0xFFF0BEDC).copy(alpha = 0.20f) else Color(0xFF542347).copy(alpha = 0.15f)

    /** Card surfaces & borders */
    val CardBg @Composable get() = if (isDark) Color(0xFF342735) else Color(0xFFFFFDFC)
    val CardBorder @Composable get() = if (isDark) Color(0xFF3A2831) else Color(0xFFF1D8E3)

    /** Badges & status */
    val Emerald @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val EmeraldBg @Composable get() = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val EmeraldBorder @Composable get() = if (isDark) Color(0xFF34D399).copy(alpha = 0.35f) else Color(0xFF059669).copy(alpha = 0.25f)

    val Rose @Composable get() = if (isDark) Color(0xFFF0BEDC) else Color(0xFF542347)
    val Lotus @Composable get() = if (isDark) Color(0xFFE05282) else Color(0xFFFFCDE0)
    val Calm @Composable get() = if (isDark) Color(0xFF8A133B) else Color(0xFFE37A9A)
    val Sky @Composable get() = if (isDark) Color(0xFF7CB9E8) else Color(0xFF5B9BD5)

    /** Pink gradient canvas behind the session screen in light mode; canonical ground zero in dark. */
    val CanvasBrush @Composable get() = SolidColor(Bg)

    fun glassBody(isLight: Boolean): Color =
        if (isLight) Color(0xFFFFFDFC) else Color(0xFF342735)

    fun onGlassText(isLight: Boolean): Color =
        if (isLight) Color(0xFF1C1C1E) else Color(0xFFF5F5F7)

    fun onGlassMuted(isLight: Boolean): Color =
        if (isLight) Color(0xFF75616F) else Color(0xFFCFBECB)
}

