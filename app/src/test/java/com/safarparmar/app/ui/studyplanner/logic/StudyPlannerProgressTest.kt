package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.DailyTodo
import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicSize
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyPlannerProgressTest {
    @Test
    fun `daily todo completion does not inflate exam progress or stale cache`() {
        val today = todayKey()
        val plan = StudyPlan(
            dailyTodos = listOf(DailyTodo("daily-1", "Revise notes")),
            dailyTodoLogs = mapOf(today to listOf("daily-1")),
            subjects = listOf(
                StudySubject(
                    id = "subject",
                    name = "Subject",
                    chapters = listOf(
                        StudyChapter(
                            id = "chapter",
                            name = "Chapter",
                            topics = listOf(StudyTopic("topic", "Topic", TopicStatus.TODO)),
                        ),
                    ),
                ),
            ),
            progress = PlanProgress(overallProgressPercent = 10),
        )

        val result = plan.rollup()

        assertEquals(100, result.dailyTodoProgressPercent)
        assertEquals(0, result.plannerProgressPercent)
        assertEquals(0, result.overallProgressPercent)
    }

    @Test
    fun `completion is effort-weighted and fractional, matching the server rollup`() {
        // big done (4 of 4 pts) + medium half-done (1 of 2 pts) + small todo (0 of 1 pt)
        // = 5 / 7 points ≈ 71%.
        val plan = StudyPlan(
            subjects = listOf(
                StudySubject(
                    id = "subject",
                    name = "Subject",
                    chapters = listOf(
                        StudyChapter(
                            id = "chapter",
                            name = "Chapter",
                            topics = listOf(
                                StudyTopic("a", "A", TopicStatus.DONE, size = TopicSize.BIG),
                                StudyTopic("b", "B", TopicStatus.IN_PROGRESS, progressPercent = 50),
                                StudyTopic("c", "C", TopicStatus.TODO, size = TopicSize.SMALL),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(71, plan.rollup().plannerProgressPercent)
        assertEquals(71, plan.subjects.first().percentDone())
    }
}
