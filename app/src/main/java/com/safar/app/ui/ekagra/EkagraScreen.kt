package com.safar.app.ui.ekagra

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.safar.app.R
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.data.remote.dto.RecentSession
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.nishtha.checkin.SlimSlider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.media.MediaPlayer
import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Rational
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.MutableStateFlow
import android.view.TextureView


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
    BREAK    (Icons.Default.FreeBreakfast,  "Break"),
    DURATION (Icons.Default.Tune,           "Duration"),
    ANALYTICS(Icons.Default.BarChart,       "Analytics"),
}


enum class TimerMode(val icon: ImageVector, val label: String, val showInPill: Boolean = true) {
    FOCUS      (Icons.Default.Timer,         "Focus",      true),
    BREAK      (Icons.Default.FreeBreakfast, "Break",      true),
    LONG_BREAK (Icons.Default.Hotel,         "Long Break", true),
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkagraScreen(
    currentRoute: String = Routes.EKAGRA,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleNightMode: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    viewModel: EkagraViewModel = hiltViewModel(),
) {
    val statsState by viewModel.stats.collectAsStateWithLifecycle()

    val timerService = LocalTimerService.current
    val context      = LocalContext.current

    val secondsLeft  by (timerService?.secondsLeft  ?: MutableStateFlow(25 * 60)).collectAsState()
    val totalSeconds by (timerService?.totalSeconds ?: MutableStateFlow(25 * 60)).collectAsState()
    val timerRunning by (timerService?.isRunning    ?: MutableStateFlow(false)).collectAsState()
    val timerMode    by (timerService?.timerMode    ?: MutableStateFlow(TimerMode.FOCUS)).collectAsState()

    var selectedTab      by remember { mutableStateOf(EkagraNavTab.TIMER) }
    var showThemeDialog  by remember { mutableStateOf(false) }
    var showSongSheet    by remember { mutableStateOf(false) }
    var tourState        by remember { mutableStateOf<com.safar.app.ui.butterfly.ButterflyTourState?>(null) }
    var showAddTask      by remember { mutableStateOf(false) }
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
    var taskText         by remember { mutableStateOf("") }

    var focusMinutes by remember { mutableStateOf(25) }
    var breakMinutes by remember { mutableStateOf(5) }

    fun startTimer(mode: TimerMode, minutes: Int) {
        timerService?.saveTheme(visualThemes.indexOf(selectedTheme), selectedSong)
        timerService?.setDuration(mode, minutes * 60)
        timerService?.start()
        if (mode == TimerMode.FOCUS) {
            viewModel.onSessionStarted(
                taskText     = taskText,
                totalSeconds = minutes * 60,
            )
        }
    }

    fun resetTimer() {
        if (timerMode == TimerMode.FOCUS) {
            viewModel.onSessionStopped(
                totalSeconds = totalSeconds,
                secondsLeft  = secondsLeft,
            )
        }
        timerService?.reset()
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            EkagraNavTab.TIMER -> {
                if (timerService?.isActive() == false)
                    timerService?.setDuration(TimerMode.FOCUS, focusMinutes * 60)
            }
            EkagraNavTab.BREAK -> {
                if (timerService?.isActive() == false)
                    timerService?.setDuration(TimerMode.BREAK, breakMinutes * 60)
            }
            else -> {}
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
    var isInPipMode by remember { mutableStateOf(false) }

    val ACTION_PIP_PLAY_PAUSE = "com.safar.ekagra.PIP_PLAY_PAUSE"
    val ACTION_PIP_RESET      = "com.safar.ekagra.PIP_RESET"
    val PIP_REQUEST_PLAY      = 1
    val PIP_REQUEST_RESET     = 2

    fun buildPipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setSeamlessResizeEnabled(true)
            builder.setAutoEnterEnabled(timerRunning)
        }
        val playPauseIcon = android.graphics.drawable.Icon.createWithResource(
            pipContext, if (timerRunning) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
        val resetIcon = android.graphics.drawable.Icon.createWithResource(pipContext, android.R.drawable.ic_menu_revert)
        builder.setActions(
            listOf(
                RemoteAction(
                    playPauseIcon,
                    if (timerRunning) "Pause" else "Play",
                    if (timerRunning) "Pause timer" else "Start timer",
                    android.app.PendingIntent.getBroadcast(
                        pipContext, PIP_REQUEST_PLAY, Intent(ACTION_PIP_PLAY_PAUSE),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                ),
                RemoteAction(
                    resetIcon, "Reset", "Reset timer",
                    android.app.PendingIntent.getBroadcast(
                        pipContext, PIP_REQUEST_RESET, Intent(ACTION_PIP_RESET),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                ),
            )
        )
        return builder.build()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                isInPipMode = pipActivity?.isInPictureInPictureMode == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(timerRunning, secondsLeft) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { buildPipParams()?.let { pipActivity?.setPictureInPictureParams(it) } } catch (_: Exception) {}
        }
        if (!timerRunning && secondsLeft == 0 && totalSeconds > 0 && timerMode == TimerMode.FOCUS) {
            viewModel.onSessionCompleted(
                totalSeconds = totalSeconds,
                secondsLeft  = 0,
            )
        }
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PIP_PLAY_PAUSE -> timerService?.togglePlayPause()
                    ACTION_PIP_RESET      -> resetTimer()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_RESET)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pipContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            pipContext.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { pipContext.unregisterReceiver(receiver) } }
    }

