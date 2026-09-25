package com.safarparmar.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.components.DeleteAccountDialog
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.components.PlannerFlatColors
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.SafarSemanticColors
import com.safarparmar.app.R

private const val URL_PRIVACY_POLICY = "https://safarapp.in/privacy"
private const val URL_TERMS = "https://safarapp.in/terms"

@Composable
private fun SettingsSheetSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PlannerFlatColors.TextDark,
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentRoute: String = Routes.SETTINGS,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onHome: () -> Unit = {},
    dataStore: SafarDataStore,
    canAccessAdminComposer: Boolean = false,
    onOpenAdminNotificationComposer: () -> Unit = {},
    onPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsagePermission by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasNotificationShieldPermission by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showPermissionInfoDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasUsagePermission = checkUsageStatsPermission(context)
        hasOverlayPermission = checkOverlayPermission(context)
        hasNotificationPermission = checkNotificationPermission(context)
        hasNotificationShieldPermission = FocusShieldPermissionHelper.hasNotificationListenerAccess(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { refreshPermissions() }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
        SafarDrawerScaffold(
            title = stringResource(R.string.settings_title),
            subtitle = null,
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            containerColor = SafarSemanticColors.plannerBackground(),
        ) { paddingValues ->
            var settingsVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                settingsVisible = true
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            fontSize = 13.sp,
                            color = PlannerFlatColors.TextMuted,
                        )
                    }

                    StaggeredSettingsEntranceBox(index = 0, isVisible = settingsVisible) {
                        SettingsSheetSection(title = stringResource(R.string.settings_account_subscription)) {
                            PremiumStatusSection(
                                isPremiumActive = premiumStatus.hasAnyPaidAccess,
                                onExplorePremium = onPremium,
                                onRestoreStatus = { premiumViewModel.refreshPremiumStatus() },
                            )
                        }
                    }

                    if (canAccessAdminComposer) {
                        PlanHairline(alpha = 0.5f)
                        StaggeredSettingsEntranceBox(index = 1, isVisible = settingsVisible) {
                            SettingsSheetSection(title = stringResource(R.string.settings_admin_tools)) {
                                SettingsNavigationRow(
                                    title = stringResource(R.string.settings_notification_composer),
                                    subtitle = stringResource(R.string.settings_notification_composer_subtitle),
                                    icon = Icons.Default.AdminPanelSettings,
                                    onClick = onOpenAdminNotificationComposer,
                                )
                            }
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 2, isVisible = settingsVisible) {
                        val haptic = LocalHapticFeedback.current
                        SettingsSheetSection(title = stringResource(R.string.settings_preferences_appearance)) {
                            SettingsSwitchRow(
                                title = stringResource(R.string.settings_dark_theme),
                                subtitle = stringResource(R.string.settings_dark_theme_subtitle),
                                checked = isDarkTheme,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleDarkTheme()
                                },
                                icon = if (isDarkTheme) Icons.Default.Nightlight else Icons.Default.WbSunny,
                            )
                            // Language selector hidden from settings as requested.
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 3, isVisible = settingsVisible) {
                        SettingsSheetSection(title = stringResource(R.string.settings_study_notifications)) {
                            NotificationsSection(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onShowTimePicker = { showTimePickerDialog = true },
                            )
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    val grantedCount = listOf(hasUsagePermission, hasOverlayPermission, hasNotificationPermission, hasNotificationShieldPermission).count { it }
                    StaggeredSettingsEntranceBox(index = 4, isVisible = settingsVisible) {
                        SettingsSheetSection(title = stringResource(R.string.settings_app_permissions, grantedCount, 4)) {
                            PermissionsSection(
                                hasUsagePermission = hasUsagePermission,
                                hasOverlayPermission = hasOverlayPermission,
                                hasNotificationPermission = hasNotificationPermission,
                                hasNotificationShieldPermission = hasNotificationShieldPermission,
                                context = context,
                            )
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 5, isVisible = settingsVisible) {
                        SettingsSheetSection(title = stringResource(R.string.settings_legal_information)) {
                            LegalSection(
                                context = context,
                                onShowPermissionInfo = { showPermissionInfoDialog = true },
                            )
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 6, isVisible = settingsVisible) {
                        SettingsSheetSection(title = stringResource(R.string.settings_account_data)) {
                            SettingsNavigationRow(
                                title = stringResource(R.string.settings_delete_account),
                                subtitle = stringResource(R.string.settings_delete_account_subtitle),
                                icon = Icons.Default.DeleteForever,
                                onClick = { viewModel.onEvent(SettingsEvent.ShowDeleteAccountDialog) },
                            )
                        }
                    }

                    FooterSection()

                    Spacer(Modifier.height(16.dp))
                }
            }

            if (uiState.showDeleteAccountDialog) {
                DeleteAccountDialog(
                    userEmail = uiState.userEmail,
                    isDeleting = uiState.isDeletingAccount,
                    errorMessage = uiState.deleteAccountError,
                    onDismiss = { viewModel.onEvent(SettingsEvent.DismissDeleteAccountDialog) },
                    onConfirmDelete = { password ->
                        viewModel.onEvent(SettingsEvent.DeleteAccount(password))
                    },
                )
            }

            if (showTimePickerDialog) {
                val (h, m) = parseReminderTime(uiState.dailyReminderTime)
                TimePickerDialog(
                    initialHour = h,
                    initialMinute = m,
                    onDismiss = { showTimePickerDialog = false },
                    onConfirm = { hour, minute ->
                        val formatted = String.format("%02d:%02d", hour, minute)
                        viewModel.onEvent(SettingsEvent.UpdateDailyReminderTime(formatted))
                        showTimePickerDialog = false
                    },
                )
            }

            if (showPermissionInfoDialog) {
                PermissionExplanationDialog(onDismiss = { showPermissionInfoDialog = false })
            }

            if (showLanguageDialog) {
                val choices = listOf(
                    "en" to stringResource(R.string.profile_language_english),
                    "hi" to stringResource(R.string.profile_language_hindi),
                    "hi-Latn" to stringResource(R.string.profile_language_hinglish),
                )
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = { Text(stringResource(R.string.profile_language_dialog_title)) },
                    text = {
                        Column {
                            choices.forEach { (languageTag, label) ->
                                TextButton(
                                    onClick = {
                                        showLanguageDialog = false
                                        AppCompatDelegate.setApplicationLocales(
                                            LocaleListCompat.forLanguageTags(languageTag)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(label, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {},
                )
            }
        }
    }
}

@Composable
private fun PremiumStatusSection(
    isPremiumActive: Boolean,
    onExplorePremium: () -> Unit,
    onRestoreStatus: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val statusTitle = stringResource(if (isPremiumActive) R.string.settings_premium_active else R.string.settings_plus_plan)
    val statusSubtitle = stringResource(if (isPremiumActive) R.string.settings_premium_unlocked else R.string.settings_free_active)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPremiumActive) SafarSemanticColors.brandPurple().copy(alpha = 0.12f) else PlannerFlatColors.TextMuted.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremiumActive) SafarSemanticColors.brandPurple() else PlannerFlatColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = statusTitle,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = statusSubtitle,
                    fontSize = 12.5.sp,
                    color = if (isPremiumActive) SafarSemanticColors.brandPurple() else PlannerFlatColors.TextMuted
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SafarSemanticColors.brandPurple().copy(alpha = 0.08f))
                    .border(1.dp, SafarSemanticColors.brandPurple().copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onExplorePremium)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(if (isPremiumActive) R.string.settings_manage else R.string.settings_explore),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarSemanticColors.brandPurple()
                )
            }
        }
    }
}

