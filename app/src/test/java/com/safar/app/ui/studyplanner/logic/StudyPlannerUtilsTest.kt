package com.safar.app.ui.studyplanner.logic

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
            - गणित
            _ बीजगणित
            > रैखिक समीकरण
            > रैखिक समीकरण
            
            _  
            >   प्रतिशत  
            random line without token
        """.trimIndent()

        val parsed = parseBulkSubjectsFromTxt(text).getOrThrow()
        assertEquals(1, parsed.size)
        assertEquals("गणित", parsed.first().subjectName)
        assertEquals(2, parsed.first().chapters.size)
        assertTrue(parsed.first().chapters.sumOf { it.topics.size } >= 2)
    }

    @Test
    fun `parseBulkSubjectsFromTxt supports topics without explicit subject or chapter`() {
        val text = """
            > Topic A
            > Topic B
        """.trimIndent()
        val parsed = parseBulkSubjectsFromTxt(text).getOrThrow()
        assertEquals(1, parsed.size)
        assertEquals(1, parsed.first().chapters.size)
        assertEquals(2, parsed.first().chapters.first().topics.size)
    }
}

