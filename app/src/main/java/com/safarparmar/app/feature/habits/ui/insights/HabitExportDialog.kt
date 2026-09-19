package com.safarparmar.app.feature.habits.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.export.ExportDateRangeOption
import com.safarparmar.app.feature.habits.ui.HabitColors
import java.time.LocalDate

@Composable
fun HabitExportDialog(
    availableHabits: List<HabitEntity>,
    onDismiss: () -> Unit,
    onExport: (rangeOption: ExportDateRangeOption, customStart: LocalDate?, customEnd: LocalDate?, selectedHabitIds: Set<Long>?) -> Unit
) {
    val outlineColor = HabitColors.Outline
    val surfaceColor = HabitColors.Surface
    val textPrimary = HabitColors.TextPrimary
    val textSecondary = HabitColors.TextSecondary
    val purple = HabitColors.RoyalPurple

    var selectedRange by remember { mutableStateOf(ExportDateRangeOption.THIS_MONTH) }
    var exportAllHabits by remember { mutableStateOf(true) }
    val selectedHabitIds = remember { mutableStateListOf<Long>() }

    // Initialize with all habits selected if user chooses specific habits
    LaunchedEffect(availableHabits) {
        if (selectedHabitIds.isEmpty()) {
            selectedHabitIds.addAll(availableHabits.map { it.id })
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Export Habit Data",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "Download spreadsheet-compatible data",
                    fontSize = 12.sp,
                    color = textSecondary
                )

                Spacer(Modifier.height(18.dp))

                // Date Range Option
                Text(
                    text = "Date Range",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(Modifier.height(6.dp))

                listOf(
                    ExportDateRangeOption.THIS_MONTH to "This Month",
                    ExportDateRangeOption.THIS_YEAR to "This Year",
                    ExportDateRangeOption.ALL_TIME to "All Time"
                ).forEach { (opt, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRange = opt }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRange == opt,
                            onClick = { selectedRange = opt },
                            colors = RadioButtonDefaults.colors(selectedColor = purple)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            color = textPrimary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Habits Filter
                Text(
                    text = "Habits",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportAllHabits = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = exportAllHabits,
                        onClick = { exportAllHabits = true },
                        colors = RadioButtonDefaults.colors(selectedColor = purple)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "All Habits", fontSize = 14.sp, color = textPrimary)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { exportAllHabits = false }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !exportAllHabits,
                        onClick = { exportAllHabits = false },
                        colors = RadioButtonDefaults.colors(selectedColor = purple)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Selected Habits", fontSize = 14.sp, color = textPrimary)
                }

                if (!exportAllHabits) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        availableHabits.forEach { habit ->
                            val checked = habit.id in selectedHabitIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedHabitIds.remove(habit.id)
                                        else selectedHabitIds.add(habit.id)
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        if (isChecked) selectedHabitIds.add(habit.id)
                                        else selectedHabitIds.remove(habit.id)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = purple)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = habit.name,
                                    fontSize = 13.sp,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Format: CSV
                Text(
                    text = "Format",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = true,
                        onClick = { },
                        colors = RadioButtonDefaults.colors(selectedColor = purple)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "CSV (.csv - Excel & Sheets compatible)",
                        fontSize = 14.sp,
                        color = textPrimary
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Action Buttons: Cancel and Export
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = textSecondary)
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val habitIds = if (exportAllHabits) null else selectedHabitIds.toSet()
                            onExport(selectedRange, null, null, habitIds)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = purple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}
