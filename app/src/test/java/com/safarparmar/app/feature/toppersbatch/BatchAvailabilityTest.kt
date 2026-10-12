package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchAvailabilityTest {
    @Test fun futureLecturesNeverBecomeBacklogEvenWithLegacyCompletionAndManualFlags() {
        val future = BatchLecture(id = "future", subjectId = "english", subjectKey = "english", order = 1,
            scheduledFor = "2026-10-05", backlogAddedAt = "2026-09-30")
        val legacyDone = future.copy(id = "done", order = 2, completedAt = "2026-09-29")
        val batch = BatchOverview(subjects = listOf(BatchSubject("english", "english", "English")),
            lectures = listOf(future, legacyDone)).withLecture(legacyDone, "2026-10-03")
        assertEquals(0, batch.progress.backlog)
        assertTrue(batch.watchList.single().backlogLectureIds.isEmpty())
        assertTrue(batch.libraryRows("", LibraryFilter.BACKLOG, "2026-10-03").isEmpty())
        assertFalse(future.olderWork("2026-10-03"))
        assertNull(batch.watchList.single().nextLectureId)
    }
    @Test fun pastClassDatesRemainEventInformationUntilAddedToPersonalBacklog() {
        val base = BatchLecture(subjectId = "maths", subjectKey = "mathematics")
        val past = base.copy(id = "past", scheduledFor = "2026-10-02", order = 1)
        val today = base.copy(id = "today", scheduledFor = "2026-10-03", order = 2)
        val future = base.copy(id = "future", scheduledFor = "2026-10-04", order = 3)
        val batch = BatchOverview(subjects = listOf(BatchSubject("maths", "mathematics", "Maths")),
            lectures = listOf(past, today, future)).withLecture(past, "2026-10-03")
        assertTrue(batch.watchList.single().backlogLectureIds.isEmpty())
        assertEquals(0, batch.progress.backlog)
        assertEquals(0, batch.progress.behind)
        val manual = batch.withLecture(past.copy(backlogAddedAt = "2026-10-03T10:00:00Z"), "2026-10-03")
        assertEquals(1, manual.progress.behind)
        assertEquals(listOf("past"), manual.watchList.single().backlogLectureIds)
        assertEquals(listOf("past"), manual.libraryRows("", LibraryFilter.BACKLOG, "2026-10-03").map { it.id })
        assertFalse(today.isLocked("2026-10-03"))
        assertTrue(future.isLocked("2026-10-03"))
    }
    @Test fun undoneLectureDoesNotRemainInRevisionList() {
        val row = BatchLecture(revisionDate = "2026-10-03", revisionSessions = listOf(ReviewSession("2026-10-03")))
        assertFalse(row.pendingRevision)
        assertTrue(row.copy(completedAt = "2026-10-02").pendingRevision)
    }
}
