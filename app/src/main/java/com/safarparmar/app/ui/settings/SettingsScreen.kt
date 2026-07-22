package com.safarparmar.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.debug.NotificationDebugSettingsEntry
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import com.safarparmar.app.ui.premium.PremiumUiState
import com.safarparmar.app.ui.premium.PremiumViewModel
import com.safarparmar.app.ui.profile.GlassCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class SettingsInfoSheet {
    EULA,
    PRIVACY,
    KAVACH,
    OVERLAY,
    USAGE_ACCESS,
    NOTIFICATIONS,
}

private fun settingsPremiumPlanLabel(planType: String?): String {
    val normalized = planType.orEmpty().lowercase(Locale.US)
    return when {
        "3month" in normalized || "3-month" in normalized -> "3-month Premium plan"
        "6month" in normalized || "6-month" in normalized -> "6-month Premium plan"
        normalized.isNotBlank() -> "Safar Premium plan"
        else -> "Safar Premium"
    }
}

private fun formatSettingsPremiumExpiry(expiresAt: String?): String? {
    if (expiresAt.isNullOrBlank()) return null
    val instant = runCatching { Instant.parse(expiresAt) }.getOrNull() ?: return expiresAt.take(10)
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)
    return formatter.format(instant.atZone(ZoneId.systemDefault()))
}

