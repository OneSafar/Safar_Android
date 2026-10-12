package com.safarparmar.app.ui.ekagra.focusshield

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.studyplanner.components.GlassButton
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
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
    val purple = KavachDesign.Primary
    return KavachPermissionColors(
        screen = scheme.background,
        card = scheme.surfaceContainerHigh,
        primaryText = scheme.onBackground,
        secondaryText = scheme.onSurfaceVariant,
        divider = scheme.outlineVariant,
        progress = purple,
        progressTrack = scheme.surfaceContainerHighest,
        helpBg = purple.copy(alpha = 0.14f),
        helpText = purple,
        cta = purple,
        ctaText = Color.White,
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
    var hasOverlay by remember { mutableStateOf(shieldState.hasOverlayPermission) }
    var hasNotifications by remember { mutableStateOf(shieldState.hasNotifications) }
    var hasNotificationSuppressionAccess by remember { mutableStateOf(shieldState.hasNotificationSuppressionAccess) }
    var hasBatterySaver by remember {
        mutableStateOf(FocusShieldPermissionHelper.isIgnoringBatteryOptimizations(context))
    }
    var selectedPermission by remember { mutableStateOf<PermissionTarget?>(null) }
    var awaitingPermission by remember { mutableStateOf<PermissionTarget?>(null) }
    val scope = rememberCoroutineScope()

    AwaitPermissionThenReturnToApp(
        awaiting = awaitingPermission,
        onReturned = { awaitingPermission = null },
    )

    fun skipSetup() {
        viewModel.setEnabled(false)
        onBack()
    }

    BackHandler(enabled = true) { skipSetup() }

    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasOverlay = FocusShieldPermissionHelper.hasOverlayPermission(context)
                hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
                hasNotificationSuppressionAccess = FocusShieldPermissionHelper.hasNotificationListenerAccess(context)
                hasBatterySaver = FocusShieldPermissionHelper.isIgnoringBatteryOptimizations(context)
                viewModel.refreshPermissions()
                // If we already returned and the watched permission is granted, stop polling.
                val watching = awaitingPermission
                if (watching != null && FocusShieldPermissionHelper.isPermissionGranted(context, watching)) {
                    awaitingPermission = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask for permissions during initial Kavach setup
    LaunchedEffect(hasUsageStats, hasOverlay, hasNotificationSuppressionAccess, hasBatterySaver) {
        if (hasUsageStats && hasOverlay && hasNotificationSuppressionAccess && hasBatterySaver) {
            viewModel.setEnabled(true)
            onFinished()
        }
    }

    val totalSteps = 5
    var grantedCount = 0
    if (hasUsageStats) grantedCount++
    if (hasOverlay) grantedCount++
    if (hasNotifications) grantedCount++
    if (hasNotificationSuppressionAccess) grantedCount++
    if (hasBatterySaver) grantedCount++
    val progress = (grantedCount.toFloat() / totalSteps).coerceAtMost(1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "kavachPermissionProgress")

    var notificationsPrompted by remember { mutableStateOf(false) }
    val isUsageNext = !hasUsageStats
    val isOverlayNext = hasUsageStats && !hasOverlay
    val isBatterySaverNext = hasUsageStats && hasOverlay && !hasBatterySaver
    val isNotificationsNext = hasUsageStats && hasOverlay && hasBatterySaver && !hasNotifications && !notificationsPrompted
    val isNotificationAccessNext = hasUsageStats && hasOverlay && hasBatterySaver && !hasNotificationSuppressionAccess

    fun launchPermissionDirectly(target: PermissionTarget) {
        if (target != PermissionTarget.NOTIFICATIONS) {
            awaitingPermission = target
        }
        when (target) {
            PermissionTarget.USAGE_STATS ->
                FocusShieldPermissionHelper.openUsageAccessSettings(context)
            PermissionTarget.OVERLAY ->
                FocusShieldPermissionHelper.openOverlaySettings(context)
            PermissionTarget.NOTIFICATIONS -> {
                notificationsPrompted = true
                requestNotificationPermission()
            }
            PermissionTarget.NOTIFICATION_ACCESS ->
                FocusShieldPermissionHelper.openNotificationListenerSettings(context)
            PermissionTarget.BATTERY_SAVER ->
                FocusShieldPermissionHelper.openBatterySaverSettings(context)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.screen,
    ) {
        CompositionLocalProvider(
            com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme provides !isLightMode,
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = colors.progress,
                    trackColor = colors.progressTrack,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { skipSetup() }) {
                    Text(
                        text = stringResource(R.string.kavach_skip_for_now),
                        color = colors.secondaryText,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

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
                            text = stringResource(R.string.kavach_almost_there),
                            color = colors.secondaryText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                Spacer(Modifier.height(48.dp))

                KavachRegainPermissionRow(
                    title = stringResource(R.string.kavach_usage_permission),
                    subtitle = stringResource(R.string.kavach_usage_permission_help),
                    granted = hasUsageStats,
                    isNext = isUsageNext,
                    colors = colors,
                    onClick = { launchPermissionDirectly(PermissionTarget.USAGE_STATS) },
                )

                HorizontalDivider(color = colors.divider)

                KavachRegainPermissionRow(
                    title = stringResource(R.string.kavach_show_on_top),
                    subtitle = stringResource(R.string.kavach_show_on_top_help),
                    granted = hasOverlay,
                    isNext = isOverlayNext,
                    colors = colors,
                    onClick = { launchPermissionDirectly(PermissionTarget.OVERLAY) },
                )

                HorizontalDivider(color = colors.divider)

                KavachRegainPermissionRow(
                    title = stringResource(R.string.kavach_background_permission),
                    subtitle = stringResource(R.string.kavach_background_permission_help),
                    granted = hasBatterySaver,
                    isNext = isBatterySaverNext,
                    colors = colors,
                    onClick = { launchPermissionDirectly(PermissionTarget.BATTERY_SAVER) },
                )

                HorizontalDivider(color = colors.divider)

                KavachRegainPermissionRow(
                    title = stringResource(R.string.kavach_notifications),
                    subtitle = stringResource(R.string.kavach_notifications_help),
                    granted = hasNotifications,
                    isNext = isNotificationsNext,
                    colors = colors,
                    onClick = { launchPermissionDirectly(PermissionTarget.NOTIFICATIONS) },
                )

                HorizontalDivider(color = colors.divider)

                KavachRegainPermissionRow(
                    title = stringResource(R.string.kavach_notification_shield),
                    subtitle = stringResource(R.string.kavach_notification_shield_help),
                    granted = hasNotificationSuppressionAccess,
                    isNext = isNotificationAccessNext,
                    colors = colors,
                    onClick = { launchPermissionDirectly(PermissionTarget.NOTIFICATION_ACCESS) },
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
                        .clickable {
                            val nextTarget = when {
                                !hasUsageStats -> PermissionTarget.USAGE_STATS
                                !hasOverlay -> PermissionTarget.OVERLAY
                                !hasBatterySaver -> PermissionTarget.BATTERY_SAVER
                                !hasNotifications -> PermissionTarget.NOTIFICATIONS
                                !hasNotificationSuppressionAccess -> PermissionTarget.NOTIFICATION_ACCESS
                                else -> PermissionTarget.USAGE_STATS
                            }
                            selectedPermission = nextTarget
                        }
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
                        text = stringResource(R.string.kavach_why_permission),
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
                    // Watch settings toggles so we auto-return to this Kavach screen
                    // the moment Usage / Overlay / Notification Shield is granted.
                    if (it != PermissionTarget.NOTIFICATIONS) {
                        awaitingPermission = it
                    }
                    scope.launch {
                        when (it) {
                            PermissionTarget.USAGE_STATS ->
                                FocusShieldPermissionHelper.openUsageAccessSettings(context)
                            PermissionTarget.OVERLAY ->
                                FocusShieldPermissionHelper.openOverlaySettings(context)
                            PermissionTarget.NOTIFICATIONS -> requestNotificationPermission()
                            PermissionTarget.NOTIFICATION_ACCESS ->
                                FocusShieldPermissionHelper.openNotificationListenerSettings(context)
                            PermissionTarget.BATTERY_SAVER ->
                                FocusShieldPermissionHelper.openBatterySaverSettings(context)
                        }
                    }
                },
            )
        }
        } // CompositionLocalProvider LocalPlannerIsDarkTheme
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
    val titleColor = if (granted || isNext) colors.primaryText else colors.secondaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, onClick = onClick)
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
            if (!granted) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = colors.secondaryText,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(16.dp))

        if (granted) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.common_granted),
                tint = colors.helpText,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(99.dp),
                color = if (isNext) colors.cta else colors.cta.copy(alpha = 0.85f),
                modifier = Modifier.height(36.dp),
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.common_allow),
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
    val isDark = KavachDesign.isDark
    val accent = KavachDesign.Primary
    val ctaLabelColor = if (accent.luminance() > 0.55f) Color(0xFF16161A) else Color.White

    val title = when (permission) {
        PermissionTarget.USAGE_STATS -> stringResource(R.string.kavach_allow_app_check)
        PermissionTarget.OVERLAY -> stringResource(R.string.kavach_allow_show_on_top)
        PermissionTarget.NOTIFICATIONS -> stringResource(R.string.kavach_allow_notifications)
        PermissionTarget.NOTIFICATION_ACCESS -> stringResource(R.string.kavach_allow_notification_shield)
        PermissionTarget.BATTERY_SAVER -> stringResource(R.string.kavach_allow_background_running)
    }

    val bullets = when (permission) {
        PermissionTarget.USAGE_STATS -> listOf(
            stringResource(R.string.kavach_usage_bullet_private),
            stringResource(R.string.kavach_usage_bullet_open_app),
            stringResource(R.string.kavach_usage_bullet_block_only),
            stringResource(R.string.kavach_usage_bullet_no_ads),
        )
        PermissionTarget.OVERLAY -> listOf(
            stringResource(R.string.kavach_overlay_bullet_block_screen),
            stringResource(R.string.kavach_overlay_bullet_timer_only),
            stringResource(R.string.kavach_overlay_bullet_no_capture),
            stringResource(R.string.kavach_permission_bullet_turn_off),
        )
        PermissionTarget.NOTIFICATIONS -> listOf(
            stringResource(R.string.kavach_notifications_bullet_timer),
            stringResource(R.string.kavach_notifications_bullet_status),
        )
        PermissionTarget.NOTIFICATION_ACCESS -> listOf(
            stringResource(R.string.kavach_shield_bullet_selected_apps),
            stringResource(R.string.kavach_shield_bullet_timer_only),
            stringResource(R.string.kavach_shield_bullet_no_save),
            stringResource(R.string.kavach_permission_bullet_turn_off),
        )
        PermissionTarget.BATTERY_SAVER -> listOf(
            stringResource(R.string.kavach_battery_bullet_open),
            stringResource(R.string.kavach_battery_bullet_unrestricted),
            stringResource(R.string.kavach_battery_bullet_background),
            stringResource(R.string.kavach_battery_bullet_brands),
        )
    }

    CompositionLocalProvider(
        com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme provides isDark,
    ) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PlannerFlatColors.BgCream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = PlannerFlatColors.BorderSoft)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            PlanEyebrow(stringResource(R.string.kavach_name))
            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                fontFamily = LoraFontFamily,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark,
                lineHeight = 32.sp,
            )

            Spacer(Modifier.height(16.dp))
            PlanHairline()
            Spacer(Modifier.height(16.dp))

            // Help cue — flat hairline outline (not a filled card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.kavach_permission_safe_question),
                    color = PlannerFlatColors.TextMuted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            bullets.forEachIndexed { index, bullet ->
                if (index > 0) {
                    Spacer(Modifier.height(12.dp))
                    PlanHairline(alpha = 0.7f)
                    Spacer(Modifier.height(12.dp))
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "–",
                        color = PlannerFlatColors.TextMuted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 1.dp, end = 12.dp),
                    )
                    Text(
                        text = bullet,
                        color = PlannerFlatColors.TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            PlanHairline()
            Spacer(Modifier.height(20.dp))

            // Primary CTA — macOS translucent glass
            GlassButton(
                onClick = { onAllow(permission) },
                accentColor = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(16.dp),
                isDarkTheme = isDark,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (permission == PermissionTarget.NOTIFICATION_ACCESS) {
                        stringResource(R.string.kavach_agree_enable_notification_shield)
                    } else {
                        stringResource(R.string.kavach_allow)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ctaLabelColor,
                )
            }

            if (permission == PermissionTarget.NOTIFICATION_ACCESS) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, PlannerFlatColors.BorderSoft, RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.kavach_not_now),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlannerFlatColors.TextMuted,
                    )
                }
            }
        }
    }
    } // CompositionLocalProvider
}