@Composable
private fun NotificationsSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onShowTimePicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_allow_notifications),
            subtitle = stringResource(R.string.settings_allow_notifications_subtitle),
            checked = uiState.notificationsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.ToggleNotifications(it)) },
            icon = Icons.Default.Notifications,
        )

        if (uiState.notificationsEnabled) {
            PlanHairline(alpha = 0.4f)

            SettingsSwitchRow(
                title = stringResource(R.string.settings_timer_updates),
                subtitle = stringResource(R.string.settings_timer_updates_subtitle),
                checked = uiState.focusTimerNotificationsEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleFocusTimerNotifications(it)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_daily_reminder),
                subtitle = stringResource(R.string.settings_daily_reminder_subtitle),
                checked = uiState.dailyStudyReminderEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleDailyStudyReminder(it)) },
            )

            if (uiState.dailyStudyReminderEnabled) {
                val (h, m) = parseReminderTime(uiState.dailyReminderTime)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onShowTimePicker)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = SafarSemanticColors.brandPurple(),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_reminder_time),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PlannerFlatColors.TextDark
                        )
                    }
                    Text(
                        text = formatTime12h(h, m),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafarSemanticColors.brandPurple()
                    )
                }
            }

            SettingsSwitchRow(
                title = stringResource(R.string.settings_streak_warning),
                subtitle = stringResource(R.string.settings_streak_warning_subtitle),
                checked = uiState.streakReminderEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleStreakReminder(it)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_course_updates),
                subtitle = stringResource(R.string.settings_course_updates_subtitle),
                checked = uiState.courseUpdatesEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleCourseUpdates(it)) },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_mehfil_replies),
                subtitle = stringResource(R.string.settings_mehfil_replies_subtitle),
                checked = uiState.communityRepliesEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleCommunityReplies(it)) },
            )
        }
    }
}

@Composable
private fun PermissionsSection(
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasNotificationShieldPermission: Boolean,
    context: Context,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PermissionRow(
            title = stringResource(R.string.settings_usage_access),
            subtitle = stringResource(R.string.settings_usage_access_subtitle),
            isGranted = hasUsagePermission,
            onGrantClick = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
        )

        PermissionRow(
            title = stringResource(R.string.settings_display_over_apps),
            subtitle = stringResource(R.string.settings_display_over_apps_subtitle),
            isGranted = hasOverlayPermission,
            onGrantClick = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            },
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                title = stringResource(R.string.settings_system_notifications),
                subtitle = stringResource(R.string.settings_system_notifications_subtitle),
                isGranted = hasNotificationPermission,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
            )
        }

        PermissionRow(
            title = stringResource(R.string.settings_notification_shield),
            subtitle = stringResource(R.string.settings_notification_shield_subtitle),
            isGranted = hasNotificationShieldPermission,
            onGrantClick = {
                FocusShieldPermissionHelper.openNotificationListenerSettings(context)
            },
        )
    }
}

