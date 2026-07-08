package com.safarparmar.app.ui.studyplanner.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { CompositionLocalProvider(LocalDensity provides clampedDensity) { Text(title) } },
        text = {
            CompositionLocalProvider(LocalDensity provides clampedDensity) {
                OutlinedTextField(
                    text,
                    { text = it },
                    label = { Text(label) },
                    supportingText = { if (text.isBlank()) Text(emptyHint) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            CompositionLocalProvider(LocalDensity provides clampedDensity) {
                TextButton(enabled = text.trim().length >= 2, onClick = { onConfirm(text.trim()) }) { Text(confirmLabel) }
            }
        },
        dismissButton = {
            CompositionLocalProvider(LocalDensity provides clampedDensity) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
