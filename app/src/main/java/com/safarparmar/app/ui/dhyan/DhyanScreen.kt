
package com.safarparmar.app.ui.dhyan

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safarparmar.app.R
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground
import com.safarparmar.app.util.bounceClick
import kotlinx.coroutines.delay
import com.safarparmar.app.ui.audio.MediaFileCache

// ─── Data ──────────────────────────────────────────────────────────────────────

private data class BreathingTechnique(
    val name: String,
    val iconRes: Int,
    val description: String,
    val inhale: Int,
    val hold: Int,
    val exhale: Int,
    val holdAfter: Int = 0,
    val pattern: String,
    val audioUrl: String? = null,
)

private data class BreathingSound(
    val id: String,
    val name: String,
    val description: String,
    val url: String = "",
    val localResId: Int? = null,
)

private val techniques = listOf(
    BreathingTechnique("Diaphragmatic", com.safarparmar.app.R.drawable.ic_wind, "Belly breathing for full oxygen exchange", 4, 0, 6, 0, "4-6"),
    BreathingTechnique("Pursed Lip", com.safarparmar.app.R.drawable.ic_wind, "Slows breathing and keeps airways open", 2, 0, 4, 0, "2-4"),
    BreathingTechnique("Box Breathing", com.safarparmar.app.R.drawable.ic_square, "Rhythmic 4-4-4-4 for stress reduction", 4, 4, 4, 4, "4-4-4-4", "https://qms-images.del1.vultrobjects.com/qms-parmar-academy/music/box_breathing.mp3"),
    BreathingTechnique("4-7-8 Breathing", com.safarparmar.app.R.drawable.ic_moon, "Deep relaxation for anxiety and sleep", 4, 7, 8, 0, "4-7-8", "https://qms-images.del1.vultrobjects.com/qms-parmar-academy/music/four_seven_eight.mp3"),
    BreathingTechnique("6-7-8 Breathing", com.safarparmar.app.R.drawable.ic_yin_yang, "Slower inhale variation for deeper calm", 6, 7, 8, 0, "6-7-8"),
)

private val breathingSounds = listOf(
    BreathingSound(
        id = "silent-breathing",
        name = "Silent Guidance",
        description = "No background music during breathing techniques",
    ),
)


private enum class DhyanBreathPhase(val label: String) {
    INHALE("INHALE"), HOLD("HOLD"), EXHALE("EXHALE"), HOLD_AFTER("REST")
}

private enum class DhyanAudioSource {
    MUSIC, BREATHING_SOUND
}

// musicOptions removed in favor of shared AudioLibrary

// ─── Dhyan brand accents (pink) + hybrid glass/flat helpers ─────────────────

private object DhyanColors {
    val LightLotus = Color(0xFFFFCDE0)
    val LightRose = Color(0xFFF49BB7)
    val LightCalm = Color(0xFFE37A9A)
    val DarkLotus = Color(0xFFE05282)
    val DarkRose = Color(0xFFB82D5C)
    val DarkCalm = Color(0xFF8A133B)

