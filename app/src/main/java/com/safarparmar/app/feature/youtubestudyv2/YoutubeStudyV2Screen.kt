package com.safarparmar.app.feature.youtubestudyv2

/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V5
 * redesign · genre: modern-minimal · theme: Cobalt (deep-black + royal-blue)
 * tokens: KavachDesign + YTCM palette · adaptive: 560dp column · isLight-flagged
 * states: default · loading · error · empty · permission-needed · permission-ok
 */

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.TvOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    isDarkTheme: Boolean = false,
    viewModel: YoutubeStudyV2ViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val isLight = !isDarkTheme

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

    val screenBg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    Scaffold(
        containerColor = screenBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "YouTube Focus",
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
                    containerColor = screenBg,
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
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentBottomPadding = 100.dp + navBarBottom

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = contentBottomPadding),
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
                "YouTube Focus",
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
            text = if (canStart) "Start YouTube Focus · ${state.allowed.size} allowed" else "Continue to Dashboard",
            isLight = isLight,
            enabled = true,
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
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentBottomPadding = 100.dp + navBarBottom

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Status Card with 2 stat chips
        item {
            ProtectionStatus(
                enabled = state.enabled,
                accessibilityEnabled = state.accessibilityEnabled,
                productiveCount = state.allowed.size,
                isLight = isLight,
                onSetEnabled = onSetEnabled,
                onOpenAccessibility = onOpenAccessibility,
            )
        }

        // 2. Add channel by handle (includes empty state box when allowed is empty)
        item {
            AddChannelCard(
                reference = state.reference,
                resolving = state.resolving,
                message = state.message,
                isError = state.isError,
                isLight = isLight,
                onReferenceChanged = onReferenceChanged,
                onAddChannel = onAddChannel,
                showEmptyState = false,
            )
        }

        // 3. Channel catalog — switchable Productive / Distracting tabs
        if (state.available.isNotEmpty() || state.classifications.isNotEmpty()) {
            item {
                val catalogBg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF18181B)
                val catalogBorder = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
                var catalogExpanded by rememberSaveable { mutableStateOf(true) }
                val catalogChevronRotation by animateFloatAsState(
                    targetValue = if (catalogExpanded) 180f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "catalogChevronRotation",
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = catalogBg,
                    border = BorderStroke(1.dp, catalogBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { catalogExpanded = !catalogExpanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = secondaryText(isLight),
                                modifier = Modifier.size(20.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Channel catalog",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText(isLight),
                                )
                                val prodCount = state.classifications.values
                                    .count { it == YoutubeChannelClassification.PRODUCTIVE }
                                val distCount = state.classifications.values
                                    .count { it == YoutubeChannelClassification.DISTRACTING }
                                Text(
                                    text = "$prodCount productive · $distCount distracting",
                                    fontSize = 12.sp,
                                    color = secondaryText(isLight),
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (catalogExpanded) "Collapse" else "Expand",
                                tint = secondaryText(isLight),
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = catalogChevronRotation },
                            )
                        }

                        AnimatedVisibility(
                            visible = catalogExpanded,
                            enter = fadeIn(tween(180)) + expandVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            ),
                            exit = fadeOut(tween(120)) + shrinkVertically(
                                animationSpec = tween(180),
                            ),
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f),
                                )
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    AvailableChannelList(
                                        channels = state.available,
                                        classifications = state.classifications,
                                        isLight = isLight,
                                        onClassificationChange = onSetAvailableClassification,
                                        onDeleteChannel = onDeleteChannel,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. «Protection stopping automatically?» warning card
        item {
            TroubleshootingWarningCard(
                expanded = reliabilityExpanded,
                isLight = isLight,
                onToggle = { reliabilityExpanded = !reliabilityExpanded },
            )
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
    val oliveDeepGreen = if (isLight) Color(0xFF2E5A27) else Color(0xFF4ADE80)
    val targetCardBg = if (active) {
        if (isLight) Color(0xFFF0F5EE) else Color(0xFF0F1E12)
    } else {
        if (isLight) Color(0xFFF1F5F9) else Color(0xFF18181B)
    }
    val targetCardBorder = if (active) {
        if (isLight) Color(0xFF2E5A27).copy(alpha = 0.30f) else Color(0xFF4ADE80).copy(alpha = 0.25f)
    } else {
        if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    }
    val cardBg by animateColorAsState(targetCardBg, animationSpec = tween(250), label = "statusCardBg")
    val cardBorder by animateColorAsState(targetCardBorder, animationSpec = tween(250), label = "statusCardBorder")
    val iconTint by animateColorAsState(
        targetValue = if (active) oliveDeepGreen else secondaryText(isLight),
        animationSpec = tween(250),
        label = "statusIconTint",
    )
    val titleColor by animateColorAsState(
        targetValue = if (active) oliveDeepGreen else primaryText(isLight),
        animationSpec = tween(250),
        label = "statusTitleColor",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = if (active) "Focus active" else "Focus off",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                modifier = Modifier.weight(1f),
            )
            Box(contentAlignment = Alignment.Center) {
                Switch(
                    checked = active,
                    onCheckedChange = { checked ->
                        if (!accessibilityEnabled) {
                            onOpenAccessibility()
                        } else {
                            onSetEnabled(checked)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = if (isLight) Color(0xFF2E5A27) else Color(0xFF3F6212),
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155),
                        uncheckedBorderColor = Color.Transparent,
                    ),
                )
                if (!accessibilityEnabled) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onOpenAccessibility,
                            ),
                    )
                }
            }
        }

        // Two clear stat chips side-by-side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatChip(
                label = "Shorts",
                value = if (active) "Blocked" else "Off",
                isLight = isLight,
                modifier = Modifier.weight(1f),
            )
            StatChip(
                label = "Channels allowed",
                value = "$productiveCount",
                isLight = isLight,
                modifier = Modifier.weight(1f),
            )
        }

        if (!accessibilityEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f))
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

