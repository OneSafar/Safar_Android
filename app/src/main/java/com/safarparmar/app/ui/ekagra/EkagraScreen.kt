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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkagraScreen(
    currentRoute: String = Routes.EKAGRA,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleNightMode: () -> Unit = {},
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
        val initialTheme = visualThemes.getOrElse(prefs.getInt("theme_index", -1)) { visualThemes[0] }
        mutableStateOf(prefs.getString("song_name", null) ?: focusMusicTracks.firstOrNull { it.themeId.equals(initialTheme.name, ignoreCase = true) }?.name ?: "Silence")
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
        selectedSong == "Silence"       -> "silence"
        else -> focusMusicTracks.firstOrNull { it.name == selectedSong }?.url ?: ""
    }

    // ── Side-effects ────────────────────────────────────────────────────────────

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedTab) {
        if (selectedTab == EkagraNavTab.TIMER && timerService?.isActive() == false)
            timerService.setDuration(TimerMode.FOCUS, focusMinutes * 60)
    }
    LaunchedEffect(initialView) {
        if (initialView == "analytics") onNavigate(Routes.nishthaAnalytics("ekagra"))
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
            builder.setTitle("SAFAR Ekagra Timer"); builder.setSubtitle("Ekagra timer running")
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
    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.75f, 1.25f)
        )
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
                        VisualThemeDialog(current = selectedTheme, onSelect = { 
                            selectedTheme = it
                            val newThemeSongs = focusMusicTracks.filter { track -> track.themeId.equals(it.name, ignoreCase = true) }
                            if (newThemeSongs.isNotEmpty()) {
                                selectedSong = newThemeSongs.first().name
                            }
                            showThemeDialog = false 
                        }, onDismiss = { showThemeDialog = false })
                    if (showSongSheet)
                        SongPickerSheet(currentThemeId = selectedTheme.name, current = selectedSong, onSelect = { selectedSong = it; showSongSheet = false }, onDismiss = { showSongSheet = false })
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
                                        titleInput.ifBlank { "Free Ekagra" }, null, null)
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
                            // Top-bar text is always white when timer tab is showing (video bg)
                            topBarContentColor = if (selectedTab == EkagraNavTab.TIMER) Color.White
                                                 else MaterialTheme.colorScheme.onSurface,
                            topBarActions = {
                                val tintColor = if (selectedTab == EkagraNavTab.TIMER) Color.White
                                                else MaterialTheme.colorScheme.onSurface
                                IconButton(onClick = { showThemeDialog = true }) {
                                    Icon(Icons.Default.Palette, contentDescription = "Theme", tint = tintColor)
                                }
                                IconButton(onClick = { showSongSheet = true }) {
                                    Icon(Icons.Default.MusicNote, contentDescription = "Sound", tint = tintColor)
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
                                        shieldState   = shieldState,
                                        isDarkTheme   = isDarkTheme,
                                        themeAccent   = themeColorScheme.primary,
                                        onToggleKavach = focusShieldViewModel::setEnabled,
                                        onOpenAppPicker = { onNavigate(Routes.APP_PICKER) },
                                        onNavigate = onNavigate,
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
