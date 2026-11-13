package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SyllabusReadinessTest {
    private val today = "2026-11-11"

    private fun plan(examDate: String?, vararg topics: StudyTopic, chapters: List<StudyChapter>? = null) = StudyPlan(
        examDate = examDate,
        subjects = listOf(
            StudySubject(
                id = "s",
                name = "Maths",
                chapters = chapters ?: listOf(StudyChapter(id = "c", name = "Algebra", topics = topics.toList())),
            ),
        ),
    )

    private fun StudyPlan.status(
        armed: Boolean = false,
        ratings: Boolean = false,
        forecast: String? = null,
    ) = syllabusReadiness(today, rebuildArmed = armed, ratingsChanged = ratings, velocityForecastDate = forecast)

    private val planned = StudyTopic("p", "Planned", plannedDate = "2026-11-20")
    private val unscheduled = StudyTopic("u", "Unscheduled")
    private val late = StudyTopic("l", "Late", plannedDate = "2026-11-01")

    @Test
    fun `everything studied is finished, even with an old exam date`() {
        val done = StudyTopic("d", "Done", TopicStatus.DONE)
        assertEquals(SyllabusReadinessStatus.Finished, plan("2026-01-01", done).status(armed = true))
    }

    @Test
    fun `an armed rebuild outranks every other problem`() {
        assertEquals(SyllabusReadinessStatus.ReorderThenCreateDates, plan("2026-01-01", unscheduled).status(armed = true))
    }

    @Test
    fun `exam date problems come before scheduling problems`() {
        assertEquals(SyllabusReadinessStatus.ExamDatePassed, plan("2026-11-10", unscheduled, late).status())
        assertEquals(SyllabusReadinessStatus.NoExamDate, plan(null, unscheduled).status())
        // An exam today has not passed yet.
        assertEquals(SyllabusReadinessStatus.NeedsDates(1), plan(today, unscheduled).status())
    }

    @Test
    fun `unscheduled topics come before ratings and late work`() {
        assertEquals(SyllabusReadinessStatus.NeedsDates(1), plan("2026-12-31", unscheduled, late).status(ratings = true))
    }

    @Test
    fun `ratings come before late work`() {
        assertEquals(SyllabusReadinessStatus.RatingsChanged, plan("2026-12-31", late, planned).status(ratings = true))
    }

    @Test
    fun `late counts every overdue topic and points at the worst chapter`() {
        val chapters = listOf(
            StudyChapter(id = "c1", name = "Algebra", topics = listOf(late.copy(id = "a"))),
            StudyChapter(id = "c2", name = "Geometry", topics = listOf(late.copy(id = "b"), late.copy(id = "c"))),
        )
        assertEquals(
            SyllabusReadinessStatus.Late(lateCount = 3, subjectId = "s", chapterId = "c2", chapterName = "Geometry"),
            plan("2026-12-31", chapters = chapters).status(),
        )
    }

    @Test
    fun `forecast after the exam warns with the gap in days`() {
        assertEquals(
            SyllabusReadinessStatus.FinishAfterExam("2027-01-10", daysAfterExam = 10),
            plan("2026-12-31", planned).status(forecast = "2027-01-10"),
        )
    }

    @Test
    fun `on track carries the forecast when there is one`() {
        assertEquals(SyllabusReadinessStatus.OnTrack("2026-12-01"), plan("2026-12-31", planned).status(forecast = "2026-12-01"))
        assertEquals(SyllabusReadinessStatus.OnTrack(null), plan("2026-12-31", planned).status())
    }
}

class ChapterFinishedTest {
    private fun plan(id: String, vararg chapters: StudyChapter) =
        StudyPlan(id = id, subjects = listOf(StudySubject(id = "s", chapters = chapters.toList())))
    private fun topic(id: String, done: Boolean) = StudyTopic(id, id, if (done) TopicStatus.DONE else TopicStatus.TODO)
    private val other = StudyChapter(id = "c2", name = "Geometry", topics = listOf(topic("g", true)))

    @Test
    fun `ticking the last open topic finishes the chapter`() {
        val before = plan("p", StudyChapter(id = "c1", name = "Algebra", topics = listOf(topic("a", true), topic("b", false))), other)
        val after = plan("p", StudyChapter(id = "c1", name = "Algebra", topics = listOf(topic("a", true), topic("b", true))), other)
        assertEquals(ChapterFinished("Algebra", finishedChapters = 2, totalChapters = 2), chapterFinishedBetween(before, after))
    }

    @Test
    fun `deleting the last open topic is not a finish`() {
        val before = plan("p", StudyChapter(id = "c1", name = "Algebra", topics = listOf(topic("a", true), topic("b", false))))
        val after = plan("p", StudyChapter(id = "c1", name = "Algebra", topics = listOf(topic("a", true))))
        assertEquals(null, chapterFinishedBetween(before, after))
    }

    @Test
    fun `switching plans or first load is not a finish`() {
        val done = StudyChapter(id = "c1", name = "Algebra", topics = listOf(topic("a", true)))
        val open = StudyChapter(id = "c1", name = "Algebra", topics = listOf(topic("a", false)))
        assertEquals(null, chapterFinishedBetween(null, plan("p", done)))
        assertEquals(null, chapterFinishedBetween(plan("p", open), plan("q", done)))
    }
}

class SyllabusSearchTest {
    private val subjects = listOf(
        StudySubject(id = "r", name = "Reasoning", chapters = listOf(
            StudyChapter(id = "lr", name = "Logical Reasoning", topics = listOf(StudyTopic("s", "Syllogism"), StudyTopic("p", "Puzzle"))),
        )),
        StudySubject(id = "e", name = "English", chapters = listOf(
            StudyChapter(id = "g", name = "Grammar", topics = listOf(StudyTopic("syn", "Synonyms"))),
        )),
    )

    @Test
    fun `a topic match is listed directly with its subject and chapter`() {
        val results = subjects.searchSyllabus("syllog")
        assertEquals(listOf("Syllogism"), results.topics.map { it.topic?.name })
        assertEquals("Reasoning", results.topics.single().subject.name)
        assertEquals("Logical Reasoning", results.topics.single().chapter.name)
        assertEquals(emptyList<StudySubject>(), results.subjects)
    }

    @Test
    fun `each level matches by its own name`() {
        val results = subjects.searchSyllabus("reason")
        assertEquals(listOf("Reasoning"), results.subjects.map { it.name })
        assertEquals(listOf("Logical Reasoning"), results.chapters.map { it.chapter.name })
        assertEquals(0, results.topics.size)
    }

    @Test
    fun `topics are capped and flagged`() {
        val results = subjects.searchSyllabus("s", topicLimit = 1)
        assertEquals(1, results.topics.size)
        assertEquals(true, results.topicsTruncated)
        assertEquals(true, subjects.searchSyllabus("   ").isEmpty)
    }
}
