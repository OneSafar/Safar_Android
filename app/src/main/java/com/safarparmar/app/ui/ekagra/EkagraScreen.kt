package com.safarparmar.app.ui.ekagra

import com.composables.ui.components.Text
import com.composables.ui.components.Icon

import com.safarparmar.app.ui.adaptive.isSpaciousLandscape
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.draw.drawBehind

import com.safarparmar.app.performance.decorativeFloat
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
import com.safarparmar.app.performance.adaptiveBlur
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
import androidx.lifecycle.LifecycleEventObserver
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
    val linksTopic: Boolean = false,
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
    val localizedResources by rememberUpdatedState(LocalResources.current)
    val requestNotificationPermission = rememberNotificationPermissionRequester()
    val shieldState          by focusShieldViewModel.shieldState.collectAsStateWithLifecycle()
    val haptics              = LocalHapticFeedback.current
    val lifecycleOwner       = LocalLifecycleOwner.current

    // fallback flows when timerService is null
    val fallbackSecondsLeft      = remember { MutableStateFlow(25 * 60) }
    val fallbackTotalSeconds     = remember { MutableStateFlow(25 * 60) }
    val fallbackTimerRunning     = remember { MutableStateFlow(false) }
    val fallbackTimerMode        = remember { MutableStateFlow(TimerMode.FOCUS) }

    val secondsLeft       by (timerService?.secondsLeft        ?: fallbackSecondsLeft).collectAsStateWithLifecycle()
    val totalSeconds      by (timerService?.totalSeconds       ?: fallbackTotalSeconds).collectAsStateWithLifecycle()
    val timerRunning      by (timerService?.isRunning          ?: fallbackTimerRunning).collectAsStateWithLifecycle()
    val timerMode         by (timerService?.timerMode          ?: fallbackTimerMode).collectAsStateWithLifecycle()
    EkagraBackgroundAccessPrompt(
        sessionId = timerService?.currentSessionId(),
        runningStudy = timerRunning && timerMode != TimerMode.BREAK,
    )
    val isMuted           by (timerService?.isMuted            ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val rankedStatus by (timerService?.rankedStatus ?: remember { MutableStateFlow<RankedFocusStatus>(RankedFocusStatus.ConnectionNeeded) }).collectAsStateWithLifecycle()
    val completedSession by (timerService?.completedSession ?: remember { MutableStateFlow<PendingEkagraSessionSave?>(null) }).collectAsStateWithLifecycle()
    val pomodoroCompletionEvent by (timerService?.pomodoroCompletionEvent ?: remember { MutableStateFlow(0) }).collectAsStateWithLifecycle()
    val blockedHitCount   by focusShieldViewModel.blockedHitCount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // UI state
    val tabBackStack             = rememberFeatureTabBackStack(EkagraNavTab.TIMER)
    val ekagraScope              = rememberCoroutineScope()
    val selectedTab              = tabBackStack.currentTab
    var showKavachActiveSession  by remember { mutableStateOf(false) }
    var showThemeDialog          by remember { mutableStateOf(false) }
    var showEndSessionConfirmDialog by remember { mutableStateOf(false) }
    var showAudioLibraryPanel    by remember { mutableStateOf(false) }
    var showOrganizeSheet        by remember { mutableStateOf(false) }
    var showTopicStudySheet      by remember { mutableStateOf(false) }
    // ── Tab-switch persistence fix ──────────────────────────────────────────────
    // Tracks which mode the pill UI shows. Decoupled from the running service's
    // timerMode so the user can browse other mode tabs without resetting the session.
    var selectedDisplayMode by remember { mutableStateOf(TimerMode.FOCUS) }
    // The mode the user wants to switch to while another session is active.
    // Non-null triggers the "switch mode?" confirmation dialog.
    var pendingModeSwitchConfirm by remember { mutableStateOf<TimerMode?>(null) }
    var modeAfterSessionEnd by remember { mutableStateOf<TimerMode?>(null) }
    var topicStudySheetState     by remember { mutableStateOf<TopicStudySheetState>(TopicStudySheetState.ReadyToSave) }
    var pendingEndedSession      by remember { mutableStateOf<PendingEndedEkagraSession?>(null) }
    var titleInput               by remember { mutableStateOf("") }
    var selectedSessionTag       by remember { mutableStateOf<String?>(null) }
    val ekagraTags               by viewModel.ekagraTags.collectAsStateWithLifecycle(initialValue = DEFAULT_EKAGRA_TAGS)
    val ekagraTagColors          by viewModel.ekagraTagColors.collectAsStateWithLifecycle(initialValue = DEFAULT_TAG_COLORS)
    // ── Two-phase session-end flow state ──
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
    val tutorialModel: EkagraTutorialViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val timerImmersiveActive = false
    val appUsageMode by viewModel.dataStore.appUsageMode.collectAsStateWithLifecycle(initialValue = null)
    // Overlay bubble permission
    val overlayGranted = remember { mutableStateOf(TimerBubbleOverlay.canDrawOverlays(context)) }
    var showOverlayPermPrompt by remember { mutableStateOf(false) }

    BackHandler(enabled = tabBackStack.hasHistory) {
        tabBackStack.goBack()
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

    var launchContextHandled by remember(linkedGoalId, linkedTopicId, linkedPlanId) {
        mutableStateOf(false)
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
    var focusMinutes        by remember(savedFocusMinutes) {
        mutableIntStateOf(savedFocusMinutes.coerceAtMost(MAX_EKAGRA_SESSION_SECONDS / 60))
    }
    var breakMinutes        by remember(savedBreakMinutes) { mutableIntStateOf(savedBreakMinutes) }
    var longBreakMinutes    by remember(savedBreakMinutes) { mutableIntStateOf(savedBreakMinutes) }
    val autoStartBreak      by viewModel.dataStore.autoStartBreak.collectAsStateWithLifecycle(initialValue = true)
    val timerAlertStyle     by viewModel.dataStore.timerAlertStyle.collectAsStateWithLifecycle(
        initialValue = com.safarparmar.app.data.local.TimerAlertStyle.SOUND,
    )

    // ── Helpers ─────────────────────────────────────────────────────────────────

    fun showLongSessionIntroIfNeeded(mode: TimerMode, durationSeconds: Int) {
        if (mode == TimerMode.STOPWATCH ||
            ((mode == TimerMode.FOCUS || mode == TimerMode.POMODORO) && durationSeconds > 4 * 60 * 60)
        ) {
            ekagraScope.launch {
                snackbarHostState.showSnackbar(localizedResources.getString(R.string.ekagra_long_session_intro))
            }
        }
    }

    // Every fresh study start must attach the planner link before TimerService
    // creates its durable session. This also covers the first-use duration prompt.
    fun prepareStudySession() {
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
        showLongSessionIntroIfNeeded(mode, minutes * 60)
        viewModel.onSessionStarted(
            taskText     = taskText,
            totalSeconds = if (mode == TimerMode.STOPWATCH) 0 else minutes * 60,
            goalId       = if (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) associatedGoalId else null,
            goalTitle    = if (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) associatedGoalTitle else null,
            mode         = mode.toApiMode(),
            clientSessionId = timerService?.currentSessionId(),
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
        val pomodoroBreak = timerMode == TimerMode.BREAK && (service?.targetPomodoroLoops?.value ?: 0) > 0
        val endingFocus = pomodoroBreak || timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO
        val endingMode = if (pomodoroBreak) TimerMode.POMODORO.toApiMode() else timerMode.toApiMode()
        val endingTopicId = associatedTopicId ?: serviceTopic?.topicId
        val endingPlanId = associatedPlanId ?: serviceTopic?.planId
        val endingTopicTitle = associatedTopicTitle ?: serviceTopic?.topicTitle
        if (!pomodoroBreak && timerMode != TimerMode.FOCUS && timerMode != TimerMode.STOPWATCH && timerMode != TimerMode.POMODORO && service?.switchToFocusFromBreak() == true) {
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
        if (session != null && endingFocus && endingTopicId == null) {
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
                mode         = endingMode,
                startedAt    = session.sessionStartedAt,
            )
            val rawTitle = session.sessionTitle ?: taskText
            val (tag, task) = EkagraTagUtils.parseTagAndTask(rawTitle)
            selectedSessionTag = tag
            titleInput = task ?: ""
            // A deliberate End tap always lets the student choose how this
            // session should be filed: normal Ekagra or one of today's goals.
            showOrganizeSheet = true
            return
        }
        if (
            serviceTopic != null &&
            endingFocus
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
                        mode = endingMode,
                        startedAt = serviceTopic.startedAt,
                    ),
                ) > 0
            ) {
                timerService?.pause()
                pendingEndedSession = PendingEndedEkagraSession(
                    sessionId = serviceTopic.clientSessionId,
                    totalSeconds = plannedSeconds,
                    secondsLeft = remainingSeconds,
                    mode = endingMode,
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
        if (endingFocus) {
            val progress = service?.focusProgressSnapshot()
            if (progress != null && progress.actualSeconds > 0) {
                service.pause()
                pendingEndedSession = PendingEndedEkagraSession(
                    sessionId = service.currentSessionId()?.takeIf { it.startsWith("local-") }
                        ?: "local-${java.util.UUID.randomUUID()}",
                    totalSeconds = if (timerMode == TimerMode.STOPWATCH) progress.actualSeconds else progress.plannedSeconds,
                    secondsLeft = if (timerMode == TimerMode.STOPWATCH) progress.actualSeconds else (progress.plannedSeconds - progress.actualSeconds).coerceAtLeast(0),
                    mode = endingMode,
                    startedAt = service.currentSessionStartedAt()
                        ?: Instant.now().minusSeconds(progress.actualSeconds.toLong()).toString(),
                )
                val (tag, task) = EkagraTagUtils.parseTagAndTask(taskText)
                selectedSessionTag = tag
                titleInput = task ?: ""
                showOrganizeSheet = true
                return
            }
        }
        viewModel.onSessionCompleted(totalSeconds, secondsLeft, timerMode.toApiMode())
        timerService?.reset()
        associatedGoalId = null; associatedGoalTitle = null
        associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
    }

    // ── Side-effects ────────────────────────────────────────────────────────────

    LaunchedEffect(modeAfterSessionEnd, showTopicStudySheet, showOrganizeSheet, pendingEndedSession, timerService) {
        val nextMode = modeAfterSessionEnd ?: return@LaunchedEffect
        if (showTopicStudySheet || showOrganizeSheet || pendingEndedSession != null) return@LaunchedEffect
        val service = timerService ?: return@LaunchedEffect
        service.awaitSessionFinalization()
        if (service.isActive()) {
            // Ending a manual break first restores its suspended focus timer.
            // Finalize that timer before replacing it with the requested mode.
            service.endAndSave()
            service.awaitSessionFinalization()
        }
        modeAfterSessionEnd = null
        val minutes = when (nextMode) {
            TimerMode.FOCUS, TimerMode.POMODORO -> focusMinutes
            TimerMode.BREAK -> breakMinutes
            TimerMode.STOPWATCH -> 0
        }
        service.setDuration(nextMode, minutes * 60, breakMinutes * 60)
        selectedDisplayMode = nextMode
        if (nextMode != TimerMode.BREAK) prepareStudySession()
        service.start()
        showLongSessionIntroIfNeeded(nextMode, minutes * 60)
        if (nextMode != TimerMode.BREAK) {
            viewModel.onSessionStarted(
                taskText = taskText,
                totalSeconds = if (nextMode == TimerMode.STOPWATCH) 0 else minutes * 60,
                goalId = associatedGoalId,
                goalTitle = associatedGoalTitle,
                mode = nextMode.toApiMode(),
                clientSessionId = service.currentSessionId(),
            )
        }
    }


    val draftOwner by viewModel.draftOwner.collectAsStateWithLifecycle()
    LaunchedEffect(timerService, timerRunning, timerMode, totalSeconds, secondsLeft, draftOwner, activeSession?.id) {
        val service = timerService ?: return@LaunchedEffect
        if (activeSession == null && service.plannerTopicMetadata() == null) {
            val draft = service.activeSessionDraft() ?: return@LaunchedEffect
            if (draft.userId != draftOwner) return@LaunchedEffect
            viewModel.restoreActiveSessionDraft(draft)
            if (associatedGoalId == null) {
                associatedGoalId = draft.goalId
                associatedGoalTitle = draft.goalTitle
            }
            if (taskText.isBlank()) taskText = draft.sessionTitle.orEmpty()
        }
    }

    LaunchedEffect(timerService, linkedTopicId, linkedPlanId, linkedGoalId, draftOwner, timerRunning) {
        val service = timerService ?: return@LaunchedEffect
        val draft = service.activeSessionDraft() ?: return@LaunchedEffect
        if (!service.isActive() || draft.userId != draftOwner) return@LaunchedEffect
        val topic = service.plannerTopicMetadata()
        val requested = EkagraTaskContext(
            goalId = linkedGoalId, goalTitle = linkedGoalTitle,
            topicId = linkedTopicId, planId = linkedPlanId, topicTitle = linkedTopicTitle,
        )
        val running = EkagraTaskContext(
            goalId = draft.goalId, goalTitle = draft.goalTitle,
            topicId = topic?.topicId, planId = topic?.planId, topicTitle = topic?.topicTitle,
            taskTitle = draft.sessionTitle,
        )
        val resolved = resolveEkagraTaskContext(requested, running)
        associatedGoalId = resolved.goalId
        associatedGoalTitle = resolved.goalTitle
        associatedTopicId = resolved.topicId
        associatedPlanId = resolved.planId
        associatedTopicTitle = resolved.topicTitle
        taskText = resolved.taskTitle ?: resolved.goalTitle ?: resolved.topicTitle.orEmpty()
        val conflictingLaunch = !launchContextHandled && requested.hasLink && !requested.hasSameLink(running)
        launchContextHandled = true
        if (conflictingLaunch) {
            snackbarHostState.showSnackbar(localizedResources.getString(R.string.ekagra_current_session_first))
        }
    }

    LaunchedEffect(shieldState.activationMessage) {
        val message = shieldState.activationMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        focusShieldViewModel.clearActivationMessage()
    }

    // Tab navigation must never reconfigure the service. Start/Save explicitly apply durations.
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

    val floatingTimerOpenRequest = (context as? com.safarparmar.app.MainActivity)?.ekagraOpenRequest ?: 0L
    LaunchedEffect(floatingTimerOpenRequest, timerService) {
        if (floatingTimerOpenRequest > 0L && timerService != null) {
            tabBackStack.select(EkagraNavTab.TIMER)
            selectedDisplayMode = when (timerService.timerMode.value) {
                TimerMode.POMODORO -> TimerMode.FOCUS
                else -> timerService.timerMode.value
            }
        }
    }

    // Follow actual phase changes, including automatic Pomodoro breaks. Browsing a
    // different pill does not mutate the timer; reopening returns to its active mode.
    LaunchedEffect(timerService, timerMode) {
        selectedDisplayMode = if (timerMode == TimerMode.POMODORO) TimerMode.FOCUS else timerMode
    }
    DisposableEffect(lifecycleOwner, timerService) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                timerService?.let { service ->
                    selectedDisplayMode = when (service.timerMode.value) {
                        TimerMode.POMODORO -> TimerMode.FOCUS
                        else -> service.timerMode.value
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

    LaunchedEffect(completedSession?.clientSessionId) {
        val completed = completedSession ?: return@LaunchedEffect
        activeSession?.id?.takeIf { it == completed.clientSessionId }?.let { viewModel.discardSession(it) }
        savedSessionId = completed.clientSessionId
        savedSessionDuration = completed.actualDurationSeconds ?: completed.actualDurationMinutes * 60
        showPostSaveGoalLinking = completed.topicId == null
        if (!completed.topicId.isNullOrBlank() && !completed.planId.isNullOrBlank()) {
            pendingEndedSession = completed.toEndedPlannerSession()
            topicStudySheetState = TopicStudySheetState.SavedOnPhone
            showTopicStudySheet = true
            timerService?.acknowledgeCompletedSession(completed.clientSessionId)
        }
        associatedGoalId = null; associatedGoalTitle = null
        associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
        viewModel.loadEkagraAnalytics()
    }

    LaunchedEffect(pomodoroCompletionEvent) {
        if (pomodoroCompletionEvent > 0) {
            timerService?.acknowledgePomodoroCompletion()
            activeSession?.id?.let { viewModel.discardSession(it) }
            associatedGoalId = null; associatedGoalTitle = null
            associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
            viewModel.loadEkagraAnalytics()
        }
    }

    // ── Derived values ───────────────────────────────────────────────────────────

    val progress = if (timerMode == TimerMode.STOPWATCH) {
        if (timerRunning) (secondsLeft.toFloat() % 60) / 60f else 0f
    } else {
        if (totalSeconds > 0) 1f - secondsLeft.toFloat() / totalSeconds else 0f
    }
    val activePillMode = if (timerMode == TimerMode.POMODORO) TimerMode.FOCUS else timerMode
    val isBrowsingOtherMode = timerService?.isActive() == true && selectedDisplayMode != activePillMode
    val displayedTimerMode = if (isBrowsingOtherMode) selectedDisplayMode else timerMode
    val displayedSeconds = if (isBrowsingOtherMode) when (selectedDisplayMode) {
        TimerMode.FOCUS, TimerMode.POMODORO -> focusMinutes * 60
        TimerMode.BREAK -> breakMinutes * 60
        TimerMode.STOPWATCH -> 0
    } else secondsLeft
    val mottoText = when {
        timerMode != TimerMode.FOCUS && timerMode != TimerMode.STOPWATCH && timerMode != TimerMode.POMODORO && timerRunning -> "BREAK TIME"
        (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO) && timerRunning && shieldState.isProtectionActive -> "STUDY TIME - KAVACH ACTIVE"
        timerRunning -> "STAY FOCUSED, YOU'RE DOING GREAT!"
        else         -> "READY TO FOCUS?"
    }
    val displayedMottoText = if (isBrowsingOtherMode) when (selectedDisplayMode) {
        TimerMode.FOCUS, TimerMode.POMODORO -> "READY TO FOCUS?"
        TimerMode.BREAK -> "READY FOR A BREAK?"
        TimerMode.STOPWATCH -> "READY TO START?"
    } else mottoText
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

    val isBreakActive = timerMode == TimerMode.BREAK && (timerRunning || (totalSeconds > 0 && secondsLeft < totalSeconds))
    val breakSaturation by animateFloatAsState(
        targetValue = if (isBreakActive) 0f else 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "breakSaturation",
    )

    val ekagraView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(ekagraView) {
        val window = (ekagraView.context as? android.app.Activity)?.window
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, ekagraView) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            previousStatus?.let { controller?.isAppearanceLightStatusBars = it }
            previousNavigation?.let { controller?.isAppearanceLightNavigationBars = it }
        }
    }

    val themeColorScheme = remember(selectedTheme, isDarkTheme) { ekagraWarmColorScheme(selectedTheme, isDarkTheme) }

    CompositionLocalProvider(
        LocalPlannerIsDarkTheme provides isDarkTheme,
        LocalEkagraWarmPalette provides ekagraWarmPalette(selectedTheme.name),
        com.safarparmar.app.ui.theme.LocalSafarComposablesAccent provides themeColorScheme.primary,
    ) {
        MaterialTheme(colorScheme = themeColorScheme) {
        com.safarparmar.app.ui.theme.SafarComposablesTheme {

            // ── Kavach overlay screens ───────────────────────────────────────────
            when {
                showKavachActiveSession -> {
                    BackHandler { showKavachActiveSession = false }
                    com.safarparmar.app.ui.ekagra.focusshield.KavachActiveSessionScreen(
                        secondsLeft  = secondsLeft,
                        blockedCount = blockedHitCount,
                        onBack       = { showKavachActiveSession = false },
                        onEndSession = { showKavachActiveSession = false; showEndSessionConfirmDialog = true },
                    )
                }
                else -> {
                    // ── Dialogs / sheets ─────────────────────────────────────────
                    if (showThemeDialog)
                        VisualThemeDialog(current = selectedTheme, onSelect = {
                            selectedTheme = it
                            showThemeDialog = false
                        }, onDismiss = { showThemeDialog = false })
                    if (showEndSessionConfirmDialog) {
                        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
                            visible = true,
                            onDismissRequest = { showEndSessionConfirmDialog = false },
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text(
                                    text = stringResource(R.string.ekagra_end_session_question),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.ekagra_end_session_body),
                                    fontSize = 14.sp
                                )
                            },
                            positiveButton= {
                                com.composables.ui.components.Button(style = com.composables.ui.components.ButtonStyle.Ghost,
                                    onClick = {
                                        showEndSessionConfirmDialog = false
                                        endCurrentSession()
                                    }
                                ) {
                                    Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_continue), fontWeight = FontWeight.Bold)
                                }
                            },
                            negativeButton= {
                                com.composables.ui.components.Button(style = com.composables.ui.components.ButtonStyle.Ghost,
                                    onClick = {
                                        showEndSessionConfirmDialog = false
                                    }
                                ) {
                                    Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_no))
                                }
                            },
                        )
                    }
                    // ── Mode-switch confirmation dialog ────────────────────────────
                    // Shown when user browses to a different mode tab (e.g. BREAK or
                    // STOPWATCH) while a session is running and then presses Play.
                    val modeSwitchTarget = pendingModeSwitchConfirm
                    if (modeSwitchTarget != null) {
                        com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
                            visible = true,
                            onDismissRequest = { pendingModeSwitchConfirm = null },
                            shape = RoundedCornerShape(20.dp),
                            title = {
                                Text(
                                    text = stringResource(R.string.ekagra_mode_switch_title, stringResource(modeSwitchTarget.labelRes)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.ekagra_mode_switch_body, stringResource(timerMode.labelRes)),
                                    fontSize = 14.sp,
                                )
                            },
                            positiveButton= {
                                com.composables.ui.components.Button(style = com.composables.ui.components.ButtonStyle.Ghost,
                                    onClick = {
                                        pendingModeSwitchConfirm = null
                                        // End opens a save sheet for planner tasks. Keep the
                                        // current service intact until that choice is resolved.
                                        modeAfterSessionEnd = modeSwitchTarget
                                        endCurrentSession()
                                    }
                                ) {
                                    Text(stringResource(R.string.ekagra_yes_switch), fontWeight = FontWeight.Bold)
                                }
                            },
                            negativeButton= {
                                com.composables.ui.components.Button(style = com.composables.ui.components.ButtonStyle.Ghost,
                                    onClick = {
                                        pendingModeSwitchConfirm = null
                                        // Snap the pill back to the currently running mode
                                        selectedDisplayMode = timerMode
                                    }
                                ) {
                                    Text(stringResource(R.string.ekagra_keep_running))
                                }
                            },
                        )
                    }
                    // ── Overlay bubble permission prompt (shown once) ─────────────
                    if (showOverlayPermPrompt) {
                        com.safarparmar.app.ui.components.popups.SafarComposablesBottomSheet(
                            state = com.composables.ui.components.rememberBottomSheetState(
                                initialDetent = com.composables.ui.components.BottomSheetDetent.FullyExpanded,
                            ),
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
                                    .wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally)
                                    .widthIn(max = 560.dp)
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
                                    stringResource(R.string.ekagra_floating_timer_title),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                                androidx.compose.material3.Text(
                                    stringResource(R.string.ekagra_floating_timer_body),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                EkagraPrimaryAction(
                                    label = stringResource(R.string.ekagra_grant_permission),
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
                                    label = stringResource(R.string.ekagra_not_now),
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
                            @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                            val topicSheetState = com.composables.ui.components.rememberBottomSheetState(initialDetent = com.composables.ui.components.BottomSheetDetent.FullyExpanded)

                            fun closeTopicSheet(discard: Boolean = false) {
                                showTopicStudySheet = false
                                pendingEndedSession = null
                                // A naturally completed session may already have started
                                // a break. Only reset the session this sheet is ending.
                                // Discard must end it without queueing the elapsed time.
                                if (timerService?.currentSessionId() == pending.sessionId) {
                                    if (discard) timerService.discard() else timerService.reset()
                                }
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
                                    closeTopicSheet(discard = true)
                                },
                            )
                        }
                    }
                    if (showOrganizeSheet) {
                        val pending = pendingEndedSession
                        val organizeSheetState = com.composables.ui.components.rememberBottomSheetState(initialDetent = com.composables.ui.components.BottomSheetDetent.FullyExpanded)
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

                        fun clearAssociations(discard: Boolean = false) {
                            // Discard ends the timer without saving; every other
                            // caller has already saved/linked the session.
                            if (discard) timerService?.discard() else timerService?.reset()
                            associatedGoalId = null
                            associatedGoalTitle = null; pendingEndedSession = null
                            selectedSessionTag = null
                            associatedTopicId = null; associatedTopicTitle = null; associatedPlanId = null
                        }

                        fun savePendingAsFree() {
                            if (pending != null) {
                                closeOrganizeSheet {
                                    val formattedTitle = EkagraTagUtils.formatTaggedTask(selectedSessionTag, titleInput)
                                    // A "local-" id is a live draft that hasn't been saved yet →
                                    // create it. Any other id is an already-saved history session
                                    // being renamed → update it IN PLACE. Using completeSession
                                    // (save-new + delete-old) for an existing session is what
                                    // produced the "renamed session clones itself" duplicates.
                                    if (pending.sessionId.startsWith("local-")) {
                                        viewModel.completeSession(pending.sessionId, pending.totalSeconds,
                                            pending.secondsLeft, pending.mode, pending.startedAt,
                                            formattedTitle, null, null, false, pending.endedAt)
                                    } else {
                                        viewModel.updateExistingSession(
                                            sessionId = pending.sessionId,
                                            taskTitle = formattedTitle,
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
                            availableTags = ekagraTags,
                            selectedTag   = selectedSessionTag,
                            onSelectTag   = { selectedSessionTag = it },
                            tagColors     = ekagraTagColors,
                            onAddTag      = { tag, colorHex -> viewModel.addTag(tag, colorHex) },
                            onDeleteTag   = { viewModel.removeTag(it) },
                            onSetTagColor = { tag, colorHex -> viewModel.setTagColor(tag, colorHex) },
                            // Swiping the sheet away files the session under its safest
                            // default rather than losing it. The explicit choices below go
                            // through a confirmation because they are final.
                            onDismiss     = { if (pending?.topicId != null) saveTopicLinkedSession(false) else savePendingAsFree() },
                            onSaveFree    = {
                                pendingSaveConfirmation = PendingSaveConfirmation(
                                    label = localizedResources.getString(R.string.ekagra_quick_save),
                                    completesTarget = false,
                                    commit = { savePendingAsFree() },
                                )
                            },
                            onSaveTopic   = { markDone ->
                                pendingSaveConfirmation = PendingSaveConfirmation(
                                    label = pending?.topicTitle ?: "this topic",
                                    linksTopic = true,
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
                                        clearAssociations(discard = true)
                                    }
                                }
                            },
                        )

                        pendingSaveConfirmation?.let { confirmation ->
                            EkagraConfirmSaveDialog(
                                label = confirmation.label,
                                completesTarget = confirmation.completesTarget,
                                keepsGoalOpen = confirmation.keepsGoalOpen,
                                linksTopic = confirmation.linksTopic,
                                accentColor = themeColorScheme.primary,
                                onConfirm = {
                                    pendingSaveConfirmation = null
                                    confirmation.commit()
                                },
                                onCancel = { pendingSaveConfirmation = null },
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
                            isDarkTheme = isDarkTheme,
                            onDismiss = {
                                savedSessionId?.let { timerService?.acknowledgeCompletedSession(it) }
                                showPostSaveGoalLinking = false
                                savedSessionId = null
                                viewModel.loadEkagraAnalytics()
                            },
                            onLinkGoal = { goal, markComplete ->
                                val sessionId = savedSessionId!!
                                viewModel.linkSavedSessionToGoal(sessionId, goal, markComplete) { saved ->
                                    if (saved) {
                                        timerService?.acknowledgeCompletedSession(sessionId)
                                        showPostSaveGoalLinking = false
                                        savedSessionId = null
                                    } else {
                                        ekagraScope.launch { snackbarHostState.showSnackbar("Could not update the goal. Please try again.") }
                                    }
                                }
                            },
                        )
                    }

                    if (showDurationPromptDialog) {
                        val dialogInk = rememberEkagraInk(onCanvas = false)
                        com.safarparmar.app.ui.components.popups.SafarDialog(
                            onDismissRequest = { showDurationPromptDialog = false },
                            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                        ) {
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
                                    .padding(horizontal = 24.dp)
                                    .widthIn(max = 380.dp)
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .border(1.dp, dialogInk.hairline, RoundedCornerShape(20.dp)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        start = 22.dp, end = 22.dp,
                                        top = 22.dp, bottom = 20.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(0.dp),
                                ) {
                                    EkagraEyebrow("Ekagra", dialogInk.secondaryText)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.ekagra_set_duration_question),
                                        fontFamily = EkagraSerif,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = dialogTitle,
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    // Body
                                    Text(
                                        text = stringResource(R.string.ekagra_set_duration_body),
                                        fontSize = 13.5.sp,
                                        color = dialogBody,
                                        lineHeight = 19.sp,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    // "Do not show this again" checkbox row
                                    com.composables.ui.components.Checkbox(
                                        checked = dontShowDurationPromptAgain,
                                        onCheckedChange = { dontShowDurationPromptAgain = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        accessibilityLabel = stringResource(R.string.common_do_not_show_again),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.common_do_not_show_again),
                                            fontSize = 13.sp,
                                            color = dialogInk.primaryText,
                                        )
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    // Action buttons — hairline styled
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // "Start anyway" — secondary action
                                        EkagraGhostAction(
                                            label = stringResource(R.string.ekagra_start_anyway),
                                            ink = dialogInk,
                                            onClick = {
                                                if (dontShowDurationPromptAgain) viewModel.disableDurationPrompt()
                                                showDurationPromptDialog = false
                                                durationPromptActedOn = true

                                                val wasInactive = timerService?.isActive() == false
                                                if (wasInactive) {
                                                    requestNotificationPermission()
                                                    if (timerMode != TimerMode.BREAK) prepareStudySession()
                                                }
                                                timerService?.togglePlayPause()
                                                if (wasInactive && timerMode != TimerMode.BREAK) {
                                                    viewModel.onSessionStarted(taskText, totalSeconds,
                                                        associatedGoalId, associatedGoalTitle, timerMode.toApiMode(), clientSessionId = timerService?.currentSessionId())
                                                } else if (timerMode == TimerMode.FOCUS) {
                                                    viewModel.syncActiveSession(totalSeconds, secondsLeft, timerMode.toApiMode(),
                                                        true, associatedGoalTitle ?: taskText.takeIf { it.isNotBlank() })
                                                }
                                            },
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        // "Yes" — primary action
                                        EkagraPrimaryAction(
                                            label = stringResource(R.string.ekagra_yes),
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
                        onCanvas = false,
                        theme = selectedTheme,
                        isDarkTheme = isDarkTheme,
                    )
                    val topBarTint = headerInk.primaryText
                    var openDrawer by remember { mutableStateOf<() -> Unit>({}) }

                    // Guide + overflow actions — unchanged in behaviour, just moved
                    // out of the old glass bar and into EkagraTopBar's trailing slot.
                    val ekagraTopBarActions: @Composable RowScope.() -> Unit = {
                        val tintColor = topBarTint
                        com.safarparmar.app.ui.tutorial.TitliGuideButton()
                        var showOverflowMenu by remember { mutableStateOf(false) }
                        com.safarparmar.app.ui.components.SafarOverflowMenu(
                            expanded = showOverflowMenu,
                            onExpandedChange = { showOverflowMenu = it },
                            actions = buildList {
                                if (com.safarparmar.app.BuildConfig.TITLI_TUTORIALS_ENABLED) add(com.safarparmar.app.ui.components.SafarMenuAction(
                                    label = stringResource(R.string.ekagra_tutorial_learn),
                                    onClick = { tutorialModel.open() },
                                    icon = androidx.compose.material.icons.Icons.Default.HelpOutline,
                                ))
                                add(com.safarparmar.app.ui.components.SafarMenuAction(
                                    label = stringResource(if (isMuted) R.string.ekagra_volume_on else R.string.ekagra_volume_off),
                                    onClick = { timerService?.setMute(!isMuted) },
                                    icon = if (isMuted) androidx.compose.material.icons.Icons.Default.VolumeUp else androidx.compose.material.icons.Icons.Default.VolumeOff,
                                ))
                                add(com.safarparmar.app.ui.components.SafarMenuAction(
                                    label = if (overlayGranted.value) "Floating timer settings" else "Enable floating timer",
                                    onClick = {
                                        if (overlayGranted.value) TimerBubbleOverlay.openOverlayPermissionSettings(pipContext)
                                        else showOverlayPermPrompt = true
                                    },
                                    icon = androidx.compose.material.icons.Icons.Default.PictureInPictureAlt,
                                ))
                                if (!shieldState.hasUsageStats || !shieldState.hasOverlayPermission) {
                                    add(com.safarparmar.app.ui.components.SafarMenuAction(
                                        label = stringResource(R.string.ekagra_kavach_setup),
                                        onClick = { onNavigate(com.safarparmar.app.ui.navigation.Routes.FOCUS_SHIELD) },
                                        icon = androidx.compose.material.icons.Icons.Default.Shield,
                                    ))
                                }
                                add(com.safarparmar.app.ui.components.SafarMenuAction(
                                    label = stringResource(R.string.ekagra_apps_to_block),
                                    onClick = { onNavigate(Routes.APP_PICKER) },
                                    icon = androidx.compose.material.icons.Icons.Default.Apps,
                                ))
                            },
                            anchor = {
                                com.composables.ui.components.IconButton(style = com.composables.ui.components.ButtonStyle.Ghost, onClick = { showOverflowMenu = !showOverflowMenu }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = stringResource(R.string.common_more_options), tint = tintColor)
                                }
                            },
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                    ) {
                        Box(Modifier.fillMaxSize().ekagraThemeBackground(LocalEkagraWarmPalette.current))
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


                            Scaffold(
                                containerColor      = Color.Transparent,
                                contentWindowInsets = WindowInsets.safeDrawing,
                                snackbarHost        = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    EkagraTopBar(
                                        ink = rememberEkagraInk(onCanvas = false),
                                        onOpenDrawer = { openDrawer() },
                                        trailing = ekagraTopBarActions,
                                    )
                                },
                                bottomBar = {
                                    EkagraBottomNav(
                                        selectedTab = ekagraNavigationSelection(selectedTab, showAudioLibraryPanel, showThemeDialog),
                                        onSelect    = { tab ->
                                            when (tab) {
                                                EkagraNavTab.MUSIC -> showAudioLibraryPanel = true
                                                EkagraNavTab.THEME -> showThemeDialog = true
                                                else -> tabBackStack.select(tab)
                                            }
                                        },
                                        // On timer tab the nav sits over the video scrim — use contrasting colours
                                        isOnVideo   = selectedTab == EkagraNavTab.TIMER,
                                        isDarkTheme = isDarkTheme,
                                        selectedTheme = selectedTheme,
                                    )
                                },
                            ) { innerPadding ->
                                EkagraTabContent(selectedTab) { tab ->
                                when (tab) {

                                    EkagraNavTab.TIMER -> TimerFocusTab(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = innerPadding.calculateTopPadding(),
                                                     bottom = innerPadding.calculateBottomPadding()),
                                        timerMode          = displayedTimerMode,
                                        selectedDisplayMode = selectedDisplayMode,
                                        secondsLeft        = displayedSeconds,
                                        isRunning          = timerRunning && !isBrowsingOtherMode,
                                        progress           = if (isBrowsingOtherMode) 0f else progress,
                                        hasProgress        = !isBrowsingOtherMode && (if (timerMode == TimerMode.STOPWATCH) secondsLeft > 0 else secondsLeft < totalSeconds),
                                        mottoText          = displayedMottoText,
                                        plannerTopicTitle = associatedTopicTitle,
                                        kavachActive       = shieldState.isProtectionActive && timerRunning && (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO),
                                        kavachBlockedCount = blockedHitCount,
                                        controlsVisible    = true,
                                        onOpenKavachSession = { showKavachActiveSession = true },
                                        onModeChange = { mode ->
                                            if (mode == selectedDisplayMode) return@TimerFocusTab
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            // ── Tab-switch persistence fix ──────────────────────────────
                                            // If a session is active, ONLY update the displayed pill — do
                                            // NOT touch the running service. The session keeps ticking.
                                            // Only when there is no active session do we configure the
                                            // service for the newly selected mode.
                                            if (timerService?.isActive() == true) {
                                                selectedDisplayMode = mode
                                                return@TimerFocusTab
                                            }
                                            // No active session — configure service duration for the new mode.
                                            selectedDisplayMode = mode
                                            val mins = when (mode) {
                                                TimerMode.FOCUS, TimerMode.POMODORO -> focusMinutes
                                                TimerMode.BREAK -> breakMinutes
                                                TimerMode.STOPWATCH  -> 0
                                            }
                                            if (mode == TimerMode.FOCUS && timerService?.switchToFocusFromBreak() == true) {
                                                // switchToFocusFromBreak handled internally; nothing extra to do
                                            } else {
                                                timerService?.setDuration(mode, mins * 60, breakMinutes * 60)
                                            }
                                        },
                                        onPlayPause = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val wasRunning  = timerRunning
                                            val wasInactive = timerService?.isActive() == false

                                            // ── Mode-switch guard ──────────────────────────────────────
                                            // If the user has browsed to a different mode tab while a
                                            // session is already active, pressing Play should ask them
                                            // whether they want to end the running session before starting
                                            // the newly selected mode. We must NOT silently reset things.
                                            if (!wasInactive && selectedDisplayMode != activePillMode) {
                                                pendingModeSwitchConfirm = selectedDisplayMode
                                                return@TimerFocusTab
                                            }

                                            if (wasInactive && showDurationPrompt && !durationPromptActedOn && (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO)) {
                                                showDurationPromptDialog = true
                                                return@TimerFocusTab
                                            }

                                            if (wasInactive) {
                                                requestNotificationPermission()
                                                if (timerMode != TimerMode.BREAK) prepareStudySession()
                                                timerService?.togglePlayPause()
                                                showLongSessionIntroIfNeeded(timerMode, totalSeconds)
                                                if (timerMode == TimerMode.FOCUS || timerMode == TimerMode.STOPWATCH || timerMode == TimerMode.POMODORO) {
                                                    viewModel.onSessionStarted(taskText, totalSeconds, associatedGoalId, associatedGoalTitle, timerMode.toApiMode(), clientSessionId = timerService?.currentSessionId())
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
                                        canStartBreak = !isBrowsingOtherMode && (timerMode == TimerMode.FOCUS || timerMode == TimerMode.POMODORO) && timerService?.isActive() == true,
                                        isBrowsingOtherMode = isBrowsingOtherMode,
                                        activeModeLabel = stringResource(timerMode.labelRes),
                                        onStartBreak  = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            timerService?.startBreak(TimerMode.BREAK, breakMinutes * 60)
                                        },
                                        onReset       = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showEndSessionConfirmDialog = true
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
                                        onStartPomodoro = startPomodoro@{ request ->
                                            requestNotificationPermission()
                                            if (request.style == PomodoroStyle.CUSTOM) {
                                                focusMinutes = request.focusMinutes.coerceIn(1, 120)
                                                breakMinutes = request.breakMinutes.coerceIn(1, 60)
                                                viewModel.setFocusDurationMinutes(focusMinutes)
                                                viewModel.setBreakDurationMinutes(breakMinutes)
                                            }
                                            val service = timerService
                                            if (service == null) {
                                                ekagraScope.launch { snackbarHostState.showSnackbar("Timer is still loading. Please try again.") }
                                                return@startPomodoro
                                            }
                                            val clientSessionId = service.startPomodoroSession(
                                                style = request.style,
                                                loops = request.loops,
                                                focusMinutes = request.focusMinutes,
                                                breakMinutes = request.breakMinutes,
                                                taskTitle = associatedGoalTitle ?: associatedTopicTitle ?: taskText.takeIf { it.isNotBlank() },
                                                goalId = associatedGoalId,
                                                goalTitle = associatedGoalTitle,
                                                topicId = associatedTopicId,
                                                planId = associatedPlanId,
                                                topicTitle = associatedTopicTitle,
                                            )
                                            if (clientSessionId == null) {
                                                ekagraScope.launch { snackbarHostState.showSnackbar("Pomodoro could not start. End the current timer or try again.") }
                                                return@startPomodoro
                                            }
                                            showLongSessionIntroIfNeeded(TimerMode.POMODORO, service.totalSeconds.value)
                                            viewModel.onSessionStarted(
                                                taskText = taskText,
                                                totalSeconds = service.totalSeconds.value,
                                                goalId = associatedGoalId,
                                                goalTitle = associatedGoalTitle,
                                                mode = TimerMode.POMODORO.toApiMode(),
                                                clientSessionId = clientSessionId,
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
            EkagraTutorialEntry(
                dataStore = viewModel.dataStore,
                eligible = ekagraTutorialEligible(
                    serviceReady = timerService != null,
                    sessionActive = timerRunning || timerService?.isActive() == true || activeSession != null,
                    flowPending = showKavachActiveSession || showThemeDialog || showEndSessionConfirmDialog ||
                        showAudioLibraryPanel || showOrganizeSheet || showTopicStudySheet || showPostSaveGoalLinking ||
                        showDurationPromptDialog || showOverlayPermPrompt || showMusicPromptDialog ||
                        pendingModeSwitchConfirm != null || pendingStartTimerArgs != null ||
                        pendingEndedSession != null || completedSession != null,
                ),
                isDarkTheme = isDarkTheme,
                model = tutorialModel,
                selectedTheme = selectedTheme,
            )
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

    com.safarparmar.app.ui.components.popups.SafarComposablesAlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.ekagra_play_ambient_question),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.ekagra_play_ambient_body),
                    fontSize = 14.sp
                )
                com.composables.ui.components.Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = { dontShowAgain = it },
                    modifier = Modifier.fillMaxWidth(),
                    accessibilityLabel = stringResource(R.string.common_do_not_show_again),
                ) {
                    Text(
                        text = stringResource(R.string.common_do_not_show_again),
                        fontSize = 14.sp
                    )
                }
            }
        },
        positiveButton= {
            com.composables.ui.components.Button(
                onClick = { onYes(dontShowAgain) }
            ) {
                Text(stringResource(R.string.common_yes), fontWeight = FontWeight.SemiBold)
            }
        },
        negativeButton= {
            com.composables.ui.components.Button(style = com.composables.ui.components.ButtonStyle.Ghost,
                onClick = { onNo(dontShowAgain) }
            ) {
                Text(androidx.compose.ui.res.stringResource(com.safarparmar.app.R.string.common_no), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}


// ─── PiP overlay ───────────────────────────────────────────────────────────────
