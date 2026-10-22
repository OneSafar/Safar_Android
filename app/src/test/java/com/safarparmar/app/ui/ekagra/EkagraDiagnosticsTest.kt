package com.safarparmar.app.ui.ekagra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EkagraDiagnosticsTest {
    @Test fun unrelatedHistoricalExitDoesNotExplainTheActiveSession() {
        assertNull(relevantEkagraProcessExit(listOf(exit(999)), "app", 1000, 2000))
    }

    @Test fun childProcessCrashDoesNotExplainTheInProcessTimerStop() {
        assertNull(relevantEkagraProcessExit(listOf(exit(1500).copy(processName = "app:webview")), "app", 1000, 2000))
    }

    @Test fun firstMainProcessExitAfterCheckpointIsTheInterruption() {
        assertEquals(1500L, relevantEkagraProcessExit(listOf(exit(1800), exit(1500), exit(900)), "app", 1000, 2000)?.timestampMs)
    }

    @Test fun futureTimestampIsNotAttributedAcrossAClockChange() {
        assertNull(relevantEkagraProcessExit(listOf(exit(2100)), "app", 1000, 2000))
    }

    private fun exit(at: Long) = EkagraProcessExitRecord(at, reason = 2, status = 9, importance = 100, pid = 42, processName = "app")
}
