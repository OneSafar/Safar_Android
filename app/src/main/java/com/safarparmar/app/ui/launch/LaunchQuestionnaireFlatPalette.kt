package com.safarparmar.app.ui.launch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Launch questionnaire flat-hairline + accent palette.
 * Surfaces/ink reuse [PlannerFlatColors]; primary accent is violet (Kavach / focus).
 */
object LaunchFlatColors {
    val Bg @Composable get() = PlannerFlatColors.BgCream
    val Text @Composable get() = PlannerFlatColors.TextDark
    val Muted @Composable get() = PlannerFlatColors.TextMuted
    val Hairline @Composable get() = PlannerFlatColors.BorderSoft

    private val isDark: Boolean
        @Composable get() = !MaterialTheme.colorScheme.background.isLightBackground()

    /** Primary Kavach / setup accent — violet */
    val Primary @Composable get() = if (isDark) Color(0xFFC084FC) else Color(0xFF6D28D9)

    val Beast @Composable get() = if (isDark) Color(0xFFFF8A65) else Color(0xFFE64A19)
    val AlwaysOn @Composable get() = Primary
    val Normal @Composable get() = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)

    val Habit @Composable get() = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
    val Journal @Composable get() = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
    val Calm @Composable get() = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
}
