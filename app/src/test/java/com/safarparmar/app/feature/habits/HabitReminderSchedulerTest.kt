package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitScheduleRevisionEntity
import com.safarparmar.app.feature.habits.reminders.HabitReminderScheduler
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class HabitReminderSchedulerTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val monday = LocalDate.of(2026, 9, 21)
    private val habit = HabitEntity(id = 1, name = "Read", remoteId = "habit-id",
        scheduledSince = monday, targetDays = setOf(DayOfWeek.MONDAY),
        isEveryDay = false, reminderTime = "08:30")

    @Test fun `next reminder repeats on the next scheduled day after todays time`() {
        val before = ZonedDateTime.of(2026, 9, 21, 8, 0, 0, 0, zone)
        assertEquals(monday.atTime(8, 30).atZone(zone),
            HabitReminderScheduler.nextOccurrence(habit, emptyList(), before))
        assertEquals(monday.plusWeeks(1).atTime(8, 30).atZone(zone),
            HabitReminderScheduler.nextOccurrence(habit, emptyList(), before.plusHours(1)))
    }

    @Test fun `schedule revisions select a new weekday without rewriting prior days`() {
        val revision = HabitScheduleRevisionEntity(habitId = 1, effectiveFrom = monday.plusDays(1),
            targetDays = setOf(DayOfWeek.WEDNESDAY), isEveryDay = false)
        val after = monday.atTime(9, 0).atZone(zone)
        assertEquals(monday.plusDays(2).atTime(8, 30).atZone(zone),
            HabitReminderScheduler.nextOccurrence(habit, listOf(revision), after))
    }

    @Test fun `archived and disabled reminders never schedule`() {
        val after = monday.atStartOfDay(zone)
        assertNull(HabitReminderScheduler.nextOccurrence(habit.copy(isArchived = true), emptyList(), after))
        assertNull(HabitReminderScheduler.nextOccurrence(habit.copy(reminderTime = null), emptyList(), after))
    }

    @Test fun `local wall clock time follows the device timezone`() {
        val after = monday.atStartOfDay(ZoneId.of("Europe/London"))
        assertEquals(8, HabitReminderScheduler.nextOccurrence(habit, emptyList(), after)!!.hour)
        assertEquals(30, HabitReminderScheduler.nextOccurrence(habit, emptyList(), after)!!.minute)
    }
}
