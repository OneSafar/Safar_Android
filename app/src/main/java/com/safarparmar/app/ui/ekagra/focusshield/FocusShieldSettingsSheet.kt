package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.safarparmar.app.ui.ekagra.EkagraDisplayTitle
import com.safarparmar.app.ui.ekagra.EkagraEyebrow
import com.safarparmar.app.ui.ekagra.EkagraHairline
import com.safarparmar.app.ui.ekagra.EkagraInk
import com.safarparmar.app.ui.ekagra.rememberEkagraInk
import com.safarparmar.app.ui.launch.AppUsageMode
import kotlinx.coroutines.delay

/**
 * Focus Shield (Kavach) settings — Flat Hairline Design System:
 * - Clean Lora typography & uppercase eyebrows
 * - 1px hairline dividers (EkagraHairline)
 * - Mutually exclusive 3-profile selector (Kavach Normal, Beast Mode, Always On)
 * - Automatic light & dark mode contrast handling
 */
@Composable
fun FocusShieldSettingsContent(
    state: FocusShieldUiState,
    accent: Color,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleAlwaysOn: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onGoToEkagra: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onToggleProfile: (String) -> Unit = {},
    onRefreshPermissions: () -> Unit = {},
    onMaybeLater: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasOverlay by remember { mutableStateOf(state.hasOverlayPermission) }
    var hasNotifications by remember { mutableStateOf(state.hasNotifications) }
    var hasNotificationSuppressionAccess by remember { mutableStateOf(state.hasNotificationSuppressionAccess) }
    var hasUsageStats by remember { mutableStateOf(state.hasUsageStats) }
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)

    var pendingEnableAfterUsage by remember { mutableStateOf(false) }
    var pendingEnableAfterOverlay by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }
    var guideTarget by remember { mutableStateOf<PermissionTarget?>(null) }
    var grantedBannerText by remember { mutableStateOf<String?>(null) }
    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    val requiredPermissionsGranted = hasUsageStats && hasOverlay
    val primaryCtaLabel = when {
        !state.isEnabled -> "Turn On KAVACH"
        !hasUsageStats -> "Allow App Check"
        !hasOverlay -> "Allow Display Over Apps"
        state.blockedPackages.isEmpty() -> "Choose Apps"
        else -> "Edit App List"
    }

    // Identify active mode
    val activeMode = when {
        !state.isEnabled -> null
        state.isAlwaysOnMode -> AppUsageMode.ALWAYS_ON
        state.isStrictMode -> AppUsageMode.BEAST
        else -> AppUsageMode.FOCUSED
    }

    LaunchedEffect(grantedBannerText) {
        if (grantedBannerText != null) {
            delay(2_400)
            grantedBannerText = null
        }
    }

    DisposableEffect(
        lifecycleOwner,
        state.hasOverlayPermission,
        state.hasNotifications,
        state.hasNotificationSuppressionAccess,
        state.hasUsageStats,
    ) {
        hasOverlay = state.hasOverlayPermission
        hasNotifications = state.hasNotifications
        hasNotificationSuppressionAccess = state.hasNotificationSuppressionAccess
        hasUsageStats = state.hasUsageStats
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newUsage = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                val newOverlay = FocusShieldPermissionHelper.hasOverlayPermission(context)
                val newNotif = FocusShieldPermissionHelper.hasNotificationPermission(context)
                val newNotificationAccess = FocusShieldPermissionHelper.hasNotificationListenerAccess(context)
                if (newUsage && !hasUsageStats) grantedBannerText = "App check is ready"
                if (newOverlay && !hasOverlay) grantedBannerText = "Block screen is ready"
                if (newNotif && !hasNotifications) grantedBannerText = "Notifications are on"
                if (newNotificationAccess && !hasNotificationSuppressionAccess) {
                    grantedBannerText = "Notification Shield is ready"
                }
                hasUsageStats = newUsage
                hasOverlay = newOverlay
                hasNotifications = newNotif
                hasNotificationSuppressionAccess = newNotificationAccess
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun handleProfileSelect(mode: String) {
        if (!requiredPermissionsGranted) {
            if (!hasUsageStats) {
                pendingEnableAfterUsage = true
                guideTarget = PermissionTarget.USAGE_STATS
                return
            }
            if (!hasOverlay) {
                pendingEnableAfterOverlay = true
                guideTarget = PermissionTarget.OVERLAY
                return
            }
        }
        onToggleProfile(mode)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Flat Hairline Header
            EkagraEyebrow("KAVACH SHIELD", ink.secondaryText)
            Spacer(Modifier.height(4.dp))
            EkagraDisplayTitle("Focus Protection", ink.primaryText)
            Spacer(Modifier.height(16.dp))
            EkagraHairline(ink.hairline)

            Spacer(Modifier.height(20.dp))

            // Master Enable Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Enable Protection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink.primaryText,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (state.isEnabled) "Kavach is active" else "Kavach is currently off",
                        fontSize = 13.sp,
                        color = ink.secondaryText,
                    )
                }
                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            handleProfileSelect(AppUsageMode.FOCUSED)
                        } else {
                            onToggleEnabled(false)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = scheme.primary,
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))
            EkagraHairline(ink.hairline)
            Spacer(Modifier.height(24.dp))

            // 3-Way Mutually Exclusive Profiles Selector
            AnimatedVisibility(
                visible = state.isEnabled,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    KavachProfileSelector(
                        activeMode = activeMode,
                        isEnabled = state.isEnabled,
                        ink = ink,
                        onSelectProfile = ::handleProfileSelect,
                    )

                    Spacer(Modifier.height(28.dp))

                    // Blocked Apps Row
                    EkagraEyebrow("APP SELECTION", ink.secondaryText)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAppPicker)
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = "Blocked Apps",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ink.primaryText,
                                )
                                Text(
                                    text = if (state.blockedPackages.isEmpty()) "No apps selected" else "${state.blockedPackages.size} apps selected for blocking",
                                    fontSize = 13.sp,
                                    color = ink.secondaryText,
                                )
                            }
                        }
                        Text(
                            text = "Choose >",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                        )
                    }

                    EkagraHairline(ink.hairline)
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Permission Disclosure checklist
            AnimatedVisibility(
                visible = !requiredPermissionsGranted,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    EkagraEyebrow("PERMISSIONS REQUIRED", ink.secondaryText)
                    Spacer(Modifier.height(12.dp))

                    KavachPermissionDisclosureCard(
                        hasUsageStats = hasUsageStats,
                        hasOverlay = hasOverlay,
                        hasNotifications = hasNotifications,
                        hasNotificationSuppressionAccess = hasNotificationSuppressionAccess,
                        onOpenUsageAccess = { guideTarget = PermissionTarget.USAGE_STATS },
                        onOpenOverlay = { guideTarget = PermissionTarget.OVERLAY },
                        onOpenNotifications = { guideTarget = PermissionTarget.NOTIFICATIONS },
                        onOpenNotificationAccess = { guideTarget = PermissionTarget.NOTIFICATION_ACCESS },
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }

        KavachBottomActions(
            primaryLabel = primaryCtaLabel,
            onPrimaryClick = {
                when {
                    !state.isEnabled -> handleProfileSelect(AppUsageMode.FOCUSED)
                    !hasUsageStats -> guideTarget = PermissionTarget.USAGE_STATS
                    !hasOverlay -> guideTarget = PermissionTarget.OVERLAY
                    else -> onOpenAppPicker()
                }
            },
            onSecondaryClick = onMaybeLater,
            secondaryLabel = "Maybe Later",
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showLearnMore) {
        KavachLearnMoreSheet(
            hasUsageStats = hasUsageStats,
            hasOverlay = hasOverlay,
            hasNotifications = hasNotifications,
            hasNotificationSuppressionAccess = hasNotificationSuppressionAccess,
            onOpenUsageAccess = { guideTarget = PermissionTarget.USAGE_STATS },
            onOpenOverlay = { guideTarget = PermissionTarget.OVERLAY },
            onOpenNotifications = { guideTarget = PermissionTarget.NOTIFICATIONS },
            onOpenNotificationAccess = { guideTarget = PermissionTarget.NOTIFICATION_ACCESS },
            onDismiss = { showLearnMore = false },
        )
    }

    guideTarget?.let { target ->
        PermissionGuideSheet(
            permission = target,
            onDismiss = {
                guideTarget = null
                when (target) {
                    PermissionTarget.USAGE_STATS -> pendingEnableAfterUsage = false
                    PermissionTarget.OVERLAY -> pendingEnableAfterOverlay = false
                    PermissionTarget.NOTIFICATIONS -> Unit
                    PermissionTarget.NOTIFICATION_ACCESS -> Unit
                }
            },
            onOpenSettings = {
                guideTarget = null
                when (target) {
                    PermissionTarget.USAGE_STATS ->
                        FocusShieldPermissionHelper.openUsageAccessSettings(context)
                    PermissionTarget.OVERLAY ->
                        onOpenOverlaySettings()
                    PermissionTarget.NOTIFICATIONS ->
                        requestNotificationPermission()
                    PermissionTarget.NOTIFICATION_ACCESS ->
                        FocusShieldPermissionHelper.openNotificationListenerSettings(context)
                }
            },
        )
    }
}

