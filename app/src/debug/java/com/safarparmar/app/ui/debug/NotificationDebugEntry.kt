package com.safarparmar.app.ui.debug

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun NotificationDebugSettingsEntry() {
    val context = LocalContext.current
    Text(
        "Debug tools for local notification testing (debug builds only).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Button(
        onClick = {
            context.startActivity(Intent(context, NotificationTestPanelActivity::class.java))
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Open notification debug panel")
    }
}
