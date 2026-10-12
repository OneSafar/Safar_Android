package com.safarparmar.app.feature.habits.ui

import com.safarparmar.app.feature.habits.data.HabitEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** A real Monday-first calendar, including empty leading/trailing positions. */
internal fun monthCells(month: LocalDate): List<LocalDate?> {
    val first = month.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1
    val dates = List(first.lengthOfMonth()) { first.plusDays(it.toLong()) }
    val trailing = (7 - (leading + dates.size) % 7) % 7
    return List(leading) { null } + dates + List(trailing) { null }
}

internal data class HabitDayState(val completed: Boolean, val scheduled: Boolean, val future: Boolean) {
    // Preserve the ability to undo legacy entries made on off days or future dates.
    val editable: Boolean get() = completed || (scheduled && !future)
    val description: String get() = when {
        completed -> "Completed"
        !scheduled -> "Not scheduled"
        future -> "Upcoming"
        else -> "Not completed"
    }
}

internal fun habitDayState(habit: HabitEntity, date: LocalDate, today: LocalDate, completed: Boolean) =
    HabitDayState(completed, date.dayOfWeek in habit.targetDays, date.isAfter(today))

internal fun habitBadgeIndex(id: Long, count: Int): Int = Math.floorMod(id, count.toLong()).toInt()

/**
 * BUG-12 FIX: Guard for empty targetDays set. Returns "No days set" instead of crashing
 * or showing an empty Summary label in the form.
 */
internal fun scheduleSummary(days: Set<DayOfWeek>): String = when {
    days.isEmpty() -> "No days set"
    days.size == 7 -> "Every day"
    else -> days.sortedBy { it.value }.joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

internal fun habitCreatedMessage(name: String, days: Set<DayOfWeek>, today: LocalDate): String {
    if (days.isEmpty()) return "Added \u201c$name\u201d."
    if (today.dayOfWeek in days) return "Added \u201c$name\u201d to today."
    val next = (1L..7L).map(today::plusDays).first { it.dayOfWeek in days }
    return "Added \u201c$name\u201d. Next scheduled: ${next.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))}."
}

/** Returns the "Next habit: [name] on [day]" hint text for the Today empty state. */
internal fun nextScheduledHint(habits: List<HabitEntity>, today: LocalDate): String? {
    if (habits.isEmpty()) return null
    // Look ahead up to 6 days
    val nextDate = (1L..6L).map(today::plusDays).firstOrNull { d ->
        habits.any { h -> d.dayOfWeek in h.targetDays }
    } ?: return null
    val habitsOnNext = habits.filter { nextDate.dayOfWeek in it.targetDays }
    val dayLabel = if (nextDate == today.plusDays(1)) "Tomorrow"
    else nextDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    val habitLabel = when {
        habitsOnNext.size == 1 -> habitsOnNext.first().name
        habitsOnNext.size <= 3 -> habitsOnNext.joinToString(", ") { it.name }
        else -> "${habitsOnNext.size} habits"
    }
    return "Next up: $habitLabel — $dayLabel"
}
