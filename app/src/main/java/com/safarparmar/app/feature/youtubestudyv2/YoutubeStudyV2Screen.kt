package com.safarparmar.app.feature.youtubestudyv2

/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
 * designed-as-app · structure: flat task-led setup to control centre
 * tokens: SAFAR Material theme + KavachDesign · adaptive: contained 560dp column
 */

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import com.safarparmar.app.ui.ekagra.focusshield.KavachDesign
import com.safarparmar.app.ui.theme.SafarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeStudyV2Screen(
    onBack: () -> Unit,
    viewModel: YoutubeStudyV2ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermission()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    fun openAccessibilitySettings() {
        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    Scaffold(
        containerColor = KavachDesign.Background,
        topBar = {
            TopAppBar(
                title = { Text("YouTube Study Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (state.setupCompleted) {
                StudyModeDashboard(
                    state = state,
                    onSetEnabled = viewModel::setEnabled,
                    onOpenAccessibility = ::openAccessibilitySettings,
                    onReferenceChanged = viewModel::setReference,
                    onAddChannel = viewModel::resolveAndAllow,
                    onSetProductive = viewModel::setProductive,
                    onToggleAvailable = viewModel::toggleAvailable,
                    onSetAvailableProductive = viewModel::setAvailableProductive,
                )
            } else {
                StudyModeSetup(
                    state = state,
                    onAgree = {
                        viewModel.acceptDisclosure()
                        if (state.accessibilityEnabled) viewModel.refreshPermission()
                        else openAccessibilitySettings()
                    },
                    onNotNow = onBack,
                    onReferenceChanged = viewModel::setReference,
                    onAddChannel = viewModel::resolveAndAllow,
                    onSetAvailableProductive = viewModel::setAvailableProductive,
                    onContinue = viewModel::continueToReview,
                    onBackToChannels = viewModel::returnToChannelSelection,
                    onStart = viewModel::finishSetup,
                )
            }
        }
    }
}

@Composable
private fun StudyModeSetup(
    state: YoutubeStudyV2UiState,
    onAgree: () -> Unit,
    onNotNow: () -> Unit,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    onSetAvailableProductive: (ResolvedYoutubeChannelDto, Boolean) -> Unit,
    onContinue: () -> Unit,
    onBackToChannels: () -> Unit,
    onStart: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { SetupProgress(state.setupStep) }
        when (state.setupStep) {
            1 -> item { PermissionStep(state.accessibilityEnabled, onAgree, onNotNow) }
            2 -> item {
                ChannelSelectionStep(
                    state,
                    onReferenceChanged,
                    onAddChannel,
                    onSetAvailableProductive,
                    onContinue,
                )
            }
            else -> item { ReadyStep(state.allowed.size, onBackToChannels, onStart) }
        }
    }
}

@Composable
private fun SetupProgress(step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Setup", style = MaterialTheme.typography.labelMedium, color = KavachDesign.Primary, fontWeight = FontWeight.Bold)
            Text("$step of 3", style = MaterialTheme.typography.labelMedium, color = KavachDesign.TextMuted)
        }
        LinearProgressIndicator(
            progress = { step / 3f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = KavachDesign.Primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun PermissionStep(permissionConnected: Boolean, onAgree: () -> Unit, onNotNow: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HeroIcon(Icons.Default.Shield)
        PageHeading(
            "Block YouTube distractions",
            "Choose the channels you want to watch. SAFAR blocks all other channels and Shorts.",
        )
        SectionRule()
        Text("Permission needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "SAFAR uses Accessibility only in YouTube. It reads the channel shown on a video you open. It also detects Shorts.",
            style = MaterialTheme.typography.bodyMedium,
            color = KavachDesign.TextMuted,
        )
        Text(
            "When you open a new channel, SAFAR sends only its @handle to verify the Channel ID. The verified channel is added to the shared channel list. SAFAR does not read passwords, messages, or other apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = KavachDesign.TextMuted,
        )
        if (permissionConnected) StatusStrip(Icons.Default.CheckCircle, "Accessibility is on")
        PrimaryActionButton(
            if (permissionConnected) "I agree · Continue" else "I agree · Open settings",
            onAgree,
        )
        TextButton(onClick = onNotNow, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Not now", color = KavachDesign.TextMuted)
        }
    }
}

@Composable
private fun ChannelSelectionStep(
    state: YoutubeStudyV2UiState,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    onSetAvailableProductive: (ResolvedYoutubeChannelDto, Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    val suggestions = starterChannels(state.available)
    var availableExpanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeading(
            "Choose productive channels",
            "Only the channels you turn on can play. All other channels and Shorts are blocked.",
        )
        SectionHeading("Suggested study channels", "Turn on specific channels you want.")
        when {
            state.loadingAvailable -> Text("Loading suggestions…", color = KavachDesign.TextMuted)
            suggestions.isEmpty() -> Text("No suggestions. Add a channel below.", color = KavachDesign.TextMuted)
            else -> suggestions.forEachIndexed { index, channel ->
                val productive = state.allowed.any { it.channelId == channel.channelId }
                ChannelToggleRow(channel.displayName, channel.handle, productive) {
                    onSetAvailableProductive(channel, it)
                }
                if (index != suggestions.lastIndex) SectionRule()
            }
        }
        ExpandableSection(
            "Available channels",
            "Find and choose more channels.",
            availableExpanded,
            { availableExpanded = !availableExpanded },
        ) {
            AvailableChannelList(
                channels = state.available,
                allowedChannelIds = state.allowed.mapTo(mutableSetOf()) { it.channelId },
                onProductiveChange = onSetAvailableProductive,
            )
        }
        SectionRule()
        SectionHeading("Add a channel", "Can't find your channel? Add its @handle.")
        OutlinedTextField(
            value = state.reference,
            onValueChange = onReferenceChanged,
            label = { Text("YouTube @handle") },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
            supportingText = { Text("Example: @parmarssc") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        PrimaryActionButton(
            if (state.resolving) "Checking…" else "Add channel",
            onAddChannel,
            !state.resolving && state.reference.isNotBlank(),
        )
        StateMessage(state)
        val canContinue = state.allowed.isNotEmpty()
        PrimaryActionButton(
            if (canContinue) "Continue · Ready" else "Choose at least one channel",
            onContinue,
            canContinue,
        )
    }
}

@Composable
private fun ReadyStep(productiveCount: Int, onBack: () -> Unit, onStart: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HeroIcon(Icons.Default.CheckCircle)
        PageHeading("Ready to start", "Check your choices.")
        SectionRule()
        RuleRow(Icons.Default.Block, "Shorts", "Always blocked")
        RuleRow(Icons.Default.CheckCircle, "Productive channels", "$productiveCount allowed")
        RuleRow(Icons.Default.Shield, "All other channels", "Blocked")
        RuleRow(Icons.Default.PlayArrow, "Home screen & previews", "Allowed")
        SectionRule()
        PrimaryActionButton("Start Study Mode", onStart)
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back to settings", color = KavachDesign.TextMuted)
        }
    }
}

@Composable
private fun StudyModeDashboard(
    state: YoutubeStudyV2UiState,
    onSetEnabled: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    onSetProductive: (String, Boolean) -> Unit,
    onToggleAvailable: () -> Unit,
    onSetAvailableProductive: (ResolvedYoutubeChannelDto, Boolean) -> Unit,
) {
    var reliabilityExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ProtectionStatus(state.enabled, state.accessibilityEnabled, state.allowed.size, onSetEnabled, onOpenAccessibility)
        }
        item { SectionHeading("Productive channels", "These specific channels can play.") }
        if (state.allowed.isEmpty()) {
            item { Text("No custom channels added.", color = KavachDesign.TextMuted) }
        } else {
            items(state.allowed, key = { it.channelId }) { channel ->
                ChannelToggleRow(channel.displayName, channel.handle, true) {
                    onSetProductive(channel.channelId, it)
                }
                SectionRule()
            }
        }
        item {
            ExpandableSection(
                "Available channels",
                "Find and choose more channels.",
                state.availableExpanded,
                onToggleAvailable,
            ) {
                AvailableChannelList(
                    channels = state.available,
                    allowedChannelIds = state.allowed.mapTo(mutableSetOf()) { it.channelId },
                    initialFilter = AvailableChannelFilter.DISTRACTING,
                    onProductiveChange = onSetAvailableProductive,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeading("Add a channel", "Can't find your channel? Add its @handle.")
                OutlinedTextField(
                    value = state.reference,
                    onValueChange = onReferenceChanged,
                    label = { Text("YouTube @handle") },
                    supportingText = { Text("Example: @parmarssc") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                PrimaryActionButton(
                    if (state.resolving) "Checking…" else "Add channel",
                    onAddChannel,
                    !state.resolving && state.reference.isNotBlank(),
                )
                StateMessage(state)
            }
        }
        item {
            ExpandableSection(
                "Study Mode not working?",
                "Check these phone settings.",
                reliabilityExpanded,
                { reliabilityExpanded = !reliabilityExpanded },
            ) {
                Text("MIUI: Turn on Autostart. Set Battery saver to No restrictions.")
                Text("ColorOS/realme: Turn on Auto launch and background activity.")
                Text("Samsung: Remove SAFAR from Sleeping apps.")
            }
        }
    }
}

internal enum class AvailableChannelFilter { ALL, PRODUCTIVE, DISTRACTING }

private val STARTER_CHANNEL_HANDLES = listOf("@parmarssc", "@safarparmar")

internal fun starterChannels(channels: List<ResolvedYoutubeChannelDto>): List<ResolvedYoutubeChannelDto> =
    STARTER_CHANNEL_HANDLES.mapNotNull { starterHandle ->
        channels.firstOrNull { it.handle.equals(starterHandle, ignoreCase = true) }
    }

internal fun filterAvailableChannels(
    channels: List<ResolvedYoutubeChannelDto>,
    allowedChannelIds: Set<String>,
    filter: AvailableChannelFilter,
): List<ResolvedYoutubeChannelDto> = channels
    .filter { channel ->
        when (filter) {
            AvailableChannelFilter.ALL -> true
            AvailableChannelFilter.PRODUCTIVE -> channel.channelId in allowedChannelIds
            AvailableChannelFilter.DISTRACTING -> channel.channelId !in allowedChannelIds
        }
    }

@Composable
private fun AvailableChannelList(
    channels: List<ResolvedYoutubeChannelDto>,
    allowedChannelIds: Set<String>,
    initialFilter: AvailableChannelFilter = AvailableChannelFilter.ALL,
    onProductiveChange: (ResolvedYoutubeChannelDto, Boolean) -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableStateOf(initialFilter) }
    val visibleChannels = filterAvailableChannels(channels, allowedChannelIds, selectedFilter)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(3.dp),
    ) {
        AvailableChannelFilter.entries.forEach { filter ->
            val selected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier)
                    .clickable { selectedFilter = filter }
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (filter) {
                        AvailableChannelFilter.ALL -> "All"
                        AvailableChannelFilter.PRODUCTIVE -> "Productive"
                        AvailableChannelFilter.DISTRACTING -> "Distracting"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) KavachDesign.TextMain else KavachDesign.TextMuted,
                    maxLines = 1,
                )
            }
        }
    }
    visibleChannels.forEachIndexed { index, channel ->
        val productive = channel.channelId in allowedChannelIds
        ChannelToggleRow(channel.displayName, channel.handle, productive) {
            onProductiveChange(channel, it)
        }
        if (index != visibleChannels.lastIndex) SectionRule()
    }
    if (visibleChannels.isEmpty()) {
        Text(
            when (selectedFilter) {
                AvailableChannelFilter.ALL -> "No channels found."
                AvailableChannelFilter.PRODUCTIVE -> "No Productive channels here."
                AvailableChannelFilter.DISTRACTING -> "No Distracting channels here."
            },
            color = KavachDesign.TextMuted,
        )
    }
}

