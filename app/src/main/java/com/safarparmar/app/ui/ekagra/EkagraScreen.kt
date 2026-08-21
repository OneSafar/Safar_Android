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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
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
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
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
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.ui.studyplanner.components.LocalPlannerIsDarkTheme
import com.safarparmar.app.notifications.rememberNotificationPermissionRequester
import com.safarparmar.app.ui.components.rememberFeatureTabBackStack
import com.safarparmar.app.ui.drawer.SafarDrawerScaffold
import com.safarparmar.app.ui.navigation.Routes
import com.safarparmar.app.ui.tour.TourManager
import com.safarparmar.app.ui.tour.ekagraTourSteps
import com.safarparmar.app.ui.nishtha.checkin.SlimSlider
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.ui.launch.AppUsageMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

private data class PendingSaveConfirmation(
    val label: String,
    val completesTarget: Boolean,
    val keepsGoalOpen: Boolean = false,
    val commit: () -> Unit,
)

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
    linkedTopicId: String? = null,
    linkedTopicTitle: String? = null,
    linkedPlanId: String? = null,
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
    
    val secondsLeft       by (timerService?.secondsLeft        ?: fallbackSecondsLeft).collectAsStateWithLifecycle()
    val totalSeconds      by (timerService?.totalSeconds       ?: fallbackTotalSeconds).collectAsStateWithLifecycle()
    val timerRunning      by (timerService?.isRunning          ?: fallbackTimerRunning).collectAsStateWithLifecycle()
    val timerMode         by (timerService?.timerMode          ?: fallbackTimerMode).collectAsStateWithLifecycle()
    val isMuted           by (timerService?.isMuted            ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val blockedHitCount   by focusShieldViewModel.blockedHitCount.collectAsStateWithLifecycle()

    // UI state
    val tabBackStack             = rememberFeatureTabBackStack(EkagraNavTab.TIMER)
    val ekagraScope              = rememberCoroutineScope()
    val selectedTab              = tabBackStack.currentTab
    var showKavachActiveSession  by remember { mutableStateOf(false) }
    var showThemeDialog          by remember { mutableStateOf(false) }
    var showAudioLibraryPanel    by remember { mutableStateOf(false) }
    var showOrganizeSheet        by remember { mutableStateOf(false) }
    var showTopicStudySheet      by remember { mutableStateOf(false) }
    var topicStudySheetState     by remember { mutableStateOf<TopicStudySheetState>(TopicStudySheetState.ReadyToSave) }
    var pendingEndedSession      by remember { mutableStateOf<PendingEndedEkagraSession?>(null) }
    var titleInput               by remember { mutableStateOf("") }
    // ── Two-phase session-end flow state ──
    // Phase 1: naming dialog (user pressed "End")
    var showSessionNameDialog    by remember { mutableStateOf(false) }
    // Phase 2: post-save goal-linking sheet
    var showPostSaveGoalLinking  by remember { mutableStateOf(false) }
    var savedSessionId           by remember { mutableStateOf<String?>(null) }
    var savedSessionDuration     by remember { mutableIntStateOf(0) }
    val showDurationPrompt          by viewModel.showDurationPrompt.collectAsStateWithLifecycle()
    val studyCircleLiveSummary      by viewModel.studyCircleLiveSummary.collectAsStateWithLifecycle()
    val myCircles                   by viewModel.myCircles.collectAsStateWithLifecycle()
    val selectedStudyCircle         by viewModel.selectedStudyCircle.collectAsStateWithLifecycle()
    var showDurationPromptDialog    by remember { mutableStateOf(false) }
    var dontShowDurationPromptAgain by remember { mutableStateOf(false) }
    // True once the user has responded to the duration prompt in this session.
    // Prevents the dialog from re-appearing every time Start is tapped after
    // the user has already visited the Duration tab via the prompt.
    var durationPromptActedOn       by remember { mutableStateOf(false) }
    var tourState                by remember { mutableStateOf<com.safarparmar.app.ui.butterfly.ButterflyTourState?>(null) }
    val timerImmersiveActive = false
    val appUsageMode by viewModel.dataStore.appUsageMode.collectAsStateWithLifecycle(initialValue = null)
    // Overlay bubble permission
    val overlayGranted = remember { mutableStateOf(TimerBubbleOverlay.canDrawOverlays(context)) }
    var showOverlayPermPrompt by remember { mutableStateOf(false) }

    BackHandler(enabled = tabBackStack.hasHistory) {
        tabBackStack.goBack()
    }

    LaunchedEffect(tourState?.isVisible, tourState?.currentStepIndex) {
        val state = tourState ?: return@LaunchedEffect
        if (!state.isVisible) return@LaunchedEffect
        when (state.currentStepIndex) {
            // Step 3 and 4 are Settings Tab (DURATION)
            3, 4 -> tabBackStack.select(EkagraNavTab.DURATION)
            // Step 6 is History Tab
            6 -> tabBackStack.select(EkagraNavTab.HISTORY)
            // All other steps (0, 1, 2, 5) are on the main Timer Tab
            else -> tabBackStack.select(EkagraNavTab.TIMER)
        }
    }

    var selectedTheme by remember {
        val prefs = context.getSharedPreferences("ekagra_theme_prefs", android.content.Context.MODE_PRIVATE)
        val persisted = visualThemes.getOrNull(prefs.getInt("theme_index", -1))
        // A hidden theme can still be persisted from before it was retired.
        // Fall back so those users land on a theme they can also re-pick.
        mutableStateOf(persisted?.takeIf { !it.hidden } ?: selectableVisualThemes.first())
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
    var associatedTopicId    by remember(linkedTopicId)    { mutableStateOf(linkedTopicId) }
    var associatedPlanId     by remember(linkedPlanId)     { mutableStateOf(linkedPlanId) }
    var associatedTopicTitle by remember(linkedTopicTitle) { mutableStateOf(linkedTopicTitle) }
    var taskText            by remember(linkedGoalId, linkedGoalTitle, linkedTopicId, linkedTopicTitle) {
        mutableStateOf(linkedGoalTitle ?: linkedTopicTitle.orEmpty())
    }
    val savedFocusMinutes   by viewModel.dataStore.focusDurationMinutes.collectAsStateWithLifecycle(initialValue = 25)
    val savedBreakMinutes   by viewModel.dataStore.breakDurationMinutes.collectAsStateWithLifecycle(initialValue = 5)
    var focusMinutes        by remember(savedFocusMinutes) { mutableIntStateOf(savedFocusMinutes) }
    var breakMinutes        by remember(savedBreakMinutes) { mutableIntStateOf(savedBreakMinutes) }
    var longBreakMinutes    by remember(savedBreakMinutes) { mutableIntStateOf(savedBreakMinutes) }
    val autoStartBreak      by viewModel.dataStore.autoStartBreak.collectAsStateWithLifecycle(initialValue = true)
    val timerAlertStyle     by viewModel.dataStore.timerAlertStyle.collectAsStateWithLifecycle(
        initialValue = com.safarparmar.app.data.local.TimerAlertStyle.SOUND,
    )

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
        if ((mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) && shieldState.isEnabled && shieldState.blockedPackages.isNotEmpty()
            && (!shieldState.hasUsageStats || !shieldState.hasOverlayPermission)) {
            onNavigate(Routes.FOCUS_SHIELD); return
        }
        requestNotificationPermission()
        timerService?.saveTheme(visualThemes.indexOf(selectedTheme), selectedMusicTrack.name)

        val service = timerService
        if ((mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) && timerMode != TimerMode.FOCUS && timerMode != TimerMode.STOPWATCH && timerMode != TimerMode.POMODORO && service?.switchToFocusFromBreak() == true) {
            service.prepareAutoSaveSession(
                taskTitle = associatedGoalTitle ?: associatedTopicTitle ?: taskText.takeIf { it.isNotBlank() },
                goalId = associatedGoalId,
                goalTitle = associatedGoalTitle,
                topicId = associatedTopicId,
                planId = associatedPlanId,
                topicTitle = associatedTopicTitle,
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
                taskTitle = associatedGoalTitle ?: associatedTopicTitle ?: taskText.takeIf { it.isNotBlank() },
                goalId = associatedGoalId,
                goalTitle = associatedGoalTitle,
                topicId = associatedTopicId,
                planId = associatedPlanId,
                topicTitle = associatedTopicTitle,
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
                taskTitle = associatedGoalTitle ?: associatedTopicTitle ?: taskText.takeIf { it.isNotBlank() },
                goalId = associatedGoalId,
                goalTitle = associatedGoalTitle,
                topicId = associatedTopicId,
                planId = associatedPlanId,
                topicTitle = associatedTopicTitle,
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
    }

    fun resetTimer() {
        activeSession?.id?.let { viewModel.discardSession(it) }
        timerService?.reset()
        associatedGoalId = null; associatedGoalTitle = null
        associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
    }

    fun endCurrentSession() {
        val service = timerService
        val serviceTopic = service?.plannerTopicMetadata()
        val endingTopicId = associatedTopicId ?: serviceTopic?.topicId
        val endingPlanId = associatedPlanId ?: serviceTopic?.planId
        val endingTopicTitle = associatedTopicTitle ?: serviceTopic?.topicTitle
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
            val progress = service?.focusProgressSnapshot()
            val loggedTotalSeconds: Int
            val loggedSecondsLeft: Int
            if (timerMode == TimerMode.STOPWATCH) {
                val elapsed = progress?.actualSeconds ?: secondsLeft.coerceAtLeast(0)
                loggedTotalSeconds = elapsed
                loggedSecondsLeft = elapsed
            } else {
                loggedTotalSeconds = progress?.plannedSeconds ?: totalSeconds
                val actual = progress?.actualSeconds
                    ?: (totalSeconds - secondsLeft).coerceAtLeast(0)
                loggedSecondsLeft = (loggedTotalSeconds - actual).coerceAtLeast(0)
            }
            timerService?.pause()
            pendingEndedSession = PendingEndedEkagraSession(
                sessionId    = session.id,
                totalSeconds = loggedTotalSeconds,
                secondsLeft  = loggedSecondsLeft,
                mode         = timerMode.toApiMode(),
                startedAt    = session.sessionStartedAt,
            )
            titleInput = session.sessionTitle ?: taskText
            // A deliberate End tap always lets the student choose how this
            // session should be filed: normal Ekagra or one of today's goals.
            showOrganizeSheet = true
            return
        }
        if (
            serviceTopic != null &&
            (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO)
        ) {
            val progress = service?.focusProgressSnapshot()
            val plannedSeconds: Int
            val remainingSeconds: Int
            if (timerMode == TimerMode.STOPWATCH) {
                val elapsed = progress?.actualSeconds ?: secondsLeft.coerceAtLeast(0)
                plannedSeconds = elapsed
                remainingSeconds = elapsed
            } else {
                plannedSeconds = progress?.plannedSeconds ?: totalSeconds
                val actual = progress?.actualSeconds
                    ?: (totalSeconds - secondsLeft).coerceAtLeast(0)
                remainingSeconds = (plannedSeconds - actual).coerceAtLeast(0)
            }
            if (topicStudyActualSeconds(
                    PendingEndedEkagraSession(
                        sessionId = serviceTopic.clientSessionId,
                        totalSeconds = plannedSeconds,
                        secondsLeft = remainingSeconds,
                        mode = timerMode.toApiMode(),
                        startedAt = serviceTopic.startedAt,
                    ),
                ) > 0
            ) {
                timerService?.pause()
                pendingEndedSession = PendingEndedEkagraSession(
                    sessionId = serviceTopic.clientSessionId,
                    totalSeconds = plannedSeconds,
                    secondsLeft = remainingSeconds,
                    mode = timerMode.toApiMode(),
                    startedAt = serviceTopic.startedAt,
                    topicId = serviceTopic.topicId,
                    planId = serviceTopic.planId,
                    topicTitle = serviceTopic.topicTitle,
                )
                topicStudySheetState = TopicStudySheetState.ReadyToSave
                showTopicStudySheet = true
                return
            }
        }
        viewModel.onSessionCompleted(totalSeconds, secondsLeft, timerMode.toApiMode())
        timerService?.reset()
        associatedGoalId = null; associatedGoalTitle = null
        associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
    }

    // ── Side-effects ────────────────────────────────────────────────────────────

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
            overlayGranted.value = TimerBubbleOverlay.canDrawOverlays(context)
            viewModel.refreshEkagra(); viewModel.loadTasks()
            while (true) {
                delay(20_000L)
                viewModel.loadEkagraAnalytics()
                viewModel.loadStudyCircleLiveSummary()
            }
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
        // Theme/music choice is a durable user preference, not session state — persist it
        // regardless of whether a timer is currently running. Previously this was gated on
        // isActive(), so picking a theme while idle (e.g. before configuring KAVACH's blocked
        // apps) was silently lost the moment the screen was recreated on navigating back.
        timerService?.saveTheme(visualThemes.indexOf(selectedTheme), selectedMusicTrack.name)
    }

    val pipContext   = LocalContext.current

    // One-time overlay permission prompt: shown when timer starts if permission not yet granted
    LaunchedEffect(timerRunning) {
        if (timerRunning && !overlayGranted.value) {
            val alreadyAsked = viewModel.dataStore.overlayPermissionAsked.first()
            if (!alreadyAsked) showOverlayPermPrompt = true
        }
        if (timerRunning) {
            overlayGranted.value = TimerBubbleOverlay.canDrawOverlays(context)
        }
    }

    LaunchedEffect(timerRunning, secondsLeft) {
        if (!timerRunning && secondsLeft == 0 && totalSeconds > 0) {
            val completedMode = timerMode
            if (completedMode != TimerMode.FOCUS && completedMode != TimerMode.POMODORO) return@LaunchedEffect
            // ── Pomodoro auto-break guard ────────────────────────────────────────
            // TimerService sets _isRunning=false momentarily, then immediately
            // switches to BREAK mode and calls start() — this all happens on the
            // Main dispatcher without suspension. Wait 500 ms for the service's
            // StateFlows to settle so we can tell whether an auto-break started.
            delay(500L)
            if (timerMode != TimerMode.FOCUS || timerRunning) {
                if (completedMode == TimerMode.POMODORO && timerMode == TimerMode.BREAK) {
                    // An intermediate loop ended. Keep the logical session draft and
                    // its associations alive for the next focus loop.
                    return@LaunchedEffect
                }
                // Auto 5-minute break is now running. The service already saved the
                // completed focus session via enqueueCompletedFocusSessionSave().
                // Just discard the stale ViewModel draft and refresh analytics —
                // do NOT call timerService?.reset() or the break will be killed.
                activeSession?.id?.let { viewModel.discardSession(it) }
                viewModel.loadEkagraAnalytics()
                associatedGoalId = null
                associatedGoalTitle = null
                associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
                return@LaunchedEffect
            }
            // No auto-break started — normal focus-end cleanup.
            val session = activeSession
            if (session != null) {
                // Natural completion is saved immediately as an unlinked Untitled
                // session. The user can long-press it in History to link it later.
                val endedAt = Instant.now().toString()
                viewModel.saveSessionImmediately(
                    sessionId = session.id,
                    totalSeconds = totalSeconds,
                    secondsLeft = 0,
                    mode = completedMode.toApiMode(),
                    startedAt = session.sessionStartedAt,
                    endedAt = endedAt,
                    taskTitle = null,
                    isAutoComplete = true,
                ) { _, _ ->
                    viewModel.loadEkagraAnalytics()
                }
                timerService?.reset()
                associatedGoalId = null; associatedGoalTitle = null
                associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
                return@LaunchedEffect
            } else if (totalSeconds > 0) {
                val endedAt = Instant.now().toString()
                val fallbackId = "local-${java.util.UUID.randomUUID()}"
                viewModel.saveSessionImmediately(
                    sessionId = fallbackId,
                    totalSeconds = totalSeconds,
                    secondsLeft = 0,
                    mode = completedMode.toApiMode(),
                    startedAt = Instant.now().minusSeconds(totalSeconds.toLong()).toString(),
                    endedAt = endedAt,
                    taskTitle = null,
                    isAutoComplete = true,
                ) { _, _ ->
                    viewModel.loadEkagraAnalytics()
                }
                timerService?.reset()
                associatedGoalId = null; associatedGoalTitle = null
                associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
                return@LaunchedEffect
            }
            viewModel.loadEkagraAnalytics()
            timerService?.reset()
            associatedGoalId = null; associatedGoalTitle = null
            associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
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
        timerMode == TimerMode.FOCUS && timerRunning && shieldState.isEnabled -> "STUDY TIME - KAVACH ENABLED"
        timerRunning -> "STAY FOCUSED, YOU'RE DOING GREAT!"
        else         -> "READY TO FOCUS?"
    }
    val todayKey = remember { IstDateUtils.todayKey() }
    val linkableGoals = remember(allGoals, todayKey) {
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
            && !goal.nextInstanceCreated
        }
    }
    val todayGoals = remember(linkableGoals, todayKey) {
        linkableGoals.filter { goal ->
            val day = IstDateUtils.getDateKey(goal.scheduledDate)
                ?: IstDateUtils.getDateKey(goal.createdAt)
                ?: IstDateUtils.getDateKey(goal.startedAt)
            day == todayKey && goal.status !in listOf("missed", "expired") && goal.lifecycleStatus != "missed"
        }
    }

    val themeColorScheme = remember(selectedTheme, isDarkTheme) {
        val seed = if (isDarkTheme) {
            val darkGradient = selectedTheme.gradientColors
            if (darkGradient != null && darkGradient.isNotEmpty()) {
                darkGradient.first()
            } else {
                when (selectedTheme.name) {
                    "Serene" -> Color(0xFF64B5F6)     // Lighter Cerulean/Blue
                    "Nostalgia" -> Color(0xFF81C784)  // Lighter Kelly Green
                    "Amber" -> Color(0xFFA5D6A7)      // Lighter Forest Green
                    "Solitude" -> Color(0xFFB39DDB)   // Lighter Violet/Purple
                    else -> selectedTheme.accent
                }
            }
        } else {
            selectedTheme.accent
        }
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
                onPrimary              = contrastOn(seed),
                primaryContainer       = seed.copy(alpha = 0.22f),
                onPrimaryContainer     = contrastOn(seed.copy(alpha = 0.22f)),
                secondary              = seed.copy(alpha = 0.72f),
                onSecondary            = contrastOn(seed.copy(alpha = 0.72f)),
                secondaryContainer     = seed.copy(alpha = 0.18f),
                onSecondaryContainer   = contrastOn(seed.copy(alpha = 0.18f)),
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
                onPrimary              = contrastOn(seed),
                primaryContainer       = seed.copy(alpha = 0.14f),
                onPrimaryContainer     = contrastOn(seed.copy(alpha = 0.14f)),
                secondary              = seed.copy(alpha = 0.68f),
                onSecondary            = contrastOn(seed.copy(alpha = 0.68f)),
                secondaryContainer     = seed.copy(alpha = 0.12f),
                onSecondaryContainer   = contrastOn(seed.copy(alpha = 0.12f)),
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

    CompositionLocalProvider(
        LocalPlannerIsDarkTheme provides isDarkTheme,
    ) {
        MaterialTheme(colorScheme = themeColorScheme) {

            // ── Kavach overlay screens ───────────────────────────────────────────
            when {
                showKavachActiveSession -> {
                    BackHandler { showKavachActiveSession = false }
                    com.safarparmar.app.ui.ekagra.focusshield.KavachActiveSessionScreen(
                        secondsLeft  = secondsLeft,
                        blockedCount = blockedHitCount,
                        onBack       = { showKavachActiveSession = false },
                        onEndSession = { showKavachActiveSession = false; endCurrentSession() },
                    )
                }
                else -> {
                    // ── Dialogs / sheets ─────────────────────────────────────────
                    if (showThemeDialog)
                        VisualThemeDialog(current = selectedTheme, onSelect = { 
                            selectedTheme = it
                            showThemeDialog = false 
                        }, onDismiss = { showThemeDialog = false })
                    // ── Overlay bubble permission prompt (shown once) ─────────────
                    if (showOverlayPermPrompt) {
                        androidx.compose.material3.ModalBottomSheet(
                            onDismissRequest = {
                                showOverlayPermPrompt = false
                                ekagraScope.launch {
                                    viewModel.dataStore.setOverlayPermissionAsked(true)
                                }
                            },
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = themeColorScheme.primary,
                                    modifier = androidx.compose.ui.Modifier.size(40.dp),
                                )
                                androidx.compose.material3.Text(
                                    "Show floating timer?",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                androidx.compose.material3.Text(
                                    "Allow SAFAR to show a small timer bubble on the side of your screen while you use other apps.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                EkagraPrimaryAction(
                                    label = "Grant Permission",
                                    accent = themeColorScheme.primary,
                                    onClick = {
                                        showOverlayPermPrompt = false
                                        ekagraScope.launch {
                                            viewModel.dataStore.setOverlayPermissionAsked(true)
                                        }
                                        TimerBubbleOverlay.openOverlayPermissionSettings(pipContext)
                                    },
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                )
                                EkagraGhostAction(
                                    label = "Not Now",
                                    ink = rememberEkagraInk(onCanvas = false),
                                    onClick = {
                                        showOverlayPermPrompt = false
                                        ekagraScope.launch {
                                            viewModel.dataStore.setOverlayPermissionAsked(true)
                                        }
                                    },
                                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                )
                                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(16.dp))
                            }
                        }
                    }
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
                    if (showTopicStudySheet) {
                        val pending = pendingEndedSession
                        if (pending != null && pending.topicId != null && pending.planId != null) {
                            val topicSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                            fun closeTopicSheet() {
                                showTopicStudySheet = false
                                pendingEndedSession = null
                                timerService?.reset()
                                associatedTopicId = null
                                associatedTopicTitle = null
                                associatedPlanId = null
                            }

                            TopicStudySaveSheet(
                                sheetState = topicSheetState,
                                pending = pending,
                                state = topicStudySheetState,
                                selectedTheme = selectedTheme,
                                isDarkTheme = isDarkTheme,
                                onDismiss = { closeTopicSheet() },
                                onSave = {
                                    topicStudySheetState = TopicStudySheetState.Saving
                                    viewModel.saveTopicStudyTime(pending) { result ->
                                        topicStudySheetState = when (result) {
                                            EkagraViewModel.StudyTimeSaveResult.Saved ->
                                                TopicStudySheetState.Saved
                                            EkagraViewModel.StudyTimeSaveResult.SavedOnPhone ->
                                                TopicStudySheetState.SavedOnPhone
                                        }
                                        timerService?.reset()
                                    }
                                },
                                onNotYet = { closeTopicSheet() },
                                onFinished = {
                                    topicStudySheetState = TopicStudySheetState.MarkingDone
                                    viewModel.markPlannerTopicDone(pending.planId, pending.topicId) { result ->
                                        when (result) {
                                            EkagraViewModel.TopicDoneResult.Done -> closeTopicSheet()
                                            is EkagraViewModel.TopicDoneResult.Error -> {
                                                topicStudySheetState = TopicStudySheetState.TopicError(
                                                    "Topic could not be marked as done.\nPlease try again.",
                                                )
                                            }
                                        }
                                    }
                                },
                                onDiscard = {
                                    viewModel.discardSession(pending.sessionId)
                                    closeTopicSheet()
                                },
                            )
                        }
                    }
                    if (showOrganizeSheet) {
                        val pending = pendingEndedSession
                        val organizeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        val organizeSheetScope = rememberCoroutineScope()
                        // Hold the exact save choice until the student confirms it.
                        // Linking study time and finishing a goal must stay separate.
                        var pendingSaveConfirmation by remember {
                            mutableStateOf<PendingSaveConfirmation?>(null)
                        }

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

                        fun clearAssociations() {
                            timerService?.reset(); associatedGoalId = null
                            associatedGoalTitle = null; pendingEndedSession = null
                            associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
                        }

                        fun savePendingAsFree() {
                            if (pending != null) {
                                closeOrganizeSheet {
                                    // A "local-" id is a live draft that hasn't been saved yet →
                                    // create it. Any other id is an already-saved history session
                                    // being renamed → update it IN PLACE. Using completeSession
                                    // (save-new + delete-old) for an existing session is what
                                    // produced the "renamed session clones itself" duplicates.
                                    if (pending.sessionId.startsWith("local-")) {
                                        viewModel.completeSession(pending.sessionId, pending.totalSeconds,
                                            pending.secondsLeft, pending.mode, pending.startedAt,
                                            titleInput.ifBlank { "Untitled" }, null, null, false, pending.endedAt)
                                    } else {
                                        viewModel.updateExistingSession(
                                            sessionId = pending.sessionId,
                                            taskTitle = titleInput.ifBlank { "Untitled" },
                                        )
                                    }
                                    clearAssociations()
                                }
                            } else {
                                closeOrganizeSheet {}
                            }
                        }

                        fun saveTopicLinkedSession(markDone: Boolean) {
                            if (pending != null && pending.topicId != null && pending.planId != null) {
                                closeOrganizeSheet {
                                    if (pending.sessionId.startsWith("local-")) {
                                        viewModel.completeSession(
                                            sessionId = pending.sessionId,
                                            totalSeconds = pending.totalSeconds,
                                            secondsLeft = pending.secondsLeft,
                                            mode = pending.mode,
                                            startedAt = pending.startedAt,
                                            taskTitle = pending.topicTitle,
                                            endedAt = pending.endedAt,
                                            topicId = pending.topicId,
                                            planId = pending.planId,
                                            topicTitle = pending.topicTitle,
                                            markTopicDone = markDone,
                                        )
                                    } else {
                                        viewModel.updateExistingSession(
                                            sessionId = pending.sessionId,
                                            taskTitle = pending.topicTitle,
                                            topicId = pending.topicId,
                                            planId = pending.planId,
                                            topicTitle = pending.topicTitle,
                                            markTopicDone = markDone,
                                        )
                                    }
                                    clearAssociations()
                                }
                            }
                        }
                        fun linkGoalNow(goal: com.safarparmar.app.domain.model.Goal, shouldMarkComplete: Boolean) {
                            if (pending != null) {
                                closeOrganizeSheet {
                                    if (pending.sessionId.startsWith("local-")) {
                                        viewModel.linkGoalAndSaveSession(pending.sessionId, goal,
                                            pending.totalSeconds, pending.secondsLeft, pending.mode, pending.startedAt,
                                            shouldMarkComplete, pending.endedAt)
                                    } else {
                                        viewModel.linkSavedSessionToGoal(
                                            pending.sessionId,
                                            goal,
                                            shouldMarkComplete,
                                        )
                                    }
                                    clearAssociations()
                                }
                            }
                        }

                        OrganizeFreeFocusSheet(
                            sheetState    = organizeSheetState,
                            pending       = pending,
                            todayGoals    = todayGoals,
                            titleInput    = titleInput,
                            onTitleChange = { titleInput = it },
                            selectedTheme = selectedTheme,
                            isDarkTheme   = isDarkTheme,
                            // Swiping the sheet away files the session under its safest
                            // default rather than losing it. The explicit choices below go
                            // through a confirmation because they are final.
                            onDismiss     = { if (pending?.topicId != null) saveTopicLinkedSession(false) else savePendingAsFree() },
                            onSaveFree    = {
                                pendingSaveConfirmation = PendingSaveConfirmation(
                                    label = "Quick Save",
                                    completesTarget = false,
                                    commit = { savePendingAsFree() },
                                )
                            },
                            onSaveTopic   = { markDone ->
                                pendingSaveConfirmation = PendingSaveConfirmation(
                                    label = pending?.topicTitle ?: "this topic",
                                    completesTarget = markDone,
                                    commit = { saveTopicLinkedSession(markDone) },
                                )
                            },
                            onLinkGoal = { goal, shouldMarkComplete ->
                                pendingSaveConfirmation = PendingSaveConfirmation(
                                    label = goal.title,
                                    completesTarget = shouldMarkComplete,
                                    keepsGoalOpen = !shouldMarkComplete,
                                    commit = { linkGoalNow(goal, shouldMarkComplete) },
                                )
                            },
                            onDiscard = {
                                if (pending != null) {
                                    closeOrganizeSheet {
                                        viewModel.discardSession(pending.sessionId)
                                        clearAssociations()
                                    }
                                }
                            },
                        )

                        pendingSaveConfirmation?.let { confirmation ->
                            EkagraConfirmSaveDialog(
                                label = confirmation.label,
                                completesTarget = confirmation.completesTarget,
                                keepsGoalOpen = confirmation.keepsGoalOpen,
                                accentColor = selectedTheme?.accent ?: themeColorScheme.primary,
                                onConfirm = {
                                    pendingSaveConfirmation = null
                                    confirmation.commit()
                                },
                                onCancel = { pendingSaveConfirmation = null },
                            )
                        }
                    }

                    // ── New two-phase flow: Phase 1 — Session name dialog ────
                    if (showSessionNameDialog) {
                        val pending = pendingEndedSession
                        if (pending != null) {
                            val actualSecs = if (pending.mode.equals("stopwatch", ignoreCase = true)) {
                                pending.secondsLeft
                            } else {
                                pending.totalSeconds - pending.secondsLeft
                            }.coerceAtLeast(0)
                            SessionNameDialog(
                                initialTitle = titleInput,
                                focusedTimeLabel = formatTopicStudyTime(actualSecs),
                                onSave = { typedTitle ->
                                    showSessionNameDialog = false
                                    viewModel.saveSessionImmediately(
                                        sessionId = pending.sessionId,
                                        totalSeconds = pending.totalSeconds,
                                        secondsLeft = pending.secondsLeft,
                                        mode = pending.mode,
                                        startedAt = pending.startedAt,
                                        endedAt = pending.endedAt,
                                        taskTitle = typedTitle,
                                        isAutoComplete = false,
                                    ) { _, _ ->
                                        viewModel.loadEkagraAnalytics()
                                    }
                                    timerService?.reset()
                                    pendingEndedSession = null
                                    associatedGoalId = null; associatedGoalTitle = null
                                    associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
                                },
                                onDiscard = {
                                    showSessionNameDialog = false
                                    if (pending.sessionId.isNotBlank()) {
                                        viewModel.discardSession(pending.sessionId)
                                    }
                                    timerService?.reset()
                                    pendingEndedSession = null
                                    associatedGoalId = null; associatedGoalTitle = null
                                    associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
                                },
                            )
                        }
                    }

                    // ── New two-phase flow: Phase 2 — Post-save goal linking ──
                    if (showPostSaveGoalLinking && savedSessionId != null) {
                        PostSaveGoalLinkingSheet(
                            savedSessionId = savedSessionId!!,
                            savedDurationSeconds = savedSessionDuration,
                            todayGoals = todayGoals,
                            selectedTheme = selectedTheme,
                            onDismiss = {
                                showPostSaveGoalLinking = false
                                savedSessionId = null
                                viewModel.loadEkagraAnalytics()
                            },
                            onLinkGoal = { goal, markComplete ->
                                val sessionId = savedSessionId!!
                                viewModel.linkSavedSessionToGoal(sessionId, goal, markComplete)
                                showPostSaveGoalLinking = false
                                savedSessionId = null
                                viewModel.loadEkagraAnalytics()
                            },
                        )
                    }

                    if (showDurationPromptDialog) {
                        val dialogInk = rememberEkagraInk(onCanvas = false)
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
                                shape = RoundedCornerShape(20.dp),
                                color = dialogBg,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .border(1.dp, dialogInk.hairline, RoundedCornerShape(20.dp)),
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
                                    // Action buttons — hairline styled
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // "Start anyway" — secondary action
                                        EkagraGhostAction(
                                            label = "Start anyway",
                                            ink = dialogInk,
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
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        // "Yes" — primary action
                                        EkagraPrimaryAction(
                                            label = "Yes",
                                            accent = dialogAccent,
                                            onClick = {
                                                if (dontShowDurationPromptAgain) viewModel.disableDurationPrompt()
                                                showDurationPromptDialog = false
                                                durationPromptActedOn = true
                                                tabBackStack.select(EkagraNavTab.DURATION)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Main scaffold ────────────────────────────────────────────
                    // The old floating liquid-glass bar is gone — its menu, guide
                    // and overflow actions now live in EkagraTopBar, a bare row
                    // drawn straight on the canvas. Tint still follows the Timer
                    // tab's video/gradient canvas vs. the plain surface elsewhere.
                    val headerInk = rememberEkagraInk(
                        onCanvas = selectedTab == EkagraNavTab.TIMER,
                        theme = selectedTheme,
                        isDarkTheme = isDarkTheme,
                    )
                    val topBarTint = headerInk.primaryText
                    var openDrawer by remember { mutableStateOf<() -> Unit>({}) }

                    // Guide + overflow actions — unchanged in behaviour, just moved
                    // out of the old glass bar and into EkagraTopBar's trailing slot.
                    val ekagraTopBarActions: @Composable RowScope.() -> Unit = {
                        val tintColor = topBarTint
                        Box {
                            var showOverflowMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "More options", tint = tintColor)
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false },
                                modifier = Modifier.border(1.dp, headerInk.hairline, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
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
                                EkagraHairline(headerInk.hairline)
                                androidx.compose.material3.DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (overlayGranted.value) "Floating timer settings"
                                            else "Enable floating timer"
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        if (overlayGranted.value) {
                                            TimerBubbleOverlay.openOverlayPermissionSettings(pipContext)
                                        } else {
                                            showOverlayPermPrompt = true
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            androidx.compose.material.icons.Icons.Default.PictureInPictureAlt,
                                            contentDescription = null,
                                        )
                                    },
                                    trailingIcon = {
                                        androidx.compose.material3.Switch(
                                            checked = overlayGranted.value,
                                            onCheckedChange = null,
                                        )
                                    },
                                )
                                val hasAllPermissions = shieldState.hasUsageStats &&
                                                        shieldState.hasOverlayPermission
                                if (!hasAllPermissions) {
                                    EkagraHairline(headerInk.hairline)
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
                                EkagraHairline(headerInk.hairline)
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Apps to Block") },
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
                    }

                    Box(
                        Modifier.fillMaxSize()
                    ) {
                        SafarDrawerScaffold(
                            title              = stringResource(R.string.module_ekagra),
                            subtitle           = stringResource(R.string.app_name),
                            currentRoute       = currentRoute,
                            isDarkTheme        = isDarkTheme,
                            onNavigate         = onNavigate,
                            onToggleDarkTheme  = onToggleNightMode,
                            showTopBar         = false,
                            containerColor     = Color.Transparent,
                            onDrawerControllerReady = { openDrawer = it },
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
                                    // Unreachable while the video themes are hidden — every
                                    // selectable theme now has a gradient. Kept so restoring
                                    // them is just a matter of clearing their gradientColors.
                                    // Blur only the moving backdrop so the timer stays
                                    // readable; gradient themes remain sharp.
                                    EkagraVideoBackground(
                                        videoUrl = selectedTheme.videoUrl,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .scale(1.06f)
                                            .blur(10.dp),
                                    )
                                }
                                val scrimAlpha by animateFloatAsState(
                                    targetValue = when {
                                        timerImmersiveActive -> 0.12f
                                        isDarkTheme -> 0.55f
                                        selectedTheme.gradientColors != null -> 0.08f
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
                                topBar = {
                                    EkagraTopBar(
                                        ink = headerInk,
                                        onOpenDrawer = { openDrawer() },
                                        trailing = ekagraTopBarActions,
                                    )
                                },
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
                                                when (tab) {
                                                    EkagraNavTab.MUSIC -> showAudioLibraryPanel = true
                                                    EkagraNavTab.THEME -> showThemeDialog = true
                                                    else -> tabBackStack.select(tab)
                                                }
                                            },
                                            // On timer tab the nav sits over the video scrim — use contrasting colours
                                            isOnVideo   = selectedTab == EkagraNavTab.TIMER,
                                            isDarkTheme = isDarkTheme,
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
                                            .padding(top = innerPadding.calculateTopPadding(),
                                                     bottom = innerPadding.calculateBottomPadding()),
                                        timerMode          = timerMode,
                                        secondsLeft        = secondsLeft,
                                        isRunning          = timerRunning,
                                        progress           = progress,
                                        hasProgress        = if (timerMode == TimerMode.STOPWATCH) secondsLeft > 0 else secondsLeft < totalSeconds,
                                        mottoText          = mottoText,
                                        kavachActive       = shieldState.isEnabled && timerRunning && timerMode == TimerMode.FOCUS,
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
                                                if (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO) {
                                                    timerService?.prepareAutoSaveSession(
                                                        taskTitle = associatedGoalTitle ?: associatedTopicTitle ?: taskText.takeIf { it.isNotBlank() },
                                                        goalId = associatedGoalId,
                                                        goalTitle = associatedGoalTitle,
                                                        topicId = associatedTopicId,
                                                        planId = associatedPlanId,
                                                        topicTitle = associatedTopicTitle,
                                                        forceNew = true,
                                                    )
                                                }
                                                timerService?.togglePlayPause()
                                                if (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO) {
                                                    viewModel.onSessionStarted(taskText, totalSeconds, associatedGoalId, associatedGoalTitle, timerMode.toApiMode())
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
                                        onOpenAppPicker = { onNavigate(Routes.APP_PICKER) },
                                        onNavigate = onNavigate,
                                        selectedTheme = selectedTheme,
                                        onOpenAnalytics = {
                                            onNavigate(Routes.nishthaAnalytics("kavach"))
                                        },
                                        studyCircleLiveCount = studyCircleLiveSummary?.totalFocusing ?: 0,
                                        myCircles = myCircles,
                                        selectedStudyCircle = selectedStudyCircle,
                                        onSelectStudyCircle = viewModel::selectStudyCircle,
                                    )

                                    EkagraNavTab.DURATION -> DurationTab(
                                        modifier      = Modifier.padding(top = innerPadding.calculateTopPadding(),
                                                                          bottom = innerPadding.calculateBottomPadding()),
                                        focusMinutes  = focusMinutes,
                                        breakMinutes  = breakMinutes,
                                        onFocusChange = {
                                            focusMinutes = it
                                            viewModel.setFocusDurationMinutes(it)
                                            if (timerService?.isActive() == false && (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO)) {
                                                timerService.setDuration(TimerMode.FOCUS, it * 60, breakMinutes * 60)
                                            }
                                        },
                                        onBreakChange = {
                                            breakMinutes = it
                                            longBreakMinutes = it
                                            viewModel.setBreakDurationMinutes(it)
                                            if (timerService?.isActive() == false && timerMode == TimerMode.BREAK) {
                                                timerService.setDuration(TimerMode.BREAK, it * 60, it * 60)
                                            }
                                        },
                                        isMuted = isMuted,
                                        onMuteChange = { timerService?.setMute(it) },
                                        autoStartBreak = autoStartBreak,
                                        onAutoStartBreakChange = { viewModel.setAutoStartBreak(it) },
                                        timerAlertStyle = timerAlertStyle,
                                        onTimerAlertStyleChange = viewModel::setTimerAlertStyle,
                                        onStartPomodoro = { loops ->
                                            viewModel.setFocusDurationMinutes(focusMinutes)
                                            viewModel.setBreakDurationMinutes(breakMinutes)
                                            timerService?.startPomodoroSession(loops, focusMinutes, breakMinutes)
                                            timerService?.prepareAutoSaveSession(
                                                taskTitle = associatedGoalTitle ?: associatedTopicTitle ?: taskText.takeIf { it.isNotBlank() },
                                                goalId = associatedGoalId,
                                                goalTitle = associatedGoalTitle,
                                                topicId = associatedTopicId,
                                                planId = associatedPlanId,
                                                topicTitle = associatedTopicTitle,
                                                forceNew = true,
                                            )
                                            timerService?.start()
                                            viewModel.onSessionStarted(
                                                taskText = taskText,
                                                totalSeconds = focusMinutes * 60,
                                                goalId = associatedGoalId,
                                                goalTitle = associatedGoalTitle,
                                                mode = TimerMode.POMODORO.toApiMode(),
                                            )
                                            tabBackStack.select(EkagraNavTab.TIMER)
                                        },
                                        onSave = {
                                            durationPromptActedOn = true
                                            viewModel.setFocusDurationMinutes(focusMinutes)
                                            viewModel.setBreakDurationMinutes(breakMinutes)
                                            if (timerService?.isActive() == false) {
                                                val currentMins = if (timerMode == TimerMode.BREAK) breakMinutes else focusMinutes
                                                timerService?.setDuration(timerMode, currentMins * 60, breakMinutes * 60)
                                            }
                                            tabBackStack.select(EkagraNavTab.TIMER)
                                        },
                                    )

                                    // History is READ-ONLY. Tapping a saved session used to
                                    // reopen the organize sheet so it could be renamed or moved
                                    // between Quick Save and a goal. That edit path is what
                                    // produced the duplicated goal pairs in the published app,
                                    // and a session's category is final once saved — so the row
                                    // is no longer interactive at all.
                                    EkagraNavTab.HISTORY -> FocusHistoryTab(
                                        modifier  = Modifier.padding(top = innerPadding.calculateTopPadding(),
                                                                      bottom = innerPadding.calculateBottomPadding()),
                                        analytics = ekagraAnalytics,
                                        selectedTheme = selectedTheme,
                                    )
                                    
                                    EkagraNavTab.MUSIC -> {}
                                    EkagraNavTab.THEME -> {}
                                }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    TourManager(
        dataStore = viewModel.dataStore,
        steps = ekagraTourSteps,
        section = "ekagra",
        askOnFirstVisit = false,
        onTourStateReady = { tourState = it },
    )
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
