package com.safar.app.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.BuildConfig
import com.safar.app.data.local.SafarDataStore
import com.safar.app.notifications.rememberNotificationPermissionRequester
import com.safar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import com.safar.app.ui.profile.NotificationToggleRow
import com.safar.app.ui.profile.ProfileSectionCard

private fun isValidReminderTimeInput(value: String): Boolean {
    if (!Regex("^\\d{2}:\\d{2}$").matches(value)) return false
    val parts = value.split(":")
    if (parts.size != 2) return false
    val hour = parts[0].toIntOrNull() ?: return false
    val minute = parts[1].toIntOrNull() ?: return false
    return hour in 0..23 && minute in 0..59
}

private fun languageDisplay(code: String): String = when (code) {
    "hi" -> "हिन्दी"
    else -> "English"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = false,
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onToggleDarkTheme: () -> Unit,
    dataStore: SafarDataStore,
    onLanguageClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentLanguage by dataStore.language.collectAsStateWithLifecycle(initialValue = "en")
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var pendingMasterEnable by remember { mutableStateOf(false) }
    var pendingDailyEnable by remember { mutableStateOf(false) }
    var reminderDraft by remember(uiState.dailyReminderTime) { mutableStateOf(uiState.dailyReminderTime) }

    var hasFocusShieldAccessibility by remember { mutableStateOf(FocusShieldPermissionHelper.hasAccessibilityService(context)) }
    var hasNotificationPermission by remember { mutableStateOf(FocusShieldPermissionHelper.hasNotificationPermission(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.shadow(8.dp, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                color = MaterialTheme.colorScheme.background,
            ) {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("Settings", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                            Text("Appearance, notifications, and permissions.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
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
            }
        },
    ) { padding ->
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
                    "Use the sun/moon icon above to switch light and dark theme.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Column {
                            Text("Language", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(languageDisplay(currentLanguage), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = onLanguageClick) {
                        Text("Change")
                    }
                }
                Text(
                    "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ProfileSectionCard(title = "Notifications", icon = Icons.Default.Notifications) {
                Text(
                    "Helpful alerts for focus sessions, streaks, and important class updates.",
                    fontSize = 12.sp,
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
                OutlinedTextField(
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
                    label = { Text("Daily reminder time") },
                    placeholder = { Text("19:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        if (reminderDraft.isNotBlank() && !isValidReminderTimeInput(reminderDraft)) {
                            Text("Use 24-hour time in HH:mm format (e.g., 07:30, 19:00).")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    ),
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
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ProfileSectionCard(title = "Permissions overview", icon = Icons.Default.Info) {
                Text(
                    "SAFAR uses these only when you allow them. For Kavach setup (blocked apps, beast mode), open Ekagra → Kavach.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsPermissionRow(
                    icon = Icons.Default.Info,
                    title = "Ekagra Mode Accessibility",
                    subtitle = "Used only to detect opened blocked apps while a focus timer or Study Session runs. You can also enable this from Ekagra → Kavach.",
                    granted = hasFocusShieldAccessibility,
                    accent = MaterialTheme.colorScheme.primary,
                    onClickWhenNotGranted = { FocusShieldPermissionHelper.openAccessibilitySettings(context) },
                )
                SettingsPermissionRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Notifications (system)",
                    subtitle = "Required for app alerts.",
                    granted = hasNotificationPermission,
                    accent = MaterialTheme.colorScheme.primary,
                    onClickWhenNotGranted = requestNotificationPermission,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsPermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    accent: Color,
    onClickWhenNotGranted: (() -> Unit)?,
) {
    val statusColor = if (granted) accent else MaterialTheme.colorScheme.error
    Card(
        shape = RoundedCornerShape(14.dp),
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
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                Text(
                    if (granted) "Granted" else "Required",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
                if (!granted && onClickWhenNotGranted != null) {
                    Icon(Icons.Default.OpenInNew, null, tint = statusColor, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
