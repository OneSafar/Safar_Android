package com.safar.app.ui.ekagra



import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.safar.app.R
import com.safar.app.domain.model.EkagraAnalyticsStats
import com.safar.app.notifications.rememberNotificationPermissionRequester
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.media.MediaPlayer
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import com.safar.app.MainActivity
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import android.view.TextureView
import androidx.annotation.DrawableRes
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign


val LocalTimerService = staticCompositionLocalOf<TimerService?> { null }


@Composable
private fun EkagraVideoBackground(videoUrl: String, modifier: Modifier = Modifier) {
    if (videoUrl.isBlank()) {
        Box(modifier.background(Color.Black))
        return
    }
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val screenW = ctx.resources.displayMetrics.widthPixels
            val screenH = ctx.resources.displayMetrics.heightPixels

            val textureView = android.view.TextureView(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(screenW, screenH)
            }

            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    p0: SurfaceTexture,
                    p1: Int,
                    p2: Int
                ) {
                    try {
                        val mp = MediaPlayer().apply {
                            setSurface(android.view.Surface(p0))
                            setDataSource(ctx, Uri.parse(videoUrl))
                            isLooping = true
                            setVolume(0f, 0f)

                            setOnPreparedListener { player ->
                                val vW = screenW.toFloat()
                                val vH = screenH.toFloat()

                                val vidW = player.videoWidth.takeIf { it > 0 }?.toFloat() ?: vW
                                val vidH = player.videoHeight.takeIf { it > 0 }?.toFloat() ?: vH

                                val videoAspect = vidW / vidH
                                val viewAspect  = vW / vH
                                val (scaleX, scaleY) = if (videoAspect > viewAspect)
                                    Pair(videoAspect / viewAspect, 1f)
                                else
                                    Pair(1f, viewAspect / videoAspect)

                                textureView.scaleX  = scaleX
                                textureView.scaleY  = scaleY
                                textureView.pivotX  = vW / 2f
                                textureView.pivotY  = vH / 2f

                                player.start()
                            }

                            setOnErrorListener { _, what, extra ->
                                Log.e("VIDEO_DEBUG", "Error: $what $extra")
                                true
                            }

                            prepareAsync()
                        }

                        textureView.setTag(R.id.ekagra_player_tag, mp)

                    } catch (e: Exception) {
                        Log.e("VIDEO_DEBUG", "Video init failed", e)
                    }
                }

                override fun onSurfaceTextureSizeChanged(
                    p0: SurfaceTexture,
                    p1: Int,
                    p2: Int
                ) = Unit

                override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                    val mp = textureView.getTag(R.id.ekagra_player_tag) as? MediaPlayer
                    textureView.setTag(R.id.ekagra_player_tag, null)

                    try {
                        mp?.stop()
                    } catch (_: Exception) {}

                    try {
                        mp?.release()
                    } catch (_: Exception) {}

                    return true                }

                override fun onSurfaceTextureUpdated(p0: SurfaceTexture) = Unit
            }

            android.widget.FrameLayout(ctx).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(screenW, screenH)
                setTag(com.safar.app.R.id.ekagra_video_url_tag, videoUrl)
                addView(textureView)
            }
        },
        update = { frame ->
            val currentUrl = frame.getTag(com.safar.app.R.id.ekagra_video_url_tag) as? String
            if (currentUrl == videoUrl) return@AndroidView

            frame.setTag(com.safar.app.R.id.ekagra_video_url_tag, videoUrl)
            val tv = frame.getChildAt(0) as? android.view.TextureView ?: return@AndroidView
            val st = tv.surfaceTexture ?: return@AndroidView

            val oldPlayer = tv.getTag(com.safar.app.R.id.ekagra_player_tag) as? MediaPlayer
            tv.setTag(com.safar.app.R.id.ekagra_player_tag, null)

            val ctx = frame.context
            val screenW = ctx.resources.displayMetrics.widthPixels
            val screenH = ctx.resources.displayMetrics.heightPixels

            Thread {
                runCatching { oldPlayer?.stop() }
                runCatching { oldPlayer?.release() }
                try {
                    val newPlayer = MediaPlayer().apply {
                        setSurface(android.view.Surface(st))
                        setDataSource(ctx, Uri.parse(videoUrl))
                        isLooping = true
                        setVolume(0f, 0f)
                        setOnPreparedListener { mp ->
                            Handler(Looper.getMainLooper()).post {
                                val vW = screenW.toFloat()
                                val vH = screenH.toFloat()
                                val vidW = mp.videoWidth.takeIf { it > 0 }?.toFloat() ?: vW
                                val vidH = mp.videoHeight.takeIf { it > 0 }?.toFloat() ?: vH
                                val videoAspect = vidW / vidH
                                val viewAspect  = vW / vH
                                val (scaleX, scaleY) = if (videoAspect > viewAspect)
                                    Pair(videoAspect / viewAspect, 1f)
                                else
                                    Pair(1f, viewAspect / videoAspect)
                                tv.scaleX  = scaleX
                                tv.scaleY  = scaleY
                                tv.pivotX  = vW / 2f
                                tv.pivotY  = vH / 2f
                                mp.start()
                            }
                        }
                        setOnErrorListener { _, _, _ -> true }
                        prepareAsync()
                    }
                    tv.setTag(com.safar.app.R.id.ekagra_player_tag, newPlayer)
                } catch (e: Exception) { /* ignore */ }
            }.start()
        },
        modifier = modifier,
    )
}


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


