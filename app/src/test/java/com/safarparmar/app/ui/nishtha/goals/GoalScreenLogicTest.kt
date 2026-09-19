package com.safarparmar.app.ui.nishtha.goals

import com.safarparmar.app.domain.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalScreenLogicTest {
    @Test
    fun `future repeating goal opens edit form with its schedule enabled`() {
        val goal = Goal(goalKind = "repeat", scheduledDate = "2026-09-20")

        assertEquals("scheduled", goal.editScheduleKind(todayKey = "2026-09-13"))
    }

    @Test
    fun `today repeating goal remains a today goal in edit form`() {
        val goal = Goal(goalKind = "repeat", scheduledDate = "2026-09-13")

        assertEquals("today", goal.editScheduleKind(todayKey = "2026-09-13"))
    }

    @Test
    fun `study time quick add is capped at 99 hours 59 minutes`() {
        assertEquals(99 to 59, addStudyTime(hours = 99, minutes = 30, addedMinutes = 120))
    }

    @Test
    fun `study time quick add carries minutes into hours`() {
        assertEquals(2 to 15, addStudyTime(hours = 1, minutes = 45, addedMinutes = 30))
    }
}
