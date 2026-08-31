// Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4
// Hallmark · semantic accent system: template-green · custom-orange · status colours preserved
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

import androidx.compose.material3.MaterialTheme
import com.safarparmar.app.ui.theme.isLightBackground

val LocalPlannerIsDarkTheme = staticCompositionLocalOf<Boolean?> { null }

/**
 * Plan-origin signature accent. It is intentionally separate from status colours:
 * template plans use deep green, while custom/manual plans use orange. Callers
 * provide it only around the active wizard or plan so destructive, warning, and
 * success actions retain their own semantic colours.
 */
val LocalPlannerAccent = staticCompositionLocalOf<Color?> { null }

object PlannerSourceAccent {
    val Template: Color
        @Composable get() = if (isPlannerDark) Color(0xFF0C8065) else Color(0xFF064E3B)

    val Custom: Color
        @Composable get() = if (isPlannerDark) Color(0xFFC84A0A) else Color(0xFFCF4A0A)
}

/**
 * Planner flat palette dark flag. Prefer [LocalPlannerIsDarkTheme] (set by SafarTheme /
 * feature screens). Fallback uses **Material** background luminance — never system dark —
 * so system-dark + app-light no longer yields black cream sheets on light cards.
 */
val isPlannerDark: Boolean
    @Composable
    get() = LocalPlannerIsDarkTheme.current
        ?: !MaterialTheme.colorScheme.background.isLightBackground()

object PlannerFlatColors {
    val BgCream @Composable get() = if (isPlannerDark) Color(0xFF131316) else Color(0xFFFFF9F0)
    val CardWhite @Composable get() = if (isPlannerDark) Color(0xFF1E1E24) else Color(0xFFFFFFFF)
    val TextDark @Composable get() = if (isPlannerDark) Color(0xFFF8FAFC) else Color(0xFF1E1B4B)
    val TextMuted @Composable get() = if (isPlannerDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val BorderSoft @Composable get() = if (isPlannerDark) Color(0xFF33333D) else Color(0xFFE2DDF0)
    val ShadowSoft @Composable get() = if (isPlannerDark) Color.Black.copy(alpha = 0.2f) else Color(0xFF1E1B4B).copy(alpha = 0.04f)
    val PrimaryAccent @Composable get() = LocalPlannerAccent.current ?: Color(0xFFE0654B)
    val AccentShadow @Composable get() = PrimaryAccent.copy(alpha = 0.30f)
    val AccentTint @Composable get() = PrimaryAccent.copy(alpha = if (isPlannerDark) 0.15f else 0.10f)
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
    val Home: Color @Composable get() = PlannerFlatColors.PrimaryAccent
    val Plan: Color @Composable get() = PlannerFlatColors.PrimaryAccent
    val Syllabus: Color @Composable get() = PlannerFlatColors.PrimaryAccent
    val Calendar: Color @Composable get() = PlannerFlatColors.PrimaryAccent
    val Progress: Color @Composable get() = PlannerFlatColors.PrimaryAccent
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
