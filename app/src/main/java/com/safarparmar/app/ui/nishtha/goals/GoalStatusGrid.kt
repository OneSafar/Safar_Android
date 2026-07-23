package com.safarparmar.app.ui.nishtha.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.util.IstDateUtils

@Composable
internal fun StatusGrid(goals: List<Goal>, ekagraAnalytics: com.safarparmar.app.domain.model.EkagraAnalyticsStats) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val pending = standardGoals.filter {
        !it.completed && it.lifecycleStatus !in listOf("abandoned", "rolled_over") &&
            !(it.lifecycleStatus == "missed" && it.nextInstanceCreated) &&
            !it.isDormant(todayKey)
    }
    val scheduled = standardGoals.filter { !it.completed && it.isDormant(todayKey) }
    val manualCompletedGoals = standardGoals.filter { it.isCompletedForStats() && !it.completedViaFocus }
    val doneToday = manualCompletedGoals.count { it.completedDateKey() == todayKey }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            StatInfoCard(
                "Done Today",
                doneToday.toString(),
                "Manual goals completed on today's date.",
                Modifier.weight(1f),
                GoalsFlatColors.Done,
            )
            StatInfoCard(
                "Open Now",
                pending.size.toString(),
                "Active manual goals available to work on right now.",
                Modifier.weight(1f),
                GoalsFlatColors.Repeat,
            )
        }
        Spacer(Modifier.height(0.dp))
        PlanHairline(alpha = 0.4f)
        StatInfoCard(
            "Scheduled Ahead",
            scheduled.size.toString(),
            "Future goals parked until their scheduled day arrives.",
            Modifier.fillMaxWidth(),
            GoalsFlatColors.Scheduled,
        )
    }
}