@Composable
private fun LegalSection(
    context: Context,
    onShowPermissionInfo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsNavigationRow(
            title = stringResource(R.string.settings_privacy_policy),
            subtitle = stringResource(R.string.settings_privacy_policy_subtitle),
            icon = Icons.Default.PrivacyTip,
            onClick = { openUrl(context, URL_PRIVACY_POLICY) },
        )

        SettingsNavigationRow(
            title = stringResource(R.string.settings_terms),
            subtitle = stringResource(R.string.settings_terms_subtitle),
            icon = Icons.Default.Gavel,
            onClick = { openUrl(context, URL_TERMS) },
        )

        SettingsNavigationRow(
            title = stringResource(R.string.settings_kavach_permissions),
            subtitle = stringResource(R.string.settings_kavach_permissions_subtitle),
            icon = Icons.Default.Info,
            onClick = onShowPermissionInfo,
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGrantClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PlannerFlatColors.TextDark
            )
            Text(
                text = subtitle,
                fontSize = 13.5.sp,
                color = PlannerFlatColors.TextMuted
            )
        }

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.settings_granted),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SafarSemanticColors.brandPurple())
                    .padding(vertical = 7.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_grant),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarSemanticColors.brandOnPurple()
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SafarSemanticColors.brandPurple(),
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    color = PlannerFlatColors.TextMuted
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = scheme.onPrimary,
                checkedTrackColor = SafarSemanticColors.brandPurple(),
                uncheckedTrackColor = PlannerFlatColors.BorderSoft,
                uncheckedThumbColor = PlannerFlatColors.TextMuted,
            )
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SafarSemanticColors.brandPurple(),
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = PlannerFlatColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafarSemanticColors.plannerBackground(),
        title = {
            Text(
                text = stringResource(R.string.settings_select_reminder_time),
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = SafarSemanticColors.brandPurple(),
                        containerColor = Color.Transparent,
                        periodSelectorBorderColor = SafarSemanticColors.brandPurple(),
                        periodSelectorSelectedContainerColor = SafarSemanticColors.brandPurple(),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        timeSelectorSelectedContainerColor = SafarSemanticColors.brandPurple().copy(alpha = 0.15f),
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = SafarSemanticColors.brandPurple(),
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafarSemanticColors.brandPurple(),
                    contentColor = SafarSemanticColors.brandOnPurple(),
                ),
            ) {
                Text(stringResource(R.string.settings_save_time), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel), fontWeight = FontWeight.Bold, color = PlannerFlatColors.TextMuted)
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun PermissionExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafarSemanticColors.plannerBackground(),
        icon = { Icon(Icons.Default.Security, null, tint = SafarSemanticColors.brandPurple()) },
        title = {
            Text(
                text = stringResource(R.string.settings_kavach_privacy_title),
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_kavach_privacy_intro),
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextMuted
                )
                Text(
                    text = stringResource(R.string.settings_kavach_usage_explanation),
                    fontSize = 12.5.sp,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = stringResource(R.string.settings_kavach_overlay_explanation),
                    fontSize = 12.5.sp,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = stringResource(R.string.settings_kavach_notification_explanation),
                    fontSize = 12.5.sp,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = stringResource(R.string.settings_kavach_privacy_promise),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarSemanticColors.brandPurple()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafarSemanticColors.brandPurple(),
                    contentColor = SafarSemanticColors.brandOnPurple(),
                ),
            ) {
                Text(stringResource(R.string.settings_got_it), fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_footer_version),
            fontSize = 12.sp,
            color = PlannerFlatColors.TextMuted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun parseReminderTime(rawTime: String): Pair<Int, Int> {
    val parts = rawTime.split(":")
    if (parts.size != 2) return Pair(19, 0)
    val h = parts[0].toIntOrNull() ?: 19
    val m = parts[1].toIntOrNull() ?: 0
    return Pair(h, m)
}

private fun formatTime12h(hour: Int, minute: Int): String {
    val amPm = if (hour >= 12) "PM" else "AM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val minStr = String.format("%02d", minute)
    return "$hour12:$minStr $amPm"
}

private fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    }
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}

private fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
}

@Composable
private fun StaggeredSettingsEntranceBox(
    index: Int,
    isVisible: Boolean,
    content: @Composable () -> Unit,
) {
    val slideOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVisible) 0.dp else (20 + index * 12).dp,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 320,
            delayMillis = index * 40,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "settingsStaggeredOffset",
    )
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 280,
            delayMillis = index * 40,
        ),
        label = "settingsStaggeredAlpha",
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = slideOffset.toPx()
                alpha = alphaAnim
            }
    ) {
        content()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(com.safarparmar.app.R.string.settings_link_failed), Toast.LENGTH_SHORT).show()
    }
}