private enum class EkagraNavTab(val icon: ImageVector, val label: String) {
    TIMER    (Icons.Default.Timer,          "Focus"),
    DURATION (Icons.Default.Tune,           "Duration"),
    HISTORY  (Icons.Default.History,        "History"),
}


enum class TimerMode(
    @DrawableRes val lightIconRes: Int,
    @DrawableRes val darkIconRes: Int,
    val label: String,
    val showInPill: Boolean = true,
) {
    FOCUS(
        R.drawable.ic_ekagra_timer_light,
        R.drawable.ic_ekagra_timer_dark,
        "Focus",
        true,
    ),
    BREAK(
        R.drawable.ic_ekagra_coffee_light,
        R.drawable.ic_ekagra_coffee_dark,
        "Break",
        true,
    ),
    LONG_BREAK(
        R.drawable.ic_ekagra_bed_light,
        R.drawable.ic_ekagra_bed_dark,
        "Long Break",
        true,
    ),
}


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
    focusShieldViewModel: com.safar.app.ui.ekagra.focusshield.FocusShieldViewModel = hiltViewModel(),
) {
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val ekagraAnalytics by viewModel.ekagraAnalytics.collectAsStateWithLifecycle()
    val allGoals by viewModel.allGoals.collectAsStateWithLifecycle()

    val timerService = LocalTimerService.current
    val context      = LocalContext.current
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    val shieldState by focusShieldViewModel.shieldState.collectAsStateWithLifecycle()

    val secondsLeft  by (timerService?.secondsLeft  ?: MutableStateFlow(25 * 60)).collectAsStateWithLifecycle()
    val totalSeconds by (timerService?.totalSeconds ?: MutableStateFlow(25 * 60)).collectAsStateWithLifecycle()
    val timerRunning by (timerService?.isRunning    ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val timerMode    by (timerService?.timerMode    ?: MutableStateFlow(TimerMode.FOCUS)).collectAsStateWithLifecycle()
    val focusShieldActive by (timerService?.focusShieldActive ?: MutableStateFlow(false)).collectAsStateWithLifecycle()

    var selectedTab      by remember { mutableStateOf(EkagraNavTab.TIMER) }
    var showThemeDialog  by remember { mutableStateOf(false) }
    var showSongSheet    by remember { mutableStateOf(false) }
    var showEkagraGuide  by remember { mutableStateOf(false) }
    var showOrganizeSheet by remember { mutableStateOf(false) }
    var pendingEndedSession by remember { mutableStateOf<PendingEndedEkagraSession?>(null) }
    var titleInput by remember { mutableStateOf("") }
    var tourState        by remember { mutableStateOf<com.safar.app.ui.butterfly.ButterflyTourState?>(null) }
    var selectedTheme by remember {
        val prefs = context.getSharedPreferences("ekagra_theme_prefs", android.content.Context.MODE_PRIVATE)
        val idx   = prefs.getInt("theme_index", -1)
        mutableStateOf(visualThemes.getOrElse(idx) { visualThemes[0] })
    }
    var selectedSong by remember {
        val prefs    = context.getSharedPreferences("ekagra_theme_prefs", android.content.Context.MODE_PRIVATE)
        val songName = prefs.getString("song_name", null)
        mutableStateOf(songName ?: "Theme Default")
    }
    var associatedGoalId by remember(linkedGoalId) { mutableStateOf(linkedGoalId) }
    var associatedGoalTitle by remember(linkedGoalTitle) { mutableStateOf(linkedGoalTitle) }
    var taskText         by remember(linkedGoalId, linkedGoalTitle) { mutableStateOf(linkedGoalTitle.orEmpty()) }

    var focusMinutes by remember { mutableStateOf(25) }
    var breakMinutes by remember { mutableStateOf(5) }
    var longBreakMinutes by remember { mutableStateOf(15) }

    fun startTimer(mode: TimerMode, minutes: Int) {
        if (
            mode == TimerMode.FOCUS &&
            shieldState.isEnabled &&
            shieldState.blockedPackages.isNotEmpty() &&
            (!shieldState.hasUsageStats || !shieldState.hasAccessibilityService)
        ) {
            onNavigate(Routes.FOCUS_SHIELD)
            return
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
            timerService?.setFocusShieldConfig(
                packages = shieldState.blockedPackages,
                strict   = shieldState.isStrictMode,
            )
            timerService?.enableFocusShieldForSession()
        }
    }

    fun resetTimer() {
        activeSession?.id?.let { viewModel.discardSession(it) }
        timerService?.reset()          // also calls disableFocusShieldForSession()
        associatedGoalId = null
        associatedGoalTitle = null
    }

    fun endCurrentSession() {
        val session = activeSession
        if (session != null && timerMode == TimerMode.FOCUS) {
            timerService?.pause()
            pendingEndedSession = PendingEndedEkagraSession(
                sessionId = session.id,
                totalSeconds = totalSeconds,
                secondsLeft = secondsLeft,
                mode = timerMode.toApiMode(),
                startedAt = session.sessionStartedAt,
            )
            titleInput = session.sessionTitle ?: taskText
            showOrganizeSheet = true
            return
        }
        viewModel.onSessionCompleted(totalSeconds, secondsLeft, timerMode.toApiMode())
        timerService?.reset()
        associatedGoalId = null
        associatedGoalTitle = null
    }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            EkagraNavTab.TIMER -> {
                if (timerService?.isActive() == false)
                    timerService?.setDuration(TimerMode.FOCUS, focusMinutes * 60)
            }
            else -> {}
        }
    }

    LaunchedEffect(initialView) {
        if (initialView == "analytics") {
            onNavigate(Routes.nishthaAnalytics("focus"))
        }
    }

    // Refresh on first composition and re-poll every 20s, but only while the
    // host lifecycle is at least STARTED. When the user backgrounds the app or
    // navigates to another tab, repeatOnLifecycle cancels this block, so we
    // stop burning CPU/network on hidden screens.
    val ekagraLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(ekagraLifecycleOwner) {
        ekagraLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshEkagra()
            viewModel.loadTasks()
            while (true) {
                delay(20_000L)
                viewModel.loadEkagraAnalytics()
            }
        }
    }

    val latestTotalSeconds by rememberUpdatedState(totalSeconds)
    val latestSecondsLeft by rememberUpdatedState(secondsLeft)
    val latestTimerMode by rememberUpdatedState(timerMode)
    val latestGoalTitle by rememberUpdatedState(associatedGoalTitle)
    val latestTaskText by rememberUpdatedState(taskText)
    LaunchedEffect(timerRunning) {
        while (timerRunning) {
            delay(15_000L)
            if (latestSecondsLeft > 0 && latestTimerMode == TimerMode.FOCUS) {
                viewModel.syncActiveSession(
                    totalSeconds = latestTotalSeconds,
                    secondsLeft = latestSecondsLeft,
                    mode = latestTimerMode.toApiMode(),
                    isRunning = true,
                    goalTitle = latestGoalTitle ?: latestTaskText.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    fun resolveAudioUrl(): String = when {
        selectedSong == "Theme Default" -> selectedTheme.musicUrl
        selectedSong == "Silence"       -> "silence"
        else -> focusMusicTracks.firstOrNull { it.first == selectedSong }?.second ?: ""
    }

    LaunchedEffect(selectedSong, selectedTheme) {
        timerService?.setMusic(resolveAudioUrl())
        if (timerService?.isActive() == true) {
            timerService.saveTheme(visualThemes.indexOf(selectedTheme), selectedSong)
        }
    }

    val pipContext  = LocalContext.current
    val pipActivity = pipContext as? Activity
    val mainActivity = pipActivity as? MainActivity
    val isInPipMode = mainActivity?.isInPipMode == true

    val PIP_REQUEST_PLAY      = 1

    fun buildPipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(1, 1))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true)
            builder.setAutoEnterEnabled(timerRunning)
        }
        val playPauseIcon = android.graphics.drawable.Icon.createWithResource(
            pipContext, if (timerRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        builder.setActions(
            listOf(
                RemoteAction(
                    playPauseIcon,
                    if (timerRunning) "Pause" else "Play",
                    if (timerRunning) "Pause timer" else "Start timer",
                    android.app.PendingIntent.getService(
                        pipContext,
                        PIP_REQUEST_PLAY,
                        Intent(pipContext, TimerService::class.java).apply { action = TimerService.ACTION_PLAY_PAUSE },
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                ),
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.setTitle("SAFAR Focus Timer")
            builder.setSubtitle("Focus timer running")
        }
        return builder.build()
    }

    LaunchedEffect(timerRunning, secondsLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { buildPipParams()?.let { pipActivity?.setPictureInPictureParams(it) } } catch (_: Exception) {}
        }
        if (!timerRunning && secondsLeft == 0 && totalSeconds > 0) {
            val session = activeSession
            if (timerMode != TimerMode.FOCUS) return@LaunchedEffect
            if (timerMode == TimerMode.FOCUS && session != null) {
                pendingEndedSession = PendingEndedEkagraSession(
                    sessionId = session.id,
                    totalSeconds = totalSeconds,
                    secondsLeft = 0,
                    mode = timerMode.toApiMode(),
                    startedAt = session.sessionStartedAt,
                )
                titleInput = session.sessionTitle ?: ""
                showOrganizeSheet = true
                return@LaunchedEffect
            }
            viewModel.onSessionCompleted(
                totalSeconds = totalSeconds,
                secondsLeft  = 0,
                mode = timerMode.toApiMode(),
            )
            associatedGoalId = null
            associatedGoalTitle = null
        }
    }

    val accent    = selectedTheme.accent
    val mottoText = when {
        timerMode != TimerMode.FOCUS && timerRunning -> "BREAK TIME - KAVACH PAUSED"
        timerRunning -> "STAY FOCUSED, YOU'RE DOING GREAT!"
        else -> "READY TO FOCUS?"
    }
    val progress  = if (totalSeconds > 0) 1f - secondsLeft.toFloat() / totalSeconds else 0f
    val topBarColor = if (selectedTab == EkagraNavTab.TIMER || isDarkTheme) Color.White else Color.Black
    val openGoals = remember(allGoals) {
        allGoals.filter { goal ->
            goal.id.isNotBlank() &&
                goal.title.isNotBlank() &&
                !goal.completed &&
                goal.lifecycleStatus !in listOf("abandoned", "rolled_over")
        }
    }

    if (isInPipMode) {
        val shieldActive = focusShieldActive && timerRunning
        val pipBg = if (shieldActive) Color(0xFF1C1917) else Color(0xFF05070A)
        val pipAccent = if (shieldActive) Color(0xFFF59E0B) else accent
        Box(
            Modifier
                .fillMaxSize()
                .background(pipBg)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (shieldActive) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(pipAccent.copy(alpha = 0.18f))
                            .border(1.dp, pipAccent.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = pipAccent,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Text(
                    "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                    fontSize = if (shieldActive) 36.sp else 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Box(Modifier.fillMaxWidth(0.82f).height(4.dp).clip(RoundedCornerShape(999.dp)).background(Color.White.copy(0.16f))) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(pipAccent),
                    )
                }
                Text(
                    when {
                        shieldActive -> "SHIELD ACTIVE"
                        timerRunning -> "FOCUSING"
                        else -> "PAUSED"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    color = if (shieldActive) pipAccent else Color.White.copy(0.65f),
                )
            }
        }
        return
    }

    if (showThemeDialog) {
        VisualThemeDialog(current = selectedTheme, onSelect = { selectedTheme = it; showThemeDialog = false }, onDismiss = { showThemeDialog = false })
    }
    if (showSongSheet) {
        SongPickerSheet(current = selectedSong, onSelect = { selectedSong = it; showSongSheet = false }, onDismiss = { showSongSheet = false })
    }
    if (showEkagraGuide) {
        EkagraGuideDialog(onDismiss = { showEkagraGuide = false }, accent = accent)
    }
    if (showOrganizeSheet) {
        val pending = pendingEndedSession
        OrganizeFreeFocusSheet(
            pending = pending,
            goals = openGoals,
            titleInput = titleInput,
            onTitleChange = { titleInput = it },
            accent = accent,
            onDismiss = { showOrganizeSheet = false },
            onSaveFree = {
                if (pending != null) {
                    viewModel.completeSession(
                        sessionId = pending.sessionId,
                        totalSeconds = pending.totalSeconds,
                        secondsLeft = pending.secondsLeft,
                        mode = pending.mode,
                        startedAt = pending.startedAt,
                        taskTitle = titleInput.ifBlank { "Free Focus" },
                        goalId = null,
                        goalTitle = null,
                    )
                    timerService?.reset()
                    associatedGoalId = null
                    associatedGoalTitle = null
                    pendingEndedSession = null
                    showOrganizeSheet = false
                }
            },
            onLinkGoal = { goal ->
                if (pending != null) {
                    viewModel.linkGoalAndCompleteSession(
                        sessionId = pending.sessionId,
                        goal = goal,
                        totalSeconds = pending.totalSeconds,
                        secondsLeft = pending.secondsLeft,
                        mode = pending.mode,
                        startedAt = pending.startedAt,
                    )
                    timerService?.reset()
                    associatedGoalId = null
                    associatedGoalTitle = null
                    pendingEndedSession = null
                    showOrganizeSheet = false
                }
            },
            onDiscard = {
                if (pending != null) {
                    viewModel.discardSession(pending.sessionId)
                    timerService?.reset()
                    associatedGoalId = null
                    associatedGoalTitle = null
                    pendingEndedSession = null
                    showOrganizeSheet = false
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        SafarDrawerScaffold(
            title             = stringResource(R.string.module_ekagra),
            subtitle          = stringResource(R.string.app_name),
            currentRoute      = currentRoute,
            isDarkTheme       = isDarkTheme,
            onNavigate        = onNavigate,
            onToggleDarkTheme = onToggleNightMode,
            onLanguageClick   = onLanguageClick,
            topBarContentColor = topBarColor,
            topBarActions     = {
                IconButton(onClick = { showEkagraGuide = true }) { Icon(Icons.Default.HelpOutline, contentDescription = "Ekagra Usage Guide") }
                IconButton(onClick = { showThemeDialog = true }) { Icon(Icons.Default.Palette, contentDescription = "Theme") }
                IconButton(onClick = { showSongSheet = true }) { Icon(Icons.Default.MusicNote, contentDescription = "Song") }
            },
        ) { padding ->

            val isTimerTab = selectedTab == EkagraNavTab.TIMER
            if (isTimerTab) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.38f)))
                EkagraVideoBackground(videoUrl = selectedTheme.videoUrl, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.38f)))
            }

            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = Color.Transparent,
                    snackbarHost   = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        EkagraBottomNav(
                            selectedTab = selectedTab,
                            onSelect = { selectedTab = it },
                            accent = accent,
                        )
                    },
                ) { innerPadding ->

                    when (selectedTab) {

                        EkagraNavTab.TIMER -> {
                            TimerFocusTab(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = padding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
                                timerMode = timerMode,
                                secondsLeft = secondsLeft,
                                isRunning = timerRunning,
                                accent = accent,
                                progress = progress,
                                mottoText = mottoText,
                                onModeChange = { mode ->
                                    val mins = when (mode) {
                                        TimerMode.FOCUS -> focusMinutes
                                        TimerMode.BREAK -> breakMinutes
                                        TimerMode.LONG_BREAK -> longBreakMinutes
                                    }
                                    val service = timerService
                                    if (
                                        mode != TimerMode.FOCUS &&
                                        timerMode == TimerMode.FOCUS &&
                                        service?.isActive() == true
                                    ) {
                                        service.startBreak(mode, mins * 60)
                                    } else {
                                        service?.setDuration(mode, mins * 60)
                                    }
                                },
                                onPlayPause = {
                                    val wasRunning = timerRunning
                                    val wasInactive = timerService?.isActive() == false
                                    if (wasInactive) requestNotificationPermission()
                                    timerService?.togglePlayPause()
                                    if (wasInactive && timerMode == TimerMode.FOCUS) {
                                        viewModel.onSessionStarted(
                                            taskText = taskText,
                                            totalSeconds = totalSeconds,
                                            goalId = if (timerMode == TimerMode.FOCUS) associatedGoalId else null,
                                            goalTitle = if (timerMode == TimerMode.FOCUS) associatedGoalTitle else null,
                                            mode = timerMode.toApiMode(),
                                        )
                                    } else if (wasRunning && timerMode == TimerMode.FOCUS) {
                                        viewModel.pauseActiveSession(totalSeconds, secondsLeft, timerMode.toApiMode(), associatedGoalTitle)
                                    } else if (timerMode == TimerMode.FOCUS) {
                                        viewModel.syncActiveSession(
                                            totalSeconds = totalSeconds,
                                            secondsLeft = secondsLeft,
                                            mode = timerMode.toApiMode(),
                                            isRunning = true,
                                            goalTitle = associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() },
                                        )
                                    }
                                },
                                canStartBreak = timerMode == TimerMode.FOCUS && timerService?.isActive() == true,
                                onStartBreak = {
                                    timerService?.startBreak(TimerMode.BREAK, breakMinutes * 60)
                                },
                                onReset = { endCurrentSession() },
                                isDarkTheme = isDarkTheme,
                            )
                        }

                        EkagraNavTab.DURATION -> {
                            DurationTab(
                                modifier      = Modifier.padding(top = padding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
                                focusMinutes  = focusMinutes,
                                breakMinutes  = breakMinutes,
                                accent        = accent,
                                onFocusChange = { focusMinutes = it },
                                onBreakChange = { breakMinutes = it; longBreakMinutes = it },
                                onStartFocusSession = { startTimer(TimerMode.FOCUS, focusMinutes) },
                            )
                        }

                        EkagraNavTab.HISTORY -> {
                            FocusHistoryTab(
                                modifier = Modifier.padding(top = padding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
                                analytics = ekagraAnalytics,
                                accent = accent,
                            )
                        }
                    }
                }
            }
        }

        com.safar.app.ui.tour.TourManager(
            dataStore        = viewModel.dataStore,
            steps            = com.safar.app.ui.tour.ekagraTourSteps,
            askOnFirstVisit  = false,
            onTourStateReady = { tourState = it },
        )
    }
}

