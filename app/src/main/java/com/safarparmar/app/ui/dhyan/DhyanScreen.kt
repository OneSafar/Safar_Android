
package com.safarparmar.app.ui.dhyan

import com.safarparmar.app.ui.theme.RoyalPurple
import com.safarparmar.app.ui.theme.RoyalPurpleDark
import com.safarparmar.app.ui.theme.RoyalPurpleSoft
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.IconButton as ComposablesIconButton
import androidx.compose.material.icons.automirrored.filled.ArrowForward

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safarparmar.app.R
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.util.bounceClick
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safarparmar.app.feature.live.presentation.DhyanLiveLockOverlay
import com.safarparmar.app.feature.live.presentation.LiveSessionsScreen
import com.safarparmar.app.ui.audio.MediaFileCache
import com.safarparmar.app.ui.premium.PremiumViewModel
import kotlinx.coroutines.delay

// ─── Data ──────────────────────────────────────────────────────────────────────

internal data class BreathingTechnique(
    val nameRes: Int,
    val iconRes: Int,
    val descriptionRes: Int,
    val inhale: Int,
    val hold: Int,
    val exhale: Int,
    val holdAfter: Int = 0,
    val pattern: String,
    val audioUrl: String? = null,
)

private data class BreathingSound(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val url: String = "",
    val localResId: Int? = null,
)

internal val techniques = listOf(
    BreathingTechnique(R.string.dhyan_technique_diaphragmatic, R.drawable.ic_wind, R.string.dhyan_technique_diaphragmatic_desc, 4, 0, 6, 0, "4-6"),
    BreathingTechnique(R.string.dhyan_technique_pursed_lip, R.drawable.ic_wind, R.string.dhyan_technique_pursed_lip_desc, 2, 0, 4, 0, "2-4"),
    BreathingTechnique(R.string.dhyan_technique_box, R.drawable.ic_square, R.string.dhyan_technique_box_desc, 4, 4, 4, 4, "4-4-4-4", "https://qms-images.del1.vultrobjects.com/qms-parmar-academy/music/box_breathing.mp3"),
    BreathingTechnique(R.string.dhyan_technique_478, R.drawable.ic_moon, R.string.dhyan_technique_478_desc, 4, 7, 8, 0, "4-7-8", "https://qms-images.del1.vultrobjects.com/qms-parmar-academy/music/four_seven_eight.mp3"),
    BreathingTechnique(R.string.dhyan_technique_678, R.drawable.ic_yin_yang, R.string.dhyan_technique_678_desc, 6, 7, 8, 0, "6-7-8"),
)

internal fun techniqueSessionId(technique: BreathingTechnique?): String = when (techniques.indexOfFirst { it.nameRes == technique?.nameRes }) {
    0 -> "1"
    1 -> "2"
    2 -> "3"
    3 -> "4"
    4 -> "5"
    else -> "1"
}

private val breathingSounds = listOf(
    BreathingSound(
        id = "silent-breathing",
        nameRes = R.string.dhyan_silent_guidance,
        descriptionRes = R.string.dhyan_silent_guidance_desc,
    ),
)


private enum class DhyanBreathPhase {
    INHALE, HOLD, EXHALE, HOLD_AFTER
}

private enum class DhyanAudioSource {
    MUSIC, BREATHING_SOUND
}

// musicOptions removed in favor of shared AudioLibrary

// ─── Dhyan brand accents (lavender) + hybrid glass/flat helpers ─────────────────

private object DhyanColors {
    val LightLotus = Color(0xFFEEE5FD)
    val LightRose = Color(0xFFC4AFE8)
    val LightCalm = Color(0xFF9575CD)
    val DarkLotus = Color(0xFFB39DDB)
    val DarkRose = Color(0xFF7954B1)
    val DarkCalm = Color(0xFF512395)

    fun lotus(isDark: Boolean) = if (isDark) DarkLotus else LightLotus
    fun rose(isDark: Boolean) = if (isDark) DarkRose else LightRose
    fun calm(isDark: Boolean) = if (isDark) DarkCalm else LightCalm
    fun sky(isDark: Boolean) = if (isDark) Color(0xFFD4C4FF) else Color(0xFF7954B1)
    fun accentBlue(isDark: Boolean) = if (isDark) com.safarparmar.app.ui.theme.RoyalPurpleDark else RoyalPurple
    fun actionPink(isDark: Boolean) = if (isDark) com.safarparmar.app.ui.theme.RoyalPurpleDark else RoyalPurple
}

private val DhyanGlassShape = RoundedCornerShape(20.dp)
private val DhyanFlatShape = RoundedCornerShape(14.dp)
private val DhyanControlShape = RoundedCornerShape(16.dp)

private val DhyanOrbSize = 236.dp
private val DhyanPlaySize = 72.dp
private val DhyanSideControlSize = 52.dp
private val DhyanControlGap = 36.dp
private val DhyanSectionGap = 20.dp
private val DhyanContentHorizontal = 20.dp

/** Opaque macOS Control Center panel for tappable content tiles. */
private fun Modifier.dhyanMacOSPanel(
    isLight: Boolean,
    shape: Shape = DhyanGlassShape,
): Modifier {
    val body = DhyanFlatColors.glassBody(isLight)
    val borderBrush = if (isLight) {
        Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)))
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    }
    val shadowElevation = if (isLight) 4.dp else 12.dp
    val shadowColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f)
    return this
        .shadow(elevation = shadowElevation, shape = shape, spotColor = shadowColor, ambientColor = shadowColor)
        .clip(shape)
        .background(body)
        .border(width = 0.5.dp, brush = borderBrush, shape = shape)
}

/** Lavender gradient canvas (plan: keep Dhyan brand atmosphere). */
@Composable
internal fun DhyanMockBackdrop() {
    Box(Modifier.fillMaxSize().background(DhyanFlatColors.CanvasBrush))
}

