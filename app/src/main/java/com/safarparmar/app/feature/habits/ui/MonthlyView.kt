package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
    onAdd: () -> Unit = {}
) {
    var filterState by remember { mutableStateOf(HabitFilterState()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var dayDetailDate by remember { mutableStateOf<LocalDate?>(null) }

    val visibleHabits = habits.filterByCriteria(filterState)
    val monthTitle = monthAnchor.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ModernPageHeader(
                    eyebrow = monthTitle,
                    title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_calendar)
                )
            }

            if (habits.isEmpty()) {
                item { ModernSoftCard { ModernEmptyState("No habits yet", "Tap + New Habit to start tracking your month.") } }
            } else {
                item {
                    HabitFilterBar(
                        habits = habits,
                        filterState = filterState,
                        onOpenFilterSheet = { showFilterSheet = true },
                        onRemoveHabitId = { id ->
                            filterState = filterState.copy(selectedHabitIds = filterState.selectedHabitIds - id)
                        },
                        onRemoveFrequency = { freq ->
                            filterState = filterState.copy(selectedFrequencies = filterState.selectedFrequencies - freq)
                        },
                        onRemoveStatus = { status ->
                            filterState = filterState.copy(selectedStatuses = filterState.selectedStatuses - status)
                        }
                    )
                }
                item {
                    ModernMonthCalendar(
                        month = monthAnchor,
                        monthTitle = monthTitle,
                        habits = visibleHabits,
                        today = today,
                        onCurrent = onCurrent,
                        onPrevious = onPrevMonth,
                        onNext = onNextMonth,
                        onOpenDay = { dayDetailDate = it }
                    )
                }
            }
        }

        if (showFilterSheet) {
            HabitFilterTwoPaneSheet(
                habits = habits,
                currentState = filterState,
                onApply = { filterState = it },
                onDismiss = { showFilterSheet = false }
            )
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
    monthTitle: String,
    habits: List<HabitWithCompletions>,
    today: LocalDate,
    onCurrent: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val first = month.withDayOfMonth(1)
    val leading = first.dayOfWeek.value - 1
    val dayCount = month.lengthOfMonth()
    val totalCells = leading + dayCount
    val rows = (totalCells + 6) / 7

    ModernSoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp)) {
            ModernPeriodToolbar(
                label = monthTitle,
                nowLabel = "This month",
                onNow = onCurrent,
                onPrevious = onPrevious,
                onNext = onNext
            )
            Text(
                "Tap any date to review or update its habits.",
                color = HabitColors.TextSecondary,
                fontSize = 11.sp
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
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
    val background by animateColorAsState(
        if (complete) HabitColors.CheckDone else Color.Transparent,
        tween(180),
        label = "monthDayBackground",
    )
    val border by animateColorAsState(targetValue = when {
        complete -> HabitColors.CheckDone
        current -> HabitColors.RoyalPurple
        else -> HabitColors.Outline
    }, animationSpec = tween(180), label = "monthDayBorder")
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