    val accent    = selectedTheme.accent
    val mottoText = if (timerRunning) "STAY FOCUSED, YOU'RE DOING GREAT!" else "READY TO FOCUS?"
    val progress  = if (totalSeconds > 0) 1f - secondsLeft.toFloat() / totalSeconds else 0f

    if (isInPipMode) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                    fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-2).sp,
                )
                Box(Modifier.fillMaxWidth(0.8f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.2f))) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(accent))
                }
                Text(
                    if (timerRunning) "FOCUSING" else "PAUSED",
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Color.White.copy(0.6f),
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
    if (showAddTask) {
        AddTaskDialog(
            current   = taskText,
            onConfirm = { taskText = it; showAddTask = false; if (it.isNotBlank()) viewModel.createTaskAsGoal(it) },
            onDismiss = { showAddTask = false },
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
            topBarActions     = {
                IconButton(onClick = { tourState?.start() }) { Icon(Icons.Default.HelpOutline, contentDescription = "Guide") }
                IconButton(onClick = { showThemeDialog = true }) { Icon(Icons.Default.Palette, contentDescription = "Theme") }
                IconButton(onClick = { showSongSheet = true }) { Icon(Icons.Default.MusicNote, contentDescription = "Song") }
            },
        ) { padding ->

            val isTimerTab = selectedTab == EkagraNavTab.TIMER || selectedTab == EkagraNavTab.BREAK
            if (isTimerTab) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.38f)))
                EkagraVideoBackground(videoUrl = selectedTheme.videoUrl, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.38f)))
            }

            Box(Modifier.fillMaxSize()) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                            EkagraNavTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick  = {
                                        selectedTab = tab
                                        if (tab == EkagraNavTab.ANALYTICS) viewModel.loadStats()
                                    },
                                    icon  = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(tab.label, fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal) },
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

                    when (selectedTab) {

                        EkagraNavTab.TIMER, EkagraNavTab.BREAK -> {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.fillMaxSize().padding(bottom = innerPadding.calculateBottomPadding())) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Spacer(Modifier.weight(1f))

                                        GlassTimerCard(
                                            timerMode    = timerMode,
                                            secondsLeft  = secondsLeft,
                                            isRunning    = timerRunning,
                                            timerActive  = timerRunning || secondsLeft < totalSeconds,
                                            taskText     = taskText,
                                            accent       = accent,
                                            onModeChange = { mode ->
                                                val mins = when (mode) {
                                                    TimerMode.FOCUS       -> focusMinutes
                                                    TimerMode.BREAK       -> breakMinutes
                                                    TimerMode.LONG_BREAK  -> 15
                                                }
                                                timerService?.setDuration(mode, mins * 60)
                                            },
                                            onPlayPause  = {
                                                val wasInactive = timerService?.isActive() == false
                                                timerService?.togglePlayPause()
                                                if (wasInactive && timerMode == TimerMode.FOCUS) {
                                                    viewModel.onSessionStarted(taskText, totalSeconds)
                                                }
                                            },
                                            onReset      = { resetTimer() },
                                            onStop       = { resetTimer() },
                                            onAddTask    = { showAddTask = true },
                                            modifier     = Modifier.padding(horizontal = 20.dp),
                                        )

                                        Spacer(Modifier.weight(1f))

                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(mottoText, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.5.sp, color = Color.White.copy(alpha = 0.85f))
                                            Spacer(Modifier.height(14.dp))

                                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                                val bugSize        = 20.dp
                                                val barHeight      = 4.dp
                                                val totalWidth     = maxWidth
                                                val clampedProgress = progress.coerceIn(0f, 1f)
                                                val bugOffsetX     = (totalWidth - bugSize) * clampedProgress

                                                Box(Modifier.fillMaxWidth().height(barHeight).align(Alignment.Center)
                                                    .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.15f)))
                                                if (clampedProgress > 0f) {
                                                    Box(Modifier.fillMaxWidth(clampedProgress).height(barHeight)
                                                        .align(Alignment.CenterStart).clip(RoundedCornerShape(2.dp)).background(accent))
                                                }
                                                AsyncImage(
                                                    model = "https://cdn-icons-png.flaticon.com/512/616/616408.png",
                                                    contentDescription = "progress bug",
                                                    modifier = Modifier.size(bugSize).offset(x = bugOffsetX).align(Alignment.CenterStart),
                                                    contentScale = ContentScale.Fit,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        EkagraNavTab.DURATION -> {
                            DurationTab(
                                modifier      = Modifier.padding(top = padding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
                                focusMinutes  = focusMinutes,
                                breakMinutes  = breakMinutes,
                                accent        = accent,
                                onFocusChange = { focusMinutes = it },
                                onBreakChange = { breakMinutes = it },
                                onStartFocus  = { startTimer(TimerMode.FOCUS, focusMinutes); selectedTab = EkagraNavTab.TIMER },
                                onStartBreak  = { startTimer(TimerMode.BREAK, breakMinutes); selectedTab = EkagraNavTab.BREAK },
                            )
                        }

                        EkagraNavTab.ANALYTICS -> {
                            AnalyticsTab(
                                modifier        = Modifier.padding(top = padding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
                                statsState      = statsState,
                                accent          = accent,
                                onRefresh       = { viewModel.loadStats() },
                                onStartFocus25  = { focusMinutes = 25; startTimer(TimerMode.FOCUS, 25); selectedTab = EkagraNavTab.TIMER },
                                onStartFocus50  = { focusMinutes = 50; startTimer(TimerMode.FOCUS, 50); selectedTab = EkagraNavTab.TIMER },
                                onStartBreak5   = { breakMinutes = 5;  startTimer(TimerMode.BREAK, 5);  selectedTab = EkagraNavTab.BREAK },
                                onStartBreak15  = { breakMinutes = 15; startTimer(TimerMode.BREAK, 15); selectedTab = EkagraNavTab.BREAK },
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


@Composable
private fun GlassTimerCard(
    timerMode: TimerMode,
    secondsLeft: Int,
    isRunning: Boolean,
    timerActive: Boolean,
    taskText: String,
    accent: Color,
    onModeChange: (TimerMode) -> Unit,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onStop: () -> Unit,
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(28.dp))
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ModePill(selected = timerMode, accent = accent, onSelect = onModeChange)

            Text(
                "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                fontSize = 80.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.92f), letterSpacing = (-2).sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )


            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onReset() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White.copy(alpha = 0.8f))
                }
                Box(
                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(accent).clickable { onPlayPause() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp),
                    )
                }
                if (timerActive) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(CircleShape)
                            .background(Color(0xFFE53935))
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}


@Composable
private fun ModePill(selected: TimerMode, accent: Color, onSelect: (TimerMode) -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
            .padding(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimerMode.entries.filter { it.showInPill }.forEach { mode ->
                val isSelected = mode == selected
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(if (isSelected) accent else Color.Transparent)
                        .clickable { onSelect(mode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        mode.icon, contentDescription = mode.label,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(22.dp),
                    )
                }
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
    onStartFocus: () -> Unit,
    onStartBreak: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Duration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Customize your sessions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        DurationCard(icon = Icons.Default.Timer, title = "Focus", value = focusMinutes, range = 5f..120f, accent = accent, onValueChange = onFocusChange)
        DurationCard(icon = Icons.Default.FreeBreakfast, title = "Break", value = breakMinutes, range = 1f..60f, accent = accent, onValueChange = onBreakChange)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onStartFocus, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Timer, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("Focus", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = onStartBreak, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.FreeBreakfast, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text("Break")
            }
        }
    }
}

@Composable
private fun DurationCard(
    icon: ImageVector,
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onValueChange: (Int) -> Unit,
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customText      by remember { mutableStateOf("") }

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text("${value}m", fontWeight = FontWeight.Bold, color = accent, fontSize = 15.sp)
                IconButton(onClick = { showCustomInput = !showCustomInput; customText = "" }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showCustomInput) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { customText = it.filter { c -> c.isDigit() }.take(3) },
                        placeholder = { Text("min") }, singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                    Button(onClick = {
                        val v = customText.toIntOrNull()
                        if (v != null && v >= range.start.toInt() && v <= range.endInclusive.toInt()) { onValueChange(v); showCustomInput = false }
                    }, shape = RoundedCornerShape(10.dp)) { Text("Set") }
                }
            }
            SlimSlider(
                value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) }, valueRange = range,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                activeColor = accent, inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val presets = when { range.endInclusive <= 30f -> listOf(5, 10, 15, 20); range.endInclusive <= 60f -> listOf(5, 15, 30, 45); else -> listOf(15, 25, 45, 60) }
                presets.forEach { preset ->
                    val isSel = value == preset
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSel) accent else accent.copy(0.08f)).clickable { onValueChange(preset) }.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text("${preset}m", fontSize = 12.sp, color = if (isSel) Color.White else accent, fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}