    fun lotus(isDark: Boolean) = if (isDark) DarkLotus else LightLotus
    fun rose(isDark: Boolean) = if (isDark) DarkRose else LightRose
    fun calm(isDark: Boolean) = if (isDark) DarkCalm else LightCalm
    fun sky(isDark: Boolean) = if (isDark) Color(0xFF7CB9E8) else Color(0xFF5B9BD5)
    fun accentBlue(isDark: Boolean) = sky(isDark)
    fun actionPink(isDark: Boolean) = if (isDark) Color(0xFFE86B96) else Color(0xFFF04880)
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

/** Pink gradient canvas (plan: keep Dhyan brand atmosphere). */
@Composable
private fun DhyanMockBackdrop() {
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

/** Pink glass meditation sphere — brand hero tile (visualizer inside untouched). */
@Composable
private fun DhyanMeditationOrb(
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

/** Flat outlined circular top-bar chip (not glass). */
@Composable
private fun DhyanTopBarChip(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, DhyanFlatColors.Hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun DhyanSessionSlider(
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
private fun DhyanControlButton(
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

private enum class DhyanControlStyle { Reset, Play, Volume }

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
    currentRoute: String = Routes.DHYAN,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
) {
    var showAudioLibraryPanel by remember { mutableStateOf(false) }
    var showTechniquesSheet   by remember { mutableStateOf(false) }
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

    CompositionLocalProvider(LocalPlannerIsDarkTheme provides isDarkTheme) {
    Box(Modifier.fillMaxSize()) {
        SafarDrawerScaffold(
            title    = "Dhyan",
            subtitle = null,
            currentRoute      = currentRoute,
            isDarkTheme       = isDarkTheme,
            onNavigate        = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            useGlassTopBar    = false,
            useDetachedMenuGlass = false,
            containerColor    = Color.Transparent,
            topBarActions = {
                DhyanTopBarChip(onClick = { showAudioLibraryPanel = true }) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Meditation Audio Library",
                        tint = DhyanFlatColors.Text,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                DhyanMockBackdrop()

                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .padding(bottom = padding.calculateBottomPadding()),
                ) {
                    BreathingTab(
                        isDarkTheme       = isDarkTheme,
                        selectedTechnique = selectedTechnique,
                        selectedMusicTrack = selectedMusicTrack,
                        selectedBreathingSound = selectedBreathingSound,
                        activeAudioSource = activeAudioSource,
                        onActiveAudioSourceChange = { activeAudioSource = it },
                        onBreatheWithMe   = { showTechniquesSheet = true },
                        onSessionComplete = { minutes -> dhyanVm.trackCompletedSession(minutes) },
                    )
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
                onDismiss = { showTechniquesSheet = false },
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

@Composable
private fun BreathingTab(
    isDarkTheme: Boolean,
    selectedTechnique: BreathingTechnique?,
    selectedMusicTrack: com.safarparmar.app.ui.audio.AudioTrack,
    selectedBreathingSound: BreathingSound,
    activeAudioSource: DhyanAudioSource,
    onActiveAudioSourceChange: (DhyanAudioSource) -> Unit,
    onBreatheWithMe: () -> Unit,
    onSessionComplete: (Int) -> Unit,
) {
    var sessionLengthMin    by remember { mutableIntStateOf(5) }
    var isRunning           by remember { mutableStateOf(false) }
    var phase               by remember { mutableStateOf(DhyanBreathPhase.INHALE) }
    var phaseSecondsLeft    by remember { mutableIntStateOf(selectedTechnique?.inhale ?: 4) }
    var sessionSecondsLeft  by remember { mutableIntStateOf(sessionLengthMin * 60) }
    var isSessionAudioMuted by remember { mutableStateOf(false) }

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
    val vizSessionId = when (techniques.indexOf(selectedTechnique)) {
        0 -> "1"; 1 -> "2"; 2 -> "3"; 3 -> "4"; else -> "1"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DhyanContentHorizontal)
            .padding(top = 2.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "\"Silence is the language of God.\"",
            fontSize = 12.sp,
            color = DhyanFlatColors.Muted.copy(alpha = 0.9f),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp),
        )

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
                            painter = painterResource(R.drawable.meditation_transparent_background),
                            contentDescription = "Meditate",
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
                                phase.label,
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
                                    "${technique.name} · ${technique.pattern}",
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
                "SESSION LENGTH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = DhyanFlatColors.Muted,
            )
            Text(
                "${sessionLengthMin} min",
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
                contentDescription = "Reset",
            )
            DhyanControlButton(
                icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                isDarkTheme = isDarkTheme,
                onClick = { isRunning = !isRunning },
                size = DhyanPlaySize,
                style = DhyanControlStyle.Play,
                contentDescription = if (isRunning) "Pause" else "Play",
            )
            DhyanControlButton(
                icon = if (isSessionAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                isDarkTheme = isDarkTheme,
                onClick = { isSessionAudioMuted = !isSessionAudioMuted },
                size = DhyanSideControlSize,
                style = DhyanControlStyle.Volume,
                contentDescription = if (isSessionAudioMuted) "Unmute" else "Mute",
            )
        }

        Spacer(Modifier.height(22.dp))

        DhyanGuidanceSheet(
            isDarkTheme = isDarkTheme,
            technique = selectedTechnique,
            onEdit = onBreatheWithMe,
        )
    }
}

/**
 * Flat-hairline guidance chrome + glass row for the tappable technique picker.
 */
@Composable
private fun DhyanGuidanceSheet(
    isDarkTheme: Boolean,
    technique: BreathingTechnique?,
    onEdit: () -> Unit,
) {
    val isLight = !isDarkTheme
    val title = technique?.let { "${it.name} · ${it.pattern}" } ?: "Breathing techniques"
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
        Text(
            "Guidance",
            fontFamily = LoraFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal,
            color = DhyanFlatColors.Text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose how you want to breathe",
            fontSize = 12.5.sp,
            color = DhyanFlatColors.Muted,
        )
        Spacer(Modifier.height(12.dp))
        PlanHairline(alpha = 0.55f)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .dhyanMacOSPanel(isLight = isLight, shape = DhyanFlatShape)
                .clickable(onClick = onEdit)
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
                contentDescription = "Choose breathing guidance",
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
    onDismiss: () -> Unit,
) {
    val isDarkTheme = !MaterialTheme.colorScheme.background.isLightBackground()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DhyanFlatColors.Bg,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DhyanFlatColors.Hairline) },
    ) {
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
            PlanEyebrow("Dhyan")
            Spacer(Modifier.height(6.dp))
            Text(
                "Breathe with me",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                color = DhyanFlatColors.Text,
            )
            Text(
                "Choose a technique to start",
                fontSize = 13.sp,
                color = DhyanFlatColors.Muted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            PlanHairline()

            techniques.forEach { t ->
                val isSelected = t.name == selectedTechnique?.name
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(onClick = { onSelectTechnique(t) })
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
                            Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhyanFlatColors.Text)
                            Text(t.description, fontSize = 12.sp, color = DhyanFlatColors.Muted, lineHeight = 17.sp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingSoundSheet(
    selectedSound: BreathingSound,
    onSelectSound: (BreathingSound) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDarkTheme = !MaterialTheme.colorScheme.background.isLightBackground()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DhyanFlatColors.Bg,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle(color = DhyanFlatColors.Hairline) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            PlanEyebrow("Dhyan")
            Spacer(Modifier.height(6.dp))
            Text(
                "Breathing sounds",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                color = DhyanFlatColors.Text,
            )
            Text(
                "Guidance audio for your technique",
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
                            Text(sound.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DhyanFlatColors.Text)
                            Text(sound.description, fontSize = 12.sp, color = DhyanFlatColors.Muted, lineHeight = 17.sp)
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
