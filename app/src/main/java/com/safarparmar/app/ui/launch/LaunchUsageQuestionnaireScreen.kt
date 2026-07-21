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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
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
import com.safarparmar.app.ui.theme.isLightBackground

// ── Premium onboarding palette ───────────────────────────────────────────────

private object OnboardPalette {
    val BackgroundLight = Color(0xFFF7F5FB)
    val BackgroundDark = Color(0xFF0F0F14)

    val CardWhite = Color.White
    val CardDark = Color(0xFF1C1C24)
    val SelectedLight = Color(0xFFF1E8FF)
    val SelectedDark = Color(0xFF2A2140)

    val BorderLight = Color(0xFFE5E0F3)
    val BorderDark = Color(0xFF3A3550)
    val SelectedBorder = Color(0xFF9B6BE8)

    val Primary = Color(0xFF9B6BE8)
    val TextPrimaryLight = Color(0xFF161827)
    val TextPrimaryDark = Color(0xFFF5F3FA)
    val TextSecondaryLight = Color(0xFF6B6478)
    val TextSecondaryDark = Color(0xFFA8A0B8)

    val FooterLight = Color.White.copy(alpha = 0.92f)
    val FooterDark = Color(0xFF16161E).copy(alpha = 0.94f)
    val FooterBorderLight = Color(0xFFE7E2F0)
    val FooterBorderDark = Color(0xFF2E2A3A)

    val SecondaryButtonBorder = Color(0xFFC8C1D8)
    val SecondaryButtonText = Color(0xFF475569)

    val ProgressTrackLight = Color(0xFFE4DFEE)
    val ProgressTrackDark = Color(0xFF2E2A3A)

    val BeastAccent = Color(0xFFFF5722)
    val AlwaysOnAccent = Color(0xFF9B6BE8)
    val NormalAccent = Color(0xFF26A69A)
}

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
    val badge: String? = null,
)

private val usageReasons = listOf(
    UsageReasonOption(
        title = "Focus without distractions",
        subtitle = "Block apps and stay on track",
        icon = Icons.Default.AutoStories,
        accent = OnboardPalette.Primary,
    ),
    UsageReasonOption(
        title = "Build daily discipline",
        subtitle = "Track goals and stay consistent",
        icon = Icons.Default.TrackChanges,
        accent = Color(0xFFE86BA8),
    ),
    UsageReasonOption(
        title = "Reflect with journaling",
        subtitle = "Write thoughts and review your day",
        icon = Icons.Default.EditNote,
        accent = Color(0xFFE8846B),
    ),
    UsageReasonOption(
        title = "Calm my mind",
        subtitle = "Use Dhyan and breathing tools",
        icon = Icons.Default.SelfImprovement,
        accent = Color(0xFF9B8BE8),
    ),
    UsageReasonOption(
        title = "Full SAFAR experience",
        subtitle = "Enable all recommended tools",
        icon = Icons.Default.CheckCircle,
        accent = OnboardPalette.Primary,
    ),
)

private val kavachModes = listOf(
    KavachModeOption(
        mode = AppUsageMode.FOCUSED,
        title = "Normal",
        description = "Redirects you back when you open a blocked app.",
        icon = Icons.Rounded.TouchApp,
        accent = OnboardPalette.NormalAccent,
        badge = "Recommended",
    ),
    KavachModeOption(
        mode = AppUsageMode.ALWAYS_ON,
        title = "Always On",
        description = "Blocks selected apps until you turn KAVACH off.",
        icon = Icons.Rounded.ShieldMoon,
        accent = OnboardPalette.AlwaysOnAccent,
        badge = "Strong",
    ),
    KavachModeOption(
        mode = AppUsageMode.BEAST,
        title = "Beast Mode",
        description = "Full lockdown. No quick unlock during focus.",
        icon = Icons.Rounded.Lock,
        accent = OnboardPalette.BeastAccent,
        badge = "Strict",
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
            selectedMode = AppUsageMode.FOCUSED
        }
    }

    fun onFinishQuestionnaire() {
        val mode = selectedMode ?: AppUsageMode.FOCUSED
        viewModel.markQuestionnaireFinished(mode, onNavigateKavach)
    }

    val isLight = MaterialTheme.colorScheme.background.isLightBackground()
    val primaryText = if (isLight) OnboardPalette.TextPrimaryLight else OnboardPalette.TextPrimaryDark
    val secondaryText = if (isLight) OnboardPalette.TextSecondaryLight else OnboardPalette.TextSecondaryDark
    val canvas = if (isLight) OnboardPalette.BackgroundLight else OnboardPalette.BackgroundDark

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(canvas),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                QuestionnaireTopBar(
                    page = page,
                    isLight = isLight,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
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
    secondaryText: Color,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (page > 0) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryText,
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
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
                        tint = OnboardPalette.Primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "KAVACH Setup",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        letterSpacing = 0.3.sp,
                    )
                }
                Text(
                    text = "Step ${page + 1} of 2",
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryText,
                )
            }

            Spacer(Modifier.size(40.dp))
        }

        QuestionnaireStepProgress(activePage = page, isLight = isLight)
    }
}

