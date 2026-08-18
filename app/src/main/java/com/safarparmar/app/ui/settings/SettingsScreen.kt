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
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showPermissionInfoDialog by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasUsagePermission = checkUsageStatsPermission(context)
        hasOverlayPermission = checkOverlayPermission(context)
        hasNotificationPermission = checkNotificationPermission(context)
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
            title = "Settings",
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
                            text = "Customize theme, notifications, and permissions",
                            fontSize = 13.sp,
                            color = PlannerFlatColors.TextMuted,
                        )
                    }

                    StaggeredSettingsEntranceBox(index = 0, isVisible = settingsVisible) {
                        SettingsSheetSection(title = "Account & Subscription") {
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
                            SettingsSheetSection(title = "Admin Tools") {
                                SettingsNavigationRow(
                                    title = "Notification Composer",
                                    subtitle = "Broadcast push alerts to all enrolled students",
                                    icon = Icons.Default.AdminPanelSettings,
                                    onClick = onOpenAdminNotificationComposer,
                                )
                            }
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 2, isVisible = settingsVisible) {
                        val haptic = LocalHapticFeedback.current
                        SettingsSheetSection(title = "Preferences & Appearance") {
                            SettingsSwitchRow(
                                title = "Dark Theme",
                                subtitle = "Switch between dark and light theme",
                                checked = isDarkTheme,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleDarkTheme()
                                },
                                icon = if (isDarkTheme) Icons.Default.Nightlight else Icons.Default.WbSunny,
                            )
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 3, isVisible = settingsVisible) {
                        SettingsSheetSection(title = "Study Notifications") {
                            NotificationsSection(
                                uiState = uiState,
                                onEvent = viewModel::onEvent,
                                onShowTimePicker = { showTimePickerDialog = true },
                            )
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    val grantedCount = listOf(hasUsagePermission, hasOverlayPermission, hasNotificationPermission).count { it }
                    StaggeredSettingsEntranceBox(index = 4, isVisible = settingsVisible) {
                        SettingsSheetSection(title = "App Permissions ($grantedCount/3)") {
                            PermissionsSection(
                                hasUsagePermission = hasUsagePermission,
                                hasOverlayPermission = hasOverlayPermission,
                                hasNotificationPermission = hasNotificationPermission,
                                context = context,
                            )
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 5, isVisible = settingsVisible) {
                        SettingsSheetSection(title = "Legal & Information") {
                            LegalSection(
                                context = context,
                                onShowPermissionInfo = { showPermissionInfoDialog = true },
                            )
                        }
                    }

                    PlanHairline(alpha = 0.5f)

                    StaggeredSettingsEntranceBox(index = 6, isVisible = settingsVisible) {
                        SettingsSheetSection(title = "Account & Data Management") {
                            SettingsNavigationRow(
                                title = "Delete Account & Data",
                                subtitle = "Permanently wipe your account, study history, and private data",
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

@Composable
private fun PremiumStatusSection(
    isPremiumActive: Boolean,
    onExplorePremium: () -> Unit,
    onRestoreStatus: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val statusTitle = if (isPremiumActive) "Safar Premium Active" else "Safar Plus Plan"
    val statusSubtitle = if (isPremiumActive) "All AI planning and analytics features unlocked" else "Standard free features active"

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
                    text = if (isPremiumActive) "Manage" else "Explore",
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
            title = "Allow Notifications",
            subtitle = "Turn on all study alerts and reminders",
            checked = uiState.notificationsEnabled,
            onCheckedChange = { onEvent(SettingsEvent.ToggleNotifications(it)) },
            icon = Icons.Default.Notifications,
        )

        if (uiState.notificationsEnabled) {
            PlanHairline(alpha = 0.4f)

            SettingsSwitchRow(
                title = "Ekagra Timer Updates",
                subtitle = "Sound and vibration alerts for study timer",
                checked = uiState.focusTimerNotificationsEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleFocusTimerNotifications(it)) },
            )

            SettingsSwitchRow(
                title = "Daily Study Reminder",
                subtitle = "Daily alert to start your study sessions",
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
                            text = "Reminder Time",
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
                title = "Streak Expiry Warnings",
                subtitle = "Alert 2 hours before losing daily streak",
                checked = uiState.streakReminderEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleStreakReminder(it)) },
            )

            SettingsSwitchRow(
                title = "Course Updates",
                subtitle = "Alerts for live classes & audio",
                checked = uiState.courseUpdatesEnabled,
                onCheckedChange = { onEvent(SettingsEvent.ToggleCourseUpdates(it)) },
            )

            SettingsSwitchRow(
                title = "Mehfil Replies",
                subtitle = "Alerts when someone replies to your posts",
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
    context: Context,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PermissionRow(
            title = "Usage Access",
            subtitle = "Required for focus app tracking",
            isGranted = hasUsagePermission,
            onGrantClick = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
        )

        PermissionRow(
            title = "Display Over Apps",
            subtitle = "Required to show focus shield overlay",
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
                title = "System Notifications",
                subtitle = "Required for timer and study alerts",
                isGranted = hasNotificationPermission,
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
            )
        }
    }
}

@Composable
private fun LegalSection(
    context: Context,
    onShowPermissionInfo: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsNavigationRow(
            title = "Privacy Policy",
            subtitle = "How SAFAR handles and protects your data",
            icon = Icons.Default.PrivacyTip,
            onClick = { openUrl(context, URL_PRIVACY_POLICY) },
        )

        SettingsNavigationRow(
            title = "Terms of Service",
            subtitle = "End User License Agreement & Rules",
            icon = Icons.Default.Gavel,
            onClick = { openUrl(context, URL_TERMS) },
        )

        SettingsNavigationRow(
            title = "Why Kavach Needs Permissions",
            subtitle = "Detailed explanation of Focus Shield privacy guarantees",
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
        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Granted",
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
                    .clickable(onClick = onGrantClick)
                    .padding(vertical = 7.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Grant",
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

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SafarSemanticColors.plannerBackground(),
        title = {
            Text(
                text = "Select Reminder Time",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime12h(selectedHour, selectedMinute),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarSemanticColors.brandPurple()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedHour, selectedMinute) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafarSemanticColors.brandPurple(),
                    contentColor = SafarSemanticColors.brandOnPurple(),
                ),
            ) {
                Text("Save Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = PlannerFlatColors.TextMuted)
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
                text = "Kavach Privacy & Permissions",
                fontFamily = LoraFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = PlannerFlatColors.TextDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SAFAR Kavach uses Android permissions strictly during active Ekagra study sessions:",
                    fontSize = 13.sp,
                    color = PlannerFlatColors.TextMuted
                )
                Text(
                    text = "• Usage Access: Detects when a distracting app is launched so Kavach can block it.",
                    fontSize = 12.5.sp,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = "• Display Over Apps: Renders the full-screen study focus shield over distracting apps.",
                    fontSize = 12.5.sp,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = "Your personal data is never transmitted or sold.",
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
                Text("Got It", fontWeight = FontWeight.Bold)
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
            text = "SAFAR Study Planner & Focus Suite\nVersion 1.0.4 • Build 104",
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
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
