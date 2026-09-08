package com.safarparmar.app.ui.ekagra

import org.junit.Assert.*
import org.junit.Test

class FocusPresenceTest {
    @Test fun shortSessionsFinishNormally() {
        assertEquals(0L, advancePresence(0, 0, 3600, 3600000, 3600).deadline)
        assertEquals(0L, advancePresence(0, 0, 5400, 5400000, 5400).deadline)
    }
    @Test fun checkInHasTwoMinuteGrace() {
        assertEquals(PresenceAdvance(1, 130000, false), advancePresence(5399, 0, 1, 10000))
    }
    @Test fun delayedTickCannotCredit24Hours() {
        assertEquals(PresenceAdvance(5520, 5520000, true), advancePresence(0, 0, 86400, 86400000))
    }
    @Test fun pausedDeadlineStillExpires() {
        assertTrue(advancePresence(5400, 500000, 0, 500000).expired)
    }
    @Test fun restoredDeadlineIsNotExtended() {
        assertEquals(500, advancePresence(5400, 500000, 1000, 1000000).creditedSeconds)
    }
    @Test fun confirmationStartsFreshInterval() {
        assertEquals(0L, advancePresence(0, 0, 5399, 20000000).deadline)
        assertEquals(20121000L, advancePresence(5399, 0, 1, 20001000).deadline)
    }
}
