package com.safarparmar.app.feature.youtubestudyv2

/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
 * redesign · genre: modern-minimal · theme: Cobalt (deep-black + royal-blue)
 * tokens: KavachDesign + YTCM palette · adaptive: 560dp column · isLight-flagged
 * states: default · loading · error · empty · permission-needed · permission-ok
 */

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.kavachanalytics.ui.KavachCategoryColors
import com.safarparmar.app.feature.kavachanalytics.ui.OutlineChip
import com.safarparmar.app.feature.kavachanalytics.ui.primaryText
import com.safarparmar.app.feature.kavachanalytics.ui.secondaryText
import com.safarparmar.app.ui.ekagra.focusshield.KavachDesign
import com.safarparmar.app.ui.theme.SafarTheme

// ── YTCM Design Tokens ───────────────────────────────────────────────────────

internal object YTCMColors {
    /** Near-black — premium button fill in light mode */
    val PremiumBlack = Color(0xFF0F172A)
    /** Deep royal purple — accent, toggle track, progress */
    val RoyalPurple = Color(0xFF6B21A8)

    /** Royal purple surface wash */
    fun royalPurpleSurface(isLight: Boolean) =
        if (isLight) Color(0xFFF3E8FF) else Color(0xFF3B0764)

    /** Primary CTA fill — near-black in light, brand purple in dark */
    fun ctaFill(isLight: Boolean) =
        if (isLight) PremiumBlack else Color(0xFFC084FC)

    /** Toggle track colors */
    fun toggleTrack(isLight: Boolean) =
        if (isLight) RoyalPurple else Color(0xFF9333EA)

    fun toggleTrackUnchecked(isLight: Boolean) =
        if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155)
}

// ── Standalone Screen Wrapper (Kept for compatibility) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoutubeStudyV2Screen(
    onBack: () -> Unit,
    viewModel: YoutubeStudyV2ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val isLight = !isSystemInDarkTheme()

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
                title = {
                    Text(
                        "Study Mode",
                        fontWeight = FontWeight.Bold,
                        color = primaryText(isLight),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryText(isLight),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KavachDesign.Background,
                ),
            )
        },
    ) { padding ->
        YoutubeStudyV2Content(
            state = state,
            isLight = isLight,
            onAgree = {
                viewModel.acceptDisclosure()
                if (state.accessibilityEnabled) viewModel.goToStep2()
                else openAccessibilitySettings()
            },
            onNotNow = onBack,
            onSetEnabled = viewModel::setEnabled,
            onOpenAccessibility = ::openAccessibilitySettings,
            onReferenceChanged = viewModel::setReference,
            onAddChannel = viewModel::resolveAndAllow,
            onSetClassification = viewModel::setClassification,
            onToggleAvailable = viewModel::toggleAvailable,
            onSetAvailableClassification = viewModel::setAvailableClassification,
            onDeleteChannel = viewModel::deleteChannel,
            onBackToStep1 = viewModel::returnToStep1,
            onStart = viewModel::finishSetup,
            modifier = Modifier.padding(padding).navigationBarsPadding(),
        )
    }
}

// ── Embeddable Content (Used in Kavach Tab) ───────────────────────────────────

@Composable
fun YoutubeStudyV2Content(
    state: YoutubeStudyV2UiState,
    isLight: Boolean,
    onAgree: () -> Unit,
    onNotNow: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    onSetClassification: (String, YoutubeChannelClassification) -> Unit,
    onToggleAvailable: () -> Unit,
    onSetAvailableClassification: (ResolvedYoutubeChannelDto, YoutubeChannelClassification) -> Unit,
    onDeleteChannel: (String) -> Unit,
    onBackToStep1: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (state.setupCompleted) {
            StudyModeDashboard(
                state = state,
                isLight = isLight,
                onSetEnabled = onSetEnabled,
                onOpenAccessibility = onOpenAccessibility,
                onReferenceChanged = onReferenceChanged,
                onAddChannel = onAddChannel,
                onSetClassification = onSetClassification,
                onToggleAvailable = onToggleAvailable,
                onSetAvailableClassification = onSetAvailableClassification,
                onDeleteChannel = onDeleteChannel,
            )
        } else {
            StudyModeSetup(
                state = state,
                isLight = isLight,
                onAgree = onAgree,
                onNotNow = onNotNow,
                onReferenceChanged = onReferenceChanged,
                onAddChannel = onAddChannel,
                onSetAvailableClassification = onSetAvailableClassification,
                onBackToStep1 = onBackToStep1,
                onStart = onStart,
            )
        }
    }
}