private val EkagraTimerCardShape = RoundedCornerShape(28.dp)

@Composable
private fun ModePill(
    selected: TimerMode,
    accent: Color,
    isDarkTheme: Boolean,
    onSelect: (TimerMode) -> Unit,
) {
    val resolvedDark = isDarkTheme || MaterialTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50.dp))
            .padding(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimerMode.entries.filter { it.showInPill }.forEach { mode ->
                val isSelected = mode == selected
                val iconRes = when {
                    isSelected -> mode.lightIconRes
                    resolvedDark -> mode.lightIconRes
                    else -> mode.darkIconRes
                }
                val iconAlpha = when {
                    isSelected -> 1f
                    resolvedDark -> 0.88f
                    else -> 0.8f
                }
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) accent else Color.Transparent,
                        )
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = mode.label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp).alpha(iconAlpha),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerFocusTab(
    modifier: Modifier,
    timerMode: TimerMode,
    secondsLeft: Int,
    isRunning: Boolean,
    accent: Color,
    progress: Float,
    mottoText: String,
    onModeChange: (TimerMode) -> Unit,
    onPlayPause: () -> Unit,
    canStartBreak: Boolean,
    onStartBreak: () -> Unit,
    onReset: () -> Unit,
    isDarkTheme: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    val resolvedDark = isDarkTheme || scheme.background.luminance() < 0.5f
    val panelColor = if (resolvedDark) Color(0x99242624) else scheme.surfaceVariant.copy(alpha = 0.58f)
    // Visible theme-aware stroke (Surface + BorderStroke avoids clip eating the border)
    val timerCardOutline = accent.copy(alpha = if (resolvedDark) 0.75f else 0.9f)
    val controlBorder = lerp(scheme.outline, scheme.primary, 0.1f)
        .copy(alpha = if (resolvedDark) 0.48f else 0.38f)
    val secondaryText = if (resolvedDark) Color(0xFFABABA8) else scheme.onSurfaceVariant
    val timerTextColor = scheme.onSurface

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(140.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = EkagraTimerCardShape,
                color = panelColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(2.9.dp, timerCardOutline),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    ModePill(
                        selected = timerMode,
                        accent = accent,
                        isDarkTheme = isDarkTheme,
                        onSelect = onModeChange,
                    )
                    Text(
                        "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = timerTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (resolvedDark) 0.35f else 0.62f))
                                .border(1.dp, controlBorder, RoundedCornerShape(999.dp))
                                .clickable { onReset() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = secondaryText, modifier = Modifier.size(16.dp))
                                Text("End", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = secondaryText)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.9f))))
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                                Text(
                                    if (isRunning) "Pause" else "Start",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                    }
                    if (canStartBreak) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (resolvedDark) 0.28f else 0.55f))
                                .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
                                .clickable { onStartBreak() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FreeBreakfast, contentDescription = null, tint = accent, modifier = Modifier.size(17.dp))
                                Text("Take Break", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.weight(1f))

            Text(
                text = mottoText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = secondaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            ) {
                val clamped = progress.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(clamped)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.6f), accent))),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = ((maxWidth - 24.dp) * clamped))
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EkagraBottomNav(
    selectedTab: EkagraNavTab,
    onSelect: (EkagraNavTab) -> Unit,
    accent: Color,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = if (selectedTab == EkagraNavTab.TIMER) {
        if (isDark) Color(0xCC111317) else scheme.surface
    } else {
        scheme.surface
    }
    val inactive = if (isDark) {
        if (selectedTab == EkagraNavTab.TIMER) Color(0xFF737373) else scheme.onSurfaceVariant
    } else {
        Color.Black
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EkagraNavTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val tint = if (isSelected) accent else inactive
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(22.dp))
                Text(tab.label, fontSize = 10.sp, letterSpacing = 1.sp, color = tint, fontWeight = FontWeight.Medium)
            }
        }
    }
}


