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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.core.os.LocaleListCompat
import androidx.compose.material3.*
import com.composables.ui.components.Text
import com.composables.ui.components.Icon
import com.safarparmar.app.ui.components.popups.SafarMaterialAlertDialog as AlertDialog
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.composables.ui.components.Switch as ComposablesSwitch

private const val URL_PRIVACY_POLICY = "https://safarapp.in/privacy"
private const val URL_TERMS = "https://safarapp.in/terms"

@Composable
private fun SettingsSheetSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 14.dp, bottom = 6.dp),
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = SettingsWarmColors.Ink
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SettingsWarmColors.Paper,
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                ) {
                content()
            }
        }
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    if (showLanguageDialog) {
        com.safarparmar.app.ui.components.LanguageSelectionDialog(onDismiss = { showLanguageDialog = false })
    }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showPermissionInfoDialog by remember { mutableStateOf(false) }

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

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme,
        com.safarparmar.app.ui.theme.LocalSafarComposablesAccent provides SettingsWarmColors.Accent) {
        com.safarparmar.app.ui.theme.SafarComposablesTheme {
        SafarDrawerScaffold(
            title = stringResource(R.string.settings_title),
            subtitle = null,
            currentRoute = currentRoute,
            isDarkTheme = isDarkTheme,
            onNavigate = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            containerColor = SettingsWarmColors.Canvas,
            topBarContentColor = SettingsWarmColors.Ink,
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
                    PremiumStatusSection(
                        isPremiumActive = premiumStatus.hasAnyPaidAccess,
                        onExplorePremium = onPremium,
                        onRestoreStatus = { premiumViewModel.refreshPremiumStatus() },
                    )

                    if (canAccessAdminComposer) {
                        
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

                    

                    StaggeredSettingsEntranceBox(index = 2, isVisible = settingsVisible) {
                        val haptic = LocalHapticFeedback.current
                        SettingsSheetSection(title = stringResource(R.string.settings_warm_appearance)) {
                            SettingsNavigationRow(
                                title = stringResource(R.string.profile_language_title),
                                subtitle = stringResource(R.string.sidebar_settings_hint),
                                icon = Icons.Default.Language,
                                onClick = { showLanguageDialog = true },
                            )
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


                        }
                    }

                    

                    StaggeredSettingsEntranceBox(index = 3, isVisible = settingsVisible) {
                        SettingsSheetSection(title = stringResource(R.string.settings_study_notifications)) {
                            NotificationsSection(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onShowTimePicker = { showTimePickerDialog = true },
                            )
                        }
                    }

                    

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

                    

                    StaggeredSettingsEntranceBox(index = 5, isVisible = settingsVisible) {
                        SettingsSheetSection(title = stringResource(R.string.settings_legal_information)) {
                            LegalSection(
                                context = context,
                                onShowPermissionInfo = { showPermissionInfoDialog = true },
                            )
                        }
                    }

                    

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
    com.composables.ui.components.Button(
        onClick = onExplorePremium, style = com.composables.ui.components.ButtonStyle.Ghost,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().background(Color(0xFF421735), RoundedCornerShape(22.dp)),
        contentPadding = PaddingValues(20.dp), contentColor = Color(0xFFFFF0F6),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).background(Color(0xFF795343), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD37A), modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.nav_premium), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(stringResource(if (isPremiumActive) R.string.settings_premium_unlocked else R.string.settings_free_active),
                    fontSize = 12.sp, color = Color(0xFFEDD8E5))
            }
            Text(stringResource(R.string.settings_warm_manage), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun NotificationsSection(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onShowTimePicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_allow_notifications),
            subtitle = stringResource(R.string.settings_allow_notifications_subtitle),
            checked = uiState.notificationsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.ToggleNotifications(it)) },
            icon = Icons.Default.Notifications,
        )

        if (uiState.notificationsEnabled) {
            GroupDivider()

            SettingsSwitchRow(
                title = stringResource(R.string.settings_timer_updates),
                subtitle = stringResource(R.string.settings_timer_updates_subtitle),
                checked = uiState.focusTimerNotificationsEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleFocusTimerNotifications(it)) },
                icon = Icons.Default.Timer,
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_daily_reminder),
                subtitle = stringResource(R.string.settings_daily_reminder_subtitle),
                checked = uiState.dailyStudyReminderEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleDailyStudyReminder(it)) },
                icon = Icons.Default.Schedule,
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
                            tint = SettingsWarmColors.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_reminder_time),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SettingsWarmColors.Ink
                        )
                    }
                    Text(
                        text = formatTime12h(h, m),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SettingsWarmColors.Accent
                    )
                }
            }

            SettingsSwitchRow(
                title = stringResource(R.string.settings_streak_warning),
                subtitle = stringResource(R.string.settings_streak_warning_subtitle),
                checked = uiState.streakReminderEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleStreakReminder(it)) },
                icon = Icons.Default.LocalFireDepartment,
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_course_updates),
                subtitle = stringResource(R.string.settings_course_updates_subtitle),
                checked = uiState.courseUpdatesEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleCourseUpdates(it)) },
                icon = Icons.Default.School,
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_mehfil_replies),
                subtitle = stringResource(R.string.settings_mehfil_replies_subtitle),
                checked = uiState.communityRepliesEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleCommunityReplies(it)) },
                icon = Icons.Default.Forum,
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
            icon = Icons.Default.QueryStats,
            onGrantClick = {
                com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper.openUsageAccessSettings(context)
            },
        )

        PermissionRow(
            title = stringResource(R.string.settings_display_over_apps),
            subtitle = stringResource(R.string.settings_display_over_apps_subtitle),
            isGranted = hasOverlayPermission,
            icon = Icons.Default.Layers,
            onGrantClick = {
                com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper.openOverlaySettings(context)
            },
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionRow(
                title = stringResource(R.string.settings_system_notifications),
                subtitle = stringResource(R.string.settings_system_notifications_subtitle),
                isGranted = hasNotificationPermission,
                icon = Icons.Default.Notifications,
                onGrantClick = {
                    com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper.openNotificationSettings(context)
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
    icon: ImageVector = androidx.compose.material.icons.Icons.Default.Security
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onGrantClick)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            SettingsSquircleIcon(
                icon = icon,
                iconTint = SettingsWarmColors.Ink,
                backgroundColor = SettingsWarmColors.Peach
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsWarmColors.Ink
                )
                if (subtitle.isNotBlank() && !subtitle.trim().equals(title.trim(), ignoreCase = true)) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = SettingsWarmColors.Muted
                    )
                }
            }
        }

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.settings_granted),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SettingsWarmColors.Accent)
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_grant),
                    fontSize = 12.sp,
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
    val isDark = LocalPlannerIsDarkTheme.current == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                SettingsSquircleIcon(
                    icon = icon,
                    iconTint = SettingsWarmColors.Ink,
                    backgroundColor = SettingsWarmColors.Peach
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsWarmColors.Ink
                )

            }
        }

        if (icon == Icons.Default.Nightlight || icon == Icons.Default.WbSunny) {
            com.safarparmar.app.ui.drawer.JetCoSwitchButton(checked, onCheckedChange,
                selectedTrackColor = SettingsWarmColors.Accent, unselectedTrackColor = SettingsWarmColors.Lavender,
                knobColor = SettingsWarmColors.Paper, iconColor = SettingsWarmColors.Ink)
        } else {
            ComposablesSwitch(checked = checked, onCheckedChange = onCheckedChange, accessibilityLabel = title)
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color? = null,
    iconBg: Color? = null
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    val defaultIconTint = SettingsWarmColors.Ink
    val defaultIconBg = SettingsWarmColors.Peach
    
    val finalIconTint = iconTint ?: defaultIconTint
    val finalIconBg = iconBg ?: defaultIconBg
    
    val isDelete = title.contains("Delete", ignoreCase = true)
    val activeTint = if (isDelete) Color(0xFFEF4444) else finalIconTint
    val activeBg = if (isDelete) Color(0xFFFEE2E2) else finalIconBg
    val activeTitleColor = if (isDelete) Color(0xFFEF4444) else SettingsWarmColors.Ink

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            SettingsSquircleIcon(
                icon = icon,
                iconTint = activeTint,
                backgroundColor = if (isDark && isDelete) Color(0xFF5F1717) else activeBg
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTitleColor
                )
                if (icon != Icons.Default.Language && subtitle.isNotBlank() && !subtitle.trim().equals(title.trim(), ignoreCase = true)) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = SettingsWarmColors.Muted
                    )
                }
            }
        }
        if (icon == Icons.Default.Language) {
            val locale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0) ?: java.util.Locale.getDefault()
            Text(locale.getDisplayLanguage(locale), color = SettingsWarmColors.Muted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp))
        }
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SettingsWarmColors.Muted,
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
        containerColor = SettingsWarmColors.ComponentBackground,
        title = {
            Text(
                text = stringResource(R.string.settings_select_reminder_time),
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = SettingsWarmColors.Ink
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
                        selectorColor = SettingsWarmColors.Accent,
                        containerColor = Color.Transparent,
                        periodSelectorBorderColor = SettingsWarmColors.Accent,
                        periodSelectorSelectedContainerColor = SettingsWarmColors.Accent,
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = Color.White,
                        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        timeSelectorSelectedContainerColor = SettingsWarmColors.Accent.copy(alpha = 0.15f),
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = SettingsWarmColors.Accent,
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
                    containerColor = SettingsWarmColors.Accent,
                    contentColor = SafarSemanticColors.brandOnPurple(),
                ),
            ) {
                Text(stringResource(R.string.settings_save_time), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_cancel), fontWeight = FontWeight.Bold, color = SettingsWarmColors.Muted)
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun PermissionExplanationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SettingsWarmColors.ComponentBackground,
        icon = { Icon(Icons.Default.Security, null, tint = SettingsWarmColors.Accent) },
        title = {
            Text(
                text = stringResource(R.string.settings_kavach_privacy_title),
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = SettingsWarmColors.Ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.settings_kavach_privacy_intro),
                    fontSize = 13.sp,
                    color = SettingsWarmColors.Muted
                )
                Text(
                    text = stringResource(R.string.settings_kavach_usage_explanation),
                    fontSize = 12.5.sp,
                    color = SettingsWarmColors.Ink
                )
                Text(
                    text = stringResource(R.string.settings_kavach_overlay_explanation),
                    fontSize = 12.5.sp,
                    color = SettingsWarmColors.Ink
                )
                Text(
                    text = stringResource(R.string.settings_kavach_notification_explanation),
                    fontSize = 12.5.sp,
                    color = SettingsWarmColors.Ink
                )
                Text(
                    text = stringResource(R.string.settings_kavach_privacy_promise),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SettingsWarmColors.Accent
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SettingsWarmColors.Accent,
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
            color = SettingsWarmColors.Muted,
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


@Composable
private fun GroupDivider() {
    val isDark = LocalPlannerIsDarkTheme.current == true
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp, end = 0.dp),
        thickness = 0.6.dp,
        color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    )
}

@Composable
fun SettingsSquircleIcon(
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}
