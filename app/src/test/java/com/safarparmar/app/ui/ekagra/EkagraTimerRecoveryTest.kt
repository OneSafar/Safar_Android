package com.safarparmar.app.ui.ekagra

import org.junit.Assert.*
import org.junit.Test

class EkagraTimerRecoveryTest {
    private val at = 1_800_000_000_000L
    private fun focus() = EkagraTimerRecoveryState(
        mode = TimerMode.FOCUS, totalSeconds = 7200, remainingSeconds = 6467,
        isRunning = true, checkpointElapsedMs = 1_000_000L,
        checkpointBootCount = 7, checkpointAtMs = at,
    )
    private fun restore(state: EkagraTimerRecoveryState = focus(), seconds: Long,
        boot: Int? = 7, wall: Long = at + seconds * 1000) = recoverEkagraTimer(
        state, state.checkpointElapsedMs + seconds * 1000, boot, wall,
    )
    private fun pomodoro() = focus().copy(
        mode = TimerMode.POMODORO, totalSeconds = 60, remainingSeconds = 48,
        targetPomodoroLoops = 3, pomodoroFocusSeconds = 60,
        pomodoroBreakSeconds = 20, pomodoroFinalBreakSeconds = 40,
        autoStartBreak = true,
    )
    private fun progress(state: EkagraTimerRecoveryState) = calculateFocusProgress(
        state.mode, state.totalSeconds, state.remainingSeconds, state.pomodoroFocusSeconds,
        state.targetPomodoroLoops, state.completedPomodoroLoops,
    ).actualSeconds

    @Test fun `two hour focus killed after twelve minutes still finishes at its original deadline`() {
        val result = restore(seconds = 6467)
        assertTrue(result.completed)
        assertTrue(result.periodFinished)
        assertEquals(7200, progress(result.state))
        assertEquals(0, result.state.remainingSeconds)
        assertEquals(at + 6467_000L, result.completedAtMs)
        assertFalse(result.state.isRunning)
    }

    @Test fun `later reopen never credits more than the selected focus duration`() {
        val result = restore(seconds = 86_400)
        assertTrue(result.completed)
        assertEquals(7200, progress(result.state))
        assertEquals(at + 6467_000L, result.completedAtMs)
    }

    @Test fun `same boot process restart resumes countdown after exact monotonic gap`() {
        val result = restore(seconds = 61)
        assertTrue(result.sameBoot)
        assertTrue(result.state.isRunning)
        assertEquals(6406, result.state.remainingSeconds)
        assertEquals(794, progress(result.state))
        assertFalse(result.completed)
    }

    @Test fun `wall clock changes cannot add or remove running study time`() {
        val forward = restore(seconds = 61, wall = at + 10 * 86_400_000L)
        val backward = restore(seconds = 61, wall = at - 10 * 86_400_000L)
        assertEquals(forward.state.remainingSeconds, backward.state.remainingSeconds)
        assertEquals(6406, backward.state.remainingSeconds)
        val completed = restore(seconds = 7000, wall = at - 86_400_000L)
        assertEquals(at + 6467_000L, completed.completedAtMs)
    }

    @Test fun `paused checkpoints never earn a missing process gap`() {
        val result = restore(focus().copy(isRunning = false), seconds = 3600)
        assertEquals(6467, result.state.remainingSeconds)
        assertEquals(733, progress(result.state))
        assertFalse(result.state.isRunning)
        assertEquals(0L, result.elapsedSeconds)
    }

    @Test fun `reboot or unavailable boot identity pauses exactly at the durable checkpoint`() {
        for (boot in listOf(8, null)) {
            val result = restore(seconds = 86_400, boot = boot)
            assertFalse(result.sameBoot)
            assertFalse(result.state.isRunning)
            assertEquals(6467, result.state.remainingSeconds)
            assertEquals(733, progress(result.state))
            assertFalse(result.completed)
        }
        val missingAnchor = restore(focus().copy(checkpointBootCount = null), seconds = 86_400)
        assertFalse(missingAnchor.state.isRunning)
        assertEquals(733, progress(missingAnchor.state))
    }

    @Test fun `monotonic clock regression cannot be mistaken for the same boot`() {
        val state = focus()
        val result = recoverEkagraTimer(state, 0L, 7, at + 86_400_000L)
        assertFalse(result.sameBoot)
        assertFalse(result.state.isRunning)
        assertEquals(6467, result.state.remainingSeconds)
    }