@Composable
private fun AnalyticsTab(
    modifier: Modifier,
    statsState: StatsUiState,
    accent: Color,
    onRefresh: () -> Unit,
    onStartFocus25: () -> Unit,
    onStartFocus50: () -> Unit,
    onStartBreak5: () -> Unit,
    onStartBreak15: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Focus Analytics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Your productivity insights", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onRefresh, shape = RoundedCornerShape(20.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Text("Refresh", fontSize = 12.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onStartFocus25, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = accent), modifier = Modifier.weight(1f)) {
                Text("Start 25m Focus", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onStartFocus50, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.weight(1f)) {
                Text("Start 50m Focus", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onStartBreak5, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("5m Break", fontSize = 13.sp) }
            OutlinedButton(onClick = onStartBreak15, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) { Text("15m Long Break", fontSize = 13.sp) }
        }

        when (val s = statsState) {
            is StatsUiState.Loading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
            is StatsUiState.Error   -> {
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) { Text("Retry") }
            }
            is StatsUiState.Success -> AnalyticsContent(data = s.data, accent = accent)
        }
    }
}

@Composable
private fun AnalyticsContent(data: FocusStatsResponse, accent: Color) {
    val abandonedSessions = data.totalSessions - data.completedSessions
    val sessionQuality    = if (data.totalSessions > 0) (data.completedSessions * 100 / data.totalSessions) else 0
    val dailyProgressMin  = (data.dailyGoalProgress * data.dailyGoalMinutes / 100f).toInt()

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(icon = Icons.Default.Bolt, label = "TOTAL FOCUS", value = formatMinutes(data.totalFocusMinutes), sub = "+ ${formatMinutes(data.totalBreakMinutes)} breaks", accent = accent, modifier = Modifier.weight(1f))
        StatCard(icon = Icons.Default.Check, label = "QUALITY", value = "$sessionQuality%", sub = "${data.completedSessions} done", accent = accent, modifier = Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(icon = Icons.Default.LocalFireDepartment, label = "STREAK", value = "${data.focusStreak}d", sub = if (data.focusStreak == 0) "Keep going" else "🔥 On a roll!", accent = accent, modifier = Modifier.weight(1f))
        StatCard(icon = Icons.Default.TrackChanges, label = "DAILY GOAL", value = "${data.dailyGoalProgress}%", sub = "${dailyProgressMin}m / ${data.dailyGoalMinutes}m", accent = accent, modifier = Modifier.weight(1f))
    }

    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Today's Focus Goal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${dailyProgressMin}m / ${data.dailyGoalMinutes}m", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { data.dailyGoalProgress / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = accent, trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
        }
    }

    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Weekly Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Focus vs break time per day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(color = accent, label = "FOCUS")
                    LegendDot(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), label = "BREAK")
                }
            }
            WeeklyBarChart(weeklyData = data.weeklyData, weeklyBreaks = data.weeklyBreaks, accent = accent)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Week total", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatMinutes(data.weeklyData.sum())} focus", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }
    }

    val peakHour  = data.hourlyDistribution.indexOfMax()
    val peakValue = data.hourlyDistribution.getOrElse(peakHour) { 0 }
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("When You Focus Best", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Hourly distribution across all sessions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HourlyChart(distribution = data.hourlyDistribution, accent = accent)
            if (peakValue > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("⚡", fontSize = 14.sp)
                    Text("Peak focus: ${formatHour(peakHour)} · ${formatMinutes(peakValue)} that hour", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
                }
            }
        }
    }

    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text("Goals Progress", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("How focus sessions align with goal completion", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Goals Completed", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${data.goalsCompleted} / ${data.goalsSet}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accent)
            }
            LinearProgressIndicator(progress = { if (data.goalsSet > 0) data.goalsCompleted.toFloat() / data.goalsSet else 0f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = accent, trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            val pct = if (data.goalsSet > 0) data.goalsCompleted * 100 / data.goalsSet else 0
            Text("$pct% complete · ${data.goalsSet - data.goalsCompleted} remaining", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text("Session Quality", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("Completion breakdown across all sessions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                DonutChart(pct = sessionQuality, accent = accent)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendRow(color = accent, label = "${data.completedSessions} completed")
                    LegendRow(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.35f), label = "${abandonedSessions} abandoned")
                }
            }
            if (sessionQuality >= 80) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.08f)).padding(10.dp)) {
                    Text("🔥 Excellent consistency — keep this up!", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
                }
            }
        }
    }

    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                    Text("Recent Sessions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text("Last ${data.recentSessions.size} sessions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            if (data.recentSessions.isEmpty()) {
                Text("No sessions yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            } else {
                data.recentSessions.forEach { session -> RecentSessionRow(session = session, accent = accent) }
            }
        }
    }
}


