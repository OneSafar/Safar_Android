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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground

/**
 * Opaque macOS Control Center glass for tappable option tiles.
 * Page chrome / CTAs stay flat-hairline via [LaunchFlatColors].
 */
private fun glassBodyColor(isLight: Boolean, selected: Boolean, accent: Color): Color {
    return if (selected) {
        if (isLight) lerp(Color(0xFFF9F9FB), accent, 0.18f)
        else lerp(Color(0xFF2C2C2E), accent, 0.28f)
    } else if (isLight) {
        Color(0xFFF9F9FB)
    } else {
        Color(0xFF2C2C2E)
    }
}

/** Title + subtitle ink keyed to the glass fill, not the page theme. */
private fun onGlassInk(bodyColor: Color): Pair<Color, Color> {
    return if (bodyColor.isLightBackground()) {
        Color(0xFF1C1C1E) to Color(0xFF3A3A3C)
    } else {
        Color(0xFFF5F5F7) to Color(0xFFD1D1D6)
    }
}

private fun Modifier.launchGlassPanel(
    isLight: Boolean,
    selected: Boolean,
    accent: Color,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
): Modifier {
    val bodyColor = glassBodyColor(isLight, selected, accent)
    val borderBrush = if (!isLight) {
        Brush.verticalGradient(
            listOf(
                if (selected) accent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.25f),
                if (selected) accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.02f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                if (selected) accent.copy(alpha = 0.55f) else Color(0xFFE5E5EA),
                if (selected) accent.copy(alpha = 0.25f) else Color(0xFFD1D1D6),
            ),
        )
    }
    val shadowElevation = if (isLight) {
        if (selected) 6.dp else 4.dp
    } else {
        if (selected) 14.dp else 12.dp
    }
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)
    return this
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            spotColor = shadowColor,
            ambientColor = shadowColor,
        )
        .clip(shape)
        .background(bodyColor)
        .border(
            width = if (selected) 1.dp else 0.5.dp,
            brush = borderBrush,
            shape = shape,
        )
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

@Composable
private fun usageReasons(): List<UsageReasonOption> = listOf(
    UsageReasonOption(
        title = "Focus better",
        subtitle = "Stay away from distractions",
        icon = Icons.Default.AutoStories,
        accent = LaunchFlatColors.Primary,
    ),
    UsageReasonOption(
        title = "Build daily habits",
        subtitle = "Do small tasks every day",
        icon = Icons.Default.TrackChanges,
        accent = LaunchFlatColors.Habit,
    ),
    UsageReasonOption(
        title = "Write my thoughts",
        subtitle = "Keep a simple daily note",
        icon = Icons.Default.EditNote,
        accent = LaunchFlatColors.Journal,
    ),
    UsageReasonOption(
        title = "Feel calm",
        subtitle = "Try breathing and meditation",
        icon = Icons.Default.SelfImprovement,
        accent = LaunchFlatColors.Calm,
    ),
    UsageReasonOption(
        title = "Use all SAFAR tools",
        subtitle = "Get the full SAFAR experience",
        icon = Icons.Default.CheckCircle,
        accent = LaunchFlatColors.Primary,
    ),
)

@Composable
private fun kavachModes(): List<KavachModeOption> = listOf(
    KavachModeOption(
        mode = AppUsageMode.FOCUSED,
        title = "Normal",
        description = "We remind you when you open a blocked app.",
        icon = Icons.Rounded.TouchApp,
        accent = LaunchFlatColors.Normal,
        badge = "Recommended",
    ),
    // "Always On" is HIDDEN for this release — only Normal and Beast Mode ship.
    // The mode, its service and its repository plumbing are all left intact so it
    // can be restored by putting this option back; nothing else needs changing.
    KavachModeOption(
        mode = AppUsageMode.BEAST,
        title = "Beast Mode",
        description = "Strict block. You cannot open blocked apps during focus.",
        icon = Icons.Rounded.Lock,
        accent = LaunchFlatColors.Beast,
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
        Box(
            Modifier
                .fillMaxSize()
                .background(LaunchFlatColors.Bg),
            contentAlignment = Alignment.Center,
        ) {
            Text("Loading…", color = LaunchFlatColors.Muted, fontSize = 14.sp)
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

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LaunchFlatColors.Bg),
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    QuestionnaireTopBar(
                        page = page,
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
                        )
                        else -> KavachModePage(
                            selectedMode = selectedMode,
                            onSelectMode = { selectedMode = it },
                            isLight = isLight,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionnaireTopBar(
    page: Int,
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, LaunchFlatColors.Hairline, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LaunchFlatColors.Text,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PlanEyebrow("Safar")
                Text(
                    text = "KAVACH Setup",
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = LaunchFlatColors.Text,
                )
                Text(
                    text = "Step ${page + 1} of 2",
                    fontSize = 12.sp,
                    color = LaunchFlatColors.Muted,
                )
            }

            Spacer(Modifier.size(40.dp))
        }

        QuestionnaireStepProgress(activePage = page)
        PlanHairline()
    }
}

@Composable
private fun QuestionnaireStepProgress(
    activePage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index <= activePage) LaunchFlatColors.Primary
                        else LaunchFlatColors.Hairline.copy(alpha = 0.65f),
                    ),
            )
        }
    }
}

