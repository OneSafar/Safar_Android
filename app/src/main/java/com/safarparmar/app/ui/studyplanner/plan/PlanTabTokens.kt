package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object PlanSpacing {
    val horizontal = 12.dp
    val section = 8.dp
    val rowVertical = 10.dp
}

object PlanShapes {
    val banner: Shape @Composable get() = MaterialTheme.shapes.medium
    val panel: Shape @Composable get() = MaterialTheme.shapes.large
    val field: Shape @Composable get() = MaterialTheme.shapes.small
}

/** Max fraction of screen height for the scrollable config panel. */
const val PLAN_CONFIG_MAX_HEIGHT_FRACTION = 0.42f
