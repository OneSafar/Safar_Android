package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Tune
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
import com.safarparmar.app.feature.habits.data.HabitWithCompletions
import java.time.DayOfWeek

enum class HabitFilterCategory {
    HABIT_NAME,
    FREQUENCY,
    STATUS
}

enum class HabitFrequencyOption(val label: String) {
    EVERYDAY("Every day"),
    WEEKDAYS("Weekdays (Mon–Fri)"),
    WEEKENDS("Weekends (Sat–Sun)"),
    CUSTOM("Custom days")
}

enum class HabitStatusOption(val label: String) {
    ACTIVE("Active Habits"),
    ARCHIVED("Archived Habits")
}

data class HabitFilterState(
    val selectedHabitIds: Set<Long> = emptySet(),
    val selectedFrequencies: Set<HabitFrequencyOption> = emptySet(),
    val selectedStatuses: Set<HabitStatusOption> = emptySet()
) {
    fun isEmpty(): Boolean =
        selectedHabitIds.isEmpty() && selectedFrequencies.isEmpty() && selectedStatuses.isEmpty()

    fun totalCount(): Int =
        selectedHabitIds.size + selectedFrequencies.size + selectedStatuses.size
}

fun List<HabitWithCompletions>.filterByCriteria(filter: HabitFilterState): List<HabitWithCompletions> {
    if (filter.isEmpty()) return this

    return this.filter { hwc ->
        val habit = hwc.habit

        val matchesHabitId = filter.selectedHabitIds.isEmpty() || habit.id in filter.selectedHabitIds

        val matchesStatus = if (filter.selectedStatuses.isEmpty()) true else {
            val isActive = !habit.isArchived
            (HabitStatusOption.ACTIVE in filter.selectedStatuses && isActive) ||
                    (HabitStatusOption.ARCHIVED in filter.selectedStatuses && habit.isArchived)
        }

        val matchesFrequency = if (filter.selectedFrequencies.isEmpty()) true else {
            val isEveryday = habit.targetDays.size == 7
            val isWeekdays = habit.targetDays.size == 5 &&
                    DayOfWeek.SATURDAY !in habit.targetDays &&
                    DayOfWeek.SUNDAY !in habit.targetDays
            val isWeekends = habit.targetDays.size == 2 &&
                    DayOfWeek.SATURDAY in habit.targetDays &&
                    DayOfWeek.SUNDAY in habit.targetDays
            val isCustom = !isEveryday && !isWeekdays && !isWeekends

            (HabitFrequencyOption.EVERYDAY in filter.selectedFrequencies && isEveryday) ||
                    (HabitFrequencyOption.WEEKDAYS in filter.selectedFrequencies && isWeekdays) ||
                    (HabitFrequencyOption.WEEKENDS in filter.selectedFrequencies && isWeekends) ||
                    (HabitFrequencyOption.CUSTOM in filter.selectedFrequencies && isCustom)
        }

        matchesHabitId && matchesStatus && matchesFrequency
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFilterTwoPaneSheet(
    habits: List<HabitWithCompletions>,
    currentState: HabitFilterState,
    onApply: (HabitFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(HabitFilterCategory.HABIT_NAME) }

    // Draft filter state within the sheet
    var draftHabitIds by remember { mutableStateOf(currentState.selectedHabitIds) }
    var draftFrequencies by remember { mutableStateOf(currentState.selectedFrequencies) }
    var draftStatuses by remember { mutableStateOf(currentState.selectedStatuses) }

    val totalSelected = draftHabitIds.size + draftFrequencies.size + draftStatuses.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = HabitColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // 1. Header: androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_filter_habits) + androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_clear_all)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 0.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = HabitColors.RoyalPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_filter_habits),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HabitColors.TextPrimary
                    )
                }

                if (totalSelected > 0) {
                    TextButton(
                        onClick = {
                            draftHabitIds = emptySet()
                            draftFrequencies = emptySet()
                            draftStatuses = emptySet()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_clear_all),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HabitColors.RoyalPurple
                        )
                    }
                }
            }

            HorizontalDivider(color = HabitColors.Outline.copy(alpha = 0.5f), thickness = 1.dp)

            // 2. Flipkart-Style Two-Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                // Left Pane: Filter Categories (Fixed 135dp width, stacked cleanly)
                Column(
                    modifier = Modifier
                        .width(135.dp)
                        .fillMaxHeight()
                        .background(HabitColors.Parchment.copy(alpha = 0.45f))
                ) {
                    CategoryTab(
                        label = "Habit Name",
                        count = draftHabitIds.size,
                        isSelected = selectedCategory == HabitFilterCategory.HABIT_NAME,
                        onClick = { selectedCategory = HabitFilterCategory.HABIT_NAME }
                    )
                    CategoryTab(
                        label = "Frequency",
                        count = draftFrequencies.size,
                        isSelected = selectedCategory == HabitFilterCategory.FREQUENCY,
                        onClick = { selectedCategory = HabitFilterCategory.FREQUENCY }
                    )
                    CategoryTab(
                        label = "Status",
                        count = draftStatuses.size,
                        isSelected = selectedCategory == HabitFilterCategory.STATUS,
                        onClick = { selectedCategory = HabitFilterCategory.STATUS }
                    )
                }

                VerticalDivider(color = HabitColors.Outline.copy(alpha = 0.5f), thickness = 1.dp)

                // Right Pane: Filter Options for Selected Category
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (selectedCategory) {
                        HabitFilterCategory.HABIT_NAME -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(habits, key = { it.habit.id }) { hwc ->
                                    val isChecked = hwc.habit.id in draftHabitIds
                                    FilterCheckboxRow(
                                        label = hwc.habit.name,
                                        isChecked = isChecked,
                                        onToggle = {
                                            draftHabitIds = if (isChecked) {
                                                draftHabitIds - hwc.habit.id
                                            } else {
                                                draftHabitIds + hwc.habit.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        HabitFilterCategory.FREQUENCY -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(HabitFrequencyOption.entries) { option ->
                                    val isChecked = option in draftFrequencies
                                    FilterCheckboxRow(
                                        label = option.label,
                                        isChecked = isChecked,
                                        onToggle = {
                                            draftFrequencies = if (isChecked) {
                                                draftFrequencies - option
                                            } else {
                                                draftFrequencies + option
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        HabitFilterCategory.STATUS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(HabitStatusOption.entries) { option ->
                                    val isChecked = option in draftStatuses
                                    FilterCheckboxRow(
                                        label = option.label,
                                        isChecked = isChecked,
                                        onToggle = {
                                            draftStatuses = if (isChecked) {
                                                draftStatuses - option
                                            } else {
                                                draftStatuses + option
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = HabitColors.Outline.copy(alpha = 0.5f), thickness = 1.dp)

            // 3. Bottom Sticky Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        draftHabitIds = emptySet()
                        draftFrequencies = emptySet()
                        draftStatuses = emptySet()
                        onApply(HabitFilterState())
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HabitColors.TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HabitColors.Outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_reset), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onApply(
                            HabitFilterState(
                                selectedHabitIds = draftHabitIds,
                                selectedFrequencies = draftFrequencies,
                                selectedStatuses = draftStatuses
                            )
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HabitColors.RoyalPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (totalSelected > 0) "Apply ($totalSelected)" else "Apply (All Habits)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) HabitColors.Surface else Color.Transparent
    val textColor = if (isSelected) HabitColors.RoyalPurple else HabitColors.TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(bgColor)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left indicator bar (3.5dp) on the very left edge
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.5.dp)
                .background(if (isSelected) HabitColors.RoyalPurple else Color.Transparent)
        )

        // Label + Count Badge
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (count > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(HabitColors.RoyalPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterCheckboxRow(
    label: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.5.sp,
            fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isChecked) HabitColors.TextPrimary else HabitColors.TextSecondary,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Modern Rounded Checkbox
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isChecked) HabitColors.RoyalPurple else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isChecked) HabitColors.RoyalPurple else HabitColors.OutlineStrong,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
