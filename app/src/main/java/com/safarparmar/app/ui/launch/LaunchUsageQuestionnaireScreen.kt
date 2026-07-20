package com.safarparmar.app.ui.launch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.glass.LiquidGlassBackdrop
import com.safarparmar.app.ui.glass.SafarGlassPalette
import com.safarparmar.app.ui.glass.liquidGlass
import com.safarparmar.app.ui.theme.isLightBackground

private data class UsageReasonOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
)

private data class KavachModeOption(
    val mode: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val recommended: Boolean = false,
)

private val usageReasons = listOf(
    UsageReasonOption(
        title = "Study without distractions",
        subtitle = "Block apps and stay on track",
        icon = Icons.Default.AutoStories,
        accent = SafarGlassPalette.Violet,
    ),
    UsageReasonOption(
        title = "Track goals",
        subtitle = "Build habits and stay consistent",
        icon = Icons.Default.TrackChanges,
        accent = SafarGlassPalette.Pink,
    ),
    UsageReasonOption(
        title = "Journal my thoughts",
        subtitle = "Write and reflect each day",
        icon = Icons.Default.EditNote,
        accent = SafarGlassPalette.Coral,
    ),
    UsageReasonOption(
        title = "Manage stress",
        subtitle = "Take care of my mental health",
        icon = Icons.Default.SelfImprovement,
        accent = SafarGlassPalette.Lavender,
    ),
    UsageReasonOption(
        title = "All of the above",
        subtitle = "I want the full SAFAR experience",
        icon = Icons.Default.CheckCircle,
        accent = SafarGlassPalette.Violet,
    ),
)

private val kavachModes = listOf(
    KavachModeOption(
        mode = AppUsageMode.BEAST,
        title = "Beast Mode",
        description = "Full lockdown. KAVACH stays on — no quick unlock.",
        icon = Icons.Rounded.Lock,
        accent = Color(0xFFFF5722),
        recommended = true,
    ),
    KavachModeOption(
        mode = AppUsageMode.ALWAYS_ON,
        title = "Always On",
        description = "KAVACH blocks apps everywhere until you turn it off.",
        icon = Icons.Rounded.ShieldMoon,
        accent = SafarGlassPalette.Violet,
    ),
    KavachModeOption(
        mode = AppUsageMode.FOCUSED,
        title = "Normal",
        description = "KAVACH sends you back to Ekagra when you open a blocked app.",
        icon = Icons.Rounded.TouchApp,
        accent = Color(0xFF26A69A),
    ),
)

@Composable
fun LaunchUsageQuestionnaireScreen(
    dataStore: SafarDataStore,
    onNavigateHome: () -> Unit,
    onNavigateKavach: () -> Unit,
    onUnauthorized: () -> Unit,
    viewModel: LaunchUsageQuestionnaireViewModel = hiltViewModel(),
) {
    val isLoggedIn by dataStore.isLoggedIn.collectAsStateWithLifecycle(initialValue = null)

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
    var page by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(page) {
        if (page == 1 && selectedMode == null) {
            selectedMode = AppUsageMode.BEAST
        }
    }

    fun onFinishQuestionnaire() {
        val mode = selectedMode ?: AppUsageMode.BEAST
        viewModel.markQuestionnaireFinished(mode, onNavigateKavach)
    }

    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val primaryText = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary
    val secondaryText = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary

    Box(Modifier.fillMaxSize()) {
        LiquidGlassBackdrop(modifier = Modifier.fillMaxSize(), isLight = isLight)

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                QuestionnaireTopBar(
                    page = page,
                    isLight = isLight,
                    primaryText = primaryText,
                    onBack = { if (page > 0) page-- },
                )
            },
            bottomBar = {
                QuestionnaireBottomBar(
                    page = page,
                    canContinue = when (page) {
                        0 -> uiState.selectedReasons.isNotEmpty()
                        else -> selectedMode != null
                    },
                    isLight = isLight,
                    onBack = { if (page > 0) page-- },
                    onContinue = {
                        if (page == 0) page = 1 else onFinishQuestionnaire()
                    },
                )
            },
        ) { padding ->
            AnimatedContent(
                targetState = page,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it / 4 } + fadeIn(tween(280)))
                            .togetherWith(slideOutHorizontally { -it / 4 } + fadeOut(tween(220)))
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn(tween(280)))
                            .togetherWith(slideOutHorizontally { it / 4 } + fadeOut(tween(220)))
                    }
                },
                label = "questionnairePage",
            ) { currentPage ->
                when (currentPage) {
                    0 -> WhyHerePage(
                        selectedReasons = uiState.selectedReasons,
                        onToggleReason = viewModel::toggleReason,
                        isLight = isLight,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                    )
                    else -> KavachModePage(
                        selectedMode = selectedMode,
                        onSelectMode = { selectedMode = it },
                        isLight = isLight,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionnaireTopBar(
    page: Int,
    isLight: Boolean,
    primaryText: Color,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (page > 0) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = primaryText,
                )
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (page == 0) "Welcome" else "KAVACH",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = primaryText,
                    letterSpacing = 0.5.sp,
                )
            }
            Text(
                text = "Step ${page + 1} of 2",
                style = MaterialTheme.typography.labelSmall,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
            )
        }

        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun QuestionnaireStepProgress(
    activePage: Int,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
    val track = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (index <= activePage) accent else track),
            )
        }
    }
}

