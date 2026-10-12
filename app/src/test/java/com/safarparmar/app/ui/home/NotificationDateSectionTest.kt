package com.safarparmar.app.ui.home

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDateSectionTest {
    private val today = LocalDate.of(2026, 9, 26)

    @Test fun utcTimestampUsesDisplayedIndiaDate() {
        assertEquals(NotificationDateSection.TODAY, notificationDateSection("2026-09-25T20:00:00Z", today))
    }
    @Test fun previousSixDaysBelongToRecentSection() {
        assertEquals(NotificationDateSection.THIS_WEEK, notificationDateSection("2026-09-20T08:00:00Z", today))
        assertEquals(NotificationDateSection.EARLIER, notificationDateSection("2026-09-19T08:00:00Z", today))
    }
    @Test fun malformedAndMissingDatesStayVisibleInEarlier() {
        assertEquals(NotificationDateSection.EARLIER, notificationDateSection("", today))
        assertEquals(NotificationDateSection.EARLIER, notificationDateSection("invalid", today))
    }
    @Test fun futureDatesDoNotAppearAsToday() {
        assertEquals(NotificationDateSection.EARLIER, notificationDateSection("2026-10-01T08:00:00Z", today))
    }
}
