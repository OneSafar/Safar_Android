package com.safarparmar.app.util

import com.safarparmar.app.domain.model.Goal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalHistoryVisibilityTest {
    @Test fun `goal completed by Ekagra stays visible in Goals`() {
        val goal = Goal(source = "ekagra", completedViaFocus = true, completed = true)
        assertTrue(goal.isVisibleInGoals() && goal.isGoalCompleted())
    }

    @Test fun `standalone Ekagra session is not a completed goal`() {
        assertFalse(Goal(source = "ekagra", completed = true).isVisibleInGoals())
    }

    @Test fun `keep open does not imply goal completion`() {
        val goal = Goal(source = "manual", completed = false)
        assertTrue(goal.isVisibleInGoals())
        assertFalse(goal.isGoalCompleted())
    }

    @Test fun `reopening a focus completed goal keeps it visible but outside Completed`() {
        val goal = Goal(source = "ekagra", completedViaFocus = true, completed = false)
        assertTrue(goal.isVisibleInGoals())
        assertFalse(goal.isGoalCompleted())
    }
}