// ── Setup flow ───────────────────────────────────────────────────────────────

@Composable
private fun StudyModeSetup(
    state: YoutubeStudyV2UiState,
    isLight: Boolean,
    onAgree: () -> Unit,
    onNotNow: () -> Unit,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    onSetAvailableClassification: (ResolvedYoutubeChannelDto, YoutubeChannelClassification) -> Unit,
    onBackToStep1: () -> Unit,
    onStart: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { SetupProgress(state.setupStep, isLight) }
        when (state.setupStep) {
            1 -> item { PermissionStep(state.accessibilityEnabled, isLight, onAgree, onNotNow) }
            else -> item {
                ChannelSelectionStep(
                    state = state,
                    isLight = isLight,
                    onReferenceChanged = onReferenceChanged,
                    onAddChannel = onAddChannel,
                    onSetAvailableClassification = onSetAvailableClassification,
                    onBackToStep1 = onBackToStep1,
                    onStart = onStart,
                )
            }
        }
    }
}

@Composable
private fun SetupProgress(step: Int, isLight: Boolean) {
    val clampedStep = step.coerceIn(1, 2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Step $clampedStep of 2",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText(isLight),
            )
            Text(
                if (clampedStep == 1) "Permission" else "Channels",
                fontSize = 12.sp,
                color = secondaryText(isLight),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(secondaryText(isLight).copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedStep / 2f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(primaryText(isLight)),
            )
        }
    }
}

// ── Step 1: Permission (Screen 3 Design) ───────────────────────────────────────

@Composable
private fun PermissionStep(
    permissionConnected: Boolean,
    isLight: Boolean,
    onAgree: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Centered Hero
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isLight) Color(0xFFFEF2F2) else Color(0xFF450A0A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.SmartDisplay,
                    contentDescription = null,
                    tint = if (isLight) Color(0xFFEF4444) else Color(0xFFF87171),
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "YouTube Study Mode",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
                textAlign = TextAlign.Center,
            )
            Text(
                "Block distracting channels. Only productive channels will open.",
                fontSize = 14.sp,
                color = secondaryText(isLight),
                textAlign = TextAlign.Center,
            )
        }

        // Feature rows (Clean, airy, non-cluttered)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeatureRow(
                icon = Icons.Default.SmartDisplay,
                iconBg = if (isLight) Color(0xFFFEF2F2) else Color(0xFF450A0A),
                iconTint = if (isLight) Color(0xFFEF4444) else Color(0xFFF87171),
                label = "Distracting channels blocked automatically",
                isLight = isLight,
            )
            FeatureRow(
                icon = Icons.Default.Block,
                iconBg = if (isLight) Color(0xFFFEF2F2) else Color(0xFF450A0A),
                iconTint = if (isLight) Color(0xFFEF4444) else Color(0xFFF87171),
                label = "YouTube Shorts always blocked",
                isLight = isLight,
            )
            FeatureRow(
                icon = Icons.Default.CheckCircle,
                iconBg = if (isLight) Color(0xFFF0FDF4) else Color(0xFF052E16),
                iconTint = if (isLight) Color(0xFF10B981) else Color(0xFF4ADE80),
                label = "Productive channels always open safely",
                isLight = isLight,
            )
        }

        // Permission status pill
        if (permissionConnected) {
            StatusPill(
                icon = Icons.Default.CheckCircle,
                text = "Permissions active",
                isOk = true,
                isLight = isLight,
            )
        } else {
            StatusPill(
                icon = Icons.Default.Settings,
                text = "Permissions are required",
                isOk = false,
                isLight = isLight,
            )
        }

        // CTA
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CtaButton(
                text = if (permissionConnected) "Select Productive Channels →" else "Turn On Permission & Continue →",
                isLight = isLight,
                onClick = onAgree,
            )
            TextButton(onClick = onNotNow) {
                Text("Not now", fontSize = 13.sp, color = secondaryText(isLight))
            }
        }
    }
}

// ── Step 2: Channel Selection ────────────────────────────────────────────────

