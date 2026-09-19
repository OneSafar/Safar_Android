package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var dayDetailDate by remember { mutableStateOf<LocalDate?>(null) }
    val end = weekStart.plusDays(6)
    val range = "${weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))} – ${end.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))}"

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ModernPageHeader(
                    eyebrow = range,
                    title = "Weekly Habits",
                    subtitle = "See each day's total progress first, then the habit-by-habit detail below."
                )
            }
            item {
                ModernPeriodToolbar(
                    label = "Week of ${weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))}",
                    nowLabel = "This week",
                    onNow = onCurrent,
                    onPrevious = onPrevWeek,
                    onNext = onNextWeek
                )
            }
            item {
                ModernWeeklyProgressCard(
                    habits = habits,
                    weekStart = weekStart,
                    today = today,
                    onOpenDay = { dayDetailDate = it }
                )
            }

            if (habits.isEmpty()) {
                item { ModernSoftCard { ModernEmptyState("No habits yet", "Create your first habit to start tracking.") } }
            } else {
                items(habits, key = { it.habit.id }) { item ->
                    ModernSoftCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModernHabitIdentity(item.habit) { onEditHabit(item.habit) }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(7) { index ->
                                    val date = weekStart.plusDays(index.toLong())
                                    val scheduled = item.completionByDate.containsKey(date)
                                    val completed = item.completionByDate[date] == true
                                    ModernWeekDayCell(
                                        date = date,
                                        today = today,
                                        scheduled = scheduled,
                                        completed = completed,
                                        onToggle = { onToggleDay(item.habit.id, date, completed) },
                                        modifier = Modifier.weight(1f),
                                        showWeekday = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        dayDetailDate?.let { date ->
            ModernDayDetailSheet(
                date = date,
                today = today,
                habits = habits,
                onToggle = onToggleDay,
                onDismiss = { dayDetailDate = null }
            )
        }
    }
}
