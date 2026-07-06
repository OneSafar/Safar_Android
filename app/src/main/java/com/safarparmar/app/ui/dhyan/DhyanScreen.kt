
package com.safarparmar.app.ui.dhyan

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.safarparmar.app.util.bounceClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.safarparmar.app.R
import com.safarparmar.app.ui.components.SafarErrorState
import com.safarparmar.app.ui.components.StatCardSkeleton
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import com.safarparmar.app.ui.theme.*
import com.safarparmar.app.ui.tour.TourManager
import com.safarparmar.app.ui.tour.dhyanTourSteps
import kotlinx.coroutines.delay

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

private val intensityLabels = listOf("Light", "Gentle", "Moderate", "Deep", "Intense")

private enum class DhyanBreathPhase(val label: String) {
    INHALE("INHALE"), HOLD("HOLD"), EXHALE("EXHALE"), HOLD_AFTER("REST")
}

// musicOptions removed in favor of shared AudioLibrary

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
    var showBreathingSoundSheet by remember { mutableStateOf(false) }
    var showTechniquesSheet   by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedMusicTrack    by remember { mutableStateOf(com.safarparmar.app.ui.audio.AudioLibrary.getPersistedTrack(context)) }
    var selectedBreathingSound by remember { mutableStateOf(breathingSounds.first()) }
    // null = no technique chosen (show image), non-null = show animation
    var selectedTechnique   by remember { mutableStateOf<BreathingTechnique?>(null) }
    var tourState           by remember { mutableStateOf<com.safarparmar.app.ui.butterfly.ButterflyTourState?>(null) }

    val themeVm: ThemeViewModel = hiltViewModel()
    val dhyanVm: DhyanViewModel = hiltViewModel()

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        SafarDrawerScaffold(
            title    = "Dhyan",
            subtitle = "SAFAR",
            currentRoute      = currentRoute,
            isDarkTheme       = isDarkTheme,
            onNavigate        = onNavigate,
            onToggleDarkTheme = onToggleDarkTheme,
            topBarActions = {
                IconButton(onClick = {
                    if (selectedTechnique == null) showAudioLibraryPanel = true
                    else showBreathingSoundSheet = true
                }) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = if (selectedTechnique == null) "Meditation Audio Library" else "Breathing Sounds",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { tourState?.start() }) {
                    Image(
                        painter = painterResource(R.drawable.ic_butterfly_tour),
                        contentDescription = "Guide",
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                val bgImageRes = com.safarparmar.app.R.drawable.dhyan_section_bg_new
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = bgImageRes),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isDarkTheme) Color.Black.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.65f)
                        )
                )

                Box(
                    Modifier.fillMaxSize().padding(
                        top = padding.calculateTopPadding(),
                    )
                ) {
                    BreathingTab(
                        isDarkTheme       = isDarkTheme,
                        selectedTechnique = selectedTechnique,
                        selectedMusicTrack = selectedMusicTrack,
                        selectedBreathingSound = selectedBreathingSound,
                        onBreatheWithMe   = { showTechniquesSheet = true },
                        onClearTechnique  = { selectedTechnique = null },
                        onSessionComplete = { minutes -> dhyanVm.trackCompletedSession(minutes) },
                    )
                }
            }
        }

        TourManager(
            dataStore        = themeVm.dataStore,
            steps            = dhyanTourSteps,
            section          = "dhyan",
            askOnFirstVisit  = true,
            onTourStateReady = { tourState = it },
        )

        if (showTechniquesSheet) {
            BreathingOptionsSheet(
                selectedTechnique = selectedTechnique,
                onSelectTechnique = {
                    selectedTechnique = it
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
                    com.safarparmar.app.ui.audio.AudioLibrary.persistTrackId(context, it.id)
                },
                onDismiss = { showAudioLibraryPanel = false }
            )
        }

        if (showBreathingSoundSheet) {
            BreathingSoundSheet(
                selectedSound = selectedBreathingSound,
                onSelectSound = {
                    selectedBreathingSound = it
                    showBreathingSoundSheet = false
                },
                onDismiss = { showBreathingSoundSheet = false },
            )
        }
    } // end outer Box
}

// ─── Breathing Tab ─────────────────────────────────────────────────────────────

