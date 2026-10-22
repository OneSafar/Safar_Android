package com.safarparmar.app.ui.ekagra

/**
 * Local-only timer state committed in the same journal row as its saved focus progress.
 * elapsedRealtime and boot count make a running timer survive process death without
 * treating changes to the phone's wall clock, or powered-off time, as study time.
 */
@androidx.annotation.Keep
data class EkagraTimerRecoveryState(
    val mode: TimerMode,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val checkpointElapsedMs: Long,
    val checkpointBootCount: Int?,
    val checkpointAtMs: Long,
    val checkpointSequence: Long = 0,
    val suspendedTotalSeconds: Int = 0,
    val suspendedRemainingSeconds: Int = 0,
    val standardBreakSeconds: Int = 300,
    val targetPomodoroLoops: Int = 0,
    val completedPomodoroLoops: Int = 0,
    val pomodoroFocusSeconds: Int = 1500,
    val pomodoroBreakSeconds: Int = 300,
    val pomodoroFinalBreakSeconds: Int = 900,
    val pomodoroCompletesAfterCurrentBreak: Boolean = false,
    val autoStartBreak: Boolean = false,
    val presenceSeconds: Int = 0,
    val presenceDeadlineElapsedMs: Long = 0,
    val presenceExpired: Boolean = false,
    val presencePromptDismissed: Boolean = false,
    val quickUnlockUntilElapsedMs: Long = 0,
    val quickUnlockGraceUntilMs: Long = 0,
)

internal data class EkagraTimerRecoveryResult(
    val state: EkagraTimerRecoveryState,
    val sameBoot: Boolean,
    val elapsedSeconds: Long,
    /** The logical focus session is complete; its existing journal identity can be finalized. */
    val completed: Boolean,
    val completedAtMs: Long?,
    val periodFinished: Boolean,
)

/**
 * Advance only periods which were authorized to run. Automatic Pomodoro transitions
 * follow the saved configuration; manual breaks restore their suspended focus paused.
 * Paused checkpoints and checkpoints from another boot never earn a missing-time gap.
 */
