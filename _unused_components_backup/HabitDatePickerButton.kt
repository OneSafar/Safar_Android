package com.safarparmar.app.feature.habits.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HabitDatePickerButton(
    selected: LocalDate,
    label: String,
    onSelect: (LocalDate) -> Unit,
    latest: LocalDate? = null
) {
    var open by remember { mutableStateOf(false) }
    com.composables.ui.components.IconButton(style = com.composables.ui.components.ButtonStyle.Ghost, onClick = { open = true }) {
        Icon(Icons.Default.DateRange, contentDescription = label, tint = HabitColors.RoyalPurple)
    }
    if (open) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = latest == null ||
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(latest)
                override fun isSelectableYear(year: Int): Boolean = latest == null || year <= latest.year
            }
        )
        DatePickerDialog(onDismissRequest = { open = false }, confirmButton = {
            com.composables.ui.components.Button(style = com.composables.ui.components.ButtonStyle.Ghost, enabled = picker.selectedDateMillis != null, onClick = {
                picker.selectedDateMillis?.let {
                    onSelect(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                open = false
            }) { Text("Open") }
        }, dismissButton = { com.composables.ui.components.Button(style = com.composables.ui.components.ButtonStyle.Ghost, onClick = { open = false }) { Text("Cancel") } }) {
            DatePicker(state = picker, title = { Text(label, modifier = androidx.compose.ui.Modifier) })
        }
    }
}
