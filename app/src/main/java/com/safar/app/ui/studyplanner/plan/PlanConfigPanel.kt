package com.safar.app.ui.studyplanner.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.ui.studyplanner.components.PlannerExamDateField

@Composable
fun PlanConfigPanel(
    settingsExpanded: Boolean,
    onToggleSettings: () -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    examType: String,
    onExamTypeChange: (String) -> Unit,
    examDate: String,
    onExamDateChange: (String) -> Unit,
    dailyGoal: String,
    onDailyGoalChange: (String) -> Unit,
    offDays: Set<Int>,
    onToggleOffDay: (Int) -> Unit,
    onSave: () -> Unit,
    dangerExpanded: Boolean,
    onToggleDanger: () -> Unit,
    onResetPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PlanShapes.panel,
        color = scheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleSettings)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Plan Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = scheme.onSurface,
                    )
                    if (!settingsExpanded) {
                        Text(
                            text = "$title · $dailyGoal topics/day",
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    imageVector = if (settingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (settingsExpanded) "Collapse" else "Expand",
                    tint = scheme.onSurfaceVariant,
                )
            }

            if (settingsExpanded) {
                CompactPlanField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "Plan title",
                )
                CompactPlanField(
                    value = examType,
                    onValueChange = onExamTypeChange,
                    label = "Exam type",
                )
                PlannerExamDateField(
                    examDateIso = examDate,
                    onExamDateChange = onExamDateChange,
                    isError = false,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Topics per day",
                        fontSize = 14.sp,
                        color = scheme.onSurface,
                    )
                    OutlinedTextField(
                        value = dailyGoal,
                        onValueChange = onDailyGoalChange,
                        modifier = Modifier.widthIn(min = 56.dp, max = 72.dp),
                        singleLine = true,
                        maxLines = 1,
                        shape = PlanShapes.field,
                    )
                }
                PlanRestDaysRow(
                    selected = offDays,
                    onToggle = onToggleOffDay,
                )
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Save Details", fontWeight = FontWeight.SemiBold)
                }

                HorizontalDivider(color = scheme.outline.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleDanger)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "More Options",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (dangerExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                    )
                }
                if (dangerExpanded) {
                    Button(
                        onClick = onResetPlan,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.error),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Reset Plan")
                    }
                }
            } else {
                TextButton(
                    onClick = onToggleSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Edit plan details", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CompactPlanField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        maxLines = 1,
        shape = PlanShapes.field,
    )
}
