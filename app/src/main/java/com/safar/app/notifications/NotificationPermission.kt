package com.safar.app.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat

import com.safar.app.ui.theme.LoraFontFamily
import com.safar.app.ui.theme.shimmer

// ── Simple requester (already used in Ekagra + Profile — kept as-is) ──────────

@Composable
fun rememberNotificationPermissionRequester(
    onResult: (Boolean) -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onResult,
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

    // Already granted — nothing to do
    val alreadyGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (alreadyGranted) return

    var showDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* nothing extra needed; OS dialog handles the result */ },
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
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1A1F2E)),
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
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF3DAC78), Color(0xFF073B3A)),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Stay in the loop",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LoraFontFamily,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Get reminders for your goals, focus sessions, streak check-ins, and community activity — so you never miss a beat.",
                    fontSize = 14.sp,
                    color = Color(0xFFB0BAD3),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )

                Spacer(Modifier.height(8.dp))

                // Feature pills
                val features = listOf(
                    com.safar.app.R.drawable.ic_target to "Goal reminders",
                    com.safar.app.R.drawable.ic_flame to "Streak alerts",
                    com.safar.app.R.drawable.ic_handshake to "Community updates"
                )
                features.forEach { (iconRes, text) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF252B3B))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(iconRes),
                            contentDescription = null,
                            tint = Color(0xFFCDD5E0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            color = Color(0xFFCDD5E0),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Allow button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .shimmer()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF3DAC78), Color(0xFF073B3A)),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(
                        onClick = onAllow,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = "Allow Notifications",
                            fontWeight = FontWeight.Bold,
                            fontFamily = LoraFontFamily,
                            fontSize = 17.sp,
                            color = Color.White,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Dismiss link
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Not now",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7A99),
                    )
                }
            }
        }
    }
}