@Composable
private fun StatChip(
    label: String,
    value: String,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    val chipBg = if (isLight) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.35f)
    val chipBorder = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryText(isLight),
            maxLines = 1,
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText(isLight),
            maxLines = 1,
        )
    }
}

// ── Add Channel Card & Empty State ────────────────────────────────────────────

@Composable
private fun AddChannelCard(
    reference: String,
    resolving: Boolean,
    message: String?,
    isError: Boolean,
    isLight: Boolean,
    onReferenceChanged: (String) -> Unit,
    onAddChannel: () -> Unit,
    showEmptyState: Boolean,
) {
    val cardBg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF18181B)
    val cardBorder = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Add channel by handle",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = primaryText(isLight),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = reference,
                onValueChange = onReferenceChanged,
                placeholder = {
                    Text(
                        "name@channelhandle",
                        fontSize = 13.5.sp,
                        color = secondaryText(isLight).copy(alpha = 0.7f),
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isLight) Color(0xFF0F172A) else Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = secondaryText(isLight).copy(alpha = 0.20f),
                    focusedContainerColor = if (isLight) Color.White else Color(0xFF121316),
                    unfocusedContainerColor = if (isLight) Color.White else Color(0xFF121316),
                ),
            )

            // Dedicated "+" Action Button
            val targetButtonBg = when {
                reference.isNotBlank() -> if (isLight) Color(0xFF0F172A) else Color.White
                isLight -> Color.White
                else -> Color(0xFF27272A)
            }
            val targetButtonBorder = when {
                reference.isNotBlank() -> if (isLight) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.40f)
                isLight -> Color.Black.copy(alpha = 0.12f)
                else -> Color.White.copy(alpha = 0.15f)
            }
            val targetIconTint = when {
                reference.isNotBlank() -> if (isLight) Color.White else Color.Black
                else -> secondaryText(isLight).copy(alpha = 0.6f)
            }
            val buttonBg by animateColorAsState(targetButtonBg, animationSpec = tween(200), label = "addBtnBg")
            val buttonBorder by animateColorAsState(targetButtonBorder, animationSpec = tween(200), label = "addBtnBorder")
            val iconTint by animateColorAsState(targetIconTint, animationSpec = tween(200), label = "addBtnIconTint")

            Surface(
                onClick = onAddChannel,
                enabled = reference.isNotBlank() && !resolving,
                shape = RoundedCornerShape(12.dp),
                color = buttonBg,
                border = BorderStroke(1.dp, buttonBorder),
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (resolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = iconTint,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add channel",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(tween(180)) + expandVertically(animationSpec = tween(180)),
            exit = fadeOut(tween(120)) + shrinkVertically(animationSpec = tween(120)),
        ) {
            message?.let {
                val msgColor = when {
                    isError -> MaterialTheme.colorScheme.error
                    it.contains("Distracting", ignoreCase = true) -> ChannelColors.distracting(isLight)
                    else -> ChannelColors.productive(isLight)
                }
                Text(
                    it,
                    fontSize = 12.sp,
                    color = msgColor,
                )
            }
        }

        // Empty state box below input if no productive channels yet
        if (showEmptyState) {
            val emptyBoxBg = if (isLight) Color(0xFFF1F5F9) else Color(0xFF121316)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(emptyBoxBg)
                    .border(
                        1.dp,
                        if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.TvOff,
                    contentDescription = null,
                    tint = secondaryText(isLight).copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "No productive channels yet",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText(isLight),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Add a handle above to allow it through",
                    fontSize = 12.sp,
                    color = secondaryText(isLight),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Allowed Channel Card ──────────────────────────────────────────────────────

@Composable
private fun ChannelAllowedCard(
    name: String,
    handle: String?,
    isLight: Boolean,
    onDelete: () -> Unit,
) {
    val cardBg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF18181B)
    val cardBorder = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelInitialsAvatar(
                name = handle ?: name,
                backgroundColor = Color(0xFF1E3A8A),
                textColor = Color(0xFF93C5FD),
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = handle ?: name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText(isLight),
                    maxLines = 1,
                )
                if (!handle.isNullOrBlank() && handle != name) {
                    Text(
                        text = name,
                        fontSize = 11.5.sp,
                        color = secondaryText(isLight),
                        maxLines = 1,
                    )
                }
            }

            // Square remove button
            Surface(
                onClick = onDelete,
                shape = RoundedCornerShape(10.dp),
                color = if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(
                    1.dp,
                    if (isLight) Color.Black.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.12f),
                ),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove channel",
                        tint = secondaryText(isLight),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Your YouTube Channels Card ────────────────────────────────────────────────

@Composable
private fun YourChannelsCard(
    channels: List<ResolvedYoutubeChannelDto>,
    classifications: Map<String, YoutubeChannelClassification>,
    expanded: Boolean,
    isLight: Boolean,
    onToggle: () -> Unit,
    onSetClassification: (ResolvedYoutubeChannelDto, YoutubeChannelClassification) -> Unit,
) {
    val cardBg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF18181B)
    val cardBorder = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = secondaryText(isLight),
                    modifier = Modifier.size(20.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Your YouTube channels",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText(isLight),
                    )
                    Text(
                        text = "${channels.size} channels found",
                        fontSize = 12.sp,
                        color = secondaryText(isLight),
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = secondaryText(isLight),
                    modifier = Modifier.size(20.dp),
                )
            }

            if (expanded) {
                HorizontalDivider(
                    color = if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.06f),
                )
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    channels.forEach { channel ->
                        val isAllowed = classifications[channel.channelId] == YoutubeChannelClassification.PRODUCTIVE
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ChannelInitialsAvatar(
                                name = channel.handle,
                                backgroundColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF27272A),
                                textColor = primaryText(isLight),
                                modifier = Modifier.size(32.dp),
                            )

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = channel.handle,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryText(isLight),
                                    maxLines = 1,
                                )
                                if (channel.displayName.isNotBlank() && channel.displayName != channel.handle) {
                                    Text(
                                        text = channel.displayName,
                                        fontSize = 11.sp,
                                        color = secondaryText(isLight),
                                        maxLines = 1,
                                    )
                                }
                            }

                            if (isAllowed) {
                                Text(
                                    text = "Allowed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ChannelColors.productive(isLight),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onSetClassification(channel, YoutubeChannelClassification.OTHERS)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            } else {
                                Surface(
                                    onClick = {
                                        onSetClassification(channel, YoutubeChannelClassification.PRODUCTIVE)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isLight) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.25f),
                                    ),
                                ) {
                                    Text(
                                        text = "Allow",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText(isLight),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (channels.isEmpty()) {
                        Text(
                            text = "No channels found yet. Open YouTube to discover your visited channels.",
                            fontSize = 12.sp,
                            color = secondaryText(isLight),
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── Troubleshooting Warning Card ──────────────────────────────────────────────

@Composable
private fun TroubleshootingWarningCard(
    expanded: Boolean,
    isLight: Boolean,
    onToggle: () -> Unit,
) {
    val warningBg = if (isLight) Color(0xFFFFFBEB) else Color(0xFF2E1905)
    val warningBorder = if (isLight) Color(0xFFF59E0B).copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.35f)
    val warningAmber = Color(0xFFF59E0B)
    val warningChevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "warningChevronRotation",
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = warningBg,
        border = BorderStroke(1.dp, warningBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = warningAmber,
                    modifier = Modifier.size(20.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Protection stopping automatically?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryText(isLight),
                    )
                    Text(
                        text = "Fix battery settings for your phone",
                        fontSize = 12.sp,
                        color = if (isLight) Color(0xFFB45309) else Color(0xFFFCD34D),
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = warningAmber,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = warningChevronRotation },
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(180)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
                exit = fadeOut(tween(120)) + shrinkVertically(
                    animationSpec = tween(180),
                ),
            ) {
                Column {
                    HorizontalDivider(color = warningAmber.copy(alpha = 0.15f))
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "• Xiaomi / Redmi: Turn on Autostart & set Battery Saver to No Restrictions",
                            fontSize = 12.sp,
                            color = if (isLight) Color(0xFF78350F) else Color(0xFFFDE68A),
                        )
                        Text(
                            "• Realme / OPPO: Turn on Auto Launch & Background Running",
                            fontSize = 12.sp,
                            color = if (isLight) Color(0xFF78350F) else Color(0xFFFDE68A),
                        )
                        Text(
                            "• Samsung: Remove SAFAR from Sleeping Apps list",
                            fontSize = 12.sp,
                            color = if (isLight) Color(0xFF78350F) else Color(0xFFFDE68A),
                        )
                        Text(
                            "• OnePlus: Lock SAFAR in Recent Apps & disable battery optimization",
                            fontSize = 12.sp,
                            color = if (isLight) Color(0xFF78350F) else Color(0xFFFDE68A),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelInitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF1E3A8A),
    textColor: Color = Color(0xFF93C5FD),
) {
    val initials = remember(name) {
        val clean = name.removePrefix("@").trim()
        val parts = clean.split(" ", "_", "-", ".").filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            clean.length >= 2 -> clean.take(2).uppercase()
            clean.isNotEmpty() -> clean.take(1).uppercase()
            else -> "YT"
        }
    }
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

// ── Channel colors ──────────────────────────────────────────────────────────

internal object ChannelColors {
    /** Deep Green / Olive */
    fun productive(isLight: Boolean) =
        if (isLight) Color(0xFF2E5A27) else Color(0xFF4D7C0F)

    /** Maroon */
    fun distracting(isLight: Boolean) =
        if (isLight) Color(0xFF881337) else Color(0xFFFB7185)
}

// ── Channel toggle row & animated chip ───────────────────────────────────────

@Composable
private fun AnimatedChannelChip(
    label: String,
    accent: Color,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
) {
    val chipBg by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = if (isLight) 0.14f else 0.20f) else Color.Transparent,
        animationSpec = tween(200),
        label = "chipBg",
    )
    val chipBorder by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.70f) else secondaryText(isLight).copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "chipBorder",
    )
    val chipTextColor by animateColorAsState(
        targetValue = if (selected) accent else secondaryText(isLight),
        animationSpec = tween(200),
        label = "chipTextColor",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipBg)
            .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = chipTextColor,
        )
    }
}

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
    val avatarBg by animateColorAsState(
        targetValue = when {
            productive -> ChannelColors.productive(isLight).copy(alpha = 0.12f)
            distracting -> ChannelColors.distracting(isLight).copy(alpha = 0.12f)
            else -> secondaryText(isLight).copy(alpha = 0.10f)
        },
        animationSpec = tween(200),
        label = "avatarBg",
    )
    val avatarIconTint by animateColorAsState(
        targetValue = when {
            productive -> ChannelColors.productive(isLight)
            distracting -> ChannelColors.distracting(isLight)
            else -> secondaryText(isLight)
        },
        animationSpec = tween(200),
        label = "avatarIconTint",
    )

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
                    .background(avatarBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        productive -> Icons.Default.Check
                        distracting -> Icons.Default.Block
                        else -> Icons.Default.VisibilityOff
                    },
                    contentDescription = null,
                    tint = avatarIconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText(isLight),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = handle ?: "Verified channel",
                    fontSize = 11.sp,
                    color = secondaryText(isLight),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
            AnimatedChannelChip(
                label = "Productive",
                accent = ChannelColors.productive(isLight),
                selected = productive,
                isLight = isLight,
                onClick = {
                    onClassificationChange(
                        if (productive) YoutubeChannelClassification.OTHERS else YoutubeChannelClassification.PRODUCTIVE,
                    )
                },
            )
            // Distracting in Maroon
            AnimatedChannelChip(
                label = "Distracting",
                accent = ChannelColors.distracting(isLight),
                selected = distracting,
                isLight = isLight,
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
            val tabBg by animateColorAsState(
                targetValue = if (selected) {
                    if (isLight) Color.White else MaterialTheme.colorScheme.surfaceVariant
                } else Color.Transparent,
                animationSpec = tween(200),
                label = "tabBg",
            )
            val tabBorder by animateColorAsState(
                targetValue = if (selected) activeColor.copy(alpha = 0.35f) else Color.Transparent,
                animationSpec = tween(200),
                label = "tabBorder",
            )
            val tabTextColor by animateColorAsState(
                targetValue = if (selected) activeColor else secondaryText(isLight),
                animationSpec = tween(200),
                label = "tabTextColor",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tabBg)
                    .border(1.dp, tabBorder, RoundedCornerShape(9.dp))
                    .clickable { selectedFilter = filter }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (filter) {
                        AvailableChannelFilter.PRODUCTIVE -> "Productive"
                        AvailableChannelFilter.DISTRACTING -> "Distracting"
                    },
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = tabTextColor,
                    maxLines = 1,
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    AnimatedContent(
        targetState = selectedFilter,
        transitionSpec = {
            if (targetState == AvailableChannelFilter.DISTRACTING) {
                (slideInHorizontally { width -> width / 4 } + fadeIn(tween(180)))
                    .togetherWith(slideOutHorizontally { width -> -width / 4 } + fadeOut(tween(140)))
            } else {
                (slideInHorizontally { width -> -width / 4 } + fadeIn(tween(180)))
                    .togetherWith(slideOutHorizontally { width -> width / 4 } + fadeOut(tween(140)))
            }
        },
        label = "availableChannelListTabTransition",
    ) { currentTab ->
        val currentChannels = filterAvailableChannels(channels, classifications, currentTab)
        Column {
            currentChannels.forEachIndexed { index, channel ->
                val classification = classifications[channel.channelId] ?: YoutubeChannelClassification.OTHERS
                ChannelToggleRow(
                    name = channel.displayName,
                    handle = channel.handle,
                    classification = classification,
                    isLight = isLight,
                    onClassificationChange = { onClassificationChange(channel, it) },
                    onDelete = { onDeleteChannel(channel.channelId) },
                )
                if (index != currentChannels.lastIndex) HorizontalDivider(color = secondaryText(isLight).copy(alpha = 0.08f))
            }
            if (currentChannels.isEmpty()) {
                Text(
                    text = when (currentTab) {
                        AvailableChannelFilter.PRODUCTIVE -> "No productive channels found."
                        AvailableChannelFilter.DISTRACTING -> "No distracting channels found."
                    },
                    fontSize = 13.sp,
                    color = secondaryText(isLight),
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
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
