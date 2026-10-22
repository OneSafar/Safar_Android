package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.data.*
import com.safarparmar.app.feature.habits.reminders.HabitReminderPolicy as Policy
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class HabitReminderPolicyTest {
    private val monday = ZonedDateTime.parse("2026-10-19T07:00:00+05:30[Asia/Kolkata]")
    private val habit = HabitRemoteRecord("read", name = "Read", isEveryDay = true, reminderTime = "08:30")

    @Test fun schedulesTodayBeforeTimeAndTomorrowAfterTime() {
        assertEquals("2026-10-19T08:30+05:30[Asia/Kolkata]", Policy.next(habit, monday).toString())
        assertEquals(LocalDate.of(2026, 10, 20), Policy.next(habit, monday.plusHours(2))!!.toLocalDate())
    }
    @Test fun followsSelectedWeekdays() {
        val weekly = habit.copy(isEveryDay = false, targetDays = listOf(7))
        assertEquals(LocalDate.of(2026, 10, 25), Policy.next(weekly, monday)!!.toLocalDate())
    }
    @Test fun completionSkipsTodayButUndoRestoresToday() {
        val done = habit.copy(completions = listOf(HabitRemoteCompletion("2026-10-19", true)))
        assertEquals(monday.toLocalDate().plusDays(1), Policy.next(done, monday)!!.toLocalDate())
        assertEquals(monday.toLocalDate(), Policy.next(done.copy(completions = listOf(HabitRemoteCompletion("2026-10-19", false))), monday)!!.toLocalDate())
    }
    @Test fun respectsStartDateAndFutureScheduleRevisions() {
        val future = habit.copy(scheduledSince = "2026-11-01", isEveryDay = false, targetDays = emptyList(),
            revisions = listOf(HabitRemoteRevision("2026-11-16", listOf(1), false)))
        assertEquals(LocalDate.of(2026, 11, 16), Policy.next(future, monday)!!.toLocalDate())
    }
    @Test fun revisionChangesRecurrenceWithoutChangingEarlierDates() {
        val revised = habit.copy(revisions = listOf(HabitRemoteRevision("2026-10-20", listOf(5), false)))
        assertTrue(Policy.scheduled(revised, monday.toLocalDate()))
        assertFalse(Policy.scheduled(revised, monday.toLocalDate().plusDays(1)))
        assertEquals(LocalDate.of(2026, 10, 23), Policy.next(revised, monday.plusDays(1))!!.toLocalDate())
    }
    @Test fun disabledDeletedArchivedAndUnconfiguredHabitsNeverSchedule() {
        listOf(habit.copy(isActive = false), habit.copy(deleted = true), habit.copy(archivedAt = "2026-10-19"),
            habit.copy(reminderTime = null), habit.copy(isEveryDay = false, targetDays = emptyList())).forEach {
            assertNull(Policy.next(it, monday))
            assertFalse(Policy.due(it, "2026-10-19", monday.plusHours(2), null))
        }
    }
    @Test fun rejectsInvalidOrNonCanonicalTimes() {
        listOf("24:00", "29:12", "8:30", "08:70", "08:30:00", "bad", "").forEach {
            assertNull(Policy.next(habit.copy(reminderTime = it), monday))
        }
        assertNull(Policy.next(habit.copy(scheduledSince = "bad"), monday))
    }
    @Test fun suppressesDuplicateAndPreviousDayDelayedAlarms() {
        val after = monday.plusHours(2)
        assertTrue(Policy.due(habit, "2026-10-19", after, null))
        assertFalse(Policy.due(habit, "2026-10-18", after, null))
        assertFalse(Policy.due(habit, "2026-10-19", after, "2026-10-19"))
        assertFalse(Policy.due(habit, "2026-10-19", monday, null))
        assertEquals(monday.toLocalDate().plusDays(1), Policy.next(habit, monday, "2026-10-19")!!.toLocalDate())
    }
    @Test fun daylightSavingGapMovesToValidLocalTime() {
        val now = ZonedDateTime.parse("2026-03-08T00:00:00-05:00[America/New_York]")
        val next = Policy.next(habit.copy(reminderTime = "02:30"), now)!!
        assertEquals(LocalTime.of(3, 30), next.toLocalTime())
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), next.toInstant())
    }
    @Test fun daylightSavingOverlapDoesNotProduceTwoReminders() {
        val now = ZonedDateTime.parse("2026-11-01T00:00:00-04:00[America/New_York]")
        val first = Policy.next(habit.copy(reminderTime = "01:30"), now)!!
        val second = Policy.next(habit.copy(reminderTime = "01:30"), first.plusMinutes(1))!!
        assertEquals(LocalDate.of(2026, 11, 2), second.toLocalDate())
    }
    @Test fun timezoneChangeKeepsChosenLocalTime() {
        val changed = monday.withZoneSameInstant(ZoneId.of("Europe/London"))
        assertEquals(LocalTime.of(8, 30), Policy.next(habit, changed)!!.toLocalTime())
        assertEquals(ZoneId.of("Europe/London"), Policy.next(habit, changed)!!.zone)
    }
    @Test fun skipsCompletedWeeklyOccurrencesBeyondFirstWeek() {
        val weekly = habit.copy(isEveryDay = false, targetDays = listOf(1), completions = listOf(
            HabitRemoteCompletion("2026-10-19", true), HabitRemoteCompletion("2026-10-26", true)))
        assertEquals(LocalDate.of(2026, 11, 2), Policy.next(weekly, monday)!!.toLocalDate())
    }

}
