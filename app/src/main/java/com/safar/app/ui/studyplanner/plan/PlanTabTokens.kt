package com.safar.app.ui.studyplanner.plan

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object PlanSpacing {
    val horizontal = 12.dp
    val section = 8.dp
    val rowVertical = 10.dp
}

object PlanShapes {
    val banner = RoundedCornerShape(12.dp)
    val panel = RoundedCornerShape(16.dp)
    val field = RoundedCornerShape(10.dp)
}

/** Max fraction of screen height for the scrollable config panel. */
const val PLAN_CONFIG_MAX_HEIGHT_FRACTION = 0.42f
