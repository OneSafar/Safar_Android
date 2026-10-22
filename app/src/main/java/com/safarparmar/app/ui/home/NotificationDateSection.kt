package com.safarparmar.app.ui.home

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

internal enum class NotificationDateSection { TODAY, THIS_WEEK, EARLIER }

/** Match the timezone used by notification timestamps; unknown dates stay visible. */
internal fun notificationDateSection(timestamp: String, today: LocalDate): NotificationDateSection {
    val date = runCatching {
        ZonedDateTime.parse(timestamp).withZoneSameInstant(ZoneId.of("Asia/Kolkata")).toLocalDate()
    }.getOrNull() ?: return NotificationDateSection.EARLIER
    return when {
        date == today -> NotificationDateSection.TODAY
        date < today && date >= today.minusDays(6) -> NotificationDateSection.THIS_WEEK
        else -> NotificationDateSection.EARLIER
    }
}
