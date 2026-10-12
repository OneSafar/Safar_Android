package com.safarparmar.app.ui.ekagra

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
            TextButton(onClick = service::confirmPresenceReminder) {
                Text(stringResource(R.string.common_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = service::declinePresenceReminder) {
                Text(stringResource(R.string.ekagra_presence_no))
            }
        },
    )
}
