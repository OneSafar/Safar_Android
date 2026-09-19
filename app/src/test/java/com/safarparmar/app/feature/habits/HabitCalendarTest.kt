package com.safarparmar.app.feature.habits

import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.ui.*
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitCalendarTest {
    @Test fun `calendar aligns weekdays including leap years and six row months`() {
        listOf("2024-02-01", "2026-02-01", "2026-03-01", "2026-04-01", "2026-09-01").forEach {
            val month = LocalDate.parse(it)
            val cells = monthCells(month)
            assertEquals(0, cells.size % 7)
            assertEquals(month.lengthOfMonth(), cells.filterNotNull().size)
            cells.forEachIndexed { i, day -> if (day != null) assertEquals(day.dayOfWeek.value - 1, i % 7) }
            assertEquals(month, cells.filterNotNull().first())
            assertEquals(month.withDayOfMonth(month.lengthOfMonth()), cells.filterNotNull().last())
        }
        assertEquals(42, monthCells(LocalDate.of(2026, 3, 1)).size)
    }
    @Test fun `off days and future days cannot be newly completed`() {
        val today = LocalDate.of(2026, 9, 14)
        val habit = HabitEntity(id = 5, name = "Read", targetDays = setOf(DayOfWeek.MONDAY))
        assertTrue(habitDayState(habit, today, today, false).editable)
        assertFalse(habitDayState(habit, today.minusDays(1), today, false).editable)
        assertEquals("Not scheduled", habitDayState(habit, today.minusDays(1), today, false).description)
        assertFalse(habitDayState(habit, today.plusWeeks(1), today, false).editable)
        assertEquals("Upcoming", habitDayState(habit, today.plusWeeks(1), today, false).description)
    }
    @Test fun `legacy completions can still be undone on future or off days`() {
        val today = LocalDate.of(2026, 9, 14)
        val habit = HabitEntity(name = "Read", targetDays = setOf(DayOfWeek.MONDAY))
        assertTrue(habitDayState(habit, today.plusDays(1), today, true).editable)
        assertEquals("Completed", habitDayState(habit, today.plusDays(1), today, true).description)
    }
    @Test fun `creation feedback explains why a habit is absent today`() {
        val today = LocalDate.of(2026, 9, 14)
        assertTrue(habitCreatedMessage("Run", setOf(DayOfWeek.SATURDAY), today).contains("Next scheduled:"))
        assertTrue(habitCreatedMessage("Read", setOf(DayOfWeek.MONDAY), today).contains("scheduled for today"))
    }
    @Test fun `badge identity survives filtering and deletion of other habits`() {
        val all = listOf(8L, 12L, 91L)
        val before = all.associateWith { habitBadgeIndex(it, 4) }
        val filtered = all.drop(1).associateWith { habitBadgeIndex(it, 4) }
        filtered.forEach { (id, color) -> assertEquals(before[id], color) }
    }
}