@Composable
private fun ProtectionStatus(
    enabled: Boolean,
    accessibilityEnabled: Boolean,
    productiveCount: Int,
    onSetEnabled: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    Surface(
        color = if (enabled && accessibilityEnabled) KavachDesign.SurfaceHighlight else KavachDesign.CardWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, KavachDesign.Border),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeroIcon(Icons.Default.Shield, compact = true)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (enabled && accessibilityEnabled) "Study Mode is on" else "Study Mode is off", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Shorts blocked · $productiveCount ${if (productiveCount == 1) "channel" else "channels"} allowed",
                        style = MaterialTheme.typography.bodySmall,
                        color = KavachDesign.TextMuted,
                    )
                }
                Switch(checked = enabled && accessibilityEnabled, enabled = accessibilityEnabled, onCheckedChange = onSetEnabled)
            }
            if (!accessibilityEnabled) {
                Text("Turn on Accessibility to block videos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open settings")
                }
            }
        }
    }
}

@Composable
private fun ChannelToggleRow(name: String, handle: String?, productive: Boolean, onProductiveChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (productive) KavachDesign.SurfaceHighlight else MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (productive) Icons.Default.Check else Icons.Default.Block,
                    contentDescription = null,
                    tint = if (productive) KavachDesign.Primary else KavachDesign.TextMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(handle ?: if (productive) "Productive" else "Distracting", style = MaterialTheme.typography.bodySmall, color = KavachDesign.TextMuted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (productive) "Productive" else "Distracting",
                style = MaterialTheme.typography.labelSmall,
                color = if (productive) KavachDesign.Primary else KavachDesign.TextMuted,
            )
            Switch(checked = productive, onCheckedChange = onProductiveChange)
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KavachDesign.TextMuted)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().border(1.dp, KavachDesign.Border, RoundedCornerShape(16.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { content() }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KavachDesign.TextMuted)
    }
}