@Composable
private fun DhyanGlassPill(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val isLight = !isDarkTheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dhyanMacOSPanel(isLight = isLight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Lavender glass meditation sphere — brand hero tile (visualizer inside untouched). */
@Composable
internal fun DhyanMeditationOrb(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLight = !isDarkTheme
    val rose = DhyanColors.rose(isDarkTheme)
    val lotus = DhyanColors.lotus(isDarkTheme)
    Box(
        modifier = modifier.size(DhyanOrbSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(DhyanOrbSize + 12.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            rose.copy(alpha = if (isLight) 0.28f else 0.35f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            Modifier
                .size(DhyanOrbSize)
                .shadow(
                    elevation = if (isLight) 12.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = rose.copy(alpha = 0.35f),
                    ambientColor = rose.copy(alpha = 0.2f),
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            lotus.copy(alpha = if (isLight) 0.55f else 0.45f),
                            rose.copy(alpha = if (isLight) 0.40f else 0.32f),
                            rose.copy(alpha = if (isLight) 0.18f else 0.16f),
                        ),
                    ),
                )
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isLight) 0.7f else 0.35f),
                            Color.White.copy(alpha = if (isLight) 0.15f else 0.05f),
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(DhyanOrbSize * 0.78f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                lotus.copy(alpha = 0.75f),
                                rose.copy(alpha = 0.55f),
                                rose.copy(alpha = 0.25f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
                content = content,
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isLight) 0.45f else 0.28f),
                                Color.Transparent,
                            ),
                            center = Offset(70f, 55f),
                            radius = 120f,
                        ),
                    ),
            )
        }
    }
}


@Composable
internal fun DhyanSessionSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 1f..60f,
) {
    val density = LocalDensity.current
    val pink = DhyanColors.actionPink(isDarkTheme)
    val inactiveColor = pink.copy(alpha = 0.22f)
    var trackWidthPx by remember { mutableFloatStateOf(1f) }
    val thumbSizeDp = 22.dp
    val trackHeightDp = 3.dp
    val thumbSizePx = with(density) { thumbSizeDp.toPx() }
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(thumbSizeDp)
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
            .pointerInput(valueRange) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (change.pressed) {
                                change.consume()
                                val newFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                                onValueChange(
                                    valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start),
                                )
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeightDp)
                .clip(RoundedCornerShape(2.dp))
                .background(inactiveColor),
        )
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(trackHeightDp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(pink),
            )
        }
        val thumbOffsetPx = (trackWidthPx - thumbSizePx) * fraction
        val thumbOffsetDp = with(density) { thumbOffsetPx.toDp() }
        Box(
            Modifier
                .size(thumbSizeDp)
                .offset(x = thumbOffsetDp)
                .clip(CircleShape)
                .background(pink)
                .border(0.5.dp, Color.White.copy(alpha = 0.85f), CircleShape),
        )
    }
}

/** Flat filled pink CTA bar. */
@Composable
private fun DhyanFlatActionButton(
    text: String,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(DhyanFlatShape)
            .background(DhyanColors.actionPink(isDarkTheme))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            iconRes != null -> Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            icon != null -> Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        if (icon != null || iconRes != null) Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
internal fun DhyanControlButton(
    icon: ImageVector,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    style: DhyanControlStyle,
    contentDescription: String? = null,
) {
    val isLight = !isDarkTheme
    val pink = DhyanColors.actionPink(isDarkTheme)
    val controlShape: Shape = when (style) {
        DhyanControlStyle.Play -> DhyanControlShape
        DhyanControlStyle.Reset, DhyanControlStyle.Volume -> CircleShape
    }
    val playBorder = if (isLight) {
        Brush.verticalGradient(listOf(Color(0xFFE5E5EA), Color(0xFFD1D1D6)))
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.02f)),
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .then(
                when (style) {
                    DhyanControlStyle.Play -> Modifier
                        .shadow(
                            elevation = if (isLight) 4.dp else 12.dp,
                            shape = controlShape,
                            spotColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f),
                            ambientColor = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.8f),
                        )
                        .clip(controlShape)
                        .background(pink)
                        .border(width = 0.5.dp, brush = playBorder, shape = controlShape)
                    else -> Modifier.dhyanMacOSPanel(isLight = isLight, shape = controlShape)
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = when (style) {
                DhyanControlStyle.Reset -> DhyanFlatColors.onGlassMuted(isLight)
                DhyanControlStyle.Play -> Color.White
                DhyanControlStyle.Volume -> DhyanColors.sky(isDarkTheme)
            },
            modifier = Modifier.size(if (style == DhyanControlStyle.Play) 36.dp else 22.dp),
        )
    }
}

internal enum class DhyanControlStyle { Reset, Play, Volume }

@Composable
private fun DhyanStatusBar(
    isDarkTheme: Boolean,
    icon: ImageVector,
    title: String,
    statusLabel: String,
    statusActive: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val isLight = !isDarkTheme
    DhyanGlassPill(
        isDarkTheme = isDarkTheme,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        onClick = onClick,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DhyanColors.accentBlue(isDarkTheme),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = DhyanFlatColors.onGlassText(isLight),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            statusLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = if (statusActive) {
                DhyanColors.accentBlue(isDarkTheme)
            } else {
                DhyanFlatColors.onGlassMuted(isLight)
            },
        )
    }
}

// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhyanScreen(
    initialTab: DhyanTab = DhyanTab.DHYAN,
    currentRoute: String = Routes.DHYAN,
    isDarkTheme: Boolean = false,
    courseId: String = "",
    initialLiveView: String = "live",
    initialCalmPractice: String? = null,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
) {
    val isLiveCentre = initialTab == DhyanTab.LIVE || initialTab == DhyanTab.COURSES
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    var showAudioLibraryPanel by remember { mutableStateOf(false) }
    var showTechniquesSheet   by remember { mutableStateOf(false) }
    var previewTechnique      by remember { mutableStateOf<BreathingTechnique?>(null) }
    var sessionActive by remember { mutableStateOf(false) }
    com.safarparmar.app.ui.tutorial.ScreenPracticeEntry(
        com.safarparmar.app.ui.tutorial.TitliFeature.DHYAN, isDarkTheme,
        eligible = !sessionActive && !showAudioLibraryPanel && !showTechniquesSheet && previewTechnique == null && selectedTab == DhyanTab.DHYAN)

    val context = LocalContext.current
    var selectedMusicTrack    by remember { mutableStateOf(com.safarparmar.app.ui.audio.AudioLibrary.getPersistedTrack(context)) }
    var selectedBreathingSound by remember { mutableStateOf(breathingSounds.first()) }
    // null = no technique chosen (show image), non-null = show animation
    var selectedTechnique   by remember { mutableStateOf<BreathingTechnique?>(null) }
    var activeAudioSource   by remember { mutableStateOf(DhyanAudioSource.MUSIC) }

    LaunchedEffect(selectedTechnique) {
        if (selectedTechnique == null) {
            activeAudioSource = DhyanAudioSource.MUSIC
        } else {
            activeAudioSource = DhyanAudioSource.BREATHING_SOUND
        }
    }

    val dhyanVm: DhyanViewModel = hiltViewModel()
    val liveSessionVm: com.safarparmar.app.feature.live.presentation.LiveSessionViewModel = hiltViewModel()
    val premiumVm: PremiumViewModel = hiltViewModel()
    val liveAccess by premiumVm.dhyanLiveAccess.collectAsStateWithLifecycle()
    // Course and live screens share the server-confirmed Dhyan entitlement.
    val hasCourseAccess = liveAccess == "ALLOWED"
    androidx.lifecycle.compose.LifecycleResumeEffect(premiumVm) {
        premiumVm.refreshDhyanAccess()
        onPauseOrDispose {}
    }

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme,
        com.safarparmar.app.ui.theme.LocalSafarComposablesAccent provides DhyanFlatColors.Primary) {
    com.safarparmar.app.ui.theme.SafarComposablesTheme {
    Box(Modifier.fillMaxSize()) {
        SafarDrawerScaffold(
            title = stringResource(
                if (isLiveCentre) R.string.nav_live_sessions
                else if (selectedTab == DhyanTab.CALM_CENTRE) R.string.nav_calm_centre
                else R.string.module_dhyan
            ),
            subtitle = if (!isLiveCentre) stringResource(R.string.dhyan_mindfulness_subtitle) else null,
            currentRoute      = if (isLiveCentre) Routes.LIVE_SESSIONS_ROOT else if (selectedTab == DhyanTab.CALM_CENTRE) Routes.CALM_CENTRE else currentRoute,
            isDarkTheme       = isDarkTheme,
            onNavigate        = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            useGlassTopBar    = false,
            useDetachedMenuGlass = false,
            containerColor    = Color.Transparent,
            topBarContentColor = DhyanFlatColors.Text,
            topBarActions = {
                if (selectedTab == DhyanTab.LIVE) {
                    val liveUiState by liveSessionVm.liveSessionsState.collectAsStateWithLifecycle()
                    ComposablesIconButton(
                        onClick = { liveSessionVm.loadSessions(courseId, status = null) },
                        style = ButtonStyle.Ghost,
                    ) {
                        if (liveUiState.isLoading) {
                            com.safarparmar.app.ui.components.SafarCircularProgressIndicator(
                                modifier = Modifier.size(19.dp),
                                strokeWidth = 2.dp,
                                color = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF6B21A8),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.dhyan_reload_live_sessions),
                                tint = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                DhyanMockBackdrop()

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .padding(bottom = padding.calculateBottomPadding()),
                ) {
                    // Top Navigation Capsule (shows Dhyan / Calm Centre in personal practice, Live / Courses in Live Centre)
                    DhyanTopNavigation(
                        selectedTab = selectedTab,
                        tabs = dhyanTabsFor(selectedTab),
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )

                    // Tab View Content Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        when (selectedTab) {
                            DhyanTab.DHYAN -> {
                                BreathingTab(
                                    isDarkTheme       = isDarkTheme,
                                    selectedTechnique = selectedTechnique,
                                    selectedMusicTrack = selectedMusicTrack,
                                    selectedBreathingSound = selectedBreathingSound,
                                    activeAudioSource = activeAudioSource,
                                    onActiveAudioSourceChange = { activeAudioSource = it },
                                    onBreatheWithMe   = { showTechniquesSheet = true },
                                    onOpenAudioLibrary = { showAudioLibraryPanel = true },
                                    onOpenCalmCentre  = { selectedTab = DhyanTab.CALM_CENTRE },
                                    onViewTechniqueAnimation = { technique ->
                                        previewTechnique = technique ?: selectedTechnique ?: techniques.first()
                                    },
                                    onSessionComplete = { minutes -> dhyanVm.trackCompletedSession(minutes) },
                                    onSessionActiveChange = { sessionActive = it },
                                )
                            }
                            DhyanTab.CALM_CENTRE -> {
                                com.safarparmar.app.ui.calmcentre.CalmCentreFlow(
                                    isDarkTheme = isDarkTheme,
                                    modifier = Modifier.fillMaxSize(),
                                    onExit = { selectedTab = DhyanTab.DHYAN },
                                    initialPractice = initialCalmPractice?.let { name ->
                                        runCatching { com.safarparmar.app.ui.calmcentre.CalmPractice.valueOf(name) }.getOrNull()
                                    },
                                )
                            }
                            DhyanTab.COURSES -> {
                                DhyanCoursesContent(
                                    isDarkTheme = isDarkTheme,
                                    isPremiumActive = hasCourseAccess,
                                    onNavigate = onNavigate,
                                    onGoToLive = { selectedTab = DhyanTab.LIVE },
                                )
                            }
                            DhyanTab.LIVE -> {
                                Box(Modifier.fillMaxSize()) {
                                    LiveSessionsScreen(
                                        courseId = courseId,
                                        initialView = initialLiveView,
                                        onBack = { selectedTab = DhyanTab.COURSES },
                                        onOpenSession = { sessionId -> onNavigate(Routes.liveSession(sessionId)) },
                                        onOpenCourses = { selectedTab = DhyanTab.COURSES },
                                        showTopBar = false,
                                        isDarkTheme = isDarkTheme,
                                    )
                                    com.safarparmar.app.feature.live.presentation.DhyanLiveAccessOverlay(
                                            accessState = if (hasCourseAccess) "ALLOWED" else liveAccess,
                                            onRetry = premiumVm::refreshDhyanAccess,
                                            modifier = Modifier.fillMaxSize(),
                                            isDarkTheme = isDarkTheme,
                                            onEnrollClick = { selectedTab = DhyanTab.COURSES },
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showTechniquesSheet) {
            BreathingOptionsSheet(
                selectedTechnique = selectedTechnique,
                onSelectTechnique = {
                    selectedTechnique = it
                    activeAudioSource = DhyanAudioSource.BREATHING_SOUND
                    showTechniquesSheet = false
                },
                onPreviewTechnique = {
                    showTechniquesSheet = false
                    previewTechnique = it
                },
                onDismiss = { showTechniquesSheet = false },
            )
        }

        if (previewTechnique != null) {
            BreathingTechniqueOverlayPopup(
                technique = previewTechnique!!,
                selectedTechnique = selectedTechnique,
                isDarkTheme = isDarkTheme,
                allTechniques = techniques,
                onSelectTechnique = {
                    selectedTechnique = it
                    activeAudioSource = DhyanAudioSource.BREATHING_SOUND
                    previewTechnique = null
                },
                onOpenTechniquesSheet = {
                    previewTechnique = null
                    showTechniquesSheet = true
                },
                onDismiss = { previewTechnique = null },
            )
        }

        if (showAudioLibraryPanel) {
            com.safarparmar.app.ui.audio.AudioLibraryPanel(
                selectedTrackId = selectedMusicTrack.id,
                onTrackSelect = {
                    selectedMusicTrack = it
                    activeAudioSource = DhyanAudioSource.MUSIC
                    com.safarparmar.app.ui.audio.AudioLibrary.persistTrackId(context, it.id)
                },
                onDismiss = { showAudioLibraryPanel = false },
                theme = com.safarparmar.app.ui.audio.AudioLibraryPanelTheme.Dhyan,
                isDarkTheme = isDarkTheme,
            )
        }

    } // end outer Box
    } // CompositionLocalProvider
}

// ─── Breathing Tab ─────────────────────────────────────────────────────────────
}

@Composable
internal fun DhyanCalmCentreCard(
    isDarkTheme: Boolean,
    onOpenCalmCentre: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLight = !isDarkTheme
    val cardBg = if (isLight) Color(0xFFF6F0FD) else Color(0xFF2B2135)
    val borderColor = if (isLight) RoyalPurple.copy(alpha = 0.16f) else Color(0xFF4A3855)
    val ink = if (isLight) Color(0xFF281432) else Color(0xFFF3ECF7)
    val muted = if (isLight) Color(0xFF6E5B75) else Color(0xFFB39EB8)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenCalmCentre),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLight) Color.White else Color(0xFF3D2C48)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    tint = RoyalPurple,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nav_calm_centre),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isLight) RoyalPurpleSoft else Color(0xFF452B58))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "Exercises",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLight) RoyalPurpleDark else Color(0xFFE2C9FA),
                        )
                    }
                }
                Text(
                    text = "Mindful breathing, grounding & self-kindness",
                    fontSize = 11.sp,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ComposablesIconButton(
                onClick = onOpenCalmCentre,
                style = ButtonStyle.Ghost,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.dhyan_switch_to_calm_centre),
                    tint = RoyalPurple,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun BreathingTab(
    isDarkTheme: Boolean,
    selectedTechnique: BreathingTechnique?,
    selectedMusicTrack: com.safarparmar.app.ui.audio.AudioTrack,
    selectedBreathingSound: BreathingSound,
    activeAudioSource: DhyanAudioSource,
    onActiveAudioSourceChange: (DhyanAudioSource) -> Unit,
    onBreatheWithMe: () -> Unit,
    onOpenAudioLibrary: () -> Unit,
    onOpenCalmCentre: () -> Unit = {},
    onViewTechniqueAnimation: (BreathingTechnique?) -> Unit = {},
    onSessionComplete: (Int) -> Unit,
    onSessionActiveChange: (Boolean) -> Unit,
) {
    var sessionLengthMin    by remember { mutableIntStateOf(5) }
    var isRunning           by remember { mutableStateOf(false) }
    var phase               by remember { mutableStateOf(DhyanBreathPhase.INHALE) }
    var phaseSecondsLeft    by remember { mutableIntStateOf(selectedTechnique?.inhale ?: 4) }
    var sessionSecondsLeft  by remember { mutableIntStateOf(sessionLengthMin * 60) }
    var isSessionAudioMuted by remember { mutableStateOf(false) }
    LaunchedEffect(isRunning, sessionSecondsLeft, sessionLengthMin) {
        onSessionActiveChange(isRunning || sessionSecondsLeft != sessionLengthMin * 60)
    }
    DisposableEffect(Unit) { onDispose { onSessionActiveChange(false) } }


    val context = LocalContext.current
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    fun releasePlayer() {
        val playerToRelease = mediaPlayer.value
        mediaPlayer.value = null
        if (playerToRelease != null) {
            kotlin.concurrent.thread {
                runCatching { playerToRelease.stop() }
                runCatching { playerToRelease.release() }
            }
        }
    }

    LaunchedEffect(isRunning, selectedMusicTrack, selectedBreathingSound, selectedTechnique, isSessionAudioMuted, activeAudioSource) {
        val technique = selectedTechnique
        val isMusicSelected = activeAudioSource == DhyanAudioSource.MUSIC
        val isBreathingSoundSelected = activeAudioSource == DhyanAudioSource.BREATHING_SOUND

        val shouldPlayMeditationMusic = isRunning &&
            !isSessionAudioMuted &&
            (technique == null || isMusicSelected) &&
            selectedMusicTrack.url.isNotBlank() &&
            selectedMusicTrack.name != "None" &&
            selectedMusicTrack.id != "none-track"

        val shouldPlayBreathingSound = isRunning &&
            !isSessionAudioMuted &&
            technique != null &&
            isBreathingSoundSelected &&
            (!technique.audioUrl.isNullOrBlank() || selectedBreathingSound.url.isNotBlank() || selectedBreathingSound.localResId != null)

        if (shouldPlayMeditationMusic || shouldPlayBreathingSound) {
            releasePlayer()
            try {
                val audioUri = if (shouldPlayBreathingSound) {
                    if (!technique.audioUrl.isNullOrBlank()) {
                        MediaFileCache.uriFor(context, technique.audioUrl)
                    } else {
                        selectedBreathingSound.localResId?.let {
                            Uri.parse("android.resource://${context.packageName}/$it")
                        } ?: MediaFileCache.uriFor(context, selectedBreathingSound.url)
                    }
                } else {
                    if (selectedMusicTrack.isLocal && selectedMusicTrack.localResId != null) {
                        Uri.parse("android.resource://${context.packageName}/${selectedMusicTrack.localResId}")
                    } else {
                        MediaFileCache.uriFor(context, selectedMusicTrack.url)
                    }
                }
                val mp = MediaPlayer().apply {
                    setDataSource(context, audioUri)
                    isLooping = true
                    setVolume(0.7f, 0.7f)
                    prepareAsync()
                    setOnPreparedListener { player ->
                        // prepareAsync() can finish after releasePlayer() already ran
                        // (screen left, technique/sound switched). Starting a released or
                        // superseded player throws IllegalStateException — only start if
                        // this is still the active player, and guard against the race.
                        if (mediaPlayer.value === player) {
                            runCatching { player.start() }
                        }
                    }
                }
                mediaPlayer.value = mp
            } catch (e: Exception) { /* ignore */ }
        } else {
            releasePlayer()
        }
    }

    DisposableEffect(Unit) { onDispose { releasePlayer() } }

    fun resetTimer(t: BreathingTechnique? = selectedTechnique, lengthMin: Int = sessionLengthMin) {
        isRunning = false
        phase = DhyanBreathPhase.INHALE
        phaseSecondsLeft   = t?.inhale ?: 4
        sessionSecondsLeft = lengthMin * 60
    }

    LaunchedEffect(selectedTechnique, sessionLengthMin) {
        // Always reset when technique changes (mode switch) — including back to null (default screen)
        resetTimer()
    }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning && sessionSecondsLeft > 0) {
            delay(1000L)
            sessionSecondsLeft--
            // Only drive breathing phase transitions when a technique is selected
            val t = selectedTechnique
            if (t != null) {
                phaseSecondsLeft--
                if (phaseSecondsLeft <= 0) {
                    phase = when (phase) {
                        DhyanBreathPhase.INHALE     -> if (t.hold > 0) { phaseSecondsLeft = t.hold; DhyanBreathPhase.HOLD } else { phaseSecondsLeft = t.exhale; DhyanBreathPhase.EXHALE }
                        DhyanBreathPhase.HOLD       -> { phaseSecondsLeft = t.exhale; DhyanBreathPhase.EXHALE }
                        DhyanBreathPhase.EXHALE     -> if (t.holdAfter > 0) { phaseSecondsLeft = t.holdAfter; DhyanBreathPhase.HOLD_AFTER } else { phaseSecondsLeft = t.inhale; DhyanBreathPhase.INHALE }
                        DhyanBreathPhase.HOLD_AFTER -> { phaseSecondsLeft = t.inhale; DhyanBreathPhase.INHALE }
                    }
                }
            }
        }
        if (sessionSecondsLeft <= 0) {
            onSessionComplete(sessionLengthMin)
            isRunning = false
        }
    }

    val vizPhase = when (phase) {
        DhyanBreathPhase.INHALE     -> BreathPhase.INHALE
        DhyanBreathPhase.HOLD       -> BreathPhase.HOLD
        DhyanBreathPhase.EXHALE     -> BreathPhase.EXHALE
        DhyanBreathPhase.HOLD_AFTER -> BreathPhase.HOLD_EMPTY
    }
    val vizCycle = BreathCycle(
        inhale  = selectedTechnique?.inhale ?: 4,
        holdIn  = selectedTechnique?.hold ?: 0,
        exhale  = selectedTechnique?.exhale ?: 4,
        holdOut = selectedTechnique?.holdAfter ?: 0,
    )
    val vizSessionId = techniqueSessionId(selectedTechnique)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DhyanContentHorizontal)
            .padding(top = 2.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = !isRunning,
            enter = fadeIn(tween(250)) + expandVertically(),
            exit = fadeOut(tween(200)) + shrinkVertically(),
        ) {
            DhyanCalmCentreCard(
                isDarkTheme = isDarkTheme,
                onOpenCalmCentre = onOpenCalmCentre,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        // Hero cluster (orb + timer) — shares remaining height so lower controls stay compact.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = selectedTechnique,
                transitionSpec = {
                    (fadeIn(tween(400)) + scaleIn(initialScale = 0.95f))
                        .togetherWith(fadeOut(tween(280)) + scaleOut(targetScale = 0.97f))
                },
                label = "dhyanHero",
            ) { technique ->
                if (technique == null) {
                    DhyanMeditationOrb(isDarkTheme = isDarkTheme) {
                        Image(
                            painter = painterResource(R.drawable.dhyan_meditation_lavender),
                            contentDescription = stringResource(R.string.dhyan_meditate),
                            modifier = Modifier.fillMaxSize(0.88f),
                            contentScale = ContentScale.Fit,
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DhyanMeditationOrb(isDarkTheme = isDarkTheme) {
                            BreathingVisualizer(
                                sessionId = vizSessionId,
                                breathPhase = vizPhase,
                                isActive = isRunning,
                                cycle = vizCycle,
                                modifier = Modifier.fillMaxSize(0.92f),
                            )
                        }
                        AnimatedVisibility(visible = isRunning) {
                            Text(
                                stringResource(
                                    when (phase) {
                                        DhyanBreathPhase.INHALE -> R.string.dhyan_phase_inhale
                                        DhyanBreathPhase.HOLD -> R.string.dhyan_phase_hold
                                        DhyanBreathPhase.EXHALE -> R.string.dhyan_phase_exhale
                                        DhyanBreathPhase.HOLD_AFTER -> R.string.dhyan_phase_rest
                                    }
                                ).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = DhyanFlatColors.Muted,
                            )
                        }
                        // Flat technique chip
                        Box(
                            modifier = Modifier
                                .clip(DhyanFlatShape)
                                .border(1.dp, DhyanColors.rose(isDarkTheme).copy(alpha = 0.55f), DhyanFlatShape)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(id = technique.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = DhyanColors.rose(isDarkTheme),
                                )
                                Text(
                                    stringResource(R.string.dhyan_technique_pattern, stringResource(technique.nameRes), technique.pattern),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DhyanFlatColors.Text,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "%02d:%02d".format(sessionSecondsLeft / 60, sessionSecondsLeft % 60),
                fontFamily = LoraFontFamily,
                fontSize = 52.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = (-2).sp,
                color = DhyanFlatColors.Text,
            )
        }

        Spacer(Modifier.height(DhyanSectionGap))

        // Flat hairline session length — structure only (no card).
        PlanHairline(alpha = 0.7f)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.dhyan_session_length).uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = DhyanFlatColors.Muted,
            )
            Text(
                stringResource(R.string.dhyan_minutes_short, sessionLengthMin),
                fontFamily = LoraFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = DhyanFlatColors.Text,
            )
        }
        Spacer(Modifier.height(12.dp))
        DhyanSessionSlider(
            value = sessionLengthMin.toFloat(),
            onValueChange = {
                sessionLengthMin = it.toInt()
                if (!isRunning) resetTimer(lengthMin = it.toInt())
            },
            isDarkTheme = isDarkTheme,
            valueRange = 1f..60f,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        PlanHairline(alpha = 0.7f)

        Spacer(Modifier.height(DhyanSectionGap))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DhyanControlGap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DhyanControlButton(
                icon = Icons.Default.Refresh,
                isDarkTheme = isDarkTheme,
                onClick = { resetTimer() },
                size = DhyanSideControlSize,
                style = DhyanControlStyle.Reset,
                contentDescription = stringResource(R.string.common_reset),
            )
            DhyanControlButton(
                icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                isDarkTheme = isDarkTheme,
                onClick = { isRunning = !isRunning },
                size = DhyanPlaySize,
                style = DhyanControlStyle.Play,
                contentDescription = stringResource(if (isRunning) R.string.common_pause else R.string.common_play),
            )
            DhyanControlButton(
                icon = if (isSessionAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                isDarkTheme = isDarkTheme,
                onClick = { isSessionAudioMuted = !isSessionAudioMuted },
                size = DhyanSideControlSize,
                style = DhyanControlStyle.Volume,
                contentDescription = stringResource(if (isSessionAudioMuted) R.string.common_unmute else R.string.common_mute),
            )
        }

        Spacer(Modifier.height(22.dp))

        DhyanGuidanceSheet(
            isDarkTheme = isDarkTheme,
            technique = selectedTechnique,
            onEdit = onBreatheWithMe,
            onOpenAudioLibrary = onOpenAudioLibrary,
        )
    }
}

/**
 * Flat-hairline guidance chrome + glass row for the tappable technique picker.
 */
@Composable
internal fun DhyanGuidanceSheet(
    isDarkTheme: Boolean,
    technique: BreathingTechnique?,
    onEdit: () -> Unit,
    onOpenAudioLibrary: () -> Unit,
) {
    val isLight = !isDarkTheme
    val title = technique?.let {
        stringResource(R.string.dhyan_technique_pattern, stringResource(it.nameRes), it.pattern)
    } ?: stringResource(R.string.dhyan_breathing_techniques)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(DhyanFlatColors.Hairline),
        )
        Spacer(Modifier.height(14.dp))
        PlanHairline(alpha = 0.65f)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    stringResource(R.string.dhyan_guidance),
                    fontFamily = LoraFontFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = DhyanFlatColors.Text,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.dhyan_choose_breathe),
                    fontSize = 12.5.sp,
                    color = DhyanFlatColors.Muted,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DhyanFlatColors.PrimaryContainer)
                    .border(width = 0.5.dp, color = DhyanFlatColors.BorderHairline, shape = CircleShape)
                    .tutorialTarget("dhyan.sound").clickable(onClick = onOpenAudioLibrary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = stringResource(R.string.dhyan_audio_library),
                    tint = DhyanFlatColors.Primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PlanHairline(alpha = 0.55f)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dhyanMacOSPanel(isLight = isLight, shape = DhyanFlatShape)
                .tutorialTarget("dhyan.guidance").clickable(onClick = onEdit)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Air,
                contentDescription = null,
                tint = DhyanColors.actionPink(isDarkTheme),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DhyanFlatColors.onGlassText(isLight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Default.Edit,
                contentDescription = stringResource(R.string.dhyan_choose_guidance),
                tint = DhyanColors.actionPink(isDarkTheme),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─── Flat hairline sheets ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingOptionsSheet(
    selectedTechnique: BreathingTechnique?,
    onSelectTechnique: (BreathingTechnique) -> Unit,
    onPreviewTechnique: (BreathingTechnique) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDarkTheme = !MaterialTheme.colorScheme.background.isLightBackground()

    DhyanSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            PlanEyebrow(stringResource(R.string.module_dhyan))
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.dhyan_breathing_techniques),
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                color = DhyanFlatColors.Text,
            )
            Text(
                stringResource(R.string.dhyan_choose_technique),
                fontSize = 13.sp,
                color = DhyanFlatColors.Muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PlanHairline()

            techniques.forEach { t ->
                val isSelected = t.nameRes == selectedTechnique?.nameRes
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(onClick = { onPreviewTechnique(t) })
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) DhyanColors.actionPink(isDarkTheme)
                                    else DhyanFlatColors.PrimarySoft,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = t.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) Color.White else DhyanColors.actionPink(isDarkTheme),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(t.nameRes), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhyanFlatColors.Text)
                            Text(stringResource(t.descriptionRes), fontSize = 12.sp, color = DhyanFlatColors.Muted, lineHeight = 17.sp)
                        }
                        Text(
                            t.pattern,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DhyanColors.actionPink(isDarkTheme),
                            modifier = Modifier
                                .border(1.dp, DhyanColors.actionPink(isDarkTheme).copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DhyanFlatColors.PrimarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = stringResource(R.string.dhyan_view_animation),
                                tint = DhyanColors.actionPink(isDarkTheme),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = DhyanColors.actionPink(isDarkTheme), modifier = Modifier.size(20.dp))
                        }
                    }
                    PlanHairline(alpha = 0.55f)
                }
            }
        }
    }
}

