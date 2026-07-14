package com.safarparmar.app.ui.studyplanner.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StudyPlannerUtilsTest {

    @Test
    fun `daysUntil returns null for missing date`() {
        assertEquals(null, daysUntil(null, LocalDate.of(2026, 1, 1)))
        assertEquals(null, daysUntil("", LocalDate.of(2026, 1, 1)))
        assertEquals(null, daysUntil("not-a-date", LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `daysUntil handles today future and past`() {
        val today = LocalDate.of(2026, 5, 20)
        assertEquals(0L, daysUntil("2026-05-20", today))
        assertEquals(12L, daysUntil("2026-06-01", today))
        assertEquals(-1L, daysUntil("2026-05-19", today))
    }

    @Test
    fun `plannerExamSubtitle has reliability-safe fallback labels`() {
        val today = LocalDate.of(2026, 5, 20)
        assertEquals("Set exam date", plannerExamSubtitle(null, today))
        assertTrue(plannerExamSubtitle("2026-05-19", today).startsWith("Exam passed"))
        assertTrue(plannerExamSubtitle("2026-05-20", today).startsWith("Exam today!"))
        assertTrue(plannerExamSubtitle("2026-05-21", today).startsWith("1 days left").not())
    }

    @Test
    fun `validateSyllabusNodeName blocks blank and whitespace-only names`() {
        assertEquals("Please type a name first", validateSyllabusNodeName(""))
        assertEquals("Please type a name first", validateSyllabusNodeName("   "))
        assertEquals(null, validateSyllabusNodeName("Physics"))
    }

    @Test
    fun `findDuplicateSiblingName is case-insensitive and trims`() {
        val siblings = listOf("Physics", "Chemistry")
        assertTrue(findDuplicateSiblingName("physics", siblings))
        assertTrue(findDuplicateSiblingName(" PHYSICS ", siblings))
        assertTrue(findDuplicateSiblingName("physics", siblings).let { it })
        assertEquals(false, findDuplicateSiblingName("Biology", siblings))
    }

    @Test
    fun `deleteImpact computes chapter and topic counts including scheduled topics`() {
        val topicA = com.safarparmar.app.domain.model.studyplanner.StudyTopic(id = "t1", name = "A", plannedDate = "2026-05-20")
        val topicB = com.safarparmar.app.domain.model.studyplanner.StudyTopic(id = "t2", name = "B", plannedDate = null)
        val chapter = com.safarparmar.app.domain.model.studyplanner.StudyChapter(id = "c1", name = "Chapter 1", topics = listOf(topicA, topicB))
        val subject = com.safarparmar.app.domain.model.studyplanner.StudySubject(id = "s1", name = "Physics", chapters = listOf(chapter))

        val subjectImpact = subject.deleteImpact()
        assertEquals(1, subjectImpact.chapterCount)
        assertEquals(2, subjectImpact.topicCount)
        assertEquals(1, subjectImpact.scheduledTopicCount)

        val chapterImpact = chapter.deleteImpact()
        assertEquals(2, chapterImpact.topicCount)
        assertEquals(1, chapterImpact.scheduledTopicCount)
    }
}
