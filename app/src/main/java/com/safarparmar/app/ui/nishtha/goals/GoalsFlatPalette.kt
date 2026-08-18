package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.theme.isLightBackground
import androidx.compose.material3.MaterialTheme

/**
 * Goals flat-hairline palette — emerald primary (matches Nishtha Goals tab),
 * with clear semantic accents for kind / status / study sources.
 * Surfaces and ink reuse [PlannerFlatColors] so Goals sits on the same magazine sheet.
 */
object GoalsFlatColors {
    val Bg @Composable get() = PlannerFlatColors.BgCream
    val Text @Composable get() = PlannerFlatColors.TextDark
    val Muted @Composable get() = PlannerFlatColors.TextMuted
    val Hairline @Composable get() = PlannerFlatColors.BorderSoft

    private val isDark: Boolean
        @Composable get() = !MaterialTheme.colorScheme.background.isLightBackground()

    /** Primary Goals accent — dark forest green */
    val Primary @Composable get() = if (isDark) Color(0xFF10B981) else Color(0xFF045435)

    /** Done / completed — dark forest green */
    val Done @Composable get() = if (isDark) Color(0xFF34D399) else Color(0xFF045435)

    /** Today goal kind */
    val Today @Composable get() = Primary

    /** Repeat goal kind — sky */
    val Repeat @Composable get() = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    /** Scheduled goal kind — violet */
    val Scheduled @Composable get() = if (isDark) Color(0xFFC084FC) else Color(0xFF581C87)

    /** Insights / progress pulse — indigo */
    val Progress @Composable get() = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A)

    /** Ekagra-linked study — terracotta */
    val Ekagra @Composable get() = if (isDark) Color(0xFFFF9E80) else Color(0xFFC2410C)

    /** In progress / partial — amber */
    val Amber @Composable get() = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)

    /** Missed / destructive */
    val Danger @Composable get() = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    /** Soft fill behind selected option rows */
    val PrimarySoft @Composable get() = Primary.copy(alpha = if (isDark) 0.16f else 0.10f)
}
