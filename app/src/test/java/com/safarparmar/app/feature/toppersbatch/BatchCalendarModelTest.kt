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
        assertEquals(listOf(BatchMilestone.BATCH_START, BatchMilestone.PLAN_START, BatchMilestone.TARGET_FINISH), events.map { it.milestone })
        assertFalse(events.any { it.kind == BatchEventKind.DONE || it.milestone == BatchMilestone.FIRST_COMPLETION })
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

    @Test fun revisionMenuListsPendingLecturesAndFollowsRescheduling() {
        val rows = subjects.map { subject -> BatchLecture(id = subject.id, subjectId = subject.id,
            completedAt = "2026-10-01T10:00:00Z", revisionSessions = listOf(ReviewSession("2026-10-05"))) }.toMutableList()
        rows[0] = rows[0].copy(revisionSessions = listOf(ReviewSession("2026-10-05"), ReviewSession("2026-10-05"), ReviewSession("2026-10-05", "2026-10-05T10:00:00Z")))
        fun pending(lectures: List<BatchLecture>, day: String = "2026-10-05") = pendingCalendarRevisions(
            BatchOverview(subjects = subjects, lectures = lectures).calendarEvents().filter { it.date == day })
        assertEquals(4, pending(rows).size)
        assertEquals(2, pending(rows).first { it.first.lecture!!.id == rows[0].id }.second)
        val moved = rows[1].copy(revisionSessions = listOf(ReviewSession("2026-10-06")))
        assertTrue(pending(listOf(moved)).isEmpty())
        assertEquals(1, pending(listOf(moved), "2026-10-06").size)
        assertTrue(pending(listOf(rows[2].copy(revisionSessions = emptyList()))).isEmpty())
        assertTrue(pending(listOf(rows[3].copy(completedAt = null))).isEmpty())
        assertTrue(pendingCalendarRevisions(BatchOverview(subjects = subjects.map { it.copy(enabled = false) }, lectures = rows).calendarEvents()).isEmpty())
    }

    @Test fun remainingClassesFollowTimeAndDisappearAtTwoHourBoundary() {
        val times = mapOf("gk" to "09:30", "reasoning" to "12:30", "english" to "15:30", "mathematics" to "18:30")
        val data = BatchOverview(subjects = subjects, lectures = subjects.map { subject -> BatchLecture(id = subject.id,
            subjectId = subject.id, subjectKey = subject.key, scheduledFor = "2026-10-05", classTime = times[subject.id]) })
        fun at(time: String) = data.remainingClasses(java.time.OffsetDateTime.parse("2026-10-05T${time}:00+05:30").toInstant()).map { it.subject.id }
        assertEquals(listOf("gk", "reasoning", "english", "mathematics"), at("10:00"))
        assertEquals(listOf("reasoning", "english", "mathematics"), at("11:30"))
        assertTrue(at("20:30").isEmpty())
    }

}
