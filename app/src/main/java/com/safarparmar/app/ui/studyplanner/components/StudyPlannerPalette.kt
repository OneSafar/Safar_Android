package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.isSystemInDarkTheme

val LocalPlannerIsDarkTheme = staticCompositionLocalOf<Boolean?> { null }

val isPlannerDark: Boolean
    @Composable
    get() = LocalPlannerIsDarkTheme.current ?: isSystemInDarkTheme()

object PlannerFlatColors {
    val BgCream @Composable get() = if (isPlannerDark) Color(0xFF131316) else Color(0xFFFFF9F0)
    val CardWhite @Composable get() = if (isPlannerDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
    val TextDark @Composable get() = if (isPlannerDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    val TextMuted @Composable get() = if (isPlannerDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val BorderSoft @Composable get() = if (isPlannerDark) Color(0xFF33333D) else Color(0xFFE2DDF0)
    val ShadowSoft @Composable get() = if (isPlannerDark) Color.Black.copy(alpha = 0.2f) else Color(0xFF1E1B4B).copy(alpha = 0.04f)
    val PrimaryAccent @Composable get() = if (isPlannerDark) Color(0xFFE0654B) else Color(0xFFE0654B) // Coral for Planner
    val AccentShadow @Composable get() = if (isPlannerDark) Color(0xFFE0654B).copy(alpha = 0.3f) else Color(0xFFE0654B).copy(alpha = 0.3f)
    val AccentTint @Composable get() = if (isPlannerDark) Color(0xFFE0654B).copy(alpha = 0.15f) else Color(0xFFE0654B).copy(alpha = 0.1f)
}

/**
 * Card surface for every planner container. The "flat 2.0" look has been retired
 * in favour of the macOS Subtle glass system: this now delegates to the neutral
 * translucent [glassSurface] so all existing call sites become macOS glass with
 * no per-site changes. The name is kept so callers don't churn.
 */
@Composable
fun Modifier.flatCard(shape: Shape = RoundedCornerShape(16.dp)): Modifier =
    this.glassSurface(shape = shape)

/**
 * The planner's warm, muted accent palette — fixed constants, not derived from
 * MaterialTheme, since they're meant to stay the same warm tones regardless of light/dark
 * mode (unlike SafarSemanticColors, which is a cross-app light/dark façade). Originally
 * picked for the Create-Plan wizard's 3 study-style cards; promoted here so Syllabus,
 * Calendar, and Plan can share the same identity instead of each screen picking its own
 * ad hoc accents.
 */
object PlannerAccent {
    val Coral: Color @Composable get() = if (isPlannerDark) Color(0xFFFF7A00) else Color(0xFFEA580C)
    val Teal: Color @Composable get() = if (isPlannerDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val Amber: Color @Composable get() = if (isPlannerDark) Color(0xFFFBBF24) else Color(0xFFB45309)
}

/**
 * Tab-level signature accent — unified to planner coral so the bottom bar
 * always matches on-screen CTAs (Open >, %, Change My Plan, etc.).
 */
object PlannerTabAccent {
    private val Coral = Color(0xFFE0654B)
    val Home: Color @Composable get() = Coral
    val Plan: Color @Composable get() = Coral
    val Syllabus: Color @Composable get() = Coral
    val Calendar: Color @Composable get() = Coral
    val Progress: Color @Composable get() = Coral
}

/**
 * Revision Engine signature colors.
 */
object PlannerRevisionAccent {
    val Parent: Color @Composable get() = if (isPlannerDark) Color(0xFFA855F7) else Color(0xFF7C3AED)
    val Spaced: Color @Composable get() = if (isPlannerDark) Color(0xFF22D3EE) else Color(0xFF0891B2)
    val Custom: Color @Composable get() = if (isPlannerDark) Color(0xFFFBBF24) else Color(0xFFD97706)
}

/**
 * Semantic Task Status colors.
 */
object PlannerStatusAccent {
    val Done: Color @Composable get() = if (isPlannerDark) Color(0xFF34D399) else Color(0xFF059669)
    val Overdue: Color @Composable get() = if (isPlannerDark) Color(0xFFF87171) else Color(0xFFDC2626)
    val InProgress: Color @Composable get() = if (isPlannerDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    val Pending: Color @Composable get() = if (isPlannerDark) Color(0xFF94A3B8) else Color(0xFF64748B)
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
