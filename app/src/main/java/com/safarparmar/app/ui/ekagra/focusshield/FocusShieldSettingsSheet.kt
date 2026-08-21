package com.safarparmar.app.ui.ekagra.focusshield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.safarparmar.app.R
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.ekagra.EkagraDisplayTitle
import com.safarparmar.app.ui.ekagra.EkagraHairline
import com.safarparmar.app.ui.ekagra.EkagraInk
import com.safarparmar.app.ui.ekagra.rememberEkagraInk
import com.safarparmar.app.ui.launch.AppUsageMode
import kotlinx.coroutines.delay

/**
 * Focus Shield (Kavach) Settings Screen — Modern Flat Design:
 * - Clean spacious layout with refined, breathable whitespace
 * - Segmented controls for "WHEN IT WORKS" and "PROTECTION LEVEL" with contextual 11sp subtexts
 * - Rich informational cards for Apps to block, App categories, and Permissions & access
 * - Deep Royal Purple (#581C87) brand styling throughout
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    /** Opens Kavach Analytics screen. */
    onOpenAnalytics: (() -> Unit)? = null,
    onToggleProfile: (String) -> Unit = {},
    onToggleStrictMode: (Boolean) -> Unit = {},
    onToggleSchedule: (Boolean) -> Unit = {},
    onSetScheduleRange: (Int, Int) -> Unit = { _, _ -> },
    onRefreshPermissions: () -> Unit = {},
    onMaybeLater: () -> Unit = {},
    onSave: () -> Unit = onMaybeLater,
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
    var pendingEnableFlow by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }
    var showPermissionCardSheet by remember { mutableStateOf(false) }
    var guideTarget by remember { mutableStateOf<PermissionTarget?>(null) }
    var awaitingPermission by remember { mutableStateOf<PermissionTarget?>(null) }
    var grantedBannerText by remember { mutableStateOf<String?>(null) }

    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    AwaitPermissionThenReturnToApp(
        awaiting = awaitingPermission,
        onReturned = { awaitingPermission = null },
    )

    val requiredPermissionsGranted = hasUsageStats && hasOverlay
    val allPermissionsGranted = hasUsageStats && hasOverlay && hasNotifications && hasNotificationSuppressionAccess
    val readyCount = listOf(hasUsageStats, hasOverlay, hasNotifications, hasNotificationSuppressionAccess).count { it }

    val primaryCtaLabel = when {
        !requiredPermissionsGranted && !hasUsageStats -> "Allow App Check"
        !requiredPermissionsGranted && !hasOverlay -> "Allow Display Over Apps"
        state.isEnabled -> "Turn off Kavach"
        else -> "Turn on Kavach"
    }

    LaunchedEffect(grantedBannerText) {
        if (grantedBannerText != null) {
            delay(3_000)
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

                val usageJustGranted = newUsage && !hasUsageStats
                val overlayJustGranted = newOverlay && !hasOverlay

                hasUsageStats = newUsage
                hasOverlay = newOverlay
                hasNotifications = newNotif
                hasNotificationSuppressionAccess = newNotificationAccess
                onRefreshPermissions()

                if (newUsage && !newOverlay && (usageJustGranted || pendingEnableFlow)) {
                    guideTarget = PermissionTarget.OVERLAY
                } else if (newUsage && newOverlay && (overlayJustGranted || pendingEnableFlow || usageJustGranted)) {
                    if (!state.isEnabled) {
                        onToggleEnabled(true)
                    }
                    grantedBannerText = "KAVACH is active and ready to protect your focus!"
                    pendingEnableFlow = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun handleProfileSelect(mode: String) {
        onToggleProfile(mode)
        if (state.isEnabled && !requiredPermissionsGranted) {
            pendingEnableFlow = true
            if (!hasUsageStats) {
                guideTarget = PermissionTarget.USAGE_STATS
            } else if (!hasOverlay) {
                guideTarget = PermissionTarget.OVERLAY
            }
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentBottomPadding = 100.dp + navBarBottom

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
                    bottom = contentBottomPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Master Status Card (Hero)
            KavachMasterStatusCard(
                isEnabled = state.isEnabled,
                isAlwaysOn = state.isAlwaysOnMode,
                isStrict = state.isStrictMode,
                onToggle = { enabled ->
                    if (enabled) {
                        if (!requiredPermissionsGranted) {
                            pendingEnableFlow = true
                            if (!hasUsageStats) {
                                guideTarget = PermissionTarget.USAGE_STATS
                            } else if (!hasOverlay) {
                                guideTarget = PermissionTarget.OVERLAY
                            }
                        } else {
                            onToggleEnabled(true)
                        }
                    } else {
                        onToggleEnabled(false)
                    }
                },
            )

            Spacer(Modifier.height(4.dp))

            // WHEN IT WORKS Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "WHEN IT WORKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ink.secondaryText,
                )
                KavachSegmentedSelector(
                    options = listOf(
                        SegmentOption(
                            key = AppUsageMode.FOCUSED,
                            label = "With Ekagra",
                            icon = Icons.Default.Timer,
                        ),
                        SegmentOption(
                            key = AppUsageMode.ALWAYS_ON,
                            label = "Always On",
                            icon = Icons.Default.Timer,
                        ),
                    ),
                    selectedKey = if (state.isAlwaysOnMode) AppUsageMode.ALWAYS_ON else AppUsageMode.FOCUSED,
                    onSelect = { handleProfileSelect(it) },
                )
                Text(
                    text = if (state.isAlwaysOnMode) {
                        "Keeps blocking in the background 24/7 until you manually turn Kavach off."
                    } else {
                        "Turns on when an Ekagra session starts and off when that session ends."
                    },
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = ink.secondaryText,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }

            Spacer(Modifier.height(4.dp))

            // PROTECTION LEVEL Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PROTECTION LEVEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = ink.secondaryText,
                )
                KavachSegmentedSelector(
                    options = listOf(
                        SegmentOption(
                            key = "normal",
                            label = "Normal",
                            icon = Icons.Default.Shield,
                        ),
                        SegmentOption(
                            key = "beast",
                            label = "Beast",
                            icon = Icons.Default.Security,
                        ),
                    ),
                    selectedKey = if (state.isStrictMode) "beast" else "normal",
                    onSelect = { key ->
                        onToggleStrictMode(key == "beast")
                    },
                )
                Text(
                    text = if (state.isStrictMode) {
                        "Blocks selected apps without Quick Unlock until study timer ends."
                    } else {
                        "Blocks selected apps and keeps Quick Unlock available for needed breaks."
                    },
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = ink.secondaryText,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            EkagraHairline(ink.hairline)
            Spacer(Modifier.height(4.dp))

            // Apps to Block Row
            KavachActionRow(
                icon = Icons.Default.Apps,
                title = "Apps to block",
                subtitle = if (state.blockedPackages.isEmpty()) {
                    "No apps chosen yet"
                } else {
                    "${state.blockedPackages.size} apps selected"
                },
                onClick = onOpenAppPicker,
                trailingContent = {
                    if (state.blockedPackages.isNotEmpty()) {
                        AppIconsPreviewRow(
                            packages = state.blockedPackages,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                },
            )

            // App Categories Row
            if (onOpenAppCategories != null) {
                EkagraHairline(ink.hairline)
                KavachActionRow(
                    icon = Icons.Default.Category,
                    title = "App categories",
                    subtitle = "Set what counts as productive or distracting in your analytics",
                    onClick = onOpenAppCategories,
                )
            }

            // Permissions & Access Row
            EkagraHairline(ink.hairline)
            KavachActionRow(
                icon = Icons.Default.Lock,
                title = "Permissions & access",
                subtitle = "$readyCount of 4 ready",
                subtitleColor = if (readyCount == 4) Color(0xFF10B981) else Color(0xFFD97706),
                onClick = {
                    if (!allPermissionsGranted) {
                        if (!hasUsageStats) {
                            guideTarget = PermissionTarget.USAGE_STATS
                        } else if (!hasOverlay) {
                            guideTarget = PermissionTarget.OVERLAY
                        } else if (!hasNotifications) {
                            guideTarget = PermissionTarget.NOTIFICATIONS
                        } else if (!hasNotificationSuppressionAccess) {
                            guideTarget = PermissionTarget.NOTIFICATION_ACCESS
                        }
                    } else {
                        showPermissionCardSheet = true
                    }
                },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        val perms = listOf(hasUsageStats, hasOverlay, hasNotifications, hasNotificationSuppressionAccess)
                        perms.forEach { granted ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (granted) Color(0xFF10B981) else ink.hairline),
                            )
                        }
                    }
                },
            )

            // Kavach Analytics Row
            if (onOpenAnalytics != null) {
                EkagraHairline(ink.hairline)
                KavachActionRow(
                    icon = Icons.Default.Analytics,
                    title = "Kavach analytics",
                    subtitle = "View focus reports, screen time & blocked attempts",
                    onClick = onOpenAnalytics,
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Top notification banner for success/ready states
        AnimatedVisibility(
            visible = grantedBannerText != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp),
        ) {
            grantedBannerText?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF064E3B),
                    border = BorderStroke(1.dp, Color(0xFF059669)),
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = msg,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECFDF5),
                        )
                    }
                }
            }
        }

        // Sticky Bottom CTAs: "Not Now" and "Save"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(scheme.background)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onMaybeLater,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, KavachDesign.Primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KavachDesign.Primary,
                ),
            ) {
                Text(
                    text = "Not Now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KavachDesign.Primary,
                )
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KavachDesign.Primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Save",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }

    if (showPermissionCardSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPermissionCardSheet = false },
            containerColor = scheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KavachPermissionDisclosureCard(
                    hasUsageStats = hasUsageStats,
                    hasOverlay = hasOverlay,
                    hasNotifications = hasNotifications,
                    hasNotificationSuppressionAccess = hasNotificationSuppressionAccess,
                    onOpenUsageAccess = {
                        showPermissionCardSheet = false
                        guideTarget = PermissionTarget.USAGE_STATS
                    },
                    onOpenOverlay = {
                        showPermissionCardSheet = false
                        guideTarget = PermissionTarget.OVERLAY
                    },
                    onOpenNotifications = {
                        showPermissionCardSheet = false
                        guideTarget = PermissionTarget.NOTIFICATIONS
                    },
                    onOpenNotificationAccess = {
                        showPermissionCardSheet = false
                        guideTarget = PermissionTarget.NOTIFICATION_ACCESS
                    },
                )
            }
        }
    }

    guideTarget?.let { target ->
        PermissionGuideSheet(
            permission = target,
            onDismiss = {
                guideTarget = null
                when (target) {
                    PermissionTarget.USAGE_STATS,
                    PermissionTarget.OVERLAY -> pendingEnableFlow = false
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

// ─────────────────────────────────────────────────────────────────────────────
// Components for Modern Flat Kavach Design
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KavachMasterStatusCard(
    isEnabled: Boolean,
    isAlwaysOn: Boolean,
    isStrict: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) Color(0xFF10B981) else scheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.Check else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isEnabled) Color.White else ink.secondaryText,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column {
                    Text(
                        text = if (isEnabled) "Kavach is on" else "Kavach is off",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink.primaryText,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${if (isAlwaysOn) "Always On" else "With Ekagra"} • ${if (isStrict) "Beast" else "Normal"}",
                        fontSize = 11.5.sp,
                        color = ink.secondaryText,
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = KavachDesign.Primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = scheme.surfaceVariant,
                ),
            )
        }
    }
}

private data class SegmentOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun KavachSegmentedSelector(
    options: List<SegmentOption>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { option ->
                val isSelected = option.key == selectedKey
                val containerColor = if (isSelected) KavachDesign.Primary else Color.Transparent
                val contentColor = if (isSelected) Color.White else ink.primaryText

                Surface(
                    onClick = { onSelect(option.key) },
                    shape = RoundedCornerShape(10.dp),
                    color = containerColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = option.label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = contentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KavachActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitleColor: Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = rememberEkagraInk(onCanvas = false)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KavachDesign.Primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = KavachDesign.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink.primaryText,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    color = subtitleColor ?: ink.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            trailingContent?.invoke()
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ink.secondaryText.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AppIconsPreviewRow(
    packages: Set<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val previewPackages = remember(packages) { packages.take(4).toList() }
    val remaining = packages.size - previewPackages.size

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        previewPackages.forEach { pkg ->
            val iconDrawable = remember(pkg) {
                runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
            }
            if (iconDrawable != null) {
                Image(
                    bitmap = iconDrawable.toBitmap(width = 48, height = 48).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(KavachDesign.Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        tint = KavachDesign.Primary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
        if (remaining > 0) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = "+$remaining",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}
