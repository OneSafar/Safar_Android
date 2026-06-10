package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.safarparmar.app.R
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import kotlinx.coroutines.delay

enum class KavachStartBlock {
    None,
    NeedsPermissions,
    NeedsApps,
}

fun kavachStartBlock(state: FocusShieldUiState): KavachStartBlock {
    if (!state.isEnabled) return KavachStartBlock.None
    val accessibilityRequired = FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()
    if (!state.hasUsageStats || (accessibilityRequired && !state.hasAccessibilityService)) {
        return KavachStartBlock.NeedsPermissions
    }
    if (state.blockedPackages.isEmpty()) return KavachStartBlock.NeedsApps
    return KavachStartBlock.None
}

/**
 * Inline KAVACH setup on the Ekagra timer tab — toggle, permissions, and app selection in one place.
 */
@Composable
fun EkagraKavachInlineCard(
    shieldState: FocusShieldUiState,
    accent: Color,
    isDarkTheme: Boolean,
    forceExpanded: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scheme = MaterialTheme.colorScheme
    val resolvedDark = isDarkTheme || scheme.background.luminance() < 0.5f
    val cardBg = if (resolvedDark) Color(0x99242624) else scheme.surfaceVariant.copy(alpha = 0.58f)
    val cardBorder = accent.copy(alpha = if (resolvedDark) 0.55f else 0.75f)
    val secondaryText = if (resolvedDark) Color(0xFFABABA8) else scheme.onSurfaceVariant

    var hasUsageStats by remember { mutableStateOf(shieldState.hasUsageStats) }
    var hasAccessibility by remember { mutableStateOf(shieldState.hasAccessibilityService) }
    var hasNotifications by remember { mutableStateOf(shieldState.hasNotifications) }
    var pendingEnableAfterUsage by remember { mutableStateOf(false) }
    var pendingEnableAfterAccessibility by remember { mutableStateOf(false) }
    var guideTarget by remember { mutableStateOf<PermissionTarget?>(null) }
    var showNotificationDisclosure by remember { mutableStateOf(false) }
    var grantedBannerText by remember { mutableStateOf<String?>(null) }

    val accessibilityRequired = FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()
    val requiredPermissionsGranted =
        hasUsageStats && (!accessibilityRequired || hasAccessibility)
    val startBlock = kavachStartBlock(
        shieldState.copy(
            hasUsageStats = hasUsageStats,
            hasAccessibilityService = hasAccessibility,
        ),
    )
    val showDetails = shieldState.isEnabled && (forceExpanded || startBlock != KavachStartBlock.None)

    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    LaunchedEffect(grantedBannerText) {
        if (grantedBannerText != null) {
            delay(2_400)
            grantedBannerText = null
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newUsage = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                val newA11y = FocusShieldPermissionHelper.hasAccessibilityService(context)
                val newNotif = FocusShieldPermissionHelper.hasNotificationPermission(context)
                if (newUsage && !hasUsageStats) grantedBannerText = "App check is ready"
                if (newA11y && !hasAccessibility) grantedBannerText = "Block screen is ready"
                if (newNotif && !hasNotifications) grantedBannerText = "Notifications are on"
                hasUsageStats = newUsage
                hasAccessibility = newA11y
                hasNotifications = newNotif
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsageStats, hasAccessibility, pendingEnableAfterUsage, pendingEnableAfterAccessibility) {
        if (pendingEnableAfterUsage && hasUsageStats) {
            pendingEnableAfterUsage = false
            if (accessibilityRequired && !hasAccessibility) {
                guideTarget = PermissionTarget.ACCESSIBILITY
            } else {
                onToggleEnabled(true)
            }
        }
        if (accessibilityRequired && pendingEnableAfterAccessibility && hasAccessibility) {
            pendingEnableAfterAccessibility = false
            if (hasUsageStats) onToggleEnabled(true)
        }
    }

    fun onKavachToggle(enabled: Boolean) {
        if (enabled) {
            when {
                !hasUsageStats -> {
                    pendingEnableAfterUsage = true
                    guideTarget = PermissionTarget.USAGE_STATS
                }
                accessibilityRequired && !hasAccessibility -> {
                    pendingEnableAfterAccessibility = true
                    guideTarget = PermissionTarget.ACCESSIBILITY
                }
                else -> onToggleEnabled(true)
            }
        } else {
            onToggleEnabled(false)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.5.dp, cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.kavach_enable_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = when {
                            !shieldState.isEnabled -> stringResource(R.string.kavach_off_hint)
                            startBlock == KavachStartBlock.NeedsPermissions -> "Allow permissions below, then start focus"
                            startBlock == KavachStartBlock.NeedsApps -> "Choose apps to block during this session"
                            else -> stringResource(R.string.kavach_enabled_ekagra_hint)
                        },
                        fontSize = 12.sp,
                        color = secondaryText,
                        lineHeight = 16.sp,
                    )
                }
                Switch(
                    checked = shieldState.isEnabled,
                    onCheckedChange = ::onKavachToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1),
                    ),
                )
            }

            grantedBannerText?.let { msg ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF22C55E).copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                    Text(msg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                }
            }

            AnimatedVisibility(
                visible = shieldState.isEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EkagraKavachAppsRow(
                        blockedCount = shieldState.blockedPackages.size,
                        accent = accent,
                        secondaryText = secondaryText,
                        needsApps = startBlock == KavachStartBlock.NeedsApps,
                        onOpenAppPicker = onOpenAppPicker,
                    )

                    AnimatedVisibility(
                        visible = showDetails && !requiredPermissionsGranted,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.kavach_setup_heading),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = secondaryText,
                            )
                            EkagraKavachPermissionChip(
                                label = "App check",
                                granted = hasUsageStats,
                                accent = accent,
                                onClick = { guideTarget = PermissionTarget.USAGE_STATS },
                            )
                            if (accessibilityRequired) {
                                EkagraKavachPermissionChip(
                                    label = "Block screen",
                                    granted = hasAccessibility,
                                    accent = accent,
                                    onClick = { guideTarget = PermissionTarget.ACCESSIBILITY },
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = shieldState.isEnabled && requiredPermissionsGranted && shieldState.blockedPackages.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                            Text(
                                text = "KAVACH will run when you start the timer",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accent,
                            )
                        }
                    }
                }
            }
        }
    }

    guideTarget?.let { target ->
        PermissionGuideSheet(
            permission = target,
            onDismiss = {
                guideTarget = null
                when (target) {
                    PermissionTarget.USAGE_STATS -> pendingEnableAfterUsage = false
                    PermissionTarget.ACCESSIBILITY -> pendingEnableAfterAccessibility = false
                    PermissionTarget.NOTIFICATIONS -> Unit
                }
            },
            onOpenSettings = {
                guideTarget = null
                when (target) {
                    PermissionTarget.USAGE_STATS ->
                        FocusShieldPermissionHelper.openUsageAccessSettings(context)
                    PermissionTarget.ACCESSIBILITY ->
                        onOpenAccessibilitySettings()
                    PermissionTarget.NOTIFICATIONS ->
                        requestNotificationPermission()
                }
            },
        )
    }

    if (showNotificationDisclosure) {
        NotificationConsentDialog(
            onDismiss = { showNotificationDisclosure = false },
            onConfirm = {
                showNotificationDisclosure = false
                requestNotificationPermission()
            },
        )
    }
}

