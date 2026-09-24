package com.safarparmar.app.feature.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.feature.habits.data.HabitWithCompletions

@Composable
fun HabitFilterBar(
    habits: List<HabitWithCompletions>,
    filterState: HabitFilterState,
    onOpenFilterSheet: () -> Unit,
    onRemoveHabitId: (Long) -> Unit,
    onRemoveFrequency: (HabitFrequencyOption) -> Unit,
    onRemoveStatus: (HabitStatusOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = filterState.totalCount()
    val isFilterActive = totalCount > 0
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Primary Filter Button (Flipkart/Blinkit style)
        Surface(
            onClick = onOpenFilterSheet,
            shape = RoundedCornerShape(12.dp),
            color = if (isFilterActive) HabitColors.RoyalPurpleBg else HabitColors.Parchment,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (isFilterActive) HabitColors.RoyalPurple else HabitColors.Outline
            ),
            modifier = Modifier.height(38.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_filters),
                    tint = if (isFilterActive) HabitColors.RoyalPurple else HabitColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_filters),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFilterActive) HabitColors.RoyalPurple else HabitColors.TextPrimary
                )

                if (isFilterActive) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(HabitColors.RoyalPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = totalCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 2. Removable Active Chips
        if (!isFilterActive) {
            // androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_all) indicator chip
            Surface(
                onClick = onOpenFilterSheet,
                shape = RoundedCornerShape(12.dp),
                color = HabitColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, HabitColors.Outline.copy(alpha = 0.6f)),
                modifier = Modifier.height(38.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_all),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HabitColors.TextSecondary
                    )
                }
            }
        } else {
            // Selected Habit Chips
            filterState.selectedHabitIds.forEach { habitId ->
                val habitName = habits.find { it.habit.id == habitId }?.habit?.name ?: "Habit"
                RemovableFilterChip(
                    label = habitName,
                    onRemove = { onRemoveHabitId(habitId) }
                )
            }

            // Selected Frequency Chips
            filterState.selectedFrequencies.forEach { freq ->
                RemovableFilterChip(
                    label = freq.label,
                    onRemove = { onRemoveFrequency(freq) }
                )
            }

            // Selected Status Chips
            filterState.selectedStatuses.forEach { status ->
                RemovableFilterChip(
                    label = status.label,
                    onRemove = { onRemoveStatus(status) }
                )
            }
        }
    }
}

@Composable
private fun RemovableFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HabitColors.RoyalPurpleBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, HabitColors.RoyalPurple.copy(alpha = 0.4f)),
        modifier = Modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HabitColors.RoyalPurple
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.habits_remove_filter, label),
                    tint = HabitColors.RoyalPurple,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
