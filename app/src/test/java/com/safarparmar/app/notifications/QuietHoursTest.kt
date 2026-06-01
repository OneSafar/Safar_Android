package com.safarparmar.app.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class QuietHoursTest {
    private val quietStart = LocalTime.of(22, 0)
    private val quietEnd = LocalTime.of(7, 0)
    private val insideQuietHours = LocalTime.of(23, 0)
    private val outsideQuietHours = LocalTime.of(12, 0)

    @Test
    fun quietHours_doesNotSuppressFocusTimer() {
        assertFalse(
            QuietHoursEvaluator.shouldSuppress(
                SafarNotificationChannels.FOCUS_TIMER,
                quietStart,
                quietEnd,
                insideQuietHours,
            ),
        )
    }

    @Test
    fun quietHours_doesNotSuppressFocusShieldStatus() {
        assertFalse(
            QuietHoursEvaluator.shouldSuppress(
                SafarNotificationChannels.FOCUS_SHIELD_STATUS,
                quietStart,
                quietEnd,
                insideQuietHours,
            ),
        )
    }

    @Test
    fun quietHours_doesNotSuppressFocusShieldBlocked() {
        assertFalse(
            QuietHoursEvaluator.shouldSuppress(
                SafarNotificationChannels.FOCUS_SHIELD_BLOCKED,
                quietStart,
                quietEnd,
                insideQuietHours,
            ),
        )
    }

    @Test
    fun quietHours_suppressesStudyReminderInsideQuietHours() {
        assertTrue(
            QuietHoursEvaluator.shouldSuppress(
                SafarNotificationChannels.STUDY_REMINDERS,
                quietStart,
                quietEnd,
                insideQuietHours,
            ),
        )
    }

    @Test
    fun quietHours_allowsStudyReminderOutsideQuietHours() {
        assertFalse(
            QuietHoursEvaluator.shouldSuppress(
                SafarNotificationChannels.STUDY_REMINDERS,
                quietStart,
                quietEnd,
                outsideQuietHours,
            ),
        )
    }
}
