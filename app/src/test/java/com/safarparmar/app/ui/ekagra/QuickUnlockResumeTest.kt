package com.safarparmar.app.ui.ekagra

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickUnlockResumeTest {
    private val unlock = QuickUnlockResume("original-session", 1_800_000L, 400_000L)

    @Test fun `five minute unlock resumes only after its full monotonic duration`() {
        // The break began at elapsed 100000 and lasts five minutes.
        assertFalse(unlock.isDue(100_000L))
        assertFalse(unlock.isDue(399_999L))
        assertTrue(unlock.isDue(400_000L))
        assertTrue(unlock.isDue(430_000L)) // late delivery after screen-off still resumes
    }

    @Test fun `an old timeout cannot resume a newly started session`() {
        assertTrue(unlock.matches("original-session", 1_800_000L))
        assertFalse(unlock.matches("new-session", 1_800_000L))
        assertFalse(unlock.matches(null, 1_800_000L))
    }

    @Test fun `manual resume or disabling protection invalidates the cleared grace window`() {
        assertFalse(unlock.matches("original-session", 0L))
    }

    @Test fun `extending quick unlock invalidates the first timeout`() {
        val extended = unlock.copy(graceUntilMs = 2_100_000L, resumeAtElapsed = 700_000L)
        assertFalse(unlock.matches("original-session", extended.graceUntilMs))
        assertFalse(extended.isDue(unlock.resumeAtElapsed))
        assertTrue(extended.isDue(700_000L))
        assertTrue(extended.matches("original-session", 2_100_000L))
    }
}
