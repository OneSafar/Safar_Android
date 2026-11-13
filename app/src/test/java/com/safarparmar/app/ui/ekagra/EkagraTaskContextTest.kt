package com.safarparmar.app.ui.ekagra

import org.junit.Assert.*
import org.junit.Test

class EkagraTaskContextTest {
    private val plannerTask = EkagraTaskContext(
        topicId = "topic-1", planId = "plan-1", topicTitle = "Trigonometric Ratios",
    )

    @Test fun `idle planner launch keeps its selected task`() {
        assertEquals(plannerTask, resolveEkagraTaskContext(plannerTask, null))
    }

    @Test fun `reopening plain Ekagra restores the active planner task`() {
        assertEquals(plannerTask, resolveEkagraTaskContext(EkagraTaskContext(), plannerTask))
    }

    @Test fun `another planner task cannot steal an active sessions study time`() {
        val anotherTask = plannerTask.copy(topicId = "topic-2", topicTitle = "Divisibility")
        assertFalse(anotherTask.hasSameLink(plannerTask))
        assertEquals(plannerTask, resolveEkagraTaskContext(anotherTask, plannerTask))
        assertFalse(plannerTask.copy(planId = "another-plan").hasSameLink(plannerTask))
    }

    @Test fun `a planner launch cannot retag an unlinked running session`() {
        val running = EkagraTaskContext(taskTitle = "Reading")
        assertEquals(running, resolveEkagraTaskContext(plannerTask, running))
    }

    @Test fun `completed timer keeps planner link and precise time for completion choice`() {
        val completed = completedSession().toEndedPlannerSession()
        assertEquals("topic-1", completed.topicId)
        assertEquals("plan-1", completed.planId)
        assertEquals("Trigonometric Ratios", completed.topicTitle)
        assertEquals(1501, topicStudyActualSeconds(completed))
        assertEquals("local-1", completed.sessionId)
    }

    @Test fun `completed stopwatch keeps full elapsed time for completion choice`() {
        val completed = completedSession().copy(mode = "stopwatch", plannedDurationMinutes = 0)
            .toEndedPlannerSession()
        assertEquals(1501, topicStudyActualSeconds(completed))
    }

    private fun completedSession() = PendingEkagraSessionSave(
        clientSessionId = "local-1", mode = "Timer", startedAt = "2026-11-04T08:00:00Z",
        endedAt = "2026-11-04T08:25:01Z", plannedDurationMinutes = 26,
        actualDurationMinutes = 25, actualDurationSeconds = 1501,
        goalId = null, goalTitle = null, topicId = "topic-1", planId = "plan-1",
        topicTitle = "Trigonometric Ratios", taskTitle = "Trigonometric Ratios", shieldEnabled = false,
    )
}
