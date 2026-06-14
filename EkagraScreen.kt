package com.safarparmar.app.ui.ekagra

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.view.TextureView
import android.graphics.SurfaceTexture
import androidx.annotation.DrawableRes
import androidx.compose.ui.draw.alpha
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTimerService = staticCompositionLocalOf<TimerService?> { null }

// ─── Video background (unchanged) ──────────────────────────────────────────────

@Composable
private fun EkagraVideoBackground(videoUrl: String, modifier: Modifier = Modifier) {
    if (videoUrl.isBlank()) { Box(modifier.background(Color.Black)); return }
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val screenW = ctx.resources.displayMetrics.widthPixels
            val screenH = ctx.resources.displayMetrics.heightPixels
            val textureView = android.view.TextureView(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(screenW, screenH)
            }
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                    try {
                        val mp = MediaPlayer().apply {
                            setSurface(android.view.Surface(p0))
                            setDataSource(ctx, Uri.parse(videoUrl))
                            isLooping = true; setVolume(0f, 0f)
                            setOnPreparedListener { player ->
                                val vW = screenW.toFloat(); val vH = screenH.toFloat()
                                val vidW = player.videoWidth.takeIf { it > 0 }?.toFloat() ?: vW
                                val vidH = player.videoHeight.takeIf { it > 0 }?.toFloat() ?: vH
                                val videoAspect = vidW / vidH; val viewAspect = vW / vH
                                val (scaleX, scaleY) = if (videoAspect > viewAspect) Pair(videoAspect / viewAspect, 1f) else Pair(1f, viewAspect / videoAspect)
                                textureView.scaleX = scaleX; textureView.scaleY = scaleY
                                textureView.pivotX = vW / 2f; textureView.pivotY = vH / 2f
                                player.start()
                            }
                            setOnErrorListener { _, what, extra -> Log.e("VIDEO_DEBUG", "Error: $what $extra"); true }
                            prepareAsync()
                        }
                        textureView.setTag(R.id.ekagra_player_tag, mp)
                    } catch (e: Exception) { Log.e("VIDEO_DEBUG", "Video init failed", e) }
                }
                override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) = Unit
                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    val mp = textureView.getTag(R.id.ekagra_player_tag) as? MediaPlayer
                    textureView.setTag(R.id.ekagra_player_tag, null)
                    try { mp?.stop() } catch (_: Exception) {}
                    try { mp?.release() } catch (_: Exception) {}
                    return true
                }
                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) = Unit
            }
            android.widget.FrameLayout(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(screenW, screenH)
                setTag(R.id.ekagra_video_url_tag, videoUrl)
                addView(textureView)
            }
        },
        update = { frame ->
            val currentUrl = frame.getTag(R.id.ekagra_video_url_tag) as? String
            if (currentUrl == videoUrl) return@AndroidView
            frame.setTag(R.id.ekagra_video_url_tag, videoUrl)
            val tv = frame.getChildAt(0) as? android.view.TextureView ?: return@AndroidView
            val st = tv.surfaceTexture ?: return@AndroidView
            val oldPlayer = tv.getTag(R.id.ekagra_player_tag) as? MediaPlayer
            tv.setTag(R.id.ekagra_player_tag, null)
            val ctx = frame.context
            val screenW = ctx.resources.displayMetrics.widthPixels
            val screenH = ctx.resources.displayMetrics.heightPixels
            Thread {
                runCatching { oldPlayer?.stop() }; runCatching { oldPlayer?.release() }
                try {
                    val newPlayer = MediaPlayer().apply {
                        setSurface(android.view.Surface(st))
                        setDataSource(ctx, Uri.parse(videoUrl))
                        isLooping = true; setVolume(0f, 0f)
                        setOnPreparedListener { mp ->
                            Handler(Looper.getMainLooper()).post {
                                val vW = screenW.toFloat(); val vH = screenH.toFloat()
                                val vidW = mp.videoWidth.takeIf { it > 0 }?.toFloat() ?: vW
                                val vidH = mp.videoHeight.takeIf { it > 0 }?.toFloat() ?: vH
                                val videoAspect = vidW / vidH; val viewAspect = vW / vH
                                val (scaleX, scaleY) = if (videoAspect > viewAspect) Pair(videoAspect / viewAspect, 1f) else Pair(1f, viewAspect / videoAspect)
                                tv.scaleX = scaleX; tv.scaleY = scaleY
                                tv.pivotX = vW / 2f; tv.pivotY = vH / 2f
                                mp.start()
                            }
                        }
                        setOnErrorListener { _, _, _ -> true }
                        prepareAsync()
                    }
                    tv.setTag(R.id.ekagra_player_tag, newPlayer)
                } catch (_: Exception) {}
            }.start()
        },
        modifier = modifier,
    )
}

// ─── Theme / music data (unchanged) ────────────────────────────────────────────

data class VisualTheme(
    val name: String,
    val emoji: String,
    val bg: Color,
    val accent: Color,
    val videoUrl: String = "",
    val musicUrl: String = "",
)

