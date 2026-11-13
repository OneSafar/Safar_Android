package com.safarparmar.app.feature.habits.timetable

import java.time.LocalDate
import java.time.LocalTime

/** Adjacent intervals are valid: an item occupies only its own half-open range. */
internal fun timetableConflicts(candidate: TimetableSession, sessions: List<TimetableSession>): List<TimetableSession> {
    val first = LocalDate.parse(candidate.date)
    return sessions.filter { other ->
        other.id != candidate.id && candidate.startMinute < other.endMinute && other.startMinute < candidate.endMinute &&
            // Repeating schedules have weekly periods; one week beyond the later start covers every pairing.
            (0L..7L).any { offset ->
                val day = maxOf(first, LocalDate.parse(other.date)).plusDays(offset)
                candidate.occursOn(day) && other.occursOn(day)
            }
    }
}

internal fun suggestTimetableSession(day: LocalDate, sessions: List<TimetableSession>, now: LocalTime = LocalTime.now()): TimetableSession {
    val earliest = if (day == LocalDate.now()) ((now.hour * 60 + now.minute + 14) / 15 * 15).coerceAtMost(1379) else 480
    val occupied = sessions.filter { it.occursOn(day) }
    val starts = (earliest..1379 step 15).toList() + (0 until earliest step 15).filter { it <= 1379 }
    val start = starts.firstOrNull { minute -> occupied.none { minute < it.endMinute && it.startMinute < minute + 60 } } ?: earliest
    return TimetableSession(date = day.toString(), startMinute = start, endMinute = start + 60)
}

internal fun sessionTimeRange(session: TimetableSession): String = "${sessionTime(session.startMinute)}–${sessionTime(session.endMinute)}"