@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, sub: String, accent: Color, modifier: Modifier = Modifier) {
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
private fun WeeklyBarChart(weeklyData: List<Int>, weeklyBreaks: List<Int>, accent: Color) {
    val days       = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxVal     = (weeklyData + weeklyBreaks).maxOrNull()?.takeIf { it > 0 } ?: 1
    val breakColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val today      = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { i, day ->
            val focusH  = weeklyData.getOrElse(i) { 0 }.toFloat() / maxVal
            val breakH  = weeklyBreaks.getOrElse(i) { 0 }.toFloat() / maxVal
            val isToday = i == today
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
                    if (breakH > 0) Box(Modifier.fillMaxWidth(0.55f).fillMaxHeight(breakH).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(breakColor))
                    if (focusH > 0) Box(Modifier.fillMaxWidth(0.55f).fillMaxHeight(focusH.coerceAtLeast(0.04f)).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(topStart = if (breakH == 0f) 3.dp else 0.dp, topEnd = if (breakH == 0f) 3.dp else 0.dp, bottomStart = 3.dp, bottomEnd = 3.dp)).background(accent))
                }
                Spacer(Modifier.height(4.dp))
                Text(day, fontSize = 10.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = if (isToday) accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
private fun HourlyChart(distribution: List<Int>, accent: Color) {
    val maxVal = distribution.maxOrNull()?.takeIf { it > 0 } ?: 1
    val labels = listOf("12am", "6am", "12pm", "5pm", "11pm")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            distribution.forEach { value ->
                val h = value.toFloat() / maxVal
                Box(Modifier.weight(1f).fillMaxHeight(h.coerceAtLeast(if (value > 0) 0.06f else 0f)).padding(horizontal = 1.dp).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(if (value > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}


@Composable
private fun DonutChart(pct: Int, accent: Color) {
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = accent, startAngle = -90f, sweepAngle = 360f * pct / 100f, useCenter = false, style = stroke)
        }
        Text("$pct%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
    }
}


@Composable
private fun RecentSessionRow(session: RecentSession, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(if (session.completed) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, tint = if (session.completed) accent else MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(session.taskText ?: "No task set", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(formatDate(session.startedAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatMinutes(session.actualMinutes), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}


@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 13.sp)
    }
}

private fun List<Int>.indexOfMax(): Int = indices.maxByOrNull { this[it] } ?: 0

private fun formatMinutes(min: Int): String = when {
    min <= 0  -> "0m"
    min < 60  -> "${min}m"
    else      -> "${min / 60}h ${min % 60}m".trimEnd().let { if (it.endsWith("0m")) it.dropLast(2).trim() else it }
}

private fun formatHour(h: Int): String = when {
    h == 0  -> "12am"
    h < 12  -> "${h}am"
    h == 12 -> "12pm"
    else    -> "${h - 12}pm"
}

private fun formatDate(iso: String): String = runCatching {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val date = sdf.parse(iso) ?: return@runCatching iso
    SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
}.getOrElse { iso }


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

@Composable
private fun AddTaskDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What are you working on?", fontWeight = FontWeight.Bold) },
        text  = {
            OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("Enter task...") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
        },
        confirmButton = { Button(onClick = { onConfirm(text) }, shape = RoundedCornerShape(10.dp)) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}