@Composable
private fun ChannelSelectionStep(
    state: YoutubeStudyV2UiState,
    isLight: Boolean,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    onSetAvailableClassification: (ResolvedYoutubeChannelDto, YoutubeChannelClassification) -> Unit,
    onBackToStep1: () -> Unit,
    onStart: () -> Unit,
) {
    val suggestions = starterChannels(state.available)
    var availableExpanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Choose your productive channels",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText(isLight),
            )
            Text(
                "Only these channels will open. Everything else is blocked.",
                fontSize = 13.sp,
                color = secondaryText(isLight),
            )
        }

        SectionLabel("SUGGESTED PRODUCTIVE CHANNELS", isLight)
        when {
            state.loadingAvailable -> Text("Loading…", fontSize = 13.sp, color = secondaryText(isLight))
            suggestions.isEmpty() -> Text("Add a channel below.", fontSize = 13.sp, color = secondaryText(isLight))
            else -> ChannelCardGroup(
                channels = suggestions,
                classifications = state.classifications,
                isLight = isLight,
                onClassificationChange = onSetAvailableClassification,
            )
        }

        ExpandableSection(
            title = "Discovered Channels",
            subtitle = "${state.available.size} channels found",
            expanded = availableExpanded,
            isLight = isLight,
            onToggle = { availableExpanded = !availableExpanded },
        ) {
            AvailableChannelList(
                channels = state.available,
                classifications = state.classifications,
                isLight = isLight,
                onClassificationChange = onSetAvailableClassification,
            )
        }

        YTCMDivider(isLight)

        SectionLabel("ADD CHANNEL BY HANDLE", isLight)
        OutlinedTextField(
            value = state.reference,
            onValueChange = onReferenceChanged,
            placeholder = { Text("@channelhandle (e.g. @PhysicsWallah)", fontSize = 14.sp, color = secondaryText(isLight)) },
            leadingIcon = {
                Icon(Icons.Default.Add, null, tint = secondaryText(isLight), modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YTCMColors.RoyalPurple,
                unfocusedBorderColor = secondaryText(isLight).copy(alpha = 0.25f),
                focusedLabelColor = YTCMColors.RoyalPurple,
            ),
        )
        if (state.reference.isNotBlank()) {
            CtaButton(
                text = if (state.resolving) "Searching channel…" else "Add Channel →",
                isLight = isLight,
                enabled = !state.resolving,
                onClick = onAddChannel,
            )
        }
        state.message?.let {
            Text(
                it,
                fontSize = 12.sp,
                color = if (state.isError) MaterialTheme.colorScheme.error else YTCMColors.RoyalPurple,
            )
        }

        YTCMDivider(isLight)

        val canStart = state.allowed.isNotEmpty()
        CtaButton(
            text = if (canStart) "Start Study Mode · ${state.allowed.size} allowed" else "Pick at least one productive channel",
            isLight = isLight,
            enabled = canStart,
            onClick = onStart,
        )
        TextButton(
            onClick = onBackToStep1,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("← Back to permissions", fontSize = 13.sp, color = secondaryText(isLight))
        }
    }
}

// ── Dashboard ────────────────────────────────────────────────────────────────

