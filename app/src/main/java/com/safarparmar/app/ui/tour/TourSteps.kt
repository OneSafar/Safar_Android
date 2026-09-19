package com.safarparmar.app.ui.tour

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.ui.butterfly.ButterflyTourStep
import com.safarparmar.app.ui.butterfly.TooltipSide
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R

/** Tour steps shown on the Nishtha screen. */
@Composable
fun nishthaTourSteps() = listOf(
    ButterflyTourStep(
        title   = stringResource(R.string.tour_nishtha_welcome_title),
        message = stringResource(R.string.tour_nishtha_welcome_message),
        anchorX = 0.5f, anchorY = 0.25f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_daily_checkin_title),
        message = stringResource(R.string.tour_daily_checkin_message),
        anchorX = 0.10f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_journal_title),
        message = stringResource(R.string.tour_journal_message),
        anchorX = 0.30f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_pencil_simple_line
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_goals_title),
        message = stringResource(R.string.tour_goals_message),
        anchorX = 0.50f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_streaks_title),
        message = stringResource(R.string.tour_streaks_message),
        anchorX = 0.70f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_flame
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_analytics_title),
        message = stringResource(R.string.tour_analytics_message),
        anchorX = 0.90f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_chart_bar
    ),
)

@Composable
fun ekagraTourSteps() = listOf(
    // Step 0 — What is Ekagra Screen
    ButterflyTourStep(
        title   = stringResource(R.string.tour_ekagra_welcome_title),
        message = stringResource(R.string.tour_ekagra_welcome_message),
        anchorX = 0.5f, anchorY = 0.47f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),

    // Step 1 — Kavach
    ButterflyTourStep(
        title   = stringResource(R.string.tour_kavach_title),
        message = stringResource(R.string.tour_kavach_message),
        anchorX = 0.5f, anchorY = 0.26f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 2 — Modes
    ButterflyTourStep(
        title   = stringResource(R.string.tour_timer_modes_title),
        message = stringResource(R.string.tour_timer_modes_message),
        anchorX = 0.5f, anchorY = 0.32f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 3 — Settings
    ButterflyTourStep(
        title   = stringResource(R.string.tour_timer_settings_title),
        message = stringResource(R.string.tour_timer_settings_message),
        anchorX = 0.5f, anchorY = 0.45f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 4 — Start Pomodoro Session
    ButterflyTourStep(
        title   = stringResource(R.string.tour_start_pomodoro_title),
        message = stringResource(R.string.tour_start_pomodoro_message),
        anchorX = 0.5f, anchorY = 0.80f,
        tooltipSide = TooltipSide.TOP,
    ),

    // Step 5 — Floating pip
    ButterflyTourStep(
        title   = stringResource(R.string.tour_floating_timer_title),
        message = stringResource(R.string.tour_floating_timer_message),
        anchorX = 0.91f, anchorY = 0.13f,
        tooltipSide = TooltipSide.BOTTOM,
    ),

    // Step 6 — Session History
    ButterflyTourStep(
        title   = stringResource(R.string.tour_session_history_title),
        message = stringResource(R.string.tour_session_history_message),
        anchorX = 0.5f, anchorY = 0.45f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
)




/** Tour steps shown on the Mehfil screen. */
@Composable
fun mehfilTourSteps() = listOf(
    ButterflyTourStep(
        title   = stringResource(R.string.tour_mehfil_welcome_title),
        message = stringResource(R.string.tour_mehfil_welcome_message),
        anchorX = 0.5f, anchorY = 0.3f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_globe
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_community_posts_title),
        message = stringResource(R.string.tour_community_posts_message),
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_sandesh_title),
        message = stringResource(R.string.tour_sandesh_message),
        anchorX = 0.5f, anchorY = 0.18f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_megaphone
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_connect_title),
        message = stringResource(R.string.tour_connect_message),
        anchorX = 0.88f, anchorY = 0.45f,
        tooltipSide = TooltipSide.LEFT,
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_connections_tab_title),
        message = stringResource(R.string.tour_connections_tab_message),
        anchorX = 0.88f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_create_post_title),
        message = stringResource(R.string.tour_create_post_message),
        anchorX = 0.88f, anchorY = 0.82f,
        tooltipSide = TooltipSide.LEFT,
        iconRes = com.safarparmar.app.R.drawable.ic_pencil_simple_line
    ),
)

