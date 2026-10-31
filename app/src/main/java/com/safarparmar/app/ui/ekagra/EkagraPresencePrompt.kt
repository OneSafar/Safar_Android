package com.safarparmar.app.ui.ekagra

import com.composables.ui.components.Button
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text

import com.safarparmar.app.ui.components.popups.SafarMaterialAlertDialog as AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R

@Composable
fun EkagraPresencePrompt(timerService: TimerService?) {
    val service = timerService ?: return
    val deadline by service.presenceCheckInDeadline.collectAsStateWithLifecycle()
    val dismissed by service.presencePromptDismissed.collectAsStateWithLifecycle()
    if (deadline == null || dismissed) return

    AlertDialog(
        onDismissRequest = service::declinePresenceReminder,
        title = { Text(stringResource(R.string.ekagra_presence_question)) },
        text = { Text(stringResource(R.string.ekagra_presence_ranked_body)) },
        confirmButton = {
            Button(style = com.composables.ui.components.ButtonStyle.Ghost, onClick = service::confirmPresenceReminder) {
                Text(stringResource(R.string.common_yes))
            }
        },
        dismissButton = {
            Button(style = com.composables.ui.components.ButtonStyle.Ghost, onClick = service::declinePresenceReminder) {
                Text(stringResource(R.string.ekagra_presence_no))
            }
        },
    )
}
