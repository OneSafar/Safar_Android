package com.safarparmar.app.ui.ekagra

import org.junit.Assert.*
import org.junit.Test

class FocusPresenceTest {
    @Test fun shortSessionsFinishNormally() {
        assertEquals(0L, advancePresence(0, 0, 3600, 3600000, 3600).deadline)
        assertEquals(0L, advancePresence(0, 0, 9000, 9000000, 9000).deadline)
    }
    @Test fun checkInHasFiveMinuteGrace() {
        assertEquals(PresenceAdvance(1, 310000, false), advancePresence(8999, 0, 1, 10000))
    }
    @Test fun delayedTickCannotCredit24Hours() {
        assertEquals(PresenceAdvance(9300, 9300000, true), advancePresence(0, 0, 86400, 86400000))
    }
    @Test fun pausedDeadlineStillExpires() {
        assertTrue(advancePresence(9000, 500000, 0, 500000).expired)
    }
    @Test fun restoredDeadlineIsNotExtended() {
        assertEquals(500, advancePresence(9000, 500000, 1000, 1000000).creditedSeconds)
    }
    @Test fun confirmationStartsFreshInterval() {
        assertEquals(0L, advancePresence(0, 0, 8999, 20000000).deadline)
        assertEquals(20301000L, advancePresence(8999, 0, 1, 20001000).deadline)
    }
}
