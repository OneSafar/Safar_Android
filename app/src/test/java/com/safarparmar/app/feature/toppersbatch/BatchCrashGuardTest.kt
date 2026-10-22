package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchCrashGuardTest {
    private val day = "2026-10-05"

    @Test fun revisionBookmarkWithoutDateDoesNotCrashProgressOrBecomeDue() {
        val base = BatchLecture(id = "bookmark", subjectId = "e", subjectKey = "english",
            completedAt = "saved", revisionTagged = true)
        val batch = BatchOverview(subjects = listOf(BatchSubject(id = "e", key = "english")), lectures = listOf(
            base, base.copy(id = "bad", revisionDate = "invalid"),
            base.copy(id = "due", revisionDate = day),
            base.copy(id = "future", revisionDate = "2026-10-06"),
            base.copy(id = "done", revisionDate = day, revisionCompletedAt = "saved")))
        assertEquals(listOf("due"), batch.dueRevisions(day).map { it.id })
        assertTrue(batch.libraryRows("", LibraryFilter.REVISION, day).any { it.id == "bookmark" })
    }

    @Test fun malformedMissedPlanDatesCannotReachTheReschedulingPreviewParser() {
        val valid = BatchStudyActivity("e1", "watch", "2026-10-04")
        val items = listOf(valid, valid.copy(date = ""), valid.copy(date = "2026-02-30"),
            valid.copy(date = "invalid"), valid.copy(lectureId = "e2", date = "2026-10-03T12:00:00Z"))
        val result = validRescheduleActivities(items)
        assertEquals(listOf("2026-10-04", "2026-10-03"), result.map { it.date })
        result.forEach { java.time.LocalDate.parse(it.date) }
    }
}
