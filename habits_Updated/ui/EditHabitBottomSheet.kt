package com.safarparmar.app.feature.habits.ui

import androidx.compose.runtime.Composable
import com.safarparmar.app.feature.habits.data.HabitEntity

@Composable
fun EditHabitBottomSheet(
    habit: HabitEntity,
    onDismiss: () -> Unit,
    onSave: suspend (HabitEntity) -> Unit,
    onArchive: suspend (Long) -> Unit,
    onDelete: suspend (HabitEntity) -> Unit
) {
    HabitFormBottomSheet(
        habit = habit,
        onDismiss = onDismiss,
        onSubmit = { name, days, isEveryDay ->
            // Schedule history is versioned atomically by the repository. The UI only
            // submits the new visible state; it must not rewrite historical dates itself.
            onSave(habit.copy(name = name, targetDays = days, isEveryDay = isEveryDay))
        },
        onArchive = { onArchive(habit.id) },
        onDelete = { onDelete(habit) }
    )
}
