package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.assertEquals
import org.junit.Test
import com.safarparmar.app.R

class BatchCompletionFeedbackTest {
    private val today = "2026-10-03"
    private fun row(id: String, date: String, done: Boolean) = BatchLecture(id = id, subjectId = "maths",
        scheduledFor = date, completedAt = if (done) "saved" else null)
    private fun overview(vararg rows: BatchLecture) = BatchOverview(
        subjects = listOf(BatchSubject(id = "maths", enabled = true)), lectures = rows.toList())

    @Test fun lastTodaysLectureCelebratesAllDoneWithoutCountingFutureLectures() {
        val completed = row("last", today, true)
        assertEquals(BatchNotice(R.string.toppers_batch_all_today_s_lectures_completed_proud_of_you),
            batchCompletionMessage(completed, overview(completed, row("future", "2026-10-04", false)), false, today))
    }
    @Test fun remainingTodaysLectureGetsNormalMessage() {
        val completed = row("first", today, true)
        assertEquals(BatchNotice(R.string.toppers_batch_congrats_you_re_consistent_and_on_track),
            batchCompletionMessage(completed, overview(completed, row("pending", today, false)), false, today))
    }
    @Test fun backlogGetsCloserMessageEvenIfTodayWasAlreadyCompleted() {
        val backlog = row("older", "2026-10-02", true)
        assertEquals(BatchNotice(R.string.toppers_batch_keep_it_up_you_re_1_lecture_closer),
            batchCompletionMessage(backlog, overview(backlog, row("today", today, true)), true, today))
    }
    @Test fun noScheduleDoesNotClaimAllTodaysLecturesDone() {
        assertEquals(BatchNotice(R.string.toppers_batch_congrats_you_re_consistent_and_on_track),
            batchCompletionMessage(BatchLecture(), overview(), false, today))
    }
}