@Composable
private fun WhyHerePage(
    selectedReasons: Set<Int>,
    onToggleReason: (Int) -> Unit,
    isLight: Boolean,
    primaryText: Color,
    secondaryText: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        QuestionnaireStepProgress(activePage = 0, isLight = isLight)

        QuestionnaireHeroBadge(
            icon = Icons.Default.Shield,
            accent = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet,
            isLight = isLight,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Why are you here?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = primaryText,
                lineHeight = 34.sp,
            )
            Text(
                text = "Pick all that fit you. We'll set things up for you.",
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryText,
                lineHeight = 24.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            usageReasons.forEachIndexed { index, option ->
                ReasonOptionCard(
                    option = option,
                    selected = index in selectedReasons,
                    isLight = isLight,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onClick = { onToggleReason(index) },
                )
            }
        }

        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun KavachModePage(
    selectedMode: String?,
    onSelectMode: (String) -> Unit,
    isLight: Boolean,
    primaryText: Color,
    secondaryText: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        QuestionnaireStepProgress(activePage = 1, isLight = isLight)

        QuestionnaireHeroBadge(
            icon = Icons.Default.Shield,
            accent = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet,
            isLight = isLight,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Pick your KAVACH mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = primaryText,
                lineHeight = 34.sp,
            )
            Text(
                text = "Choose how strong you want app blocking to be. You can change this anytime.",
                style = MaterialTheme.typography.bodyLarge,
                color = secondaryText,
                lineHeight = 24.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            kavachModes.forEach { option ->
                KavachModeCard(
                    option = option,
                    selected = selectedMode == option.mode,
                    isLight = isLight,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    onClick = { onSelectMode(option.mode) },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isLight) SafarGlassPalette.LightViolet.copy(alpha = 0.08f)
                    else SafarGlassPalette.Violet.copy(alpha = 0.14f),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Not sure? Start with Normal — you can turn on Always On or Beast Mode later in KAVACH settings.",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun QuestionnaireHeroBadge(
    icon: ImageVector,
    accent: Color,
    isLight: Boolean,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isLight) 0.18f else 0.28f),
                        accent.copy(alpha = if (isLight) 0.06f else 0.10f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (isLight) 0.35f else 0.45f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun ReasonOptionCard(
    option: UsageReasonOption,
    selected: Boolean,
    isLight: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "reasonCardScale",
    )
    val accent = option.accent
    val tintAlpha = if (selected) 0.12f else if (isLight) 0.04f else 0.05f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .liquidGlass(
                shape = RoundedCornerShape(20.dp),
                surfaceTint = if (selected) accent else if (isLight) Color.Black else Color.White,
                tintAlpha = tintAlpha,
                isLight = isLight,
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = accent.copy(alpha = if (isLight) 0.55f else 0.70f),
                        shape = RoundedCornerShape(20.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = if (isLight) 0.12f else 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = primaryText,
            )
            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                lineHeight = 18.sp,
            )
        }

        SelectionIndicator(selected = selected, accent = accent, isLight = isLight)
    }
}

@Composable
private fun KavachModeCard(
    option: KavachModeOption,
    selected: Boolean,
    isLight: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "modeCardScale",
    )
    val accent = option.accent
    val tintAlpha = if (selected) 0.14f else if (isLight) 0.04f else 0.05f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .liquidGlass(
                shape = RoundedCornerShape(22.dp),
                surfaceTint = if (selected) accent else if (isLight) Color.Black else Color.White,
                tintAlpha = tintAlpha,
                isLight = isLight,
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = accent.copy(alpha = if (isLight) 0.60f else 0.75f),
                        shape = RoundedCornerShape(22.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = if (isLight) 0.14f else 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(26.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (selected) accent else primaryText,
                    )
                    if (option.recommended) {
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(accent.copy(alpha = if (isLight) 0.12f else 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryText,
                    lineHeight = 20.sp,
                )
            }

            SelectionIndicator(selected = selected, accent = accent, isLight = isLight)
        }
    }
}

@Composable
private fun SelectionIndicator(
    selected: Boolean,
    accent: Color,
    isLight: Boolean,
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                if (selected) accent
                else if (isLight) Color.Black.copy(alpha = 0.06f)
                else Color.White.copy(alpha = 0.08f),
            )
            .then(
                if (!selected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = if (isLight) Color.Black.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.18f),
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(100)),
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun QuestionnaireBottomBar(
    page: Int,
    canContinue: Boolean,
    isLight: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val accent = if (isLight) SafarGlassPalette.LightViolet else SafarGlassPalette.Violet
    val primaryText = if (isLight) SafarGlassPalette.LightTextPrimary else SafarGlassPalette.TextPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                surfaceTint = if (isLight) Color.Black else Color.White,
                tintAlpha = if (isLight) 0.05f else 0.07f,
                isLight = isLight,
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (page > 0) Arrangement.spacedBy(12.dp) else Arrangement.Center,
        ) {
            if (page > 0) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Back", fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier
                    .then(if (page > 0) Modifier.weight(1f) else Modifier.fillMaxWidth())
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.White,
                    disabledContainerColor = accent.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.70f),
                ),
            ) {
                Text(
                    text = if (page == 0) "Continue" else "Set up KAVACH",
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (page == 0) {
            Text(
                text = "Select at least one option to continue",
                style = MaterialTheme.typography.labelSmall,
                color = if (isLight) SafarGlassPalette.LightTextSecondary else SafarGlassPalette.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
