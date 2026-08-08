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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.ekagra.EkagraDisplayTitle
import com.safarparmar.app.ui.ekagra.EkagraEyebrow
import com.safarparmar.app.ui.ekagra.EkagraHairline
import com.safarparmar.app.ui.ekagra.EkagraInk
import com.safarparmar.app.ui.ekagra.rememberEkagraInk
import com.safarparmar.app.ui.launch.AppUsageMode
import kotlinx.coroutines.delay

private enum class KavachSettingsTab(val label: String) {
    APP_SHIELD("App Shield"),
}

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
    onOpenAppPicker: () -> Unit,
    onGoToEkagra: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    /** Opens the app-category editor that feeds Kavach analytics. */
    onOpenAppCategories: (() -> Unit)? = null,
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
    var awaitingPermission by remember { mutableStateOf<PermissionTarget?>(null) }
    var grantedBannerText by remember { mutableStateOf<String?>(null) }
    val activeTab = KavachSettingsTab.APP_SHIELD
    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    AwaitPermissionThenReturnToApp(
        awaiting = awaitingPermission,
        onReturned = { awaitingPermission = null },
    )

    val requiredPermissionsGranted = hasUsageStats && hasOverlay
    val primaryCtaLabel = when {
        !state.isEnabled -> "Turn on Kavach"
        !hasUsageStats -> "Allow app check"
        !hasOverlay -> "Allow show on top"
        state.blockedPackages.isEmpty() -> "Choose apps"
        else -> "Change apps"
    }

    // Identify active mode.
    // Always On is checked first: it is its own mode rather than a variant of the
    // timer-bound ones, and leaving it out here is what made its row impossible to
    // select — the tap saved correctly, then this recomputed to Normal and the
    // radio snapped straight back.
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
                if (newOverlay && !hasOverlay) grantedBannerText = "Show on top is ready"
                if (newNotif && !hasNotifications) grantedBannerText = "Notifications are on"
                if (newNotificationAccess && !hasNotificationSuppressionAccess) {
                    grantedBannerText = "Notification shield is ready"
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
                .padding(
                    top = 16.dp,
                    bottom = if (activeTab == KavachSettingsTab.APP_SHIELD) 140.dp else 32.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            EkagraEyebrow("KAVACH", ink.secondaryText)
            Spacer(Modifier.height(4.dp))
            EkagraDisplayTitle(
                text = "Block apps while you study",
                color = ink.primaryText,
            )
            Spacer(Modifier.height(16.dp))

            when (activeTab) {
                KavachSettingsTab.APP_SHIELD -> {
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
                                text = "Turn on Kavach",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = ink.primaryText,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (state.isEnabled) "Kavach is on" else "Kavach is off",
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
                                checkedTrackColor = KavachDesign.Primary,
                            ),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    EkagraHairline(ink.hairline)
                    Spacer(Modifier.height(24.dp))

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

                            EkagraEyebrow("YOUR APPS", ink.secondaryText)
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
                                            .background(KavachDesign.Primary.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Default.Apps,
                                            contentDescription = null,
                                            tint = KavachDesign.Primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Apps to block",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ink.primaryText,
                                        )
                                        Text(
                                            text = if (state.blockedPackages.isEmpty()) {
                                                "No apps chosen yet"
                                            } else {
                                                "${state.blockedPackages.size} apps chosen"
                                            },
                                            fontSize = 13.sp,
                                            color = ink.secondaryText,
                                        )
                                    }
                                }
                                Text(
                                    text = "Choose >",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KavachDesign.Primary,
                                )
                            }

                            if (onOpenAppCategories != null) {
                                EkagraHairline(ink.hairline)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onOpenAppCategories)
                                        .padding(vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "App categories",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ink.primaryText,
                                        )
                                        Text(
                                            text = "Set what counts as productive or distracting in your analytics",
                                            fontSize = 13.sp,
                                            color = ink.secondaryText,
                                        )
                                    }
                                    Text(
                                        text = "Edit >",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KavachDesign.Primary,
                                    )
                                }
                            }

                            EkagraHairline(ink.hairline)
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    AnimatedVisibility(
                        visible = !requiredPermissionsGranted,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column {
                            EkagraEyebrow("PERMISSIONS NEEDED", ink.secondaryText)
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

            }
        }

        if (activeTab == KavachSettingsTab.APP_SHIELD) {
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
                secondaryLabel = "Not now",
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
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
                if (target != PermissionTarget.NOTIFICATIONS) {
                    awaitingPermission = target
                }
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

@Composable
private fun KavachSettingsTabSwitch(
    selected: KavachSettingsTab,
    ink: EkagraInk,
    onSelect: (KavachSettingsTab) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Choose what to set up",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink.secondaryText,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ink.secondaryText.copy(alpha = 0.10f))
                .padding(3.dp),
        ) {
            KavachSettingsTab.entries.forEach { tab ->
                val active = tab == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .then(
                            if (active) {
                                Modifier.background(MaterialTheme.colorScheme.surface)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) ink.primaryText else ink.secondaryText,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubeScopeRow(label: String, scope: String, ink: EkagraInk, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = ink.primaryText)
        Text(
            when (scope) { "protected" -> "Protected time"; "always" -> "Always"; else -> "Off" },
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KavachDesign.Primary,
        )
    }
}

private fun nextYoutubeScope(current: String): String = when (current) {
    "off" -> "protected"
    "protected" -> "always"
    else -> "off"
}

/**
 * Mutually exclusive profile selector (Normal / Beast Mode) presented with Flat
 * Hairline rules. A third "Always On" option is hidden for this release.
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
        EkagraEyebrow("CHOOSE HOW IT WORKS", ink.secondaryText)
        Spacer(Modifier.height(12.dp))

        // 1. Kavach Normal
        val isNormalSelected = isEnabled && (activeMode == AppUsageMode.FOCUSED || activeMode == AppUsageMode.STANDARD)
        ProfileOptionRow(
            title = "Normal",
            subtitle = "Blocks apps when your study timer is on. You can open a blocked app for 5 minutes if you need to.",
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
            subtitle = "Blocks apps when your study timer is on. You cannot open blocked apps until the timer ends.",
            icon = Icons.Default.FlashOn,
            isSelected = isBeastSelected,
            ink = ink,
            onClick = { onSelectProfile(AppUsageMode.BEAST) },
        )

        EkagraHairline(ink.hairline)

        // 3. Always On — the only profile that keeps blocking with no timer running.
        // Its subtitle names the ongoing notification explicitly: a mode that blocks
        // apps all day has to be obvious that it is running, and obvious how to stop.
        val isAlwaysOnSelected = isEnabled && activeMode == AppUsageMode.ALWAYS_ON
        ProfileOptionRow(
            title = "Always On",
            subtitle = "Blocks your chosen apps all day, even with no timer running. " +
                "A notification stays up while it's on — tap it any time to turn it off.",
            icon = Icons.Default.Shield,
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
    val accent = KavachDesign.Primary

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
