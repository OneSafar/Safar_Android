package com.safarparmar.app.ui.ekagra

import org.junit.Assert.*
import org.junit.Test

class FocusPresenceTest {
    @Test fun longStopwatchSurvivesRestoreWithoutTruncation() {
        assertEquals(10800, restoredTimerSeconds(TimerMode.STOPWATCH, 10800, 60))
        assertEquals(60, restoredTimerSeconds(TimerMode.FOCUS, 10800, 60))
        assertEquals(0, restoredTimerSeconds(TimerMode.STOPWATCH, -1, 60))
    }

    @Test fun shortSessionsFinishNormally() {
        assertEquals(0L, advancePresence(0, 0, 20, 20000, 20).deadline)
        assertEquals(0L, advancePresence(0, 0, PRESENCE_INTERVAL_SECONDS, 30000, PRESENCE_INTERVAL_SECONDS).deadline)
    }
    @Test fun checkInHasTwoMinuteGrace() {
        assertEquals(PresenceAdvance(1, 130000, false), advancePresence(PRESENCE_INTERVAL_SECONDS - 1, 0, 1, 10000))
    }
    @Test fun delayedTickCannotCredit24Hours() {
        assertEquals(PresenceAdvance(150, 150000, true), advancePresence(0, 0, 86400, 86400000))
    }
    @Test fun pausedDeadlineStillExpires() {
        assertTrue(advancePresence(PRESENCE_INTERVAL_SECONDS, 500000, 0, 500000).expired)
    }
    @Test fun restoredDeadlineIsNotExtended() {
        assertEquals(500, advancePresence(PRESENCE_INTERVAL_SECONDS, 500000, 1000, 1000000).creditedSeconds)
    }
    @Test fun confirmationStartsFreshInterval() {
        assertEquals(0L, advancePresence(0, 0, PRESENCE_INTERVAL_SECONDS - 1, 20000000).deadline)
        assertEquals(20121000L, advancePresence(PRESENCE_INTERVAL_SECONDS - 1, 0, 1, 20001000).deadline)
    }
}
