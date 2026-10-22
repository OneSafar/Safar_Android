package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchReleaseControlTest {
    @Test fun backendContentRefreshUsesPermanentSlotsAndPreservesPersonalHistory() {
        val row = BatchLecture(id = "same-id", subjectId = "reasoning", subjectKey = "reasoning",
            lectureNumber = 17, dailySlotNumber = 23, order = 23, displayLectureNumber = 6,
            originalTopic = "Old chapter", displayTopic = "Old chapter", completedAt = "saved",
            revisionDate = "2026-10-28", color = "#123456", studyPlannedFor = "2026-10-22")
        val controls = listOf(
            BatchLectureControl("reasoning", 17, "2099-01-01", null, "locked", false, "Wrong slot", 99),
            BatchLectureControl("reasoning", 23, "2026-10-21", "12:30", "scheduled", true, "Series", 7),
        )
        val overview = BatchOverview(subjects = listOf(BatchSubject("reasoning", "reasoning", "Reasoning")),
            lectures = listOf(row), lectureControls = controls, serverDay = "2026-10-21")
        val result = overview.applyBackendControls().lectures.single()
        assertEquals(row.copy(displayLectureNumber = 7, originalTopic = "Series", displayTopic = "Series",
            scheduledFor = "2026-10-21", classTime = "12:30", releaseState = "scheduled", isAvailable = true), result)
        assertEquals(7, result.shownLectureNumber)
        assertEquals(23, result.controlSlotNumber)
        assertEquals("My notes", overview.copy(lectures = listOf(row.copy(displayTopic = "My notes")))
            .applyBackendControls().lectures.single().displayTopic)
    }
    @Test fun chapterNumberResetsNeverReorderPlanningOrCompletion() {
        val first = BatchLecture(id = "first", subjectId = "gk", subjectKey = "gk", lectureNumber = 15,
            order = 15, displayLectureNumber = 10, isAvailable = true)
        val second = first.copy(id = "second", lectureNumber = 16, order = 16, displayLectureNumber = 1)
        val data = BatchOverview(subjects = listOf(BatchSubject("gk", "gk", "GK")), lectures = listOf(first, second))
        assertEquals(listOf("first", "second"), data.planningCandidates("2026-10-21").map { it.id })
        assertEquals("first", data.nextPlanningLectures("2026-10-21")["gk"]?.id)
        val updated = data.withLecture(first.copy(completedAt = "saved"), "2026-10-21")
        assertEquals("second", updated.watchList.single().nextLectureId)
        assertEquals(1, updated.progress.completed)
    }
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
