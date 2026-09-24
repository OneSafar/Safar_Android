package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.data.local.TimerAlertStyle
import org.junit.Assert.*
import org.junit.Test

class FocusPresenceTest {
    @Test fun oldOffPreferenceFallsBackToAudibleEkagraAlerts() {
        assertEquals(TimerAlertStyle.SOUND, TimerAlertStyle.fromStoredValue("off"))
        assertEquals(TimerAlertStyle.VIBRATE, TimerAlertStyle.fromStoredValue("vibrate"))
    }

    @Test fun longStopwatchSurvivesRestoreWithoutTruncation() {
        assertEquals(10800, restoredTimerSeconds(TimerMode.STOPWATCH, 10800, 60))
        assertEquals(60, restoredTimerSeconds(TimerMode.FOCUS, 10800, 60))
        assertEquals(0, restoredTimerSeconds(TimerMode.STOPWATCH, -1, 60))
    }

    @Test fun fourHourBoundaryRequestsReminderWithoutDroppingElapsedTime() {
        assertEquals(
            PresenceReminderAdvance(0, true),
            advancePresenceReminder(PRESENCE_REMINDER_INTERVAL_SECONDS - 1, 1),
        )
    }

    @Test fun reminderCounterContinuesAfterBoundary() {
        assertEquals(
            PresenceReminderAdvance(5, true),
            advancePresenceReminder(PRESENCE_REMINDER_INTERVAL_SECONDS - 1, 6),
        )
    }

    @Test fun shortSessionsDoNotRequestReminder() {
        assertEquals(PresenceReminderAdvance(20, false), advancePresenceReminder(0, 20))
    }

    @Test fun focusIsCappedAt18HoursButStopwatchRemainsUnboundedByDuration() {
        assertEquals(MAX_EKAGRA_SESSION_SECONDS, configuredTimerSeconds(TimerMode.FOCUS, 24 * 60 * 60))
        assertEquals(MAX_EKAGRA_SESSION_SECONDS, configuredTimerSeconds(TimerMode.POMODORO, 24 * 60 * 60))
        assertEquals(0, configuredTimerSeconds(TimerMode.STOPWATCH, Int.MAX_VALUE))
    }
}
