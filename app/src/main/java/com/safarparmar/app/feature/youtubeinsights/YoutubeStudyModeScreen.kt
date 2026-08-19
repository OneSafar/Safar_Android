package com.safarparmar.app.feature.youtubeinsights

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import com.safarparmar.app.ui.ekagra.focusshield.KavachDesign
import com.safarparmar.app.ui.navigation.Routes

@Composable
fun YoutubeStudyModeScreen(
    currentRoute: String,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit,
    onToggleDarkTheme: () -> Unit,
    viewModel: YoutubeStudyModeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SafarDrawerScaffold(
        title = "YouTube Study Mode",
        subtitle = "SAFAR",
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        emphasizeTopBar = true,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            if (!state.onboardingDone) {
                YoutubeStudyOnboarding(
                    state = state,
                    onSetEnabled = { enabled ->
                        if (enabled) viewModel.recordConsentAndEnable() else viewModel.setEnabled(false)
                    },
                    onSetProductive = viewModel::setProductive,
                    onConsent = viewModel::recordConsentAndEnable,
                    onRefresh = viewModel::refresh,
                    onFinish = viewModel::finishOnboarding,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                YoutubeStudyManagement(
                    state = state,
                    onSetEnabled = viewModel::setEnabled,
                    onSetProductive = viewModel::setProductive,
                    onOpenAnalytics = { onNavigate(Routes.YOUTUBE_STUDY_ANALYTICS) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Onboarding setup screen with modern flat card design (matching Kavach & Ekagra).
 * Padded dynamically with `.navigationBarsPadding()` so the bottom "Finish setup"
 * action is NEVER hidden by 3-button or gesture system navigation bars.
 */
@Composable
private fun YoutubeStudyOnboarding(
    state: YoutubeStudyModeUiState,
    onSetEnabled: (Boolean) -> Unit,
    onSetProductive: (String, Boolean) -> Unit,
    onConsent: () -> Unit,
    onRefresh: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val requestNotifications = rememberNotificationPermissionRequester { onRefresh() }
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding() // Ensures bottom buttons adapt to 3-button or gesture navigation bars
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero Card Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(KavachDesign.Border)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(KavachDesign.Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.SmartDisplay,
                            contentDescription = null,
                            tint = KavachDesign.Primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "YouTube Study Mode",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KavachDesign.TextMain,
                        )
                        Text(
                            "Block Shorts & distracting channels while studying",
                            fontSize = 13.sp,
                            color = KavachDesign.TextMuted,
                            lineHeight = 17.sp,
                        )
                    }
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = onSetEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KavachDesign.Primary,
                        ),
                    )
                }

                HorizontalDivider(color = KavachDesign.Border.copy(alpha = 0.5f))

                // Privacy assurance pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KavachDesign.Primary.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = KavachDesign.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Reads visible YouTube screen locally on device. No video content or history is uploaded.",
                        fontSize = 12.sp,
                        color = KavachDesign.TextMuted,
                        lineHeight = 16.sp,
                    )
                }
            }
        }

        Text(
            "Setup Instructions",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = KavachDesign.TextMain,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )

        // Step 1: Accessibility
        FlatSetupCard(
            stepNumber = 1,
            title = "Allow YouTube detection",
            body = if (state.hasAccessibility) "Accessibility detection is ready." else "Enable SAFAR YouTube Study Mode in Android Accessibility Settings.",
            actionText = if (state.hasAccessibility) "Ready" else "Enable in Settings",
            isDone = state.hasAccessibility,
            onClick = {
                onConsent()
                FocusShieldPermissionHelper.openAccessibilitySettings(context)
            },
        )

        // Step 2: Notifications
        FlatSetupCard(
            stepNumber = 2,
            title = "Allow channel actions",
            body = if (state.hasNotifications) {
                "Notifications ready. Mark channels Productive directly from notifications."
            } else {
                "Notifications let you mark blocked channels Productive without opening SAFAR."
            },
            actionText = if (state.hasNotifications) "Ready" else "Allow Notifications",
            isDone = state.hasNotifications,
            onClick = { requestNotifications() },
        )

        Text(
            "Step 3 · Choose study channels",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = KavachDesign.TextMain,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )
        Text(
            "Selected channels are Productive. All other channels start as Distracting by default.",
            fontSize = 12.5.sp,
            color = KavachDesign.TextMuted,
            modifier = Modifier.padding(start = 4.dp),
        )

        // Starter Channels list
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(KavachDesign.Border)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val starterChannels = state.channels.filter { it.channelKey in YoutubeInsightsRepository.STARTER_CHANNEL_KEYS }
                starterChannels.forEachIndexed { index, channel ->
                    FlatChannelToggleRow(channel, onSetProductive)
                    if (index < starterChannels.size - 1) {
                        HorizontalDivider(color = KavachDesign.Border.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // Newly detected channels section
        val detectedChannels = state.channels.filter {
            it.channelKey !in YoutubeInsightsRepository.STARTER_CHANNEL_KEYS
        }
        if (detectedChannels.isNotEmpty()) {
            Text(
                "Newly detected channels",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = KavachDesign.TextMain,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(KavachDesign.Border)),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    detectedChannels.forEachIndexed { index, channel ->
                        FlatChannelToggleRow(channel, onSetProductive)
                        if (index < detectedChannels.size - 1) {
                            HorizontalDivider(color = KavachDesign.Border.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Action Buttons — explicitly elevated and padded for 3-button or gesture bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    onConsent()
                    val intent = context.packageManager.getLaunchIntentForPackage(YoutubeUiParser.YOUTUBE_PACKAGE)
                        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
                shape = CircleShape,
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(KavachDesign.Primary)),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = KavachDesign.Primary)
                Spacer(Modifier.width(8.dp))
                Text("Open YouTube App", fontWeight = FontWeight.SemiBold, color = KavachDesign.Primary)
            }

            Button(
                onClick = {
                    onRefresh()
                    onFinish()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KavachDesign.Primary,
                    contentColor = Color.White,
                ),
            ) {
                Text("Finish Setup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * YouTube Study Mode active management screen in modern flat style.
 * Includes `.navigationBarsPadding()` so channel list and controls never get cut off by
 * footer navigation buttons/gestures.
 */
@Composable
private fun YoutubeStudyManagement(
    state: YoutubeStudyModeUiState,
    onSetEnabled: (Boolean) -> Unit,
    onSetProductive: (String, Boolean) -> Unit,
    onOpenAnalytics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf("detected") }
    val scheme = MaterialTheme.colorScheme

    val filtered = state.channels.filter { it.displayName.contains(query, true) }.filter { channel ->
        when (section) {
            "productive" -> channel.isProductive
            "distracting" -> !channel.isProductive && channel.channelKey in YoutubeInsightsRepository.STARTER_CHANNEL_KEYS
            else -> !channel.isProductive && channel.channelKey !in YoutubeInsightsRepository.STARTER_CHANNEL_KEYS
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding() // Ensures bottom elements adapt dynamically to gesture/3-button nav
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Master Control Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(KavachDesign.Border)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.enabled) KavachDesign.Primary.copy(alpha = 0.12f)
                                else scheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.SmartDisplay,
                            contentDescription = null,
                            tint = if (state.enabled) KavachDesign.Primary else scheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Column {
                        Text(
                            "YouTube Study Mode",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = KavachDesign.TextMain,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (state.enabled && state.hasAccessibility) Color(0xFF22C55E) else Color.Gray)
                            )
                            Text(
                                if (!state.enabled) "Disabled"
                                else if (state.hasAccessibility) "Active — Protecting YouTube"
                                else "Accessibility Needed",
                                fontSize = 12.5.sp,
                                color = KavachDesign.TextMuted,
                            )
                        }
                    }
                }
                Switch(
                    checked = state.enabled,
                    onCheckedChange = onSetEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = KavachDesign.Primary,
                    ),
                )
            }
        }

        // Blocking Configuration Cards
        Text(
            "Blocking Rules",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = KavachDesign.TextMain,
            modifier = Modifier.padding(start = 4.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(KavachDesign.Border)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(Modifier.padding(vertical = 4.dp)) {
                FlatRuleStatusRow("Block Shorts", state.enabled)
                HorizontalDivider(color = KavachDesign.Border.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                FlatRuleStatusRow("Block Distracting Channels", state.enabled)
            }
        }

        // Analytics Action Card Button
        Button(
            onClick = onOpenAnalytics,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KavachDesign.SurfaceHighlight,
                contentColor = KavachDesign.Primary,
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp),
        ) {
            Icon(Icons.Default.Analytics, contentDescription = null, tint = KavachDesign.Primary)
            Spacer(Modifier.width(8.dp))
            Text("View YouTube Study Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        HorizontalDivider(color = KavachDesign.Border.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

        // Channel Management Section
        Text(
            "Manage Channels",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = KavachDesign.TextMain,
            modifier = Modifier.padding(start = 4.dp),
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search channels…", fontSize = 13.5.sp, color = KavachDesign.SearchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KavachDesign.SearchHint) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KavachDesign.SearchFieldBg,
                unfocusedContainerColor = KavachDesign.SearchFieldBg,
                focusedBorderColor = KavachDesign.Primary,
                unfocusedBorderColor = KavachDesign.Border,
            ),
        )

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("detected" to "New", "productive" to "Productive", "distracting" to "Distracting").forEachIndexed { index, item ->
                SegmentedButton(
                    selected = section == item.first,
                    onClick = { section = item.first },
                    shape = SegmentedButtonDefaults.itemShape(index, 3),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = KavachDesign.Primary,
                        activeContentColor = Color.White,
                    ),
                ) { Text(item.second, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(KavachDesign.Border)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (filtered.isEmpty()) {
                    Text(
                        if (section == "detected") "Open a YouTube video and newly detected channels will appear here automatically."
                        else "No channels found in this section.",
                        fontSize = 13.sp,
                        color = KavachDesign.TextMuted,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    )
                } else {
                    filtered.forEachIndexed { index, channel ->
                        FlatChannelToggleRow(channel, onSetProductive)
                        if (index < filtered.size - 1) {
                            HorizontalDivider(color = KavachDesign.Border.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }

        Text(
            "New channels are stored locally on your device and start as Distracting by default.",
            fontSize = 12.sp,
            color = KavachDesign.TextMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun FlatSetupCard(
    stepNumber: Int,
    title: String,
    body: String,
    actionText: String,
    isDone: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KavachDesign.CardWhite),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isDone) Color(0xFF22C55E).copy(alpha = 0.4f) else KavachDesign.Border)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDone) Color(0xFF22C55E).copy(alpha = 0.14f)
                                else KavachDesign.Primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDone) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(
                                "$stepNumber",
                                color = KavachDesign.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = KavachDesign.TextMain,
                    )
                }
                if (isDone) {
                    Text(
                        "Done",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF22C55E),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF22C55E).copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                body,
                fontSize = 13.sp,
                color = KavachDesign.TextMuted,
                lineHeight = 17.sp,
            )

            if (!isDone) {
                TextButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(actionText, fontWeight = FontWeight.Bold, color = KavachDesign.Primary)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = KavachDesign.Primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FlatChannelToggleRow(
    channel: com.safarparmar.app.feature.kavachanalytics.data.local.YoutubeChannelEntity,
    onSetProductive: (String, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                channel.displayName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = KavachDesign.TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (channel.isProductive) Color(0xFF22C55E) else Color(0xFFF59E0B))
                )
                Text(
                    if (channel.isProductive) "Productive (Allowed)" else "Distracting (Blocked)",
                    fontSize = 12.sp,
                    color = KavachDesign.TextMuted,
                )
            }
        }
        Switch(
            checked = channel.isProductive,
            onCheckedChange = { onSetProductive(channel.channelKey, it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF22C55E),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = KavachDesign.Border,
            ),
        )
    }
}

@Composable
private fun FlatRuleStatusRow(label: String, isEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            color = KavachDesign.TextMain,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (isEnabled) Color(0xFFEF4444).copy(alpha = 0.12f)
                    else KavachDesign.Border.copy(alpha = 0.3f)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                if (isEnabled) "Always Blocked" else "Disabled",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color(0xFFEF4444) else KavachDesign.TextMuted,
            )
        }
    }
}
