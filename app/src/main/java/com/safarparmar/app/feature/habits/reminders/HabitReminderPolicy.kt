package com.safarparmar.app.feature.habits.reminders

import com.safarparmar.app.feature.habits.data.HabitRemoteRecord
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** The same dated recurrence as the tracker; wall-clock times follow the device time zone. */
object HabitReminderPolicy {
    fun time(habit: HabitRemoteRecord): LocalTime? = habit.reminderTime
        ?.takeIf { it.matches(Regex("[0-2][0-9]:[0-5][0-9]")) }
        ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

    fun scheduled(habit: HabitRemoteRecord, date: LocalDate): Boolean {
        if (!habit.isActive || habit.deleted || habit.archivedAt != null || time(habit) == null) return false
        val since = runCatching { LocalDate.parse(habit.scheduledSince) }.getOrNull() ?: return false
        if (date < since) return false
        val revision = habit.revisions.filter { it.effectiveFrom <= date.toString() }.maxByOrNull { it.effectiveFrom }
        val everyDay = revision?.isEveryDay ?: habit.isEveryDay
        val days = revision?.targetDays ?: habit.targetDays
        return (everyDay || date.dayOfWeek.value in days) &&
            habit.completions.none { it.date == date.toString() && it.completed }
    }

    fun next(habit: HabitRemoteRecord, now: ZonedDateTime, deliveredDate: String? = null): ZonedDateTime? {
        val time = time(habit) ?: return null
        val since = runCatching { LocalDate.parse(habit.scheduledSince) }.getOrNull() ?: return null
        val start = maxOf(since, now.toLocalDate())
        // A disabled weekly schedule may have a future revision that enables it again.
        val starts = listOf(start) + habit.revisions.mapNotNull {
            runCatching { LocalDate.parse(it.effectiveFrom) }.getOrNull()?.takeIf { date -> date > start }
        } + habit.completions.filter { it.completed }.mapNotNull {
            runCatching { LocalDate.parse(it.date).plusDays(1) }.getOrNull()?.takeIf { date -> date > start }
        }
        return starts.flatMap { date -> (0L..7L).map { date.plusDays(it) } }.distinct()
            .filter { it.toString() != deliveredDate && scheduled(habit, it) }
            .map { it.atTime(time).atZone(now.zone) }
            .filter { it.toInstant() > now.toInstant() }.minByOrNull { it.toInstant() }
    }

    fun due(habit: HabitRemoteRecord, date: String, now: ZonedDateTime, deliveredDate: String?): Boolean {
        if (date != now.toLocalDate().toString() || date == deliveredDate) return false
        return scheduled(habit, now.toLocalDate()) &&
            !now.toInstant().isBefore(now.toLocalDate().atTime(time(habit)).atZone(now.zone).toInstant())
    }
}
