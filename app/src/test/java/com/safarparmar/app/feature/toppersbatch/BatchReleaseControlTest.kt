package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchReleaseControlTest {
    @Test fun backendAvailabilityOverridesDeviceDate() {
        val row = BatchLecture(scheduledFor = "2099-01-01", isAvailable = true, releaseState = "released")
        assertFalse(row.isLocked("2026-10-03"))
        assertTrue(row.copy(scheduledFor = "2020-01-01", isAvailable = false, releaseState = "locked").isLocked("2026-10-03"))
        assertFalse(row.copy(completedAt = "saved", isAvailable = false).isLocked("2026-10-03"))
    }
    @Test fun controlsReplaceLocalDatesWithoutChangingSavedWork() {
        val row = BatchLecture(id = "local-gk-2026-10-05", subjectId = "gk", subjectKey = "gk", lectureNumber = 1,
            scheduledFor = "2026-10-05", completedAt = "saved", revisionDate = "2026-10-10")
        val result = BatchOverview(subjects = listOf(BatchSubject("gk", "gk", "GK")), lectures = listOf(row),
            lectureControls = listOf(BatchLectureControl("gk", 1, null, null, "released", true)),
            serverDay = "2026-10-03").applyBackendControls()
        assertEquals(row.id, result.lectures.single().id)
        assertNull(result.lectures.single().scheduledFor)
        assertEquals("saved", result.lectures.single().completedAt)
        assertEquals("2026-10-10", result.lectures.single().revisionDate)
        assertEquals(1, result.progress.completed)
    }
    @Test fun manuallyLockedPastLectureIsExcludedFromBacklog() {
        val row = BatchLecture(id = "math-1", subjectId = "math", subjectKey = "mathematics", lectureNumber = 1,
            scheduledFor = "2026-09-28", backlogAddedAt = "2026-09-29")
        val result = BatchOverview(subjects = listOf(BatchSubject("math", "mathematics", "Maths")), lectures = listOf(row),
            lectureControls = listOf(BatchLectureControl("mathematics", 1, "2026-09-28", null, "locked", false)),
            serverDay = "2026-10-03").applyBackendControls()
        assertEquals(0, result.progress.backlog)
        assertEquals(0, result.progress.behind)
        assertEquals(0, result.progress.bySubject.single().batchAt)
        assertTrue(result.watchList.single().backlogLectureIds.isEmpty())
        assertNull(result.watchList.single().nextLectureId)
    }
}