/**
 * 3-Way Mutually Exclusive Profile Selector cleanly presented with Flat Hairline rules.
 */
@Composable
private fun KavachProfileSelector(
    activeMode: String?,
    isEnabled: Boolean,
    ink: EkagraInk,
    onSelectProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        EkagraEyebrow("PROTECTION PROFILE", ink.secondaryText)
        Spacer(Modifier.height(12.dp))

        // 1. Kavach Normal
        val isNormalSelected = isEnabled && (activeMode == AppUsageMode.FOCUSED || activeMode == AppUsageMode.STANDARD)
        ProfileOptionRow(
            title = "Kavach Normal",
            subtitle = "Timer-bound protection. Allows 5-minute Quick Unlocks for short intentional access during study sessions.",
            icon = Icons.Default.Shield,
            isSelected = isNormalSelected,
            ink = ink,
            onClick = { onSelectProfile(AppUsageMode.FOCUSED) },
        )

        EkagraHairline(ink.hairline)

        // 2. Beast Mode
        val isBeastSelected = isEnabled && activeMode == AppUsageMode.BEAST
        ProfileOptionRow(
            title = "Beast Mode",
            subtitle = "Timer-bound strict lockout. Quick Unlocks are completely disabled until your Ekagra timer finishes.",
            icon = Icons.Default.FlashOn,
            isSelected = isBeastSelected,
            ink = ink,
            onClick = { onSelectProfile(AppUsageMode.BEAST) },
        )

        EkagraHairline(ink.hairline)

        // 3. Always On
        val isAlwaysOnSelected = isEnabled && activeMode == AppUsageMode.ALWAYS_ON
        ProfileOptionRow(
            title = "Always On",
            subtitle = "24/7 perpetual shield across your entire phone, inside and outside Ekagra timer sessions.",
            icon = Icons.Default.Lock,
            isSelected = isAlwaysOnSelected,
            ink = ink,
            onClick = { onSelectProfile(AppUsageMode.ALWAYS_ON) },
        )

        EkagraHairline(ink.hairline)
    }
}

@Composable
private fun ProfileOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    ink: EkagraInk,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSelected) accent.copy(alpha = 0.16f) else ink.trackFaint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accent else ink.secondaryText,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accent else ink.primaryText,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = ink.secondaryText,
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (isSelected) accent else ink.hairline, CircleShape)
                .background(if (isSelected) accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

