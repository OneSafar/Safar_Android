package com.safar.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.safar.app.notifications.rememberNotificationPermissionRequester
import com.safar.app.ui.theme.Green500

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment

private val examOptions = listOf("UPSC", "SSC", "IBPS", "RRB", "NEET", "JEE", "12th Boards", "State PSC", "CAT", "GATE", "Other")
private val stageOptions = listOf("Beginner", "Intermediate", "Advanced", "Revision", "Mock Tests")
private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isDarkTheme: Boolean = false,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onHome: () -> Unit = {},
    onToggleDarkTheme: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.shadow(8.dp, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                color = MaterialTheme.colorScheme.background
            ) {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("Profile Settings", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.5).sp)
                            Text("Manage your sanctuary preferences.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary) }
                    },
                    actions = {
                        IconButton(onClick = onHome) { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = onToggleDarkTheme) { Icon(if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.Nightlight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
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
            // Avatar / name card with subtle glow
            Box {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            // Avatar Aura
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .blur(12.dp, BlurredEdgeTreatment.Unbounded)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        )
                                    )
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(uiState.userName.firstOrNull()?.uppercase() ?: "U", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Surface(
                                modifier = Modifier.size(22.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(uiState.userName.ifEmpty { "User" }, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = (-0.5).sp)
                            Text(uiState.userEmail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Surface(
                                color = Green500.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Box(Modifier.size(6.dp).clip(CircleShape).background(Green500))
                                    Text("ONLINE", fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = Green500)
                                }
                            }
                        }
                    }
                }
            }

            // Personal Information
            ProfileSectionCard(title = "Personal Information", icon = Icons.Default.Person) {
                ProfileFieldLabel("FULL NAME")
                OutlinedTextField(
                    value = uiState.editName,
                    onValueChange = { viewModel.onEvent(ProfileEvent.UpdateName(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )

                Spacer(Modifier.height(8.dp))
                ProfileFieldLabel("EMAIL ADDRESS")
                OutlinedTextField(
                    value = uiState.userEmail,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Text("Contact support to update your primary email address.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.padding(start = 4.dp))

                Spacer(Modifier.height(8.dp))
                ProfileFieldLabel("GENDER")
                DropdownSelector(
                    selected = uiState.editGender.ifEmpty { "Select gender" },
                    options = genderOptions,
                    onSelect = { viewModel.onEvent(ProfileEvent.UpdateGender(it)) },
                )
            }

            // Exam Focus
            ProfileSectionCard(title = "Exam Focus", icon = Icons.Default.School) {
                ProfileFieldLabel("TARGET EXAM")
                DropdownSelector(
                    selected = uiState.editExamType.ifEmpty { "Select exam" },
                    options = examOptions,
                    onSelect = { viewModel.onEvent(ProfileEvent.UpdateExamType(it)) },
                )

                Spacer(Modifier.height(14.dp))
                ProfileFieldLabel("PREPARATION STAGE")
                DropdownSelector(
                    selected = uiState.editStage.ifEmpty { "Select stage" },
                    options = stageOptions,
                    onSelect = { viewModel.onEvent(ProfileEvent.UpdateStage(it)) },
                )
            }

            // Account Status
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), Color.Transparent)
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Account Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Your account is verified and secured.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "VERIFIED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
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
                        if (it) requestNotificationPermission()
                        viewModel.onEvent(ProfileEvent.ToggleNotifications(it))
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
                NotificationToggleRow(
                    title = "Focus timer updates",
                    subtitle = "Timer running, session complete, and break status.",
                    checked = uiState.focusTimerNotificationsEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleFocusTimerNotifications(it)) },
                )
                NotificationToggleRow(
                    title = "Daily study reminders",
                    subtitle = "A planned reminder for your Ekagra study block.",
                    checked = uiState.dailyStudyReminderEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = {
                        if (it) requestNotificationPermission()
                        viewModel.onEvent(ProfileEvent.ToggleDailyStudyReminder(it))
                    },
                )
                OutlinedTextField(
                    value = uiState.dailyReminderTime,
                    onValueChange = { value ->
                        if (value.length <= 5 && value.all { it.isDigit() || it == ':' }) {
                            viewModel.onEvent(ProfileEvent.UpdateDailyReminderTime(value))
                        }
                    },
                    enabled = uiState.notificationsEnabled && uiState.dailyStudyReminderEnabled,
                    label = { Text("Reminder time") },
                    placeholder = { Text("19:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                NotificationToggleRow(
                    title = "Streak reminders",
                    subtitle = "Evening warning before your streak expires.",
                    checked = uiState.streakReminderEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleStreakReminder(it)) },
                )
                NotificationToggleRow(
                    title = "Course/class updates",
                    subtitle = "New lessons, tests, PDFs, and live class alerts.",
                    checked = uiState.courseUpdatesEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleCourseUpdates(it)) },
                )
                NotificationToggleRow(
                    title = "Achievements",
                    subtitle = "Badges, milestones, and goal completion.",
                    checked = uiState.achievementsEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleAchievements(it)) },
                )
                NotificationToggleRow(
                    title = "Community replies",
                    subtitle = "Mehfil replies, mentions, and teacher responses.",
                    checked = uiState.communityRepliesEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleCommunityReplies(it)) },
                )
                NotificationToggleRow(
                    title = "SAFAR announcements",
                    subtitle = "Admin announcements and major challenges.",
                    checked = uiState.announcementsEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleAnnouncements(it)) },
                )
                NotificationToggleRow(
                    title = "Weekly summary",
                    subtitle = "Progress recap when summaries are enabled.",
                    checked = uiState.weeklySummaryEnabled,
                    enabled = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.onEvent(ProfileEvent.ToggleWeeklySummary(it)) },
                )
                Text(
                    "Quiet hours: ${uiState.quietHoursStart} to ${uiState.quietHoursEnd}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.onEvent(ProfileEvent.ShowLogoutDialog) },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(0.dp) // Important for gradient background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    if (uiState.isSaving) listOf(Color.Gray, Color.Gray)
                                    else listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "For any technical or app related queries mail at\nonesafar@gmail.com • safarparmar0@gmail.com",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text("© 2026 SAFAR • Version 1.0.4", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (uiState.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) },
            icon = { Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout? You will need to sign in again to access your sanctuary.") },
            confirmButton = { Button(onClick = { viewModel.logout { onLogout() } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Logout", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { viewModel.onEvent(ProfileEvent.DismissLogoutDialog) }) { Text("Stay Here", fontWeight = FontWeight.SemiBold) } },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ProfileSectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.12f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = (-0.3).sp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.06f), thickness = 1.dp)
            content()
        }
    }
}

@Composable
private fun ProfileFieldLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSelector(selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontWeight = FontWeight.Medium) },
                    onClick = { onSelect(opt); expanded = false },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
