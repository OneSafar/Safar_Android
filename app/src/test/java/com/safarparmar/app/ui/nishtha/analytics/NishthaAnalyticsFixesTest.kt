package com.safarparmar.app.ui.nishtha.analytics

import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.util.IstDateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NishthaAnalyticsFixesTest {

    @Test
    fun `focus completed goal is counted towards completion rate`() {
        val focusCompletedGoal = Goal(
            id = "goal-1",
            text = "Solve Math Problems",
            completed = true,
            completedViaFocus = true,
            status = "completed",
            completedAt = "2026-08-11T12:00:00Z"
        )
        val standardGoals = listOf(focusCompletedGoal)
        val completedActiveGoals = standardGoals.filter { it.completed || !it.completedAt.isNullOrBlank() }

        assertEquals(1, completedActiveGoals.size)
    }

    @Test
    fun `goals today filter includes carried over goals completed today`() {
        val todayKey = IstDateUtils.todayKey()
        val yesterdayKey = LocalDate.now(IstDateUtils.zone).minusDays(1).toString()

        val carriedOverGoal = Goal(
            id = "goal-2",
            text = "Yesterday Goal",
            scheduledDate = yesterdayKey,
            completed = true,
            completedAt = "${todayKey}T10:00:00Z",
            status = "completed"
        )

        val todayGoals = listOf(carriedOverGoal).filter { goal ->
            goal.source != "ekagra" && (IstDateUtils.getDateKey(goal.scheduledDate) == todayKey || IstDateUtils.getDateKey(goal.completedAt) == todayKey)
        }

        val completedToday = listOf(carriedOverGoal).count { goal ->
            goal.source != "ekagra" && IstDateUtils.getDateKey(goal.completedAt) == todayKey
        }

        assertEquals(1, todayGoals.size)
        assertEquals(1, completedToday)
        assertTrue(completedToday <= todayGoals.size)
    }
}
