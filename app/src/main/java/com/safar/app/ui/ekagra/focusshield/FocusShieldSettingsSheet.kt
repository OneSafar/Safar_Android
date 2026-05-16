package com.safar.app.ui.ekagra.focusshield

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.safar.app.notifications.rememberNotificationPermissionRequester

/**
 * Focus Shield settings content rendered inside EkagraScreen's SHIELD tab.
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAccessibilityService by remember { mutableStateOf(state.hasAccessibilityService) }
    var hasNotifications by remember { mutableStateOf(state.hasNotifications) }
    var hasUsageStats by remember { mutableStateOf(state.hasUsageStats) }
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val card = if (isDark) Color(0xFF1A1C1F) else Color(0xFFF8FAFC)
    val border = if (isDark) Color(0xFF2D3139) else Color(0xFFD6DEE8)
    val textPrimary = scheme.onBackground
    val textSecondary = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val iconBg = if (isDark) Color(0xFF23262C) else Color(0xFFEFF3F8)
    var showEnableDisclosure by remember { mutableStateOf(false) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    var showUsageDisclosure by remember { mutableStateOf(false) }
    var pendingEnableAfterUsage by remember { mutableStateOf(false) }
    var pendingEnableAfterAccessibility by remember { mutableStateOf(false) }
    var showNotificationDisclosure by remember { mutableStateOf(false) }
    val requestNotificationPermission = rememberNotificationPermissionRequester {
        hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner, state.hasAccessibilityService, state.hasNotifications, state.hasUsageStats) {
        hasAccessibilityService = state.hasAccessibilityService
        hasNotifications = state.hasNotifications
        hasUsageStats = state.hasUsageStats
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibilityService = FocusShieldPermissionHelper.hasAccessibilityService(context)
                hasNotifications = FocusShieldPermissionHelper.hasNotificationPermission(context)
                hasUsageStats = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsageStats, hasAccessibilityService, pendingEnableAfterUsage, pendingEnableAfterAccessibility) {
        if (pendingEnableAfterUsage && hasUsageStats) {
            pendingEnableAfterUsage = false
            if (!hasAccessibilityService) {
                showAccessibilityDisclosure = true
            } else {
                onToggleEnabled(true)
            }
        }
        if (pendingEnableAfterAccessibility && hasAccessibilityService) {
            pendingEnableAfterAccessibility = false
            if (hasUsageStats) onToggleEnabled(true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column {
            Text("Kavach", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
            Text("Your shield against distractions", style = MaterialTheme.typography.labelMedium, color = textSecondary)
        }
        Text(
            "Block only the distracting apps you choose, only while a focus timer is running.",
            style = MaterialTheme.typography.bodySmall,
            color = textSecondary,
        )

        DisclosureCard(
            cardColor = if (isDark) Color(0xFF1D2025) else Color(0xFFF1F5F9),
            borderColor = border,
            textColor = textSecondary,
        )

        PermissionTransparencyCard(
            cardColor = card,
            borderColor = border,
            titleColor = textPrimary,
            bodyColor = textSecondary,
        )

        ShieldToggleCard(
            icon = Icons.Default.Shield,
            title = "Enable Kavach",
            subtitle = if (state.isEnabled) "Activates automatically when your study timer starts" else "Off by default until you enable it",
            checked = state.isEnabled,
            accent = accent,
            cardColor = card,
            borderColor = border,
            iconBgColor = iconBg,
            iconTint = Color(0xFF60A5FA),
            titleColor = textPrimary,
            subtitleColor = textSecondary,
            onCheckedChange = { enabled ->
                if (enabled) showEnableDisclosure = true else onToggleEnabled(false)
            },
        )

        PermissionRow(
            icon = Icons.Default.BarChart,
            title = "App Usage Permission",
            subtitle = "Lets SAFAR detect which app you're using, so it can block distractions during study time",
            granted = hasUsageStats,
            accent = accent,
            cardColor = card,
            borderColor = border,
            titleColor = textPrimary,
            subtitleColor = textSecondary,
            iconColor = textSecondary,
            onClick = { showUsageDisclosure = true },
        )

        PermissionRow(
            icon = Icons.Default.Info,
            title = "Ekagra Mode Accessibility",
            subtitle = "Helps SAFAR detect and block a distracting app the moment you open it",
            granted = hasAccessibilityService,
            accent = accent,
            cardColor = card,
            borderColor = border,
            titleColor = textPrimary,
            subtitleColor = textSecondary,
            iconColor = textSecondary,
            onClick = { showAccessibilityDisclosure = true },
        )

        PermissionRow(
            icon = Icons.Default.CheckCircle,
            title = "Notifications",
            subtitle = "Get reminders and updates about your study session. You can skip this, and still use the timer.",
            granted = hasNotifications,
            accent = accent,
            cardColor = card,
            borderColor = border,
            titleColor = textPrimary,
            subtitleColor = textSecondary,
            iconColor = textSecondary,
            onClick = { showNotificationDisclosure = true },
        )

        if (state.isEnabled) {
            AppPickerCard(
                count = state.blockedPackages.size,
                cardColor = card,
                borderColor = border,
                iconBgColor = iconBg,
                titleColor = textPrimary,
                subtitleColor = textSecondary,
                onClick = onOpenAppPicker,
            )

            ShieldToggleCard(
                icon = Icons.Default.GppMaybe,
                title = "Beast Mode",
                subtitle = "Keep blocking until the current timer ends",
                checked = state.isStrictMode,
                accent = accent,
                cardColor = card,
                borderColor = border,
                iconBgColor = iconBg,
                iconTint = Color(0xFF60A5FA),
                titleColor = textPrimary,
                subtitleColor = textSecondary,
                onCheckedChange = onToggleStrictMode,
            )

            ShieldToggleCard(
                icon = Icons.Default.LockOpen,
                title = "Emergency Unlock",
                subtitle = "Allow one-time unlock for urgent situations",
                checked = state.allowEmergencyUnlock,
                accent = accent,
                cardColor = card,
                borderColor = border,
                iconBgColor = iconBg,
                iconTint = Color(0xFF60A5FA),
                titleColor = textPrimary,
                subtitleColor = textSecondary,
                onCheckedChange = onToggleEmergencyUnlock,
            )

            Spacer(Modifier.height(84.dp))
        }
    }

    if (showEnableDisclosure) {
        FocusShieldConsentDialog(
            title = "Enable Kavach?",
            body = "Kavach is fully optional. SAFAR will not access App Usage Permission or Accessibility unless you choose to grant those permissions in Android settings.\n\nIf you enable it and grant permissions, SAFAR uses Accessibility only while a focus timer or Study Session is running to detect whether an app you selected has opened. If a selected app opens, SAFAR shows its own block screen and a notification so you can return to your focus session.\n\nSAFAR does not read messages, passwords, typed text, contacts, photos, or screen content. It does not click buttons, change settings, prevent uninstall, or control your device. Your blocked app choices stay on this device. You can turn Kavach off here at any time.",
            confirmText = "I Agree",
            onDismiss = { showEnableDisclosure = false },
            onConfirm = {
                showEnableDisclosure = false
                when {
                    !hasUsageStats -> {
                        pendingEnableAfterUsage = true
                        showUsageDisclosure = true
                    }
                    !hasAccessibilityService -> {
                        pendingEnableAfterAccessibility = true
                        showAccessibilityDisclosure = true
                    }
                    else -> onToggleEnabled(true)
                }
            },
        )
    }

    if (showUsageDisclosure) {
        FocusShieldConsentDialog(
            title = "App Usage Permission",
            body = "This opens Android's App Usage Permission settings for SAFAR. SAFAR asks for this only for Kavach setup checks and only uses the permission when you have enabled Kavach yourself.\n\nYou are in control. If you do not grant App Usage Permission, Kavach will stay off and the timer will still work.",
            confirmText = "Open settings",
            onDismiss = {
                showUsageDisclosure = false
                pendingEnableAfterUsage = false
            },
            onConfirm = {
                showUsageDisclosure = false
                FocusShieldPermissionHelper.openUsageAccessSettings(context)
            },
        )
    }

    if (showAccessibilityDisclosure) {
        FocusShieldConsentDialog(
            title = "Accessibility Required",
            body = "This opens Android Accessibility settings for the service named “SAFAR Ekagra Mode Accessibility”. Grant it only if you want SAFAR to block selected apps during focus timers and Study Sessions.\n\nWhat SAFAR uses it for: detecting opened app package names while a focus timer or Study Session is running.\n\nWhat SAFAR does not do: read messages, passwords, typed text, contacts, photos, or screen content; click buttons; change settings; prevent uninstall; or control your device.\n\nIf you do not grant this service, Kavach stays off and the timer still works.",
            confirmText = "Open Settings",
            onDismiss = {
                showAccessibilityDisclosure = false
                pendingEnableAfterAccessibility = false
            },
            onConfirm = {
                showAccessibilityDisclosure = false
                onOpenAccessibilitySettings()
            },
        )
    }

    if (showNotificationDisclosure) {
        FocusShieldConsentDialog(
            title = "Notifications",
            body = "SAFAR can ask Android for notification permission so the focus timer and Kavach status are visible while running. This is optional and user-controlled.\n\nIf you deny notifications, Kavach still requires App Usage Permission and Accessibility before it can block apps, and the timer still works.",
            confirmText = "Ask Android",
            onDismiss = { showNotificationDisclosure = false },
            onConfirm = {
                showNotificationDisclosure = false
                requestNotificationPermission()
            },
        )
    }
}

@Composable
private fun DisclosureCard(
    cardColor: Color,
    borderColor: Color,
    textColor: Color,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor.copy(alpha = 0.7f))),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
            Text(
                "Kavach is optional. It uses Android Accessibility only during active focus timers or Study Sessions to identify opened app package names and compare them with apps you selected to block. It does not use Display over other apps, and it does not read SMS, messages, passwords, contacts, typed text, or screen content. Your selected blocked apps stay on this device.",
                fontSize = 12.sp,
                color = textColor,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun PermissionTransparencyCard(
    cardColor: Color,
    borderColor: Color,
    titleColor: Color,
    bodyColor: Color,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Permissions are your choice", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = titleColor)
            Text(
                "SAFAR will ask for a few permissions to help you block distracting apps while you study. You decide what to allow.",
                fontSize = 12.sp,
                color = bodyColor,
                lineHeight = 17.sp,
            )
            Text("Required for blocking: App Usage Permission and SAFAR Ekagra Mode Accessibility.", fontSize = 12.sp, color = bodyColor, lineHeight = 17.sp)
            Text("Optional: Notifications for timer and shield status.", fontSize = 12.sp, color = bodyColor, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AppPickerCard(
    count: Int,
    cardColor: Color,
    borderColor: Color,
    iconBgColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBgColor), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AppBlocking, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Blocked Apps", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = titleColor)
                Text(
                    if (count == 0) "Tap to choose apps" else "$count app${if (count != 1) "s" else ""} selected",
                    fontSize = 12.sp,
                    color = subtitleColor,
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = subtitleColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ShieldToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    cardColor: Color,
    borderColor: Color,
    iconBgColor: Color,
    iconTint: Color,
    titleColor: Color,
    subtitleColor: Color,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(iconBgColor), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = titleColor)
                Text(subtitle, fontSize = 12.sp, color = subtitleColor)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF4B5563),
                ),
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    accent: Color,
    cardColor: Color,
    borderColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    iconColor: Color,
    onClick: (() -> Unit)?,
) {
    val statusColor by animateColorAsState(
        targetValue = if (granted) accent else Color(0xFFEF4444),
        label = "permColor",
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(borderColor)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null && !granted) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
                Text(subtitle, fontSize = 12.sp, color = subtitleColor, lineHeight = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                Text(
                    if (granted) "Granted" else "Required",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                )
                if (!granted && onClick != null) {
                    Icon(Icons.Default.OpenInNew, null, tint = statusColor, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
