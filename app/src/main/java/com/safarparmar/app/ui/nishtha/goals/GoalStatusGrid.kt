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
import com.safarparmar.app.util.isGoalCompleted
import com.safarparmar.app.util.isMissedGoal
import com.safarparmar.app.util.isTodayGoal
import com.safarparmar.app.util.isUpcomingGoal

@Composable
internal fun StatusGrid(goals: List<Goal>, ekagraAnalytics: com.safarparmar.app.domain.model.EkagraAnalyticsStats) {
    val todayKey = IstDateUtils.todayKey()
    val standardGoals = goals.filter { it.source != "ekagra" }
    val pending = standardGoals.filter { it.isTodayGoal(todayKey) }
    val scheduled = standardGoals.filter { it.isUpcomingGoal(todayKey) }
    val missed = standardGoals.filter { it.isMissedGoal(todayKey) }
    val doneToday = standardGoals.count { it.isGoalCompleted() && it.anchorDateKey() == todayKey }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            StatInfoCard(
                "Done Today",
                doneToday.toString(),
                "Goals assigned today that are complete.",
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
            "Missed",
            missed.size.toString(),
            "Open goals whose assigned day has passed.",
            Modifier.fillMaxWidth(),
            GoalsFlatColors.Danger,
        )
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