internal fun recoverEkagraTimer(
    checkpoint: EkagraTimerRecoveryState,
    nowElapsedMs: Long,
    currentBootCount: Int?,
    nowAtMs: Long,
): EkagraTimerRecoveryResult {
    val sameBoot = checkpoint.checkpointBootCount != null && currentBootCount != null &&
        checkpoint.checkpointBootCount >= 0 && checkpoint.checkpointBootCount == currentBootCount &&
        checkpoint.checkpointElapsedMs >= 0L && nowElapsedMs >= checkpoint.checkpointElapsedMs
    val quickUnlockResumeDue = sameBoot && !checkpoint.isRunning &&
        checkpoint.mode != TimerMode.BREAK &&
        checkpoint.quickUnlockUntilElapsedMs > 0L && nowElapsedMs >= checkpoint.quickUnlockUntilElapsedMs
    val advanceStartedElapsedMs = if (quickUnlockResumeDue) {
        maxOf(checkpoint.checkpointElapsedMs, checkpoint.quickUnlockUntilElapsedMs)
    } else checkpoint.checkpointElapsedMs
    val elapsed = if (sameBoot && (checkpoint.isRunning || quickUnlockResumeDue)) {
        (nowElapsedMs - advanceStartedElapsedMs) / 1000L
    } else 0L
    val previousPresenceExpired = checkpoint.presenceExpired || (checkpoint.presenceDeadlineElapsedMs > 0L &&
        (!sameBoot || nowElapsedMs >= checkpoint.presenceDeadlineElapsedMs))
    var state = checkpoint.copy(
        totalSeconds = checkpoint.totalSeconds.coerceAtLeast(0),
        remainingSeconds = restoredTimerSeconds(checkpoint.mode, checkpoint.remainingSeconds, checkpoint.totalSeconds),
        isRunning = (checkpoint.isRunning || quickUnlockResumeDue) && sameBoot,
        checkpointElapsedMs = nowElapsedMs,
        checkpointBootCount = currentBootCount,
        checkpointAtMs = nowAtMs,
        presenceDeadlineElapsedMs = if (sameBoot && !previousPresenceExpired) checkpoint.presenceDeadlineElapsedMs else 0L,
        presenceExpired = previousPresenceExpired,
        quickUnlockUntilElapsedMs = if (sameBoot && !quickUnlockResumeDue) checkpoint.quickUnlockUntilElapsedMs else 0L,
        quickUnlockGraceUntilMs = if (sameBoot && !quickUnlockResumeDue) checkpoint.quickUnlockGraceUntilMs else 0L,
    )
    if (!state.isRunning) return EkagraTimerRecoveryResult(state, sameBoot, elapsed, false, null, false)

    if (state.mode == TimerMode.STOPWATCH) {
        val seconds = addSeconds(state.remainingSeconds, elapsed)
        state = state.copy(
            totalSeconds = seconds,
            remainingSeconds = seconds,
        )
        state = advanceRecoveryPresence(state, elapsed, advanceStartedElapsedMs, nowElapsedMs)
        return EkagraTimerRecoveryResult(state, sameBoot, elapsed, false, null, false)
    }

    // All supported custom/traditional configurations have at most twelve rounds.
    // Malformed state must not invent arbitrary extra rounds or enter an unbounded loop.
    if (state.targetPomodoroLoops !in 0..12 || state.completedPomodoroLoops !in 0..state.targetPomodoroLoops ||
        (state.targetPomodoroLoops > 0 && (state.pomodoroFocusSeconds <= 0 || state.pomodoroBreakSeconds <= 0 || state.pomodoroFinalBreakSeconds <= 0))) {
        return EkagraTimerRecoveryResult(state.copy(isRunning = false), sameBoot, 0, false, null, false)
    }

    var gap = elapsed
    var consumed = 0L
    var periodFinished = false
    var completed = false
    var completedAtMs: Long? = null
    while (state.isRunning) {
        val advance = minOf(gap, state.remainingSeconds.toLong())
        if (state.mode != TimerMode.BREAK) {
            state = advanceRecoveryPresence(state, advance,
                addMillis(advanceStartedElapsedMs, consumed * 1000L), nowElapsedMs)
        }
        state = state.copy(remainingSeconds = (state.remainingSeconds - advance).toInt())
        gap -= advance
        consumed += advance
        if (state.remainingSeconds > 0) break
        periodFinished = true

        when {
            state.mode == TimerMode.BREAK && state.suspendedTotalSeconds > 0 -> {
                state = state.copy(
                    mode = if (state.targetPomodoroLoops > 0) TimerMode.POMODORO else TimerMode.FOCUS,
                    totalSeconds = state.suspendedTotalSeconds,
                    remainingSeconds = state.suspendedRemainingSeconds.coerceIn(0, state.suspendedTotalSeconds),
                    suspendedTotalSeconds = 0,
                    suspendedRemainingSeconds = 0,
                    isRunning = false,
                )
            }
            state.mode == TimerMode.FOCUS || (state.mode == TimerMode.POMODORO && state.targetPomodoroLoops == 0) -> {
                completed = true
                state = state.copy(isRunning = false)
            }
            state.mode == TimerMode.POMODORO -> {
                val rounds = (state.completedPomodoroLoops + 1).coerceAtMost(state.targetPomodoroLoops)
                val transition = pomodoroBreakTransition(rounds, state.targetPomodoroLoops,
                    state.pomodoroBreakSeconds, state.pomodoroFinalBreakSeconds)
                state = state.copy(
                    mode = TimerMode.BREAK,
                    totalSeconds = transition.breakSeconds,
                    remainingSeconds = transition.breakSeconds,
                    completedPomodoroLoops = rounds,
                    pomodoroCompletesAfterCurrentBreak = transition.completesAfterBreak,
                    isRunning = state.autoStartBreak,
                )
            }
            state.mode == TimerMode.BREAK && state.targetPomodoroLoops > 0 -> {
                if (state.pomodoroCompletesAfterCurrentBreak || state.completedPomodoroLoops >= state.targetPomodoroLoops) {
                    completed = true
                    state = state.copy(isRunning = false)
                } else {
                    state = state.copy(
                        mode = TimerMode.POMODORO,
                        totalSeconds = state.pomodoroFocusSeconds,
                        remainingSeconds = state.pomodoroFocusSeconds,
                        isRunning = state.autoStartBreak,
                    )
                }
            }
            else -> state = state.copy(isRunning = false)
        }
        if (completed) {
            val pausedGapMs = (advanceStartedElapsedMs - checkpoint.checkpointElapsedMs).coerceAtLeast(0L)
            completedAtMs = addMillis(addMillis(checkpoint.checkpointAtMs, pausedGapMs), consumed * 1000L)
            // The end timestamp belongs to the actual timer boundary, even if the app
            // returns hours later or the user has changed the wall clock in between.
            state = state.copy(checkpointAtMs = completedAtMs)
        }
        if (gap == 0L) break
    }
    return EkagraTimerRecoveryResult(state, sameBoot, elapsed, completed, completedAtMs, periodFinished)
}

private fun addSeconds(current: Int, extra: Long): Int =
    (current.coerceAtLeast(0).toLong() + extra.coerceAtMost(Int.MAX_VALUE.toLong()))
        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

private fun addMillis(current: Long, extra: Long): Long =
    if (current > Long.MAX_VALUE - extra) Long.MAX_VALUE else current + extra

private fun advanceRecoveryPresence(
    state: EkagraTimerRecoveryState,
    focusSeconds: Long,
    focusStartedElapsedMs: Long,
    nowElapsedMs: Long,
): EkagraTimerRecoveryState {
    var deadline = state.presenceDeadlineElapsedMs
    if (deadline == 0L && !state.presenceExpired) {
        val beforeReminder = (PRESENCE_REMINDER_INTERVAL_SECONDS - state.presenceSeconds.coerceAtLeast(0)).coerceAtLeast(0).toLong()
        if (focusSeconds >= beforeReminder) {
            deadline = addMillis(addMillis(focusStartedElapsedMs, beforeReminder * 1000L), PRESENCE_RESPONSE_WINDOW_MS)
        }
    }
    val expired = state.presenceExpired || (deadline > 0L && nowElapsedMs >= deadline)
    return state.copy(
        presenceSeconds = addSeconds(state.presenceSeconds, focusSeconds),
        presenceDeadlineElapsedMs = if (expired) 0L else deadline,
        presenceExpired = expired,
    )
}
