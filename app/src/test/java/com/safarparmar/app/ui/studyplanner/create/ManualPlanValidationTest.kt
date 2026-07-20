package com.safarparmar.app.ui.studyplanner.create

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualPlanValidationTest {
    @Test
    fun `rejects an empty chapter even when another chapter has a topic`() {
        val subjects = listOf(
            DraftSubject(
                name = "Physics",
                chapters = listOf(
                    DraftChapter(name = "Motion", topics = listOf(DraftTopic(name = "Velocity"))),
                    DraftChapter(name = "Gravity"),
                ),
            ),
        )

        assertEquals("Add a topic to Physics › Gravity.", validateManualSubjects(subjects))
    }

    @Test
    fun `rejects case insensitive duplicate names used by ordering maps`() {
        val subjects = listOf(
            completeSubject("Physics"),
            completeSubject(" physics "),
        )

        assertEquals(
            "Subject names must be unique: \"Physics\" is repeated.",
            validateManualSubjects(subjects),
        )
    }

    @Test
    fun `accepts a complete unique tree`() {
        assertNull(validateManualSubjects(listOf(completeSubject("Physics"))))
    }

    @Test
    fun `request conversion defensively removes empty branches and trims names`() {
        val request = listOf(
            DraftSubject(name = " Empty "),
            DraftSubject(
                name = " Physics ",
                chapters = listOf(
                    DraftChapter(name = "Empty chapter"),
                    DraftChapter(name = " Motion ", topics = listOf(DraftTopic(name = " Velocity "))),
                ),
            ),
        ).toImportRequest()

        assertEquals(1, request.size)
        assertEquals("Physics", request.single().name)
        assertEquals("Motion", request.single().chapters.single().name)
        assertEquals("Velocity", request.single().chapters.single().topics.single().name)
    }

    private fun completeSubject(name: String) = DraftSubject(
        name = name,
        chapters = listOf(
            DraftChapter(name = "Motion", topics = listOf(DraftTopic(name = "Velocity"))),
        ),
    )
}
