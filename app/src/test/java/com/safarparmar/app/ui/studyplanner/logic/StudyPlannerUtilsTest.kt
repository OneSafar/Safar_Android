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
    fun `parseBulkSubjectsFromTxt handles unicode malformed and whitespace`() {
        val text = """
            विषय: गणित
            अध्याय: बीजगणित
            टॉपिक: रैखिक समीकरण
            टॉपिक: रैखिक समीकरण
            
            अध्याय:   
            टॉपिक: प्रतिशत  
            random line without token
        """.trimIndent()

        val parsed = parseBulkSubjectsFromTxt(text).getOrThrow()
        assertEquals(1, parsed.size)
        assertEquals("गणित", parsed.first().subjectName)
        assertEquals(2, parsed.first().chapters.size)
        assertTrue(parsed.first().chapters.sumOf { it.topics.size } >= 2)
    }

    @Test
    fun `parseBulkSubjectsFromTxt supports labeled topics without explicit subject or chapter`() {
        val text = """
            Topic: Topic A
            Topic: Topic B
        """.trimIndent()
        val parsed = parseBulkSubjectsFromTxt(text).getOrThrow()
        assertEquals(1, parsed.size)
        assertEquals(1, parsed.first().chapters.size)
        assertEquals(2, parsed.first().chapters.first().topics.size)
    }

    @Test
    fun `parseBulkSubjectsFromTxt rejects old symbol format`() {
        val parsed = parseBulkSubjectsFromTxt(
            """
                - Maths
                _ Algebra
                > Linear equations
            """.trimIndent()
        )

        assertTrue(parsed.isFailure)
        assertEquals("Use labels instead: Subject:, Chapter:, and Topic:.", parsed.exceptionOrNull()?.message)
    }

    @Test
    fun `parseBulkSubjectsFromTxt supports readable labels and comma separated topic labels`() {
        val text = """
            Subject: Maths
            Chapter: Algebra
            Topics: Linear equations, Quadratic equations
            Topic: Polynomials
            Subject: Science
            Unit: Physics
            Topic: Motion
        """.trimIndent()

        val parsed = parseBulkSubjectsFromTxt(text).getOrThrow()

        assertEquals(2, parsed.size)
        assertEquals("Maths", parsed[0].subjectName)
        assertEquals("Algebra", parsed[0].chapters.first().chapterName)
        assertEquals(
            listOf("Linear equations", "Quadratic equations", "Polynomials"),
            parsed[0].chapters.first().topics,
        )
        assertEquals("Science", parsed[1].subjectName)
        assertEquals("Physics", parsed[1].chapters.first().chapterName)
        assertEquals(listOf("Motion"), parsed[1].chapters.first().topics)
    }
}
