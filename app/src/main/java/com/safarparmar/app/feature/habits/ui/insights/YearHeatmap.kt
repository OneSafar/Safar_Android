/* Hallmark · pre-emit critique: P5 H4 E4 S5 R4 V4 */
package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.DailyHabitStats
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.ui.HabitColors
import com.safarparmar.app.feature.habits.ui.LocalHabitDarkTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearHeatmap(
    selectedYear: Int,
    dailyStats: List<DailyHabitStats>,
    availableHabits: List<HabitEntity>,
    selectedHabitId: Long?,
    onSelectHabit: (Long?) -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onCurrentYear: () -> Unit,
    canNavigateNextYear: Boolean,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()
) {
    val outlineColor = HabitColors.Outline
    val surfaceColor = HabitColors.Surface
    val textPrimary = HabitColors.TextPrimary
    val textSecondary = HabitColors.TextSecondary
    val textTertiary = HabitColors.TextTertiary

    var expandedFilter by remember { mutableStateOf(false) }
    var selectedCellInfo by remember { mutableStateOf<DailyHabitStats?>(null) }

    // Map dailyStats by date for fast lookup
    val statsByDate = remember(dailyStats) {
        dailyStats.associateBy { it.date }
    }

    val months = remember(selectedYear) {
        (1..12).map { month -> YearMonth.of(selectedYear, month) }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        // 1. Header: Title and Year Navigator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_year_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_consistency_heatmap),
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            // Year navigation controls: ‹ 2026 › This Year
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onPreviousYear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_previous_year),
                        tint = textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = selectedYear.toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = onNextYear,
                    enabled = canNavigateNextYear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_next_year),
                        tint = if (canNavigateNextYear) textPrimary else textTertiary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (selectedYear != today.year) {
                    TextButton(
                        onClick = onCurrentYear,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_this_year),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HabitColors.RoyalPurple
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 2. Habit Filter Dropdown
        val selectedHabitName = availableHabits.firstOrNull { it.id == selectedHabitId }?.name ?: androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_all)

        Box {
            Surface(
                onClick = { expandedFilter = true },
                shape = RoundedCornerShape(12.dp),
                color = HabitColors.Parchment,
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = selectedHabitName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_filter_one),
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expandedFilter,
                onDismissRequest = { expandedFilter = false },
                modifier = Modifier.background(surfaceColor)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_all),
                            fontWeight = if (selectedHabitId == null) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedHabitId == null) HabitColors.RoyalPurple else textPrimary
                        )
                    },
                    onClick = {
                        onSelectHabit(null)
                        expandedFilter = false
                    }
                )
                availableHabits.forEach { habit ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    habit.name,
                                    fontWeight = if (selectedHabitId == habit.id) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedHabitId == habit.id) HabitColors.RoyalPurple else textPrimary
                                )
                                if (habit.isArchived) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "(Archived)",
                                        fontSize = 11.sp,
                                        color = textTertiary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectHabit(habit.id)
                            expandedFilter = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 3. Month-owned calendar grids keep every date visibly tied to its month.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val monthColumnCount = if (maxWidth >= 600.dp) 3 else 2

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                months.chunked(monthColumnCount).forEach { monthRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        monthRow.forEach { month ->
                            MonthHeatmap(
                                month = month,
                                statsByDate = statsByDate,
                                today = today,
                                onDateSelected = { date ->
                                    selectedCellInfo = statsByDate[date]
                                        ?: DailyHabitStats(date, 0, 0, 0f)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(monthColumnCount - monthRow.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 4. Selected Cell Feedback Banner
        selectedCellInfo?.let { cell ->
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = HabitColors.Parchment,
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fmt = cell.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                    Text(
                        text = fmt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    val statusText = when {
                        cell.date.isAfter(today) -> "Upcoming"
                        cell.scheduledCount == 0 -> "No habits scheduled"
                        else -> "${cell.completedCount}/${cell.scheduledCount} completed (${(cell.completionRate * 100).toInt()}%)"
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (cell.completionRate >= 1.0f && cell.scheduledCount > 0) HabitColors.CheckDone else textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 5. Heatmap Legend: Less [0 1 2 3 4] More
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_less),
                fontSize = 10.sp,
                color = textTertiary,
                modifier = Modifier.padding(end = 6.dp)
            )

            (0..4).forEach { level ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(11.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(getHeatmapCellColor(level, isCurrentYear = true, isFuture = false))
                        .then(
                            if (level == 0) Modifier.border(0.75.dp, outlineColor, RoundedCornerShape(2.5.dp))
                            else Modifier
                        )
                )
            }

            Text(
                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_more),
                fontSize = 10.sp,
                color = textTertiary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun MonthHeatmap(
    month: YearMonth,
    statsByDate: Map<LocalDate, DailyHabitStats>,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = HabitColors.Outline
    val textPrimary = HabitColors.TextPrimary
    val textTertiary = HabitColors.TextTertiary
    val leadingEmptyCells = month.atDay(1).dayOfWeek.value - 1
    val cellCount = leadingEmptyCells + month.lengthOfMonth()
    val weekCount = (cellCount + 6) / 7
    val levelColors = (0..4).map { level ->
        getHeatmapCellColor(level, isCurrentYear = true, isFuture = false)
    }
    val futureColor = getHeatmapCellColor(0, isCurrentYear = true, isFuture = true)
    val dayVisuals = remember(month, statsByDate, today) {
        (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            val isFuture = date.isAfter(today)
            val stat = statsByDate[date]
            val level = when {
                isFuture || stat == null || stat.scheduledCount == 0 -> 0
                stat.completionRate <= 0f -> 0
                stat.completionRate <= 0.25f -> 1
                stat.completionRate <= 0.50f -> 2
                stat.completionRate < 1f -> 3
                else -> 4
            }
            MonthDayVisual(
                dayOfMonth = dayOfMonth,
                level = level,
                isFuture = isFuture
            )
        }
    }

    Column(modifier = modifier) {
        Text(
            text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(7f / weekCount)
                .pointerInput(month) {
                    detectTapGestures { tap ->
                        val gap = 3.dp.toPx()
                        val cellWidth = (size.width - gap * 6f) / 7f
                        val cellHeight = (size.height - gap * (weekCount - 1)) / weekCount
                        val column = (tap.x / (cellWidth + gap)).toInt().coerceIn(0, 6)
                        val row = (tap.y / (cellHeight + gap)).toInt().coerceIn(0, weekCount - 1)
                        val day = row * 7 + column - leadingEmptyCells + 1
                        if (day in 1..month.lengthOfMonth()) onDateSelected(month.atDay(day))
                    }
                }
        ) {
            val gap = 3.dp.toPx()
            val cellWidth = (size.width - gap * 6f) / 7f
            val cellHeight = (size.height - gap * (weekCount - 1)) / weekCount
            val radius = 3.dp.toPx()

            dayVisuals.forEach { visual ->
                val dayOfMonth = visual.dayOfMonth
                val dayIndex = dayOfMonth - 1
                val gridIndex = leadingEmptyCells + dayIndex
                val column = gridIndex % 7
                val row = gridIndex / 7
                val topLeft = Offset(
                    x = column * (cellWidth + gap),
                    y = row * (cellHeight + gap)
                )
                val color = if (visual.isFuture) futureColor else levelColors[visual.level]
                drawRoundRect(
                    color = color,
                    topLeft = topLeft,
                    size = Size(cellWidth, cellHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
                if (visual.level == 0 && !visual.isFuture) {
                    drawRoundRect(
                        color = outlineColor.copy(alpha = 0.5f),
                        topLeft = topLeft,
                        size = Size(cellWidth, cellHeight),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = 0.75.dp.toPx())
                    )
                }
            }
        }
    }
}

private data class MonthDayVisual(
    val dayOfMonth: Int,
    val level: Int,
    val isFuture: Boolean
)

@Composable
private fun getHeatmapCellColor(level: Int, isCurrentYear: Boolean, isFuture: Boolean): Color {
    if (!isCurrentYear) return Color.Transparent
    if (isFuture) return HabitColors.Parchment.copy(alpha = 0.4f)
    val isDark = LocalHabitDarkTheme.current

    return when (level) {
        0 -> if (isDark) Color(0xFF1E1F24) else Color(0xFFF3EFE6)
        1 -> if (isDark) Color(0xFF2E1065) else Color(0xFFEDE9FE)
        2 -> if (isDark) Color(0xFF4C1D95) else Color(0xFFC4B5FD)
        3 -> if (isDark) Color(0xFF7E22CE) else Color(0xFF8B5CF6)
        4 -> if (isDark) Color(0xFFC084FC) else Color(0xFF581C87)
        else -> Color.Transparent
    }
}