val visualThemes = listOf(
    VisualTheme("Serene",    "🌊", Color(0xFF0a4d68), Color(0xFF1b8ec3),
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_2.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/music_1.mp3"),
    VisualTheme("Nostalgia", "🌿", Color(0xFFf97316), Color(0xFF1cbc31),
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_3.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/relaxingtime-sleep-music-vol16-195422.mp3"),
    VisualTheme("Amber",     "🍂", Color(0xFF1e3a5f), Color(0xFF2e7144),
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_4.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/WhatsApp_Audio_2026-02-18_at_10.05.04_AM.mpeg"),
    VisualTheme("Solitude",  "🌙", Color(0xFF1c527c), Color(0xFF7c3aed),
        videoUrl = "https://del1.vultrobjects.com/qms-images/Safar/theme_1.mp4",
        musicUrl = "https://del1.vultrobjects.com/qms-images/Safar/music_3.mp3"),
)

private val focusMusicTracks = listOf(
    "Theme Default"    to "",
    "Serene Flow"      to "https://del1.vultrobjects.com/qms-images/Safar/music_1.mp3",
    "Nostalgia Breeze" to "https://del1.vultrobjects.com/qms-images/Safar/relaxingtime-sleep-music-vol16-195422.mp3",
    "Amber Pulse"      to "https://del1.vultrobjects.com/qms-images/Safar/WhatsApp_Audio_2026-02-18_at_10.05.04_AM.mpeg",
    "Solitude Deep"    to "https://del1.vultrobjects.com/qms-images/Safar/music_3.mp3",
    "Silence"          to "silence",
)

// ─── Nav tab & Timer mode enums ────────────────────────────────────────────────

private enum class EkagraNavTab(val icon: ImageVector, val label: String) {
    TIMER    (Icons.Default.Timer,   "Focus"),
    DURATION (Icons.Default.Tune,    "Duration"),
    HISTORY  (Icons.Default.History, "History"),
}

enum class TimerMode(
    @DrawableRes val lightIconRes: Int,
    @DrawableRes val darkIconRes: Int,
    val label: String,
    val showInPill: Boolean = true,
) {
    FOCUS(R.drawable.ic_ekagra_timer_light, R.drawable.ic_ekagra_timer_dark, "Focus"),
    BREAK(R.drawable.ic_ekagra_coffee_light, R.drawable.ic_ekagra_coffee_dark, "Break"),
    LONG_BREAK(R.drawable.ic_ekagra_bed_light, R.drawable.ic_ekagra_bed_dark, "Long Break"),
}

// ─── Root screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkagraScreen(
    currentRoute: String = Routes.EKAGRA,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    linkedGoalId: String? = null,
    linkedGoalTitle: String? = null,
    initialView: String? = null,
    viewModel: EkagraViewModel = hiltViewModel(),
    focusShieldViewModel: com.safarparmar.app.ui.ekagra.focusshield.FocusShieldViewModel = hiltViewModel(),
) {
    val activeSession        by viewModel.activeSession.collectAsStateWithLifecycle()
    val ekagraAnalytics      by viewModel.ekagraAnalytics.collectAsStateWithLifecycle()
    val allGoals             by viewModel.allGoals.collectAsStateWithLifecycle()
    val timerService         = LocalTimerService.current
    val context              = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    val shieldState          by focusShieldViewModel.shieldState.collectAsStateWithLifecycle()

    // fallback flows when timerService is null
    val fallbackSecondsLeft      = remember { MutableStateFlow(25 * 60) }
    val fallbackTotalSeconds     = remember { MutableStateFlow(25 * 60) }
    val fallbackTimerRunning     = remember { MutableStateFlow(false) }
    val fallbackTimerMode        = remember { MutableStateFlow(TimerMode.FOCUS) }
    val fallbackFocusShieldActive = remember { MutableStateFlow(false) }

    val secondsLeft       by (timerService?.secondsLeft        ?: fallbackSecondsLeft).collectAsStateWithLifecycle()
    val totalSeconds      by (timerService?.totalSeconds       ?: fallbackTotalSeconds).collectAsStateWithLifecycle()
    val timerRunning      by (timerService?.isRunning          ?: fallbackTimerRunning).collectAsStateWithLifecycle()
    val timerMode         by (timerService?.timerMode          ?: fallbackTimerMode).collectAsStateWithLifecycle()
    val focusShieldActive by (timerService?.focusShieldActive  ?: fallbackFocusShieldActive).collectAsStateWithLifecycle()
    val blockedHitCount   by focusShieldViewModel.blockedHitCount.collectAsStateWithLifecycle()

    // UI state
    var selectedTab              by remember { mutableStateOf(EkagraNavTab.TIMER) }
    var showKavachActiveSession  by remember { mutableStateOf(false) }
    var showKavachSessionSummary by remember { mutableStateOf(false) }
    var kavachSummaryMinutes     by remember { mutableIntStateOf(0) }
    var kavachSummaryAttempts    by remember { mutableStateOf<List<com.safarparmar.app.ui.ekagra.focusshield.KavachBlockedAttempt>>(emptyList()) }
    var showThemeDialog          by remember { mutableStateOf(false) }
    var showSongSheet            by remember { mutableStateOf(false) }
    var showEkagraGuide          by remember { mutableStateOf(false) }
    var showOrganizeSheet        by remember { mutableStateOf(false) }
    var pendingEndedSession      by remember { mutableStateOf<PendingEndedEkagraSession?>(null) }
    var titleInput               by remember { mutableStateOf("") }
    var tourState                by remember { mutableStateOf<com.safarparmar.app.ui.butterfly.ButterflyTourState?>(null) }

    var selectedTheme by remember {
        val prefs = context.getSharedPreferences("ekagra_theme_prefs", android.content.Context.MODE_PRIVATE)
        mutableStateOf(visualThemes.getOrElse(prefs.getInt("theme_index", -1)) { visualThemes[0] })
    }
    var selectedSong by remember {
        val prefs = context.getSharedPreferences("ekagra_theme_prefs", android.content.Context.MODE_PRIVATE)
        mutableStateOf(prefs.getString("song_name", null) ?: "Theme Default")
    }

    var associatedGoalId    by remember(linkedGoalId)    { mutableStateOf(linkedGoalId) }
    var associatedGoalTitle by remember(linkedGoalTitle) { mutableStateOf(linkedGoalTitle) }
    var taskText            by remember(linkedGoalId, linkedGoalTitle) { mutableStateOf(linkedGoalTitle.orEmpty()) }
    var focusMinutes        by remember { mutableIntStateOf(25) }
    var breakMinutes        by remember { mutableIntStateOf(5) }
    var longBreakMinutes    by remember { mutableIntStateOf(15) }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    fun startTimer(mode: TimerMode, minutes: Int) {
        if (mode == TimerMode.FOCUS && shieldState.isEnabled && shieldState.blockedPackages.isNotEmpty()
            && (!shieldState.hasUsageStats || !shieldState.hasAccessibilityService)) {
            onNavigate(Routes.FOCUS_SHIELD); return
        }
        requestNotificationPermission()
        timerService?.saveTheme(visualThemes.indexOf(selectedTheme), selectedSong)
        timerService?.setDuration(mode, minutes * 60)
        timerService?.start()
        viewModel.onSessionStarted(
            taskText     = taskText,
            totalSeconds = minutes * 60,
            goalId       = if (mode == TimerMode.FOCUS) associatedGoalId else null,
            goalTitle    = if (mode == TimerMode.FOCUS) associatedGoalTitle else null,
            mode         = mode.toApiMode(),
        )
        if (mode == TimerMode.FOCUS && shieldState.isEnabled && shieldState.blockedPackages.isNotEmpty()) {
            timerService?.setFocusShieldConfig(shieldState.blockedPackages, shieldState.isStrictMode)
            timerService?.enableFocusShieldForSession()
        }
    }

    fun resetTimer() {
        activeSession?.id?.let { viewModel.discardSession(it) }
        timerService?.reset()
        associatedGoalId = null; associatedGoalTitle = null
    }

    fun captureKavachSessionSummary() {
        if (blockedHitCount <= 0) return
        kavachSummaryMinutes  = ((totalSeconds - secondsLeft).coerceAtLeast(0) / 60).coerceAtLeast(1)
        kavachSummaryAttempts = focusShieldViewModel.snapshotBlockedAttempts()
        showKavachSessionSummary = true
    }

    fun endCurrentSession() {
        captureKavachSessionSummary()
        val session = activeSession
        if (session != null && timerMode == TimerMode.FOCUS) {
            timerService?.pause()
            pendingEndedSession = PendingEndedEkagraSession(
                sessionId    = session.id,
                totalSeconds = totalSeconds,
                secondsLeft  = secondsLeft,
                mode         = timerMode.toApiMode(),
                startedAt    = session.sessionStartedAt,
            )
            titleInput = session.sessionTitle ?: taskText
            showOrganizeSheet = true
            return
        }
        viewModel.onSessionCompleted(totalSeconds, secondsLeft, timerMode.toApiMode())
        timerService?.reset()
        associatedGoalId = null; associatedGoalTitle = null
    }

    fun resolveAudioUrl(): String = when {
        selectedSong == "Theme Default" -> selectedTheme.musicUrl
        selectedSong == "Silence"       -> "silence"
        else -> focusMusicTracks.firstOrNull { it.first == selectedSong }?.second ?: ""
    }

    // ── Side-effects ────────────────────────────────────────────────────────────

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedTab) {
        if (selectedTab == EkagraNavTab.TIMER && timerService?.isActive() == false)
            timerService.setDuration(TimerMode.FOCUS, focusMinutes * 60)
    }
    LaunchedEffect(initialView) {
        if (initialView == "analytics") onNavigate(Routes.nishthaAnalytics("focus"))
    }

    val ekagraLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(ekagraLifecycleOwner) {
        ekagraLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshEkagra(); viewModel.loadTasks()
            while (true) { delay(20_000L); viewModel.loadEkagraAnalytics() }
        }
    }

    val latestTotalSeconds by rememberUpdatedState(totalSeconds)
    val latestSecondsLeft  by rememberUpdatedState(secondsLeft)
    val latestTimerMode    by rememberUpdatedState(timerMode)
    val latestGoalTitle    by rememberUpdatedState(associatedGoalTitle)
    val latestTaskText     by rememberUpdatedState(taskText)
    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(15_000L)
            if (latestSecondsLeft > 0 && latestTimerMode == TimerMode.FOCUS)
                viewModel.syncActiveSession(latestTotalSeconds, latestSecondsLeft, latestTimerMode.toApiMode(),
                    true, latestGoalTitle ?: latestTaskText.takeIf { it.isNotBlank() })
        }
    }

    LaunchedEffect(selectedSong, selectedTheme) {
        timerService?.setMusic(resolveAudioUrl())
        if (timerService?.isActive() == true)
            timerService.saveTheme(visualThemes.indexOf(selectedTheme), selectedSong)
    }

    // PiP
    val pipContext   = LocalContext.current
    val pipActivity  = pipContext as? Activity
    val mainActivity = pipActivity as? MainActivity
    val isInPipMode  = mainActivity?.isInPipMode == true
    val PIP_REQUEST_PLAY = 1

    fun buildPipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(1, 1))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true); builder.setAutoEnterEnabled(timerRunning)
        }
        val playPauseIcon = android.graphics.drawable.Icon.createWithResource(
            pipContext, if (timerRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
        builder.setActions(listOf(RemoteAction(playPauseIcon,
            if (timerRunning) "Pause" else "Play",
            if (timerRunning) "Pause timer" else "Start timer",
            android.app.PendingIntent.getService(pipContext, PIP_REQUEST_PLAY,
                Intent(pipContext, TimerService::class.java).apply { action = TimerService.ACTION_PLAY_PAUSE },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setTitle("SAFAR Focus Timer"); builder.setSubtitle("Focus timer running")
        }
        return builder.build()
    }

    LaunchedEffect(timerRunning, secondsLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { buildPipParams()?.let { pipActivity?.setPictureInPictureParams(it) } } catch (_: Exception) {}
        }
        if (!timerRunning && secondsLeft == 0 && totalSeconds > 0) {
            if (timerMode != TimerMode.FOCUS) return@LaunchedEffect
            if (blockedHitCount > 0) {
                kavachSummaryMinutes  = (totalSeconds / 60).coerceAtLeast(1)
                kavachSummaryAttempts = focusShieldViewModel.snapshotBlockedAttempts()
                showKavachSessionSummary = true
            }
            val session = activeSession
            if (session != null) {
                pendingEndedSession = PendingEndedEkagraSession(session.id, totalSeconds, 0, timerMode.toApiMode(), session.sessionStartedAt)
                titleInput = session.sessionTitle ?: ""; showOrganizeSheet = true
                return@LaunchedEffect
            }
            viewModel.onSessionCompleted(totalSeconds, 0, timerMode.toApiMode())
            associatedGoalId = null; associatedGoalTitle = null
        }
    }

    // ── Derived values ───────────────────────────────────────────────────────────

    val progress = if (totalSeconds > 0) 1f - secondsLeft.toFloat() / totalSeconds else 0f
    val mottoText = when {
        timerMode != TimerMode.FOCUS && timerRunning -> "BREAK TIME — KAVACH PAUSED"
        timerRunning -> "STAY FOCUSED, YOU'RE DOING GREAT!"
        else         -> "READY TO FOCUS?"
    }
    val openGoals = remember(allGoals) {
        allGoals.filter { it.id.isNotBlank() && it.title.isNotBlank() && !it.completed
            && it.lifecycleStatus !in listOf("abandoned", "rolled_over") }
    }

    // ── M3 dynamic color scheme (theme-aware) ────────────────────────────────────
    // We derive a proper M3 scheme from each visual theme's primary accent colour.
    // This replaces the old hand-rolled darkColorScheme with hardcoded raw hex values.
    val themeColorScheme = remember(selectedTheme, isDarkTheme) {
        val seed = selectedTheme.accent
        if (isDarkTheme) {
            darkColorScheme(
                primary                = seed,
                onPrimary              = Color.White,
                primaryContainer       = seed.copy(alpha = 0.22f),
                onPrimaryContainer     = seed.copy(alpha = 0.90f),
                secondary              = seed.copy(alpha = 0.72f),
                onSecondary            = Color.White,
                secondaryContainer     = seed.copy(alpha = 0.18f),
                onSecondaryContainer   = seed.copy(alpha = 0.88f),
                background             = Color(0xFF0F1115),
                onBackground           = Color(0xFFE7EBEF),
                surface                = Color(0xFF111416),
                onSurface              = Color(0xFFE1E2E5),
                surfaceVariant         = Color(0xFF272C35),
                onSurfaceVariant       = Color(0xFFC2C9CF),
                surfaceContainerLowest = Color(0xFF0C0E10),
                surfaceContainerLow    = Color(0xFF191C1F),
                surfaceContainer       = Color(0xFF1D2024),
                surfaceContainerHigh   = Color(0xFF272A2E),
                surfaceContainerHighest= Color(0xFF323538),
                outline                = Color(0xFF8C9399),
                outlineVariant         = Color(0xFF42494F),
                tertiary               = Color(0xFFC8BFFF),
                tertiaryContainer      = Color(0xFF43398A),
                onTertiaryContainer    = Color(0xFFE3DFFF),
            )
        } else {
            lightColorScheme(
                primary                = seed,
                onPrimary              = Color.White,
                primaryContainer       = seed.copy(alpha = 0.14f),
                onPrimaryContainer     = seed.copy(alpha = 0.90f),
                secondary              = seed.copy(alpha = 0.68f),
                onSecondary            = Color.White,
                secondaryContainer     = seed.copy(alpha = 0.12f),
                onSecondaryContainer   = seed.copy(alpha = 0.85f),
                background             = Color(0xFFF8F9FB),
                onBackground           = Color(0xFF191C1F),
                surface                = Color(0xFFF8F9FB),
                onSurface              = Color(0xFF191C1F),
                surfaceVariant         = Color(0xFFE4E8EC),
                onSurfaceVariant       = Color(0xFF42494F),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow    = Color(0xFFF2F4F6),
                surfaceContainer       = Color(0xFFEBEEF1),
                surfaceContainerHigh   = Color(0xFFE4E8EC),
                surfaceContainerHighest= Color(0xFFDEE2E7),
                outline                = Color(0xFF72797F),
                outlineVariant         = Color(0xFFC2C9CF),
                tertiary               = Color(0xFF5C5490),
                tertiaryContainer      = Color(0xFFE3DFFF),
                onTertiaryContainer    = Color(0xFF19115B),
            )
        }
    }

    // Font-scale clamp — preserve user font scale but cap extreme sizes
    val displayMetrics = context.resources.displayMetrics
    val clampedDensity = remember(displayMetrics.density, context.resources.configuration.fontScale) {
        Density(density = displayMetrics.density,
            fontScale = context.resources.configuration.fontScale.coerceIn(0.75f, 1.25f))
    }

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        MaterialTheme(colorScheme = themeColorScheme) {

            // ── Kavach overlay screens ───────────────────────────────────────────
            when {
                showKavachSessionSummary -> {
                    com.safarparmar.app.ui.ekagra.focusshield.KavachSessionSummaryScreen(
                        focusedMinutes  = kavachSummaryMinutes,
                        blockedAttempts = kavachSummaryAttempts,
                        onBack  = { showKavachSessionSummary = false },
                        onDone  = { showKavachSessionSummary = false; focusShieldViewModel.clearSessionStats() },
                    )
                }
                showKavachActiveSession && focusShieldActive && timerRunning && timerMode == TimerMode.FOCUS -> {
                    com.safarparmar.app.ui.ekagra.focusshield.KavachActiveSessionScreen(
                        secondsLeft  = secondsLeft,
                        blockedCount = blockedHitCount,
                        onBack       = { showKavachActiveSession = false },
                        onEndSession = { endCurrentSession() },
                    )
                }
                // ── PiP overlay ─────────────────────────────────────────────────
                isInPipMode -> {
                    EkagraPipOverlay(
                        secondsLeft       = secondsLeft,
                        progress          = progress,
                        timerRunning      = timerRunning,
                        focusShieldActive = focusShieldActive,
                        primary           = themeColorScheme.primary,
                    )
                }
                else -> {
                    // ── Dialogs / sheets ─────────────────────────────────────────
                    if (showThemeDialog)
                        VisualThemeDialog(current = selectedTheme, onSelect = { selectedTheme = it; showThemeDialog = false }, onDismiss = { showThemeDialog = false })
                    if (showSongSheet)
                        SongPickerSheet(current = selectedSong, onSelect = { selectedSong = it; showSongSheet = false }, onDismiss = { showSongSheet = false })
                    if (showEkagraGuide)
                        EkagraGuideDialog(onDismiss = { showEkagraGuide = false })
                    if (showOrganizeSheet) {
                        val pending = pendingEndedSession
                        OrganizeFreeFocusSheet(
                            pending       = pending,
                            goals         = openGoals,
                            titleInput    = titleInput,
                            onTitleChange = { titleInput = it },
                            onDismiss     = { showOrganizeSheet = false },
                            onSaveFree    = {
                                if (pending != null) {
                                    viewModel.completeSession(pending.sessionId, pending.totalSeconds,
                                        pending.secondsLeft, pending.mode, pending.startedAt,
                                        titleInput.ifBlank { "Free Focus" }, null, null)
                                    timerService?.reset(); associatedGoalId = null
                                    associatedGoalTitle = null; pendingEndedSession = null; showOrganizeSheet = false
                                }
                            },
                            onLinkGoal = { goal ->
                                if (pending != null) {
                                    viewModel.linkGoalAndCompleteSession(pending.sessionId, goal,
                                        pending.totalSeconds, pending.secondsLeft, pending.mode, pending.startedAt)
                                    timerService?.reset(); associatedGoalId = null
                                    associatedGoalTitle = null; pendingEndedSession = null; showOrganizeSheet = false
                                }
                            },
                            onDiscard = {
                                if (pending != null) {
                                    viewModel.discardSession(pending.sessionId)
                                    timerService?.reset(); associatedGoalId = null
                                    associatedGoalTitle = null; pendingEndedSession = null; showOrganizeSheet = false
                                }
                            },
                        )
                    }

                    // ── Main scaffold ────────────────────────────────────────────
                    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
                        SafarDrawerScaffold(
                            title              = stringResource(R.string.module_ekagra),
                            subtitle           = stringResource(R.string.app_name),
                            currentRoute       = currentRoute,
                            isDarkTheme        = isDarkTheme,
                            onNavigate         = onNavigate,
                            onToggleDarkTheme  = onToggleNightMode,
                            onLanguageClick    = onLanguageClick,
                            // Top-bar text is always white when timer tab is showing (video bg)
                            topBarContentColor = if (selectedTab == EkagraNavTab.TIMER) Color.White
                                                 else MaterialTheme.colorScheme.onSurface,
                            topBarActions = {
                                IconButton(onClick = { showEkagraGuide = true }) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = "Guide")
                                }
                                IconButton(onClick = { showThemeDialog = true }) {
                                    Icon(Icons.Default.Palette, contentDescription = "Theme")
                                }
                                IconButton(onClick = { showSongSheet = true }) {
                                    Icon(Icons.Default.MusicNote, contentDescription = "Sound")
                                }
                            },
                        ) { padding ->

                            // Video + scrim only on timer tab
                            if (selectedTab == EkagraNavTab.TIMER) {
                                EkagraVideoBackground(videoUrl = selectedTheme.videoUrl, modifier = Modifier.fillMaxSize())
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.38f)))
                            }

                            Scaffold(
                                containerColor      = Color.Transparent,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                snackbarHost        = { SnackbarHost(snackbarHostState) },
                                bottomBar = {
                                    EkagraBottomNav(
                                        selectedTab = selectedTab,
                                        onSelect    = { selectedTab = it },
                                        // On timer tab the nav sits over the video scrim — use contrasting colours
                                        isOnVideo   = selectedTab == EkagraNavTab.TIMER,
                                    )
                                },
                            ) { innerPadding ->
                                when (selectedTab) {

                                    EkagraNavTab.TIMER -> TimerFocusTab(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = padding.calculateTopPadding(),
                                                     bottom = innerPadding.calculateBottomPadding()),
                                        timerMode          = timerMode,
                                        secondsLeft        = secondsLeft,
                                        isRunning          = timerRunning,
                                        progress           = progress,
                                        mottoText          = mottoText,
                                        kavachActive       = focusShieldActive && timerRunning && timerMode == TimerMode.FOCUS,
                                        kavachBlockedCount = blockedHitCount,
                                        onOpenKavachSession = { showKavachActiveSession = true },
                                        onModeChange = { mode ->
                                            val mins = when (mode) {
                                                TimerMode.FOCUS      -> focusMinutes
                                                TimerMode.BREAK      -> breakMinutes
                                                TimerMode.LONG_BREAK -> longBreakMinutes
                                            }
                                            if (mode != TimerMode.FOCUS && timerMode == TimerMode.FOCUS && timerService?.isActive() == true)
                                                timerService.startBreak(mode, mins * 60)
                                            else
                                                timerService?.setDuration(mode, mins * 60)
                                        },
                                        onPlayPause = {
                                            val wasRunning  = timerRunning
                                            val wasInactive = timerService?.isActive() == false
                                            if (wasInactive) requestNotificationPermission()
                                            timerService?.togglePlayPause()
                                            when {
                                                wasInactive && timerMode == TimerMode.FOCUS ->
                                                    viewModel.onSessionStarted(taskText, totalSeconds,
                                                        associatedGoalId, associatedGoalTitle, timerMode.toApiMode())
                                                wasRunning && timerMode == TimerMode.FOCUS ->
                                                    viewModel.pauseActiveSession(totalSeconds, secondsLeft, timerMode.toApiMode(), associatedGoalTitle)
                                                timerMode == TimerMode.FOCUS ->
                                                    viewModel.syncActiveSession(totalSeconds, secondsLeft, timerMode.toApiMode(),
                                                        true, associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() })
                                            }
                                        },
                                        canStartBreak = timerMode == TimerMode.FOCUS && timerService?.isActive() == true,
                                        onStartBreak  = { timerService?.startBreak(TimerMode.BREAK, breakMinutes * 60) },
                                        onReset       = { endCurrentSession() },
                                    )

                                    EkagraNavTab.DURATION -> DurationTab(
                                        modifier      = Modifier.padding(top = padding.calculateTopPadding(),
                                                                          bottom = innerPadding.calculateBottomPadding()),
                                        focusMinutes  = focusMinutes,
                                        breakMinutes  = breakMinutes,
                                        onFocusChange = { focusMinutes = it },
                                        onBreakChange = { breakMinutes = it; longBreakMinutes = it },
                                        onStartFocusSession = { startTimer(TimerMode.FOCUS, focusMinutes) },
                                    )

                                    EkagraNavTab.HISTORY -> FocusHistoryTab(
                                        modifier  = Modifier.padding(top = padding.calculateTopPadding(),
                                                                      bottom = innerPadding.calculateBottomPadding()),
                                        analytics = ekagraAnalytics,
                                    )
                                }
                            }
                        }
                    }

                    com.safarparmar.app.ui.tour.TourManager(
                        dataStore       = viewModel.dataStore,
                        steps           = com.safarparmar.app.ui.tour.ekagraTourSteps,
                        askOnFirstVisit = false,
                        onTourStateReady = { tourState = it },
                    )
                }
            }
        }
    }
}

