package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density

/**
 * Shared "type a name, confirm" dialog — used by both the live Syllabus screen (which
 * mutates a real saved plan) and the Create-Plan wizard's manual topic-tree step (which
 * only touches in-memory draft state). Neither caller's mutation logic lives here; it's
 * purely a `(String) -> Unit` callback, so both flows share one look with zero coupling.
 * [confirmLabel] defaults to "Save" (renaming an existing item) — pass "Add" when creating
 * a brand-new one.
 */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    onDismiss: () -> Unit,
    confirmLabel: String = "Save",
    emptyHint: String = "Please type a name first",
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f),
        )
    }
    PlannerDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = {
            CompositionLocalProvider(LocalDensity provides clampedDensity) {
                OutlinedTextField(
                    text,
                    { text = it },
                    label = { Text(label) },
                    supportingText = { if (text.isBlank()) Text(emptyHint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (text.trim().length >= 2) onConfirm(text.trim()) },
                    ),
                )
            }
        },
        dismissButton = {
            CompositionLocalProvider(LocalDensity provides clampedDensity) {
                PlannerDialogTextAction("Cancel", onClick = onDismiss)
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalDensity provides clampedDensity) {
                PlannerDialogAction(
                    text = confirmLabel,
                    onClick = { onConfirm(text.trim()) },
                    enabled = text.trim().length >= 2,
                )
            }
        },
    )
}
