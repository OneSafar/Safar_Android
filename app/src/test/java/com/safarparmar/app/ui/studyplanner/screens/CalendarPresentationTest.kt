package com.safarparmar.app.ui.studyplanner.screens

import com.safarparmar.app.domain.model.studyplanner.CalendarTopicItem
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import org.junit.Assert.*
import org.junit.Test

class CalendarPresentationTest {
    private val today = "2026-10-23"
    private fun item(status: TopicStatus) = CalendarTopicItem(status = status)
    @Test fun `empty days have no workload status`() { assertNull(calendarDateStatus(today, emptyList(), today)) }
    @Test fun `revision appointments stay visible even though initial study is complete`() {
        assertEquals(CalendarDateStatus.REVISE, calendarDateStatus(today, listOf(item(TopicStatus.REVISION_NEEDED)), today))
    }
    @Test fun `completed work does not hide a pending topic`() {
        assertEquals(CalendarDateStatus.PLANNED, calendarDateStatus(today, listOf(item(TopicStatus.DONE), item(TopicStatus.TODO)), today))
        assertEquals(CalendarDateStatus.OVERDUE, calendarDateStatus("2026-10-22", listOf(item(TopicStatus.DONE), item(TopicStatus.TODO)), today))
    }
    @Test fun `completed days remain complete after their date passes`() {
        assertEquals(CalendarDateStatus.DONE, calendarDateStatus("2026-10-22", listOf(item(TopicStatus.DONE)), today))
    }
}