    @Test fun `stopwatch continues elapsed time across process death without countdown clamp`() {
        val state = focus().copy(mode = TimerMode.STOPWATCH, totalSeconds = 733, remainingSeconds = 733)
        val result = restore(state, seconds = 7200)
        assertEquals(7933, result.state.remainingSeconds)
        assertEquals(7933, progress(result.state))
        assertTrue(result.state.isRunning)
        assertFalse(result.completed)
    }

    @Test fun `stopwatch overflow saturates instead of becoming negative`() {
        val state = focus().copy(mode = TimerMode.STOPWATCH, remainingSeconds = Int.MAX_VALUE - 1)
        val result = restore(state, seconds = Int.MAX_VALUE.toLong() + 500)
        assertEquals(Int.MAX_VALUE, result.state.remainingSeconds)
        assertTrue(result.state.isRunning)
    }

    @Test fun `automatic pomodoro recovery counts only scheduled focus across later rounds`() {
        val result = restore(pomodoro(), seconds = 178)
        assertEquals(TimerMode.POMODORO, result.state.mode)
        assertEquals(2, result.state.completedPomodoroLoops)
        assertEquals(30, result.state.remainingSeconds)
        assertEquals(150, progress(result.state))
        assertEquals(138, result.state.presenceSeconds)
        assertTrue(result.state.isRunning)
        assertFalse(result.completed)
    }

    @Test fun `automatic pomodoro cannot exceed its configured rounds after a long absence`() {
        val result = restore(pomodoro(), seconds = 86_400)
        assertTrue(result.completed)
        assertEquals(3, result.state.completedPomodoroLoops)
        assertEquals(180, progress(result.state))
        assertEquals(at + 248_000L, result.completedAtMs)
        assertFalse(result.state.isRunning)
    }

    @Test fun `manual pomodoro transition stops at the first unstarted break`() {
        val result = restore(pomodoro().copy(autoStartBreak = false), seconds = 86_400)
        assertEquals(TimerMode.BREAK, result.state.mode)
        assertEquals(1, result.state.completedPomodoroLoops)
        assertEquals(20, result.state.remainingSeconds)
        assertEquals(60, progress(result.state))
        assertFalse(result.state.isRunning)
        assertFalse(result.completed)
    }

    @Test fun `manual pomodoro break never starts an unapproved focus round`() {
        val state = pomodoro().copy(mode = TimerMode.BREAK, totalSeconds = 20, remainingSeconds = 15,
            completedPomodoroLoops = 1, autoStartBreak = false)
        val result = restore(state, seconds = 86_400)
        assertEquals(TimerMode.POMODORO, result.state.mode)
        assertEquals(60, result.state.remainingSeconds)
        assertEquals(60, progress(result.state))
        assertFalse(result.state.isRunning)
    }

    @Test fun `manual break restores suspended focus paused without crediting the break or absent time`() {
        val state = focus().copy(mode = TimerMode.BREAK, totalSeconds = 300, remainingSeconds = 30,
            suspendedTotalSeconds = 7200, suspendedRemainingSeconds = 6600, autoStartBreak = true)
        val result = restore(state, seconds = 3600)
        assertEquals(TimerMode.FOCUS, result.state.mode)
        assertEquals(6600, result.state.remainingSeconds)
        assertEquals(600, progress(result.state))
        assertEquals(0, result.state.suspendedTotalSeconds)
        assertFalse(result.state.isRunning)
        assertFalse(result.completed)
    }

    @Test fun `paused manual break keeps its suspended study checkpoint unchanged`() {
        val state = focus().copy(mode = TimerMode.BREAK, totalSeconds = 300, remainingSeconds = 30,
            suspendedTotalSeconds = 7200, suspendedRemainingSeconds = 6600, isRunning = false)
        val result = restore(state, seconds = 3600)
        assertEquals(state.remainingSeconds, result.state.remainingSeconds)
        assertEquals(state.suspendedRemainingSeconds, result.state.suspendedRemainingSeconds)
        assertFalse(result.periodFinished)
    }

    @Test fun `reboot drops stale elapsed presence and quick unlock deadlines`() {
        val state = focus().copy(presenceDeadlineElapsedMs = 5_000_000L,
            quickUnlockUntilElapsedMs = 4_000_000L, quickUnlockGraceUntilMs = at + 3_000_000L)
        val result = restore(state, seconds = 1000, boot = 8)
        assertTrue(result.state.presenceExpired)
        assertEquals(0L, result.state.presenceDeadlineElapsedMs)
        assertEquals(0L, result.state.quickUnlockUntilElapsedMs)
        assertEquals(0L, result.state.quickUnlockGraceUntilMs)
    }

