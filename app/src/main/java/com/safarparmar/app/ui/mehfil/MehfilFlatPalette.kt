package com.safarparmar.app.ui.mehfil

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark

/**
 * Shared ivory, plum, peach, and lavender palette for phone and tablet Mehfil cards.
 * Dark mode supplies equivalent surface and text contrast.
 */
object MehfilFlatColors {
    val Bg @Composable get() = if (isDark) BgDark else Color(0xFFFAF8FF)
    // Preserve component fills independently of the screen canvas.
    val ComponentBackground @Composable get() = if (isDark) Color(0xFF211921) else Color(0xFFFAF8FF)
    val Text @Composable get() = if (isDark) Color(0xFFFFF0F6) else Color(0xFF241544)
    val Muted @Composable get() = if (isDark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Hairline @Composable get() = if (isDark) Color(0xFF654D66) else Color(0xFFE8DEE5)
    val Surface @Composable get() = if (isDark) Color(0xFF342735) else Color(0xFFFFFDFC)

    private val isDark: Boolean
        @Composable get() = isPlannerDark

    /** Primary Mehfil accent — plum */
    val Primary @Composable get() = if (isDark) RoyalPurpleDark else RoyalPurple

    val CommunityPanel @Composable get() = if (isDark) Color(0xFF211921) else Color(0xFFFFF0F6)
    val SandeshPanel @Composable get() = if (isDark) RoyalPurpleSoftDark else RoyalPurpleSoft
    val SandeshAccent @Composable get() = if (isDark) Color(0xFFFFB08A) else Color(0xFFE36A3C)
    val AcademicBadge @Composable get() = if (isDark) Color(0xFF293655) else RoyalPurpleSoft
    val ReflectiveBadge @Composable get() = if (isDark) Color(0xFF243C30) else Color(0xFFE1F0DC)
    val Lavender @Composable get() = if (isDark) RoyalPurpleSoftDark else RoyalPurpleSoft
    val Community @Composable get() = Primary
    val Saved @Composable get() = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val Activity @Composable get() = Primary
    val Chats @Composable get() = Primary

    val Like @Composable get() = if (isDark) Color(0xFFF87171) else Color(0xFFFF386B)
    val Connect @Composable get() = Primary
    val Sandesh @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
}