/** Tour steps shown on the Dhyan screen. */
@Composable
fun dhyanTourSteps() = listOf(
    ButterflyTourStep(
        title   = stringResource(R.string.tour_dhyan_welcome_title),
        message = stringResource(R.string.tour_dhyan_welcome_message),
        anchorX = 0.5f, anchorY = 0.28f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_person_standing
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_breathe_title),
        message = stringResource(R.string.tour_breathe_message),
        anchorX = 0.25f, anchorY = 0.93f,
        tooltipSide = TooltipSide.TOP,
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_techniques_title),
        message = stringResource(R.string.tour_techniques_message),
        anchorX = 0.88f, anchorY = 0.82f,
        tooltipSide = TooltipSide.LEFT,
        iconRes = com.safarparmar.app.R.drawable.ic_wind
    ),
    ButterflyTourStep(
        title   = stringResource(R.string.tour_ambient_title),
        message = stringResource(R.string.tour_ambient_message),
        anchorX = 0.84f, anchorY = 0.16f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_music_notes_simple
    ),
)

/** Tour steps shown on the Exam Planner screen. */
@Composable
fun studyPlannerTourSteps() = listOf(
    // Step 0 (YOUR_EXAMS)
    ButterflyTourStep(
        title   = stringResource(R.string.tour_exam_plans_title),
        message = stringResource(R.string.tour_exam_plans_message),
        anchorX = 0.5f, anchorY = 0.45f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_library
    ),
    // Step 1 (YOUR_EXAMS) - Create Plan
    ButterflyTourStep(
        title   = stringResource(R.string.tour_new_plan_title),
        message = stringResource(R.string.tour_new_plan_message),
        anchorX = 0.5f, anchorY = 0.85f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    // Step 2 (PLAN) - Dashboard
    ButterflyTourStep(
        title   = stringResource(R.string.tour_plan_overview_title),
        message = stringResource(R.string.tour_plan_overview_message),
        anchorX = 0.5f, anchorY = 0.15f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 3 (PLAN) - Quick Filters
    ButterflyTourStep(
        title   = stringResource(R.string.tour_preparation_status_title),
        message = stringResource(R.string.tour_preparation_status_message),
        anchorX = 0.5f, anchorY = 0.35f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 4 (PLAN) - Daily Todo
    ButterflyTourStep(
        title   = stringResource(R.string.tour_daily_todo_title),
        message = stringResource(R.string.tour_daily_todo_message),
        anchorX = 0.5f, anchorY = 0.45f,
        tooltipSide = TooltipSide.TOP,
    ),
    // Step 5 (PLAN) - Today's Mission
    ButterflyTourStep(
        title   = stringResource(R.string.tour_today_plan_title),
        message = stringResource(R.string.tour_today_plan_message),
        anchorX = 0.5f, anchorY = 0.55f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_target
    ),
    // Step 6 (SYLLABUS)
    ButterflyTourStep(
        title   = stringResource(R.string.tour_syllabus_title),
        message = stringResource(R.string.tour_syllabus_message),
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.BOTTOM,
    ),
    // Step 7 (CALENDAR)
    ButterflyTourStep(
        title   = stringResource(R.string.tour_calendar_title),
        message = stringResource(R.string.tour_calendar_message),
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.BOTTOM,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 8 (CALENDAR) - Revision & Missed Topics Buttons
    ButterflyTourStep(
        title   = stringResource(R.string.tour_revision_title),
        message = stringResource(R.string.tour_revision_message),
        anchorX = 0.5f, anchorY = 0.85f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_calendar_dots
    ),
    // Step 9 (INSIGHTS)
    ButterflyTourStep(
        title   = stringResource(R.string.tour_progress_title),
        message = stringResource(R.string.tour_progress_message),
        anchorX = 0.5f, anchorY = 0.5f,
        tooltipSide = TooltipSide.TOP,
        iconRes = com.safarparmar.app.R.drawable.ic_chart_bar
    ),
)
