package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WeeklyView(
    weekStart: LocalDate,
    habits: List<HabitWithCompletions>,
    onToggleDay: (Long, LocalDate, Boolean) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onEditHabit: (HabitEntity) -> Unit = {},
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onCurrent: () -> Unit = {},
    onAdd: () -> Unit = {}
) {
    var selectedHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dayDetailDate by remember { mutableStateOf<LocalDate?>(null) }
    val end = weekStart.plusDays(6)
    val days = remember(weekStart) { (0L..6L).map(weekStart::plusDays) }
    val visibleHabits = selectedHabitId?.let { id -> habits.filter { it.habit.id == id } } ?: habits
    val range = "${weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))} – ${end.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))}"

    LaunchedEffect(habits) {
        if (selectedHabitId != null && habits.none { it.habit.id == selectedHabitId }) selectedHabitId = null
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") {
                ModernPageHeader(
                    eyebrow = range,
                    title = "Your Week, Day by Day"
                )
            }
            item(key = "toolbar") {
                ModernPeriodToolbar(
                    label = "Week of ${weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}",
                    nowLabel = "This week",
                    onNow = onCurrent,
                    onPrevious = onPrevWeek,
                    onNext = onNextWeek
                )
            }

            if (habits.isEmpty()) {
                item(key = "empty") {
                    ModernSoftCard { ModernEmptyState("No habits yet", "Tap + New Habit to start tracking your week.") }
                }
            } else {
                item(key = "filter") {
                    ModernFilterRow(habits, selectedHabitId) { selectedHabitId = it }
                }
                items(days, key = { it.toEpochDay() }) { date ->
                    WeeklyTimelineDay(
                        date = date,
                        today = today,
                        habits = visibleHabits,
                        onToggleDay = onToggleDay,
                        onOpenDay = { dayDetailDate = date }
                    )
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
private fun WeeklyTimelineDay(
    date: LocalDate,
    today: LocalDate,
    habits: List<HabitWithCompletions>,
    onToggleDay: (Long, LocalDate, Boolean) -> Unit,
    onOpenDay: () -> Unit
) {
    val scheduled = remember(habits, date) { habits.filter { it.completionByDate.containsKey(date) } }
    val done = scheduled.count { it.completionByDate[date] == true }
    val isToday = date == today
    val isFuture = date.isAfter(today)
    val complete = scheduled.isNotEmpty() && done == scheduled.size
    val markerColor = when {
        complete -> HabitColors.CheckDone
        isToday -> HabitColors.DeepOrange
        else -> HabitColors.RoyalPurple
    }
    val status = when {
        scheduled.isEmpty() -> "Rest day"
        complete -> "All ${scheduled.size} completed"
        isFuture -> "${scheduled.size} scheduled"
        else -> "$done of ${scheduled.size} completed"
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.width(30.dp).fillMaxHeight()) {
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .align(Alignment.Center)
                    .background(HabitColors.Outline)
            )
            Box(
                Modifier
                    .padding(top = 21.dp)
                    .size(15.dp)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .background(if (complete) markerColor else HabitColors.Surface)
                    .border(2.dp, markerColor, CircleShape)
            )
        }

        ModernSoftCard(
            Modifier
                .weight(1f)
                .clickable(onClick = onOpenDay)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())),
                                color = HabitColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isToday) {
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    "TODAY",
                                    color = HabitColors.DeepOrange,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = .6.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(HabitColors.DeepOrangeBg)
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(status, color = HabitColors.TextSecondary, fontSize = 10.sp)
                    }
                    Text("Details →", color = HabitColors.RoyalPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (scheduled.isEmpty()) {
                    Text("Nothing scheduled", color = HabitColors.TextTertiary, fontSize = 11.sp)
                } else {
                    scheduled.take(3).forEach { item ->
                        val completed = item.completionByDate[date] == true
                        WeeklyTimelineHabitRow(
                            name = item.habit.name,
                            completed = completed,
                            enabled = !isFuture || completed,
                            onClick = { onToggleDay(item.habit.id, date, completed) }
                        )
                    }
                    if (scheduled.size > 3) {
                        Text(
                            "+${scheduled.size - 3} more habits",
                            color = HabitColors.RoyalPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTimelineHabitRow(
    name: String,
    completed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (completed) HabitColors.CheckDoneBg else HabitColors.Parchment)
            .border(
                1.dp,
                if (completed) HabitColors.CheckDone.copy(alpha = .35f) else HabitColors.Outline,
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            modifier = Modifier.weight(1f),
            color = HabitColors.TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (completed) HabitColors.CheckDone else HabitColors.Surface)
                .border(1.5.dp, HabitColors.CheckDone, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (completed) Text("✓", color = HabitColors.CheckDoneOn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
