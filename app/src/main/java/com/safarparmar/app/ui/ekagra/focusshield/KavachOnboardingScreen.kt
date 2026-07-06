package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import kotlinx.coroutines.launch

private data class KavachPermissionColors(
    val screen: Color,
    val card: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val divider: Color,
    val progress: Color,
    val progressTrack: Color,
    val helpBg: Color,
    val helpText: Color,
    val cta: Color,
    val ctaText: Color,
)

@Composable
private fun kavachPermissionColors(): KavachPermissionColors {
    val scheme = MaterialTheme.colorScheme
    return KavachPermissionColors(
        screen = scheme.background,
        card = scheme.surfaceContainerHigh,
        primaryText = scheme.onBackground,
        secondaryText = scheme.onSurfaceVariant,
        divider = scheme.outlineVariant,
        progress = scheme.primary,
        progressTrack = scheme.surfaceContainerHighest,
        helpBg = scheme.primaryContainer.copy(alpha = 0.72f),
        helpText = scheme.onPrimaryContainer,
        cta = scheme.primary,
        ctaText = scheme.onPrimary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachOnboardingScreen(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    viewModel: FocusShieldViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = kavachPermissionColors()
    val scheme = MaterialTheme.colorScheme
    val isLightMode = scheme.background.luminance() > 0.5f
    val logoRes = if (isLightMode) {
        R.drawable.ic_safar_logo_brand_light
    } else {
        R.drawable.ic_safar_logo_brand_dark
    }
    val shieldState by viewModel.shieldState.collectAsStateWithLifecycle()
    var hasUsageStats by remember { mutableStateOf(shieldState.hasUsageStats) }
    var hasAccessibility by remember { mutableStateOf(shieldState.hasAccessibilityService) }
    var hasNotifications by remember { mutableStateOf(shieldState.hasNotifications) }
    var hasNotificationSuppressionAccess by remember { mutableStateOf(shieldState.hasNotificationSuppressionAccess) }
    var selectedPermission by remember { mutableStateOf<PermissionTarget?>(null) }
    val accessibilityRequired = FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) { onBack() }

    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasAccessibility = FocusShieldPermissionHelper.hasAccessibilityService(context)
                hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
                hasNotificationSuppressionAccess = FocusShieldPermissionHelper.hasNotificationListenerAccess(context)
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsageStats, hasAccessibility, hasNotificationSuppressionAccess) {
        if (hasUsageStats && (!accessibilityRequired || hasAccessibility) && hasNotificationSuppressionAccess) {
            viewModel.setEnabled(true)
            onFinished()
        }
    }

    val totalSteps = if (accessibilityRequired) 3 else 2
    var grantedCount = 0
    if (hasUsageStats) grantedCount++
    if (hasAccessibility && accessibilityRequired) grantedCount++
    if (hasNotificationSuppressionAccess) grantedCount++
    val progress = (grantedCount.toFloat() / totalSteps).coerceAtMost(1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "kavachPermissionProgress")

    val isUsageNext = !hasUsageStats
    val isAccessibilityNext = hasUsageStats && accessibilityRequired && !hasAccessibility
    val isNotificationAccessNext = hasUsageStats &&
        (!accessibilityRequired || hasAccessibility) &&
        !hasNotificationSuppressionAccess
    val isNotificationsNext = hasUsageStats &&
        (!accessibilityRequired || hasAccessibility) &&
        hasNotificationSuppressionAccess &&
        !hasNotifications

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.screen,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(6.dp)
                    .clip(CircleShape),
                color = colors.progress,
                trackColor = colors.progressTrack,
                strokeCap = StrokeCap.Round,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(scheme.surfaceContainerHighest)
                            .border(
                                width = 1.dp,
                                color = scheme.outlineVariant,
                                shape = CircleShape,
                            )
                            .padding(13.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = "SAFAR",
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Surface(
                        color = colors.card,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "Almost there. Allow these permissions to use KAVACH.",
                            color = colors.secondaryText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                KavachRegainPermissionRow(
                    title = "Usage permission",
                    subtitle = "Lets KAVACH check which app you opened.",
                    granted = hasUsageStats,
                    isNext = isUsageNext,
                    colors = colors,
                    onClick = { if (isUsageNext) selectedPermission = PermissionTarget.USAGE_STATS },
                )

                HorizontalDivider(color = colors.divider)

                if (accessibilityRequired) {
                    KavachRegainPermissionRow(
                        title = "Accessibility Service",
                        subtitle = "Required to monitor app launches and display the blocking shield screen over distracting apps.",
                        granted = hasAccessibility,
                        isNext = isAccessibilityNext,
                        colors = colors,
                        onClick = { if (isAccessibilityNext) selectedPermission = PermissionTarget.ACCESSIBILITY },
                    )
                    HorizontalDivider(color = colors.divider)
                }

                KavachRegainPermissionRow(
                    title = "Notification Shield",
                    subtitle = "Required to dismiss notifications from the apps you selected for blocking.",
                    granted = hasNotificationSuppressionAccess,
                    isNext = isNotificationAccessNext,
                    colors = colors,
                    onClick = {
                        if (isNotificationAccessNext) selectedPermission = PermissionTarget.NOTIFICATION_ACCESS
                    },
                )

                HorizontalDivider(color = colors.divider)

                KavachRegainPermissionRow(
                    title = "Background permission",
                    subtitle = "Keeps KAVACH status and timer alerts working.",
                    granted = hasNotifications,
                    isNext = isNotificationsNext,
                    colors = colors,
                    onClick = {
                        if (isNotificationsNext) requestNotificationPermission()
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.helpBg)
                        .clickable { selectedPermission = PermissionTarget.USAGE_STATS }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = colors.helpText,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Why should I give this permission?",
                        color = colors.helpText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.helpText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        selectedPermission?.let { permission ->
            KavachRegainExplanationSheet(
                permission = permission,
                onDismiss = { selectedPermission = null },
                onAllow = {
                    selectedPermission = null
                    scope.launch {
                        when (it) {
                            PermissionTarget.USAGE_STATS -> {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                            PermissionTarget.ACCESSIBILITY -> {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                            PermissionTarget.NOTIFICATIONS -> requestNotificationPermission()
                            PermissionTarget.NOTIFICATION_ACCESS ->
                                FocusShieldPermissionHelper.openNotificationListenerSettings(context)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun KavachRegainPermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    isNext: Boolean,
    colors: KavachPermissionColors,
    onClick: () -> Unit,
) {
    val titleColor = if (granted || isNext) colors.primaryText else colors.secondaryText.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isNext && !granted, onClick = onClick)
            .padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
            )
            if (isNext && !granted) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = colors.secondaryText,
                    lineHeight = 20.sp,
                )
            }
        }
        Spacer(Modifier.width(16.dp))

        if (granted) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Granted",
                tint = colors.helpText,
                modifier = Modifier.size(24.dp),
            )
        } else if (isNext) {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(99.dp),
                color = colors.cta,
                modifier = Modifier.height(36.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Allow",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.ctaText,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KavachRegainExplanationSheet(
    permission: PermissionTarget,
    onDismiss: () -> Unit,
    onAllow: (PermissionTarget) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = kavachPermissionColors()

    val title = when (permission) {
        PermissionTarget.USAGE_STATS -> "Allow usage permission to check app use"
        PermissionTarget.ACCESSIBILITY -> "Enable KAVACH - Focus Shield"
        PermissionTarget.NOTIFICATIONS -> "Allow notifications for status"
        PermissionTarget.NOTIFICATION_ACCESS -> "Enable Notification shield"
    }

    val bullets = when (permission) {
        PermissionTarget.USAGE_STATS -> listOf(
            "We do not read your messages, passwords, photos, or screen content.",
            "Usage Access helps SAFAR understand which app is currently open.",
            "SAFAR uses this only for KAVACH Focus Shield to compare opened apps with the apps you selected for blocking.",
            "SAFAR does not use Usage Access for ads, marketing, or third-party sharing.",
            "SAFAR does not upload the names of apps you open or block.",
            "This helps KAVACH block only the apps you choose during an active Ekagra focus timer session.",
        )
        PermissionTarget.ACCESSIBILITY -> listOf(
            "KAVACH is optional. It helps you stay focused by blocking only the distracting apps you choose during an active Ekagra focus timer session.",
            "To do this, SAFAR uses Android Accessibility Service only for KAVACH Focus Shield. When you open an app you selected for blocking, SAFAR detects that app's package name, checks it against your blocked app list, and shows SAFAR's own KAVACH block screen so you can return to your focus session.",
            "SAFAR does not use Accessibility Service to read your screen, messages, passwords, typed text, notifications, contacts, photos, or files.",
            "SAFAR does not use Accessibility Service to click buttons, perform gestures, change settings, prevent uninstall, or control your phone.",
            "SAFAR does not request the Display over other apps permission. KAVACH shows SAFAR's own block screen only when a user-selected blocked app opens during an active focus session.",
            "SAFAR does not collect, sell, store, or share Accessibility data for ads, analytics, or third parties. All processing happens locally on your device.",
            "SAFAR does not upload the names of apps you open or block.",
            "You can keep using SAFAR without KAVACH, and you can turn this permission off anytime in Android Settings.",
        )
        PermissionTarget.NOTIFICATIONS -> listOf(
            "Receive alerts when the Ekagra timer finishes.",
            "Keep KAVACH status visible in the background.",
            "Foreground service and media playback notifications keep active Ekagra sessions visible and controllable outside the app.",
        )
        PermissionTarget.NOTIFICATION_ACCESS -> listOf(
            "KAVACH can dismiss notifications only from apps you selected to block.",
            "This works only while an Ekagra focus timer is running.",
            "SAFAR does not store notification content or use it for ads.",
            "You can turn Notification access off anytime in Android Settings.",
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primaryText,
                lineHeight = 30.sp,
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.helpBg)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = colors.helpText,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Is it safe to give this permission?",
                    color = colors.helpText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.helpText,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.padding(bottom = 20.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "-",
                        color = colors.secondaryText.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = bullet,
                        color = colors.secondaryText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                onClick = { onAllow(permission) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(99.dp),
                color = colors.cta,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (permission == PermissionTarget.ACCESSIBILITY) {
                            "Agree & enable KAVACH"
                        } else if (permission == PermissionTarget.NOTIFICATION_ACCESS) {
                            "Agree & enable Notification Shield"
                        } else {
                            "Allow"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ctaText,
                    )
                }
            }

            if (permission == PermissionTarget.ACCESSIBILITY) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(99.dp),
                    color = Color.Transparent,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Not now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.secondaryText,
                        )
                    }
                }
            }
        }
    }
}
