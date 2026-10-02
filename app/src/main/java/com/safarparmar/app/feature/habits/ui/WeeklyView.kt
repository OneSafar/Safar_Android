package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.safarparmar.app.ui.components.SafarColoredCheckbox
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
    var filterState by remember { mutableStateOf(HabitFilterState()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val days = remember(weekStart) { (0L..6L).map(weekStart::plusDays) }
    val visibleHabits = remember(habits, filterState) { habits.filterByCriteria(filterState) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    val isCurrentWeek = today in days
    val due = visibleHabits.sumOf { item -> days.count { !it.isAfter(today) && item.completionByDate.containsKey(it) } }
    val done = visibleHabits.sumOf { item -> days.count { !it.isAfter(today) && item.completionByDate[it] == true } }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "week-header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isCurrentWeek) "This week" else "Your week", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = HabitColors.TextPrimary)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${weekStart.format(dateFormat)} – ${days.last().format(dateFormat)}",
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = HabitColors.TextSecondary)
                    IconButton(onClick = onPrevWeek, style = ButtonStyle.Outlined, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onNextWeek, style = ButtonStyle.Outlined, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week", modifier = Modifier.size(20.dp))
                    }
                }
                if (!isCurrentWeek) Button(onClick = onCurrent, style = ButtonStyle.Ghost) { Text("Back to this week") }
            }
        }
        if (habits.isEmpty()) {
            item(key = "empty") {
                ModernSoftCard(Modifier.fillMaxWidth().padding(0.dp)) {
                    ModernEmptyState("Start with one habit", "Choose something small you want to practise regularly.")
                    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add your first habit") }
                }
            }
        } else {
            if (habits.size > 1 || filterState.totalCount() > 0) {
                item(key = "filters") {
                    Button(onClick = { showFilterSheet = true }, style = ButtonStyle.Outlined) {
                        Text(if (filterState.totalCount() > 0) "Filter habits · ${filterState.totalCount()} selected" else "Filter habits")
                    }
                }
            }
            if (visibleHabits.isEmpty()) {
                item(key = "no-match") {
                    ModernSoftCard(Modifier.fillMaxWidth().padding(0.dp)) {
                        Text("No habits match your filters", style = MaterialTheme.typography.titleMedium, color = HabitColors.TextPrimary)
                        Button(onClick = { filterState = HabitFilterState() }, style = ButtonStyle.Ghost) { Text("Show all habits") }
                    }
                }
            } else {
                item(key = "summary") {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(HabitColors.RoyalPurpleBg).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (due == 0) "Nothing due yet" else "$done of $due done${if (days.last().isAfter(today)) " so far" else " this week"}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HabitColors.RoyalPurple)
                        Text("Tap a checkbox when you finish. Future days unlock on their date.",
                            style = MaterialTheme.typography.bodySmall, color = HabitColors.TextSecondary)
                    }
                }
                items(visibleHabits, key = { "habit-${it.habit.id}" }) { item ->
                    WeeklyHabitCard(item, days, today, onToggleDay, onEditHabit)
                }
                item(key = "day-key") {
                    Text("Off = not scheduled · Later = a future day", style = MaterialTheme.typography.bodySmall,
                        color = HabitColors.TextSecondary)
                }
            }
        }
    }
    if (showFilterSheet) {
        HabitFilterTwoPaneSheet(habits = habits, currentState = filterState,
            onApply = { filterState = it }, onDismiss = { showFilterSheet = false })
    }
}

@Composable
private fun WeeklyHabitCard(
    item: HabitWithCompletions,
    days: List<LocalDate>,
    today: LocalDate,
    onToggleDay: (Long, LocalDate, Boolean) -> Unit,
    onEditHabit: (HabitEntity) -> Unit,
) {
    val due = days.count { !it.isAfter(today) && item.completionByDate.containsKey(it) }
    val done = days.count { !it.isAfter(today) && item.completionByDate[it] == true }
    val upcoming = days.count { it.isAfter(today) && item.completionByDate.containsKey(it) && item.completionByDate[it] != true }
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = HabitColors.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, HabitColors.Outline),
    ) {
        Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HabitColors.TextPrimary)
                Text(buildString {
                    append(if (due == 0) "Nothing due yet" else "$done of $due done")
                    if (upcoming > 0) append(" · $upcoming upcoming")
                }, style = MaterialTheme.typography.bodySmall, color = HabitColors.TextSecondary)
            }
            Button(onClick = { onEditHabit(item.habit) }, style = ButtonStyle.Ghost) { Text("Edit") }
        }
        Spacer(Modifier.height(14.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val dayWidth = (maxWidth / 7).coerceAtLeast(48.dp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                days.forEach { date ->
                    val completed = item.completionByDate[date] == true
                    val scheduled = item.completionByDate.containsKey(date)
                    val future = date.isAfter(today)
                    val label = when { completed -> "Done"; !scheduled -> "Off"; future -> "Later"; else -> "To do" }
                    val format = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
                    Column(Modifier.width(dayWidth).clip(RoundedCornerShape(12.dp))
                        .background(if (date == today) HabitColors.RoyalPurpleBg else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.format(format), fontSize = 11.sp, color = HabitColors.TextSecondary)
                        Text(date.dayOfMonth.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (date == today) HabitColors.RoyalPurple else HabitColors.TextPrimary)
                        if (completed || scheduled) {
                            SafarColoredCheckbox(
                                checked = completed, onCheckedChange = { onToggleDay(item.habit.id, date, completed) },
                                enabled = completed || !future,
                                checkedColor = HabitColors.RoyalPurple, uncheckedColor = HabitColors.OutlineStrong,
                                checkmarkColor = HabitColors.CheckDoneOn,
                                accessibilityLabel = "${item.habit.name}, $date, $label${if (date == today) ", today" else ""}",
                            )
                        } else {
                            Box(Modifier.size(48.dp).semantics { contentDescription = "${item.habit.name}, $date, not scheduled" }, contentAlignment = Alignment.Center) {
                                Text("—", color = HabitColors.TextTertiary)
                            }
                        }
                        Text(label, fontSize = 10.sp, color = if (completed) HabitColors.RoyalPurple else HabitColors.TextSecondary)
                        Text(if (date == today) "Today" else "", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = HabitColors.RoyalPurple)
                    }
                }
            }
        }
        }
    }
}

private fun getHabitEmoji(name: String): String {
    val lower = name.lowercase(Locale.getDefault())
    return when {
        lower.contains("read") || lower.contains("book") || lower.contains("page") -> "📖"
        lower.contains("run") || lower.contains("jog") -> "👟"
        lower.contains("walk") || lower.contains("step") -> "🚶"
        lower.contains("water") || lower.contains("drink") || lower.contains("hydrate") -> "💧"
        lower.contains("meditat") || lower.contains("mind") || lower.contains("zen") -> "🧘"
        lower.contains("workout") || lower.contains("gym") || lower.contains("exercise") || lower.contains("fitness") -> "🏋️"
        lower.contains("yoga") || lower.contains("stretch") -> "🧘"
        lower.contains("sleep") || lower.contains("bed") || lower.contains("rest") -> "😴"
        lower.contains("study") || lower.contains("code") || lower.contains("learn") -> "💻"
        lower.contains("journal") || lower.contains("write") || lower.contains("diary") -> "✍️"
        lower.contains("eat") || lower.contains("food") || lower.contains("diet") || lower.contains("meal") -> "🥗"
        lower.contains("clean") || lower.contains("tidy") -> "🧹"
        name.any { Character.isSurrogate(it) } -> ""
        else -> "✦"
    }
}