// ─── PiP overlay ───────────────────────────────────────────────────────────────

@Composable
private fun EkagraPipOverlay(
    secondsLeft: Int,
    progress: Float,
    timerRunning: Boolean,
    focusShieldActive: Boolean,
    primary: Color,
) {
    val shieldActive = focusShieldActive && timerRunning
    val pipBg     = if (shieldActive) com.safarparmar.app.ui.ekagra.focusshield.KavachDesign.Primary else Color(0xFF05070A)
    val pipAccent = if (shieldActive) Color.White else primary

    Box(
        Modifier.fillMaxSize().background(pipBg).padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (shieldActive) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape)
                        .background(pipAccent.copy(alpha = 0.18f))
                        .border(1.dp, pipAccent.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Shield, contentDescription = null, tint = pipAccent, modifier = Modifier.size(21.dp)) }
            }
            Text("%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                fontSize = if (shieldActive) 36.sp else 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Box(Modifier.fillMaxWidth(0.82f).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(0.16f))) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp)).background(pipAccent))
            }
            Text(
                when { shieldActive -> "SHIELD ACTIVE"; timerRunning -> "FOCUSING"; else -> "PAUSED" },
                fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp,
                color = if (shieldActive) pipAccent else Color.White.copy(0.65f),
            )
        }
    }
}

