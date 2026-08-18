package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R

enum class PermissionTarget {
    USAGE_STATS,
    OVERLAY,
    NOTIFICATIONS,
    NOTIFICATION_ACCESS,
}

@Composable
fun KavachPermissionSetupScreen(
    usageStatsGranted: Boolean,
    overlayGranted: Boolean,
    notificationsGranted: Boolean,
    notificationAccessGranted: Boolean,
    onRequestUsageStats: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val allRequiredGranted = usageStatsGranted && overlayGranted

    Scaffold(
        topBar = {
            if (onBack != null) {
                Box(modifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp)) {
                    KavachCircularBackButton(onClick = onBack)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "K A V A C H",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KavachDesign.Primary,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = "App Shield Permissions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Enable permissions below so KAVACH can block app distractions during study sessions.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PermissionCard(
                title = "App Check (Usage Access)",
                description = "Required to detect when a blocked app is opened.",
                isGranted = usageStatsGranted,
                icon = Icons.Default.Security,
                onClick = onRequestUsageStats,
            )

            PermissionCard(
                title = "Display Over Apps (Overlay)",
                description = "Required to show the focus screen over blocked apps.",
                isGranted = overlayGranted,
                icon = Icons.Default.Shield,
                onClick = onRequestOverlay,
            )

            PermissionCard(
                title = "Notification Shield (Optional)",
                description = "Dismisses notifications from blocked apps while Ekagra timer is running.",
                isGranted = notificationAccessGranted,
                icon = Icons.Default.NotificationsActive,
                onClick = onRequestNotificationAccess,
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onContinue,
                enabled = allRequiredGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = CircleShape,
            ) {
                Text(
                    text = if (allRequiredGranted) "Continue" else "Enable Required Permissions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) scheme.surfaceVariant.copy(alpha = 0.4f) else scheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) Color(0xFF22C55E).copy(alpha = 0.15f)
                        else KavachDesign.Primary.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF22C55E) else KavachDesign.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = scheme.onSurface,
                )
                Text(
                    text = description,
                    fontSize = 12.5.sp,
                    color = scheme.onSurfaceVariant,
                )
            }

            if (!isGranted) {
                Text(
                    text = "Grant",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = KavachDesign.Primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideSheet(
    permission: PermissionTarget,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    val titleText = when (permission) {
        PermissionTarget.USAGE_STATS -> "Allow app check"
        PermissionTarget.OVERLAY -> "Allow show on top"
        PermissionTarget.NOTIFICATIONS -> "Allow notifications"
        PermissionTarget.NOTIFICATION_ACCESS -> "Allow notification shield"
    }

    val primaryButtonText = when (permission) {
        PermissionTarget.USAGE_STATS -> "Allow"
        PermissionTarget.OVERLAY -> "Allow"
        PermissionTarget.NOTIFICATIONS -> "Allow Notifications"
        PermissionTarget.NOTIFICATION_ACCESS -> "Agree & enable Notification Shield"
    }

    val bulletPoints = when (permission) {
        PermissionTarget.USAGE_STATS -> listOf(
            "We do not read your messages, passwords, or photos.",
            "This only tells SAFAR which app is open right now.",
            "SAFAR uses it only to block the apps you chose.",
            "Nothing is shared for ads, and app names are not uploaded."
        )
        PermissionTarget.OVERLAY -> listOf(
            "This lets SAFAR show a block screen over a distracting app.",
            "It is used only while you are studying with the timer on.",
            "SAFAR does not read or capture other apps.",
            "You can turn this off anytime in phone Settings."
        )
        PermissionTarget.NOTIFICATIONS -> listOf(
            "Shows Ekagra study timer progress and break alerts.",
            "Works only on your phone and is never shared for ads.",
            "This permission is optional — KAVACH works without it.",
            "You can turn this off anytime in phone Settings."
        )
        PermissionTarget.NOTIFICATION_ACCESS -> listOf(
            "KAVACH can hide notifications only from apps you chose to block.",
            "This works only while your study timer is on.",
            "SAFAR does not save notification text.",
            "You can turn this off anytime in phone Settings."
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "K A V A C H",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = KavachDesign.Primary,
                    letterSpacing = 2.sp,
                )
                Text(
                    text = titleText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.35f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(KavachDesign.Primary.copy(alpha = 0.25f))
                ),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(KavachDesign.Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KavachDesign.Primary)
                        }
                        Text(
                            "Is it safe to give this permission?",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                bulletPoints.forEachIndexed { index, bullet ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "—",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Text(
                            bullet,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (index < bulletPoints.size - 1) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(scheme.outlineVariant.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KavachDesign.Primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    primaryButtonText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                shape = CircleShape,
            ) {
                Text(
                    "Not now",
                    color = scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun PermissionGrantedBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val granted = Color(0xFF22C55E)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(granted.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = granted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text,
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