@Composable
private fun StudyModeDashboard(
    state: YoutubeStudyV2UiState,
    isLight: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    onSetClassification: (String, YoutubeChannelClassification) -> Unit,
    onToggleAvailable: () -> Unit,
    onSetAvailableClassification: (ResolvedYoutubeChannelDto, YoutubeChannelClassification) -> Unit,
    onDeleteChannel: (String) -> Unit,
) {
    var reliabilityExpanded by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            ProtectionStatus(
                state.enabled, state.accessibilityEnabled, state.allowed.size, isLight,
                onSetEnabled, onOpenAccessibility,
            )
        }

        item { SectionLabel("ALLOWED PRODUCTIVE CHANNELS", isLight) }

        if (state.allowed.isEmpty()) {
            item { Text("No productive channels added yet.", fontSize = 13.sp, color = secondaryText(isLight)) }
        } else {
            items(state.allowed, key = { it.channelId }) { channel ->
                ChannelToggleRow(
                    name = channel.displayName,
                    handle = channel.handle,
                    classification = YoutubeChannelClassification.PRODUCTIVE,
                    isLight = isLight,
                    onClassificationChange = { onSetClassification(channel.channelId, it) },
                    onDelete = { onDeleteChannel(channel.channelId) },
                )
                YTCMDivider(isLight)
            }
        }

        item {
            ExpandableSection(
                title = "Your YouTube Channels",
                subtitle = "${state.available.size} channels found",
                expanded = state.availableExpanded,
                isLight = isLight,
                onToggle = onToggleAvailable,
            ) {
                AvailableChannelList(
                    channels = state.available,
                    classifications = state.classifications,
                    initialFilter = AvailableChannelFilter.DISTRACTING,
                    isLight = isLight,
                    onClassificationChange = onSetAvailableClassification,
                    onDeleteChannel = onDeleteChannel,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                YTCMDivider(isLight)
                SectionLabel("ADD CHANNEL BY HANDLE", isLight)
                OutlinedTextField(
                    value = state.reference,
                    onValueChange = onReferenceChanged,
                    placeholder = { Text("@channelhandle (e.g. @PhysicsWallah)", fontSize = 14.sp, color = secondaryText(isLight)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YTCMColors.RoyalPurple,
                        unfocusedBorderColor = secondaryText(isLight).copy(alpha = 0.25f),
                    ),
                )
                if (state.reference.isNotBlank()) {
                    CtaButton(
                        text = if (state.resolving) "Searching channel…" else "Add Channel →",
                        isLight = isLight,
                        enabled = !state.resolving,
                        onClick = onAddChannel,
                    )
                }
                state.message?.let {
                    Text(it, fontSize = 12.sp, color = if (state.isError) MaterialTheme.colorScheme.error else YTCMColors.RoyalPurple)
                }
            }
        }

        item {
            ExpandableSection(
                title = "Protection Stopping Automatically?",
                subtitle = "Fix background settings for your phone",
                expanded = reliabilityExpanded,
                isLight = isLight,
                onToggle = { reliabilityExpanded = !reliabilityExpanded },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Xiaomi / Redmi: Turn on Autostart & set Battery Saver to No Restrictions", fontSize = 12.sp, color = secondaryText(isLight))
                    Text("Realme / OPPO: Turn on Auto Launch & Background Running", fontSize = 12.sp, color = secondaryText(isLight))
                    Text("Samsung: Remove SAFAR from Sleeping Apps list", fontSize = 12.sp, color = secondaryText(isLight))
                }
            }
        }
    }
}

// ── Protection Status Card ────────────────────────────────────────────────────

@Composable
private fun ProtectionStatus(
    enabled: Boolean,
    accessibilityEnabled: Boolean,
    productiveCount: Int,
    isLight: Boolean,
    onSetEnabled: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    val active = enabled && accessibilityEnabled
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (active) YTCMColors.royalPurpleSurface(isLight)
                else if (isLight) Color(0xFFF8FAFC) else Color(0xFF0F172A),
            )
            .border(
                1.dp,
                if (active) YTCMColors.RoyalPurple.copy(alpha = 0.35f) else secondaryText(isLight).copy(alpha = 0.12f),
                RoundedCornerShape(18.dp),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (active) YTCMColors.RoyalPurple.copy(alpha = 0.15f)
                        else secondaryText(isLight).copy(alpha = 0.10f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (active) Icons.Default.SmartDisplay else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = if (active) YTCMColors.RoyalPurple else secondaryText(isLight),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (active) "YouTube Study Mode Active" else "YouTube Study Mode Off",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText(isLight),
                )
                Text(
                    if (active) "Shorts blocked · $productiveCount productive ${if (productiveCount == 1) "channel" else "channels"} allowed"
                    else "Turn on toggle to start blocking",
                    fontSize = 12.sp,
                    color = secondaryText(isLight),
                )
            }
            Switch(
                checked = active,
                enabled = accessibilityEnabled,
                onCheckedChange = onSetEnabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = YTCMColors.toggleTrack(isLight),
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = YTCMColors.toggleTrackUnchecked(isLight),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
        if (!accessibilityEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                    .clickable(onClick = onOpenAccessibility)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Text(
                    "Permission off — Tap to fix in Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Channel colors ──────────────────────────────────────────────────────────

internal object ChannelColors {
    /** Deep Green */
    fun productive(isLight: Boolean) =
        if (isLight) Color(0xFF047857) else Color(0xFF34D399)

    /** Maroon */
    fun distracting(isLight: Boolean) =
        if (isLight) Color(0xFF881337) else Color(0xFFFB7185)
}

// ── Channel toggle row ───────────────────────────────────────────────────────

@Composable
private fun ChannelToggleRow(
    name: String,
    handle: String?,
    classification: YoutubeChannelClassification,
    isLight: Boolean,
    onClassificationChange: (YoutubeChannelClassification) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val productive = classification == YoutubeChannelClassification.PRODUCTIVE
    val distracting = classification == YoutubeChannelClassification.DISTRACTING
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            productive -> ChannelColors.productive(isLight).copy(alpha = 0.12f)
                            distracting -> ChannelColors.distracting(isLight).copy(alpha = 0.12f)
                            else -> secondaryText(isLight).copy(alpha = 0.10f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        productive -> Icons.Default.Check
                        distracting -> Icons.Default.Block
                        else -> Icons.Default.VisibilityOff
                    },
                    contentDescription = null,
                    tint = when {
                        productive -> ChannelColors.productive(isLight)
                        distracting -> ChannelColors.distracting(isLight)
                        else -> secondaryText(isLight)
                    },
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryText(isLight))
                Text(handle ?: "Verified channel", fontSize = 11.sp, color = secondaryText(isLight))
            }
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete channel",
                        tint = secondaryText(isLight).copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Productive in Deep Green
            OutlineChip(
                label = "Productive",
                accent = ChannelColors.productive(isLight),
                isLight = isLight,
                selected = productive,
                onClick = {
                    onClassificationChange(
                        if (productive) YoutubeChannelClassification.OTHERS else YoutubeChannelClassification.PRODUCTIVE,
                    )
                },
            )
            // Distracting in Maroon
            OutlineChip(
                label = "Distracting",
                accent = ChannelColors.distracting(isLight),
                isLight = isLight,
                selected = distracting,
                onClick = {
                    onClassificationChange(
                        if (distracting) YoutubeChannelClassification.OTHERS else YoutubeChannelClassification.DISTRACTING,
                    )
                },
            )
        }
    }
}

