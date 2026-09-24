package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.DailyHabitStats
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.ui.HabitColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ContributionHeatmapMatrix(
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

    val statsByDate = remember(dailyStats) {
        dailyStats.associateBy { it.date }
    }

    // 12 months for the selected year
    val months = remember(selectedYear) {
        (1..12).map { month -> YearMonth.of(selectedYear, month) }
    }

    val scrollState = rememberScrollState()

    // Scroll to current month on initial launch if on current year
    LaunchedEffect(selectedYear) {
        if (selectedYear == today.year) {
            val currentMonthIndex = today.monthValue - 1
            // Estimate width per month ~ 5 weeks * 21dp + 24dp gap ~= 129dp
            val targetScroll = (currentMonthIndex * 125 * 3).coerceAtMost(scrollState.maxValue)
            scrollState.scrollTo(targetScroll)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(surfaceColor)
            .border(1.dp, outlineColor, RoundedCornerShape(22.dp))
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
                    text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_consistency),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_monthly_matrix),
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

        // 3. GitHub/HabitBox 7-row Heatmap Matrix Segregated Horizontally by Month
        // Cell size increased to 17.dp with 4.dp spacing for high legibility
        val cellSize = 17.dp
        val cellSpacing = 4.dp
        val cellCornerRadius = 3.5.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Day-of-week labels on the left: Mon, Wed, Fri
            Column(
                modifier = Modifier.padding(top = 28.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                dayLabels.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier.size(width = 14.dp, height = cellSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 0 || index == 2 || index == 4) { // Mon, Wed, Fri
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textTertiary
                            )
                        }
                    }
                }
            }

            // Scrollable Container: Month blocks arranged side by side
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                months.forEach { month ->
                    MonthHeatmapBlock(
                        month = month,
                        statsByDate = statsByDate,
                        today = today,
                        selectedYear = selectedYear,
                        cellSize = cellSize,
                        cellSpacing = cellSpacing,
                        cellCornerRadius = cellCornerRadius,
                        selectedDate = selectedCellInfo?.date,
                        onSelectDate = { date ->
                            selectedCellInfo = statsByDate[date] ?: DailyHabitStats(date, 0, 0, 0f)
                        }
                    )

                    // Subtle vertical separator between months (except after December)
                    if (month.monthValue < 12) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height((cellSize + cellSpacing) * 7 + 28.dp)
                                .background(outlineColor.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // 4. Selected Cell Feedback Banner
        selectedCellInfo?.let { cell ->
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = HabitColors.Parchment,
                border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fmt = cell.date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
                    Text(
                        text = fmt,
                        fontSize = 13.sp,
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cell.completionRate >= 1.0f && cell.scheduledCount > 0) HabitColors.CheckDone else textSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 5. Heatmap Legend: Less [0 1 2 3 4] More
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_less),
                fontSize = 11.sp,
                color = textTertiary,
                modifier = Modifier.padding(end = 6.dp)
            )

            listOf(
                HabitColors.Parchment,
                HabitColors.RoyalPurple.copy(alpha = 0.28f),
                HabitColors.RoyalPurple.copy(alpha = 0.55f),
                HabitColors.RoyalPurple.copy(alpha = 0.80f),
                HabitColors.RoyalPurple
            ).forEachIndexed { idx, color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(cellSize)
                        .clip(RoundedCornerShape(cellCornerRadius))
                        .background(color)
                        .then(
                            if (idx == 0) Modifier.border(0.75.dp, outlineColor, RoundedCornerShape(cellCornerRadius))
                            else Modifier
                        )
                )
            }

            Text(
                text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_more),
                fontSize = 11.sp,
                color = textTertiary,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * Renders a single month's segregated 7-row block of cells.
 * Displays the month title with day count above, followed by columns for that month.
 */
@Composable
private fun MonthHeatmapBlock(
    month: YearMonth,
    statsByDate: Map<LocalDate, DailyHabitStats>,
    today: LocalDate,
    selectedYear: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    cellSpacing: androidx.compose.ui.unit.Dp,
    cellCornerRadius: androidx.compose.ui.unit.Dp,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit
) {
    val outlineColor = HabitColors.Outline
    val daysInMonth = month.lengthOfMonth()
    // Day of week of the 1st: 1 = Monday, 7 = Sunday
    val startDow = month.atDay(1).dayOfWeek.value
    // Calculate how many week columns are needed for this month
    val totalSlots = (startDow - 1) + daysInMonth
    val columnCount = (totalSlots + 6) / 7

    val isCurrentMonth = month == YearMonth.now()

    Column {
        // Month Header: e.g. "January" with day count "31d"
        Row(
            modifier = Modifier
                .height(24.dp)
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                fontSize = 12.sp,
                fontWeight = if (isCurrentMonth) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isCurrentMonth) HabitColors.RoyalPurple else HabitColors.TextPrimary
            )
            Text(
                text = "${daysInMonth}d",
                fontSize = 10.sp,
                color = HabitColors.TextTertiary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(4.dp))

        // 7 Rows (Mon..Sun)
        (0..6).forEach { rowDowIndex -> // 0 = Mon, 6 = Sun
            Row(
                horizontalArrangement = Arrangement.spacedBy(cellSpacing),
                modifier = Modifier.padding(bottom = cellSpacing)
            ) {
                (0 until columnCount).forEach { colIndex ->
                    val slotIndex = colIndex * 7 + rowDowIndex
                    val dayOfMonth = slotIndex - (startDow - 1) + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = month.atDay(dayOfMonth)
                        val isFuture = date.isAfter(today)
                        val stat = statsByDate[date]
                        val level = when {
                            isFuture || stat == null || stat.scheduledCount == 0 -> 0
                            stat.completionRate <= 0f -> 0
                            stat.completionRate <= 0.25f -> 1
                            stat.completionRate <= 0.50f -> 2
                            stat.completionRate <= 0.75f -> 3
                            else -> 4
                        }
                        val isToday = date == today
                        val isSelected = selectedDate == date

                        val cellColor = when {
                            isFuture -> HabitColors.Parchment.copy(alpha = 0.60f)
                            level == 0 -> HabitColors.Parchment
                            level == 1 -> HabitColors.RoyalPurple.copy(alpha = 0.28f)
                            level == 2 -> HabitColors.RoyalPurple.copy(alpha = 0.55f)
                            level == 3 -> HabitColors.RoyalPurple.copy(alpha = 0.80f)
                            else -> HabitColors.RoyalPurple
                        }

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(cellCornerRadius))
                                .background(cellColor)
                                .then(
                                    when {
                                        isSelected -> Modifier.border(1.5.dp, HabitColors.TextPrimary, RoundedCornerShape(cellCornerRadius))
                                        isToday -> Modifier.border(1.5.dp, HabitColors.DeepOrange, RoundedCornerShape(cellCornerRadius))
                                        level == 0 -> Modifier.border(0.5.dp, outlineColor.copy(alpha = 0.5f), RoundedCornerShape(cellCornerRadius))
                                        else -> Modifier
                                    }
                                )
                                .clickable { onSelectDate(date) }
                        )
                    } else {
                        // Empty spacer cell so the grid maintains its rigid Monday..Sunday alignment
                        Spacer(modifier = Modifier.size(cellSize))
                    }
                }
            }
        }
    }
}
