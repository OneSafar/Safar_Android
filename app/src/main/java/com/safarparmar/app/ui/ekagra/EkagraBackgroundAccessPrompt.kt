package com.safarparmar.app.ui.ekagra

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.popups.SafarMaterialAlertDialog
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper

/** Observes actual study starts, covering focus, stopwatch, Pomodoro and restored sessions. */
@Composable
internal fun EkagraBackgroundAccessPrompt(sessionId: String?, runningStudy: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var state by rememberSaveable(stateSaver = listSaver(
        save = { listOf(it.promptedSessionId.orEmpty(), it.visible, it.waitingForSettings) },
        restore = { EkagraBackgroundAccessPromptState((it[0] as String).ifBlank { null }, it[1] as Boolean, it[2] as Boolean) },
    )) { mutableStateOf(EkagraBackgroundAccessPromptState()) }
    var settingsUnavailable by rememberSaveable { mutableStateOf(false) }
    val currentRunning by rememberUpdatedState(runningStudy)

    LaunchedEffect(sessionId, runningStudy) {
        state = state.sessionChanged(sessionId, runningStudy, FocusShieldPermissionHelper.hasBackgroundRunningAccess(context))
    }
    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Settings activities often return RESULT_CANCELED even after a grant.
                // Check the shared OS permission, never infer consent from returning.
                val granted = FocusShieldPermissionHelper.hasBackgroundRunningAccess(context)
                state = if (currentRunning) state.settingsReturned(granted) else state.dismiss()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (state.visible && runningStudy) {
        SafarMaterialAlertDialog(
            onDismissRequest = { state = state.dismiss() },
            title = { Text(stringResource(R.string.ekagra_background_access_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ekagra_background_access_body))
                    Text(stringResource(R.string.ekagra_background_access_steps))
                    if (settingsUnavailable) Text(stringResource(R.string.ekagra_background_access_unavailable))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    state = state.openSettings()
                    settingsUnavailable = !FocusShieldPermissionHelper.tryOpenBatterySaverSettings(context)
                    if (settingsUnavailable) state = state.settingsReturned(granted = false)
                }) { Text(stringResource(R.string.kavach_allow_background_running)) }
            },
            dismissButton = {
                TextButton(onClick = { state = state.dismiss() }) { Text(stringResource(R.string.ekagra_not_now)) }
            },
        )
    }
}