/**
 * Overlay popup showing animated breathing techniques with real-time visualizers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingTechniqueOverlayPopup(
    technique: BreathingTechnique,
    selectedTechnique: BreathingTechnique?,
    isDarkTheme: Boolean,
    allTechniques: List<BreathingTechnique> = techniques,
    onSelectTechnique: (BreathingTechnique) -> Unit,
    onOpenTechniquesSheet: () -> Unit,
    onDismiss: () -> Unit,
) {
    var currentTechnique by remember(technique) { mutableStateOf(technique) }
    var isPreviewActive by remember { mutableStateOf(true) }
    var previewPhase by remember(currentTechnique) { mutableStateOf(BreathPhase.INHALE) }
    var phaseSecondsLeft by remember(currentTechnique) { mutableIntStateOf(currentTechnique.inhale) }

    LaunchedEffect(currentTechnique, isPreviewActive) {
        if (!isPreviewActive) return@LaunchedEffect
        previewPhase = BreathPhase.INHALE
        phaseSecondsLeft = currentTechnique.inhale
        while (isPreviewActive) {
            delay(1000L)
            phaseSecondsLeft--
            if (phaseSecondsLeft <= 0) {
                previewPhase = when (previewPhase) {
                    BreathPhase.INHALE -> if (currentTechnique.hold > 0) {
                        phaseSecondsLeft = currentTechnique.hold
                        BreathPhase.HOLD
                    } else {
                        phaseSecondsLeft = currentTechnique.exhale
                        BreathPhase.EXHALE
                    }
                    BreathPhase.HOLD -> {
                        phaseSecondsLeft = currentTechnique.exhale
                        BreathPhase.EXHALE
                    }
                    BreathPhase.EXHALE -> if (currentTechnique.holdAfter > 0) {
                        phaseSecondsLeft = currentTechnique.holdAfter
                        BreathPhase.HOLD_EMPTY
                    } else {
                        phaseSecondsLeft = currentTechnique.inhale
                        BreathPhase.INHALE
                    }
                    BreathPhase.HOLD_EMPTY -> {
                        phaseSecondsLeft = currentTechnique.inhale
                        BreathPhase.INHALE
                    }
                }
            }
        }
    }

    val previewCycle = BreathCycle(
        inhale = currentTechnique.inhale,
        holdIn = currentTechnique.hold,
        exhale = currentTechnique.exhale,
        holdOut = currentTechnique.holdAfter,
    )
    val sessionId = techniqueSessionId(currentTechnique)

    DhyanSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row: Eyebrow + Close
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlanEyebrow(stringResource(R.string.module_dhyan))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = DhyanFlatColors.Muted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Title + Pattern tag
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(currentTechnique.nameRes),
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = DhyanFlatColors.Text,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(currentTechnique.descriptionRes),
                        fontSize = 13.sp,
                        color = DhyanFlatColors.Muted,
                        lineHeight = 18.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    currentTechnique.pattern,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DhyanColors.actionPink(isDarkTheme),
                    modifier = Modifier
                        .border(
                            1.dp,
                            DhyanColors.actionPink(isDarkTheme).copy(alpha = 0.45f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            // Horizontal switcher chips so user can immediately preview any technique animation
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                allTechniques.forEach { t ->
                    val isCurrent = t.nameRes == currentTechnique.nameRes
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrent) DhyanColors.actionPink(isDarkTheme)
                                else DhyanFlatColors.PrimarySoft,
                            )
                            .clickable { currentTechnique = t }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(t.nameRes),
                            fontSize = 12.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) Color.White else DhyanFlatColors.Text,
                        )
                    }
                }
            }

            PlanHairline(alpha = 0.6f)

            // Live Animated Breathing Visualizer Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(RoyalPurple, Color(0xFF32132D))
                        )
                    )
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier.size(240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BreathingVisualizer(
                            sessionId = sessionId,
                            breathPhase = previewPhase,
                            isActive = isPreviewActive,
                            cycle = previewCycle,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Phase status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        val phaseText = when (previewPhase) {
                            BreathPhase.INHALE -> stringResource(R.string.dhyan_phase_inhale)
                            BreathPhase.HOLD -> stringResource(R.string.dhyan_phase_hold)
                            BreathPhase.EXHALE -> stringResource(R.string.dhyan_phase_exhale)
                            BreathPhase.HOLD_EMPTY -> stringResource(R.string.dhyan_phase_rest)
                        }
                        Text(
                            text = "$phaseText · ${phaseSecondsLeft}s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFFFAF8FF),
                        )
                    }
                }
            }

            // Technique Timing Breakdown
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TechniqueStepBadge(stringResource(R.string.dhyan_phase_inhale), "${currentTechnique.inhale}s", Modifier.weight(1f), isDarkTheme)
                if (currentTechnique.hold > 0) {
                    TechniqueStepBadge(stringResource(R.string.dhyan_phase_hold), "${currentTechnique.hold}s", Modifier.weight(1f), isDarkTheme)
                }
                TechniqueStepBadge(stringResource(R.string.dhyan_phase_exhale), "${currentTechnique.exhale}s", Modifier.weight(1f), isDarkTheme)
                if (currentTechnique.holdAfter > 0) {
                    TechniqueStepBadge(stringResource(R.string.dhyan_phase_rest), "${currentTechnique.holdAfter}s", Modifier.weight(1f), isDarkTheme)
                }
            }

            // Actions: Pause/Resume animation, Choose Another, and Primary Select Button
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { isPreviewActive = !isPreviewActive },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DhyanFlatColors.PrimarySoft),
                ) {
                    Icon(
                        imageVector = if (isPreviewActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(if (isPreviewActive) R.string.common_pause else R.string.common_play),
                        tint = DhyanColors.actionPink(isDarkTheme),
                        modifier = Modifier.size(24.dp),
                    )
                }

                OutlinedButton(
                    onClick = onOpenTechniquesSheet,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DhyanFlatColors.BorderHairline),
                ) {
                    Text(
                        stringResource(R.string.dhyan_breathing_techniques),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DhyanFlatColors.Text,
                    )
                }

                val isCurrentSelected = selectedTechnique != null && currentTechnique.nameRes == selectedTechnique.nameRes
                Button(
                    onClick = {
                        if (!isCurrentSelected) {
                            onSelectTechnique(currentTechnique)
                        }
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DhyanColors.actionPink(isDarkTheme),
                        contentColor = Color.White,
                    ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isCurrentSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            stringResource(if (isCurrentSelected) R.string.dhyan_technique_selected else R.string.dhyan_use_technique),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TechniqueStepBadge(
    label: String,
    duration: String,
    modifier: Modifier,
    isDarkTheme: Boolean,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DhyanFlatColors.CardBg)
            .border(0.5.dp, DhyanFlatColors.BorderHairline, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = DhyanFlatColors.Muted,
            maxLines = 1,
        )
        Text(
            duration,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DhyanColors.actionPink(isDarkTheme),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingSoundSheet(
    selectedSound: BreathingSound,
    onSelectSound: (BreathingSound) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDarkTheme = !MaterialTheme.colorScheme.background.isLightBackground()

    DhyanSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            PlanEyebrow(stringResource(R.string.module_dhyan))
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.dhyan_breathing_sounds),
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                color = DhyanFlatColors.Text,
            )
            Text(
                stringResource(R.string.dhyan_guidance_audio_help),
                fontSize = 13.sp,
                color = DhyanFlatColors.Muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PlanHairline()

            breathingSounds.forEach { sound ->
                val isSelected = sound.id == selectedSound.id
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(onClick = { onSelectSound(sound) })
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) DhyanColors.actionPink(isDarkTheme)
                                    else DhyanFlatColors.PrimarySoft,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) Color.White else DhyanColors.actionPink(isDarkTheme),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(sound.nameRes), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhyanFlatColors.Text)
                            Text(stringResource(sound.descriptionRes), fontSize = 12.sp, color = DhyanFlatColors.Muted, lineHeight = 17.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = DhyanColors.actionPink(isDarkTheme), modifier = Modifier.size(20.dp))
                        }
                    }
                    PlanHairline(alpha = 0.55f)
                }
            }
        }
    }
}


/** Composables UI sheet with Dhyan's existing black 45% scrim. */
@Composable
private fun DhyanSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        com.safarparmar.app.ui.theme.LocalSafarComposablesScrim provides Color.Black.copy(alpha = 0.45f),
    ) {
        com.safarparmar.app.ui.theme.SafarComposablesTheme {
            com.safarparmar.app.ui.components.popups.SafarComposablesBottomSheet(
                onDismissRequest = onDismiss,
                state = com.composables.ui.components.rememberBottomSheetState(
                    initialDetent = com.composables.ui.components.BottomSheetDetent.FullyExpanded,
                ),
                backgroundColor = DhyanFlatColors.ComponentBackground,
                contentColor = DhyanFlatColors.Text,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                contentPadding = PaddingValues(0.dp),
            ) { content() }
        }
    }
}