@Composable
private fun EkagraKavachAppsRow(
    blockedCount: Int,
    accent: Color,
    secondaryText: Color,
    needsApps: Boolean,
    onOpenAppPicker: () -> Unit,
) {
    val appsLabel = if (blockedCount == 0) {
        stringResource(R.string.kavach_blocked_apps_none)
    } else {
        stringResource(R.string.kavach_blocked_apps_count, blockedCount)
    }
    val rowBg = if (needsApps) accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.08f)
    val rowBorder = if (needsApps) accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenAppPicker)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (needsApps) Icons.Default.Warning else Icons.Default.Apps,
            contentDescription = null,
            tint = if (needsApps) accent else secondaryText,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.kavach_blocked_apps_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f),
            )
            Text(
                text = appsLabel,
                fontSize = 12.sp,
                color = secondaryText,
            )
        }
        Text(
            text = if (needsApps) "Choose" else "Edit",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
    }
}

@Composable
private fun EkagraKavachPermissionChip(
    label: String,
    granted: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (granted) Color(0xFF22C55E).copy(alpha = 0.12f) else accent.copy(alpha = 0.12f))
            .clickable(enabled = !granted, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.92f),
        )
        Text(
            text = if (granted) "Ready" else "Allow",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) Color(0xFF22C55E) else accent,
        )
    }
}
