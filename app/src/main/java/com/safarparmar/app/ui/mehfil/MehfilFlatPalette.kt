package com.safarparmar.app.ui.mehfil

import com.safarparmar.app.ui.theme.BgDark
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark

/**
 * Shared ivory, plum, peach, and lavender palette for phone and tablet Mehfil cards.
 * Dark mode supplies equivalent surface and text contrast.
 */
object MehfilFlatColors {
    val Bg @Composable get() = if (isDark) BgDark else Color(0xFFFFF5F0)
    // Preserve component fills independently of the screen canvas.
    val ComponentBackground @Composable get() = if (isDark) Color(0xFF211921) else Color(0xFFFFF5F0)
    val Text @Composable get() = if (isDark) Color(0xFFFFF0F6) else Color(0xFF39132E)
    val Muted @Composable get() = if (isDark) Color(0xFFCFBECB) else Color(0xFF75616F)
    val Hairline @Composable get() = if (isDark) Color(0xFF654D66) else Color(0xFFE8DEE5)
    val Surface @Composable get() = if (isDark) Color(0xFF342735) else Color(0xFFFFFDFC)

    private val isDark: Boolean
        @Composable get() = isPlannerDark

    /** Primary Mehfil accent — plum */
    val Primary @Composable get() = if (isDark) Color(0xFFF0BEDC) else Color(0xFF542347)

    val CommunityPanel @Composable get() = if (isDark) Color(0xFF211921) else Color(0xFFFFF0F6)
    val SandeshPanel @Composable get() = if (isDark) Color(0xFF684047) else Color(0xFFFFCBBE)
    val SandeshAccent @Composable get() = if (isDark) Color(0xFFFFB08A) else Color(0xFFE36A3C)
    val AcademicBadge @Composable get() = if (isDark) Color(0xFF293655) else Color(0xFFEDE0F4)
    val ReflectiveBadge @Composable get() = if (isDark) Color(0xFF243C30) else Color(0xFFE1F0DC)
    val Lavender @Composable get() = if (isDark) Color(0xFF513551) else Color(0xFFEDE0F4)
    val Community @Composable get() = Primary
    val Saved @Composable get() = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val Activity @Composable get() = Primary
    val Chats @Composable get() = Primary

    val Like @Composable get() = if (isDark) Color(0xFFF87171) else Color(0xFFFF386B)
    val Connect @Composable get() = Primary
    val Sandesh @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
}