    @Test fun `invalid pomodoro configuration pauses rather than inventing rounds`() {
        val result = restore(pomodoro().copy(targetPomodoroLoops = Int.MAX_VALUE), seconds = 86_400)
        assertFalse(result.state.isRunning)
        assertEquals(48, result.state.remainingSeconds)
        assertFalse(result.completed)
    }

    @Test fun `already reached running deadline is completed even with no new tick`() {
        val result = restore(focus().copy(remainingSeconds = 0), seconds = 0)
        assertTrue(result.completed)
        assertEquals(at, result.completedAtMs)
        assertEquals(7200, progress(result.state))
    }

    @Test fun `quick unlock gap is excluded and its authorized automatic resume continues study`() {
        val state = focus().copy(isRunning = false,
            quickUnlockUntilElapsedMs = focus().checkpointElapsedMs + 120_000L,
            quickUnlockGraceUntilMs = at + 120_000L)
        val result = restore(state, seconds = 300)
        assertTrue(result.state.isRunning)
        assertEquals(180L, result.elapsedSeconds)
        assertEquals(6287, result.state.remainingSeconds)
        assertEquals(0L, result.state.quickUnlockUntilElapsedMs)
        assertEquals(0L, result.state.quickUnlockGraceUntilMs)
    }

    @Test fun `unexpired quick unlock keeps study paused`() {
        val state = focus().copy(isRunning = false,
            quickUnlockUntilElapsedMs = focus().checkpointElapsedMs + 120_000L)
        val result = restore(state, seconds = 30)
        assertFalse(result.state.isRunning)
        assertEquals(6467, result.state.remainingSeconds)
        assertEquals(state.quickUnlockUntilElapsedMs, result.state.quickUnlockUntilElapsedMs)
    }

    @Test fun `reboot cannot automatically resume an old quick unlock`() {
        val state = focus().copy(isRunning = false,
            quickUnlockUntilElapsedMs = focus().checkpointElapsedMs + 120_000L)
        val result = restore(state, seconds = 3600, boot = 8)
        assertFalse(result.state.isRunning)
        assertEquals(6467, result.state.remainingSeconds)
        assertEquals(0L, result.state.quickUnlockUntilElapsedMs)
    }

    @Test fun `quick unlock completion timestamp includes its uncredited pause`() {
        val state = focus().copy(isRunning = false,
            quickUnlockUntilElapsedMs = focus().checkpointElapsedMs + 120_000L)
        val result = restore(state, seconds = 86_400)
        assertTrue(result.completed)
        assertEquals(at + (120L + 6467L) * 1000L, result.completedAtMs)
        assertEquals(7200, progress(result.state))
    }

    @Test fun `presence deadline crosses only focus time and retains intervening breaks`() {
        val state = pomodoro().copy(presenceSeconds = PRESENCE_REMINDER_INTERVAL_SECONDS - 50)
        val result = restore(state, seconds = 75)
        // 48 focus + 20 break + 2 focus reaches the reminder at checkpoint + 70s.
        assertEquals(state.checkpointElapsedMs + 70_000L + PRESENCE_RESPONSE_WINDOW_MS,
            result.state.presenceDeadlineElapsedMs)
        assertEquals(PRESENCE_REMINDER_INTERVAL_SECONDS + 5, result.state.presenceSeconds)
        assertFalse(result.state.presenceExpired)
    }

    @Test fun `expired presence reminder never pauses personal study on process recovery`() {
        val state = focus().copy(mode = TimerMode.STOPWATCH, remainingSeconds = 100,
            presenceSeconds = PRESENCE_REMINDER_INTERVAL_SECONDS - 10)
        val result = restore(state, seconds = 4000)
        assertTrue(result.state.presenceExpired)
        assertEquals(0L, result.state.presenceDeadlineElapsedMs)
        assertTrue(result.state.isRunning)
        assertEquals(4100, result.state.remainingSeconds)
    }

    @Test fun `paused recovery expires an existing monotonic presence deadline`() {
        val state = focus().copy(isRunning = false,
            presenceDeadlineElapsedMs = focus().checkpointElapsedMs + 30_000L)
        val result = restore(state, seconds = 60)
        assertTrue(result.state.presenceExpired)
        assertEquals(0L, result.state.presenceDeadlineElapsedMs)
        assertFalse(result.state.isRunning)
        assertEquals(6467, result.state.remainingSeconds)
    }

}
