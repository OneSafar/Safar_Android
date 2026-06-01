package com.safarparmar.app.notifications

import java.time.LocalTime

internal object QuietHoursEvaluator {
    private val EXEMPT_CHANNELS = setOf(
        SafarNotificationChannels.ACCOUNT_SYSTEM,
        SafarNotificationChannels.FOCUS_TIMER,
        SafarNotificationChannels.FOCUS_SHIELD_STATUS,
        SafarNotificationChannels.FOCUS_SHIELD_BLOCKED,
    )

    internal var quietHoursNow: () -> LocalTime = { LocalTime.now() }

    fun shouldSuppress(
        channelId: String,
        quietStart: LocalTime,
        quietEnd: LocalTime,
        now: LocalTime = quietHoursNow(),
    ): Boolean {
        if (channelId in EXEMPT_CHANNELS) return false

        if (quietStart == quietEnd) return false

        val inQuietHours = if (quietStart < quietEnd) {
            now >= quietStart && now < quietEnd
        } else {
            // Window crosses midnight, e.g. 22:00..07:00.
            now >= quietStart || now < quietEnd
        }
        return inQuietHours
    }
}
