package com.safarparmar.app.ui.launch

import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.safarparmar.app.ui.theme.RoyalPurpleSoftDark

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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ShieldMoon
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.composables.ui.components.Text
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import androidx.compose.foundation.layout.PaddingValues
import com.safarparmar.app.ui.onboarding.editorialCornerArtwork
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    return if (isLight) {
        if (selected) RoyalPurpleSoft else Color(0xFFFFFDFC)
    } else {
        if (selected) RoyalPurpleSoftDark else Color(0xFF342735)
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
    return clip(shape).background(glassBodyColor(isLight, selected, accent))
        .border(1.dp, if (selected) accent.copy(alpha = .4f) else if (isLight) Color(0xFFE8DEE5) else Color(0xFF654D66), shape)
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
        title = stringResource(R.string.launch_reason_focus_title),
        subtitle = stringResource(R.string.launch_reason_focus_subtitle),
        icon = Icons.Default.AutoStories,
        accent = LaunchFlatColors.Primary,
    ),
    UsageReasonOption(
        title = stringResource(R.string.launch_reason_habits_title),
        subtitle = stringResource(R.string.launch_reason_habits_subtitle),
        icon = Icons.Default.TrackChanges,
        accent = LaunchFlatColors.Habit,
    ),
    UsageReasonOption(
        title = stringResource(R.string.launch_reason_journal_title),
        subtitle = stringResource(R.string.launch_reason_journal_subtitle),
        icon = Icons.Default.EditNote,
        accent = LaunchFlatColors.Journal,
    ),
    UsageReasonOption(
        title = stringResource(R.string.launch_reason_calm_title),
        subtitle = stringResource(R.string.launch_reason_calm_subtitle),
        icon = Icons.Default.SelfImprovement,
        accent = LaunchFlatColors.Calm,
    ),
    UsageReasonOption(
        title = stringResource(R.string.launch_reason_all_title),
        subtitle = stringResource(R.string.launch_reason_all_subtitle),
        icon = Icons.Default.CheckCircle,
        accent = LaunchFlatColors.Primary,
    ),
)

@Composable
private fun kavachActivationModes(): List<KavachModeOption> = listOf(
    KavachModeOption(
        mode = AppUsageMode.FOCUSED,
        title = stringResource(R.string.launch_kavach_with_ekagra_title),
        description = stringResource(R.string.launch_kavach_with_ekagra_description),
        icon = Icons.Rounded.TouchApp,
        accent = LaunchFlatColors.Normal,
        badge = null,
    ),
    KavachModeOption(
        mode = AppUsageMode.ALWAYS_ON,
        title = stringResource(R.string.launch_kavach_always_on_title),
        description = stringResource(R.string.launch_kavach_always_on_description),
        icon = Icons.Default.Shield,
        accent = LaunchFlatColors.Primary,
        badge = "24/7",
    ),
)

