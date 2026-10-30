package com.safarparmar.app.ui.mehfil

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark

/**
 * Shared cream, navy, and violet reference palette for phone and tablet Mehfil cards.
 * Dark mode supplies equivalent surface and text contrast.
 */
object MehfilFlatColors {
    val Bg @Composable get() = if (isDark) Color(0xFF131316) else Color(0xFFFFFBF5)
    val Text @Composable get() = if (isDark) Color(0xFFF3EEFF) else Color(0xFF0C1039)
    val Muted @Composable get() = if (isDark) Color(0xFFB4AFCC) else Color(0xFF555E85)
    val Hairline @Composable get() = if (isDark) Color(0xFF363240) else Color(0xFFE8E5ED)
    val Surface @Composable get() = if (isDark) Color(0xFF211E2B) else Color.White

    private val isDark: Boolean
        @Composable get() = isPlannerDark

    /** Primary Mehfil accent — violet */
    val Primary @Composable get() = if (isDark) Color(0xFFC084FC) else Color(0xFF6525E8)

    val CommunityPanel @Composable get() = if (isDark) Color(0xFF251E38) else Color(0xFFF3EEFF)
    val SandeshPanel @Composable get() = if (isDark) Color(0xFF33251F) else Color(0xFFFFF0E4)
    val SandeshAccent @Composable get() = if (isDark) Color(0xFFFFB08A) else Color(0xFFE36A3C)
    val AcademicBadge @Composable get() = if (isDark) Color(0xFF293655) else Color(0xFFDCE7FF)
    val ReflectiveBadge @Composable get() = if (isDark) Color(0xFF243C30) else Color(0xFFE1F0DC)
    val Lavender @Composable get() = if (isDark) Color(0xFF342441) else Color(0xFFF5ECFF)
    val Community @Composable get() = Primary
    val Saved @Composable get() = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val Activity @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val Chats @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val Like @Composable get() = if (isDark) Color(0xFFF87171) else Color(0xFFFF386B)
    val Connect @Composable get() = if (isDark) Color(0xFF818CF8) else Primary
    val Sandesh @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
}
