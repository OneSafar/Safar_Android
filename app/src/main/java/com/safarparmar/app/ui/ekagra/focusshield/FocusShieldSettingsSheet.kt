package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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

/**
 * Focus Shield (Kavach) settings — state-driven layout:
 * - Setup Needed: permission checklist until all three are granted
 * - Configured: status pill, master toggle, unified control center
 */
@Composable
fun FocusShieldSettingsContent(
    state: FocusShieldUiState,
    accent: Color,
    onToggleEnabled: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onGoToEkagra: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
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
    val screenBackground = scheme.background
    val card = scheme.surfaceContainerHigh
    val border = scheme.outlineVariant
    val textPrimary = scheme.onBackground
    val textSecondary = scheme.onSurfaceVariant
    var pendingEnableAfterUsage by remember { mutableStateOf(false) }
    var pendingEnableAfterOverlay by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }
    var guideTarget by remember { mutableStateOf<PermissionTarget?>(null) }
    var grantedBannerText by remember { mutableStateOf<String?>(null) }
    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    // Required: Usage access + Display-over-other-apps. Notification Shield is optional.
    val requiredPermissionsGranted = hasUsageStats && hasOverlay
    val primaryCtaLabel = when {
        !state.isEnabled -> "Turn On KAVACH"
        !hasUsageStats -> "Allow App Check"
        !hasOverlay -> "Allow Display Over Apps"
        state.blockedPackages.isEmpty() -> "Choose Apps"
        else -> "Edit App List"
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

    LaunchedEffect(
        hasUsageStats,
        hasOverlay,
        pendingEnableAfterUsage,
        pendingEnableAfterOverlay,
    ) {
        if (pendingEnableAfterUsage && hasUsageStats) {
            pendingEnableAfterUsage = false
            if (!hasOverlay) {
                guideTarget = PermissionTarget.OVERLAY
            } else {
                onToggleEnabled(true)
            }
        }
        if (pendingEnableAfterOverlay && hasOverlay) {
            pendingEnableAfterOverlay = false
            if (hasUsageStats) {
                onToggleEnabled(true)
            }
        }
    }

    fun onKavachToggle(enabled: Boolean) {
        if (enabled) {
            when {
                !hasUsageStats -> {
                    pendingEnableAfterUsage = true
                    guideTarget = PermissionTarget.USAGE_STATS
                }
                !hasOverlay -> {
                    pendingEnableAfterOverlay = true
                    guideTarget = PermissionTarget.OVERLAY
                }
                else -> onToggleEnabled(true)
            }
        } else {
            onToggleEnabled(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(screenBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            KavachCompactIntro(onLearnMore = { showLearnMore = true })

            Spacer(Modifier.height(24.dp))

            grantedBannerText?.let { msg ->
                PermissionGrantedBanner(text = msg)
                Spacer(Modifier.height(16.dp))
            }

            AnimatedVisibility(
                visible = state.isEnabled,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    KavachEnabledSummaryCard(
                        enabled = state.isEnabled,
                        blockedAppCount = state.blockedPackages.size,
                        onEnabledChange = ::onKavachToggle,
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            AnimatedVisibility(
                visible = state.isEnabled && requiredPermissionsGranted,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    KavachStatusPill()
                    Spacer(Modifier.height(32.dp))
                    KavachControlCenterContainer(
                        blockedAppCount = state.blockedPackages.size,
                        accent = accent,
                        onOpenAppPicker = onOpenAppPicker,
                        onGoToEkagra = onGoToEkagra,
                        hasNotificationShield = hasNotificationSuppressionAccess,
                        onEnableNotificationShield = { guideTarget = PermissionTarget.NOTIFICATION_ACCESS },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AnimatedVisibility(
                visible = !requiredPermissionsGranted,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
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
            }


            Spacer(Modifier.height(20.dp))
        }

        KavachBottomActions(
            primaryLabel = primaryCtaLabel,
            onPrimaryClick = {
                when {
                    !state.isEnabled -> onKavachToggle(true)
                    !hasUsageStats -> guideTarget = PermissionTarget.USAGE_STATS
                    !hasOverlay -> guideTarget = PermissionTarget.OVERLAY
                    else -> onOpenAppPicker()
                }
            },
            onSecondaryClick = onMaybeLater,
            secondaryLabel = if (state.isEnabled) "Maybe Later" else "Maybe Later",
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
