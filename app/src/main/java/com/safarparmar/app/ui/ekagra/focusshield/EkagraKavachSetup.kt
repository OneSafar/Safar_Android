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
import androidx.compose.ui.draw.drawBehind

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
    isSessionRunning: Boolean = false,
    onToggleEnabled: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onSetupPermissions: () -> Unit,
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasAccessibility = FocusShieldPermissionHelper.hasAccessibilityService(context)
                hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun onKavachToggle(enabled: Boolean) {
        if (enabled) {
            if (!hasUsageStats || (accessibilityRequired && !hasAccessibility)) {
                onSetupPermissions()
            } else {
                onToggleEnabled(true)
            }
        } else {
            onToggleEnabled(false)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        shadowElevation = 4.dp,
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.1f))
                        .border(1.dp, accent.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
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
                            !shieldState.isEnabled -> {
                                if (isSessionRunning) "Shield is off. Distracting apps won't be blocked."
                                else stringResource(R.string.kavach_off_hint)
                            }
                            startBlock == KavachStartBlock.NeedsPermissions -> {
                                if (isSessionRunning) "Allow permissions below to activate blocking immediately"
                                else "Allow permissions below, then start focus"
                            }
                            startBlock == KavachStartBlock.NeedsApps -> {
                                if (isSessionRunning) "Choose apps to block during this active session"
                                else "Choose apps to block during this session"
                            }
                            else -> {
                                if (isSessionRunning) "Focus Shield is active and blocking distracting apps."
                                else stringResource(R.string.kavach_enabled_ekagra_hint)
                            }
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
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Permission-granted banners removed — KAVACH on indicator is sufficient

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
                                text = if (isSessionRunning) "KAVACH is active and blocking apps" else "KAVACH will run when you start the timer",
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
}

@Composable
private fun EkagraKavachAppsRow(
    blockedCount: Int,
    accent: Color,
    secondaryText: Color,
    needsApps: Boolean,
    onOpenAppPicker: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val appsLabel = if (blockedCount == 0) {
        stringResource(R.string.kavach_blocked_apps_none)
    } else {
        stringResource(R.string.kavach_blocked_apps_count, blockedCount)
    }
    val rowBg = if (needsApps) scheme.primaryContainer.copy(alpha = 0.4f) else scheme.surfaceVariant.copy(alpha = 0.5f)
    val rowBorder = if (needsApps) scheme.primary.copy(alpha = 0.5f) else scheme.outlineVariant

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
            tint = if (needsApps) scheme.primary else secondaryText,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.kavach_blocked_apps_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
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
            color = scheme.primary,
        )
    }
}
