
package com.safar.app.ui.dhyan

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.nishtha.checkin.SlimSlider
import com.safar.app.ui.theme.*
import com.safar.app.ui.tour.TourManager
import com.safar.app.ui.tour.dhyanTourSteps
import kotlinx.coroutines.delay

// ─── Data ──────────────────────────────────────────────────────────────────────

private data class BreathingTechnique(
    val name: String,
    val emoji: String,
    val description: String,
    val inhale: Int,
    val hold: Int,
    val exhale: Int,
    val holdAfter: Int = 0,
    val pattern: String,
)

private val techniques = listOf(
    BreathingTechnique("Diaphragmatic", "🌬️", "Belly breathing for full oxygen exchange", 4, 0, 6, 0, "4-6"),
    BreathingTechnique("Box Breathing", "⬜", "Rhythmic 4-4-4-4 for stress reduction", 4, 4, 4, 4, "4-4-4-4"),
    BreathingTechnique("4-7-8 Breathing", "🌙", "Deep relaxation for anxiety and sleep", 4, 7, 8, 0, "4-7-8"),
    BreathingTechnique("6-7-8 Breathing", "☯️", "Slower inhale variation for deeper calm", 6, 7, 8, 0, "6-7-8"),
)

private val intensityLabels = listOf("Light", "Gentle", "Moderate", "Deep", "Intense")

private enum class DhyanBreathPhase(val label: String) {
    INHALE("INHALE"), HOLD("HOLD"), EXHALE("EXHALE"), HOLD_AFTER("REST")
}

private val musicOptions = listOf(
    "None"             to "",
    "Serene Flow"      to "https://del1.vultrobjects.com/qms-images/Safar/music_1.mp3",
    "Nostalgia Breeze" to "https://del1.vultrobjects.com/qms-images/Safar/relaxingtime-sleep-music-vol16-195422.mp3",
    "Amber Pulse"      to "https://del1.vultrobjects.com/qms-images/Safar/WhatsApp_Audio_2026-02-18_at_10.05.04_AM.mpeg",
    "Solitude Deep"    to "https://del1.vultrobjects.com/qms-images/Safar/music_3.mp3",
)

private enum class DhyanTab(val label: String, val icon: ImageVector) {
    BREATHING("Breathing", Icons.Default.Air),
    COURSES("Courses", Icons.Default.MenuBook),
}

private const val MEDITATION_IMAGE_URL =
    "https://raw.githubusercontent.com/OneSafar/Safar_Android/refs/heads/master/assets/image_dhyan_1.png"

// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhyanScreen(
    currentRoute: String = Routes.DHYAN,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
) {
    var selectedTab         by remember { mutableStateOf(DhyanTab.BREATHING) }
    var showMusicSheet      by remember { mutableStateOf(false) }
    var showTechniquesSheet by remember { mutableStateOf(false) }
    var selectedMusic       by remember { mutableStateOf(musicOptions[0]) }
    // null = no technique chosen (show image), non-null = show animation
    var selectedTechnique   by remember { mutableStateOf<BreathingTechnique?>(null) }
    var tourState           by remember { mutableStateOf<com.safar.app.ui.butterfly.ButterflyTourState?>(null) }

    val themeVm: ThemeViewModel = hiltViewModel()

    if (showTechniquesSheet) {
        BreathingOptionsSheet(
            selectedTechnique = selectedTechnique,
            onSelectTechnique = { selectedTechnique = it; showTechniquesSheet = false },
            onDismiss         = { showTechniquesSheet = false },
        )
    }

    if (showMusicSheet) {
        MusicSheet(
            selected  = selectedMusic,
            onSelect  = { selectedMusic = it; showMusicSheet = false },
            onDismiss = { showMusicSheet = false },
        )
    }

    Box(Modifier.fillMaxSize()) {
    SafarDrawerScaffold(
        title    = "Dhyan",
        subtitle = "Safar",
        currentRoute      = currentRoute,
        isDarkTheme       = isDarkTheme,
        onNavigate        = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick   = onLanguageClick,
        topBarActions = {
            IconButton(onClick = { tourState?.start() }) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Guide")
            }
            IconButton(onClick = { showMusicSheet = true }) {
                Icon(Icons.Default.MusicNote, contentDescription = "Music")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                        DhyanTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick  = { selectedTab = tab },
                                icon  = { Icon(tab.icon, contentDescription = tab.label) },
                                label = {
                                    Text(
                                        tab.label,
                                        fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize   = 11.sp,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                                    indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    Modifier.fillMaxSize().padding(
                        top    = padding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                    )
                ) {
                    when (selectedTab) {
                        DhyanTab.BREATHING -> BreathingTab(
                            selectedTechnique = selectedTechnique,
                            selectedMusic     = selectedMusic,
                            onBreatheWithMe   = { showTechniquesSheet = true },
                            onShowMusic       = { showMusicSheet = true },
                            onClearTechnique  = { selectedTechnique = null },
                        )
                        DhyanTab.COURSES   -> CoursesTab()
                    }
                }
            }
        }
    }

    // Tour overlay rendered OUTSIDE the scaffold so it appears above the TopAppBar
    TourManager(
        dataStore        = themeVm.dataStore,
        steps            = dhyanTourSteps,
        askOnFirstVisit  = false,
        onTourStateReady = { tourState = it },
    )
    } // end outer Box
}

