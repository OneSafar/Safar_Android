package com.safarparmar.app.ui.ekagra

internal const val PRESENCE_REMINDER_INTERVAL_SECONDS = 4 * 60 * 60
internal const val PRESENCE_RESPONSE_WINDOW_MS = 30 * 60_000L
internal const val MAX_EKAGRA_SESSION_SECONDS = 18 * 60 * 60
internal data class PresenceReminderAdvance(val secondsSinceReminder: Int, val reminderDue: Boolean)

internal fun configuredTimerSeconds(mode: TimerMode, requestedSeconds: Int): Int = when (mode) {
    TimerMode.STOPWATCH -> 0
    TimerMode.FOCUS, TimerMode.POMODORO -> requestedSeconds.coerceIn(0, MAX_EKAGRA_SESSION_SECONDS)
    TimerMode.BREAK -> requestedSeconds.coerceAtLeast(0)
}

internal fun advancePresenceReminder(activeSeconds: Int, elapsedSeconds: Int): PresenceReminderAdvance {
    val accumulated = activeSeconds.coerceAtLeast(0) + elapsedSeconds.coerceAtLeast(0)
    return PresenceReminderAdvance(
        secondsSinceReminder = accumulated % PRESENCE_REMINDER_INTERVAL_SECONDS,
        reminderDue = accumulated >= PRESENCE_REMINDER_INTERVAL_SECONDS,
    )
}

/** Stopwatch stores elapsed seconds, which must never be clamped to a countdown target. */
internal fun restoredTimerSeconds(mode: TimerMode, savedSeconds: Int, totalSeconds: Int): Int =
    if (mode == TimerMode.STOPWATCH) savedSeconds.coerceAtLeast(0)
    else savedSeconds.coerceIn(0, totalSeconds.coerceAtLeast(0))
