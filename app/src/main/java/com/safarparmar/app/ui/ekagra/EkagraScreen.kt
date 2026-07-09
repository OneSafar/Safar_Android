package com.safarparmar.app.ui.ekagra

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.components.rememberFeatureTabBackStack
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import com.safarparmar.app.util.IstDateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTimerService = staticCompositionLocalOf<TimerService?> { null }

private fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
    val r = color1.red * ratio + color2.red * (1f - ratio)
    val g = color1.green * ratio + color2.green * (1f - ratio)
    val b = color1.blue * ratio + color2.blue * (1f - ratio)
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
}

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
    val haptics              = LocalHapticFeedback.current

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
    val isMuted           by (timerService?.isMuted            ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val blockedHitCount   by focusShieldViewModel.blockedHitCount.collectAsStateWithLifecycle()

    // UI state
    val tabBackStack             = rememberFeatureTabBackStack(EkagraNavTab.TIMER)
    val selectedTab              = tabBackStack.currentTab
    var showKavachActiveSession  by remember { mutableStateOf(false) }
    var showKavachSessionSummary by remember { mutableStateOf(false) }
    var kavachSummaryMinutes     by remember { mutableIntStateOf(0) }
    var kavachSummaryAttempts    by remember { mutableStateOf<List<com.safarparmar.app.ui.ekagra.focusshield.KavachBlockedAttempt>>(emptyList()) }
    var showThemeDialog          by remember { mutableStateOf(false) }
    var showAudioLibraryPanel    by remember { mutableStateOf(false) }
    var showOrganizeSheet        by remember { mutableStateOf(false) }
    var pendingEndedSession      by remember { mutableStateOf<PendingEndedEkagraSession?>(null) }
    var titleInput               by remember { mutableStateOf("") }
    val showDurationPrompt          by viewModel.showDurationPrompt.collectAsStateWithLifecycle()
    var showDurationPromptDialog    by remember { mutableStateOf(false) }
    var dontShowDurationPromptAgain by remember { mutableStateOf(false) }
    // True once the user has responded to the duration prompt in this session.
    // Prevents the dialog from re-appearing every time Start is tapped after
    // the user has already visited the Duration tab via the prompt.
    var durationPromptActedOn       by remember { mutableStateOf(false) }
    var tourState                by remember { mutableStateOf<com.safarparmar.app.ui.butterfly.ButterflyTourState?>(null) }
    val timerImmersiveActive = false

    BackHandler(enabled = tabBackStack.hasHistory) {
        tabBackStack.goBack()
    }

    LaunchedEffect(tourState?.isVisible, tourState?.currentStepIndex) {
        val state = tourState ?: return@LaunchedEffect
        if (!state.isVisible) return@LaunchedEffect
        when (state.currentStepIndex) {
            0, 1 -> tabBackStack.select(EkagraNavTab.TIMER)
            2 -> tabBackStack.select(EkagraNavTab.DURATION)
        }
    }

    var selectedTheme by remember {
        val prefs = context.getSharedPreferences("ekagra_theme_prefs", android.content.Context.MODE_PRIVATE)
        mutableStateOf(visualThemes.getOrElse(prefs.getInt("theme_index", -1)) { visualThemes[0] })
    }
    var selectedMusicTrack by remember {
        mutableStateOf(com.safarparmar.app.ui.audio.AudioLibrary.getPersistedTrack(context))
    }

    var showMusicPromptDialog by remember { mutableStateOf(false) }
    var pendingStartTimerArgs by remember { mutableStateOf<Pair<TimerMode, Int>?>(null) }
    var isStartingFromMusicSelection by remember { mutableStateOf(false) }

    val musicPromptPrefs = remember {
        context.getSharedPreferences("ekagra_music_prompt_prefs", android.content.Context.MODE_PRIVATE)
    }
    var skipMusicPrompt by remember {
        mutableStateOf(musicPromptPrefs.getBoolean("skip_prompt", false))
    }

    var associatedGoalId    by remember(linkedGoalId)    { mutableStateOf(linkedGoalId) }
    var associatedGoalTitle by remember(linkedGoalTitle) { mutableStateOf(linkedGoalTitle) }
    var taskText            by remember(linkedGoalId, linkedGoalTitle) { mutableStateOf(linkedGoalTitle.orEmpty()) }
    var focusMinutes        by remember { mutableIntStateOf(25) }
    var breakMinutes        by remember { mutableIntStateOf(5) }
    var countdownValue      by remember { mutableIntStateOf(0) }
    var longBreakMinutes    by remember { mutableIntStateOf(15) }
    val autoStartBreak      by viewModel.dataStore.autoStartBreak.collectAsStateWithLifecycle(initialValue = true)

    // ── Helpers ─────────────────────────────────────────────────────────────────

    fun startTimer(mode: TimerMode, minutes: Int) {
        if ((mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) && !skipMusicPrompt && !isStartingFromMusicSelection) {
            pendingStartTimerArgs = Pair(mode, minutes)
            showMusicPromptDialog = true
            return
        }
        if (isStartingFromMusicSelection) {
            isStartingFromMusicSelection = false
        }
        val accessibilityRequired = com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper.isAccessibilityFeatureEnabled()
        if ((mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) && shieldState.isEnabled && shieldState.blockedPackages.isNotEmpty()
            && (!shieldState.hasUsageStats || (accessibilityRequired && !shieldState.hasAccessibilityService) || !shieldState.hasNotificationSuppressionAccess)) {
            onNavigate(Routes.FOCUS_SHIELD); return
        }
        requestNotificationPermission()
        timerService?.saveTheme(visualThemes.indexOf(selectedTheme), selectedMusicTrack.name)

        val service = timerService
        if ((mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) && timerMode != TimerMode.FOCUS && timerMode != TimerMode.STOPWATCH && timerMode != TimerMode.POMODORO && service?.switchToFocusFromBreak() == true) {
            service.prepareAutoSaveSession(
                taskTitle = associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() },
                goalId = associatedGoalId,
                goalTitle = associatedGoalTitle,
            )
            service.start()
            viewModel.syncActiveSession(
                service.totalSeconds.value,
                service.secondsLeft.value,
                service.timerMode.value.toApiMode(),
                true,
                associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() },
            )
            return
        }

        if ((mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) && (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO) && timerService?.isActive() == true) {
            timerService.prepareAutoSaveSession(
                taskTitle = associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() },
                goalId = associatedGoalId,
                goalTitle = associatedGoalTitle,
            )
            timerService.start()
            viewModel.syncActiveSession(
                totalSeconds,
                secondsLeft,
                timerMode.toApiMode(),
                true,
                associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() },
            )
            return
        }

        timerService?.setDuration(mode, minutes * 60, breakMinutes * 60)
        if (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) {
            timerService?.prepareAutoSaveSession(
                taskTitle = associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() },
                goalId = associatedGoalId,
                goalTitle = associatedGoalTitle,
                forceNew = true,
            )
        }
        timerService?.start()
        viewModel.onSessionStarted(
            taskText     = taskText,
            totalSeconds = if (mode == TimerMode.STOPWATCH) 0 else minutes * 60,
            goalId       = if (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) associatedGoalId else null,
            goalTitle    = if (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) associatedGoalTitle else null,
            mode         = mode.toApiMode(),
        )
        if ((mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) && shieldState.isEnabled && shieldState.blockedPackages.isNotEmpty()) {
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
        val service = timerService
        if (timerMode != TimerMode.FOCUS && timerMode != TimerMode.STOPWATCH && timerMode != TimerMode.POMODORO && service?.switchToFocusFromBreak() == true) {
            activeSession?.let { session ->
                viewModel.pauseActiveSession(
                    service.totalSeconds.value,
                    service.secondsLeft.value,
                    TimerMode.FOCUS.toApiMode(),
                    associatedGoalTitle ?: session.sessionTitle,
                )
            }
            return
        }
        val session = activeSession
        if (session != null && (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO)) {
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

    // ── Side-effects ────────────────────────────────────────────────────────────

    val toneGenerator = remember { 
        runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100) }.getOrNull() 
    }
    DisposableEffect(Unit) {
        onDispose { toneGenerator?.release() }
    }

    LaunchedEffect(countdownValue) {
        if (countdownValue > 0) {
            if (countdownValue <= 3) {
                runCatching { toneGenerator?.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 150) }
            }
            kotlinx.coroutines.delay(1000)
            countdownValue -= 1
            if (countdownValue == 0) {
                runCatching { toneGenerator?.startTone(android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT, 300) }
                val wasInactive = timerService?.isActive() == false
                if (wasInactive) requestNotificationPermission()
                timerService?.togglePlayPause()
                if (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO) {
                    viewModel.onSessionStarted(taskText, totalSeconds, associatedGoalId, associatedGoalTitle, timerMode.toApiMode())
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(selectedTab) {
        if (selectedTab == EkagraNavTab.TIMER && timerService?.isActive() == false)
            timerService.setDuration(TimerMode.FOCUS, focusMinutes * 60, breakMinutes * 60)
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
            if (latestSecondsLeft > 0 && (latestTimerMode == TimerMode.FOCUS || latestTimerMode == TimerMode.STOPWATCH))
                viewModel.syncActiveSession(latestTotalSeconds, latestSecondsLeft, latestTimerMode.toApiMode(),
                    true, latestGoalTitle ?: latestTaskText.takeIf { it.isNotBlank() })
        }
    }

    LaunchedEffect(selectedMusicTrack, selectedTheme) {
        timerService?.setMusic(selectedMusicTrack.url)
        if (timerService?.isActive() == true)
            timerService.saveTheme(visualThemes.indexOf(selectedTheme), selectedMusicTrack.name)
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
            // ── Pomodoro auto-break guard ────────────────────────────────────────
            // TimerService sets _isRunning=false momentarily, then immediately
            // switches to BREAK mode and calls start() — this all happens on the
            // Main dispatcher without suspension. Wait 500 ms for the service's
            // StateFlows to settle so we can tell whether an auto-break started.
            delay(500L)
            if (timerMode != TimerMode.FOCUS || timerRunning) {
                // Auto 5-minute break is now running. The service already saved the
                // completed focus session via enqueueCompletedFocusSessionSave().
                // Just discard the stale ViewModel draft and refresh analytics —
                // do NOT call timerService?.reset() or the break will be killed.
                activeSession?.id?.let { viewModel.discardSession(it) }
                viewModel.loadEkagraAnalytics()
                associatedGoalId = null
                associatedGoalTitle = null
                return@LaunchedEffect
            }
            // No auto-break started — normal focus-end cleanup.
            if (blockedHitCount > 0) {
                kavachSummaryMinutes  = (totalSeconds / 60).coerceAtLeast(1)
                kavachSummaryAttempts = focusShieldViewModel.snapshotBlockedAttempts()
                showKavachSessionSummary = true
            }
            val session = activeSession
            if (session != null) {
                viewModel.discardSession(session.id)
                viewModel.loadEkagraAnalytics()
                timerService?.reset()
                associatedGoalId = null
                associatedGoalTitle = null
                return@LaunchedEffect
            }
            viewModel.loadEkagraAnalytics()
            timerService?.reset()
            associatedGoalId = null; associatedGoalTitle = null
        }
    }

    // ── Derived values ───────────────────────────────────────────────────────────

    val progress = if (timerMode == TimerMode.STOPWATCH) {
        if (timerRunning) (secondsLeft.toFloat() % 60) / 60f else 0f
    } else {
        if (totalSeconds > 0) 1f - secondsLeft.toFloat() / totalSeconds else 0f
    }
    val mottoText = when {
        timerMode != TimerMode.FOCUS && timerMode != TimerMode.STOPWATCH && timerMode != TimerMode.POMODORO && timerRunning -> "BREAK TIME"
        timerMode == TimerMode.FOCUS && timerRunning && focusShieldActive -> "STUDY TIME - KAVACH ENABLED"
        timerRunning -> "STAY FOCUSED, YOU'RE DOING GREAT!"
        else         -> "READY TO FOCUS?"
    }
    val todayKey = remember { IstDateUtils.todayKey() }
    val openGoals = remember(allGoals) {
        val sevenDaysAgoKey = runCatching {
            java.time.LocalDate.parse(todayKey).minusDays(7).toString()
        }.getOrDefault(todayKey)
        allGoals.filter { goal ->
            // Basic validity
            goal.id.isNotBlank() && goal.title.isNotBlank()
            // Not already completed
            && !goal.completed
            // Exclude Ekagra-internal tasks (they are not user Goals)
            && goal.source != "ekagra"
            // Status-based exclusions
            && goal.status !in listOf("completed", "done")
            && goal.lifecycleStatus !in listOf("abandoned", "rolled_over", "completed")
            // Staleness guard — mirrors the Goals screen "Pending" section.
            // Old repeat/today goal instances that the user skipped (completed=false,
            // no lifecycleStatus) accumulate over time and pollute the link list.
            // Exclude any goal whose scheduledDate is older than 7 days UNLESS it
            // is actively in_progress (e.g. a carry-forward repeat like g1).
            && run {
                val scheduled = goal.scheduledDate?.takeIf { it.isNotBlank() }
                    ?: return@run true  // no date = timeless goal, always include
                if (goal.status == "in_progress") return@run true // carry-forward, always include
                // scheduled >= 7 days ago means it is still recent enough to be relevant
                scheduled.take(10) >= sevenDaysAgoKey
            }
        }
    }

    // ── M3 dynamic color scheme (theme-aware) ────────────────────────────────────
    val themeColorScheme = remember(selectedTheme, isDarkTheme) {
        val seed = if (!isDarkTheme && selectedTheme.name == "Focus") Color(0xFF9A3412) else selectedTheme.accent
        if (isDarkTheme) {
            val bgTint = blendColors(seed, Color(0xFF0F1115), 0.08f)
            val surfaceTint = blendColors(seed, Color(0xFF111416), 0.10f)
            val surfaceVariantTint = blendColors(seed, Color(0xFF272C35), 0.12f)
            val containerLowestTint = blendColors(seed, Color(0xFF0C0E10), 0.05f)
            val containerLowTint = blendColors(seed, Color(0xFF191C1F), 0.08f)
            val containerTint = blendColors(seed, Color(0xFF1D2024), 0.10f)
            val containerHighTint = blendColors(seed, Color(0xFF272A2E), 0.12f)
            val containerHighestTint = blendColors(seed, Color(0xFF323538), 0.15f)
            val outlineTint = blendColors(seed, Color(0xFF8C9399), 0.35f)
            val outlineVariantTint = blendColors(seed, Color(0xFF42494F), 0.20f)
            
            val onBgTint = blendColors(seed, Color(0xFFE7EBEF), 0.05f)
            val onSurfaceTint = blendColors(seed, Color(0xFFE1E2E5), 0.05f)
            val onSurfaceVariantTint = blendColors(seed, Color(0xFFC2C9CF), 0.10f)

            darkColorScheme(
                primary                = seed,
                onPrimary              = Color.White,
                primaryContainer       = seed.copy(alpha = 0.22f),
                onPrimaryContainer     = seed.copy(alpha = 0.90f),
                secondary              = seed.copy(alpha = 0.72f),
                onSecondary            = Color.White,
                secondaryContainer     = seed.copy(alpha = 0.18f),
                onSecondaryContainer   = seed.copy(alpha = 0.88f),
                background             = bgTint,
                onBackground           = onBgTint,
                surface                = surfaceTint,
                onSurface              = onSurfaceTint,
                surfaceVariant         = surfaceVariantTint,
                onSurfaceVariant       = onSurfaceVariantTint,
                surfaceContainerLowest = containerLowestTint,
                surfaceContainerLow    = containerLowTint,
                surfaceContainer       = containerTint,
                surfaceContainerHigh   = containerHighTint,
                surfaceContainerHighest= containerHighestTint,
                outline                = outlineTint,
                outlineVariant         = outlineVariantTint,
                tertiary               = Color(0xFFC8BFFF),
                tertiaryContainer      = Color(0xFF43398A),
                onTertiaryContainer    = Color(0xFFE3DFFF),
            )
        } else {
            val bgTint = blendColors(seed, Color(0xFFF8F9FB), 0.04f)
            val surfaceTint = blendColors(seed, Color(0xFFF8F9FB), 0.04f)
            val surfaceVariantTint = blendColors(seed, Color(0xFFE4E8EC), 0.08f)
            val containerLowestTint = blendColors(seed, Color(0xFFFFFFFF), 0.00f)
            val containerLowTint = blendColors(seed, Color(0xFFF2F4F6), 0.04f)
            val containerTint = blendColors(seed, Color(0xFFEBEEF1), 0.06f)
            val containerHighTint = blendColors(seed, Color(0xFFE4E8EC), 0.08f)
            val containerHighestTint = blendColors(seed, Color(0xFFDEE2E7), 0.10f)
            val outlineTint = blendColors(seed, Color(0xFF72797F), 0.25f)
            val outlineVariantTint = blendColors(seed, Color(0xFFC2C9CF), 0.10f)
            
            val onBgTint = blendColors(seed, Color(0xFF191C1F), 0.10f)
            val onSurfaceTint = blendColors(seed, Color(0xFF191C1F), 0.10f)
            val onSurfaceVariantTint = blendColors(seed, Color(0xFF42494F), 0.15f)

            lightColorScheme(
                primary                = seed,
                onPrimary              = Color.White,
                primaryContainer       = seed.copy(alpha = 0.14f),
                onPrimaryContainer     = seed.copy(alpha = 0.90f),
                secondary              = seed.copy(alpha = 0.68f),
                onSecondary            = Color.White,
                secondaryContainer     = seed.copy(alpha = 0.12f),
                onSecondaryContainer   = seed.copy(alpha = 0.85f),
                background             = bgTint,
                onBackground           = onBgTint,
                surface                = surfaceTint,
                onSurface              = onSurfaceTint,
                surfaceVariant         = surfaceVariantTint,
                onSurfaceVariant       = onSurfaceVariantTint,
                surfaceContainerLowest = containerLowestTint,
                surfaceContainerLow    = containerLowTint,
                surfaceContainer       = containerTint,
                surfaceContainerHigh   = containerHighTint,
                surfaceContainerHighest= containerHighestTint,
                outline                = outlineTint,
                outlineVariant         = outlineVariantTint,
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
                            showThemeDialog = false 
                        }, onDismiss = { showThemeDialog = false })
                    if (showMusicPromptDialog) {
                        MusicPromptDialog(
                            onYes = { dontShowAgain ->
                                if (dontShowAgain) {
                                    musicPromptPrefs.edit().putBoolean("skip_prompt", true).apply()
                                    skipMusicPrompt = true
                                }
                                showMusicPromptDialog = false
                                isStartingFromMusicSelection = true
                                showAudioLibraryPanel = true
                            },
                            onNo = { dontShowAgain ->
                                if (dontShowAgain) {
                                    musicPromptPrefs.edit().putBoolean("skip_prompt", true).apply()
                                    skipMusicPrompt = true
                                }
                                showMusicPromptDialog = false
                                
                                val silenceTrack = com.safarparmar.app.ui.audio.AudioLibrary.NONE_TRACK
                                selectedMusicTrack = silenceTrack
                                com.safarparmar.app.ui.audio.AudioLibrary.persistTrackId(context, silenceTrack.id)
                                
                                isStartingFromMusicSelection = true
                                pendingStartTimerArgs?.let { (mode, mins) ->
                                    startTimer(mode, mins)
                                }
                                pendingStartTimerArgs = null
                            },
                            onDismiss = {
                                showMusicPromptDialog = false
                                pendingStartTimerArgs = null
                            }
                        )
                    }
                    if (showAudioLibraryPanel) {
                        com.safarparmar.app.ui.audio.AudioLibraryPanel(
                            selectedTrackId = selectedMusicTrack.id,
                            onTrackSelect = {
                                selectedMusicTrack = it
                                com.safarparmar.app.ui.audio.AudioLibrary.persistTrackId(context, it.id)
                                if (isStartingFromMusicSelection) {
                                    isStartingFromMusicSelection = false
                                    showAudioLibraryPanel = false
                                    pendingStartTimerArgs?.let { (mode, mins) ->
                                        startTimer(mode, mins)
                                    }
                                    pendingStartTimerArgs = null
                                }
                            },
                            onDismiss = {
                                showAudioLibraryPanel = false
                                if (isStartingFromMusicSelection) {
                                    isStartingFromMusicSelection = false
                                    pendingStartTimerArgs = null
                                }
                            }
                        )
                    }
                    if (showOrganizeSheet) {
                        val pending = pendingEndedSession
                        val organizeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        val organizeSheetScope = rememberCoroutineScope()

                        // ModalBottomSheet must finish its hide animation before we tear the
                        // composable down, otherwise its scrim/Popup can be left behind
                        // half-animated — showing as a stuck, dimmed screen until back is pressed.
                        fun closeOrganizeSheet(onClosed: () -> Unit) {
                            organizeSheetScope.launch {
                                organizeSheetState.hide()
                            }.invokeOnCompletion {
                                onClosed()
                                showOrganizeSheet = false
                            }
                        }

                        fun savePendingAsFree() {
                            if (pending != null) {
                                closeOrganizeSheet {
                                    viewModel.completeSession(pending.sessionId, pending.totalSeconds,
                                        pending.secondsLeft, pending.mode, pending.startedAt,
                                        titleInput.ifBlank { "Untitled" }, null, null, false, pending.endedAt)
                                    timerService?.reset(); associatedGoalId = null
                                    associatedGoalTitle = null; pendingEndedSession = null
                                }
                            } else {
                                closeOrganizeSheet {}
                            }
                        }
                        OrganizeFreeFocusSheet(
                            sheetState    = organizeSheetState,
                            pending       = pending,
                            goals         = openGoals,
                            titleInput    = titleInput,
                            onTitleChange = { titleInput = it },
                            onDismiss     = { savePendingAsFree() },
                            onSaveFree    = { savePendingAsFree() },
                            onLinkGoal = { goal, shouldMarkComplete ->
                                if (pending != null) {
                                    closeOrganizeSheet {
                                        viewModel.linkGoalAndCompleteSession(pending.sessionId, goal,
                                            pending.totalSeconds, pending.secondsLeft, pending.mode, pending.startedAt,
                                            shouldMarkComplete, pending.endedAt)
                                        timerService?.reset(); associatedGoalId = null
                                        associatedGoalTitle = null; pendingEndedSession = null
                                    }
                                }
                            },
                            onDiscard = {
                                if (pending != null) {
                                    closeOrganizeSheet {
                                        viewModel.discardSession(pending.sessionId)
                                        timerService?.reset(); associatedGoalId = null
                                        associatedGoalTitle = null; pendingEndedSession = null
                                    }
                                }
                            },
                        )
                    }
                    if (showDurationPromptDialog) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showDurationPromptDialog = false },
                        ) {
                            // Card — light blue-tinted surface in light mode; follows the
                            // theme's dark surfaces at night instead of glaring light-blue.
                            val dialogBg     = themeColorScheme.surfaceContainerHigh
                            val dialogTitle  = themeColorScheme.onSurface
                            val dialogBody   = themeColorScheme.onSurfaceVariant
                            val dialogAccent = themeColorScheme.primary
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = dialogBg,
                                tonalElevation = 0.dp,
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        start = 24.dp, end = 24.dp,
                                        top = 28.dp, bottom = 20.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                ) {
                                    // Title
                                    Text(
                                        text = "Set Duration",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = dialogTitle,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    // Body
                                    Text(
                                        text = "Would you like to set your own duration before starting?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = dialogBody,
                                        lineHeight = 20.sp,
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    // "Do not show this again" checkbox row
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { dontShowDurationPromptAgain = !dontShowDurationPromptAgain }
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Checkbox(
                                            checked = dontShowDurationPromptAgain,
                                            onCheckedChange = { dontShowDurationPromptAgain = it },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = dialogAccent,
                                                uncheckedColor = dialogBody,
                                                checkmarkColor = Color.White,
                                            ),
                                        )
                                        Text(
                                            text = "Do not show this again",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = dialogTitle,
                                        )
                                    }
                                    Spacer(Modifier.height(24.dp))
                                    // Action buttons — right-aligned, text only
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // "Start anyway" — secondary action
                                        TextButton(
                                            onClick = {
                                                if (dontShowDurationPromptAgain) viewModel.disableDurationPrompt()
                                                showDurationPromptDialog = false
                                                durationPromptActedOn = true

                                                val wasInactive = timerService?.isActive() == false
                                                if (wasInactive) requestNotificationPermission()
                                                timerService?.togglePlayPause()
                                                if (wasInactive && timerMode == TimerMode.FOCUS) {
                                                    viewModel.onSessionStarted(taskText, totalSeconds,
                                                        associatedGoalId, associatedGoalTitle, timerMode.toApiMode())
                                                } else if (timerMode == TimerMode.FOCUS) {
                                                    viewModel.syncActiveSession(totalSeconds, secondsLeft, timerMode.toApiMode(),
                                                        true, associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() })
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                text = "Start anyway",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = dialogAccent,
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        // "Yes" — primary action
                                        TextButton(
                                            onClick = {
                                                if (dontShowDurationPromptAgain) viewModel.disableDurationPrompt()
                                                showDurationPromptDialog = false
                                                durationPromptActedOn = true
                                                tabBackStack.select(EkagraNavTab.DURATION)
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                text = "Yes",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = dialogAccent,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Main scaffold ────────────────────────────────────────────
                    // Top-bar content sits over the video scrim on the Timer tab and
                    // over a plain surface elsewhere — animate the tint so switching
                    // tabs fades the icons instead of snapping between colours.
                    val topBarTint by animateColorAsState(
                        targetValue = if (selectedTab == EkagraNavTab.TIMER) Color.White
                                      else themeColorScheme.onSurface,
                        animationSpec = tween(350),
                        label = "topBarTint",
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        SafarDrawerScaffold(
                            title              = stringResource(R.string.module_ekagra),
                            subtitle           = stringResource(R.string.app_name),
                            currentRoute       = currentRoute,
                            isDarkTheme        = isDarkTheme,
                            onNavigate         = onNavigate,
                            onToggleDarkTheme  = onToggleNightMode,
                            showTopBar         = true,
                            // Top-bar text is always white when timer tab is showing (video bg)
                            topBarContentColor = topBarTint,
                            topBarActions = {
                                val tintColor = topBarTint
                                IconButton(onClick = { showThemeDialog = true }) {
                                    Icon(Icons.Default.Palette, contentDescription = "Theme", tint = tintColor)
                                }
                                Box {
                                    var showOverflowMenu by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More options", tint = tintColor)
                                    }
                                    androidx.compose.material3.DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(if (isMuted) "Volume On" else "Volume Off") },
                                            onClick = { 
                                                timerService?.setMute(!isMuted)
                                                showOverflowMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(if (isMuted) androidx.compose.material.icons.Icons.Default.VolumeUp else androidx.compose.material.icons.Icons.Default.VolumeOff, contentDescription = null)
                                            }
                                        )
                                        val hasAllPermissions = shieldState.hasUsageStats &&
                                                                shieldState.hasAccessibilityService &&
                                                                shieldState.hasNotificationSuppressionAccess &&
                                                                shieldState.hasNotifications
                                        if (!hasAllPermissions) {
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Kavach Setup") },
                                                onClick = { 
                                                    onNavigate(com.safarparmar.app.ui.navigation.Routes.FOCUS_SHIELD)
                                                    showOverflowMenu = false
                                                },
                                                leadingIcon = {
                                                    Icon(androidx.compose.material.icons.Icons.Default.Shield, contentDescription = null)
                                                }
                                            )
                                        }
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(if (shieldState.isStrictMode) "Disable Beast Mode" else "Enable Beast Mode") },
                                            onClick = { 
                                                focusShieldViewModel.setStrictMode(!shieldState.isStrictMode)
                                                showOverflowMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(androidx.compose.material.icons.Icons.Default.FlashOn, contentDescription = null)
                                            }
                                        )
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text("Apps to unlock") },
                                            onClick = { 
                                                onNavigate(Routes.APP_PICKER)
                                                showOverflowMenu = false
                                            },
                                            leadingIcon = {
                                                Icon(androidx.compose.material.icons.Icons.Default.Apps, contentDescription = null)
                                            }
                                        )
                                    }
                                }
                            },
                        ) { padding ->

                            // Background + scrim only on timer tab
                            if (selectedTab == EkagraNavTab.TIMER) {
                                val colors = selectedTheme.gradientColors
                                if (colors != null) {
                                    // Cross-fade palette swaps when the user picks a new
                                    // visual theme instead of snapping to the new colours.
                                    val topColor by animateColorAsState(
                                        targetValue = colors[0],
                                        animationSpec = tween(1200),
                                        label = "bgTopColor",
                                    )
                                    val bottomColor by animateColorAsState(
                                        targetValue = colors[1],
                                        animationSpec = tween(1200),
                                        label = "bgBottomColor",
                                    )

                                    val infiniteTransition = rememberInfiniteTransition(label = "gradientAnimation")
                                    val angle by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 30000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "gradientAngle"
                                    )

                                    val configuration = LocalConfiguration.current
                                    val density = LocalDensity.current
                                    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
                                    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

                                    val dynamicGradient = remember(topColor, bottomColor, angle, screenWidthPx, screenHeightPx) {
                                        val radians = (angle * Math.PI / 180f).toFloat()
                                        val centerX = screenWidthPx * (0.5f + 0.25f * kotlin.math.cos(radians))
                                        val centerY = screenHeightPx * (0.5f + 0.25f * kotlin.math.sin(radians))
                                        androidx.compose.ui.graphics.Brush.radialGradient(
                                            colors = listOf(topColor, bottomColor),
                                            radius = maxOf(screenWidthPx, screenHeightPx) * 1.5f,
                                            center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                                        )
                                    }
                                    Box(modifier = Modifier.fillMaxSize().background(dynamicGradient))
                                } else {
                                    EkagraVideoBackground(videoUrl = selectedTheme.videoUrl, modifier = Modifier.fillMaxSize())
                                }
                                val scrimAlpha by animateFloatAsState(
                                    targetValue = when {
                                        timerImmersiveActive -> 0.12f
                                        isDarkTheme -> 0.55f
                                        else -> 0.38f
                                    },
                                    animationSpec = tween(600),
                                    label = "scrimAlpha",
                                )
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)))
                            }

                            Scaffold(
                                containerColor      = Color.Transparent,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                snackbarHost        = { SnackbarHost(snackbarHostState) },
                                bottomBar = {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = true,
                                        enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) +
                                                androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(500)),
                                        exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) +
                                               androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(500))
                                    ) {
                                        EkagraBottomNav(
                                            selectedTab = selectedTab,
                                            onSelect    = { tab ->
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (tab == EkagraNavTab.MUSIC) showAudioLibraryPanel = true
                                                else tabBackStack.select(tab)
                                            },
                                            // On timer tab the nav sits over the video scrim — use contrasting colours
                                            isOnVideo   = selectedTab == EkagraNavTab.TIMER,
                                        )
                                    }
                                },
                            ) { innerPadding ->
                                // Cross-fade with a gentle rise between tabs — the old
                                // instant snap made switching feel abrupt and unpolished.
                                AnimatedContent(
                                    targetState = selectedTab,
                                    transitionSpec = {
                                        (fadeIn(tween(240, delayMillis = 40)) +
                                            slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it / 24 })
                                            .togetherWith(fadeOut(tween(120)))
                                    },
                                    label = "ekagraTabContent",
                                ) { tab ->
                                when (tab) {

                                    EkagraNavTab.TIMER -> TimerFocusTab(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = padding.calculateTopPadding(),
                                                     bottom = innerPadding.calculateBottomPadding()),
                                        timerMode          = timerMode,
                                        secondsLeft        = secondsLeft,
                                        isRunning          = timerRunning,
                                        progress           = progress,
                                        hasProgress        = if (timerMode == TimerMode.STOPWATCH) secondsLeft > 0 else secondsLeft < totalSeconds,
                                        mottoText          = mottoText,
                                        countdownValue     = countdownValue,
                                        kavachActive       = focusShieldActive && timerRunning && timerMode == TimerMode.FOCUS,
                                        kavachBlockedCount = blockedHitCount,
                                        controlsVisible    = true,
                                        onOpenKavachSession = { showKavachActiveSession = true },
                                        onModeChange = { mode ->
                                            if (mode == timerMode) return@TimerFocusTab
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val mins = when (mode) {
                                                TimerMode.FOCUS, TimerMode.POMODORO -> focusMinutes
                                                TimerMode.BREAK -> breakMinutes
                                                TimerMode.STOPWATCH  -> 0
                                            }
                                            when {
                                                mode == TimerMode.FOCUS && timerMode != TimerMode.FOCUS ->
                                                    if (timerService?.switchToFocusFromBreak() != true) timerService?.setDuration(mode, mins * 60, breakMinutes * 60)
                                                mode != TimerMode.FOCUS && timerMode == TimerMode.FOCUS && timerService?.isActive() == true ->
                                                    timerService.startBreak(mode, mins * 60)
                                                else ->
                                                    timerService?.setDuration(mode, mins * 60, breakMinutes * 60)
                                            }
                                        },
                                        onPlayPause = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val wasRunning  = timerRunning
                                            val wasInactive = timerService?.isActive() == false
                                            
                                            if (wasInactive && showDurationPrompt && !durationPromptActedOn && (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO)) {
                                                showDurationPromptDialog = true
                                                return@TimerFocusTab
                                            }
                                            
                                            if (wasInactive) {
                                                requestNotificationPermission()
                                                if (timerMode == TimerMode.BREAK) {
                                                    timerService?.togglePlayPause()
                                                } else {
                                                    countdownValue = 3
                                                }
                                            } else if (wasRunning) {
                                                timerService?.togglePlayPause()
                                                if (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO) {
                                                    viewModel.pauseActiveSession(totalSeconds, secondsLeft, timerMode.toApiMode(), associatedGoalTitle)
                                                }
                                            } else {
                                                timerService?.togglePlayPause()
                                                if (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO) {
                                                    viewModel.syncActiveSession(totalSeconds, secondsLeft, timerMode.toApiMode(), true, associatedGoalTitle)
                                                }
                                            }
                                        },
                                        canStartBreak = (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO) && timerService?.isActive() == true,
                                        onStartBreak  = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            timerService?.startBreak(TimerMode.BREAK, breakMinutes * 60)
                                        },
                                        onReset       = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            endCurrentSession()
                                        },
                                        onGoToDuration = { tabBackStack.select(EkagraNavTab.DURATION) },
                                        shieldState   = shieldState,
                                        isDarkTheme   = isDarkTheme,
                                        themeAccent   = themeColorScheme.primary,
                                        onToggleKavach = focusShieldViewModel::setEnabled,
                                        onToggleStrictMode = focusShieldViewModel::setStrictMode,
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
                                        isMuted = isMuted,
                                        onMuteChange = { timerService?.setMute(it) },
                                        autoStartBreak = autoStartBreak,
                                        onAutoStartBreakChange = { viewModel.setAutoStartBreak(it) },
                                        onStartPomodoro = { loops ->
                                            timerService?.startPomodoroSession(loops, focusMinutes, breakMinutes)
                                            countdownValue = 3
                                            tabBackStack.select(EkagraNavTab.TIMER)
                                        },
                                        onSave = {
                                            durationPromptActedOn = true
                                            tabBackStack.select(EkagraNavTab.TIMER)
                                        },
                                    )

                                    EkagraNavTab.HISTORY -> FocusHistoryTab(
                                        modifier  = Modifier.padding(top = padding.calculateTopPadding(),
                                                                      bottom = innerPadding.calculateBottomPadding()),
                                        analytics = ekagraAnalytics,
                                        onSessionClick = { session ->
                                            val isStopwatch = session.timerMode.equals("stopwatch", ignoreCase = true)
                                            pendingEndedSession = PendingEndedEkagraSession(
                                                sessionId = session.id,
                                                totalSeconds = if (isStopwatch) session.actualMinutes * 60 else session.durationMinutes * 60,
                                                secondsLeft = if (isStopwatch) session.actualMinutes * 60 else (session.durationMinutes - session.actualMinutes) * 60,
                                                mode = if (isStopwatch) "stopwatch" else "Timer",
                                                startedAt = session.startedAt,
                                                endedAt = session.endedAt
                                            )
                                            titleInput = session.taskText ?: ""
                                            showOrganizeSheet = true
                                        }
                                    )
                                    
                                    EkagraNavTab.MUSIC -> {}
                                }
                                }
                            }
                        }
                    }

                    // Full-screen countdown blocker — sits above the whole scaffold
                    // (topbar, buttons, everything), so no tap can reach anything
                    // underneath while the 3-2-1 countdown is running. Fades out
                    // smoothly once the countdown hits 0 and the timer starts.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = countdownValue > 0,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(600, easing = FastOutSlowInEasing)),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .pointerInput(Unit) { detectTapGestures { } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MusicPromptDialog(
    onYes: (Boolean) -> Unit,
    onNo: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Play Ambient Music?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Would you like to play ambient music or Indian Classical ragas during this focus session?",
                    fontSize = 14.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Text(
                        text = "Do not show again",
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onYes(dontShowAgain) }
            ) {
                Text("Yes", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onNo(dontShowAgain) }
            ) {
                Text("No", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}


// ─── PiP overlay ───────────────────────────────────────────────────────────────
