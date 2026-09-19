package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MonthlyView(
    monthAnchor: LocalDate,
    habits: List<HabitWithCompletions>,
    onToggleDay: (Long, LocalDate, Boolean) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEditHabit: (HabitEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onCurrent: () -> Unit = {},
    onAdd: () -> Unit = {},
    streakHabits: List<HabitWithCompletions> = habits
) {
    var selectedHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dayDetailDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(habits, selectedHabitId) {
        if (selectedHabitId != null && habits.none { it.habit.id == selectedHabitId }) selectedHabitId = null
    }

    val visibleHabits = selectedHabitId?.let { id -> habits.filter { it.habit.id == id } } ?: habits
    val monthTitle = monthAnchor.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val monthEnd = monthAnchor.withDayOfMonth(monthAnchor.lengthOfMonth())
    val elapsedEnd = when {
        today.isBefore(monthAnchor) -> monthAnchor.minusDays(1)
        today.isAfter(monthEnd) -> monthEnd
        else -> today
    }
    val elapsedDates = if (elapsedEnd.isBefore(monthAnchor)) emptyList() else generateSequence(monthAnchor) { it.plusDays(1) }
        .takeWhile { !it.isAfter(elapsedEnd) }
        .toList()

    val totalOpportunities = elapsedDates.sumOf { d -> visibleHabits.count { it.completionByDate.containsKey(d) } }
    val doneOpportunities = elapsedDates.sumOf { d -> visibleHabits.count { it.completionByDate[d] == true } }
    val rate = if (totalOpportunities == 0) 0 else ((doneOpportunities.toFloat() / totalOpportunities) * 100).roundToInt()
    val streak = currentAggregateStreak(streakHabits, today, selectedHabitId)

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ModernPageHeader(
                    eyebrow = monthTitle,
                    title = "Monthly Habits",
                    subtitle = "See patterns at a glance. Future days do not reduce your completion rate."
                )
            }
            item {
                ModernPeriodToolbar(
                    label = monthTitle,
                    nowLabel = "This month",
                    onNow = onCurrent,
                    onPrevious = onPrevMonth,
                    onNext = onNextMonth
                )
            }

            if (habits.isEmpty()) {
                item { ModernSoftCard { ModernEmptyState("No habits yet", "Create your first habit to start tracking.") } }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("HABITS", color = HabitColors.TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                        ModernFilterRow(habits, selectedHabitId) { selectedHabitId = it }
                    }
                }
                item { ModernSectionRow("${visibleHabits.size} habit${if (visibleHabits.size == 1) "" else "s"} in view", "Tap a day for details") }
                item {
                    ModernMonthCalendar(
                        month = monthAnchor,
                        habits = visibleHabits,
                        today = today,
                        onOpenDay = { dayDetailDate = it }
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ModernMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Current streak",
                            value = "$streak day${if (streak == 1) "" else "s"}",
                            subtitle = "A current unfinished day does not erase yesterday's streak."
                        )
                        ModernMetricCard(
                            modifier = Modifier.weight(1f),
                            label = "Completion rate",
                            value = "$rate%",
                            subtitle = "$doneOpportunities of $totalOpportunities elapsed opportunities completed."
                        )
                    }
                }
            }
        }

        dayDetailDate?.let { date ->
            ModernDayDetailSheet(
                date = date,
                today = today,
                habits = visibleHabits,
                onToggle = onToggleDay,
                onDismiss = { dayDetailDate = null }
            )
        }
    }
}

@Composable
private fun ModernMonthCalendar(
    month: LocalDate,
    habits: List<HabitWithCompletions>,
    today: LocalDate,
    onOpenDay: (LocalDate) -> Unit
) {
    val first = month.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()
    val totalCells = leading + dayCount
    val rows = (totalCells + 6) / 7

    ModernSoftCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            MondayFirst.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    color = HabitColors.TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(rows) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { col ->
                        val cellIndex = row * 7 + col
                        val day = cellIndex - leading + 1
                        Box(Modifier.weight(1f)) {
                            if (day in 1..dayCount) {
                                val date = month.withDayOfMonth(day)
                                val summary = modernDaySummary(habits, date)
                                ModernCalendarDay(date, today, summary, onOpenDay)
                            } else {
                                Spacer(Modifier.fillMaxWidth().aspectRatio(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernCalendarDay(
    date: LocalDate,
    today: LocalDate,
    summary: ModernDaySummary,
    onOpenDay: (LocalDate) -> Unit
) {
    val complete = summary.complete
    val current = date == today
    val future = date.isAfter(today)
    val color = when {
        complete -> Color.White
        current -> HabitColors.RoyalPurple
        future -> HabitColors.TextTertiary
        else -> HabitColors.TextPrimary
    }
    val background = if (complete) HabitColors.CheckDone else Color.Transparent
    val border = when {
        complete -> HabitColors.CheckDone
        current -> HabitColors.RoyalPurple
        else -> HabitColors.Outline
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .border(if (current && !complete) 2.dp else 1.dp, border, RoundedCornerShape(11.dp))
            .clickable { onOpenDay(date) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(date.dayOfMonth.toString(), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(if (summary.total > 0) "${summary.done}/${summary.total}" else "–", color = color.copy(alpha = if (complete) .88f else 1f), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
