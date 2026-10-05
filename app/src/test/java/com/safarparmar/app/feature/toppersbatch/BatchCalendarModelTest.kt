package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchCalendarModelTest {
    private val subjects = listOf("mathematics", "english", "reasoning", "gk").map { BatchSubject(it, it, it) }
    @Test fun completionUsesIndiaDateAndKeepsClassDateSeparate() {
        val row = BatchLecture(id = "m1", subjectId = "mathematics", scheduledFor = "2026-10-03",
            completedAt = "2026-10-03T20:00:00Z")
        val events = BatchOverview(subjects = subjects, lectures = listOf(row)).calendarEvents()
        assertEquals("2026-10-04", events.single { it.kind == BatchEventKind.DONE }.date)
        assertEquals("2026-10-03", events.single { it.kind == BatchEventKind.CLASS }.date)
        assertEquals("2026-10-04", events.single { it.kind == BatchEventKind.MILESTONE }.date)
    }
    @Test fun allSubjectsHaveIndependentNextClassAndUnknownDatesStayUnknown() {
        val lectures = subjects.flatMap { subject -> listOf(
            BatchLecture(id = "${subject.id}-past", subjectId = subject.id, scheduledFor = "2026-10-02"),
            BatchLecture(id = "${subject.id}-next", subjectId = subject.id, scheduledFor = "2026-10-05")) }
        val result = BatchOverview(subjects = subjects, lectures = lectures).calendarSubjects("2026-10-04")
        assertEquals(4, result.size)
        assertTrue(result.all { it.nextClass?.scheduledFor == "2026-10-05" && it.firstCompletion == null })
        assertNull(calendarDate("2026-02-30"))
        assertNull(completionDay("invalid"))
    }
    @Test fun planStartAndTargetRemainSeparateFromRecordedStudyActivity() {
        val data = BatchOverview(subjects = subjects, course = BatchCourse(officialStartDate = "2026-09-28"),
            studyPlan = BatchStudyPlan("2026-10-01", "2027-03-28"))
        val events = data.calendarEvents()
        assertEquals(listOf("Batch starts", "Your plan starts", "Your target finish"), events.map { it.title })
        assertFalse(events.any { it.kind == BatchEventKind.DONE || it.title == "First lecture completed" })
    }
    @Test fun completedRevisionHistoryRemainsAndDisabledSubjectsDoNotAppear() {
        val row = BatchLecture(id = "r", subjectId = "mathematics", completedAt = "2026-10-02T10:00:00Z",
            revisionSessions = listOf(ReviewSession("2026-10-03", "2026-10-03T10:00:00Z"), ReviewSession("2026-10-06")))
        val data = BatchOverview(subjects = subjects, lectures = listOf(row))
        assertEquals(listOf("2026-10-03", "2026-10-06"), data.calendarEvents().filter { it.kind == BatchEventKind.REVISION }.map { it.date })
        assertTrue(data.copy(subjects = subjects.map { it.copy(enabled = false) }).calendarEvents().isEmpty())
    }
    @Test fun lateCompletionPersonalPlanAndOfficialDateStayIndependent() {
        val row = BatchLecture(id = "late", subjectId = "mathematics", scheduledFor = "2026-10-03", studyPlannedFor = "2026-10-12", completedAt = "2026-10-10T20:00:00Z", revisionTagged = true)
        val events = BatchOverview(subjects = subjects, lectures = listOf(row)).calendarEvents()
        assertEquals("2026-10-03", events.single { it.kind == BatchEventKind.CLASS }.date)
        assertEquals("2026-10-12", events.single { it.kind == BatchEventKind.PLANNED }.date)
        assertEquals("2026-10-11", events.single { it.kind == BatchEventKind.DONE }.date)
        assertTrue(events.single { it.kind == BatchEventKind.DONE }.lecture!!.revisionTagged)
    }

}
