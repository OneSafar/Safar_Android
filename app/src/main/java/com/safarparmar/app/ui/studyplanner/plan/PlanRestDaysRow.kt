package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safarparmar.app.R
import java.time.DayOfWeek
import java.time.format.TextStyle

/** Native selectable chips wrap on narrow screens instead of squeezing seven hit targets. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanRestDaysRow(selected: Set<Int>, onToggle: (Int) -> Unit, modifier: Modifier = Modifier) {
    val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.planner_warm_rest_days), style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            (0..6).forEach { index ->
                val day = if (index == 0) DayOfWeek.SUNDAY else DayOfWeek.of(index)
                FilterChip(selected = index in selected, onClick = { onToggle(index) },
                    label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
            }
        }
    }
}
