package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R

/**
 * Permissions Kavach needs from the user. Drives the step list + per-permission guide sheet.
 */
enum class PermissionTarget {
    USAGE_STATS,
    ACCESSIBILITY,
    NOTIFICATIONS,
}

/**
 * Numbered, scannable permission row used in Kavach setup. Replaces the older `PermissionRow` so
 * onboarding feels like a checklist instead of three identical cards. Tap "Allow" to open the
 * per-permission guide sheet (handled by the caller), tap the row to expand a short rationale.
 */
@Composable
fun KavachStepRow(
    stepNumber: Int,
    title: String,
    subtitle: String,
    rationale: String,
    optional: Boolean,
    granted: Boolean,
    accent: Color,
    cardColor: Color,
    borderColor: Color,
    titleColor: Color,
    subtitleColor: Color,
    onAllow: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    val grantedColor = Color(0xFF22C55E)
    val rowAccent = if (granted) grantedColor else accent

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (granted) grantedColor.copy(alpha = 0.45f) else borderColor),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(rowAccent.copy(alpha = if (granted) 0.16f else 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (granted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = grantedColor,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            "$stepNumber",
                            color = rowAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (optional) {
                            Text(
                                "Optional",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = subtitleColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(borderColor.copy(alpha = 0.5f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = subtitleColor,
                        lineHeight = 16.sp,
                    )
                }

                if (granted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(grantedColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = grantedColor,
                            modifier = Modifier.size(13.dp),
                        )
                        Text("Done", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = grantedColor)
                    }
                } else {
                    Button(
                        onClick = onAllow,
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 4.dp,
                        ),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.heightIn(min = 34.dp),
                    ) {
                        Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Why this?",
                    fontSize = 12.sp,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Text(
                    rationale,
                    fontSize = 12.sp,
                    color = subtitleColor,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            }
        }
    }
}

/**
 * Unified permission guide sheet — combines Usage Stats + Accessibility into ONE sheet
 * with numbered steps. No longer shows two separate sheets. User sees everything they
 * need to do in one place, taps "Open Settings" per step, and returns each time.
 *
 * For NOTIFICATIONS: shows a compact single-step sheet (system prompt follows immediately).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGuideSheet(
    permission: PermissionTarget,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val accessibilityRequired = FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        when (permission) {
                            PermissionTarget.USAGE_STATS, PermissionTarget.ACCESSIBILITY -> Icons.Default.Shield
                            PermissionTarget.NOTIFICATIONS -> Icons.Default.Shield
                        },
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        when (permission) {
                            PermissionTarget.USAGE_STATS -> if (accessibilityRequired) stringResource(R.string.kavach_setup_guide_title) else "Allow App Check"
                            PermissionTarget.ACCESSIBILITY -> "Allow Accessibility API"
                            PermissionTarget.NOTIFICATIONS -> "Allow Notifications"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = scheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.kavach_setup_guide_subtitle),
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
            }

            // Steps box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.primary.copy(alpha = 0.06f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (permission) {
                    PermissionTarget.USAGE_STATS -> {
                        // Step 1 — App Check (Usage Stats)
                        GuideStepGroup(
                            stepNumber = 1,
                            title = "App Check",
                            subtitle = "Lets KAVACH see which app you opened",
                            steps = listOf(
                                "Find SAFAR in the list of apps",
                                "Tap Allow usage access",
                                "Press back to return to SAFAR",
                            ),
                            accent = scheme.primary,
                        )

                        // Step 2 — KAVACH Alert (Accessibility) — only if required
                        if (accessibilityRequired) {
                            GuideStepGroup(
                                stepNumber = 2,
                                title = "AccessibilityService API",
                                subtitle = "Required to monitor app launches and display the blocking shield overlay over distracting apps. We do not collect or share your personal data.",
                                steps = listOf(
                                    "Scroll down to find SAFAR KAVACH",
                                    "Tap it and toggle the switch on",
                                    "Confirm and press back to return",
                                ),
                                accent = scheme.primary,
                            )
                        }
                    }
                    PermissionTarget.ACCESSIBILITY -> {
                        GuideStepGroup(
                            stepNumber = 1,
                            title = "AccessibilityService API",
                            subtitle = "Required for KAVACH Focus Shield to detect selected distracting apps and show the block screen.",
                            steps = listOf(
                                "Scroll down to find SAFAR KAVACH",
                                "Tap it and toggle the switch on",
                                "Confirm and press back to return",
                            ),
                            accent = scheme.primary,
                        )
                    }
                    PermissionTarget.NOTIFICATIONS -> {
                        GuideStepGroup(
                            stepNumber = 1,
                            title = "Notifications",
                            subtitle = "For ekagra timer progress and KAVACH status",
                            steps = listOf(
                                "Tap Allow on the prompt that follows",
                                "(This is optional — KAVACH works without it)",
                            ),
                            accent = scheme.primary,
                        )
                    }
                }
            }

            if (permission == PermissionTarget.ACCESSIBILITY) {
                AccessibilityProminentDisclosureCard()
            }

            // Privacy badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.kavach_privacy_badge),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    when (permission) {
                        PermissionTarget.USAGE_STATS -> if (accessibilityRequired) "Open Settings — Step 1" else "Open Settings"
                        PermissionTarget.ACCESSIBILITY -> "I Agree - Open Accessibility Settings"
                        PermissionTarget.NOTIFICATIONS -> "Allow Notifications"
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    if (permission == PermissionTarget.ACCESSIBILITY) "Not Now" else "Maybe later",
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AccessibilityProminentDisclosureCard() {
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant.copy(alpha = 0.55f)),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "How SAFAR uses AccessibilityService",
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            Text(
                text = "SAFAR uses Android AccessibilityService only for KAVACH Focus Shield. When KAVACH is enabled, SAFAR detects the package name of the app you open and compares it with the apps you selected to block.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = "If a selected app opens during an active Ekagra focus timer session, SAFAR shows the KAVACH block screen so you can return to studying.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = "SAFAR does not request the Display over other apps permission. KAVACH shows its own block screen only when a user-selected blocked app is opened during an active Ekagra focus timer session.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = "SAFAR does not read your screen content, messages, passwords, typed text, notifications, contacts, photos, or files. SAFAR does not click buttons, perform gestures, change device settings, prevent uninstall, or control your phone.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = "All processing happens locally on your device. SAFAR does not collect, sell, store, or share Accessibility data for ads, analytics, or third parties. You can keep using SAFAR without KAVACH and can disable this permission anytime in Android Settings.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A grouped step section inside the guide sheet. Shows a label, subtitle, and numbered steps.
 */
@Composable
private fun GuideStepGroup(
    stepNumber: Int,
    title: String,
    subtitle: String,
    steps: List<String>,
    accent: Color,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text("$stepNumber", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = scheme.onSurfaceVariant, lineHeight = 16.sp)
            }
        }
        Column(
            modifier = Modifier.padding(start = 30.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${index + 1}.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(
                        step,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        color = scheme.onSurface,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

/**
 * Lightweight banner shown after the user returns from Android Settings with a freshly granted
 * permission. Replaces a silent state flip so the win feels acknowledged.
 */
@Composable
fun PermissionGrantedBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val granted = Color(0xFF22C55E)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(granted.copy(alpha = 0.14f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = granted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text,
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachPermissionSetup(
    onBack: () -> Unit,
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = sheetState,
        containerColor = scheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Block distracting apps",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "SAFAR uses Android AccessibilityService only for KAVACH Focus Shield. When KAVACH is enabled, SAFAR detects the package name of the app you open and compares it with the apps you selected to block.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "If a selected app opens during an active Ekagra focus timer session, SAFAR shows the KAVACH block screen so you can return to studying.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "SAFAR does not request the Display over other apps permission. KAVACH shows its own block screen only when a user-selected blocked app is opened during an active Ekagra focus timer session.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "SAFAR does not read your screen content, messages, passwords, typed text, notifications, contacts, photos, or files. SAFAR does not click buttons, perform gestures, change device settings, prevent uninstall, or control your phone.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "All processing happens locally on your device. SAFAR does not collect, sell, store, or share Accessibility data for ads, analytics, or third parties. You can keep using SAFAR without KAVACH and can disable this permission anytime in Android Settings.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Compact privacy badge - no long legal paragraph
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.kavach_privacy_badge),
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        sheetState.hide()
                        if (!FocusShieldPermissionHelper.hasUsageStatsPermission(context)) {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            ) {
                Text(
                    text = "I Agree - Continue to Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text(
                    text = "Not Now",
                    color = scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