// ─── Breathing Tab ─────────────────────────────────────────────────────────────

@Composable
private fun BreathingTab(
    selectedTechnique: BreathingTechnique?,
    selectedMusic: Pair<String, String>,
    onBreatheWithMe: () -> Unit,
    onShowMusic: () -> Unit,
    onClearTechnique: () -> Unit,
) {
    var sessionLengthMin    by remember { mutableStateOf(5) }
    var isRunning           by remember { mutableStateOf(false) }
    var phase               by remember { mutableStateOf(DhyanBreathPhase.INHALE) }
    var phaseSecondsLeft    by remember { mutableStateOf(selectedTechnique?.inhale ?: 4) }
    var sessionSecondsLeft  by remember { mutableStateOf(sessionLengthMin * 60) }

    val context = LocalContext.current
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    fun releasePlayer() {
        runCatching { mediaPlayer.value?.stop() }
        runCatching { mediaPlayer.value?.release() }
        mediaPlayer.value = null
    }

    LaunchedEffect(isRunning, selectedMusic) {
        if (isRunning && selectedMusic.second.isNotBlank()) {
            releasePlayer()
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(selectedMusic.second))
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
        if (sessionSecondsLeft <= 0) isRunning = false
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
        0 -> "1"; 1 -> "3"; 2 -> "4"; 3 -> "4"; else -> "1"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "\"Silence is the language of God.\"",
            fontSize  = 13.sp,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )

        // ── Visual area: image OR breathing animation ──────────────────────
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
                        .size(220.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model              = MEDITATION_IMAGE_URL,
                        contentDescription = "Meditate",
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                    )
                }
            } else {
                // Live breathing animation + technique chip
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BreathingVisualizer(
                        sessionId   = vizSessionId,
                        breathPhase = vizPhase,
                        isActive    = isRunning,
                        cycle       = vizCycle,
                        modifier    = Modifier.size(220.dp),
                    )
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(0.12f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "${technique.emoji} ${technique.name} · ${technique.pattern}",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary,
                            )
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

        // Large timer
        Text(
            "%02d:%02d".format(sessionSecondsLeft / 60, sessionSecondsLeft % 60),
            fontSize      = 52.sp,
            fontWeight    = FontWeight.Bold,
            letterSpacing = (-2).sp,
        )

        // Session length card
        Card(
            shape     = RoundedCornerShape(16.dp),
            modifier  = Modifier.fillMaxWidth(),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp),
            border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        .padding(vertical = 4.dp),
                    activeColor   = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Play / Pause / Reset row
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick  = { resetTimer() },
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            ) {
                Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) Amber500 else MaterialTheme.colorScheme.primary)
                    .clickable { isRunning = !isRunning },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint     = if (isRunning) Color.Black else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(34.dp),
                )
            }
            IconButton(
                onClick  = onShowMusic,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    null,
                    tint = if (selectedMusic.first != "None") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Breathe with me button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(BreathGradientStart, BreathGradientEnd)))
                .clickable(onClick = onBreatheWithMe)
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Air, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Text("Breathe with me", fontWeight = FontWeight.SemiBold, color = Color.Black)
            }
        }

        if (selectedMusic.first != "None") {
            Card(
                shape     = RoundedCornerShape(12.dp),
                modifier  = Modifier.fillMaxWidth(),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(selectedMusic.first, fontSize = 13.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text(
                        if (isRunning) "PLAYING" else "READY",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
            Text("Breathe with me 🌬️", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Choose a technique to start.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.1f))

            techniques.forEach { t ->
                val isSelected = t.name == selectedTechnique?.name
                Card(
                    shape     = RoundedCornerShape(14.dp),
                    modifier  = Modifier.fillMaxWidth().clickable { onSelectTechnique(t) },
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
                        Text(t.emoji, fontSize = 26.sp)
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(t.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
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

// ─── Music Sheet ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicSheet(
    selected: Pair<String, String>,
    onSelect: (Pair<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Ambient Sound", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            musicOptions.forEach { option ->
                val isSel = selected.first == option.first
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) MaterialTheme.colorScheme.primary.copy(0.1f) else Color.Transparent)
                        .clickable { onSelect(option) }
                        .padding(14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(option.first, fontSize = 14.sp, modifier = Modifier.weight(1f), color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                    if (isSel) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─── Courses Tab ──────────────────────────────────────────────────────────────

@Composable
private fun CoursesTab() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Dhyan Learning Tracks", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Deepen your meditation journey with guided courses, daily structure, and progress checkpoints.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)), elevation = CardDefaults.cardElevation(0.dp)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🌱", fontSize = 28.sp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("It will be available soon", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("We're crafting thoughtful courses for you. Stay tuned.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                }
            }
        }
        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(0.1f)), contentAlignment = Alignment.Center) { Text("🧘", fontSize = 24.sp) }
                    Text("SAFAR 30-Day Meditation Course", fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
                }
                Text("A 30-day guided meditation journey to build a consistent practice, reduce stress, and deepen self-awareness.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) { Text("Status", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold) }
                        Text("Coming soon", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("It will be available soon", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun DhyanInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}