package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.DailyTodo
import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
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
}
