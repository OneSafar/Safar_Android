package com.safarparmar.app.ui.studyplanner.plan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.safarparmar.app.ui.studyplanner.components.PlannerDialog
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogAction
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogText
import com.safarparmar.app.ui.studyplanner.components.PlannerDialogTextAction

@Composable
fun PlanConfirmDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    PlannerDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = { PlannerDialogText(body) },
        dismissButton = { PlannerDialogTextAction("Cancel", onClick = onDismiss) },
        confirmButton = {
            // Destructive confirm keeps the error colour — GlassButton renders it
            // as translucent glass without changing the colour itself.
            PlannerDialogAction(
                text = "Confirm",
                onClick = onConfirm,
                accentColor = MaterialTheme.colorScheme.error,
            )
        },
    )
}
