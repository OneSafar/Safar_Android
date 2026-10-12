package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class UpcomingStudyActivitiesTest {
    private val day = "2026-10-06"
    private val english = BatchSubject("english", "english", "English")
    private fun row(id: String, date: String?) = BatchLecture(id = id, subjectId = "english", subjectKey = "english", studyPlannedFor = date)

    @Test fun futureLectureStaysVisibleWhenTheSameSubjectHasEarlierUnfinishedWork() {
        val overview = BatchOverview(subjects = listOf(english), lectures = listOf(row("missed", "2026-10-05"), row("today", day), row("tomorrow", "2026-10-07"), row("later", "2026-10-08")))
        assertEquals(listOf("tomorrow", "later"), overview.upcomingStudyActivities(day).map { it.lectureId })
    }

    @Test fun pendingRevisionDatesAppearAlongsideFutureWatchDates() {
        val revision = row("revision", null).copy(completedAt = "2026-10-05T10:00:00Z", revisionSessions = listOf(
            ReviewSession("2026-10-07", "2026-10-06T10:00:00Z"), ReviewSession("2026-10-08"), ReviewSession("2026-10-10")))
        val overview = BatchOverview(subjects = listOf(english), lectures = listOf(revision, row("watch", "2026-10-09")))
        assertEquals(listOf("revision", "watch", "revision"), overview.upcomingStudyActivities(day).map { it.lectureId })
        assertEquals(listOf(1, null, 2), overview.upcomingStudyActivities(day).map { it.sessionIndex })
    }

    @Test fun completedWatchDatesAndDisabledSubjectsDoNotAppear() {
        val overview = BatchOverview(subjects = listOf(english, BatchSubject("maths", "mathematics", "Maths", enabled = false)),
            lectures = listOf(row("done", "2026-10-08").copy(completedAt = "2026-10-06T10:00:00Z"),
                row("disabled", "2026-10-08").copy(subjectId = "maths"), row("bad-date", "invalid")))
        assertTrue(overview.upcomingStudyActivities(day).isEmpty())
    }
}
