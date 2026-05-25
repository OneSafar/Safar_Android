package com.safar.app.ui.dashboard

import com.safar.app.domain.model.studyplanner.CalendarTopicItem
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.TopicStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DashboardStudyPlanStateTest {

    private val today = LocalDate.of(2026, 5, 20)

    @Test
    fun `no plan returns no active plan state`() {
        val state = buildDashboardStudyPlanState(null, emptyMap(), today)

        assertEquals(DashboardStudyPlanStatus.NO_ACTIVE_PLAN, state.status)
    }

    @Test
    fun `active plan with no topics today returns no topics state`() {
        val state = buildDashboardStudyPlanState(StudyPlan(id = "p1", title = "NEET", examDate = "2026-06-01"), emptyMap(), today)

        assertEquals(DashboardStudyPlanStatus.NO_TOPICS_TODAY, state.status)
        assertEquals("12 days left", state.daysLeftText)
    }

    @Test
    fun `active plan with one topic returns visible progress`() {
        val state = buildDashboardStudyPlanState(
            StudyPlan(id = "p1", title = "NEET"),
            mapOf(today.toString() to listOf(topic("t1", "Physics", TopicStatus.TODO))),
            today,
        )

        assertEquals(DashboardStudyPlanStatus.HAS_TOPICS, state.status)
        assertEquals(0, state.doneCount)
        assertEquals(1, state.totalCount)
        assertEquals(1, state.visibleTopics.size)
    }

    @Test
    fun `active plan with more than three topics summarizes overflow`() {
        val topics = (1..5).map { topic("t$it", "Topic $it", if (it <= 2) TopicStatus.DONE else TopicStatus.TODO) }
        val state = buildDashboardStudyPlanState(StudyPlan(id = "p1", title = "JEE"), mapOf(today.toString() to topics), today)

        assertEquals(DashboardStudyPlanStatus.HAS_TOPICS, state.status)
        assertEquals(2, state.doneCount)
        assertEquals(5, state.totalCount)
        assertEquals(3, state.visibleTopics.size)
        assertEquals(2, state.moreCount)
    }

    @Test
    fun `all topics done still shows today's plan`() {
        val topics = listOf(topic("t1", "Chemistry", TopicStatus.DONE), topic("t2", "Maths", TopicStatus.DONE))
        val state = buildDashboardStudyPlanState(StudyPlan(id = "p1", title = "Boards"), mapOf(today.toString() to topics), today)

        assertEquals(DashboardStudyPlanStatus.HAS_TOPICS, state.status)
        assertEquals(2, state.doneCount)
        assertEquals(2, state.totalCount)
    }

    private fun topic(id: String, name: String, status: TopicStatus) = CalendarTopicItem(
        topicId = id,
        topicName = name,
        chapterName = "Chapter",
        subjectName = "Subject",
        status = status,
    )
}
