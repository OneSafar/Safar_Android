package com.safar.app.ui.ekagra.focusshield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.R

private val KavachIconTint @Composable get() = if (KavachDesign.isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
private val KavachTitleColor @Composable get() = KavachDesign.TextMain
private val KavachSubtitleColor @Composable get() = KavachDesign.TextMuted
private val KavachRowDivider @Composable get() = KavachDesign.Surface
private val KavachStatusBg @Composable get() = KavachDesign.SuccessBg
private val KavachStatusText @Composable get() = KavachDesign.SuccessText

@Composable
fun KavachCompactIntro(
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(KavachDesign.HubHeroBackground)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KavachDesign.HubHeroIconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = KavachDesign.Primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                text = "Study Without Distractions",
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = KavachDesign.HubText,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Kavach blocks the apps you choose while you study.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = KavachDesign.HubTextMuted,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLearnMore)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = KavachDesign.Primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Learn More",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KavachDesign.Primary,
                )
            }
        }
    }
}

@Composable
fun KavachIntroHeroCard(
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = scheme.primaryContainer.copy(alpha = 0.72f),
            contentColor = scheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(142.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.outline.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                    .padding(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(scheme.surfaceVariant.copy(alpha = 0.72f))
                        .align(Alignment.TopCenter),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KavachBlockedAppGlyph(Icons.AutoMirrored.Filled.Chat)
                    KavachBlockedAppGlyph(Icons.Default.Block)
                    KavachBlockedAppGlyph(Icons.Default.Apps)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = "Kavach Is On",
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onPrimaryContainer,
                )
                Text(
                    text = "Your selected apps stay paused during study time.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun KavachBlockedAppGlyph(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
fun KavachIntroCopy(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Study Without Distractions",
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Choose the apps that waste your time. Kavach will help block them while you study.",
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun KavachBenefitCards(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KavachFeatureCard(
            icon = Icons.Default.Apps,
            title = "Choose Apps",
            body = "Pick the apps that distract you.",
        )
        KavachFeatureCard(
            icon = Icons.Default.Timer,
            title = "Start Studying",
            body = "Run your focus timer and Kavach will help.",
        )
        KavachFeatureCard(
            icon = Icons.Default.Tune,
            title = "You Control It",
            body = "Change apps or turn Kavach off anytime.",
        )
    }
}

@Composable
private fun KavachFeatureCard(
    icon: ImageVector,
    title: String,
    body: String,
) {
    val scheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
                Text(
                    text = body,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun KavachPermissionDisclosureCard(
    hasUsageStats: Boolean,
    hasAccessibilityService: Boolean,
    hasNotifications: Boolean,
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val requiredReady = hasUsageStats && hasAccessibilityService

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = KavachDesign.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = KavachDesign.Primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Small Setup Needed",
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                )
            }
            Text(
                text = "Android asks for permission before Kavach can block apps.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = scheme.onSurface,
            )
            Text(
                text = "SAFAR only uses this for Kavach. We do not read chats, passwords, photos, or private content.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = scheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KavachPermissionStatusRow(
                    title = "App Check",
                    body = "Checks when a selected app opens.",
                    granted = hasUsageStats,
                    required = true,
                    onClick = onOpenUsageAccess,
                )
                KavachPermissionStatusRow(
                    title = "Block Screen",
                    body = "Shows Kavach when a blocked app opens.",
                    granted = hasAccessibilityService,
                    required = true,
                    onClick = onOpenAccessibility,
                )
                KavachPermissionStatusRow(
                    title = "Notifications",
                    body = "Shows study timer updates.",
                    granted = hasNotifications,
                    required = false,
                    onClick = onOpenNotifications,
                )
            }
            Text(
                text = if (requiredReady) "Kavach is ready to block apps." else "Allow the two required permissions to use Kavach.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (requiredReady) KavachStatusText else scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KavachPermissionStatusRow(
    title: String,
    body: String,
    granted: Boolean,
    required: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = KavachDesign.TextMain,
            )
            Text(
                text = body,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = KavachDesign.TextMuted,
            )
        }
        Text(
            text = when {
                granted -> "Ready"
                required -> "Needed"
                else -> "Optional"
            },
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (granted) KavachDesign.SuccessBg else KavachDesign.SurfaceHighlight,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (granted) KavachDesign.SuccessText else KavachDesign.TextMuted,
        )
    }
}