@Composable
private fun WhyHerePage(
    selectedReasons: Set<Int>,
    onToggleReason: (Int) -> Unit,
    isLight: Boolean,
) {
    val reasons = usageReasons()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        QuestionnaireTitli()

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "What do you need help with?",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 26.sp,
                color = LaunchFlatColors.Text,
                lineHeight = 32.sp,
            )
            Text(
                text = "Choose all that you want.",
                fontSize = 14.sp,
                color = LaunchFlatColors.Muted,
                lineHeight = 20.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            reasons.forEachIndexed { index, option ->
                ReasonOptionCard(
                    option = option,
                    selected = index in selectedReasons,
                    isLight = isLight,
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
) {
    val modes = kavachModes()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        QuestionnaireTitli()

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "How do you want KAVACH to block apps?",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 26.sp,
                color = LaunchFlatColors.Text,
                lineHeight = 32.sp,
            )
            Text(
                text = "You can change this later.",
                fontSize = 14.sp,
                color = LaunchFlatColors.Muted,
                lineHeight = 20.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            modes.forEach { option ->
                KavachModeCard(
                    option = option,
                    selected = selectedMode == option.mode,
                    isLight = isLight,
                    onClick = { onSelectMode(option.mode) },
                )
            }
        }

        // Flat tip strip — not a glass tile
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlanHairline(alpha = 0.6f)
            Text(
                text = "Start with Normal. You can choose a stronger mode later.",
                fontSize = 13.sp,
                color = LaunchFlatColors.Muted,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionnaireTitli(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_butterfly_tour),
        contentDescription = null,
        modifier = modifier.size(56.dp),
    )
}

@Composable
private fun ReasonOptionCard(
    option: UsageReasonOption,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.995f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "reasonCardScale",
    )
    val shape = RoundedCornerShape(20.dp)
    val body = glassBodyColor(isLight, selected, option.accent)
    val (titleInk, mutedInk) = onGlassInk(body)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .launchGlassPanel(
                isLight = isLight,
                selected = selected,
                accent = option.accent,
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
                .background(
                    if (selected) option.accent
                    else option.accent.copy(alpha = if (isLight) 0.14f else 0.24f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (selected) {
                    if (option.accent.isLightBackground()) Color(0xFF1C1C1E) else Color.White
                } else {
                    option.accent
                },
                modifier = Modifier.size(22.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = option.title,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = titleInk,
            )
            Text(
                text = option.subtitle,
                fontSize = 12.sp,
                color = mutedInk,
                lineHeight = 17.sp,
            )
        }

        SelectionIndicator(selected = selected, accent = option.accent, isLight = isLight)
    }
}

@Composable
private fun KavachModeCard(
    option: KavachModeOption,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.995f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "modeCardScale",
    )
    val shape = RoundedCornerShape(20.dp)
    val body = glassBodyColor(isLight, selected, option.accent)
    val (titleInk, mutedInk) = onGlassInk(body)
    // Accent title only when it stays readable on the tinted glass fill.
    val titleColor = when {
        !selected -> titleInk
        option.accent.isLightBackground() -> titleInk
        else -> option.accent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .launchGlassPanel(
                isLight = isLight,
                selected = selected,
                accent = option.accent,
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
                .background(
                    if (selected) option.accent
                    else option.accent.copy(alpha = if (isLight) 0.14f else 0.24f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (selected) {
                    if (option.accent.isLightBackground()) Color(0xFF1C1C1E) else Color.White
                } else {
                    option.accent
                },
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                )
                option.badge?.let { badge ->
                    val badgeInk = if (option.accent.isLightBackground()) titleInk else option.accent
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeInk,
                        modifier = Modifier
                            .border(1.dp, badgeInk.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            Text(
                text = option.description,
                fontSize = 13.sp,
                color = mutedInk,
                lineHeight = 18.sp,
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
    val ring = if (isLight) Color(0xFF8E8E93) else Color(0xFFAEAEB2)
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (selected) accent else Color.Transparent)
            .then(
                if (!selected) {
                    Modifier.border(width = 1.5.dp, color = ring, shape = CircleShape)
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
                tint = if (accent.isLightBackground()) Color(0xFF1C1C1E) else Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun QuestionnaireBottomBar(
    page: Int,
    canContinue: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LaunchFlatColors.Bg)
            .navigationBarsPadding(),
    ) {
        PlanHairline()
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, LaunchFlatColors.Hairline, RoundedCornerShape(14.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = LaunchFlatColors.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Back",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = LaunchFlatColors.Text,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .then(if (page > 0) Modifier.weight(1.4f) else Modifier.fillMaxWidth())
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (canContinue) LaunchFlatColors.Primary
                            else LaunchFlatColors.Hairline.copy(alpha = 0.55f),
                        )
                        .clickable(enabled = canContinue, onClick = onContinue),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = if (page == 0) "Continue" else "Finish setup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (canContinue) Color.White else LaunchFlatColors.Muted,
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (canContinue) Color.White else LaunchFlatColors.Muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = page == 0 && !canContinue,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(120)),
            ) {
                Text(
                    text = "Choose at least one option to continue",
                    fontSize = 12.sp,
                    color = LaunchFlatColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
