package com.safarparmar.app.ui.debug

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.safarparmar.app.notifications.NotificationAvailabilityReason
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTestPanel(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val manager = remember { SafarNotificationManager(context) }
    val appAvailability = remember { manager.evaluateNotificationAvailability() }
    val permissionGranted = remember {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    val channelIds = remember {
        listOf(
            SafarNotificationChannels.FOCUS_TIMER,
            SafarNotificationChannels.FOCUS_SHIELD_STATUS,
            SafarNotificationChannels.FOCUS_SHIELD_BLOCKED,
            SafarNotificationChannels.STUDY_REMINDERS,
            SafarNotificationChannels.COURSE_UPDATES,
            SafarNotificationChannels.ACHIEVEMENTS,
            SafarNotificationChannels.COMMUNITY,
            SafarNotificationChannels.ACCOUNT_SYSTEM,
            SafarNotificationChannels.ANNOUNCEMENTS,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Notification Debug") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("System status", style = MaterialTheme.typography.titleMedium)
            Text("POST_NOTIFICATIONS: ${if (permissionGranted) "granted" else "denied"}")
            Text("App notifications: ${appAvailability.reason}")
            channelIds.forEach { channelId ->
                val availability = manager.evaluateNotificationAvailability(channelId)
                val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.getSystemService(NotificationManager::class.java)
                        .getNotificationChannel(channelId)
                        ?.importance
                        ?.toString() ?: "missing"
                } else {
                    "n/a"
                }
                val blocked = availability.reason == NotificationAvailabilityReason.channel_blocked
                Text("$channelId — importance=$importance, status=${availability.reason}, blocked=$blocked")
            }

            Text("Triggers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            DebugButton("Custom local notification") {
                NotificationDebugActions.runCommand(context, "local")
            }
            DebugButton("Morning nudge worker") {
                NotificationDebugActions.runCommand(context, "morning_nudge")
            }
            DebugButton("Study reminder worker") {
                NotificationDebugActions.runCommand(context, "study_reminder")
            }
            DebugButton("Planner alerts worker") {
                NotificationDebugActions.runCommand(context, "planner_alerts")
            }
            DebugButton("Focus timer completion") {
                NotificationDebugActions.runCommand(context, "focus_complete")
            }
            DebugButton("Kavach blocked notification") {
                NotificationDebugActions.runCommand(context, "shield_blocked")
            }
            DebugButton("Kavach status notification") {
                NotificationDebugActions.runCommand(context, "shield_status")
            }
        }
    }
}

@Composable
private fun DebugButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}
