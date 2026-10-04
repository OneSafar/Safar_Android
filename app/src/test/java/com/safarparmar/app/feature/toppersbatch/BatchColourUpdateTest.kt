package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchColourUpdateTest {
    @Test fun colourUpdateReachesEveryViewWithoutChangingSavedWork() {
        val row = BatchLecture(id = "lecture", color = "old", completedAt = "finished", scheduledFor = "2026-10-05",
            revisionSessions = listOf(ReviewSession("2026-10-06")))
        val other = row.copy(id = "other")
        val state = BatchUiState(overview = BatchOverview(lectures = listOf(row, other)),
            today = BatchToday(lectures = listOf(row), backlogPick = row),
            calendar = BatchCalendar(days = mapOf("day" to listOf(row)), classes = mapOf("day" to listOf(row))))
        val updated = state.withLectureColor(row.id, "new")
        assertEquals(row.copy(color = "new"), updated.overview!!.lectures.first())
        assertEquals(other, updated.overview!!.lectures.last())
        assertEquals("new", updated.today!!.lectures.first().color)
        assertEquals("new", updated.today!!.backlogPick!!.color)
        assertEquals("new", updated.calendar!!.days["day"]!!.first().color)
        assertEquals("new", updated.calendar!!.classes["day"]!!.first().color)
        assertEquals(state, updated.withLectureColor(row.id, "old"))
    }
}