// ─── Mode pill (icon-only) ─────────────────────────────────────────────────────

@Composable
private fun ModePill(selected: TimerMode, onSelect: (TimerMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            // M3 token: surfaceContainerHigh
            .background(scheme.surfaceContainerHigh)
            .border(0.5.dp, scheme.outlineVariant, RoundedCornerShape(50.dp))
            .padding(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimerMode.entries.filter { it.showInPill }.forEach { mode ->
                val isSelected = mode == selected
                // Icon resource: use light (white) icon when selected on primary bg,
                // dark icon when unselected on surfaceContainerHigh bg.
                val iconRes = if (isSelected) mode.lightIconRes else mode.darkIconRes

                Box(
                    modifier = Modifier
                        // Fixed 48dp square chip — no text label
                        .size(48.dp)
                        .clip(RoundedCornerShape(50.dp))
                        // M3 token: primary when selected, transparent when not
                        .background(if (isSelected) scheme.primary else Color.Transparent)
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter            = painterResource(iconRes),
                        contentDescription = mode.label, // keep for a11y — screen reader reads this
                        // M3 token: onPrimary when selected, onSurfaceVariant when not
                        tint               = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                        modifier           = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

// ─── Timer / Focus tab ─────────────────────────────────────────────────────────

@Composable
private fun TimerFocusTab(
    modifier: Modifier,
    timerMode: TimerMode,
    secondsLeft: Int,
    isRunning: Boolean,
    progress: Float,
    mottoText: String,
    kavachActive: Boolean = false,
    kavachBlockedCount: Int = 0,
    onOpenKavachSession: () -> Unit = {},
    onModeChange: (TimerMode) -> Unit,
    onPlayPause: () -> Unit,
    canStartBreak: Boolean,
    onStartBreak: () -> Unit,
    onReset: () -> Unit,
) {
    val scheme  = MaterialTheme.colorScheme
    // On the timer tab the background is always the video scrim (dark).
    // Use onSurface tokens from our M3 scheme, which the dark themeColorScheme
    // already resolves to light colours.
    val configuration   = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 600

    // Pulse animation for the ring inner glow
    val pulse by animateFloatAsState(
        targetValue    = if (isRunning) 1f else 0f,
        animationSpec  = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label          = "timerPulse",
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .then(if (isCompactHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(if (isCompactHeight) 16.dp else 56.dp))

            // Icon-only mode pill
            ModePill(selected = timerMode, onSelect = onModeChange)

            Spacer(Modifier.height(32.dp))

            // ── Ring — NO card wrapper ───────────────────────────────────────────
            // The ring floats directly on the video background.
            val clampedProgress = progress.coerceIn(0f, 1f)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(232.dp)) {
                // Track ring — M3 secondaryContainer
                CircularProgressIndicator(
                    progress      = { 1f },
                    modifier      = Modifier.fillMaxSize(),
                    color         = scheme.secondaryContainer,
                    strokeWidth   = 14.dp,
                    strokeCap     = StrokeCap.Round,
                )
                // Progress ring — M3 primary
                CircularProgressIndicator(
                    progress      = { clampedProgress },
                    modifier      = Modifier.fillMaxSize().padding(1.dp),
                    color         = scheme.primary,
                    strokeWidth   = 14.dp,
                    strokeCap     = StrokeCap.Round,
                )
                // Subtle inner glow, pulses when running
                Box(
                    Modifier
                        .size((180 + pulse * 10).dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.05f + pulse * 0.04f)),
                )
                // Timer text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))
                    ) {
                        Text(
                            "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                            fontSize     = 54.sp,
                            fontWeight   = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            // M3 onSurface — white in dark theme
                            color        = scheme.onSurface,
                            textAlign    = TextAlign.Center,
                        )
                    }
                    Text(
                        if (isRunning) "Focus running" else "Ready to focus",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        // M3 onSurfaceVariant
                        color      = scheme.onSurfaceVariant,
                        textAlign  = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Control buttons ──────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // End — M3 FilledTonalButton style
                FilledTonalButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        // M3 surfaceContainerHigh with outlineVariant border
                        containerColor = scheme.surfaceContainerHigh,
                        contentColor   = scheme.onSurfaceVariant,
                    ),
                    border = BorderStroke(0.5.dp, scheme.outlineVariant),
                    shape  = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("End", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Start/Pause — M3 FilledButton
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors   = ButtonDefaults.buttonColors(
                        // M3 primary / onPrimary
                        containerColor = scheme.primary,
                        contentColor   = scheme.onPrimary,
                    ),
                    shape     = RoundedCornerShape(100.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(if (isRunning) "Pause" else "Start", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Take Break — M3 FilledTonalButton, only when a focus session is active
            if (canStartBreak) {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onStartBreak,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        // M3 secondaryContainer / primary
                        containerColor = scheme.secondaryContainer,
                        contentColor   = scheme.primary,
                    ),
                    border = BorderStroke(0.5.dp, scheme.primary.copy(alpha = 0.45f)),
                    shape  = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FreeBreakfast, contentDescription = null, modifier = Modifier.size(17.dp))
                        Text("Take break", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Motto line
            Text(
                text       = mottoText,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color      = scheme.onSurfaceVariant,
                textAlign  = TextAlign.Center,
            )

            // Kavach active pill
            if (kavachActive) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    onClick = onOpenKavachSession,
                    shape   = RoundedCornerShape(100.dp),
                    color   = com.safarparmar.app.ui.ekagra.focusshield.KavachDesign.Primary.copy(alpha = 0.92f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.kavach_active_status, kavachBlockedCount),
                            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Bottom navigation ─────────────────────────────────────────────────────────

@Composable
private fun EkagraBottomNav(
    selectedTab: EkagraNavTab,
    onSelect: (EkagraNavTab) -> Unit,
    isOnVideo: Boolean,
) {
    val scheme = MaterialTheme.colorScheme

    NavigationBar(
        // M3 spec: NavigationBar container = surfaceContainer
        // When we're over the video scrim, use a translucent dark bar
        containerColor = if (isOnVideo) Color(0xCC0F1115) else scheme.surfaceContainer,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(0.5.dp, scheme.outlineVariant.copy(alpha = if (isOnVideo) 0.25f else 1f),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
    ) {
        EkagraNavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            NavigationBarItem(
                selected = isSelected,
                onClick  = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector     = tab.icon,
                        contentDescription = tab.label,
                        modifier        = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(tab.label, style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                },
                colors = NavigationBarItemDefaults.colors(
                    // M3 NavigationBarItem tokens
                    selectedIconColor   = scheme.onSecondaryContainer,
                    selectedTextColor   = scheme.primary,
                    indicatorColor      = scheme.secondaryContainer,
                    unselectedIconColor = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                    unselectedTextColor = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            )
        }
    }
}

// ─── Duration tab ──────────────────────────────────────────────────────────────

@Composable
private fun DurationTab(
    modifier: Modifier,
    focusMinutes: Int,
    breakMinutes: Int,
    onFocusChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    onStartFocusSession: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            // M3 background token
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Timer duration",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = scheme.onSurface)

        DurationCard(
            icon          = Icons.Default.Timer,
            title         = "Focus duration",
            value         = focusMinutes,
            range         = 1f..120f,
            onValueChange = onFocusChange,
        )
        DurationCard(
            icon          = Icons.Default.FreeBreakfast,
            title         = "Break duration",
            value         = breakMinutes,
            range         = 1f..60f,
            onValueChange = onBreakChange,
        )

        Spacer(Modifier.height(6.dp))

        // M3 FilledButton — primary CTA, 56dp tall for prominence
        Button(
            onClick        = onStartFocusSession,
            modifier       = Modifier.fillMaxWidth().height(56.dp),
            shape          = RoundedCornerShape(16.dp),
            colors         = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor   = scheme.onPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Start focus session", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DurationCard(
    icon: ImageVector,
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var showCustomInput by remember { mutableStateOf(false) }
    var customText      by remember { mutableStateOf("") }

    // M3 ElevatedCard: surfaceContainerLow + outlineVariant border at 0.5dp
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, contentDescription = null,
                    tint     = scheme.primary,
                    modifier = Modifier.size(20.dp))
                Text(title,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 16.sp,
                    color      = scheme.onSurface,
                    modifier   = Modifier.weight(1f))
                // M3 displayLarge-ish value badge
                Text("${value}m",
                    fontWeight = FontWeight.Bold,
                    color      = scheme.onSurface,
                    fontSize   = 26.sp)
                // Small edit icon button
                IconButton(
                    onClick  = { showCustomInput = !showCustomInput; customText = "" },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Custom value",
                        modifier = Modifier.size(15.dp), tint = scheme.onSurfaceVariant)
                }
            }

            // Inline custom input (visible only when edit tapped)
            if (showCustomInput) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value         = customText,
                        onValueChange = { customText = it.filter { c -> c.isDigit() }.take(3) },
                        placeholder   = { Text("Minutes") },
                        singleLine    = true,
                        modifier      = Modifier.weight(1f),
                        shape         = RoundedCornerShape(12.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                    Button(
                        onClick = {
                            val v = customText.toIntOrNull()
                            if (v != null && v >= range.start.toInt() && v <= range.endInclusive.toInt()) {
                                onValueChange(v); showCustomInput = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Set") }
                }
            }

            // M3 Slider — track = secondaryContainer, thumb + fill = primary
            SlimSlider(
                value         = value.toFloat(),
                onValueChange = { onValueChange(it.roundToInt().coerceIn(range.start.toInt(), range.endInclusive.toInt())) },
                valueRange    = range,
                modifier      = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                activeColor   = scheme.primary,
                inactiveColor = scheme.secondaryContainer,
            )

            // Preset chips — M3 InputChip style
            val presets = if (range.endInclusive <= 60f) listOf(1, 5, 15, 30) else listOf(1, 25, 45, 60, 90)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { preset ->
                    val isSel = value == preset
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            // M3: primary when selected, surfaceContainerHigh when not
                            .background(if (isSel) scheme.primary else scheme.surfaceContainerHigh)
                            .clickable { onValueChange(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "${preset}m",
                            fontSize   = 14.sp,
                            // M3: onPrimary when selected, onSurfaceVariant when not
                            color      = if (isSel) scheme.onPrimary else scheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

// ─── History tab ───────────────────────────────────────────────────────────────

@Composable
private fun FocusHistoryTab(
    modifier: Modifier,
    analytics: EkagraAnalyticsStats,
) {
    val scheme = MaterialTheme.colorScheme
    val todaySessions    = analytics.focusSessions.filter { isTodayIso(it.endedAt ?: it.startedAt) }
    val linkedSessions   = todaySessions.filter { !it.associatedGoalId.isNullOrBlank() && it.associatedGoalId?.startsWith("named:") != true }
    val freeSessions     = todaySessions.filterNot { !it.associatedGoalId.isNullOrBlank() && it.associatedGoalId?.startsWith("named:") != true }
    val todayFocusMins   = todaySessions.sumOf { it.actualMinutes }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (todaySessions.isEmpty()) {
            // Empty state
            Card(
                shape     = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                border    = BorderStroke(0.5.dp, scheme.outlineVariant),
                modifier  = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.History, contentDescription = null,
                        tint     = scheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(40.dp))
                    Text("No focus sessions today.",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = scheme.onSurfaceVariant)
                }
            }
            return@Column
        }

        Column(Modifier.fillMaxWidth()) {
            Text("Today's focus",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = scheme.onSurface)
            Text("Today only.", fontSize = 12.sp, color = scheme.onSurfaceVariant)
        }

        // Total minutes card — primary accent on the number
        Card(
            shape     = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
            border    = BorderStroke(0.5.dp, scheme.outlineVariant),
            modifier  = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
                    // primary colour — same as ring, consistent meaning across screen
                    Text(formatMinutes(todayFocusMins), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = scheme.primary)
                }
                Text("TOTAL FOCUS TIME",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color         = scheme.onSurfaceVariant)
            }
        }

        HistorySection(
            title      = "Goal focus",
            subtitle   = "Sessions linked to a goal",
            sessions   = linkedSessions,
            emptyText  = "No linked sessions today.",
        )
        HistorySection(
            title      = "Free focus",
            subtitle   = "Sessions without a linked goal",
            sessions   = freeSessions,
            emptyText  = "No free focus today.",
        )
    }
}

@Composable
private fun HistorySection(
    title: String,
    subtitle: String,
    sessions: List<com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession>,
    emptyText: String,
) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        // Accent border on history sections using outlineVariant — subtle, not primary
        border    = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = scheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = scheme.onSurfaceVariant)
            if (sessions.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        // M3: surfaceContainer for inner empty states
                        .background(scheme.surfaceContainer)
                        .padding(12.dp)
                ) {
                    Text(emptyText, fontSize = 13.sp, color = scheme.onSurfaceVariant)
                }
            } else {
                sessions.forEach { session -> FocusSessionRow(session) }
            }
        }
    }
}

@Composable
private fun FocusSessionRow(session: com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession) {
    val scheme    = MaterialTheme.colorScheme
    val isLinked  = !session.associatedGoalId.isNullOrBlank() && session.associatedGoalId?.startsWith("named:") != true

    Card(
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        // M3 surfaceContainer — one elevation step above the parent surfaceContainerLow card
        colors    = CardDefaults.cardColors(containerColor = scheme.surfaceContainer),
        border    = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier  = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(session.taskText ?: "Unlabeled task",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = scheme.onSurface,
                        maxLines   = 1,
                        modifier   = Modifier.weight(1f, fill = false))
                    if (isLinked) {
                        // M3 tertiaryContainer badge — complementary accent role
                        Text(
                            "Linked",
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = scheme.onTertiaryContainer,
                            modifier   = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(scheme.tertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text("Planned ${session.durationMinutes}m · Actual ${session.actualMinutes}m",
                    fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
            // Time — primary colour, consistent with ring and total card
            Text(
                formatTime(session.endedAt ?: session.startedAt),
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = scheme.primary,
            )
        }
    }
}

// ─── Guide dialog ──────────────────────────────────────────────────────────────

@Composable
private fun EkagraGuideDialog(onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Medium) }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ekagra guide", fontWeight = FontWeight.Bold)
                Text("Controls, flows, and best practices.",
                    fontSize = 12.sp, color = scheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Highlight card — primaryContainer
                Card(
                    shape     = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors    = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
                    border    = BorderStroke(0.5.dp, scheme.primary.copy(alpha = 0.30f)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What changed", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.onPrimaryContainer)
                        listOf(
                            "Goals live in Goals — Ekagra only links to them.",
                            "Saved sessions appear in History and Analytics.",
                            "Focus time is added to a goal; completion is your choice.",
                            "Analytics now uses actual timer time per session.",
                        ).forEach { GuideBullet(it) }
                    }
                }
                GuideSection("Starting a session", listOf(
                    "Start first, then save the finished timer as Free Focus or Goal Focus.",
                    "Set duration from the Duration tab.",
                    "Press Start to begin."))
                GuideSection("Timer controls", listOf(
                    "Start / Pause — begins or pauses the session.",
                    "End — finishes and saves the session to history.",
                    "Mode pill — switch between Focus, Break, and Long break.",
                    "Take Break — switches directly to a break without ending.",
                    "PiP — mini timer window while you work elsewhere."))
                GuideSection("History & sessions", listOf(
                    "Recovery — unfinished timers can be resumed, saved, or discarded.",
                    "Resume — reopens a paused session and restarts the timer.",
                    "History — today's saved focus split into Goal and Free focus."))
                GuideSection("Linked goal behaviour", listOf(
                    "The linked goal shows at the top while you work.",
                    "Unlinking removes the connection but keeps the goal.",
                    "After the session ends you can link or free-save it."))
                GuideSection("Audio & environment", listOf(
                    "Theme — changes the video background.",
                    "Music — pick ambient sound from the toolbar.",
                    "Volume — adjust from the system or media controls."))
                GuideSection("Recommended flow", listOf(
                    "Set a realistic duration before starting.",
                    "Pause only when needed — resume the same session.",
                    "End when you finish; it moves to History automatically.",
                    "Review patterns in Analytics › Focus."))
            }
        },
        shape = RoundedCornerShape(24.dp),
    )
}

@Composable
private fun GuideSection(title: String, bullets: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        bullets.forEach { GuideBullet(it) }
    }
}

@Composable
private fun GuideBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(4.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant))
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── Theme & sound dialogs / sheets ───────────────────────────────────────────

@Composable
private fun VisualThemeDialog(current: VisualTheme, onSelect: (VisualTheme) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = scheme.surface,
        title  = { Text("Visual theme", fontWeight = FontWeight.Bold) },
        text   = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visualThemes.forEach { theme ->
                    val isSelected = theme.name == current.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            // M3: primaryContainer tint when selected
                            .background(if (isSelected) scheme.primaryContainer else scheme.surfaceContainerHigh)
                            .then(if (isSelected) Modifier.border(1.5.dp, scheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { onSelect(theme) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(theme.accent),
                            contentAlignment = Alignment.Center) {
                            Text(theme.emoji, fontSize = 18.sp)
                        }
                        Text(theme.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                            modifier   = Modifier.weight(1f))
                        if (isSelected)
                            Icon(Icons.Default.Check, contentDescription = null,
                                tint = scheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongPickerSheet(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // M3: bottom sheets use surfaceContainerLow
        containerColor   = scheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ambient sound", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            focusMusicTracks.forEach { (name, _) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        // M3: primaryContainer tint on selected row
                        .background(if (current == name) scheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                        .clickable { onSelect(name) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (current == name) scheme.primary else scheme.onSurface.copy(0.5f))
                    Text(name, fontSize = 14.sp, modifier = Modifier.weight(1f), color = scheme.onSurface)
                    if (current == name)
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = scheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ─── Organize free focus sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrganizeFreeFocusSheet(
    pending: PendingEndedEkagraSession?,
    goals: List<com.safarparmar.app.domain.model.Goal>,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaveFree: () -> Unit,
    onLinkGoal: (com.safarparmar.app.domain.model.Goal) -> Unit,
    onDiscard: () -> Unit,
) {
    val scheme        = MaterialTheme.colorScheme
    val focusedMins   = ((pending?.totalSeconds ?: 0) - (pending?.secondsLeft ?: 0)).coerceAtLeast(60) / 60
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = scheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("You focused for ${focusedMins} min.",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("What were you working on?", fontSize = 13.sp, color = scheme.onSurfaceVariant)

            Text("Link to a goal",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color    = scheme.onSurfaceVariant)

            if (goals.isEmpty()) {
                Text("No open goals available.", fontSize = 13.sp, color = scheme.onSurfaceVariant)
            } else {
                goals.take(5).forEach { goal ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLinkGoal(goal) }
                            // M3 surfaceContainer for list rows
                            .background(scheme.surfaceContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null,
                            tint = scheme.primary, modifier = Modifier.size(16.dp))
                        Text(goal.title,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = scheme.onSurface,
                            modifier   = Modifier.weight(1f),
                            maxLines   = 1)
                    }
                }
            }

            OutlinedTextField(
                value         = titleInput,
                onValueChange = onTitleChange,
                placeholder   = { Text("Add a title") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("Discard", fontSize = 12.sp)
                }
                Button(
                    onClick  = onSaveFree,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor   = scheme.onPrimary,
                    ),
                ) {
                    Text("Save free focus", fontSize = 12.sp)
                }
            }
        }
    }
}

// ─── Private data class ────────────────────────────────────────────────────────

private data class PendingEndedEkagraSession(
    val sessionId: String,
    val totalSeconds: Int,
    val secondsLeft: Int,
    val mode: String,
    val startedAt: String?,
)

// ─── Utility functions ─────────────────────────────────────────────────────────

private fun formatMinutes(min: Int): String = when {
    min <= 0 -> "0m"
    min < 60 -> "${min}m"
    else     -> "${min / 60}h ${min % 60}m".let { if (it.endsWith(" 0m")) it.dropLast(3) else it }
}

private fun parseInstantOrNull(iso: String?): Instant? =
    iso?.let { runCatching { Instant.parse(it) }.getOrNull() }

private fun isTodayIso(iso: String?): Boolean {
    val zone = ZoneId.systemDefault()
    return parseInstantOrNull(iso)?.atZone(zone)?.toLocalDate() == java.time.LocalDate.now(zone)
}

private fun formatTime(iso: String?): String {
    val zone     = ZoneId.systemDefault()
    val dateTime = parseInstantOrNull(iso)?.atZone(zone) ?: return "-"
    return java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(dateTime)
}

private fun TimerMode.toApiMode(): String = when (this) {
    TimerMode.FOCUS      -> "Timer"
    TimerMode.BREAK      -> "short"
    TimerMode.LONG_BREAK -> "long"
}
