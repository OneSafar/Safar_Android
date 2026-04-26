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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Text("Focus Shield", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Block distracting apps during your focus sessions",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Master toggle ────────────────────────────────────────────────────
        ShieldToggleCard(
            icon = Icons.Default.Shield,
            title = "Enable Focus Shield",
            subtitle = if (state.isEnabled) "Apps will be blocked during focus sessions" else "Turn on to block distracting apps",
            checked = state.isEnabled,
            accent = accent,
            onCheckedChange = onToggleEnabled,
        )

        if (state.isEnabled) {
            // ── App picker ───────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                border = CardDefaults.outlinedCardBorder(),
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
                            .background(accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.AppBlocking, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Blocked Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val count = state.blockedPackages.size
                        Text(
                            if (count == 0) "Tap to choose apps" else "$count app${if (count != 1) "s" else ""} selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                onCheckedChange = onToggleStrictMode,
            )

            ShieldToggleCard(
                icon = Icons.Default.LockOpen,
                title = "Emergency Unlock",
                subtitle = "Allow one-time unlock for urgent situations",
                checked = state.allowEmergencyUnlock,
                accent = accent,
                onCheckedChange = onToggleEmergencyUnlock,
            )

            // ── Permissions section ──────────────────────────────────────────
            Text(
                "PERMISSIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            PermissionRow(
                icon = Icons.Default.QueryStats,
                title = "Usage Access",
                granted = hasUsage,
                accent = accent,
                onClick = onOpenUsageAccess,
            )

            PermissionRow(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                granted = hasAccessibility,
                accent = accent,
                onClick = onOpenAccessibility,
            )

            PermissionRow(
                icon = Icons.Default.Layers,
                title = "Display Over Other Apps",
                granted = hasOverlay,
                accent = accent,
                onClick = onOpenOverlaySettings,
            )

            PermissionRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                granted = hasNotifications,
                accent = accent,
                onClick = null, // Already granted or handled via system
            )

            // ── Info card ────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accent.copy(alpha = 0.06f),
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Focus Shield only blocks apps during active focus sessions. " +
                                "It does not collect any data about your app usage.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
            }
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
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
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
    onClick: (() -> Unit)?,
) {
    val statusColor by animateColorAsState(
        targetValue = if (granted) Color(0xFF43A047) else Color(0xFFE53935),
        label = "permColor",
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder(),
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
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f))
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
                    color = statusColor,
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
