package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The planner's warm, muted accent palette — fixed constants, not derived from
 * MaterialTheme, since they're meant to stay the same warm tones regardless of light/dark
 * mode (unlike SafarSemanticColors, which is a cross-app light/dark façade). Originally
 * picked for the Create-Plan wizard's 3 study-style cards; promoted here so Syllabus,
 * Calendar, and Plan can share the same identity instead of each screen picking its own
 * ad hoc accents.
 */
object PlannerAccent {
    val Coral = Color(0xFFE0654B)
    val Teal = Color(0xFF3E7C8C)
    val Amber = Color(0xFFCE9B34)
}

/** Softened, muted versions of the Calendar tab's original fully-saturated status colors —
 *  centralized here so a future palette tweak doesn't require hunting through CalendarTab.kt. */
object PlannerCalendarStatus {
    val Planned = Color(0xFF7C90B0)
    val Done = Color(0xFF2E7D32)
    val Overdue = Color(0xFFC62828)
    val Off = Color(0xFFEF6C00)
}

/** Shared corner-radius tokens so Syllabus/Calendar/Plan stop picking slightly different
 *  radii ad hoc, mirroring the existing plan-scoped PlanShapes/PlanSpacing in PlanTabTokens.kt. */
object PlannerCardShapes {
    val card: Shape @Composable get() = RoundedCornerShape(18.dp)
    val row: Shape @Composable get() = RoundedCornerShape(14.dp)
}
