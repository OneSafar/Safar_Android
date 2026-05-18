package com.safar.app.ui.launch

import android.os.Build
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.data.local.SafarDataStore
import com.safar.app.notifications.rememberNotificationPermissionRequester
import com.safar.app.ui.ekagra.focusshield.FocusShieldConsentDialog
import com.safar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import com.safar.app.ui.ekagra.focusshield.PermissionGuideSheet
import com.safar.app.ui.ekagra.focusshield.PermissionTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchUsageQuestionnaireScreen(
    dataStore: SafarDataStore,
    onNavigateHome: () -> Unit,
    onUnauthorized: () -> Unit,
    viewModel: LaunchUsageQuestionnaireViewModel = hiltViewModel(),
) {
    val isLoggedIn by dataStore.isLoggedIn.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false) onUnauthorized()
    }

    if (isLoggedIn != true) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading…", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reasons = listOf(
        "I want to study without distractions",
        "I want to track my goals and stay consistent",
        "I want to journal my thoughts and feelings",
        "I want to manage my stress and mental wellness",
        "All of the above"
    )

    // Indices for focus setup: 0, 1, 4 (Study, Goals, All)
    val needsFocusSetup = remember(uiState.selectedReasons) {
        uiState.selectedReasons.any { it in setOf(0, 1, 4) }
    }

    var page by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<String?>(null) }

    var hasUsage by remember { mutableStateOf(FocusShieldPermissionHelper.hasUsageStatsPermission(context)) }
    var hasA11y by remember { mutableStateOf(FocusShieldPermissionHelper.hasAccessibilityService(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsage = FocusShieldPermissionHelper.hasUsageStatsPermission(context)
                hasA11y = FocusShieldPermissionHelper.hasAccessibilityService(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showShieldConsent by remember { mutableStateOf(false) }
    var showUsageConsent by remember { mutableStateOf(false) }
    var showA11yConsent by remember { mutableStateOf(false) }
    var awaitingUsageGrant by remember { mutableStateOf(false) }
    var awaitingA11yGrant by remember { mutableStateOf(false) }
    var pendingDirectUsageSettings by remember { mutableStateOf(false) }

    var strictPipelineAfterNotif by remember { mutableStateOf(false) }

    fun finishStandard() {
        viewModel.markQuestionnaireFinished(AppUsageMode.STANDARD, onNavigateHome)
    }

    fun finishFocused() {
        viewModel.markQuestionnaireFinished(AppUsageMode.FOCUSED, onNavigateHome)
    }

    fun enableShieldAndFinishFocused() {
        viewModel.setFocusShieldEnabled(true)
        finishFocused()
    }

    val requestNotification = rememberNotificationPermissionRequester { _ ->
        if (strictPipelineAfterNotif) {
            strictPipelineAfterNotif = false
            showShieldConsent = true
        } else {
            finishStandard()
        }
    }

    fun onFinishQuestionnaire() {
        val mode = selectedMode ?: (if (needsFocusSetup) AppUsageMode.FOCUSED else AppUsageMode.STANDARD)
        if (mode == AppUsageMode.STANDARD) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotification()
            } else {
                finishStandard()
            }
            return
        }
        strictPipelineAfterNotif = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotification()
        } else {
            strictPipelineAfterNotif = false
            showShieldConsent = true
        }
    }

    val scheme = MaterialTheme.colorScheme

    fun beginFocusedPermissionPipeline() {
        if (selectedMode == null) selectedMode = AppUsageMode.FOCUSED
        onFinishQuestionnaire()
    }

    LaunchedEffect(page) {
        if (page == 1 && selectedMode == null) {
            selectedMode = AppUsageMode.FOCUSED
        }
    }

    val topBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = scheme.surface,
        titleContentColor = scheme.primary,
        navigationIconContentColor = scheme.primary,
    )

    Scaffold(
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            when (page) {
                1 -> CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "SAFAR",
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { page-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = topBarColors,
                )
                else -> TopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (page == 2) "Focus Setup" else "Welcome",
                                fontWeight = FontWeight.Bold,
                            )
                            if (page == 2) {
                                Spacer(Modifier.height(6.dp))
                                QuestionnaireProgressRow(activePage = page, accent = scheme.primary, track = scheme.outline)
                            }
                        }
                    },
                    navigationIcon = {
                        if (page > 0) {
                            IconButton(onClick = { page-- }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = scheme.onBackground,
                        navigationIconContentColor = scheme.onBackground,
                    ),
                )
            }
        },
        bottomBar = {
            if (page == 2) {
                FocusSetupBottomActions(
                    onOpenSettings = {
                        pendingDirectUsageSettings = true
                        beginFocusedPermissionPipeline()
                    },
                    onCancel = {
                        selectedMode = AppUsageMode.STANDARD
                        pendingDirectUsageSettings = false
                        onFinishQuestionnaire()
                    },
                )
            } else if (page == 1) {
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val density = androidx.compose.ui.platform.LocalDensity.current
                val isCompactLayout = configuration.screenHeightDp < 700 || density.fontScale > 1.15f
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            start = if (isCompactLayout) 20.dp else 0.dp,
                            end = 20.dp, 
                            bottom = if (isCompactLayout) 16.dp else 32.dp
                        ),
                    contentAlignment = if (isCompactLayout) Alignment.BottomCenter else Alignment.BottomEnd,
                ) {
                    Button(
                        onClick = {
                            if (selectedMode == AppUsageMode.FOCUSED || (selectedMode == null && needsFocusSetup)) {
                                if (selectedMode == null) selectedMode = AppUsageMode.FOCUSED
                                page = 2
                            } else {
                                onFinishQuestionnaire()
                            }
                        },
                        enabled = selectedMode != null,
                        modifier = Modifier
                            .height(if (isCompactLayout) 56.dp else 64.dp)
                            .then(if (isCompactLayout) Modifier.fillMaxWidth() else Modifier.widthIn(min = 180.dp))
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(999.dp),
                                spotColor = scheme.primary.copy(alpha = 0.35f),
                            ),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ),
                    ) {
                        Text("Next", fontSize = if (isCompactLayout) 18.sp else 20.sp, fontWeight = FontWeight.SemiBold)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(if (isCompactLayout) 20.dp else 24.dp),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, scheme.background),
                                startY = 0f,
                                endY = 80f,
                            ),
                        )
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { page = 1 },
                        enabled = uiState.selectedReasons.isNotEmpty(),
                        modifier = Modifier
                            .widthIn(max = 380.dp)
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ),
                    ) {
                        Text("Next", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            Modifier.padding(start = 8.dp).size(20.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (page) {
            1 -> ModeSelectionScreen(
                selectedMode = selectedMode,
                onSelectMode = { selectedMode = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (page < 2) {
                    QuestionnaireProgressRow(activePage = page, accent = scheme.primary, track = scheme.outline)
                }

                when (page) {
                0 -> {
                    Text(
                        "What brings you to SAFAR today?",
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Select all that apply to personalize your experience.",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reasons.forEachIndexed { index, reason ->
                            ReasonChip(
                                text = reason,
                                selected = index in uiState.selectedReasons,
                                onClick = { viewModel.toggleReason(index) }
                            )
                        }
                    }
                }
                2 -> {
                    FocusSetupMergedPage()
                }
                }

                Spacer(Modifier.height(32.dp))
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showShieldConsent) {
        FocusShieldConsentDialog(
            onDismiss = {
                showShieldConsent = false
                finishFocused()
            },
            onConfirm = {
                showShieldConsent = false
                when {
                    !FocusShieldPermissionHelper.hasUsageStatsPermission(context) -> {
                        if (pendingDirectUsageSettings) {
                            pendingDirectUsageSettings = false
                            awaitingUsageGrant = true
                            FocusShieldPermissionHelper.openUsageAccessSettings(context)
                        } else {
                            showUsageConsent = true
                        }
                    }
                    !FocusShieldPermissionHelper.hasAccessibilityService(context) -> showA11yConsent = true
                    else -> enableShieldAndFinishFocused()
                }
            },
        )
    }

    if (showUsageConsent) {
        PermissionGuideSheet(
            permission = PermissionTarget.USAGE_STATS,
            onDismiss = {
                showUsageConsent = false
                if (!FocusShieldPermissionHelper.hasAccessibilityService(context)) showA11yConsent = true
                else finishFocused()
            },
            onOpenSettings = {
                showUsageConsent = false
                awaitingUsageGrant = true
                FocusShieldPermissionHelper.openUsageAccessSettings(context)
            },
        )
    }

    if (showA11yConsent) {
        PermissionGuideSheet(
            permission = PermissionTarget.ACCESSIBILITY,
            onDismiss = {
                showA11yConsent = false
                finishFocused()
            },
            onOpenSettings = {
                showA11yConsent = false
                awaitingA11yGrant = true
                FocusShieldPermissionHelper.openAccessibilitySettings(context)
            },
        )
    }

    LaunchedEffect(hasUsage, awaitingUsageGrant) {
        if (awaitingUsageGrant && FocusShieldPermissionHelper.hasUsageStatsPermission(context)) {
            awaitingUsageGrant = false
            if (!FocusShieldPermissionHelper.hasAccessibilityService(context)) showA11yConsent = true
            else enableShieldAndFinishFocused()
        }
    }

    LaunchedEffect(hasA11y, awaitingA11yGrant) {
        if (awaitingA11yGrant && FocusShieldPermissionHelper.hasAccessibilityService(context)) {
            awaitingA11yGrant = false
            if (FocusShieldPermissionHelper.hasUsageStatsPermission(context)) {
                enableShieldAndFinishFocused()
            } else {
                finishFocused()
            }
        }
    }
}

@Composable
private fun ReasonChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bgColor = if (selected) scheme.secondaryContainer else scheme.surface
    val borderColor = if (selected) scheme.primary else scheme.outlineVariant
    val textColor = scheme.onSurface
    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun ModeSelectionScreen(
    selectedMode: String?,
    onSelectMode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val constrainedHeight = maxHeight < 700.dp
        val largeFont = fontScale > 1.15f

        if (constrainedHeight || largeFont) {
            ModeSelectionCompactList(
                selectedMode = selectedMode,
                onSelectMode = onSelectMode,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ModeSelectionSplitScreenLarge(
                selectedMode = selectedMode,
                onSelectMode = onSelectMode,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ModeSelectionCompactList(
    selectedMode: String?,
    onSelectMode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ModeSelectionCompactCard(
                title = "Beast Mode",
                description = "Kavach with clear App Usage Permission, Ekagra Mode Accessibility, and notification prompts. You decide what to grant.",
                icon = Icons.Default.Shield,
                selected = selectedMode == AppUsageMode.FOCUSED,
                primaryStyle = true,
                onClick = { onSelectMode(AppUsageMode.FOCUSED) }
            )
        }
        item {
            ModeSelectionCompactCard(
                title = "Normal",
                description = "Notifications only. You can enable Kavach later from the menu.",
                icon = Icons.Default.Notifications,
                selected = selectedMode == AppUsageMode.STANDARD,
                primaryStyle = false,
                onClick = { onSelectMode(AppUsageMode.STANDARD) }
            )
        }
        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ModeSelectionCompactCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    primaryStyle: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val borderColor = if (selected) {
        if (primaryStyle) scheme.primary else scheme.onSurfaceVariant
    } else {
        scheme.outlineVariant.copy(alpha = 0.5f)
    }
    val bgColor = if (selected) {
        if (primaryStyle) scheme.secondaryContainer else scheme.surfaceVariant
    } else {
        scheme.surface
    }
    val iconTint = if (primaryStyle) scheme.primary else scheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(scheme.background)
                .border(1.5.dp, if (selected) borderColor else scheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(30.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (primaryStyle) scheme.primary else scheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = scheme.onSurfaceVariant,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (primaryStyle) scheme.primary else scheme.onSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = scheme.surface,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(Modifier.size(32.dp))
        }
    }
}

@Composable
private fun ModeSelectionSplitScreenLarge(
    selectedMode: String?,
    onSelectMode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        ModeSelectionHalf(
            title = "Beast Mode",
            description = "Kavach with clear App Usage Permission, Ekagra Mode Accessibility, and notification prompts. You decide what to grant.",
            icon = Icons.Default.Shield,
            selected = selectedMode == AppUsageMode.FOCUSED,
            primaryStyle = true,
            showBottomDivider = true,
            onClick = { onSelectMode(AppUsageMode.FOCUSED) },
            modifier = Modifier.weight(1f),
        )
        ModeSelectionHalf(
            title = "Normal",
            description = "Notifications only. You can enable Kavach later from the menu.",
            icon = Icons.Default.Notifications,
            selected = selectedMode == AppUsageMode.STANDARD,
            primaryStyle = false,
            showBottomDivider = false,
            onClick = { onSelectMode(AppUsageMode.STANDARD) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeSelectionHalf(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    primaryStyle: Boolean,
    showBottomDivider: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val iconBorderColor = if (primaryStyle) scheme.primary else scheme.outlineVariant
    val iconTint = if (primaryStyle) scheme.primary else scheme.onSurfaceVariant
    val titleColor = if (primaryStyle) scheme.primary else scheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (selected) scheme.secondaryContainer
                    else Color.Transparent,
                ),
        )
        if (showBottomDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(scheme.outlineVariant),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.08f))
                    .clip(CircleShape)
                    .background(scheme.surface)
                    .border(2.dp, iconBorderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = description,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (primaryStyle) scheme.primary else scheme.outlineVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionnaireProgressRow(activePage: Int, accent: Color, track: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (i <= activePage) accent else track.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun FocusSetupMergedPage() {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.secondaryContainer)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(scheme.surface)
                    .border(1.dp, scheme.outline.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                "To help you stay focused, SAFAR needs to know which apps you're using. This helps us gently remind you if you drift away during study time.",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSecondaryContainer,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "How to allow access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onBackground,
            )
            Box {
                Box(
                    Modifier
                        .padding(start = 15.dp)
                        .width(1.dp)
                        .height(132.dp)
                        .background(scheme.outline.copy(alpha = 0.45f)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    FocusSetupStep(
                        number = 1,
                        title = "Find SAFAR",
                        subtitle = "Look for SAFAR in the list of apps.",
                        scheme = scheme,
                    )
                    FocusSetupStep(
                        number = 2,
                        title = "Tap \"Allow usage access\"",
                        subtitle = "Toggle the switch to permit access.",
                        scheme = scheme,
                    )
                    FocusSetupStep(
                        number = 3,
                        title = "Press back",
                        subtitle = "Return to SAFAR to complete setup.",
                        scheme = scheme,
                    )
                }
            }
        }

        Text(
            "SAFAR never reads what's inside any app. The package name of the open app is enough to know when a blocked app appears.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun FocusSetupStep(
    number: Int,
    title: String,
    subtitle: String,
    scheme: ColorScheme,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(scheme.surface)
                .border(1.dp, scheme.outline.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurfaceVariant,
            )
        }
        Column(Modifier.padding(top = 2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FocusSetupBottomActions(
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Open Settings", fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(8.dp))
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Cancel", fontWeight = FontWeight.SemiBold, color = scheme.primary)
        }
    }
}
