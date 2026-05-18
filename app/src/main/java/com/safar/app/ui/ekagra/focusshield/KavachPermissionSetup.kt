package com.safar.app.ui.ekagra.focusshield

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                        Text("Granted", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = grantedColor)
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
                    "Why this permission?",
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
 * Modal bottom sheet shown right before deep-linking the user into Android Settings. Mimics
 * Regain's "what to tap next" coaching: mascot, numbered steps, social proof, primary CTA.
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

    val content = remember(permission) { PermissionGuideContent.forTarget(permission) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        content.icon,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        content.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = scheme.onSurface,
                    )
                    Text(
                        content.subtitle,
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant,
                        lineHeight = 17.sp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.primary.copy(alpha = 0.06f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "On the next screen:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurfaceVariant,
                )
                content.steps.forEachIndexed { index, step ->
                    GuideStepRow(number = index + 1, text = step, accent = scheme.primary)
                }
            }

            Text(
                content.reassurance,
                fontSize = 12.sp,
                color = scheme.onSurfaceVariant,
                lineHeight = 17.sp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Made for focused study sessions. You choose what Kavach blocks.",
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
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
                Text("Open Settings", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Cancel", color = scheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GuideStepRow(number: Int, text: String, accent: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
            color = granted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class PermissionGuideContent(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val steps: List<String>,
    val reassurance: String,
) {
    companion object {
        fun forTarget(target: PermissionTarget): PermissionGuideContent = when (target) {
            PermissionTarget.USAGE_STATS -> PermissionGuideContent(
                icon = Icons.Default.BarChart,
                title = "App Usage Permission",
                subtitle = "Helps Kavach notice opened apps during focus sessions.",
                steps = listOf(
                    "Find SAFAR in the list of apps.",
                    "Tap Allow usage access (or Permit usage access).",
                    "Press back to return to SAFAR.",
                ),
                reassurance = "SAFAR does not read what is inside any app. The open app name is enough for Kavach to match your blocked list.",
            )
            PermissionTarget.ACCESSIBILITY -> PermissionGuideContent(
                icon = Icons.Default.Info,
                title = "Ekagra Mode Accessibility",
                subtitle = "Shows the block screen when a chosen app opens.",
                steps = listOf(
                    "Scroll to Downloaded apps (or Installed apps).",
                    "Tap SAFAR Ekagra Mode.",
                    "Toggle the switch on and confirm.",
                ),
                reassurance = "SAFAR does not read messages, passwords, typed text, photos, or screen content. It only helps Kavach react to apps you selected.",
            )
            PermissionTarget.NOTIFICATIONS -> PermissionGuideContent(
                icon = Icons.Default.NotificationsActive,
                title = "Notifications",
                subtitle = "Used for focus timer progress and active Kavach status.",
                steps = listOf(
                    "Tap Allow on the system prompt that follows.",
                    "(If no prompt appears, toggle Notifications on for SAFAR.)",
                ),
                reassurance = "Notifications are optional. Kavach and study timers still work without them.",
            )
        }
    }
}