@Composable
fun KavachEnabledSummaryCard(
    enabled: Boolean,
    blockedAppCount: Int,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val appsLabel = if (blockedAppCount == 1) "1 app selected" else "$blockedAppCount apps selected"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KavachDesign.HubBorder),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KavachDesign.HubGreenIconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = KavachDesign.HubGreenIcon,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Kavach is on",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = KavachDesign.HubText,
                )
                Text(
                    text = appsLabel,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = KavachDesign.HubTextMuted,
                )
            }
            KavachStitchPillToggle(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
fun KavachBottomActions(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String = "Maybe Later",
    primaryEnabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KavachDesign.Background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = KavachDesign.HubBorder, thickness = 1.dp)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        KavachStitchPrimaryButton(
            text = primaryLabel,
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
        )
        TextButton(
            onClick = onSecondaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = secondaryLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = KavachDesign.HubTextMuted,
            )
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachLearnMoreSheet(
    hasUsageStats: Boolean,
    hasAccessibilityService: Boolean,
    hasNotifications: Boolean,
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenNotifications: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KavachDesign.Background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(KavachDesign.Background)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            KavachIntroHeroCard()
            KavachIntroCopy()
            KavachBenefitCards()
            KavachPermissionDisclosureCard(
                hasUsageStats = hasUsageStats,
                hasAccessibilityService = hasAccessibilityService,
                hasNotifications = hasNotifications,
                onOpenUsageAccess = {
                    onDismiss()
                    onOpenUsageAccess()
                },
                onOpenAccessibility = {
                    onDismiss()
                    onOpenAccessibility()
                },
                onOpenNotifications = {
                    onDismiss()
                    onOpenNotifications()
                },
            )
        }
    }
}

@Composable
fun KavachScreenSubtitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.kavach_screen_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun KavachOffHintCard(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.kavach_off_hint),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KavachDesign.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = KavachSubtitleColor,
    )
}

/**
 * Collapsible “How Kavach works” card — progressive disclosure (see ui-ux.md).
 * Collapsed: title + “Learn more”. Expanded: intro + short guide bullets.
 */
@Composable
fun KavachHowItWorksCard(modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KavachDesign.SurfaceHighlight.copy(alpha = 0.55f))
            .border(1.dp, KavachDesign.Primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KavachDesign.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.kavach_info_how_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KavachTitleColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (expanded) R.string.kavach_how_it_works_show_less
                        else R.string.kavach_how_it_works_learn_more,
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = KavachDesign.Primary,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 54.dp, top = 12.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.kavach_how_it_works_intro),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = KavachSubtitleColor,
                )
                KavachGuideLine(stringResource(R.string.kavach_guide_use))
                KavachGuideLine(stringResource(R.string.kavach_guide_beast))
                KavachGuideLine(stringResource(R.string.kavach_guide_unlock))
                Text(
                    text = stringResource(R.string.kavach_about_privacy_card),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = KavachSubtitleColor,
                )
            }
        }
    }
}

@Composable
private fun KavachGuideLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("•", fontSize = 14.sp, color = KavachDesign.Primary, fontWeight = FontWeight.Bold)
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = KavachSubtitleColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun KavachSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 4.dp, bottom = 12.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = KavachDesign.HubSectionLabel,
    )
}

@Composable
fun KavachStatusPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KavachStatusBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = KavachStatusText,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.kavach_status_pill),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = KavachStatusText,
            lineHeight = 20.sp,
        )
    }
}

@Composable
fun KavachHeroSwitchCard(
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val cardColor = if (isDark) Color(0xFF1A1C1F) else Color.White

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.kavach_enable_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) scheme.onBackground else KavachTitleColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.kavach_enable_subtitle),
                    fontSize = 13.sp,
                    color = if (isDark) scheme.onSurfaceVariant else KavachSubtitleColor,
                    lineHeight = 18.sp,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFCBD5E1),
                ),
            )
        }
    }
}

