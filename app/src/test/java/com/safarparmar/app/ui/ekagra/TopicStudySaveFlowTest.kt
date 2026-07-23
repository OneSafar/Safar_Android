package com.safarparmar.app.ui.ekagra

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicStudySaveFlowTest {
    @Test
    fun `timer saves only the time that was studied`() {
        val pending = PendingEndedEkagraSession(
            sessionId = "local-1",
            totalSeconds = 30 * 60,
            secondsLeft = 8 * 60,
            mode = "focus",
            startedAt = null,
            topicId = "topic-1",
            planId = "plan-1",
            topicTitle = "Divisibility Rules",
        )

        assertEquals(22 * 60, topicStudyActualSeconds(pending))
    }

    @Test
    fun `stopwatch saves its full counted time`() {
        val pending = PendingEndedEkagraSession(
            sessionId = "local-2",
            totalSeconds = 0,
            secondsLeft = 12 * 60 + 8,
            mode = "stopwatch",
            startedAt = null,
        )

        assertEquals(12 * 60 + 8, topicStudyActualSeconds(pending))
    }

    @Test
    fun `study time uses simple readable words`() {
        assertEquals("42 min", formatTopicStudyTime(42 * 60))
        assertEquals("8 min 5 sec", formatTopicStudyTime(8 * 60 + 5))
        assertEquals("35 sec", formatTopicStudyTime(35))
    }
}
