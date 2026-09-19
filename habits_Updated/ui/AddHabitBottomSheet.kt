package com.safarparmar.app.feature.habits.ui

import androidx.compose.runtime.Composable
import java.time.DayOfWeek

@Composable
fun AddHabitBottomSheet(
    onDismiss: () -> Unit,
    onCreate: suspend (String, Set<DayOfWeek>, Boolean) -> Unit,
    onCreated: (String, Set<DayOfWeek>) -> Unit = { _, _ -> }
) {
    HabitFormBottomSheet(onDismiss = onDismiss, onSubmit = onCreate, onSuccess = onCreated)
}
