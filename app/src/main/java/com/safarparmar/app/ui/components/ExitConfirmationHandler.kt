package com.safarparmar.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.safarparmar.app.R

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
            title = { Text(stringResource(R.string.exit_title)) },
            text = { Text(stringResource(R.string.exit_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onConfirmExit()
                }) {
                    Text(stringResource(R.string.exit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.exit_stay))
                }
            },
        )
    }
}
