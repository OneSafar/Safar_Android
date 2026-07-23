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
    fun `completion is effort-weighted but treats progress as done or not (partial hidden)`() {
        // Partial completion is hidden this release, so the half-done medium topic
        // counts as 0. big done (4 of 4) + medium not-done (0 of 2) + small todo
        // (0 of 1) = 4 / 7 points ≈ 57%.
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
                                StudyTopic("b", "B", TopicStatus.TODO, progressPercent = 50),
                                StudyTopic("c", "C", TopicStatus.TODO, size = TopicSize.SMALL),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(57, plan.rollup().plannerProgressPercent)
        assertEquals(57, plan.subjects.first().percentDone())
    }

    @Test
    fun `chapter with some but not all topics done is doing`() {
        // With partial completion and the "in progress" status both removed, DOING
        // means exactly one thing: started but not finished.
        val chapter = StudyChapter(
            topics = listOf(
                StudyTopic("one", "One", status = TopicStatus.DONE),
                StudyTopic("two", "Two", status = TopicStatus.TODO),
            ),
        )

        assertEquals(NodeState.DOING, chapter.progressState().state)
        assertEquals(1, chapter.progressState().finishedTopics)
    }

    @Test
    fun `stored partial value does not make a todo chapter doing`() {
        // A stray progressPercent left on a TODO topic must read as not started.
        val chapter = StudyChapter(
            topics = listOf(StudyTopic("stray", "Stray", status = TopicStatus.TODO, progressPercent = 40)),
        )

        assertEquals(NodeState.NOT_STARTED, chapter.progressState().state)
        assertEquals(0, chapter.progressState().percent)
    }

    @Test
    fun `all done chapter is finished`() {
        val chapter = StudyChapter(
            topics = listOf(
                StudyTopic("one", "One", status = TopicStatus.DONE),
                StudyTopic("two", "Two", status = TopicStatus.DONE),
            ),
        )

        assertEquals(NodeState.FINISHED, chapter.progressState().state)
        assertEquals(100, chapter.progressState().percent)
    }

    @Test
    fun `empty chapter is not started`() {
        assertEquals(NodeState.NOT_STARTED, StudyChapter().progressState().state)
    }

    @Test
    fun `node percent uses shared weighted completion formula`() {
        val chapter = StudyChapter(
            difficulty = com.safarparmar.app.domain.model.studyplanner.ChapterDifficulty.TOUGH,
            topics = listOf(
                StudyTopic("one", "One", status = TopicStatus.DONE, size = TopicSize.SMALL),
                StudyTopic("two", "Two", progressPercent = 50, size = TopicSize.BIG),
            ),
        )
        val expected = weightedCompletionPercent(chapter.topics.map { it to chapter })

        assertEquals(expected, chapter.progressState().percent)
    }

    @Test
    fun `course map prioritises late work and ignores partial progress for lateness`() {
        val plan = StudyPlan(
            subjects = listOf(
                StudySubject(
                    id = "subject",
                    chapters = listOf(
                        StudyChapter(
                            id = "chapter",
                            name = "Number System",
                            topics = listOf(
                                StudyTopic("late", "Late", plannedDate = "2026-07-22", progressPercent = 60),
                                StudyTopic("future", "Future", plannedDate = "2026-07-25", progressPercent = 80),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val nudge = plan.courseMapNudge(today = "2026-07-23")

        assertEquals(CourseMapNudgeKind.LATE, nudge.kind)
        assertEquals(1, nudge.topicCount)
        assertEquals("Number System", nudge.chapterName)
    }

    @Test
    fun `course map shows todays unfinished focus when nothing is late`() {
        val plan = StudyPlan(
            subjects = listOf(
                StudySubject(
                    id = "subject",
                    chapters = listOf(
                        StudyChapter(
                            id = "chapter",
                            name = "Percentage",
                            topics = listOf(
                                StudyTopic("today", "Today", plannedDate = "2026-07-23"),
                                StudyTopic("future", "Future", plannedDate = "2026-07-24"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(CourseMapNudgeKind.TODAY, plan.courseMapNudge("2026-07-23").kind)
    }

    @Test
    fun `course map reassures student when scheduled work is not late`() {
        val plan = StudyPlan(
            subjects = listOf(
                StudySubject(
                    chapters = listOf(
                        StudyChapter(topics = listOf(StudyTopic("future", "Future", plannedDate = "2026-07-24"))),
                    ),
                ),
            ),
        )

        assertEquals(CourseMapNudgeKind.ON_TRACK, plan.courseMapNudge("2026-07-23").kind)
    }

    @Test
    fun `course map asks for a schedule when no topic has a date`() {
        val plan = StudyPlan(
            subjects = listOf(StudySubject(chapters = listOf(StudyChapter(topics = listOf(StudyTopic()))))),
        )

        assertEquals(CourseMapNudgeKind.NEEDS_SCHEDULE, plan.courseMapNudge("2026-07-23").kind)
    }

    @Test
    fun `course map celebrates when all of todays work is finished`() {
        val plan = StudyPlan(
            subjects = listOf(
                StudySubject(
                    chapters = listOf(
                        StudyChapter(
                            topics = listOf(
                                StudyTopic("done", "Done", status = TopicStatus.DONE, plannedDate = "2026-07-23"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(CourseMapNudgeKind.TODAY_FINISHED, plan.courseMapNudge("2026-07-23").kind)
    }
}
