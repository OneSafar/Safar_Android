package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.*
import org.junit.Assert.*
import org.junit.Test

class PlannerDailyAgendaTest {
    private val today = "2026-09-05"

    @Test fun `completed review remains checked after next date advances`() {
        val topic = StudyTopic(status = TopicStatus.REVISION_NEEDED, plannedDate = "2026-09-12",
            revisionCompletedDates = listOf(today), revisionReminderDates = listOf("2026-09-12"))
        assertTrue(topic.isVisibleToday(today))
        assertTrue(topic.isCompletedToday(today))
        assertEquals(today, topic.completedReviewForDay(today))
    }

    @Test fun `late review uses actual completion day and undoes original appointment`() {
        val topic = StudyTopic(status = TopicStatus.REVISION_NEEDED, plannedDate = "2026-09-12",
            revisionCompletedDates = listOf("2026-09-03"),
            revisionCompletionLog = listOf(RevisionCompletion("2026-09-03", today)))
        assertTrue(topic.isVisibleToday(today))
        assertTrue(topic.isCompletedToday(today))
        assertEquals("2026-09-03", topic.completedReviewForDay(today))
    }

    @Test fun `legacy nullable revision fields are safe`() {
        val topic = StudyTopic(plannedDate = today, revisionCompletedDates = null, revisionCompletionLog = null)
        assertTrue(topic.isVisibleToday(today))
        assertFalse(topic.isCompletedToday(today))
    }

    @Test fun `future work does not leak into today`() {
        val topic = StudyTopic(status = TopicStatus.REVISION_NEEDED, plannedDate = "2026-09-12",
            revisionCompletedDates = listOf("2026-09-04"), revisionReminderDates = listOf("2026-09-12"))
        assertFalse(topic.isVisibleToday(today))
        assertFalse(topic.isCompletedToday(today))
    }

    @Test fun `ordinary completion stays visible with reversible status`() {
        val topic = StudyTopic(plannedDate = today, status = TopicStatus.DONE)
        assertTrue(topic.isVisibleToday(today))
        assertTrue(topic.isCompletedToday(today))
        assertFalse(topic.copy(status = TopicStatus.TODO).isCompletedToday(today))
    }

    @Test fun `stopped work with no date is still missed and not unscheduled`() {
        val topic = StudyTopic(plannedDate = null, missedReason = "done_for_day", originalPlannedDate = today)
        assertTrue(topic.isMissed(today))
        assertFalse(topic.isUnscheduled())
        assertFalse(topic.isVisibleToday(today))
    }

    @Test fun `landing restores valid exam and never guesses among several`() {
        val plans = listOf(StudyPlan(id = "a"), StudyPlan(id = "b"))
        assertEquals("b", plannerLandingPlan(plans, "b")?.id)
        assertNull(plannerLandingPlan(plans, "removed-or-another-user"))
        assertNull(plannerLandingPlan(emptyList(), "a"))
        assertEquals("a", plannerLandingPlan(plans.take(1), null)?.id)
    }
    @Test fun `stopping early keeps missed work in denominator and never celebrates completion`() {
        val topics = listOf(
            StudyTopic(status = TopicStatus.DONE, plannedDate = today),
            StudyTopic(missedReason = "done_for_day", originalPlannedDate = today),
        )
        val summary = dailyAgendaSummary(topics, today, stopped = true)
        assertEquals(1, summary.completed)
        assertEquals(2, summary.total)
        assertFalse(summary.isComplete)
        assertFalse(dailyAgendaSummary(topics.take(1), today, stopped = true).isComplete)
        assertTrue(dailyAgendaSummary(topics.take(1), today, stopped = false).isComplete)
    }
}
