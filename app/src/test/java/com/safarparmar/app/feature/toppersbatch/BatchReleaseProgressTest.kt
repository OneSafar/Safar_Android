package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchReleaseProgressTest {
    private val day = "2026-10-05"
    private val subject = BatchSubject(id = "english", key = "english", name = "English")
    private fun lecture(number: Int) = BatchLecture(id = "e$number", subjectId = subject.id, subjectKey = subject.key,
        lectureNumber = number, order = number, scheduledFor = day, isAvailable = true)
    private fun overview(vararg lectures: BatchLecture) = BatchOverview(subjects = listOf(subject), lectures = lectures.toList())

    @Test fun comparesCompletedLecturesWithTheReleasedSetInsteadOfTheWholeCourse() {
        val rows = (1..100).map { number -> lecture(number).copy(isAvailable = number <= 40,
            completedAt = if (number <= 10) "saved" else null) }
        val progress = overview(*rows.toTypedArray()).releaseProgress(day)
        assertEquals(40, progress.releasedCount)
        assertEquals(10, progress.completedCount)
        assertEquals(30, progress.remainingCount)
        assertEquals(0.25f, progress.fraction)
    }

    @Test fun serverAvailabilityOverridesScheduleAndCompletedDoesNotUnlockTheReleaseCount() {
        val progress = overview(
            lecture(1).copy(scheduledFor = "2099-01-01", isAvailable = true),
            lecture(2).copy(scheduledFor = "2020-01-01", isAvailable = false),
            lecture(3).copy(isAvailable = false, completedAt = "saved"),
        ).releaseProgress(day)
        assertEquals(1, progress.releasedCount)
        assertEquals(0, progress.completedCount)
        assertEquals(listOf("e1"), progress.subjects.single().remaining.map { it.id })
    }

    @Test fun releaseFlagsOverrideDatesWhenAvailabilityIsAbsent() {
        val progress = overview(
            lecture(1).copy(isAvailable = null, releaseState = "released", scheduledFor = null),
            lecture(2).copy(isAvailable = null, releaseState = "locked", scheduledFor = "2020-01-01"),
            lecture(3).copy(isAvailable = null, releaseState = "locked", completedAt = "saved"),
        ).releaseProgress(day)
        assertEquals(listOf("e1"), progress.subjects.single().lectures.map { it.id })
        assertEquals(0, progress.completedCount)
    }

    @Test fun legacyDatesIncludeTodayAndPastButExcludeFutureMissingAndInvalidDates() {
        val dates = listOf("2026-10-04", day, "2026-10-06", null, "invalid")
        val rows = dates.mapIndexed { index, date -> lecture(index + 1).copy(isAvailable = null, scheduledFor = date) }
        assertEquals(listOf("e1", "e2"), overview(*rows.toTypedArray()).releaseProgress(day).subjects.single().lectures.map { it.id })
    }

    @Test fun personalDatesBacklogAndRevisionSessionsDoNotChangeTheComparison() {
        val batch = overview(lecture(1), lecture(2).copy(completedAt = "saved"))
        val planned = batch.copy(studyPlan = BatchStudyPlan("2026-10-01"), lectures = batch.lectures.map {
            it.copy(studyPlannedFor = "2026-12-01", backlogAddedAt = day,
                revisionSessions = listOf(ReviewSession(day, "saved"), ReviewSession("2026-10-10")))
        })
        val before = batch.releaseProgress(day)
        val after = planned.releaseProgress(day)
        assertEquals(before.releasedCount, after.releasedCount)
        assertEquals(before.completedCount, after.completedCount)
        assertEquals(before.remainingCount, after.remainingCount)
        assertEquals(listOf("e1"), after.subjects.single().remaining.map { it.id })
    }

    @Test fun disabledAndUnknownSubjectsDoNotInflateCounts() {
        val hidden = subject.copy(id = "hidden", enabled = false)
        val batch = overview(lecture(1), lecture(2).copy(subjectId = hidden.id), lecture(3).copy(subjectId = "unknown"))
            .copy(subjects = listOf(subject, hidden))
        assertEquals(1, batch.releaseProgress(day).releasedCount)
    }

    @Test fun studentAdditionsDoNotInflateOfficialBatchProgress() {
        val progress = overview(lecture(1), lecture(2).copy(userAdded = true, completedAt = "saved"))
            .releaseProgress(day)
        assertEquals(1, progress.releasedCount)
        assertEquals(0, progress.completedCount)
        assertEquals(listOf("e1"), progress.subjects.single().remaining.map { it.id })
    }

    @Test fun remainingLecturesUseSubjectOrderAndNumericLectureOrder() {
        val maths = subject.copy(id = "maths", key = "mathematics", name = "Maths")
        val reason = subject.copy(id = "reasoning", key = "reasoning", name = "Reasoning")
        val batch = overview(lecture(10), lecture(2), lecture(1).copy(completedAt = "saved"),
            lecture(3).copy(id = "math3", subjectId = maths.id), lecture(4).copy(id = "r4", subjectId = reason.id))
            .copy(subjects = listOf(maths, reason, subject))
        val progress = batch.releaseProgress(day)
        assertEquals(listOf("English", "Reasoning", "Maths"), progress.subjects.map { it.subject.name })
        assertEquals(listOf("e2", "e10"), progress.subjects.first().remaining.map { it.id })
    }

    @Test fun confirmedCompletionAndUndoUpdateTheReleasedComparisonImmediately() {
        val row = lecture(1)
        val initial = overview(row)
        val completed = initial.withLecture(row.copy(completedAt = "saved"), day)
        assertEquals(1, completed.releaseProgress(day).completedCount)
        assertEquals(0, completed.releaseProgress(day).remainingCount)
        val undone = completed.withLecture(row, day)
        assertEquals(0, undone.releaseProgress(day).completedCount)
        assertEquals(1, undone.releaseProgress(day).remainingCount)
    }

    @Test fun importedCompletionWithUnknownDateStillCountsAsCourseCoverage() {
        val progress = overview(lecture(1).copy(completedAt = "saved", completionDateUnknown = true)).releaseProgress(day)
        assertEquals(1, progress.completedCount)
        assertEquals(1f, progress.fraction)
    }

    @Test fun zeroReleasedLecturesIsSafeAndDoesNotTreatFutureCompletionAsTeacherProgress() {
        val progress = overview(lecture(1).copy(isAvailable = false, completedAt = "saved")).releaseProgress(day)
        assertEquals(0, progress.releasedCount)
        assertEquals(0, progress.completedCount)
        assertEquals(0, progress.remainingCount)
        assertEquals(0f, progress.fraction)
        assertEquals(0f, BatchOverview().releaseProgress(day).fraction)
    }
}
