package com.safarparmar.app.ui.ekagra

import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import java.time.Duration
import java.time.Instant

internal data class FocusProgressSnapshot(
    val plannedSeconds: Int,
    val actualSeconds: Int,
)

/**
 * Converts the timer's current period into progress for the whole logical session.
 * Pomodoro's completedLoops deliberately excludes the currently running focus period.
 */
internal fun calculateFocusProgress(
    mode: TimerMode,
    currentPeriodTotalSeconds: Int,
    currentPeriodRemainingSeconds: Int,
    pomodoroFocusSeconds: Int,
    targetPomodoroLoops: Int,
    completedPomodoroLoops: Int,
): FocusProgressSnapshot {
    if (mode == TimerMode.STOPWATCH) {
        val elapsed = currentPeriodRemainingSeconds.coerceAtLeast(0)
        return FocusProgressSnapshot(plannedSeconds = elapsed, actualSeconds = elapsed)
    }

    val isPomodoroSession = targetPomodoroLoops > 0 || completedPomodoroLoops > 0 || mode == TimerMode.POMODORO
    if (isPomodoroSession) {
        val focusSeconds = pomodoroFocusSeconds.coerceAtLeast(1)
        val completed = completedPomodoroLoops.coerceAtLeast(0)
        val minimumLoops = completed + if (mode == TimerMode.POMODORO) 1 else 0
        val target = targetPomodoroLoops.coerceAtLeast(minimumLoops).coerceAtLeast(1)
        val completedWithinTarget = completed.coerceAtMost(target)
        val currentLoopElapsed = if (mode == TimerMode.POMODORO && completedWithinTarget < target) {
            (currentPeriodTotalSeconds - currentPeriodRemainingSeconds)
                .coerceIn(0, focusSeconds)
        } else {
            0
        }
        val planned = multiplySecondsSafely(focusSeconds, target)
        val actual = (multiplySecondsSafely(focusSeconds, completedWithinTarget).toLong() + currentLoopElapsed)
            .coerceIn(0L, planned.toLong())
            .toInt()
        return FocusProgressSnapshot(plannedSeconds = planned, actualSeconds = actual)
    }

    val planned = currentPeriodTotalSeconds.coerceAtLeast(0)
    val actual = (planned - currentPeriodRemainingSeconds).coerceIn(0, planned)
    return FocusProgressSnapshot(plannedSeconds = planned, actualSeconds = actual)
}

internal fun EkagraAnalyticsFocusSession.toPendingEndedSession(): PendingEndedEkagraSession {
    val isStopwatch = timerMode.equals("stopwatch", ignoreCase = true)
    val exactSeconds = exactActualSeconds()
    val plannedSeconds = if (isStopwatch) {
        exactSeconds
    } else {
        (durationMinutes.coerceAtLeast(0) * 60).coerceAtLeast(exactSeconds)
    }
    val remainingSeconds = if (isStopwatch) {
        exactSeconds
    } else {
        (plannedSeconds - exactSeconds).coerceAtLeast(0)
    }

    return PendingEndedEkagraSession(
        sessionId = id,
        totalSeconds = plannedSeconds,
        secondsLeft = remainingSeconds,
        mode = if (isStopwatch) "stopwatch" else "Timer",
        startedAt = startedAt,
        endedAt = endedAt,
    )
}

private fun EkagraAnalyticsFocusSession.exactActualSeconds(): Int {
    if (actualSeconds > 0) return actualSeconds
    val elapsedFromTimestamps = runCatching {
        val start = Instant.parse(startedAt)
        val end = Instant.parse(endedAt)
        Duration.between(start, end).seconds.coerceAtLeast(0).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }.getOrNull()
    return elapsedFromTimestamps ?: (actualMinutes.coerceAtLeast(0) * 60)
}

private fun multiplySecondsSafely(seconds: Int, count: Int): Int =
    (seconds.toLong() * count.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
