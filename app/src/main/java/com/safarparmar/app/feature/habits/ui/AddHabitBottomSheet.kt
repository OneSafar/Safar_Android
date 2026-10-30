package com.safarparmar.app.feature.habits.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import java.util.UUID
import java.time.DayOfWeek

@Composable
fun AddHabitBottomSheet(
    onDismiss: () -> Unit,
    onCreate: suspend (String, Set<DayOfWeek>, Boolean, String?, String) -> Unit,
    onCreated: (String, Set<DayOfWeek>) -> Unit = { _, _ -> }
) {
    val operationId = rememberSaveable { UUID.randomUUID().toString() }
    HabitFormBottomSheet(onDismiss = onDismiss,
        onSubmit = { name, days, everyDay, reminder -> onCreate(name, days, everyDay, reminder, operationId) },
        onSuccess = onCreated)
}
