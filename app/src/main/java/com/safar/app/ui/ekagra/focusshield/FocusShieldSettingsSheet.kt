package com.safar.app.ui.ekagra.focusshield

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.alpha
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

/**
 * Focus Shield settings content that renders inside EkagraScreen's SHIELD tab.
 */
@Composable
fun FocusShieldSettingsContent(
    state: FocusShieldUiState,
    accent: Color,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleStrictMode: (Boolean) -> Unit,
    onToggleEmergencyUnlock: (Boolean) -> Unit,
    onOpenAppPicker: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Re-check permissions when screen resumes (user may have just granted them)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasUsage by remember { mutableStateOf(state.hasUsageAccess) }
    var hasAccessibility by remember { mutableStateOf(state.hasAccessibility) }
    var hasOverlay by remember { mutableStateOf(state.hasOverlayPermission) }
    var hasNotifications by remember { mutableStateOf(state.hasNotifications) }
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = scheme.background
    val card = if (isDark) Color(0xFF1A1C1F) else Color(0xFFF8FAFC)
    val border = if (isDark) Color(0xFF2D3139) else Color(0xFFD6DEE8)
    val textPrimary = scheme.onBackground
    val textSecondary = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)
    val cardIconBg = if (isDark) Color(0xFF23262C) else Color(0xFFEFF3F8)
    val infoCard = if (isDark) Color(0xFF1D2025) else Color(0xFFF1F5F9)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsage = UsageAccessHelper.hasUsageAccess(context)
                hasAccessibility = UsageAccessHelper.isAccessibilityServiceEnabled(context)
                hasOverlay = UsageAccessHelper.canDrawOverlays(context)
                hasNotifications = UsageAccessHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header
        Text("Focus Shield", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(
            "Block distracting apps during your focus sessions",
            style = MaterialTheme.typography.bodySmall, color = textSecondary,
        )

        // ── Master toggle ────────────────────────────────────────────────────
        ShieldToggleCard(
            icon = Icons.Default.Shield,
            title = "Enable Focus Shield",
            subtitle = if (state.isEnabled) "Apps will be blocked during focus sessions" else "Turn on to block distracting apps",
            checked = state.isEnabled,
            accent = accent,
            cardColor = card,
            borderColor = border,
            iconBgColor = cardIconBg,
            iconTint = Color(0xFF60A5FA),
            titleColor = textPrimary,
            subtitleColor = textSecondary,
            onCheckedChange = onToggleEnabled,
        )

        if (state.isEnabled) {
            // ── App picker ───────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = card),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(border)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAppPicker)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardIconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.AppBlocking, null, tint = Color(0xFF60A5FA), modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Blocked Apps", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textPrimary)
                        val count = state.blockedPackages.size
                        Text(
                            if (count == 0) "Tap to choose apps" else "$count app${if (count != 1) "s" else ""} selected",
                            fontSize = 12.sp,
                            color = textSecondary,
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // ── Mode toggles ─────────────────────────────────────────────────
            ShieldToggleCard(
                icon = Icons.Default.GppMaybe,
                title = "Strict Mode",
                subtitle = "Cannot disable blocking until timer ends",
                checked = state.isStrictMode,
                accent = accent,
                cardColor = card,
                borderColor = border,
                iconBgColor = cardIconBg,
                iconTint = Color(0xFF60A5FA),
                titleColor = textPrimary,
                subtitleColor = textSecondary,
                contentAlpha = 0.7f,
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
                iconBgColor = cardIconBg,
                iconTint = Color(0xFF60A5FA),
                titleColor = textPrimary,
                subtitleColor = textSecondary,
                onCheckedChange = onToggleEmergencyUnlock,
            )

            // ── Permissions section ──────────────────────────────────────────
            Text(
                "PERMISSIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textSecondary,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp),
            )

            PermissionRow(
                icon = Icons.Default.QueryStats,
                title = "Usage Access",
                granted = hasUsage,
                accent = accent,
                cardColor = card,
                borderColor = border,
                titleColor = textPrimary,
                iconColor = textSecondary,
                onClick = onOpenUsageAccess,
            )

            PermissionRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                granted = hasAccessibility,
                accent = accent,
                cardColor = card,
                borderColor = border,
                titleColor = textPrimary,
                iconColor = textSecondary,
                onClick = onOpenAccessibility,
            )

            PermissionRow(
                icon = Icons.Default.Layers,
                title = "Display Over Other Apps",
                granted = hasOverlay,
                accent = accent,
                cardColor = card,
                borderColor = border,
                titleColor = textPrimary,
                iconColor = textSecondary,
                onClick = onOpenOverlaySettings,
            )

            PermissionRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                granted = hasNotifications,
                accent = accent,
                cardColor = card,
                borderColor = border,
                titleColor = textPrimary,
                iconColor = textSecondary,
                onClick = null, // Already granted or handled via system
            )

            // ── Info card ────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = infoCard),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(border.copy(alpha = 0.7f))),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Focus Shield only blocks apps during active focus sessions. " +
                                "It does not collect any data about your app usage.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        lineHeight = 17.sp,
                    )
                }
            }
            Spacer(Modifier.height(84.dp))
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
    contentAlpha: Float = 1f,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
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
    granted: Boolean,
    accent: Color,
    cardColor: Color,
    borderColor: Color,
    titleColor: Color,
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
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor, modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    if (granted) "Granted" else "Required",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                if (!granted && onClick != null) {
                    Icon(
                        Icons.Default.OpenInNew, null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
