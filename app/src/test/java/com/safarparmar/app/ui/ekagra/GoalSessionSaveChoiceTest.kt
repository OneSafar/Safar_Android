package com.safarparmar.app.ui.ekagra

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSessionSaveChoiceTest {
    @Test
    fun `keep open links time without finishing the goal`() {
        assertFalse(GoalSessionSaveChoice.KEEP_GOAL_OPEN.marksGoalDone)
    }

    @Test
    fun `mark done explicitly finishes the goal`() {
        assertTrue(GoalSessionSaveChoice.MARK_GOAL_DONE.marksGoalDone)
    }
}
