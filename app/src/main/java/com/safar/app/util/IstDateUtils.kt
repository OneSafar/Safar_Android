package com.safar.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object IstDateUtils {
    val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    fun todayKey(): String = getDateKey(Instant.now())

    fun getDateKey(instant: Instant): String =
        instant.atZone(zone).toLocalDate().toString()

    fun getDateKey(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(raw.take(10))) return raw.take(10)
        return runCatching { getDateKey(Instant.parse(raw)) }.getOrNull()
    }

    fun isoFromDateAndTime(date: LocalDate, time: LocalTime): String =
        ZonedDateTime.of(date, time, zone).toInstant().toString()

    fun dateKeyToUtcIso(dateKey: String): String = "${dateKey}T00:00:00.000Z"

    fun labelFor(dateKeyOrIso: String?): String {
        val key = getDateKey(dateKeyOrIso) ?: return ""
        val today = LocalDate.parse(todayKey())
        val date = LocalDate.parse(key)
        val diff = date.toEpochDay() - today.toEpochDay()
        return when {
            diff == 0L -> "Today"
            diff == 1L -> "Tomorrow"
            diff == -1L -> "Yesterday"
            kotlin.math.abs(diff) < 7 -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
        }
    }
}
