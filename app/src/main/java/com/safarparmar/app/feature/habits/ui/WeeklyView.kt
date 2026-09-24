package com.safarparmar.app.feature.habits.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
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
    var dayDetailDate by remember { mutableStateOf<LocalDate?>(null) }
    val end = weekStart.plusDays(6)
    val days = remember(weekStart) { (0L..6L).map(weekStart::plusDays) }
    val visibleHabits = habits.filterByCriteria(filterState)
    val range = "${weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))} – ${end.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))}"

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                ModernPageHeader(
                    eyebrow = range,
                    title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_week_glance)
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

                if (visibleHabits.isEmpty()) {
                    item(key = "no_match") {
                        ModernSoftCard {
                            ModernEmptyState(
                                title = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_no_matching),
                                detail = "No habits match the selected filter criteria."
                            )
                        }
                    }
                } else {
                    item(key = "matrix_grid") {
                        WeeklyMatrixGridCard(
                            days = days,
                            today = today,
                            habits = visibleHabits,
                            onToggleDay = onToggleDay,
                            onEditHabit = onEditHabit,
                            onOpenDay = { dayDetailDate = it }
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

        if (showFilterSheet) {
            HabitFilterTwoPaneSheet(
                habits = habits,
                currentState = filterState,
                onApply = { filterState = it },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun WeeklyMatrixGridCard(
    days: List<LocalDate>,
    today: LocalDate,
    habits: List<HabitWithCompletions>,
    onToggleDay: (Long, LocalDate, Boolean) -> Unit,
    onEditHabit: (HabitEntity) -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val todayIndex = days.indexOf(today)
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(HabitColors.Surface)
            .border(1.dp, HabitColors.OutlineStrong, cardShape)
    ) {
        // 1. Continuous Vertical Today Highlight Column (Single unbroken pill from top to bottom)
        if (todayIndex != -1) {
            Row(
                modifier = Modifier
                    .matchParentSize()
            ) {
                if (todayIndex > 0) {
                    Spacer(modifier = Modifier.weight(todayIndex.toFloat()))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.5.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(HabitColors.RoyalPurple.copy(alpha = 0.11f))
                )
                if (todayIndex < 6) {
                    Spacer(modifier = Modifier.weight((6 - todayIndex).toFloat()))
                }
            }
        }

        // 2. Card Content (Headers, Daily Scores, and Habit Check Rows)
        Column(modifier = Modifier.fillMaxWidth()) {
            // Day Column Headers (M 14 | T 15 | W 16 ...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEachIndexed { index, date ->
                    val isToday = date == today
                    val dayNarrow = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    val dayNum = date.dayOfMonth.toString()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenDay(date) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text(
                                text = "$dayNarrow $dayNum",
                                color = if (isToday) HabitColors.RoyalPurple else HabitColors.TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold
                            )
                            if (isToday) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_today_parenthetical),
                                    color = HabitColors.RoyalPurple,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (index < 6) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(0.5.dp)
                                    .height(18.dp)
                                    .background(HabitColors.Outline.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // Daily Score Row (e.g. 4/5, 100%, —)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                days.forEach { date ->
                    val isToday = date == today
                    val scheduled = habits.filter { it.completionByDate.containsKey(date) }
                    val done = scheduled.count { it.completionByDate[date] == true }
                    val isAllDone = scheduled.isNotEmpty() && done == scheduled.size

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val scoreLabel = when {
                            scheduled.isEmpty() -> "—"
                            isAllDone -> "100%"
                            else -> "$done/${scheduled.size}"
                        }
                        Text(
                            text = scoreLabel,
                            color = when {
                                isAllDone -> HabitColors.CheckDone
                                isToday -> HabitColors.RoyalPurple
                                else -> HabitColors.TextTertiary
                            },
                            fontSize = 10.5.sp,
                            fontWeight = if (isAllDone || isToday) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Divider between headers and habits
            HorizontalDivider(
                color = HabitColors.Outline.copy(alpha = 0.45f),
                thickness = 0.75.dp
            )

            // Habit Rows
            habits.forEachIndexed { habitIndex, item ->
                val isLastHabit = habitIndex == habits.lastIndex
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp)
                ) {
                    // Habit Info Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditHabit(item.habit) }
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val emoji = remember(item.habit.name) { getHabitEmoji(item.habit.name) }
                            if (emoji.isNotEmpty()) {
                                Text(
                                    text = emoji,
                                    fontSize = 16.sp
                                )
                            }
                            Text(
                                text = item.habit.name,
                                color = HabitColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val weeklyDoneCount = days.count { item.completionByDate[it] == true }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$weeklyDoneCount/7 completed",
                                color = if (weeklyDoneCount > 0) HabitColors.RoyalPurple else HabitColors.TextTertiary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_edit),
                                tint = HabitColors.TextTertiary.copy(alpha = 0.5f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // 7 Check Circles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        days.forEach { date ->
                            val isScheduled = item.completionByDate.containsKey(date)
                            val isCompleted = item.completionByDate[date] == true

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isScheduled) {
                                    val targetBg by animateColorAsState(
                                        targetValue = if (isCompleted) HabitColors.RoyalPurple else HabitColors.Parchment,
                                        animationSpec = tween(140),
                                        label = "matrixCellBg"
                                    )
                                    val checkScale by animateFloatAsState(
                                        targetValue = if (isCompleted) 1f else 0.94f,
                                        animationSpec = spring(dampingRatio = 0.7f),
                                        label = "matrixCheckScale"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .scale(checkScale)
                                            .clip(CircleShape)
                                            .background(targetBg)
                                            .border(
                                                width = 1.2.dp,
                                                color = if (isCompleted) HabitColors.RoyalPurple else HabitColors.OutlineStrong,
                                                shape = CircleShape
                                            )
                                            .clickable(
                                                role = Role.Checkbox,
                                                onClick = { onToggleDay(item.habit.id, date, isCompleted) }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_completed),
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            // Subtle central dot target (matching reference mockup)
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(HabitColors.TextTertiary.copy(alpha = 0.7f))
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "·",
                                        color = HabitColors.OutlineStrong,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isLastHabit) {
                    HorizontalDivider(
                        color = HabitColors.Outline.copy(alpha = 0.35f),
                        thickness = 0.75.dp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Derives a representative emoji for a habit based on keywords in its name.
 */
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
