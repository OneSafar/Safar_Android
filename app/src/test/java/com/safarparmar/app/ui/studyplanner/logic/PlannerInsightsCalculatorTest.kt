package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.RevisionCompletion
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerInsightsCalculatorTest {

    @Test
    fun `finish date uses topic size for both finished and remaining work`() {
        val plan = planWith(
            StudyTopic(
                id = "finished-big",
                name = "Finished big topic",
                status = TopicStatus.DONE,
                completedDate = "2026-07-23",
                size = TopicSize.BIG,
            ),
            StudyTopic(
                id = "remaining-big",
                name = "Remaining big topic",
                status = TopicStatus.TODO,
                size = TopicSize.BIG,
            ),
        )

        val result = PlannerInsightsCalculator.compute(
            plan = plan,
            calendar = emptyMap(),
            analytics = null,
            todayIso = "2026-07-23",
        )

        assertEquals(2f / 14f, result.summary.recentTopicsPerStudyDay!!, 0.0001f)
        assertEquals("2026-08-05", result.summary.velocityForecastCompletionDate)
    }

    @Test
    fun `finished revisions count as study on each day`() {
        val plan = planWith(
            StudyTopic(
                id = "revision-topic",
                name = "Revision topic",
                status = TopicStatus.REVISION_NEEDED,
                revisionReminderDates = listOf("2026-07-30"),
                revisionCompletedDates = listOf("2026-07-22", "2026-07-23"),
                revisionCompletionLog = listOf(
                    RevisionCompletion("2026-07-22", "2026-07-22"),
                    RevisionCompletion("2026-07-23", "2026-07-23"),
                ),
                size = TopicSize.MEDIUM,
            ),
        )

        val result = PlannerInsightsCalculator.compute(
            plan = plan,
            calendar = emptyMap(),
            analytics = null,
            todayIso = "2026-07-23",
        )

        assertEquals(2, result.consistency.studyStreak)
        assertEquals(1, result.consistency.heatmap.first { it.date == "2026-07-22" }.count)
        assertEquals(1, result.consistency.heatmap.first { it.date == "2026-07-23" }.count)
        assertEquals(2f / 14f, result.summary.recentTopicsPerStudyDay!!, 0.0001f)
    }

    @Test
    fun `last revision is not counted twice when it also finishes the topic`() {
        val plan = planWith(
            StudyTopic(
                id = "finished-revision-topic",
                name = "Finished revision topic",
                status = TopicStatus.DONE,
                completedDate = "2026-07-23",
                revisionCompletedDates = listOf("2026-07-22", "2026-07-23"),
                revisionCompletionLog = listOf(
                    RevisionCompletion("2026-07-22", "2026-07-22"),
                    RevisionCompletion("2026-07-23", "2026-07-23"),
                ),
                size = TopicSize.MEDIUM,
            ),
        )

        val result = PlannerInsightsCalculator.compute(
            plan = plan,
            calendar = emptyMap(),
            analytics = null,
            todayIso = "2026-07-23",
        )

        assertEquals(1, result.consistency.heatmap.first { it.date == "2026-07-23" }.count)
        assertEquals(2f / 14f, result.summary.recentTopicsPerStudyDay!!, 0.0001f)
    }

    @Test
    fun `late revision counts on the day it was actually finished`() {
        val plan = planWith(
            StudyTopic(
                id = "late-revision",
                name = "Late revision",
                status = TopicStatus.REVISION_NEEDED,
                revisionReminderDates = listOf("2026-07-30"),
                revisionCompletedDates = listOf("2026-07-20"),
                revisionCompletionLog = listOf(
                    RevisionCompletion(
                        sessionDate = "2026-07-20",
                        completedDate = "2026-07-23",
                    ),
                ),
            ),
        )

        val result = PlannerInsightsCalculator.compute(
            plan = plan,
            calendar = emptyMap(),
            analytics = null,
            todayIso = "2026-07-23",
        )

        assertEquals(0, result.consistency.heatmap.first { it.date == "2026-07-20" }.count)
        assertEquals(1, result.consistency.heatmap.first { it.date == "2026-07-23" }.count)
    }

    @Test
    fun `old plan without revision log opens Progress safely`() {
        val plan = planWith(
            StudyTopic(
                id = "old-plan-topic",
                name = "Old plan topic",
                status = TopicStatus.DONE,
                completedDate = "2026-07-23",
                revisionCompletedDates = null,
                revisionCompletionLog = null,
            ),
        )

        val result = PlannerInsightsCalculator.compute(
            plan = plan,
            calendar = emptyMap(),
            analytics = null,
            todayIso = "2026-07-23",
        )

        assertEquals(1, result.consistency.heatmap.first { it.date == "2026-07-23" }.count)
    }

    private fun planWith(vararg topics: StudyTopic) = StudyPlan(
        id = "plan",
        examDate = "2026-09-30",
        dailyGoal = 1,
        subjects = listOf(
            StudySubject(
                id = "subject",
                name = "Subject",
                chapters = listOf(
                    StudyChapter(
                        id = "chapter",
                        name = "Chapter",
                        topics = topics.toList(),
                    ),
                ),
            ),
        ),
    )
}
