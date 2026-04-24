package com.safar.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*

/**
 * Intercepts the back button on root screens and shows a confirmation dialog
 * before exiting the app.
 *
 * Place this once in [MainActivity] or the root composable:
 * ```kotlin
 * ExitConfirmationHandler()
 * ```
 */
@Composable
fun ExitConfirmationHandler(
    onConfirmExit: () -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Exit Safar?") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onConfirmExit()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Stay")
                }
            },
        )
    }
}
