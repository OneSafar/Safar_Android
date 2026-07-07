package com.safarparmar.app.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat

// ── Simple requester (already used in Ekagra + Profile — kept as-is) ──────────

@Composable
fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val activity = context.findActivity()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                context.openAppNotificationSettings()
            }
            onResult(granted)
        },
    )

    return remember(context, launcher) {
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onResult(true)
                return@remember
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                onResult(true)
            } else if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                context.openAppNotificationSettings()
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

// ── Rationale dialog + one-shot trigger ───────────────────────────────────────

/**
 * Shows a beautiful rationale dialog the first time the user reaches the Home screen,
 * then requests the OS permission if they accept. Only runs on Android 13+.
 *
 * Usage: call [NotificationPermissionRequest] anywhere inside a Composable that is
 * shown after login (e.g. HomeScreen). It handles its own visibility state.
 */
@Composable
fun NotificationPermissionRequest() {
    // Only needed on Android 13+ (TIRAMISU)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val activity = context.findActivity()

    // Already granted — nothing to do
    val alreadyGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (alreadyGranted) return

    // If the user has already dismissed the rationale once, don't nag them again
    if (context.hasDismissedNotificationRationale()) return

    var showDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                showSettingsDialog = true
            }
        },
    )

    // Show the rationale dialog after a short delay so the home screen renders first
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500L)
        showDialog = true
    }

    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f),
        exit  = fadeOut(tween(200)) + scaleOut(tween(200)),
    ) {
        NotificationRationaleDialog(
            onAllow = {
                showDialog = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onDismiss = {
                showDialog = false
                context.setDismissedNotificationRationale()
            },
        )
    }

    AnimatedVisibility(
        visible = showSettingsDialog,
        enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f),
        exit = fadeOut(tween(200)) + scaleOut(tween(200)),
    ) {
        NotificationSettingsDialog(
            onOpenSettings = {
                showSettingsDialog = false
                context.openAppNotificationSettings()
            },
            onDismiss = {
                showSettingsDialog = false
                context.setDismissedNotificationRationale()
            },
        )
    }
}

@Composable
private fun NotificationRationaleDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f)), // Standard scrim background
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 38.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(22.dp)
                        ),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Icon badge
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(27.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Stay in the loop",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Get reminders for your study goals, focus sessions, daily streaks, and new classes. We’ll also send important account alerts so you never miss a thing!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                        )

                        Spacer(Modifier.height(16.dp))

                        // Feature pills
                        val features = listOf(
                            com.safarparmar.app.R.drawable.ic_target to "Goal reminders",
                            com.safarparmar.app.R.drawable.ic_flame to "Streak & Focus alerts",
                            com.safarparmar.app.R.drawable.ic_graduation_cap to "New classes & updates",
                            com.safarparmar.app.R.drawable.ic_handshake to "Community & account alerts"
                        )
                        features.forEach { (iconRes, text) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = text,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Allow button
                        Button(
                            onClick = onAllow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(19.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                text = "Allow Notifications",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Dismiss link
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "Not now",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                }
            }
        }
    }
}
}

@Composable
private fun NotificationSettingsDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
            )
        },
        title = { Text("Notifications are off") },
        text = {
            Text("To receive study reminders and ekagra-session alerts, enable notifications from Android settings.")
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val NOTIFICATION_PROMPT_PREFS = "notification_prompt_prefs"
private const val KEY_DISMISSED_RATIONALE = "dismissed_rationale"

private fun Context.hasDismissedNotificationRationale(): Boolean =
    getSharedPreferences(NOTIFICATION_PROMPT_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_DISMISSED_RATIONALE, false)

private fun Context.setDismissedNotificationRationale() {
    getSharedPreferences(NOTIFICATION_PROMPT_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_DISMISSED_RATIONALE, true)
        .apply()
}

private fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:$packageName"))
    }
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
