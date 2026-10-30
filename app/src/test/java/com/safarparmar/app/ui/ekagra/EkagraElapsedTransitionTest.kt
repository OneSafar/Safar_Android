package com.safarparmar.app.ui.ekagra

import org.junit.Assert.*
import org.junit.Test

class EkagraElapsedTransitionTest {
    private val start = EkagraTimerRecoveryState(TimerMode.POMODORO, 60, 60, true,
        1000L, 7, 1_800_000_000_000L, targetPomodoroLoops = 2,
        pomodoroFocusSeconds = 60, pomodoroBreakSeconds = 10, pomodoroFinalBreakSeconds = 20, autoStartBreak = true)

    @Test fun delayedWakeCrossesFocusAndBreakWithoutGivingBreakTimeFocusCredit() {
        val result = advanceEkagraTimer(start, 91_000L, 7, start.checkpointAtMs + 90_000L)
        assertEquals(TimerMode.POMODORO, result.state.mode)
        assertEquals(40, result.state.remainingSeconds)
        assertEquals(1, result.state.completedPomodoroLoops)
        assertEquals(listOf(10), result.completedBreaks.map { it.seconds })
        assertEquals(71_000L, result.completedBreaks.single().endedElapsedMs)
        assertFalse(result.completed)
    }

    @Test fun veryLateWakeFinalizesOnceAtOriginalBoundaryAndRetainsBothBreaks() {
        val result = advanceEkagraTimer(start, 901_000L, 7, start.checkpointAtMs + 900_000L)
        assertTrue(result.completed)
        assertFalse(result.state.isRunning)
        assertEquals(start.checkpointAtMs + 150_000L, result.completedAtMs)
        assertEquals(listOf(10, 20), result.completedBreaks.map { it.seconds })
        val progress = calculateFocusProgress(result.state.mode, result.state.totalSeconds,
            result.state.remainingSeconds, 60, 2, result.state.completedPomodoroLoops)
        assertEquals(120, progress.actualSeconds)
    }

    @Test fun liveAndRecoveredBreakCheckpointProduceSameDurableBoundaryIdentity() {
        val live = advanceEkagraTimer(start, 91_000L, 7, start.checkpointAtMs + 90_000L)
        val duringBreak = start.copy(mode = TimerMode.BREAK, totalSeconds = 10, remainingSeconds = 5,
            completedPomodoroLoops = 1, checkpointElapsedMs = 66_000L, checkpointAtMs = start.checkpointAtMs + 65_000L)
        val recovered = recoverEkagraTimer(duringBreak, 91_000L, 7, start.checkpointAtMs + 90_000L)
        assertEquals(live.completedBreaks, recovered.completedBreaks)
        assertEquals(live.state.remainingSeconds, recovered.state.remainingSeconds)
    }

    @Test fun manualBreakFinishesWithoutAutoResumingOrCreditingTheRestOfTheGap() {
        val state = start.copy(mode = TimerMode.BREAK, totalSeconds = 10, remainingSeconds = 4,
            suspendedTotalSeconds = 60, suspendedRemainingSeconds = 35)
        val result = advanceEkagraTimer(state, 101_000L, 7, state.checkpointAtMs + 100_000L)
        assertEquals(TimerMode.POMODORO, result.state.mode)
        assertEquals(35, result.state.remainingSeconds)
        assertFalse(result.state.isRunning)
        assertEquals(state.checkpointAtMs + 4000L, result.completedBreaks.single().endedAtMs)
    }
}
