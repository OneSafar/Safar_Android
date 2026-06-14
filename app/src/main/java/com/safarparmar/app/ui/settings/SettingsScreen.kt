package com.safarparmar.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.safarparmar.app.ui.components.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.debug.NotificationDebugSettingsEntry
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import com.safarparmar.app.ui.profile.NotificationToggleRow
import com.safarparmar.app.ui.profile.ProfileSectionCard

private enum class SettingsInfoSheet {
    EULA,
    PRIVACY,
    KAVACH,
    ACCESSIBILITY,
    USAGE_ACCESS,
    NOTIFICATIONS,
}

private fun isValidReminderTimeInput(value: String): Boolean {
    if (!Regex("^\\d{2}:\\d{2}$").matches(value)) return false
    val parts = value.split(":")
    if (parts.size != 2) return false
    val hour = parts[0].toIntOrNull() ?: return false
    val minute = parts[1].toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
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
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var pendingMasterEnable by remember { mutableStateOf(false) }
    var pendingDailyEnable by remember { mutableStateOf(false) }
    var reminderDraft by remember(uiState.dailyReminderTime) { mutableStateOf(uiState.dailyReminderTime) }
    var activeInfoSheet by remember { mutableStateOf<SettingsInfoSheet?>(null) }

    var hasUsageAccess by remember { mutableStateOf(FocusShieldPermissionHelper.hasUsageStatsPermission(context)) }
    var hasFocusShieldAccessibility by remember { mutableStateOf(FocusShieldPermissionHelper.hasAccessibilityService(context)) }
    var hasNotificationPermission by remember { mutableStateOf(FocusShieldPermissionHelper.hasNotificationPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasFocusShieldAccessibility = FocusShieldPermissionHelper.hasAccessibilityService(context)
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
        )
    }

    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Appearance, notifications, and permissions.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = onHome) {
                        Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onToggleDarkTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Nightlight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background)
        ) {
            val gradientStart = if (isDarkTheme) Color(0xFF1B212D) else Color(0xFFD6E9FF)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(gradientStart, Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ProfileSectionCard(title = "Appearance & about", icon = Icons.Default.Tune) {
                    Text(
                        "Sun/moon toggles light and dark theme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NotificationToggleRow(
                        title = "Dark Mode",
                        subtitle = if (isDarkTheme) "On — dark theme enabled" else "Off",
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme() },
                    )
                    Text(
                        "Version ${BuildConfig.VERSION_NAME.substringBefore('-')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ProfileSectionCard(title = "Notifications", icon = Icons.Default.Notifications) {
                    Text(
                        "Helpful alerts for focus sessions, streaks, and important class updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NotificationToggleRow(
                        title = "Notifications",
                        subtitle = "Master switch for SAFAR alerts.",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = {
                            if (!it) {
                                viewModel.onEvent(SettingsEvent.ToggleNotifications(false))
                            } else {
                                pendingMasterEnable = true
                                requestNotificationPermission()
                            }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
                    NotificationToggleRow(
                        title = "Focus timer updates",
                        subtitle = "Timer running, session complete, and break status.",
                        checked = uiState.focusTimerNotificationsEnabled,
                        enabled = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleFocusTimerNotifications(it)) },
                    )
                    NotificationToggleRow(
                        title = "Daily study reminders",
                        subtitle = "A planned reminder for your Ekagra study block.",
                        checked = uiState.dailyStudyReminderEnabled,
                        enabled = uiState.notificationsEnabled,
                        onCheckedChange = {
                            if (!it) {
                                viewModel.onEvent(SettingsEvent.ToggleDailyStudyReminder(false))
                            } else {
                                pendingDailyEnable = true
                                requestNotificationPermission()
                            }
                        },
                    )
                    SafarCustomTextField(
                        label = "DAILY REMINDER TIME",
                        value = reminderDraft,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() || it == ':' }) {
                                reminderDraft = value
                            }
                            if (isValidReminderTimeInput(value)) {
                                viewModel.onEvent(SettingsEvent.UpdateDailyReminderTime(value))
                            }
                        },
                        enabled = uiState.notificationsEnabled && uiState.dailyStudyReminderEnabled,
                        placeholder = "19:00",
                        errorText = if (reminderDraft.isNotBlank() && !isValidReminderTimeInput(reminderDraft)) {
                            "Use 24-hour time in HH:mm format (e.g., 07:30, 19:00)."
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                NotificationToggleRow(
                    title = "Streak reminders",
                    subtitle = "Evening warning before your streak expires.",
                    checked = uiState.streakReminderEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleStreakReminder(it)) },
                )
                NotificationToggleRow(
                    title = "Course/class updates",
                    subtitle = "New lessons, tests, PDFs, and live class alerts.",
                    checked = uiState.courseUpdatesEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleCourseUpdates(it)) },
                )
                NotificationToggleRow(
                    title = "Achievements",
                    subtitle = "Badges, milestones, and goal completion.",
                    checked = uiState.achievementsEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleAchievements(it)) },
                )
                NotificationToggleRow(
                    title = "Community replies",
                    subtitle = "Mehfil replies, mentions, and teacher responses.",
                    checked = uiState.communityRepliesEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleCommunityReplies(it)) },
                )
                NotificationToggleRow(
                    title = "SAFAR announcements",
                    subtitle = "Admin announcements and major challenges.",
                    checked = uiState.announcementsEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleAnnouncements(it)) },
                )
                NotificationToggleRow(
                    title = "Weekly summary",
                    subtitle = "Progress recap when summaries are enabled.",
                    checked = uiState.weeklySummaryEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleWeeklySummary(it)) },
                )
                Text(
                    "Quiet hours: ${uiState.quietHoursStart} to ${uiState.quietHoursEnd}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            NotificationDebugSettingsEntry()

            if (canAccessAdminComposer) {
                ProfileSectionCard(title = "Admin", icon = Icons.Default.Notifications) {
                    Text(
                        "Internal admin tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenAdminNotificationComposer,
                    ) {
                        Text("Open Admin Notification Composer")
                    }
                }
            }

            ProfileSectionCard(title = "Legal & permissions", icon = Icons.Default.Info) {
                Text(
                    "Simple notes about SAFAR, your choices, and the permissions KAVACH uses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsInfoRow(
                    title = "EULA",
                    subtitle = "Your basic agreement for using SAFAR.",
                    onClick = { activeInfoSheet = SettingsInfoSheet.EULA },
                )
                SettingsInfoRow(
                    title = "Privacy & data",
                    subtitle = "What SAFAR uses, and what it does not read.",
                    onClick = { activeInfoSheet = SettingsInfoSheet.PRIVACY },
                )
                SettingsInfoRow(
                    title = "Why KAVACH needs permissions",
                    subtitle = "A friendly guide to focus blocking.",
                    onClick = { activeInfoSheet = SettingsInfoSheet.KAVACH },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
                SettingsPermissionRow(
                    icon = Icons.Default.Info,
                    title = "App Usage Permission",
                    subtitle = "Helps KAVACH notice when a selected app opens during a focus session.",
                    granted = hasUsageAccess,
                    accent = MaterialTheme.colorScheme.primary,
                    onClickWhenNotGranted = { FocusShieldPermissionHelper.openUsageAccessSettings(context) },
                    onInfoClick = { activeInfoSheet = SettingsInfoSheet.USAGE_ACCESS },
                )
                if (FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()) {
                    SettingsPermissionRow(
                        icon = Icons.Default.Info,
                        title = "KAVACH Block Screen",
                        subtitle = "Shows the block screen for apps you selected during KAVACH sessions.",
                        granted = hasFocusShieldAccessibility,
                        accent = MaterialTheme.colorScheme.primary,
                        onClickWhenNotGranted = { FocusShieldPermissionHelper.openAccessibilitySettings(context) },
                        onInfoClick = { activeInfoSheet = SettingsInfoSheet.ACCESSIBILITY },
                    )
                }
                SettingsPermissionRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Notifications (system)",
                    subtitle = "Shows timer progress, reminders, and KAVACH status.",
                    granted = hasNotificationPermission,
                    accent = MaterialTheme.colorScheme.primary,
                    onClickWhenNotGranted = requestNotificationPermission,
                    onInfoClick = { activeInfoSheet = SettingsInfoSheet.NOTIFICATIONS },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLegalInfoSheet(
    sheet: SettingsInfoSheet,
    onDismiss: () -> Unit,
) {
    val content = when (sheet) {
        SettingsInfoSheet.EULA -> SettingsInfoContent(
            title = "EULA",
            subtitle = "Use SAFAR for learning, focus, and exam preparation.",
            points = listOf(
                "Keep your account details private.",
                "Use study tools fairly and respectfully.",
                "SAFAR may improve features and fix issues over time.",
            ),
        )
        SettingsInfoSheet.PRIVACY -> SettingsInfoContent(
            title = "Privacy & data",
            subtitle = "We keep permission use focused on the feature you choose.",
            points = listOf(
                "KAVACH does not read messages, passwords, photos, or typed text.",
                "Your blocked app choices stay on this device.",
                "Notifications are used for reminders and active session status.",
            ),
        )
        SettingsInfoSheet.KAVACH -> SettingsInfoContent(
            title = "Why KAVACH asks",
            subtitle = "KAVACH needs a few Android permissions to block distractions during focus time.",
            points = listOf(
                "Usage Access helps notice opened apps.",
                "Accessibility shows the block screen for apps you selected.",
                "You can use SAFAR without KAVACH permissions.",
            ),
        )
        SettingsInfoSheet.ACCESSIBILITY -> SettingsInfoContent(
            title = "Accessibility",
            subtitle = "Used only for KAVACH app blocking.",
            points = listOf(
                "It notices when a selected distracting app opens.",
                "It helps show the KAVACH block screen during active sessions.",
                "It does not read private content or control your phone.",
            ),
        )
        SettingsInfoSheet.USAGE_ACCESS -> SettingsInfoContent(
            title = "Usage Access",
            subtitle = "Helps KAVACH understand which app is open.",
            points = listOf(
                "Used during KAVACH setup and active focus sessions.",
                "Helps match opened apps with your blocked app list.",
                "You stay in control of the permission.",
            ),
        )
        SettingsInfoSheet.NOTIFICATIONS -> SettingsInfoContent(
            title = "Notifications",
            subtitle = "Helpful study updates, only when enabled.",
            points = listOf(
                "Timer progress and session complete alerts.",
                "Daily study reminders if you switch them on.",
                "KAVACH status while a focus session is active.",
            ),
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(point, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class SettingsInfoContent(
    val title: String,
    val subtitle: String,
    val points: List<String>,
)

@Composable
private fun SettingsPermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    accent: Color,
    onClickWhenNotGranted: (() -> Unit)?,
    onInfoClick: (() -> Unit)? = null,
) {
    val statusColor = if (granted) accent else MaterialTheme.colorScheme.error
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!granted && onClickWhenNotGranted != null) {
                        Modifier.clickable(onClick = onClickWhenNotGranted)
                    } else {
                        Modifier
                    },
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                Text(
                    if (granted) "Granted" else "Required",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
                if (!granted && onClickWhenNotGranted != null) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = statusColor, modifier = Modifier.size(14.dp))
                }
                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "More info", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