private fun formatTimeDisplay(timeStr: String): String {
    if (timeStr.isBlank()) return "19:00 (7:00 PM)"
    val parts = timeStr.split(":")
    if (parts.size != 2) return timeStr
    val hour = parts[0].toIntOrNull() ?: return timeStr
    val minute = parts[1].toIntOrNull() ?: return timeStr
    val isPm = hour >= 12
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (isPm) "PM" else "AM"
    return String.format(Locale.US, "%02d:%02d (%d:%02d %s)", hour, minute, displayHour, minute, amPm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = false,
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onToggleDarkTheme: () -> Unit,
    dataStore: SafarDataStore,
    canAccessAdminComposer: Boolean = false,
    onOpenAdminNotificationComposer: () -> Unit = {},
    onPremium: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
    premiumViewModel: PremiumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val premiumStatus by premiumViewModel.premiumStatus.collectAsStateWithLifecycle()
    val premiumUiState by premiumViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scheme = MaterialTheme.colorScheme

    var pendingMasterEnable by remember { mutableStateOf(false) }
    var pendingDailyEnable by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var activeInfoSheet by remember { mutableStateOf<SettingsInfoSheet?>(null) }

    var hasUsageAccess by remember { mutableStateOf(FocusShieldPermissionHelper.hasUsageStatsPermission(context)) }
    var hasFocusShieldOverlay by remember { mutableStateOf(FocusShieldPermissionHelper.hasOverlayPermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(FocusShieldPermissionHelper.hasNotificationPermission(context)) }
    val isPremiumSyncing = premiumUiState is PremiumUiState.Loading
    val premiumExpiryText = remember(premiumStatus.expiresAt) { formatSettingsPremiumExpiry(premiumStatus.expiresAt) }
    val premiumPlanText = remember(premiumStatus.planType) { settingsPremiumPlanLabel(premiumStatus.planType) }

    LaunchedEffect(premiumUiState) {
        when (val state = premiumUiState) {
            is PremiumUiState.PaymentSuccess -> {
                Toast.makeText(context, "Safar Premium status synced.", Toast.LENGTH_SHORT).show()
                premiumViewModel.resetState()
            }
            is PremiumUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                premiumViewModel.resetState()
            }
            else -> Unit
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasFocusShieldOverlay = FocusShieldPermissionHelper.hasOverlayPermission(context)
                hasNotificationPermission = FocusShieldPermissionHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val requestNotificationPermission = rememberNotificationPermissionRequester { granted ->
        hasNotificationPermission = FocusShieldPermissionHelper.hasNotificationPermission(context)
        if (pendingMasterEnable) {
            pendingMasterEnable = false
            if (granted) {
                viewModel.onEvent(SettingsEvent.ToggleNotifications(true))
            } else {
                Toast.makeText(context, "Notification permission is required to enable alerts.", Toast.LENGTH_SHORT).show()
            }
        }
        if (pendingDailyEnable) {
            pendingDailyEnable = false
            if (granted) {
                if (!uiState.notificationsEnabled) viewModel.onEvent(SettingsEvent.ToggleNotifications(true))
                viewModel.onEvent(SettingsEvent.ToggleDailyStudyReminder(true))
            } else {
                Toast.makeText(context, "Notification permission is required for daily reminders.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    activeInfoSheet?.let { sheet ->
        SettingsLegalInfoSheet(
            sheet = sheet,
            onDismiss = { activeInfoSheet = null },
            onOpenOverlaySettings = {
                activeInfoSheet = null
                FocusShieldPermissionHelper.openOverlaySettings(context)
            },
        )
    }

    if (showTimePickerDialog) {
        DailyReminderTimePickerDialog(
            currentTime = uiState.dailyReminderTime,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { selectedTime ->
                viewModel.onEvent(SettingsEvent.UpdateDailyReminderTime(selectedTime))
                showTimePickerDialog = false
            }
        )
    }

    Scaffold(
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "Preferences, notifications, and permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = scheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = onHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = scheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onToggleDarkTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Nightlight,
                            contentDescription = "Toggle Theme",
                            tint = scheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── ACCOUNT & PREMIUM ─────────────────────────────────────────────────────────────
                SettingsGroupCard(title = "ACCOUNT & SUBSCRIPTION") {
                    SettingsPremiumHeaderCard(
                        isPremiumActive = premiumStatus.hasAnyPaidAccess,
                        planLabel = premiumPlanText,
                        expiryText = premiumExpiryText,
                        isSyncing = isPremiumSyncing,
                        onManagePlan = onPremium,
                        onSyncStatus = {
                            premiumViewModel.refreshPremiumStatus(
                                showLoading = true,
                                fallbackError = "No active Safar Premium plan found for this account."
                            )
                        },
                    )
                }

                // ── PREFERENCES & THEME ──────────────────────────────────────────────────────────
                SettingsGroupCard(title = "PREFERENCES & APPEARANCE") {
                    SettingsSwitchRow(
                        icon = if (isDarkTheme) Icons.Default.Nightlight else Icons.Default.WbSunny,
                        iconBgColor = scheme.primaryContainer,
                        iconTint = scheme.primary,
                        title = "Dark Theme",
                        subtitle = if (isDarkTheme) "Dark mode enabled" else "Light mode enabled",
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme() },
                    )
                }

                // ── STUDY NOTIFICATIONS ───────────────────────────────────────────────────────────
                SettingsGroupCard(title = "STUDY NOTIFICATIONS") {
                    SettingsSwitchRow(
                        icon = Icons.Default.Notifications,
                        iconBgColor = scheme.primaryContainer,
                        iconTint = scheme.primary,
                        title = "Allow Notifications",
                        subtitle = "Master switch for SAFAR study & class alerts",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                viewModel.onEvent(SettingsEvent.ToggleNotifications(false))
                            } else {
                                pendingMasterEnable = true
                                requestNotificationPermission()
                            }
                        },
                    )

                    AnimatedVisibility(
                        visible = uiState.notificationsEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))

                            SettingsSwitchRow(
                                icon = Icons.Default.CheckCircle,
                                iconBgColor = scheme.secondaryContainer,
                                iconTint = scheme.secondary,
                                title = "Ekagra Timer Updates",
                                subtitle = "Active timer status, break alerts, and session complete",
                                checked = uiState.focusTimerNotificationsEnabled,
                                enabled = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleFocusTimerNotifications(it)) },
                            )

                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                            SettingsSwitchRow(
                                icon = Icons.Default.AccessTime,
                                iconBgColor = scheme.tertiaryContainer,
                                iconTint = scheme.tertiary,
                                title = "Daily Study Reminder",
                                subtitle = "Scheduled daily reminder for your Ekagra focus block",
                                checked = uiState.dailyStudyReminderEnabled,
                                enabled = uiState.notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        viewModel.onEvent(SettingsEvent.ToggleDailyStudyReminder(false))
                                    } else {
                                        pendingDailyEnable = true
                                        requestNotificationPermission()
                                    }
                                },
                            )

                            if (uiState.dailyStudyReminderEnabled) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showTimePickerDialog = true }
                                        .padding(start = 66.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "REMINDER TIME",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                            color = scheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = formatTimeDisplay(uiState.dailyReminderTime),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = scheme.primary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = scheme.primaryContainer,
                                        modifier = Modifier.clickable { showTimePickerDialog = true }
                                    ) {
                                        Text(
                                            text = "Change",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = scheme.primary
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                            SettingsSwitchRow(
                                icon = Icons.Default.Notifications,
                                title = "Streak Expiry Warnings",
                                subtitle = "Evening reminder before your study streak expires",
                                checked = uiState.streakReminderEnabled,
                                enabled = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleStreakReminder(it)) },
                            )

                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                            SettingsSwitchRow(
                                icon = Icons.Default.Notifications,
                                title = "Course & Class Updates",
                                subtitle = "Live class alerts, new tests, and study PDFs",
                                checked = uiState.courseUpdatesEnabled,
                                enabled = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleCourseUpdates(it)) },
                            )

                            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                            SettingsSwitchRow(
                                icon = Icons.Default.Notifications,
                                title = "Community & Teacher Replies",
                                subtitle = "Mehfil replies, mentions, and Parmar Sir alerts",
                                checked = uiState.communityRepliesEnabled,
                                enabled = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleCommunityReplies(it)) },
                            )
                        }
                    }
                }

                // ── KAVACH PERMISSIONS ───────────────────────────────────────────────────────────
                val grantedCount = listOf(hasUsageAccess, hasFocusShieldOverlay, hasNotificationPermission).count { it }
                SettingsGroupCard(title = "KAVACH PERMISSIONS ($grantedCount OF 3 GRANTED)") {
                    SettingsPermissionRow(
                        icon = Icons.Default.Security,
                        title = "App Usage Permission",
                        subtitle = "Required for KAVACH to detect blocked apps during Ekagra sessions",
                        granted = hasUsageAccess,
                        onClickWhenNotGranted = { FocusShieldPermissionHelper.openUsageAccessSettings(context) },
                        onInfoClick = { activeInfoSheet = SettingsInfoSheet.USAGE_ACCESS },
                    )

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsPermissionRow(
                        icon = Icons.Default.Lock,
                        title = "Display Over Other Apps",
                        subtitle = "Allows KAVACH to show its block screen over distracting apps",
                        granted = hasFocusShieldOverlay,
                        onClickWhenNotGranted = { activeInfoSheet = SettingsInfoSheet.OVERLAY },
                        onInfoClick = { activeInfoSheet = SettingsInfoSheet.OVERLAY },
                    )

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsPermissionRow(
                        icon = Icons.Default.Notifications,
                        title = "System Notification Permission",
                        subtitle = "Android permission for SAFAR study and timer alerts",
                        granted = hasNotificationPermission,
                        onClickWhenNotGranted = requestNotificationPermission,
                        onInfoClick = { activeInfoSheet = SettingsInfoSheet.NOTIFICATIONS },
                    )
                }

                // ── LEGAL & ABOUT ────────────────────────────────────────────────────────────────
                SettingsGroupCard(title = "LEGAL & ABOUT") {
                    SettingsNavigationRow(
                        icon = Icons.Default.Info,
                        title = "End User License Agreement (EULA)",
                        subtitle = "User terms and conditions for SAFAR",
                        onClick = { activeInfoSheet = SettingsInfoSheet.EULA },
                    )

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsNavigationRow(
                        icon = Icons.Default.Lock,
                        title = "Privacy Policy & Data Security",
                        subtitle = "How SAFAR handles your data and privacy",
                        onClick = { activeInfoSheet = SettingsInfoSheet.PRIVACY },
                    )

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsNavigationRow(
                        icon = Icons.Default.Tune,
                        title = "Why KAVACH Needs Permissions",
                        subtitle = "A guide to KAVACH distraction blocking",
                        onClick = { activeInfoSheet = SettingsInfoSheet.KAVACH },
                    )

                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "App Version",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = scheme.onSurface
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME.substringBefore('-')}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                if (canAccessAdminComposer) {
                    SettingsGroupCard(title = "ADMIN TOOLS") {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            onClick = onOpenAdminNotificationComposer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Admin Notification Composer", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                NotificationDebugSettingsEntry()

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Grouped Inset Container ──────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        GlassCard {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

// ── Switch Row Item ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconBgColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

// ── Navigation Row Item ──────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    iconBgColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Permission Row Item ──────────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsPermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClickWhenNotGranted: (() -> Unit)?,
    onInfoClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val statusColor = if (granted) Color(0xFF10B981) else scheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted && onClickWhenNotGranted != null) {
                onClickWhenNotGranted?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (granted) Color(0xFF10B981).copy(alpha = 0.15f) else scheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (granted) "Granted" else "Required",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = statusColor,
            )
            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "More info",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── macOS Control Hero Card for Premium ──────────────────────────────────────────────────────────
@Composable
private fun SettingsPremiumHeaderCard(
    isPremiumActive: Boolean,
    planLabel: String,
    expiryText: String?,
    isSyncing: Boolean,
    onManagePlan: () -> Unit,
    onSyncStatus: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val statusText = if (isPremiumActive) {
        expiryText?.let { "Valid until $it" } ?: "$planLabel is active"
    } else {
        "Free plan active on this account"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPremiumActive) Color(0xFF10B981) else scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isPremiumActive) Color.White else scheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (isPremiumActive) "Safar Premium Active" else "Safar Plus Plan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = scheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isPremiumActive) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isPremiumActive) Color(0xFF10B981) else scheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onManagePlan,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
            ) {
                Text(if (isPremiumActive) "Manage Plan" else "Explore Premium", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onSyncStatus,
                enabled = !isSyncing,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = if (isSyncing) "Syncing..." else "Restore Status",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Native Material 3 TimePicker Dialog ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyReminderTimePickerDialog(
    currentTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialHour = currentTime.split(":").getOrNull(0)?.toIntOrNull() ?: 19
    val initialMinute = currentTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Daily Reminder Time",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    onConfirm(formatted)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Set Time", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ── Legal Bottom Sheet Component ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLegalInfoSheet(
    sheet: SettingsInfoSheet,
    onDismiss: () -> Unit,
    onOpenOverlaySettings: (() -> Unit)? = null,
) {
    val content = when (sheet) {
        SettingsInfoSheet.EULA -> SettingsInfoContent(
            title = "End User License Agreement",
            subtitle = "Terms for using SAFAR study and Ekagra focus tools.",
            points = listOf(
                "Keep your account credentials secure.",
                "Use study tools fairly and respectfully.",
                "KAVACH Focus Shield uses Usage Access and Overlay permissions solely to detect and block selected distracting apps during active Ekagra focus sessions.",
                "SAFAR does not read passwords, screen text, messages, or typed text.",
                "SAFAR does not sell or share application usage data.",
            ),
        )
        SettingsInfoSheet.PRIVACY -> SettingsInfoContent(
            title = "Privacy Policy & Data Protection",
            subtitle = "How SAFAR keeps your personal information private.",
            points = listOf(
                "Blocked app preferences remain stored locally on your device.",
                "Usage access is checked locally to match opened apps against your blocklist.",
                "SAFAR does not sell or share your activity or usage data with third parties.",
                "Payments are processed securely via Razorpay/PhonePe.",
            ),
        )
        SettingsInfoSheet.KAVACH -> SettingsInfoContent(
            title = "Why KAVACH Needs Permissions",
            subtitle = "Guide to KAVACH distraction shielding.",
            points = listOf(
                "Usage Access detects when a blocked app is opened.",
                "Display Over Other Apps renders the KAVACH focus screen over blocked apps.",
                "All checks happen locally on your device.",
            ),
        )
        SettingsInfoSheet.OVERLAY -> SettingsInfoContent(
            title = "Display Over Other Apps",
            subtitle = "Permission required to show the KAVACH block screen.",
            points = listOf(
                "Used only during active Ekagra focus timer sessions.",
                "Draws the KAVACH focus block screen when a selected app is launched.",
                "No background recording or screen reading takes place.",
            ),
        )
        SettingsInfoSheet.USAGE_ACCESS -> SettingsInfoContent(
            title = "App Usage Access",
            subtitle = "Permission to detect opened apps.",
            points = listOf(
                "Allows KAVACH to compare opened apps with your study blocklist.",
                "Used solely during active focus timer sessions.",
            ),
        )
        SettingsInfoSheet.NOTIFICATIONS -> SettingsInfoContent(
            title = "System Notification Permission",
            subtitle = "Android notification permissions for SAFAR.",
            points = listOf(
                "Ekagra timer progress and session complete alerts.",
                "Daily study reminder notifications.",
                "Streak loss warning alerts.",
            ),
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(content.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(content.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content.points.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(point, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (sheet == SettingsInfoSheet.OVERLAY && onOpenOverlaySettings != null) {
                Button(
                    onClick = onOpenOverlaySettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Open Display Over Apps Settings", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Not Now", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class SettingsInfoContent(
    val title: String,
    val subtitle: String,
    val points: List<String>,
)