// ── Channel card group (setup suggestions) ───────────────────────────────────

@Composable
private fun ChannelCardGroup(
    channels: List<ResolvedYoutubeChannelDto>,
    classifications: Map<String, YoutubeChannelClassification>,
    isLight: Boolean,
    onClassificationChange: (ResolvedYoutubeChannelDto, YoutubeChannelClassification) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, secondaryText(isLight).copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
    ) {
        channels.forEachIndexed { index, channel ->
            val classification = classifications[channel.channelId] ?: YoutubeChannelClassification.OTHERS
            Column(Modifier.padding(horizontal = 14.dp)) {
                ChannelToggleRow(
                    name = channel.displayName,
                    handle = channel.handle,
                    classification = classification,
                    isLight = isLight,
                    onClassificationChange = { onClassificationChange(channel, it) },
                )
            }
            if (index != channels.lastIndex) {
                HorizontalDivider(color = secondaryText(isLight).copy(alpha = 0.08f))
            }
        }
    }
}

// ── Available channel list ────────────────────────────────────────────────────

@Composable
private fun AvailableChannelList(
    channels: List<ResolvedYoutubeChannelDto>,
    classifications: Map<String, YoutubeChannelClassification>,
    initialFilter: AvailableChannelFilter = AvailableChannelFilter.PRODUCTIVE,
    isLight: Boolean,
    onClassificationChange: (ResolvedYoutubeChannelDto, YoutubeChannelClassification) -> Unit,
    onDeleteChannel: (String) -> Unit = {},
) {
    var selectedFilter by rememberSaveable { mutableStateOf(initialFilter) }
    val visibleChannels = filterAvailableChannels(channels, classifications, selectedFilter)

    // Segmented control — Productive in Deep Green | Distracting in Maroon
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(secondaryText(isLight).copy(alpha = 0.08f))
            .padding(3.dp),
    ) {
        AvailableChannelFilter.entries.forEach { filter ->
            val selected = selectedFilter == filter
            val activeColor = when (filter) {
                AvailableChannelFilter.PRODUCTIVE -> ChannelColors.productive(isLight)
                AvailableChannelFilter.DISTRACTING -> ChannelColors.distracting(isLight)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .then(
                        if (selected) Modifier
                            .background(if (isLight) Color.White else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, activeColor.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                        else Modifier,
                    )
                    .clickable { selectedFilter = filter }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (filter) {
                        AvailableChannelFilter.PRODUCTIVE -> "Productive"
                        AvailableChannelFilter.DISTRACTING -> "Distracting"
                    },
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) activeColor else secondaryText(isLight),
                    maxLines = 1,
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    visibleChannels.forEachIndexed { index, channel ->
        val classification = classifications[channel.channelId] ?: YoutubeChannelClassification.OTHERS
        ChannelToggleRow(
            name = channel.displayName,
            handle = channel.handle,
            classification = classification,
            isLight = isLight,
            onClassificationChange = { onClassificationChange(channel, it) },
            onDelete = { onDeleteChannel(channel.channelId) },
        )
        if (index != visibleChannels.lastIndex) HorizontalDivider(color = secondaryText(isLight).copy(alpha = 0.08f))
    }
    if (visibleChannels.isEmpty()) {
        Text(
            when (selectedFilter) {
                AvailableChannelFilter.PRODUCTIVE -> "No productive channels found."
                AvailableChannelFilter.DISTRACTING -> "No distracting channels found."
            },
            fontSize = 13.sp,
            color = secondaryText(isLight),
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

// ── Expandable section ────────────────────────────────────────────────────────

@Composable
private fun ExpandableSection(
    title: String,
    subtitle: String,
    expanded: Boolean,
    isLight: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = primaryText(isLight))
                Text(subtitle, fontSize = 11.sp, color = secondaryText(isLight))
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = secondaryText(isLight),
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLight) Color(0xFFF8FAFC) else Color(0xFF0F172A))
                    .border(1.dp, secondaryText(isLight).copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) { content() }
        }
    }
}

