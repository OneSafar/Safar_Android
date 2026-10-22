package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchStudyWorkflowTest {
    private val subjects = listOf(BatchSubject(id = "english", key = "english", enabled = true), BatchSubject(id = "gk", key = "gk", enabled = true), BatchSubject(id = "hidden", enabled = false))
    private fun row(number: Int) = BatchLecture(id = "e$number", subjectId = "english", subjectKey = "english", order = number, lectureNumber = number)
    @Test fun futurePlansAreDeliberateWhileMissedSkippedAndManualRemainDistinct() {
        val rows = listOf(row(1).copy(studyPlannedFor = "2026-10-06"), row(2).copy(studyPlannedFor = "2026-10-04"), row(3),
            row(4).copy(completedAt = "2026-10-04T10:00:00Z"), row(5).copy(studyPlannedFor = "2026-10-06", backlogAddedAt = "2026-10-01"))
        val (watch, workflow) = BatchOverview(subjects = subjects, lectures = rows).projectStudyWorkflow("2026-10-05")
        assertEquals("e2", watch.first().nextLectureId)
        assertEquals(mapOf("e2" to "missed", "e3" to "skipped", "e5" to "manual"), workflow.backlogReasons)
        assertEquals(listOf("e2"), workflow.missed.map { it.lectureId })
        assertNull(watch.last().nextLectureId)
    }
    @Test fun oldBatchDatesNeverCreateLateEnrollmentBacklogAndUnknownImportAddsNoActivity() {
        val rows = listOf(row(1).copy(scheduledFor = "2026-01-01"), row(2).copy(completedAt = "2026-10-05T10:00:00Z", completionDateUnknown = true))
        val data = BatchOverview(subjects = subjects, lectures = rows)
        assertFalse(BatchOverview(subjects = subjects, lectures = listOf(rows[0])).projectStudyWorkflow("2026-10-05").second.backlogReasons.isNotEmpty())
        assertFalse(data.calendarEvents().any { it.kind == BatchEventKind.DONE || it.milestone == BatchMilestone.FIRST_COMPLETION })
    }
    @Test fun revisionHistoryHasBothPlannedAndActualDatesAndSeparateCounts() {
        val row = row(1).copy(completedAt = "2026-10-01T10:00:00Z", revisionSessions = listOf(
            ReviewSession("2026-10-03", "2026-10-04T10:00:00Z"), ReviewSession("2026-10-05")))
        val data = BatchOverview(subjects = subjects, lectures = listOf(row))
        val (_, workflow) = data.projectStudyWorkflow("2026-10-05")
        assertEquals(1, workflow.revisionsCompleted)
        assertEquals("revision", workflow.today.single().kind)
        assertEquals(listOf("2026-10-03", "2026-10-04", "2026-10-05"), data.calendarEvents().filter { it.kind == BatchEventKind.REVISION }.map { it.date }.sorted())
    }
}
