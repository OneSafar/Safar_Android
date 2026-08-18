package com.safarparmar.app.ui.mehfil

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.components.isPlannerDark

/**
 * Mehfil flat-hairline + accent palette.
 * Surfaces/ink reuse [PlannerFlatColors]; accents are violet/indigo for Community.
 */
object MehfilFlatColors {
    val Bg @Composable get() = PlannerFlatColors.BgCream
    val Text @Composable get() = PlannerFlatColors.TextDark
    val Muted @Composable get() = PlannerFlatColors.TextMuted
    val Hairline @Composable get() = PlannerFlatColors.BorderSoft
    val Surface @Composable get() = PlannerFlatColors.CardWhite

    private val isDark: Boolean
        @Composable get() = isPlannerDark

    /** Primary Mehfil accent — violet */
    val Primary @Composable get() = if (isDark) Color(0xFFC084FC) else Color(0xFF581C87)

    val Community @Composable get() = Primary
    val Saved @Composable get() = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val Activity @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val Chats @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)

    val Like @Composable get() = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
    val Connect @Composable get() = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
    val Sandesh @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
}