@Composable
private fun BreathingTab(
    isDarkTheme: Boolean,
    selectedTechnique: BreathingTechnique?,
    selectedMusicTrack: com.safarparmar.app.ui.audio.AudioTrack,
    selectedBreathingSound: BreathingSound,
    onBreatheWithMe: () -> Unit,
    onClearTechnique: () -> Unit,
    onSessionComplete: (Int) -> Unit,
) {
    val LightLotusPink = Color(0xFFFFCDE0)
    val MediumRosePink = Color(0xFFF49BB7)
    val DeepCalmingPink = Color(0xFFE37A9A)

    val DarkLotusPink = Color(0xFF8C4A60)
    val DarkMediumRosePink = Color(0xFF6E2F44)
    val DarkDeepCalmingPink = Color(0xFF4C182A)

    val currentLotusPink = if (isDarkTheme) DarkLotusPink else LightLotusPink
    val currentRosePink = if (isDarkTheme) DarkMediumRosePink else MediumRosePink
    val currentCalmingPink = if (isDarkTheme) DarkDeepCalmingPink else DeepCalmingPink

    val dhyanGradient = Brush.verticalGradient(listOf(currentLotusPink, currentRosePink, currentCalmingPink))

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

    LaunchedEffect(isRunning, selectedMusicTrack, selectedBreathingSound, selectedTechnique, isSessionAudioMuted) {
        val technique = selectedTechnique
        val shouldPlayMeditationMusic = isRunning &&
            !isSessionAudioMuted &&
            technique == null &&
            selectedMusicTrack.url.isNotBlank() &&
            selectedMusicTrack.name != "None" &&
            selectedMusicTrack.id != "none-track"
        val shouldPlayBreathingSound = isRunning &&
            !isSessionAudioMuted &&
            technique != null &&
            (!technique.audioUrl.isNullOrBlank() || selectedBreathingSound.url.isNotBlank() || selectedBreathingSound.localResId != null)

        if (shouldPlayMeditationMusic || shouldPlayBreathingSound) {
            releasePlayer()
            try {
                val audioUri = if (shouldPlayBreathingSound) {
                    if (!technique.audioUrl.isNullOrBlank()) {
                        Uri.parse(technique.audioUrl)
                    } else {
                        selectedBreathingSound.localResId?.let {
                            Uri.parse("android.resource://${context.packageName}/$it")
                        } ?: Uri.parse(selectedBreathingSound.url)
                    }
                } else {
                    if (selectedMusicTrack.isLocal && selectedMusicTrack.localResId != null) {
                        Uri.parse("android.resource://${context.packageName}/${selectedMusicTrack.localResId}")
                    } else {
                        Uri.parse(selectedMusicTrack.url)
                    }
                }
                val mp = MediaPlayer().apply {
                    setDataSource(context, audioUri)
                    isLooping = true
                    setVolume(0.7f, 0.7f)
                    prepareAsync()
                    setOnPreparedListener { start() }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "\"Silence is the language of God.\"",
            fontSize  = 12.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )



        // ── Visual area: image OR breathing animation (Flexible height) ────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = selectedTechnique,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
                label = "vizSwitch",
            ) { technique ->
                if (technique == null) {
                    // Default pulsing circle image
                    val pulseAnim = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by pulseAnim.animateFloat(
                        initialValue  = 1f,
                        targetValue   = 1.05f,
                        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
                        label         = "pulseScale",
                    )
                    Box(
                        modifier = Modifier
                            .sizeIn(maxHeight = 200.dp, maxWidth = 200.dp)
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                            .clip(CircleShape)
                            .border(3.dp, currentRosePink.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Image(
                            painter            = androidx.compose.ui.res.painterResource(id = com.safarparmar.app.R.drawable.meditation_transparent_background),
                            contentDescription = "Meditate",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Fit,
                        )
                    }
                } else {
                    // Live breathing animation + technique chip
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .sizeIn(maxHeight = 200.dp, maxWidth = 200.dp)
                                .aspectRatio(1f)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            currentRosePink.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BreathingVisualizer(
                                sessionId   = vizSessionId,
                                breathPhase = vizPhase,
                                isActive    = isRunning,
                                cycle       = vizCycle,
                                modifier    = Modifier.fillMaxSize(),
                            )
                        }
                        if (isRunning) {
                            Text(
                                phase.label,
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color         = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primary.copy(0.12f))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(painter = androidx.compose.ui.res.painterResource(id = technique.iconRes), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text("${technique.name} · ${technique.pattern}",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(0.12f))
                                    .clickable { onClearTechnique(); resetTimer(null) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear technique",
                                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Large timer
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = density.density,
                fontScale = density.fontScale.coerceAtMost(1.3f)
            )
        ) {
            Text(
                "%02d:%02d".format(sessionSecondsLeft / 60, sessionSecondsLeft % 60),
                fontSize      = 48.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = (-2).sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Session length card
        Card(
            shape     = MaterialTheme.shapes.large,
            modifier  = Modifier.fillMaxWidth(),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp),
            border    = CardDefaults.outlinedCardBorder(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text("SESSION LENGTH", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${sessionLengthMin} min", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
                SlimSlider(
                    value         = sessionLengthMin.toFloat(),
                    onValueChange = { sessionLengthMin = it.toInt(); if (!isRunning) resetTimer(lengthMin = it.toInt()) },
                    valueRange    = 1f..60f,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    activeColor   = currentCalmingPink,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Play / Pause / Reset row
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(
                onClick  = { resetTimer() },
                modifier = Modifier.size(48.dp),
                colors   = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Refresh, null)
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) Brush.verticalGradient(listOf(currentCalmingPink, currentRosePink)) else dhyanGradient)
                    .clickable { isRunning = !isRunning },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
            FilledTonalIconButton(
                onClick  = { isSessionAudioMuted = !isSessionAudioMuted },
                modifier = Modifier.size(48.dp),
                colors   = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (isSessionAudioMuted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                    contentColor   = if (isSessionAudioMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isSessionAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isSessionAudioMuted) "Unmute meditation music" else "Mute meditation music",
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Breathe with me button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(dhyanGradient)
                .clickable(onClick = onBreatheWithMe)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Air, null, modifier = Modifier.size(18.dp), tint = Color.White)
                Text("Breathe with me", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (selectedTechnique == null && selectedMusicTrack.name != "None" && selectedMusicTrack.id != "none-track") {
            Spacer(Modifier.height(8.dp))
            Card(
                shape     = MaterialTheme.shapes.medium,
                modifier  = Modifier.fillMaxWidth(),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                border    = CardDefaults.outlinedCardBorder(),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(selectedMusicTrack.name, fontSize = 12.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text(
                        if (isSessionAudioMuted) "MUTED" else if (isRunning) "PLAYING" else "READY",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isRunning && !isSessionAudioMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (selectedTechnique != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                shape     = MaterialTheme.shapes.medium,
                modifier  = Modifier.fillMaxWidth(),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                border    = CardDefaults.outlinedCardBorder(),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Air, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selectedBreathingSound.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("Breathing sound", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (isSessionAudioMuted) "MUTED" else if (isRunning && (selectedBreathingSound.url.isNotBlank() || selectedBreathingSound.localResId != null)) "PLAYING" else "SILENT",
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (!isSessionAudioMuted && (selectedBreathingSound.url.isNotBlank() || selectedBreathingSound.localResId != null)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─── Breathe with me sheet — list only ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingOptionsSheet(
    selectedTechnique: BreathingTechnique?,
    onSelectTechnique: (BreathingTechnique) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painter = androidx.compose.ui.res.painterResource(id = com.safarparmar.app.R.drawable.ic_wind), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                Text("Breathe with me", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text("Choose a technique to start.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.1f))

            techniques.forEach { t ->
                val isSelected = t.name == selectedTechnique?.name
                Card(
                    shape     = MaterialTheme.shapes.medium,
                    modifier  = Modifier.fillMaxWidth().bounceClick { onSelectTechnique(t) },
                    colors    = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.1f) else MaterialTheme.colorScheme.background,
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(painter = androidx.compose.ui.res.painterResource(id = t.iconRes), contentDescription = null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(t.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primary.copy(0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(t.pattern, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                Text("Breathing sounds", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Text(
                "Breathing techniques use their own sound set. Add the final audio files here when they are ready.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            breathingSounds.forEach { sound ->
                val isSelected = sound.id == selectedSound.id
                Card(
                    shape     = MaterialTheme.shapes.medium,
                    modifier  = Modifier.fillMaxWidth().bounceClick { onSelectSound(sound) },
                    colors    = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.1f) else MaterialTheme.colorScheme.background,
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Column(Modifier.weight(1f)) {
                            Text(sound.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(sound.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                        if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// MusicSheet removed in favor of shared AudioLibraryPanel

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun DhyanInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