// ── Shared primitives ─────────────────────────────────────────────────────────

/** Feature explanation row — icon + concise label. No text walls. */
@Composable
private fun FeatureRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    isLight: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = primaryText(isLight),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Pill showing permission / status. Green = ok, amber = needs action. */
@Composable
private fun StatusPill(icon: ImageVector, text: String, isOk: Boolean, isLight: Boolean) {
    val color = if (isOk) KavachCategoryColors.productive(isLight) else KavachCategoryColors.unclassified(isLight)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

/** Primary CTA — near-black fill in light, brand purple in dark. */
@Composable
private fun CtaButton(
    text: String,
    isLight: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = YTCMColors.ctaFill(isLight),
            contentColor = Color.White,
            disabledContainerColor = secondaryText(isLight).copy(alpha = 0.12f),
            disabledContentColor = secondaryText(isLight).copy(alpha = 0.50f),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun SectionLabel(text: String, isLight: Boolean) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = secondaryText(isLight),
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun YTCMDivider(isLight: Boolean) =
    HorizontalDivider(color = secondaryText(isLight).copy(alpha = 0.10f))

// ── Data helpers ──────────────────────────────────────────────────────────────

internal enum class AvailableChannelFilter { PRODUCTIVE, DISTRACTING }

private val STARTER_CHANNEL_HANDLES = listOf("@parmarssc", "@safarparmar")

internal fun starterChannels(channels: List<ResolvedYoutubeChannelDto>): List<ResolvedYoutubeChannelDto> =
    STARTER_CHANNEL_HANDLES.mapNotNull { starterHandle ->
        channels.firstOrNull { it.handle.equals(starterHandle, ignoreCase = true) }
    }

internal fun filterAvailableChannels(
    channels: List<ResolvedYoutubeChannelDto>,
    classifications: Map<String, YoutubeChannelClassification>,
    filter: AvailableChannelFilter,
): List<ResolvedYoutubeChannelDto> = channels.filter { channel ->
    when (filter) {
        AvailableChannelFilter.PRODUCTIVE -> classifications[channel.channelId] == YoutubeChannelClassification.PRODUCTIVE
        AvailableChannelFilter.DISTRACTING -> classifications[channel.channelId] == YoutubeChannelClassification.DISTRACTING
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(name = "Step 1 – Light", widthDp = 360, heightDp = 780, showBackground = true)
@Composable
private fun PreviewStep1Light() {
    SafarTheme {
        YoutubeStudyV2Content(
            state = YoutubeStudyV2UiState(setupStep = 1, accessibilityEnabled = false),
            isLight = true,
            onAgree = {}, onNotNow = {},
            onSetEnabled = {}, onOpenAccessibility = {},
            onReferenceChanged = {}, onAddChannel = {},
            onSetClassification = { _, _ -> },
            onToggleAvailable = {},
            onSetAvailableClassification = { _, _ -> },
            onDeleteChannel = {},
            onBackToStep1 = {}, onStart = {},
        )
    }
}
