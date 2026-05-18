package com.safar.app.ui.ekagra.focusshield

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
import com.safar.app.R
import com.safar.app.notifications.rememberNotificationPermissionRequester
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
    onToggleStrictMode: (Boolean) -> Unit,
    onToggleEmergencyUnlock: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onRefreshPermissions: () -> Unit = {},
    onMaybeLater: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAccessibilityService by remember { mutableStateOf(state.hasAccessibilityService) }
    var hasNotifications by remember { mutableStateOf(state.hasNotifications) }
    var hasUsageStats by remember { mutableStateOf(state.hasUsageStats) }
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val screenBackground = if (isDark) Color(0xFF0F172A) else KavachDesign.Background
    val card = if (isDark) Color(0xFF1E293B) else KavachDesign.Surface
    val border = if (isDark) Color(0xFF334155) else KavachDesign.Border
    val textPrimary = scheme.onBackground
    val textSecondary = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
    var pendingEnableAfterUsage by remember { mutableStateOf(false) }
    var pendingEnableAfterAccessibility by remember { mutableStateOf(false) }
    var showNotificationDisclosure by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }
    var guideTarget by remember { mutableStateOf<PermissionTarget?>(null) }
    var grantedBannerText by remember { mutableStateOf<String?>(null) }
    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    val requiredPermissionsGranted = hasUsageStats && hasAccessibilityService
    val primaryCtaLabel = when {
        !state.isEnabled -> "Turn On Kavach"
        !hasUsageStats -> "Allow App Check"
        !hasAccessibilityService -> "Allow Block Screen"
        state.blockedPackages.isEmpty() -> "Choose Apps"
        else -> "Edit App List"
    }

    LaunchedEffect(grantedBannerText) {
        if (grantedBannerText != null) {
            delay(2_400)
            grantedBannerText = null
        }
    }

    DisposableEffect(lifecycleOwner, state.hasAccessibilityService, state.hasNotifications, state.hasUsageStats) {
        hasAccessibilityService = state.hasAccessibilityService
        hasNotifications = state.hasNotifications
        hasUsageStats = state.hasUsageStats
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newUsage = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                val newA11y = FocusShieldPermissionHelper.hasAccessibilityService(context)
                val newNotif = FocusShieldPermissionHelper.hasNotificationPermission(context)
                if (newUsage && !hasUsageStats) grantedBannerText = "App check is ready"
                if (newA11y && !hasAccessibilityService) grantedBannerText = "Block screen is ready"
                if (newNotif && !hasNotifications) grantedBannerText = "Notifications are on"
                hasUsageStats = newUsage
                hasAccessibilityService = newA11y
                hasNotifications = newNotif
                onRefreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsageStats, hasAccessibilityService, pendingEnableAfterUsage, pendingEnableAfterAccessibility) {
        if (pendingEnableAfterUsage && hasUsageStats) {
            pendingEnableAfterUsage = false
            if (!hasAccessibilityService) {
                guideTarget = PermissionTarget.ACCESSIBILITY
            } else {
                onToggleEnabled(true)
            }
        }
        if (pendingEnableAfterAccessibility && hasAccessibilityService) {
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
                !hasAccessibilityService -> {
                    pendingEnableAfterAccessibility = true
                    guideTarget = PermissionTarget.ACCESSIBILITY
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
                        beastModeEnabled = state.isStrictMode,
                        emergencyUnlockEnabled = state.allowEmergencyUnlock,
                        accent = accent,
                        onOpenAppPicker = onOpenAppPicker,
                        onBeastModeChange = onToggleStrictMode,
                        onEmergencyUnlockChange = onToggleEmergencyUnlock,
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
                    hasAccessibilityService = hasAccessibilityService,
                    hasNotifications = hasNotifications,
                    onOpenUsageAccess = { guideTarget = PermissionTarget.USAGE_STATS },
                    onOpenAccessibility = { guideTarget = PermissionTarget.ACCESSIBILITY },
                    onOpenNotifications = { showNotificationDisclosure = true },
                )
            }

            AnimatedVisibility(
                visible = state.isEnabled && !requiredPermissionsGranted,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.kavach_setup_heading),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.kavach_setup_intro),
                        fontSize = 13.sp,
                        color = textSecondary,
                        lineHeight = 18.sp,
                    )

                    KavachStepRow(
                        stepNumber = 1,
                        title = "App Check",
                        subtitle = "Helps Kavach know when a blocked app opens",
                        rationale = "SAFAR only checks the app name, so Kavach can stop apps you picked. It does not read anything inside your apps.",
                        optional = false,
                        granted = hasUsageStats,
                        accent = accent,
                        cardColor = card,
                        borderColor = border,
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onAllow = { guideTarget = PermissionTarget.USAGE_STATS },
                    )

                    KavachStepRow(
                        stepNumber = 2,
                        title = "Block Screen",
                        subtitle = "Shows the Kavach screen when a blocked app opens",
                        rationale = "This is only used for Kavach during focus time. SAFAR does not read chats, passwords, typing, photos, or your screen.",
                        optional = false,
                        granted = hasAccessibilityService,
                        accent = accent,
                        cardColor = card,
                        borderColor = border,
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onAllow = { guideTarget = PermissionTarget.ACCESSIBILITY },
                    )

                    KavachStepRow(
                        stepNumber = 3,
                        title = "Notifications",
                        subtitle = "Shows focus timer updates",
                        rationale = "This is optional. Kavach still works without notifications.",
                        optional = true,
                        granted = hasNotifications,
                        accent = accent,
                        cardColor = card,
                        borderColor = border,
                        titleColor = textPrimary,
                        subtitleColor = textSecondary,
                        onAllow = { showNotificationDisclosure = true },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        KavachBottomActions(
            primaryLabel = primaryCtaLabel,
            onPrimaryClick = {
                when {
                    !state.isEnabled -> onKavachToggle(true)
                    !hasUsageStats -> guideTarget = PermissionTarget.USAGE_STATS
                    !hasAccessibilityService -> guideTarget = PermissionTarget.ACCESSIBILITY
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
            hasAccessibilityService = hasAccessibilityService,
            hasNotifications = hasNotifications,
            onOpenUsageAccess = { guideTarget = PermissionTarget.USAGE_STATS },
            onOpenAccessibility = { guideTarget = PermissionTarget.ACCESSIBILITY },
            onOpenNotifications = { showNotificationDisclosure = true },
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
