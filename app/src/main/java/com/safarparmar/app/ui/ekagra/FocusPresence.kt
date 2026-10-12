package com.safarparmar.app.ui.ekagra

internal const val PRESENCE_INTERVAL_SECONDS = 90 * 60
internal const val PRESENCE_GRACE_MS = 2 * 60 * 1000L
internal data class PresenceAdvance(val creditedSeconds: Int, val deadline: Long, val expired: Boolean)

internal fun advancePresence(activeSeconds: Int, deadline: Long, elapsedSeconds: Int, now: Long, remainingSeconds: Int = Int.MAX_VALUE): PresenceAdvance {
    val elapsed = elapsedSeconds.coerceIn(0, remainingSeconds.coerceAtLeast(0))
    val untilPrompt = (PRESENCE_INTERVAL_SECONDS - activeSeconds).coerceAtLeast(0)
    val nextDeadline = if (deadline > 0) deadline else if (elapsed >= untilPrompt && remainingSeconds > untilPrompt)
        now - (elapsedSeconds - untilPrompt) * 1000L + PRESENCE_GRACE_MS else 0L
    val expired = nextDeadline > 0 && now >= nextDeadline
    val credited = if (expired) (elapsedSeconds - ((now - nextDeadline + 999) / 1000).toInt()).coerceIn(0, elapsed) else elapsed
    return PresenceAdvance(credited, nextDeadline, expired)
}