@Composable
fun KavachControlCenterContainer(
    blockedAppCount: Int,
    beastModeEnabled: Boolean,
    emergencyUnlockEnabled: Boolean,
    accent: Color,
    onOpenAppPicker: () -> Unit,
    onBeastModeChange: (Boolean) -> Unit,
    onEmergencyUnlockChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val cardColor = if (isDark) Color(0xFF1A1C1F) else Color.White
    val appsLabel = if (blockedAppCount == 0) {
        stringResource(R.string.kavach_blocked_apps_none)
    } else {
        stringResource(R.string.kavach_blocked_apps_count, blockedAppCount)
    }
    val emergencySubtitle = if (beastModeEnabled) {
        stringResource(R.string.kavach_emergency_off_beast)
    } else {
        stringResource(R.string.kavach_emergency_unlock_subtitle)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        KavachSectionLabel(stringResource(R.string.kavach_section_apps))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, KavachDesign.HubBorder) else null,
        ) {
            KavachControlRow(
                iconRes = R.drawable.ic_apps,
                title = stringResource(R.string.kavach_blocked_apps_title),
                subtitle = stringResource(R.string.kavach_blocked_apps_subtitle),
                onClick = onOpenAppPicker,
                isDark = isDark,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = appsLabel,
                        fontSize = 14.sp,
                        color = KavachDesign.HubText,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_caret_right),
                        contentDescription = null,
                        tint = KavachDesign.HubTextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        KavachSectionLabel(stringResource(R.string.kavach_section_session))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = if (!isDark) androidx.compose.foundation.BorderStroke(1.dp, KavachDesign.HubBorder) else null,
        ) {
            Column {
                KavachControlRow(
                    iconRes = R.drawable.ic_zap,
                    title = stringResource(R.string.kavach_beast_mode_title),
                    subtitle = stringResource(R.string.kavach_beast_mode_subtitle),
                    onClick = { onBeastModeChange(!beastModeEnabled) },
                    isDark = isDark,
                ) {
                    KavachSwitch(
                        checked = beastModeEnabled,
                        accent = accent,
                        onCheckedChange = onBeastModeChange,
                    )
                }

                HorizontalDivider(color = KavachDesign.Surface, thickness = 1.dp)

                KavachControlRow(
                    iconRes = R.drawable.ic_key,
                    title = stringResource(R.string.kavach_emergency_unlock_title),
                    subtitle = emergencySubtitle,
                    onClick = {
                        if (!beastModeEnabled) onEmergencyUnlockChange(!emergencyUnlockEnabled)
                    },
                    isDark = isDark,
                    enabled = !beastModeEnabled,
                ) {
                    KavachSwitch(
                        checked = emergencyUnlockEnabled && !beastModeEnabled,
                        accent = accent,
                        onCheckedChange = { if (!beastModeEnabled) onEmergencyUnlockChange(it) },
                        enabled = !beastModeEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun KavachSwitch(
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = KavachDesign.Primary,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFCBD5E1),
            disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
            disabledUncheckedThumbColor = Color.White.copy(alpha = 0.7f),
            disabledCheckedTrackColor = KavachDesign.Primary.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = Color(0xFFCBD5E1).copy(alpha = 0.5f),
        ),
    )
}

@Composable
fun KavachControlRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDark: Boolean,
    enabled: Boolean = true,
    trailingContent: @Composable () -> Unit,
) {
    val titleColor = when {
        !enabled -> KavachSubtitleColor.copy(alpha = 0.6f)
        isDark -> MaterialTheme.colorScheme.onBackground
        else -> KavachTitleColor
    }
    val subtitleColor = if (enabled) {
        if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else KavachSubtitleColor
    } else {
        KavachSubtitleColor.copy(alpha = 0.55f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = title,
            tint = KavachDesign.HubText,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) titleColor else KavachDesign.HubText,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = if (isDark) subtitleColor else KavachDesign.HubTextMuted,
                lineHeight = 20.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        trailingContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KavachInfoBottomSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme
    val howBody = stringResource(R.string.kavach_info_how_body)
    val permissionsBody = stringResource(R.string.kavach_info_permissions_body)
    val introText = remember(howBody) {
        howBody.substringBefore(". It").ifBlank { howBody }
    }
    val privacyTail = howBody.substringAfter(". It").takeIf { it.isNotBlank() }.orEmpty()
    val privacyBody = stringResource(R.string.kavach_consent_privacy_body)
    val privacyRows = remember(privacyTail, privacyBody) {
        listOfNotNull(
            privacyTail.takeIf { it.isNotBlank() }?.let { KavachPrivacyRow(Icons.AutoMirrored.Filled.Chat, it) },
            KavachPrivacyRow(Icons.Default.VisibilityOff, privacyBody),
        )
    }
    val permissionLines = remember(permissionsBody) {
        permissionsBody.split(". ").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val permissionTitles = listOf(
        "App Check",
        "Block Screen",
        "Notifications",
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            KavachAboutSheetHeader(
                title = stringResource(R.string.kavach_info_sheet_title),
                description = introText,
            )

            KavachPrivacyGuaranteeCard(
                title = stringResource(R.string.kavach_consent_privacy_label),
                rows = privacyRows,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.kavach_info_permissions_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                permissionTitles.forEachIndexed { index, title ->
                    KavachDevicePermissionCard(
                        title = title,
                        subtitle = permissionLines.getOrElse(index) { "" },
                        required = index < 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun KavachAboutSheetHeader(
    title: String,
    description: String,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private data class KavachPrivacyRow(
    val icon: ImageVector,
    val text: String,
)

@Composable
private fun KavachPrivacyGuaranteeCard(
    title: String,
    rows: List<KavachPrivacyRow>,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, scheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield_check),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
        }
        rows.forEach { row ->
            KavachPrivacyBulletRow(icon = row.icon, text = row.text)
        }
    }
}

@Composable
private fun KavachPrivacyBulletRow(
    icon: ImageVector,
    text: String,
) {
    val scheme = MaterialTheme.colorScheme
    val neverWord = "never"
    val annotated = buildAnnotatedString {
        val lower = text.lowercase()
        val index = lower.indexOf(neverWord)
        if (index >= 0) {
            append(text.substring(0, index))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = scheme.onSurface)) {
                append(text.substring(index, index + neverWord.length))
            }
            append(text.substring(index + neverWord.length))
        } else {
            append(text)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Text(
            text = annotated,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun KavachDevicePermissionCard(
    title: String,
    subtitle: String,
    required: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        KavachPermissionBadge(required = required)
    }
}

@Composable
private fun KavachPermissionBadge(required: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (required) scheme.primary else scheme.surfaceVariant
    val fg = if (required) scheme.onPrimary else scheme.onSurfaceVariant
    val label = if (required) "REQUIRED" else "OPTIONAL"
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        color = fg,
    )
}