@Composable
private fun PageHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KavachDesign.TextMain,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = KavachDesign.TextMuted,
        )
    }
}

@Composable
private fun HeroIcon(icon: ImageVector, compact: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(if (compact) 14.dp else 20.dp),
        color = KavachDesign.SurfaceHighlight,
        modifier = Modifier.size(if (compact) 48.dp else 68.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = KavachDesign.Primary, modifier = Modifier.size(if (compact) 25.dp else 34.dp))
        }
    }
}

@Composable
private fun RuleRow(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = KavachDesign.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusStrip(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().border(1.dp, KavachDesign.Border, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = KavachDesign.Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrimaryActionButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = KavachDesign.Primary, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun StateMessage(state: YoutubeStudyV2UiState) {
    state.message?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = if (state.isError) MaterialTheme.colorScheme.error else KavachDesign.Primary)
    }
}

@Composable
private fun SectionRule() = HorizontalDivider(color = KavachDesign.Border)

@Preview(name = "Phone · channel setup", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun YoutubeStudySetupPhonePreview() {
    SafarTheme {
        StudyModeSetup(
            state = previewState(setupStep = 2),
            onAgree = {},
            onNotNow = {},
            onReferenceChanged = {},
            onAddChannel = {},
            onSetAvailableProductive = { _, _ -> },
            onContinue = {},
            onBackToChannels = {},
            onStart = {},
        )
    }
}

@Preview(name = "Tablet · active dashboard", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun YoutubeStudyDashboardTabletPreview() {
    SafarTheme {
        StudyModeDashboard(
            state = previewState(setupStep = 3).copy(
                setupCompleted = true,
                enabled = true,
                accessibilityEnabled = true,
                availableExpanded = true,
            ),
            onSetEnabled = {},
            onOpenAccessibility = {},
            onReferenceChanged = {},
            onAddChannel = {},
            onSetProductive = { _, _ -> },
            onToggleAvailable = {},
            onSetAvailableProductive = { _, _ -> },
        )
    }
}

private fun previewState(setupStep: Int): YoutubeStudyV2UiState {
    val channel = YoutubeV2IdentityEntity(
        channelId = "UCPxPvWsvqwU18UkpsZD4bODw",
        handle = "@parmarssc",
        displayName = "PARMAR SSC",
        thumbnailUrl = null,
        resolvedAtMs = 0L,
    )
    return YoutubeStudyV2UiState(
        setupStep = setupStep,
        allowed = listOf(channel),
        available = listOf(
            ResolvedYoutubeChannelDto(
                channelId = channel.channelId,
                handle = channel.handle,
                displayName = channel.displayName,
            ),
            ResolvedYoutubeChannelDto(
                channelId = "UCsbT4wZ_FUUpJGtVa4mooow",
                handle = "@safarparmar",
                displayName = "SAFAR_PARMAR",
            ),
            ResolvedYoutubeChannelDto(
                channelId = "UC0000000000000000000001",
                handle = "@mathsclass",
                displayName = "Maths Class",
            ),
            ResolvedYoutubeChannelDto(
                channelId = "UC0000000000000000000002",
                handle = "@englishclass",
                displayName = "English Class",
            ),
        ),
    )
}
