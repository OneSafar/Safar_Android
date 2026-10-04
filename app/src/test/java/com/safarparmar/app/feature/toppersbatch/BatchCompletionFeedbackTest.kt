package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.assertEquals
import org.junit.Test

class BatchCompletionFeedbackTest {
    private val today = "2026-10-03"
    private fun row(id: String, date: String, done: Boolean) = BatchLecture(id = id, subjectId = "maths",
        scheduledFor = date, completedAt = if (done) "saved" else null)
    private fun overview(vararg rows: BatchLecture) = BatchOverview(
        subjects = listOf(BatchSubject(id = "maths", enabled = true)), lectures = rows.toList())

    @Test fun lastTodaysLectureCelebratesAllDoneWithoutCountingFutureLectures() {
        val completed = row("last", today, true)
        assertEquals("All today's lectures completed. Proud of you!",
            batchCompletionMessage(completed, overview(completed, row("future", "2026-10-04", false)), false, today))
    }
    @Test fun remainingTodaysLectureGetsNormalMessage() {
        val completed = row("first", today, true)
        assertEquals("Congrats! You're consistent and on track.",
            batchCompletionMessage(completed, overview(completed, row("pending", today, false)), false, today))
    }
    @Test fun backlogGetsCloserMessageEvenIfTodayWasAlreadyCompleted() {
        val backlog = row("older", "2026-10-02", true)
        assertEquals("Keep it up! You're 1 lecture closer.",
            batchCompletionMessage(backlog, overview(backlog, row("today", today, true)), true, today))
    }
    @Test fun noScheduleDoesNotClaimAllTodaysLecturesDone() {
        assertEquals("Congrats! You're consistent and on track.",
            batchCompletionMessage(BatchLecture(), overview(), false, today))
    }
}
