package com.safarparmar.app.ui.ekagra.focusshield

import org.junit.Assert.*
import org.junit.Test

class KavachServiceLifecycleTest {
    @Test fun newStartAlwaysPromotesEvenAfterEarlierSuccess() {
        val lifecycle = KavachServiceLifecycle()
        var promotions = 0
        assertTrue(lifecycle.start(1) { promotions++; true })
        assertTrue(lifecycle.start(2) { promotions++; true })
        assertEquals(2, promotions)
    }

    @Test fun oldSettingsReadCannotStopNewTimerStart() {
        val lifecycle = KavachServiceLifecycle()
        lifecycle.start(1) { true }
        lifecycle.start(2) { true }
        assertFalse(lifecycle.stopIfCurrent(1) { error("Must not stop newer service") })
        assertTrue(lifecycle.stopIfCurrent(2) { it == 2 })
    }

    @Test fun acceptedButUndeliveredStartPreventsShutdownCleanup() {
        val lifecycle = KavachServiceLifecycle()
        lifecycle.start(1) { true }
        // Android knows about start 2 before onStartCommand receives it.
        assertFalse(lifecycle.stopIfCurrent(1) { false })
        assertTrue(lifecycle.start(2) { true })
    }

    @Test fun failedPromotionIsNotRememberedAsSuccess() {
        val lifecycle = KavachServiceLifecycle()
        assertFalse(lifecycle.start(1) { false })
        assertTrue(lifecycle.start(2) { true })
    }
}