@Composable
private fun DurationTab(
    modifier: Modifier,
    focusMinutes: Int,
    breakMinutes: Int,
    accent: Color,
    onFocusChange: (Int) -> Unit,
    onBreakChange: (Int) -> Unit,
    onStartFocusSession: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val sectionTitleColor = scheme.onSurface
    val cardColor = scheme.surface
    val cardBorder = scheme.outline.copy(alpha = if (scheme.background.luminance() < 0.5f) 0.45f else 0.28f)
    val chipMutedBg = scheme.surfaceVariant.copy(alpha = if (scheme.background.luminance() < 0.5f) 0.65f else 0.9f)
    val chipMutedText = scheme.onSurface
    val buttonTextColor = if (scheme.background.luminance() < 0.5f) scheme.background else scheme.onPrimary

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Timer Duration", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = sectionTitleColor)
        DurationCard(
            icon = Icons.Default.Timer,
            title = "Timer Duration",
            value = focusMinutes,
            range = 1f..120f,
            accent = accent,
            cardColor = cardColor,
            cardBorder = cardBorder,
            chipMutedBg = chipMutedBg,
            chipMutedText = chipMutedText,
            onValueChange = onFocusChange,
        )
        DurationCard(
            icon = Icons.Default.FreeBreakfast,
            title = "Set Break",
            value = breakMinutes,
            range = 1f..60f,
            accent = accent,
            cardColor = cardColor,
            cardBorder = cardBorder,
            chipMutedBg = chipMutedBg,
            chipMutedText = chipMutedText,
            onValueChange = onBreakChange,
        )
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = onStartFocusSession,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = buttonTextColor,
            ),
        ) {
            Text("Start Focus Session", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
    accent: Color,
    cardColor: Color,
    cardBorder: Color,
    chipMutedBg: Color,
    chipMutedText: Color,
    onValueChange: (Int) -> Unit,
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customText      by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("${value}m", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 25.sp)
                IconButton(onClick = { showCustomInput = !showCustomInput; customText = "" }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showCustomInput) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it.filter { c -> c.isDigit() }.take(3) },
                        placeholder = { Text("Enter minutes") }, singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                    Button(onClick = {
                        val v = customText.toIntOrNull()
                        if (v != null && v >= range.start.toInt() && v <= range.endInclusive.toInt()) {
                            onValueChange(v)
                            showCustomInput = false
                        }
                    }, shape = RoundedCornerShape(10.dp)) { Text("Set") }
                }
            }
            SlimSlider(
                value = value.toFloat(),
                onValueChange = {
                    onValueChange(
                        it.roundToInt().coerceIn(range.start.toInt(), range.endInclusive.toInt()),
                    )
                },
                valueRange = range,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                activeColor = accent, inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val presets = if (range.endInclusive <= 60f) {
                    listOf(1, 5, 15, 30)
                } else {
                    listOf(1, 25, 45, 60, 90)
                }
                presets.forEach { preset ->
                    val isSel = value == preset
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) accent else chipMutedBg)
                            .border(
                                width = if (isSel) 2.dp else 0.dp,
                                color = if (isSel) accent else Color.Transparent,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable { onValueChange(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            "${preset}m",
                            fontSize = 14.sp,
                            color = if (isSel) (if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onPrimary) else chipMutedText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun EkagraGuideDialog(onDismiss: () -> Unit, accent: Color) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.SemiBold) }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ekagra Mode Detailed Guide", fontWeight = FontWeight.Bold)
                Text(
                    "Har major control ka kaam aur practical usage steps.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("What changed (quick recap)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        GuideBullet("Goals stay in Goals. Ekagra only links to them while you focus.")
                        GuideBullet("Saved sessions appear in History and Analytics.")
                        GuideBullet("When saving to a goal, Ekagra adds focus time; complete the goal only when you choose to.")
                        GuideBullet("Timer analytics now uses actual timer time used by the session.")
                    }
                }

                GuideSection(
                    title = "1. How to start a session",
                    bullets = listOf(
                        "Start first, then save the finished timer as Free Focus or Goal Focus.",
                        "Set the timer duration from the sidebar.",
                        "Press Start to begin the active focus session.",
                    ),
                )
                GuideSection(
                    title = "2. Timer controls",
                    bullets = listOf(
                        "Start / Pause: begins or pauses the current session.",
                        "Reset: resets the current timer cycle.",
                        "Modes: switch between focus, short break, and long break.",
                        "End Session: finishes the session and saves it to focus history.",
                        "PiP: opens a mini timer window while you work elsewhere.",
                    ),
                )
                GuideSection(
                    title = "3. History and Sessions",
                    bullets = listOf(
                        "Recovery: unfinished local timers can be resumed, saved, or discarded.",
                        "Reload safety: if you pause and reload, your session stays safe under Sessions.",
                        "Resume: reopens the same paused session and starts the timer again.",
                        "Delete: removes a live session completely.",
                        "History: shows today's saved focus work, split into Goal Focus and Free Focus.",
                    ),
                )
                GuideSection(
                    title = "4. Linked goal behaviour",
                    bullets = listOf(
                        "The linked goal appears at the top so you always know what you are working on.",
                        "The goal stays inside Goals; Ekagra only links to it.",
                        "Unlink removes the connection without deleting the goal.",
                        "When the linked session ends, the goal is completed and the banner clears.",
                    ),
                )
                GuideSection(
                    title = "5. Audio and environment",
                    bullets = listOf(
                        "Theme changes the visual background of your focus room.",
                        "Music controls are in the top toolbar.",
                        "Volume can be adjusted directly from the slider.",
                    ),
                )
                GuideSection(
                    title = "6. Recommended flow",
                    bullets = listOf(
                        "Save the timer as Free Focus, or assign it to a goal after you finish.",
                        "Set a realistic duration before you start.",
                        "Pause only when needed, then resume the same session.",
                        "End the session when you finish. It will move into today's History automatically.",
                        "Open Analytics > Focus or Sessions when you want to review patterns and logs.",
                    ),
                )
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
        Box(
            Modifier.padding(top = 7.dp).size(4.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FocusHistoryTab(
    modifier: Modifier,
    analytics: EkagraAnalyticsStats,
    accent: Color,
) {
    val todaySessions = analytics.focusSessions.filter { isTodayIso(it.endedAt ?: it.startedAt) }
    val linkedSessions = todaySessions.filter { !it.associatedGoalId.isNullOrBlank() && it.associatedGoalId?.startsWith("named:") != true }
    val freeFocusSessions = todaySessions.filterNot { !it.associatedGoalId.isNullOrBlank() && it.associatedGoalId?.startsWith("named:") != true }
    val todayFocusMinutes = todaySessions.sumOf { it.actualMinutes }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (todaySessions.isEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(40.dp))
                    Text("No focus sessions today.", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Column
        }

        Column(Modifier.fillMaxWidth()) {
            Text("Today's Focus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Today only.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatMinutes(todayFocusMinutes), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = accent)
                Text("Total Focus Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HistorySection(
            title = "Goal Focus",
            subtitle = "Today's goal-linked focus sessions.",
            sessions = linkedSessions,
            accent = accent,
            emptyText = "No linked sessions today.",
        )

        HistorySection(
            title = "Free Focus",
            subtitle = "Saved focus sessions without a goal.",
            sessions = freeFocusSessions,
            accent = accent,
            emptyText = "No free focus today.",
        )
    }
}

@Composable
private fun HistorySection(
    title: String,
    subtitle: String,
    sessions: List<com.safar.app.domain.model.EkagraAnalyticsFocusSession>,
    accent: Color,
    emptyText: String,
) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (sessions.isEmpty()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)).padding(12.dp)) {
                    Text(emptyText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                sessions.forEach { session ->
                    FocusSessionRow(
                        title = session.taskText ?: "Unlabeled task",
                        meta = "Planned ${session.durationMinutes}m | Actual ${session.actualMinutes}m",
                        trailing = formatTime(session.endedAt ?: session.startedAt),
                        accent = accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusSessionRow(title: String, meta: String, accent: Color, trailing: String? = null, badge: String? = null) {
    Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    if (!badge.isNullOrBlank()) {
                        val badgeColor = if (badge == "Linked") Color(0xFF7C3AED) else Color(0xFF2563EB)
                        Text(
                            badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor,
                            modifier = Modifier.clip(RoundedCornerShape(50.dp))
                                .background(badgeColor.copy(alpha = 0.10f))
                                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!trailing.isNullOrBlank()) Text(trailing, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

private fun formatMinutes(min: Int): String = when {
    min <= 0  -> "0m"
    min < 60  -> "${min}m"
    else      -> "${min / 60}h ${min % 60}m".trimEnd().let { if (it.endsWith("0m")) it.dropLast(2).trim() else it }
}

private fun formatDate(iso: String): String = runCatching {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val date = sdf.parse(iso) ?: return@runCatching iso
    SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
}.getOrElse { iso }

private fun parseInstantOrNull(iso: String?): Instant? = iso?.let { value ->
    runCatching { Instant.parse(value) }.getOrNull()
}

private fun isTodayIso(iso: String?): Boolean {
    val zone = ZoneId.systemDefault()
    val date = parseInstantOrNull(iso)?.atZone(zone)?.toLocalDate() ?: return false
    return date == java.time.LocalDate.now(zone)
}

private fun formatDateTime(iso: String?): String {
    val zone = ZoneId.systemDefault()
    val dateTime = parseInstantOrNull(iso)?.atZone(zone) ?: return "-"
    return java.time.format.DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault()).format(dateTime)
}

private fun formatTime(iso: String?): String {
    val zone = ZoneId.systemDefault()
    val dateTime = parseInstantOrNull(iso)?.atZone(zone) ?: return "-"
    return java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(dateTime)
}

private fun TimerMode.toApiMode(): String = when (this) {
    TimerMode.FOCUS -> "Timer"
    TimerMode.BREAK -> "short"
    TimerMode.LONG_BREAK -> "long"
}

private fun String.toTimerMode(): TimerMode = when (this) {
    "short" -> TimerMode.BREAK
    "long" -> TimerMode.LONG_BREAK
    else -> TimerMode.FOCUS
}

private fun sessionModeLabel(mode: String): String = when (mode.lowercase(Locale.US)) {
    "short" -> "Short break"
    "long" -> "Long break"
    else -> "Focus"
}

@Composable
private fun VisualThemeDialog(current: VisualTheme, onSelect: (VisualTheme) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        title = { Text("Choose Visual Theme", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visualThemes.forEach { theme ->
                    val isSelected = theme.name == current.name
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) theme.accent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .then(if (isSelected) Modifier.border(2.dp, theme.accent, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { onSelect(theme) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(theme.accent), contentAlignment = Alignment.Center) { Text(theme.emoji, fontSize = 18.sp) }
                        Text(theme.name, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        if (isSelected) { Spacer(Modifier.weight(1f)); Icon(Icons.Default.Check, contentDescription = null, tint = theme.accent, modifier = Modifier.size(18.dp)) }
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ambient Sound", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            focusMusicTracks.forEach { (name, _) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (current == name) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent)
                        .clickable { onSelect(name) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (current == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    Text(name, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (current == name) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrganizeFreeFocusSheet(
    pending: PendingEndedEkagraSession?,
    goals: List<com.safar.app.domain.model.Goal>,
    titleInput: String,
    onTitleChange: (String) -> Unit,
    accent: Color,
    onDismiss: () -> Unit,
    onSaveFree: () -> Unit,
    onLinkGoal: (com.safar.app.domain.model.Goal) -> Unit,
    onDiscard: () -> Unit,
) {
    val focusedMinutes = ((pending?.totalSeconds ?: 0) - (pending?.secondsLeft ?: 0)).coerceAtLeast(60) / 60
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("You focused for ${focusedMinutes} min.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("What were you working on?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text("Link to existing goal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (goals.isEmpty()) {
                Text("No open goals available.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                goals.take(5).forEach { goal ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onLinkGoal(goal) }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Text(goal.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                    }
                }
            }

            OutlinedTextField(
                value = titleInput,
                onValueChange = onTitleChange,
                placeholder = { Text("Add title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("Discard", fontSize = 12.sp)
                }
                Button(onClick = onSaveFree, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                    Text("Save Free Focus", fontSize = 12.sp)
                }
            }
        }
    }
}

private data class PendingEndedEkagraSession(
    val sessionId: String,
    val totalSeconds: Int,
    val secondsLeft: Int,
    val mode: String,
    val startedAt: String?,
)