@Composable
private fun QuestionnaireStepProgress(
    activePage: Int,
    isLight: Boolean,
    modifier: Modifier = Modifier,
) {
    val track = if (isLight) OnboardPalette.ProgressTrackLight else OnboardPalette.ProgressTrackDark

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(if (index <= activePage) OnboardPalette.Primary else track),
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        QuestionnaireHeroBadge(
            icon = Icons.Default.Shield,
            accent = OnboardPalette.Primary,
            isLight = isLight,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "What do you want SAFAR to help with?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                lineHeight = 30.sp,
            )
            Text(
                text = "Choose one or more. We'll personalize your setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText,
                lineHeight = 22.sp,
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

        Spacer(Modifier.height(24.dp))
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        QuestionnaireHeroBadge(
            icon = Icons.Default.Shield,
            accent = OnboardPalette.Primary,
            isLight = isLight,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Choose your KAVACH strength",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                lineHeight = 30.sp,
            )
            Text(
                text = "You can change this anytime from settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText,
                lineHeight = 22.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    if (isLight) OnboardPalette.Primary.copy(alpha = 0.08f)
                    else OnboardPalette.Primary.copy(alpha = 0.16f),
                )
                .border(
                    width = 1.dp,
                    color = if (isLight) OnboardPalette.Primary.copy(alpha = 0.18f)
                    else OnboardPalette.Primary.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = "New users should start with Normal. You can switch to stricter modes later.",
                style = MaterialTheme.typography.bodySmall,
                color = secondaryText,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
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
            .size(64.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isLight) 0.16f else 0.26f),
                        accent.copy(alpha = if (isLight) 0.06f else 0.10f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = if (isLight) 0.28f else 0.40f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun onboardCardSurface(
    selected: Boolean,
    isLight: Boolean,
    accent: Color,
): Pair<Color, Color> {
    val bg = when {
        selected && isLight -> OnboardPalette.SelectedLight
        selected && !isLight -> OnboardPalette.SelectedDark
        isLight -> OnboardPalette.CardWhite
        else -> OnboardPalette.CardDark
    }
    val border = when {
        selected -> accent
        isLight -> OnboardPalette.BorderLight
        else -> OnboardPalette.BorderDark
    }
    return bg to border
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
        targetValue = if (selected) 1f else 0.995f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "reasonCardScale",
    )
    val (bg, borderColor) = onboardCardSurface(
        selected = selected,
        isLight = isLight,
        accent = OnboardPalette.SelectedBorder,
    )
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isLight && !selected) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = shape,
                        spotColor = Color.Black.copy(alpha = 0.06f),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(bg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(option.accent.copy(alpha = if (isLight) 0.12f else 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = option.accent,
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

        SelectionIndicator(selected = selected, accent = OnboardPalette.Primary, isLight = isLight)
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
        targetValue = if (selected) 1f else 0.995f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "modeCardScale",
    )
    val (bg, borderColor) = onboardCardSurface(
        selected = selected,
        isLight = isLight,
        accent = option.accent,
    )
    val shape = RoundedCornerShape(18.dp)
    val titleColor = if (selected) option.accent else primaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isLight && !selected) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = shape,
                        spotColor = Color.Black.copy(alpha = 0.06f),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(bg)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(option.accent.copy(alpha = if (isLight) 0.12f else 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = option.accent,
                modifier = Modifier.size(24.dp),
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
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                )
                option.badge?.let { badge ->
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = option.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(option.accent.copy(alpha = if (isLight) 0.12f else 0.20f))
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

        SelectionIndicator(selected = selected, accent = option.accent, isLight = isLight)
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
            .background(if (selected) accent else Color.Transparent)
            .then(
                if (!selected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = if (isLight) OnboardPalette.BorderLight else OnboardPalette.BorderDark,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isLight) OnboardPalette.FooterLight else OnboardPalette.FooterDark)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    if (isLight) OnboardPalette.FooterBorderLight else OnboardPalette.FooterBorderDark,
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isLight) OnboardPalette.SecondaryButtonText
                        else OnboardPalette.TextSecondaryDark,
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isLight) OnboardPalette.SecondaryButtonBorder
                        else OnboardPalette.BorderDark,
                    ),
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
                    .then(if (page > 0) Modifier.weight(1.4f) else Modifier.fillMaxWidth())
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnboardPalette.Primary,
                    contentColor = Color.White,
                    disabledContainerColor = OnboardPalette.Primary.copy(alpha = 0.35f),
                    disabledContentColor = Color.White.copy(alpha = 0.70f),
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp,
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

            AnimatedVisibility(
                visible = page == 0 && !canContinue,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
                Text(
                    text = "Select at least one option to continue",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLight) OnboardPalette.TextSecondaryLight else OnboardPalette.TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