@Composable
private fun kavachProtectionModes(): List<KavachModeOption> = listOf(
    KavachModeOption(
        mode = AppUsageMode.NORMAL,
        title = stringResource(R.string.launch_kavach_normal_title),
        description = stringResource(R.string.launch_kavach_normal_description),
        icon = Icons.Rounded.TouchApp,
        accent = LaunchFlatColors.Normal,
    ),
    KavachModeOption(
        mode = AppUsageMode.BEAST,
        title = stringResource(R.string.launch_kavach_strict_title),
        description = stringResource(R.string.launch_kavach_strict_description),
        icon = Icons.Default.Shield,
        accent = LaunchFlatColors.Beast,
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
                .background(LaunchFlatColors.Bg).editorialCornerArtwork(),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.common_loading), color = LaunchFlatColors.Muted, fontSize = 14.sp)
        }
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var page by remember { mutableIntStateOf(0) }
    var selectedMode by remember { mutableStateOf<String?>(null) }
    var selectedProtection by remember { mutableStateOf(AppUsageMode.NORMAL) }

    LaunchedEffect(page) {
        if (page == 1 && selectedMode == null) {
            selectedMode = AppUsageMode.FOCUSED
        }
    }

    fun onFinishQuestionnaire() {
        val mode = selectedMode ?: AppUsageMode.FOCUSED
        viewModel.markQuestionnaireFinished(
            mode = mode,
            strict = selectedProtection == AppUsageMode.BEAST,
            onDone = onNavigateKavach,
        )
    }

    val isLight = MaterialTheme.colorScheme.background.isLightBackground()

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides !isLight,
        com.safarparmar.app.ui.theme.LocalSafarComposablesAccent provides LaunchFlatColors.Primary) {
    com.safarparmar.app.ui.theme.SafarComposablesTheme {
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
                            selectedProtection = selectedProtection,
                            onSelectProtection = { selectedProtection = it },
                            isLight = isLight,
                        )
                    }
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
    Column(Modifier.fillMaxWidth().background(com.safarparmar.app.ui.onboarding.EditorialPlum).statusBarsPadding()) {
        com.safarparmar.app.ui.onboarding.EditorialHeader(
            title = stringResource(if (page == 0) R.string.onboarding_goals_title else R.string.onboarding_protection_title),
            onBack = onBack.takeIf { page > 0 }, step = stringResource(R.string.launch_step_of_two, page + 1),
        )
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
                text = stringResource(R.string.launch_help_question),
                fontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                color = LaunchFlatColors.Text,
                lineHeight = 32.sp,
            )
            Text(
                text = stringResource(R.string.launch_choose_all),
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
    selectedProtection: String,
    onSelectProtection: (String) -> Unit,
    isLight: Boolean,
) {
    val activationModes = kavachActivationModes()
    val protectionModes = kavachProtectionModes()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        QuestionnaireTitli()

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.launch_set_up_kavach),
                fontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                color = LaunchFlatColors.Text,
                lineHeight = 32.sp,
            )
            Text(
                text = stringResource(R.string.launch_change_later),
                fontSize = 14.sp,
                color = LaunchFlatColors.Muted,
                lineHeight = 20.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.launch_when_does_it_work).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LaunchFlatColors.Muted)
            activationModes.forEach { option ->
                KavachModeCard(
                    option = option,
                    selected = selectedMode == option.mode,
                    isLight = isLight,
                    onClick = { onSelectMode(option.mode) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.launch_protection_level).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LaunchFlatColors.Muted)
            protectionModes.forEach { option ->
                KavachModeCard(
                    option = option,
                    selected = selectedProtection == option.mode,
                    isLight = isLight,
                    onClick = { onSelectProtection(option.mode) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun QuestionnaireTitli(
    modifier: Modifier = Modifier,
) {
    if (!com.safarparmar.app.BuildConfig.TITLI_TUTORIALS_ENABLED) return
    com.safarparmar.app.ui.tutorial.TitliIcon(modifier.size(32.dp))
}

@Composable
private fun ReasonOptionCard(
    option: UsageReasonOption,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
) {
    OnboardingChoice(option.title, option.subtitle, option.icon, selected, isLight, onClick, multi = true)
}

@Composable
private fun KavachModeCard(
    option: KavachModeOption,
    selected: Boolean,
    isLight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingChoice(option.title, option.description, option.icon, selected, isLight, onClick, modifier)
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
    Row(Modifier.fillMaxWidth().background(LaunchFlatColors.Bg).navigationBarsPadding().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (page > 0) Button(onClick = onBack, style = ButtonStyle.Outlined,
            modifier = Modifier.weight(1f).heightIn(min = 52.dp), contentColor = LaunchFlatColors.Text) {
            Text(stringResource(R.string.common_back))
        }
        Button(onClick = onContinue, enabled = canContinue, style = ButtonStyle.Ghost,
            modifier = Modifier.weight(1.4f).heightIn(min = 52.dp).background(
                if (canContinue) LaunchFlatColors.Primary else LaunchFlatColors.Hairline, RoundedCornerShape(14.dp)),
            contentColor = if (LaunchFlatColors.Primary.isLightBackground()) Color(0xFF241544) else Color.White) {
            Text(stringResource(if (page == 0) R.string.common_continue else R.string.launch_finish_setup), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OnboardingChoice(title: String, subtitle: String, icon: ImageVector, selected: Boolean,
    isLight: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, multi: Boolean = false) {
    Button(onClick = onClick, style = ButtonStyle.Ghost, shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = modifier.fillMaxWidth().semantics { this.selected = selected; role = if (multi) Role.Checkbox else Role.RadioButton }.heightIn(min = 64.dp).launchGlassPanel(isLight, selected, LaunchFlatColors.Primary, RoundedCornerShape(16.dp))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = LaunchFlatColors.Primary,
                modifier = Modifier.size(36.dp).background(if (isLight) RoyalPurpleSoft else RoyalPurpleSoftDark, RoundedCornerShape(10.dp)).padding(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LaunchFlatColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = LaunchFlatColors.Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Icon(if (multi) { if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank }
                else { if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked },
                contentDescription = null, tint = LaunchFlatColors.Primary, modifier = Modifier.size(22.dp))
        }
    }
}
