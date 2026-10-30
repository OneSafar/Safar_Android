package com.safarparmar.app.ui.ekagra

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.safarparmar.app.ui.audio.FocusAwareMusicPlayer
import android.media.AudioAttributes
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.local.TimerAlertStyle
import com.safarparmar.app.data.remote.api.FocusApi
import com.safarparmar.app.data.remote.dto.FocusPresenceRequest
import com.safarparmar.app.data.remote.dto.EkagraSession
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldEntryPoint
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import com.safarparmar.app.ui.ekagra.focusshield.NotificationShieldPrefs
import com.safarparmar.app.ui.ekagra.focusshield.BlockedMediaEnforcer
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRecorder
import com.safarparmar.app.feature.kavachanalytics.domain.KavachSessionOutcome
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import android.widget.RemoteViews
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt
import com.safarparmar.app.ui.audio.MediaFileCache

@AndroidEntryPoint
class TimerService : Service() {

    @Inject lateinit var focusApi: FocusApi

    companion object {
        const val CHANNEL_ID        = SafarNotificationChannels.FOCUS_TIMER
        const val NOTIFICATION_ID   = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
        const val FOCUS_SHIELD_BLOCKED_NOTIFICATION_ID = 1003
        const val FOCUS_SHIELD_ACTIVE_NOTIFICATION_ID = 1004
        const val PRESENCE_NOTIFICATION_ID = 1005
        const val POMODORO_TRANSITION_NOTIFICATION_ID = 1006
        const val ACTION_PLAY_PAUSE = "com.safar.ekagra.ACTION_PLAY_PAUSE"
        const val ACTION_DECLINE_PRESENCE = "com.safar.ekagra.ACTION_DECLINE_PRESENCE"
        const val ACTION_CONFIRM_PRESENCE = "com.safar.ekagra.ACTION_CONFIRM_PRESENCE"
        const val ACTION_QUICK_UNLOCK = "com.safar.ekagra.ACTION_QUICK_UNLOCK"
        const val EXTRA_QUICK_UNLOCK_UNTIL = "quickUnlockUntil"
        const val ACTION_PAUSE = "com.safar.ekagra.ACTION_PAUSE"
        const val ACTION_END_SAVE = "com.safar.ekagra.ACTION_END_SAVE"
        const val ACTION_RECOVER = "com.safar.ekagra.ACTION_RECOVER"
        internal var live: TimerService? = null
        const val ACTION_RESET      = "com.safar.ekagra.ACTION_RESET"
        const val ACTION_FOCUS_SHIELD_BLOCKED = "com.safar.ekagra.ACTION_FOCUS_SHIELD_BLOCKED"
        private const val TIMER_STATE_PREFS = "ekagra_timer_state_prefs"
        private const val KEY_HAS_STATE = "has_state"
        private const val KEY_MODE = "mode"
        private const val KEY_TOTAL_SECONDS = "total_seconds"
        private const val KEY_REMAINING_SECONDS = "remaining_seconds"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_SAVED_AT_MS = "saved_at_ms"
        private const val KEY_SAVED_ELAPSED_MS = "saved_elapsed_ms"
        private const val KEY_SAVED_BOOT_COUNT = "saved_boot_count"
        @Volatile private var bootCountCache: Int? = null
        private fun bootCount(context: Context): Int? = bootCountCache ?: runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrNull().also { bootCountCache = it }

        private fun persistedElapsedSeconds(context: Context, prefs: SharedPreferences): Long? {
            if (prefs.contains(KEY_SAVED_BOOT_COUNT)) {
                val currentBoot = bootCount(context) ?: return null
                if (prefs.getInt(KEY_SAVED_BOOT_COUNT, -1) != currentBoot) return null
                val anchor = prefs.getLong(KEY_SAVED_ELAPSED_MS, -1L)
                if (anchor < 0 || SystemClock.elapsedRealtime() < anchor) return null
                return (SystemClock.elapsedRealtime() - anchor) / 1000L
            }
            // Read compatibility for checkpoints written before monotonic recovery.
            return ((System.currentTimeMillis() - prefs.getLong(KEY_SAVED_AT_MS, System.currentTimeMillis())) / 1000L).coerceAtLeast(0L)
        }
        private const val KEY_SUSPENDED_TOTAL_SECONDS = "suspended_total_seconds"
        private const val KEY_SUSPENDED_REMAINING_SECONDS = "suspended_remaining_seconds"
        private const val KEY_STANDARD_BREAK_SECONDS = "standard_break_seconds"
        private const val KEY_TARGET_POMODORO_LOOPS = "target_pomodoro_loops"
        private const val KEY_COMPLETED_POMODORO_LOOPS = "completed_pomodoro_loops"
        private const val KEY_POMODORO_FOCUS_SECONDS = "pomodoro_focus_seconds"
        private const val KEY_POMODORO_BREAK_SECONDS = "pomodoro_break_seconds"
        private const val TIMER_STATE_WRITE_INTERVAL_MS = 5_000L
        private const val KEY_AUTO_SAVE_CLIENT_SESSION_ID = "auto_save_client_session_id"
        private const val KEY_AUTO_SAVE_STARTED_AT = "auto_save_started_at"
        private const val KEY_AUTO_SAVE_TASK_TITLE = "auto_save_task_title"
        private const val KEY_AUTO_SAVE_GOAL_ID = "auto_save_goal_id"
        private const val KEY_AUTO_SAVE_GOAL_TITLE = "auto_save_goal_title"
        private const val KEY_AUTO_SAVE_TOPIC_ID = "auto_save_topic_id"
        private const val KEY_AUTO_SAVE_PLAN_ID = "auto_save_plan_id"
        private const val KEY_AUTO_SAVE_TOPIC_TITLE = "auto_save_topic_title"
        private const val DEFAULT_UNTITLED_SESSION_TITLE = "Untitled"
        private val KNOWN_HOME_PACKAGES = setOf(
            "com.miui.home",
            "com.mi.android.globallauncher",
            "com.android.launcher",
            "com.android.launcher2",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.vivo.launcher",
            "com.transsion.XOSLauncher",
        )

        fun isFocusTimerRunning(context: Context): Boolean {
            val prefs = context.getSharedPreferences(TIMER_STATE_PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_HAS_STATE, false)) return false
            if (!prefs.getBoolean(KEY_IS_RUNNING, false)) return false
            val elapsed = persistedElapsedSeconds(context, prefs) ?: return false
            val modeStr = prefs.getString(KEY_MODE, TimerMode.FOCUS.name)
            if (
                modeStr != TimerMode.FOCUS.name &&
                modeStr != TimerMode.STOPWATCH.name &&
                modeStr != TimerMode.POMODORO.name
            ) return false

            if (modeStr == TimerMode.STOPWATCH.name) return true

            val total = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60).coerceAtLeast(1)
            val savedRemaining = prefs.getInt(KEY_REMAINING_SECONDS, total).coerceIn(0, total)
            return savedRemaining - elapsed > 0
        }

        /** Notification Shield remains active through every focus, break, and paused period. */
        fun isKavachNotificationSuppressionActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(TIMER_STATE_PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_HAS_STATE, false)) return false
            val elapsed = persistedElapsedSeconds(context, prefs) ?: return false

            val modeStr = prefs.getString(KEY_MODE, TimerMode.FOCUS.name)
            val isBreak = modeStr == TimerMode.BREAK.name
            val isFocusPeriod = modeStr == TimerMode.FOCUS.name ||
                modeStr == TimerMode.STOPWATCH.name ||
                modeStr == TimerMode.POMODORO.name
            if (!isFocusPeriod && !isBreak) return false
            if (prefs.getBoolean(KEY_IS_RUNNING, false) && modeStr != TimerMode.STOPWATCH.name) {
                val total = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60).coerceAtLeast(1)
                val savedRemaining = prefs.getInt(KEY_REMAINING_SECONDS, total).coerceIn(0, total)
                if (savedRemaining - elapsed <= 0) return false
            }
            return true
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder  = TimerBinder()
    private val timerErrors = CoroutineExceptionHandler { _, error ->
        EkagraDiagnostics.recordFailure(this, "timer_coroutine_failed", error)
        // The durable timer clock continues independently of a failed display or
        // optional integration callback. The next wake-up retries its ticker.
        if (_isRunning.value) scheduleTimerAlarms()
    }
    private val scope   = CoroutineScope(Dispatchers.Main + SupervisorJob() + timerErrors)
    private val notificationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private data class NotificationUpdate(val generation: Long, val notification: Notification?, val removed: CompletableDeferred<Unit>? = null)
    private val notificationUpdates = Channel<NotificationUpdate>(Channel.UNLIMITED)
    @Volatile private var notificationGeneration = 0L
    private var lastNotificationPresentation: List<Any?>? = null
    private var terminalSave: Deferred<Unit>? = null
    private var closingSession = false
    private var endingJob: Job? = null
    private var terminalCleanupRetry = false
    private var restoredStartAnchor: Pair<Long, Long>? = null
    private var destroyingService = false
    private var notificationBuildCount = 0
    internal var elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() }
    private val persistenceScope = CoroutineScope(Dispatchers.IO + SupervisorJob() +
        CoroutineExceptionHandler { _, error -> EkagraDiagnostics.recordFailure(this, "timer_preferences_failed", error) })
    private val timerStateWrites = Channel<TimerStateWrite>(Channel.CONFLATED)
    private var persistenceWriterJob: Job? = null
    private var lastPeriodicStateWriteElapsedMs = 0L
    private val rankedTracker by lazy { RankedFocusTracker(focusApi, scope) }
    val rankedStatus: StateFlow<RankedFocusStatus> get() = rankedTracker.status
    private var breakActionId: String? = null
    fun currentSessionId(): String? = autoSaveMetadata?.clientSessionId ?: breakActionId
    fun currentSessionStartedAt(): String? = autoSaveMetadata?.startedAt
    fun hasQueuedSessionSave(): Boolean = sessionSaveQueuedThisRun

    /** Reconnect screen state to the service without replacing its durable identity. */
    internal fun activeSessionDraft(): EkagraSession? {
        val metadata = autoSaveMetadata ?: return null
        if (!recoveryReady || sessionSaveQueuedThisRun || sessionOwnerId != signedInUserId) return null
        val progress = focusProgressSnapshot()
        val stopwatch = _timerMode.value == TimerMode.STOPWATCH
        val runningFocus = _isRunning.value && _timerMode.value != TimerMode.BREAK
        return EkagraSession(
            id = metadata.clientSessionId, userId = sessionOwnerId.orEmpty(),
            goalId = metadata.goalId, goalTitle = metadata.goalTitle,
            sessionType = if (metadata.goalId.isNullOrBlank()) "named" else "goal",
            sessionTitle = metadata.taskTitle ?: metadata.goalTitle ?: "Untitled",
            source = if (metadata.goalId.isNullOrBlank()) "manual" else "goal_continue",
            status = if (runningFocus) "active" else "paused",
            mode = if (stopwatch) TimerMode.STOPWATCH.toApiMode() else TimerMode.FOCUS.toApiMode(),
            totalSeconds = progress.plannedSeconds,
            remainingSeconds = if (stopwatch) progress.actualSeconds else progress.plannedSeconds - progress.actualSeconds,
            isRunning = runningFocus, sessionStartedAt = metadata.startedAt,
            createdAt = metadata.startedAt, updatedAt = Instant.now().toString(),
        )
    }

    private fun syncRankedFocus(confirm: Boolean = false, close: Boolean = false) {
        val id = currentSessionId() ?: return
        rankedTracker.update(id, _isRunning.value && _timerMode.value != TimerMode.BREAK, confirm, close)
    }

    private var sessionOwnerId: String? = null
    private var signedInUserId: String? = null
    private var recoveryReady = false
    private var alarmGeneration = 0L
    private var presenceDueElapsed = 0L
    private var periodDueElapsed = 0L
    private var presenceExpiryElapsed = 0L
    private var periodStartRemaining = 0
    private var periodStartElapsed = 0L
    private var periodStartAtMs = 0L
    private var presenceStartSeconds = 0
    private var saveFailure: String? = null
    private var reconcilingElapsedTime = false
    private var restoredOwner: String? = null
    private var foregroundRecoveryRequested = false
    private var pendingServiceAction: Intent? = null
    private var checkpointSequence = 0L
    private val recoveryBootCount: Int? by lazy {
        bootCount(this)
    }
    private var presenceSeconds = 0
    private val _presenceCheckInDeadline = MutableStateFlow<Long?>(null)
    val presenceCheckInDeadline: StateFlow<Long?> = _presenceCheckInDeadline
    private val _presencePromptDismissed = MutableStateFlow(false)
    val presencePromptDismissed: StateFlow<Boolean> = _presencePromptDismissed
    private var presenceCheckInExpired = false
    private var presenceExpiryJob: Job? = null
    private var tickJob: Job? = null
    private var focusPresenceJob: Job? = null
    private var lastTickElapsedMs: Long = 0L
    private val safarDataStore by lazy { SafarDataStore(applicationContext) }
    private var suspendedFocusState: SuspendedFocusState? = null
    private var cachedUserName: String = ""
    private var autoSaveMetadata: AutoSaveMetadata? = null
    private var sessionSaveQueuedThisRun: Boolean = false

    private data class SuspendedFocusState(
        val totalSeconds: Int,
        val remainingSeconds: Int,
    )

    private sealed interface TimerStateWrite {
        data class Save(
            val total: Int,
            val remaining: Int,
            val mode: String,
            val isRunning: Boolean,
            val savedAtMs: Long,
            val savedElapsedMs: Long,
            val savedBootCount: Int,
            val presenceSeconds: Int,
            val presenceDeadlineAtMs: Long,
            val presenceExpired: Boolean,
            val suspendedTotal: Int,
            val suspendedRemaining: Int,
            val standardBreakSeconds: Int,
            val targetPomodoroLoops: Int,
            val completedPomodoroLoops: Int,
            val pomodoroFocusSeconds: Int,
            val pomodoroBreakSeconds: Int,
        ) : TimerStateWrite

        data object Clear : TimerStateWrite
    }

    private data class AutoSaveMetadata(
        val clientSessionId: String,
        val startedAt: String,
        val taskTitle: String?,
        val goalId: String?,
        val goalTitle: String?,
        // Study-planner topic link, preserved across process death so a session
        // that crash-recovers still credits its time to the right topic. The
        // topic is never auto-marked-done here — the user never got to confirm
        // that checkbox — only the association is kept.
        val topicId: String?,
        val planId: String?,
        val topicTitle: String?,
    )

    internal data class PlannerTopicMetadata(
        val clientSessionId: String,
        val startedAt: String,
        val topicId: String,
        val planId: String,
        val topicTitle: String?,
    )

    // ── Exposed state ─────────────────────────────────────────────────────────
    private val _secondsLeft  = MutableStateFlow(25 * 60)
    private val _totalSeconds = MutableStateFlow(25 * 60)
    private val _isRunning    = MutableStateFlow(false)
    private val _timerMode    = MutableStateFlow(TimerMode.FOCUS)
    private val _isMuted      = MutableStateFlow(false)
    private val _targetPomodoroLoops = MutableStateFlow(0)
    private val _pomodorosCompleted = MutableStateFlow(0)
    private val _completedSession = MutableStateFlow<PendingEkagraSessionSave?>(null)
    val completedSession: StateFlow<PendingEkagraSessionSave?> = _completedSession
    fun acknowledgeCompletedSession(id: String) {
        if (_completedSession.value?.clientSessionId == id) _completedSession.value = null
    }

    private val _pomodoroCompletionEvent = MutableStateFlow(0)
    private var pomodoroFocusSeconds = 25 * 60
    private var pomodoroBreakSeconds = 5 * 60
    private var pomodoroFinalBreakSeconds = 15 * 60
    private var pomodoroCompletesAfterCurrentBreak = false

    val secondsLeft:  StateFlow<Int>       = _secondsLeft
    val totalSeconds: StateFlow<Int>       = _totalSeconds
    val isRunning:    StateFlow<Boolean>   = _isRunning
    val timerMode:    StateFlow<TimerMode> = _timerMode
    val isMuted:      StateFlow<Boolean>   = _isMuted
    val targetPomodoroLoops: StateFlow<Int> = _targetPomodoroLoops
    val pomodorosCompleted: StateFlow<Int> = _pomodorosCompleted
    val pomodoroCompletionEvent: StateFlow<Int> = _pomodoroCompletionEvent

    fun acknowledgePomodoroCompletion() { _pomodoroCompletionEvent.value = 0 }
    
    private var standardBreakSeconds = 5 * 60
    private var autoStartBreak = true // default: auto-start breaks
    private var timerAlertStyle = TimerAlertStyle.SOUND

    // ── Focus Shield state ─────────────────────────────────────────────────

    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─
    private fun themePrefs() = getSharedPreferences("ekagra_theme_prefs", MODE_PRIVATE)

    private fun debugFocusShield(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("FocusShield", message)
    }


    /**
     * Closes the Kavach analytics session with an explicit outcome.
     *
     * Only the three real endings are reported: the timer finished, the student
     * ended it, or the process/device died. A pause, a break, or a lost permission
     * is never any of these — those keep the session open or flag it, so nothing a
     * student didn't choose is ever presented back to them as giving up.
     */
    private fun endKavachAnalyticsSession(outcome: KavachSessionOutcome) {
        val actualSeconds = runCatching { focusProgressSnapshot().actualSeconds }.getOrDefault(0)
        val recorder = runCatching { KavachAnalyticsRecorder.from(applicationContext) }.getOrNull()
        if (recorder != null) {
            when (outcome) {
                KavachSessionOutcome.COMPLETED -> recorder.sessionCompleted(actualSeconds)
                KavachSessionOutcome.ENDED_EARLY -> recorder.sessionEndedEarly(actualSeconds)
                KavachSessionOutcome.INTERRUPTED -> recorder.sessionInterrupted(actualSeconds)
            }
        }
        focusShieldRepository().deactivateSession()
    }

    private fun focusShieldRepository(): FocusShieldRepository =
        EntryPointAccessors.fromApplication(
            applicationContext,
            FocusShieldEntryPoint::class.java,
        ).focusShieldRepository()

    // ── Audio player (lives in the service — survives navigation) ─────────────
    private var musicPlayer: FocusAwareMusicPlayer? = null
    private var quickUnlockResume: QuickUnlockResume? = null
    private var quickUnlockResumeJob: kotlinx.coroutines.Job? = null
    private var currentMusicUrl: String   = ""

    /**
     * Brief WakeLock acquired when the timer completes so the CPU and audio subsystem
     * stay awake long enough to play the alert sound when the screen is off.
     * Auto-released after 10 seconds (well beyond any alert sound duration).
     */
    private var completionWakeLock: PowerManager.WakeLock? = null

    private fun acquireCompletionWakeLock() {
        // Release any lingering lock from a previous completion first
        completionWakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        completionWakeLock = (getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "safar:EkagraTimerCompletion")
            ?.also { lock -> lock.acquire(10_000L) }
    }

    fun setMute(mute: Boolean) {
        _isMuted.value = mute
        if (_isRunning.value && musicPlayer != null) {
            val volume = if (mute) 0f else 0.7f
            musicPlayer?.setVolume(volume)
        }
    }

    internal fun startPomodoroSession(
        style: PomodoroStyle,
        loops: Int,
        focusMinutes: Int,
        breakMinutes: Int,
        taskTitle: String?,
        goalId: String?,
        goalTitle: String?,
        topicId: String? = null,
        planId: String? = null,
        topicTitle: String? = null,
    ): String? {
        if (closingSession || terminalCleanupRetry || !recoveryReady || isActive() || _targetPomodoroLoops.value > 0) return null

        return try {
            val config = normalizePomodoroConfig(style, loops, focusMinutes, breakMinutes)

            setDuration(TimerMode.POMODORO, config.focusSeconds, config.breakSeconds)
            _targetPomodoroLoops.value = config.loops
            _pomodorosCompleted.value = 0
            pomodoroFocusSeconds = config.focusSeconds
            pomodoroBreakSeconds = config.breakSeconds
            pomodoroFinalBreakSeconds = config.finalBreakSeconds
            pomodoroCompletesAfterCurrentBreak = false
            prepareAutoSaveSession(
                taskTitle = taskTitle,
                goalId = goalId,
                goalTitle = goalTitle,
                topicId = topicId,
                planId = planId,
                topicTitle = topicTitle,
                forceNew = true,
            )
            start()
            currentSessionId()?.takeIf { _isRunning.value }
        } catch (error: Exception) {
            Log.e("TimerService", "Could not start Pomodoro session", error)
            runCatching { reset() }
            null
        }
    }

    fun setMusic(url: String) {
        if (url == currentMusicUrl) return
        currentMusicUrl = url
        if (_isRunning.value) startMusic(url) else releaseMusic()
    }

    private fun startMusic(url: String) {
        releaseMusic()
        if (url.isBlank() || url == "silence") return
        try {
            val player = FocusAwareMusicPlayer(this)
            musicPlayer = player
            player.play(
                uri = MediaFileCache.uriFor(this, url),
                looping = true,
                volume = if (_isMuted.value) 0f else 0.7f,
            )
        } catch (error: Exception) {
            Log.w("TimerService", "Could not start background music", error)
            releaseMusic()
        }
    }

    private fun releaseMusic() {
        val player = musicPlayer
        musicPlayer = null
        player?.release()
    }


    // ── Lifecycle ─────────────────────────────────────────────────────────────
    // ── Floating pill visibility: show ONLY when a session is active AND SAFAR is
    //    in the background. Never over our own UI. ──────────────────────────────
    private var appInForeground = true
    private var startedActivityCount = 0
    private var timerSessionActive = false

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivityCount++
            appInForeground = startedActivityCount > 0
            syncBubble()
        }
        override fun onActivityStopped(activity: Activity) {
            startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
            appInForeground = startedActivityCount > 0
            syncBubble()
        }
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    /** Reconciles the floating pill with current state. Cheap; safe to call often. */
    private fun syncBubble() {
        if (timerSessionActive && !appInForeground) {
            TimerBubbleOverlay.show(
                context      = applicationContext,
                secondsLeft  = _secondsLeft.value,
                totalSeconds = _totalSeconds.value,
                kavachActive = FocusShieldRepository.ShieldPrefs.isActive(this),
                isRunning    = _isRunning.value,
                attention = TimerBubbleAttention.NONE,
                timerMode = _timerMode.value,
            )
        } else {
            TimerBubbleOverlay.hide()
        }
    }

    override fun onCreate() {
        super.onCreate()
        live = this
        SafarNotificationChannels.createAll(this)
        persistenceWriterJob = persistenceScope.launch {
            for (write in timerStateWrites) {
                try {
                    val committed = when (write) {
                        is TimerStateWrite.Save -> timerStatePrefs().edit()
                            .putBoolean(KEY_HAS_STATE, true)
                            .putString(KEY_MODE, write.mode)
                            .putInt(KEY_TOTAL_SECONDS, write.total)
                            .putInt(KEY_REMAINING_SECONDS, write.remaining)
                            .putBoolean(KEY_IS_RUNNING, write.isRunning)
                            .putLong(KEY_SAVED_AT_MS, write.savedAtMs)
                            .putLong(KEY_SAVED_ELAPSED_MS, write.savedElapsedMs)
                            .putInt(KEY_SAVED_BOOT_COUNT, write.savedBootCount)
                            .putInt("presence_seconds", write.presenceSeconds)
                            .putLong("presence_deadline", write.presenceDeadlineAtMs)
                            .putBoolean("presence_expired", write.presenceExpired)
                            .remove("presence_paused")
                            .putInt(KEY_SUSPENDED_TOTAL_SECONDS, write.suspendedTotal)
                            .putInt(KEY_SUSPENDED_REMAINING_SECONDS, write.suspendedRemaining)
                            .putInt(KEY_STANDARD_BREAK_SECONDS, write.standardBreakSeconds)
                            .putInt(KEY_TARGET_POMODORO_LOOPS, write.targetPomodoroLoops)
                            .putInt(KEY_COMPLETED_POMODORO_LOOPS, write.completedPomodoroLoops)
                            .putInt(KEY_POMODORO_FOCUS_SECONDS, write.pomodoroFocusSeconds)
                            .putInt(KEY_POMODORO_BREAK_SECONDS, write.pomodoroBreakSeconds)
                            .commit()
                        TimerStateWrite.Clear -> timerStatePrefs().edit().clear().commit()
                    }
                    check(committed) { "Legacy timer state write failed" }
                } catch (error: Exception) {
                    // Room owns recovery. A failed compatibility preference write
                    // must neither kill the timer process nor stop this writer.
                    EkagraDiagnostics.recordFailure(this@TimerService, "legacy_timer_state_write_failed", error)
                }
            }
        }
        notificationScope.launch {
            val notificationManager = getSystemService(NotificationManager::class.java)
            for (update in notificationUpdates) {
                // notify() is a synchronous Binder call. Some OEM notification services can
                // stall it long enough to ANR the app, so never execute it on the main thread.
                try {
                    if (update.generation == notificationGeneration) {
                        if (update.notification == null) notificationManager.cancel(NOTIFICATION_ID)
                        else notificationManager.notify(NOTIFICATION_ID, update.notification)
                    }
                } catch (error: Exception) {
                    EkagraDiagnostics.recordFailure(this@TimerService, "timer_notification_failed", error)
                } finally { update.removed?.complete(Unit) }
            }
        }
        (application as? Application)?.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        scope.launch {
            safarDataStore.userName.collect { name ->
                cachedUserName = name?.trim().orEmpty()
            }
        }
        scope.launch {
            safarDataStore.autoStartBreak.collect { value ->
                autoStartBreak = value
            }
        }
        scope.launch {
            safarDataStore.timerAlertStyle.collect { style ->
                val changed = timerAlertStyle != style
                timerAlertStyle = style
                if (changed && timerSessionActive) updateNotification()
            }
        }
        scope.launch {
            safarDataStore.focusDurationMinutes.collect { minutes ->
                if (!timerSessionActive && _timerMode.value == TimerMode.FOCUS) {
                    _totalSeconds.value = minutes * 60
                    _secondsLeft.value = minutes * 60
                }
            }
        }
        scope.launch {
            safarDataStore.breakDurationMinutes.collect { minutes ->
                standardBreakSeconds = minutes * 60
                if (!timerSessionActive && _timerMode.value == TimerMode.BREAK) {
                    _totalSeconds.value = minutes * 60
                    _secondsLeft.value = minutes * 60
                }
            }
        }
        scope.launch {
            combine(_isRunning, _timerMode) { running, mode ->
                running && (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH || mode == TimerMode.POMODORO)
            }.collect(::syncFocusPresence)
        }
        scope.launch {
            safarDataStore.userId.distinctUntilChanged().collect { user ->
                if (_completedSession.value?.ownerId != user) _completedSession.value = null
                if (sessionOwnerId != null && sessionOwnerId != user && (timerSessionActive || closingSession)) {
                    endAndSave()
                    endingJob?.join()
                    // A natural completion may have started its authorized automatic break.
                    if (timerSessionActive && !closingSession) {
                        endAndSave()
                        endingJob?.join()
                    }
                    // A failed local commit must remain owned by its original account.
                    if (timerSessionActive || closingSession) return@collect
                }
                signedInUserId = user
                if (user.isNullOrBlank()) restoredOwner = null
                recoveryReady = false
                runCatching {
                    EkagraSessionJournal.get(this@TimerService).recover().await()
                    if (!user.isNullOrBlank() && restoredOwner != user) {
                        restoreDurableSession(user)
                        restoredOwner = user
                    }
                    recoveryReady = true
                }.onSuccess {
                        if (foregroundRecoveryRequested && !timerSessionActive && !closingSession) {
                            stopForegroundCompat().await()
                            foregroundRecoveryRequested = false
                            if (!timerSessionActive && !closingSession) stopSelf()
                        }
                        pendingServiceAction?.let { action ->
                            pendingServiceAction = null
                            onStartCommand(action, 0, 0)
                        }
                        EkagraSessionSaveWorker.enqueue(this@TimerService)
                        scope.launch { runCatching { EkagraSessionSaveWorker.drainPendingSaves(applicationContext) } }
                    }
                    .onFailure {
                        recoveryReady = false
                        saveFailure = "Unable to recover study time. Please retry after freeing storage."
                        EkagraDiagnostics.recordFailure(this@TimerService, "timer_restore_failed", it)
                    }
            }
        }
        scope.launch {
            while (true) {
                delay(30_000L)
                if (timerSessionActive) syncRankedFocus()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A sticky restart or alarm starts a fresh Service instance. Promote it
        // immediately, before asynchronous account/journal reads, to meet Android's
        // foreground-start deadline even when storage or authentication is slow.
        if ((intent == null || intent.action == ACTION_RECOVER) && !timerSessionActive && !closingSession) {
            if (recoveryReady) {
                stopForegroundCompat()
                stopSelfResult(startId)
                return START_NOT_STICKY
            }
            foregroundRecoveryRequested = true
            if (!promoteTimerForeground(restoring = true)) {
                // Android would otherwise kill this process for missing the
                // foreground-start deadline. The original journal row stays active.
                scope.cancel()
                stopSelfResult(startId)
                return START_NOT_STICKY
            }
        }
        if (!recoveryReady && intent?.action != null && intent.action != ACTION_RECOVER) {
            pendingServiceAction = Intent(intent)
            return START_STICKY
        }
        when (intent?.action) {
            ACTION_CONFIRM_PRESENCE, ACTION_DECLINE_PRESENCE -> if (
                intent.getStringExtra("session") == currentSessionId() &&
                intent.getLongExtra("presenceDeadline", -1L) == _presenceCheckInDeadline.value
            ) {
                if (intent.action == ACTION_CONFIRM_PRESENCE) confirmPresenceReminder() else declinePresenceReminder()
            }
            ACTION_PLAY_PAUSE -> if (currentSessionId() != null && intent.getStringExtra("session") == currentSessionId() && (timerSessionActive || _timerMode.value == TimerMode.BREAK)) togglePlayPause()
            ACTION_QUICK_UNLOCK -> if (intent.getStringExtra("session") == currentSessionId()) {
                pauseForQuickUnlock(intent.getLongExtra(EXTRA_QUICK_UNLOCK_UNTIL, 0L))
            }
            ACTION_PAUSE -> if (_isRunning.value || quickUnlockResume != null) pause()
            ACTION_RESET, ACTION_END_SAVE -> if (currentSessionId() != null && intent.getStringExtra("session") == currentSessionId() && timerSessionActive) endAndSave()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Removing the UI task is not a request to end a foreground timer.
        reconcileElapsedTime()
        persistTimerState()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        destroyingService = true
        // A sleeping phone may not have run the display ticker since its last
        // checkpoint. Account for the live period before finalizing the service.
        reconcileElapsedTime()
        // Service destruction is not an End tap. Leave the same durable session
        // active, including its monotonic clock, for the sticky/alarm restart.
        persistTimerState()
        cancelQuickUnlockResume()
        if (live === this) live = null
        if (!timerSessionActive) EkagraTimerAlarms.cancel(this)
        cancelPresenceNotification()
        presenceExpiryJob?.cancel()
        (application as? Application)?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        TimerBubbleOverlay.hide()
        releaseMusic()
        completionWakeLock?.let { if (it.isHeld) runCatching { it.release() } }
        completionWakeLock = null
        focusPresenceJob?.cancel()
        notificationUpdates.close()
        notificationScope.cancel()
        timerStateWrites.close()
        persistenceWriterJob?.invokeOnCompletion { persistenceScope.cancel() }
        scope.cancel()
        super.onDestroy()
    }

    private fun syncFocusPresence(active: Boolean) {
        focusPresenceJob?.cancel()
        focusPresenceJob = scope.launch(Dispatchers.IO) {
            if (!active) {
                runCatching { focusApi.setFocusPresence(FocusPresenceRequest(false)) }
                return@launch
            }
            while (true) {
                runCatching { focusApi.setFocusPresence(FocusPresenceRequest(true)) }
                delay(30_000L)
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────
    fun saveTheme(themeIndex: Int, songName: String) {
        persistenceScope.launch {
            themePrefs().edit()
                .putInt("theme_index", themeIndex)
                .putString("song_name", songName)
                .commit()
        }
    }

    fun setDuration(mode: TimerMode, seconds: Int, breakSeconds: Int = 5 * 60) {
        if (!recoveryReady || closingSession || terminalCleanupRetry) return
        if (_isRunning.value) reconcileElapsedTime()
        if (closingSession) return
        saveCurrentProgress("user-ended")
        EkagraTimerAlarms.cancel(this)
        runCatching { getSystemService(NotificationManager::class.java).cancel(POMODORO_TRANSITION_NOTIFICATION_ID) }
        syncRankedFocus(close = true)
        autoSaveMetadata = null
        timerSessionActive = false
        clearPresence()
        // Reconfiguring the timer abandons whatever was running — an explicit choice.
        endKavachAnalyticsSession(KavachSessionOutcome.ENDED_EARLY)
        _timerMode.value    = mode
        val initialSeconds = configuredTimerSeconds(mode, seconds)
        _secondsLeft.value  = initialSeconds
        _totalSeconds.value = initialSeconds
        standardBreakSeconds = breakSeconds
        _isRunning.value    = false
        _targetPomodoroLoops.value = 0
        _pomodorosCompleted.value = 0
        pomodoroCompletesAfterCurrentBreak = false
        sessionSaveQueuedThisRun = false
        suspendedFocusState = null
        tickJob?.cancel()
        releaseMusic()
        stopForegroundCompat()
        clearPersistedTimerState()
    }

    fun restoreSession(mode: TimerMode, totalSeconds: Int, remainingSeconds: Int, running: Boolean) {
        tickJob?.cancel()
        _timerMode.value = mode
        _totalSeconds.value = totalSeconds.coerceAtLeast(60)
        _secondsLeft.value = restoredTimerSeconds(mode, remainingSeconds, _totalSeconds.value)
        _isRunning.value = false
        suspendedFocusState = null
        releaseMusic()
        persistTimerState()
        if (running && _secondsLeft.value > 0) start() else updateNotification()
    }

    fun prepareAutoSaveSession(
        taskTitle: String?,
        goalId: String?,
        goalTitle: String?,
        topicId: String? = null,
        planId: String? = null,
        topicTitle: String? = null,
        forceNew: Boolean = false,
    ) {
        if (!recoveryReady || closingSession || terminalCleanupRetry) return
        if (forceNew) terminalSave = null
        val current = autoSaveMetadata
        val shouldCreate = forceNew || current == null
        autoSaveMetadata = AutoSaveMetadata(
            clientSessionId = if (shouldCreate) "local-${UUID.randomUUID()}" else current.clientSessionId,
            startedAt = if (shouldCreate) Instant.now().toString() else current.startedAt,
            taskTitle = taskTitle?.trim()?.takeIf { it.isNotBlank() } ?: current?.taskTitle,
            goalId = goalId?.takeIf { it.isNotBlank() && !it.startsWith("named:") } ?: current?.goalId,
            goalTitle = goalTitle?.trim()?.takeIf { it.isNotBlank() } ?: current?.goalTitle,
            topicId = topicId?.takeIf { it.isNotBlank() } ?: current?.topicId,
            planId = planId?.takeIf { it.isNotBlank() } ?: current?.planId,
            topicTitle = topicTitle?.trim()?.takeIf { it.isNotBlank() } ?: current?.topicTitle,
        )
        sessionSaveQueuedThisRun = false
        if (shouldCreate) sessionOwnerId = signedInUserId
        persistAutoSaveMetadata()
        if (timerSessionActive) persistTimerState()
    }

    /**
     * The foreground service outlives screen recreation. Use its copy of the
     * planner link when Compose state was recreated while a timer was running.
     */
    internal fun plannerTopicMetadata(): PlannerTopicMetadata? {
        val metadata = autoSaveMetadata ?: return null
        val topicId = metadata.topicId?.takeIf { it.isNotBlank() } ?: return null
        val planId = metadata.planId?.takeIf { it.isNotBlank() } ?: return null
        return PlannerTopicMetadata(
            clientSessionId = metadata.clientSessionId,
            startedAt = metadata.startedAt,
            topicId = topicId,
            planId = planId,
            topicTitle = metadata.topicTitle,
        )
    }

    fun togglePlayPause() {
        if (_isRunning.value) pause() else start()
    }

    fun switchToFocusFromBreak(): Boolean {
        val focusState = suspendedFocusState ?: return _timerMode.value == TimerMode.FOCUS || _timerMode.value == TimerMode.POMODORO
        tickJob?.cancel()
        suspendedFocusState = null
        _timerMode.value = if (_targetPomodoroLoops.value > 0) TimerMode.POMODORO else TimerMode.FOCUS
        _totalSeconds.value = focusState.totalSeconds
        _secondsLeft.value = focusState.remainingSeconds.coerceIn(1, focusState.totalSeconds)
        _isRunning.value = false
        releaseMusic()
        persistTimerState()
        updateNotification()
        return true
    }

    internal fun focusProgressSnapshot(): FocusProgressSnapshot {
        reconcileElapsedTime()
        return durableFocusProgress()
    }

    private fun currentPeriodFocusProgress(): FocusProgressSnapshot = calculateFocusProgress(
        mode = _timerMode.value,
        currentPeriodTotalSeconds = _totalSeconds.value,
        currentPeriodRemainingSeconds = _secondsLeft.value,
        pomodoroFocusSeconds = pomodoroFocusSeconds,
        targetPomodoroLoops = _targetPomodoroLoops.value,
        completedPomodoroLoops = _pomodorosCompleted.value,
    )

    fun startBreak(mode: TimerMode, seconds: Int): Boolean {
        if (closingSession || terminalCleanupRetry || !recoveryReady) return false
        if (mode == TimerMode.FOCUS || mode == TimerMode.POMODORO || seconds <= 0) return false
        // Freeze the focus period at the break tap, including time elapsed while
        // Android suspended the display ticker.
        reconcileElapsedTime()
        if (closingSession) return false

        val focusState = when {
            (_timerMode.value == TimerMode.FOCUS || _timerMode.value == TimerMode.POMODORO) && _secondsLeft.value > 0 -> {
                SuspendedFocusState(
                    totalSeconds = _totalSeconds.value,
                    remainingSeconds = _secondsLeft.value,
                )
            }
            suspendedFocusState != null -> suspendedFocusState
            else -> null
        } ?: return false

        suspendedFocusState = focusState
        tickJob?.cancel()
        releaseMusic()
        _timerMode.value = mode
        _totalSeconds.value = seconds.coerceAtLeast(60)
        _secondsLeft.value = _totalSeconds.value
        _isRunning.value = false
        persistTimerState()
        start()
        return true
    }

    internal fun retryRecovery() {
        scope.launch {
            runCatching {
                    EkagraSessionJournal.get(this@TimerService).recover().await()
                    if (terminalCleanupRetry) {
                        EkagraSessionJournal.get(this@TimerService).retryWrites().await()
                        terminalSave = null
                        sessionSaveQueuedThisRun = false
                        recoveryReady = true
                        reset()
                        recoveryReady = false
                        endingJob?.join()
                        if (!terminalCleanupRetry) {
                            // An account change may have waited on the failed End. Do not
                            // leave the old account bound after its retry finally commits.
                            val currentAccount = safarDataStore.userId.first()
                            if (signedInUserId != currentAccount) {
                                signedInUserId = currentAccount
                                restoredOwner = null
                                if (!currentAccount.isNullOrBlank()) {
                                    restoreDurableSession(currentAccount)
                                    restoredOwner = currentAccount
                                }
                            }
                        }
                        recoveryReady = true
                        return@launch
                    }
                    signedInUserId?.takeIf { restoredOwner != it }?.let {
                        restoreDurableSession(it)
                        restoredOwner = it
                    }
                    recoveryReady = true
                    saveFailure = null
                }.onFailure {
                    saveFailure = "Unable to recover study time. Please retry after freeing storage."
                    EkagraDiagnostics.recordFailure(this@TimerService, "timer_restore_failed", it)
                }
        }
    }

    private fun promoteTimerForeground(restoring: Boolean = false): Boolean = try {
        notificationGeneration++
        lastNotificationPresentation = null
        val notification = if (restoring) {
            val open = PendingIntent.getActivity(this, 0,
                Intent(this, com.safarparmar.app.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
                .setContentTitle(getString(R.string.ekagra_restoring_title))
                .setContentText(getString(R.string.ekagra_restoring_body))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).build()
        } else buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 0)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        true
    } catch (error: Exception) {
        EkagraDiagnostics.recordFailure(this, "foreground_start_failed", error)
        false
    }

    /** Restore the same journal identity before any worker can upload its final duration. */
    private suspend fun restoreDurableSession(account: String) {
        val saved = EkagraSessionJournal.get(this).claimRecovery(account) ?: return
        val checkpoint = saved.recovery ?: return
        val exit = EkagraDiagnostics.reportRecovery(this, saved.clientSessionId, checkpoint.checkpointAtMs)
        val safeCheckpoint = if (exit?.isUserStopped == true) checkpoint.copy(
            isRunning = false, quickUnlockUntilElapsedMs = 0L, quickUnlockGraceUntilMs = 0L,
        ) else checkpoint
        val nowElapsed = elapsedRealtime()
        val restored = recoverEkagraTimer(safeCheckpoint, nowElapsed, recoveryBootCount, System.currentTimeMillis())
        val state = restored.state
        checkpointSequence = state.checkpointSequence
        sessionOwnerId = account
        autoSaveMetadata = AutoSaveMetadata(saved.clientSessionId, saved.startedAt, saved.taskTitle,
            saved.goalId, saved.goalTitle, saved.topicId, saved.planId, saved.topicTitle)
        sessionSaveQueuedThisRun = false
        timerSessionActive = true
        _timerMode.value = state.mode
        _totalSeconds.value = state.totalSeconds
        _secondsLeft.value = state.remainingSeconds
        _isRunning.value = false
        suspendedFocusState = state.suspendedTotalSeconds.takeIf { it > 0 }?.let {
            SuspendedFocusState(it, state.suspendedRemainingSeconds.coerceIn(0, it))
        }
        standardBreakSeconds = state.standardBreakSeconds
        _targetPomodoroLoops.value = state.targetPomodoroLoops
        _pomodorosCompleted.value = state.completedPomodoroLoops
        pomodoroFocusSeconds = state.pomodoroFocusSeconds
        pomodoroBreakSeconds = state.pomodoroBreakSeconds
        pomodoroFinalBreakSeconds = state.pomodoroFinalBreakSeconds
        pomodoroCompletesAfterCurrentBreak = state.pomodoroCompletesAfterCurrentBreak
        autoStartBreak = state.autoStartBreak
        presenceSeconds = state.presenceSeconds
        presenceStartSeconds = presenceSeconds
        presenceCheckInExpired = state.presenceExpired
        _presencePromptDismissed.value = state.presencePromptDismissed
        presenceExpiryElapsed = state.presenceDeadlineElapsedMs
        _presenceCheckInDeadline.value = state.presenceDeadlineElapsedMs.takeIf { it > 0 && !state.presenceExpired }?.let {
            System.currentTimeMillis() + (it - nowElapsed)
        }
        val phaseMs = if (restored.sameBoot && state.isRunning) {
            val advanceStart = maxOf(safeCheckpoint.checkpointElapsedMs,
                safeCheckpoint.quickUnlockUntilElapsedMs.takeIf { !safeCheckpoint.isRunning } ?: 0L)
            (nowElapsed - advanceStart).coerceAtLeast(0L) % 1000L
        } else 0L
        periodStartElapsed = nowElapsed - phaseMs
        periodStartAtMs = if (restored.sameBoot && state.isRunning)
            safeCheckpoint.checkpointAtMs + (periodStartElapsed - safeCheckpoint.checkpointElapsedMs)
        else state.checkpointAtMs
        periodStartRemaining = state.remainingSeconds
        // All suspending recovery reads are complete. Public timer mutations can
        // now run without racing a later replacement of the active session.
        recoveryReady = true
        EkagraDiagnostics.record(this, "timer_restored",
            "session=${saved.clientSessionId} same_boot=${restored.sameBoot} running=${state.isRunning} completed=${restored.completed} user_stop=${exit?.isUserStopped == true}")
        if (restored.periodFinished) {
            applyElapsedTransition(restored, nowElapsed)
            if (_isRunning.value) launchTicker()
            return
        }
        persistTimerState()
        if (state.isRunning) {
            restoredStartAnchor = periodStartElapsed to periodStartAtMs
            start()
        } else {
            // A reboot or explicit system Stop leaves the original session paused.
            // It remains resumable and cannot accrue powered-off or paused time.
            promoteTimerForeground()
            if (state.quickUnlockUntilElapsedMs > nowElapsed) {
                val pending = QuickUnlockResume(saved.clientSessionId, state.quickUnlockGraceUntilMs,
                    state.quickUnlockUntilElapsedMs)
                quickUnlockResume = pending
                quickUnlockResumeJob = scope.launch {
                    delay((pending.resumeAtElapsed - elapsedRealtime()).coerceAtLeast(0))
                    resumeExpiredQuickUnlock()
                }
                persistTimerState()
            }
            scheduleTimerAlarms()
            updateNotification()
        }
        _presenceCheckInDeadline.value?.let { deadline ->
            if (!_presencePromptDismissed.value) showPresenceReminderNotification()
            schedulePresenceExpiry(deadline)
        }
    }

    private fun recoveryState(): EkagraTimerRecoveryState {
        val now = elapsedRealtime()
        val advancedSeconds = ((now - periodStartElapsed).coerceAtLeast(0L) / 1000L)
            .coerceAtMost(if (_timerMode.value == TimerMode.STOPWATCH) Long.MAX_VALUE else periodStartRemaining.toLong())
        // Remaining seconds describe a whole-second boundary. Persist that same
        // boundary so recovery cannot shift deadlines or duplicate completed breaks.
        val checkpointElapsed = if (_isRunning.value) periodStartElapsed + advancedSeconds * 1000L else now
        val checkpointAt = if (_isRunning.value && periodStartAtMs > 0)
            periodStartAtMs + advancedSeconds * 1000L else System.currentTimeMillis()
        return EkagraTimerRecoveryState(
            mode = _timerMode.value, totalSeconds = _totalSeconds.value,
            remainingSeconds = _secondsLeft.value, isRunning = _isRunning.value,
            checkpointElapsedMs = checkpointElapsed, checkpointBootCount = recoveryBootCount,
            checkpointAtMs = checkpointAt,
            checkpointSequence = ++checkpointSequence,
            suspendedTotalSeconds = suspendedFocusState?.totalSeconds ?: 0,
            suspendedRemainingSeconds = suspendedFocusState?.remainingSeconds ?: 0,
            standardBreakSeconds = standardBreakSeconds, targetPomodoroLoops = _targetPomodoroLoops.value,
            completedPomodoroLoops = _pomodorosCompleted.value, pomodoroFocusSeconds = pomodoroFocusSeconds,
            pomodoroBreakSeconds = pomodoroBreakSeconds, pomodoroFinalBreakSeconds = pomodoroFinalBreakSeconds,
            pomodoroCompletesAfterCurrentBreak = pomodoroCompletesAfterCurrentBreak, autoStartBreak = autoStartBreak,
            presenceSeconds = presenceSeconds, presenceDeadlineElapsedMs = presenceExpiryElapsed.takeIf {
                _presenceCheckInDeadline.value != null
            } ?: 0L,
            presenceExpired = presenceCheckInExpired, presencePromptDismissed = _presencePromptDismissed.value,
            quickUnlockUntilElapsedMs = quickUnlockResume?.resumeAtElapsed ?: 0L,
            quickUnlockGraceUntilMs = quickUnlockResume?.graceUntilMs ?: 0L,
        )
    }

    fun start() {
        // Repeated Start requests must not replace an already-running period's
        // monotonic anchor with the last (possibly sleeping) display tick.
        if (_isRunning.value || closingSession || terminalCleanupRetry) return
        cancelQuickUnlockResume()
        if (!recoveryReady || signedInUserId.isNullOrBlank()) {
            android.widget.Toast.makeText(this, saveFailure ?: "Preparing your saved study time. Please try again in a moment.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        if (EkagraSessionJournal.get(this).errors.value != null) {
            android.widget.Toast.makeText(this, EkagraSessionJournal.get(this).errors.value, android.widget.Toast.LENGTH_LONG).show()
            return
        }
        if (_secondsLeft.value <= 0 && _timerMode.value != TimerMode.STOPWATCH) return
        runCatching { getSystemService(NotificationManager::class.java).cancel(POMODORO_TRANSITION_NOTIFICATION_ID) }
        if (_timerMode.value != TimerMode.BREAK && (autoSaveMetadata == null || sessionSaveQueuedThisRun)) {
            prepareAutoSaveSession(null, null, null, forceNew = true)
        }
        if (sessionOwnerId == null) sessionOwnerId = signedInUserId
        if (_timerMode.value == TimerMode.BREAK && autoSaveMetadata == null && breakActionId == null)
            breakActionId = "break-action-${UUID.randomUUID()}"
        val restoredAnchor = restoredStartAnchor
        restoredStartAnchor = null
        periodStartElapsed = restoredAnchor?.first ?: elapsedRealtime()
        periodStartAtMs = restoredAnchor?.second ?: System.currentTimeMillis()
        periodStartRemaining = _secondsLeft.value
        presenceStartSeconds = presenceSeconds
        val resumingPausedSession = timerSessionActive && !_isRunning.value && _timerMode.value != TimerMode.BREAK
        _isRunning.value = true
        // Set before below — its polling loop's while
        // condition reads this on a freshly launched coroutine, so it must already
        // be true before that coroutine is scheduled, not after.
        timerSessionActive = true
        persistTimerState()

        if (!promoteTimerForeground()) {
            pause()
            return
        }

        syncNotificationShieldForTimerSession()
        if (resumingPausedSession) {
            focusShieldRepository().endQuickUnlockForEkagraResume()
        }
        syncRankedFocus()
        // MediaPlayer setup can do synchronous URI work. Finish it before asking
        // Android to start Kavach's foreground service, whose deadline is short.
        startMusic(currentMusicUrl)
        focusShieldRepository().activateForSession(
            plannedSeconds = if (_timerMode.value == TimerMode.STOPWATCH) 0 else _totalSeconds.value,
            isFocusPeriod = _timerMode.value != TimerMode.BREAK,
        )
        lastTickElapsedMs = elapsedRealtime()
        scheduleTimerAlarms()
        // A session is now live. The pill shows only while SAFAR is backgrounded (syncBubble).
        syncBubble()
        launchTicker()
    }

    private fun launchTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (_isRunning.value) {
                delay(1000L)
                reconcileElapsedTime()
                persistTimerState(periodic = true)
                updateNotification()
                TimerBubbleOverlay.update(
                    secondsLeft = _secondsLeft.value, totalSeconds = _totalSeconds.value,
                    kavachActive = FocusShieldRepository.ShieldPrefs.isActive(applicationContext),
                    isRunning = _isRunning.value, timerMode = _timerMode.value,
                )
            }
        }
    }

    /** Apply reducer transitions once by moving the monotonic anchor before running effects. */
    private fun applyElapsedTransition(result: EkagraTimerRecoveryResult, nowElapsed: Long) {
        val previousMode = _timerMode.value
        val previousTotal = _totalSeconds.value
        val wasManualBreak = suspendedFocusState != null
        val wasPomodoro = _targetPomodoroLoops.value > 0
        val state = result.state
        val breakWrites = result.completedBreaks.map { completed ->
            enqueueCompletedBreakSessionSave(completed.seconds, completed.seconds,
                Instant.ofEpochMilli(completed.endedAtMs).toString(),
                "ekagra-break-${recoveryBootCount ?: -1}-${completed.endedElapsedMs}")
        }
        _timerMode.value = state.mode
        _totalSeconds.value = state.totalSeconds
        _secondsLeft.value = state.remainingSeconds
        _pomodorosCompleted.value = state.completedPomodoroLoops
        pomodoroCompletesAfterCurrentBreak = state.pomodoroCompletesAfterCurrentBreak
        suspendedFocusState = state.suspendedTotalSeconds.takeIf { it > 0 }?.let {
            SuspendedFocusState(it, state.suspendedRemainingSeconds)
        }
        // Carry sub-second time across transitions instead of dropping it on each round.
        val remainderMs = (nowElapsed - periodStartElapsed).coerceAtLeast(0L) % 1000L
        val logicalNowAtMs = periodStartAtMs + (nowElapsed - periodStartElapsed)
        periodStartElapsed = nowElapsed - remainderMs
        periodStartAtMs = logicalNowAtMs - remainderMs
        periodStartRemaining = state.remainingSeconds
        presenceSeconds = state.presenceSeconds
        presenceStartSeconds = presenceSeconds
        presenceCheckInExpired = state.presenceExpired
        _isRunning.value = state.isRunning
        releaseMusic()

        if (result.completed) {
            val progress = durableFocusProgress()
            _isRunning.value = false
            timerSessionActive = false
            closingSession = true
            clearPresence()
            enqueueCompletedFocusSessionSave(progress.plannedSeconds, progress.actualSeconds,
                if (wasPomodoro) TimerMode.POMODORO else TimerMode.FOCUS,
                endedAtOverride = result.completedAtMs?.let { Instant.ofEpochMilli(it).toString() },
                completedElapsedMs = result.completedElapsedMs)
            val saved = terminalSave
            endingJob = scope.launch {
                try {
                    saved?.await()
                    breakWrites.forEach { it?.await() }
                    endKavachAnalyticsSession(KavachSessionOutcome.COMPLETED)
                    clearPersistedTimerState()
                    if (wasPomodoro) {
                        NotificationShieldPrefs.clear(this@TimerService)
                        _pomodoroCompletionEvent.value += 1
                        _targetPomodoroLoops.value = 0
                        _pomodorosCompleted.value = 0
                        pomodoroCompletesAfterCurrentBreak = false
                        _timerMode.value = TimerMode.FOCUS
                        _totalSeconds.value = pomodoroFocusSeconds
                        _secondsLeft.value = pomodoroFocusSeconds
                    } else {
                        _timerMode.value = TimerMode.BREAK
                        _totalSeconds.value = standardBreakSeconds
                        _secondsLeft.value = standardBreakSeconds
                    }
                    TimerBubbleOverlay.hide()
                    terminalSave = null
                    if (!wasPomodoro && autoStartBreak) {
                        closingSession = false
                        start()
                        // If the callback was delayed beyond the automatic break, account for that gap too.
                        val boundary = result.completedElapsedMs
                        if (boundary != null) {
                            val gapMs = (elapsedRealtime() - boundary).coerceAtLeast(0L)
                            periodStartElapsed -= gapMs
                            periodStartAtMs = result.completedAtMs ?: System.currentTimeMillis() - gapMs
                            reconcileElapsedTime()
                        }
                    } else {
                        stopForegroundCompat().await()
                        closingSession = false
                        stopSelf()
                    }
                } catch (error: Exception) {
                    closingSession = false
                    saveFailure = "Study time could not be saved. Please free up storage and retry."
                    terminalCleanupRetry = true
                    EkagraDiagnostics.recordFailure(this@TimerService, "timer_completion_commit_failed", error)
                }
            }
        } else if (state.mode == TimerMode.BREAK && !state.isRunning && state.remainingSeconds == 0 && !wasPomodoro && !wasManualBreak) {
            timerSessionActive = false
            closingSession = true
            _secondsLeft.value = previousTotal
            endingJob = scope.launch {
                try {
                    breakWrites.forEach { it?.await() }
                    clearPersistedTimerState()
                    TimerBubbleOverlay.hide()
                    if (result.completedBreaks.lastOrNull()?.let { elapsedRealtime() - it.endedElapsedMs <= 30_000L } == true)
                        showCompletionNotification(TimerMode.BREAK)
                    stopForegroundCompat().await()
                    closingSession = false
                    stopSelf()
                } catch (error: Exception) {
                    closingSession = false
                    terminalCleanupRetry = true
                    EkagraDiagnostics.recordFailure(this@TimerService, "break_commit_failed", error)
                }
            }
        } else {
            timerSessionActive = true
            if (state.isRunning) startMusic(currentMusicUrl)
            persistTimerState()
            syncRankedFocus()
            focusShieldRepository().activateForSession(
                plannedSeconds = _totalSeconds.value, isFocusPeriod = state.mode != TimerMode.BREAK)
            if (previousMode != state.mode) showPomodoroTransitionNotification(
                title = if (state.mode == TimerMode.BREAK) "Focus complete" else "Break complete",
                body = if (state.isRunning) "${getString(state.mode.labelRes)} in progress" else "Ready. Tap Start when ready.",
                canStart = !state.isRunning,
            )
            updateNotification()
            syncBubble()
            scheduleTimerAlarms()
        }
    }

    private fun cancelQuickUnlockResume() {
        quickUnlockResume = null
        quickUnlockResumeJob?.cancel()
        quickUnlockResumeJob = null
    }

    private fun pauseForQuickUnlock(graceUntilMs: Long) {
        val id = currentSessionId() ?: return
        val repository = focusShieldRepository()
        if (graceUntilMs <= System.currentTimeMillis() ||
            FocusShieldRepository.ShieldPrefs.getGraceUntilMs(this) != graceUntilMs ||
            !timerSessionActive || _timerMode.value == TimerMode.BREAK ||
            repository.isAlwaysOnMode.value || !repository.isEnabled.value ||
            (!_isRunning.value && quickUnlockResume?.sessionId != id)
        ) return
        // Pause/reconcile before starting the break so none of its time is credited.
        pause()
        val pending = QuickUnlockResume(id, graceUntilMs,
            elapsedRealtime() + (graceUntilMs - System.currentTimeMillis()).coerceAtLeast(0L))
        quickUnlockResume = pending
        quickUnlockResumeJob = scope.launch {
            delay((pending.resumeAtElapsed - elapsedRealtime()).coerceAtLeast(0L))
            resumeExpiredQuickUnlock()
        }
        // The alarm is a wake-up fallback when the coroutine sleeps with the screen off.
        persistTimerState()
        scheduleTimerAlarms()
    }

    private fun resumeExpiredQuickUnlock(): Boolean {
        val pending = quickUnlockResume ?: return false
        if (!pending.isDue(elapsedRealtime())) return false
        val repository = focusShieldRepository()
        val eligible = pending.matches(currentSessionId(), FocusShieldRepository.ShieldPrefs.getGraceUntilMs(this)) &&
            timerSessionActive && !_isRunning.value && _timerMode.value != TimerMode.BREAK &&
            sessionOwnerId == signedInUserId && !signedInUserId.isNullOrBlank() &&
            repository.sessionActive.value && repository.isEnabled.value && !repository.isAlwaysOnMode.value
        cancelQuickUnlockResume()
        if (!eligible) return false
        repository.finishQuickUnlockForAutomaticResume()
        start()
        return _isRunning.value
    }

    fun pause() {
        cancelQuickUnlockResume()
        if (_isRunning.value) reconcileElapsedTime()
        if (closingSession) return
        EkagraTimerAlarms.cancel(this)
        _isRunning.value = false
        syncRankedFocus()
        tickJob?.cancel()
        persistTimerState()
        releaseMusic()
        // KAVACH intentionally stays active through a pause — the session hasn't
        // ended, the user could just be checking something and forget to resume.
        //'s loop and syncFocusShieldState() are both
        // keyed on timerSessionActive (unaffected by pause), so the monitor and
        // block screen keep working exactly as they did while running.
        updateNotification()
        // Session is still active (paused) — keep the pill if backgrounded, now showing Play.
        syncBubble()
        scheduleTimerAlarms()
    }

    fun reset() {
        if (destroyingService || !recoveryReady || closingSession) return
        if (_isRunning.value) reconcileElapsedTime()
        if (closingSession) return
        saveCurrentProgress("user-ended")
        closingSession = true
        cancelQuickUnlockResume()
        _isRunning.value = false
        tickJob?.cancel()
        EkagraTimerAlarms.cancel(this)
        releaseMusic()
        val id = currentSessionId()
        val pending = terminalSave
        endingJob = scope.launch {
            try {
                pending?.await()
                // A committed terminal identity must precede removing foreground protection.
                syncRankedFocus(close = true)
                clearPresence()
                endKavachAnalyticsSession(KavachSessionOutcome.ENDED_EARLY)
                runCatching { getSystemService(NotificationManager::class.java).cancel(POMODORO_TRANSITION_NOTIFICATION_ID) }
                timerSessionActive = false
                _secondsLeft.value = if (_timerMode.value == TimerMode.STOPWATCH) 0 else _totalSeconds.value
                _targetPomodoroLoops.value = 0
                _pomodorosCompleted.value = 0
                pomodoroCompletesAfterCurrentBreak = false
                suspendedFocusState = null
                clearPersistedTimerState()
                sessionOwnerId = null
                TimerBubbleOverlay.hide()
                stopForegroundCompat().await()
                terminalSave = null
                terminalCleanupRetry = false
                saveFailure = null
                sessionSaveQueuedThisRun = false
                closingSession = false
                stopSelf()
            } catch (error: Exception) {
                closingSession = false
                sessionSaveQueuedThisRun = false
                terminalSave = null
                saveFailure = "Study time could not be saved. Please free up storage and retry."
                terminalCleanupRetry = true
                EkagraDiagnostics.recordFailure(this@TimerService, "timer_end_commit_failed", error)
                if (currentSessionId() == id) updateNotification()
            }
        }
    }

    fun isActive(): Boolean = _isRunning.value || (if (_timerMode.value == TimerMode.STOPWATCH) _secondsLeft.value > 0 else _secondsLeft.value < _totalSeconds.value)

    internal fun endAndSave() = reset()

    private fun saveCurrentProgress(reason: String) {
        if (autoSaveMetadata == null || sessionSaveQueuedThisRun) return
        val progress = durableFocusProgress()
        if (progress.actualSeconds <= 0) {
            val id = currentSessionId() ?: return
            terminalSave = sessionOwnerId?.let { EkagraSessionJournal.get(this).closeEmptySession(id, it) }
            sessionSaveQueuedThisRun = true
            return
        }
        enqueueCompletedFocusSessionSave(progress.plannedSeconds, progress.actualSeconds,
            if (_targetPomodoroLoops.value > 0) TimerMode.POMODORO
            else if (_timerMode.value == TimerMode.STOPWATCH) TimerMode.STOPWATCH else TimerMode.FOCUS,
            endReason = reason)
    }

    private fun durableFocusProgress(): FocusProgressSnapshot {
        val suspended = suspendedFocusState ?: return currentPeriodFocusProgress()
        return calculateFocusProgress(
            if (_targetPomodoroLoops.value > 0) TimerMode.POMODORO else TimerMode.FOCUS,
            suspended.totalSeconds, suspended.remainingSeconds, pomodoroFocusSeconds,
            _targetPomodoroLoops.value, _pomodorosCompleted.value)
    }

    private fun snapshot(reason: String? = null): PendingEkagraSessionSave? {
        val metadata = autoSaveMetadata ?: return null
        val progress = durableFocusProgress()
        val mode = if (_targetPomodoroLoops.value > 0) TimerMode.POMODORO
            else if (_timerMode.value == TimerMode.STOPWATCH) TimerMode.STOPWATCH else TimerMode.FOCUS
        return PendingEkagraSessionSave(
            clientSessionId = metadata.clientSessionId, mode = mode.toApiMode(),
            startedAt = metadata.startedAt, endedAt = Instant.now().toString(),
            plannedDurationMinutes = if (mode == TimerMode.STOPWATCH) 0 else (progress.plannedSeconds + 59) / 60,
            actualDurationMinutes = (progress.actualSeconds / 60.0).roundToInt(), actualDurationSeconds = progress.actualSeconds,
            taskTitle = metadata.taskTitle ?: "Untitled", goalId = metadata.goalId, goalTitle = metadata.goalTitle,
            topicId = metadata.topicId, planId = metadata.planId, topicTitle = metadata.topicTitle,
            shieldEnabled = FocusShieldRepository.ShieldPrefs.isActive(this), endReason = reason, ownerId = sessionOwnerId,
            completedFocusRounds = _pomodorosCompleted.value, targetFocusRounds = _targetPomodoroLoops.value, periodMode = _timerMode.value.name,
        )
    }

    private fun syncNotificationShieldForTimerSession() {
        scope.launch {
            val enabled = safarDataStore.focusShieldEnabled.first()
            val packages = safarDataStore.focusShieldBlockedPackages.first()
            if (enabled && packages.isNotEmpty()) {
                NotificationShieldPrefs.write(this@TimerService, packages)
            } else {
                NotificationShieldPrefs.clear(this@TimerService)
            }
        }
    }

    private fun timerStatePrefs(): SharedPreferences =
        getSharedPreferences(TIMER_STATE_PREFS, Context.MODE_PRIVATE)

    private fun persistAutoSaveMetadata() {
        val metadata = autoSaveMetadata ?: return
        persistenceScope.launch {
            timerStatePrefs().edit()
                .putString(KEY_AUTO_SAVE_CLIENT_SESSION_ID, metadata.clientSessionId)
                .putString(KEY_AUTO_SAVE_STARTED_AT, metadata.startedAt)
                .putString(KEY_AUTO_SAVE_TASK_TITLE, metadata.taskTitle)
                .putString(KEY_AUTO_SAVE_GOAL_ID, metadata.goalId)
                .putString(KEY_AUTO_SAVE_GOAL_TITLE, metadata.goalTitle)
                .putString(KEY_AUTO_SAVE_TOPIC_ID, metadata.topicId)
                .putString(KEY_AUTO_SAVE_PLAN_ID, metadata.planId)
                .putString(KEY_AUTO_SAVE_TOPIC_TITLE, metadata.topicTitle)
                .commit()
        }
    }

    private fun enqueueCompletedFocusSessionSave(
        totalSeconds: Int,
        actualSeconds: Int,
        mode: TimerMode,
        endedAtOverride: String? = null,
        forceUntitled: Boolean = false,
        endReason: String = "completed",
        completedElapsedMs: Long? = null,
    ) {
        val total = totalSeconds.coerceAtLeast(1)
        val actual = if (mode == TimerMode.STOPWATCH) actualSeconds.coerceAtLeast(0)
            else actualSeconds.coerceIn(0, total)
        if (actual == 0) return
        val endedAt = endedAtOverride ?: Instant.now().toString()
        val metadata = autoSaveMetadata ?: AutoSaveMetadata(
            clientSessionId = "local-${UUID.randomUUID()}",
            startedAt = Instant.parse(endedAt).minusSeconds(actual.toLong()).toString(),
            taskTitle = null,
            goalId = null,
            goalTitle = null,
            topicId = null,
            planId = null,
            topicTitle = null,
        )
        // The service owns finalization; the screen can organize this same saved identity.
        val title = if (forceUntitled) DEFAULT_UNTITLED_SESSION_TITLE
            else metadata.taskTitle?.takeIf { it.isNotBlank() } ?: DEFAULT_UNTITLED_SESSION_TITLE
        val savingOwner = sessionOwnerId
        val durableSave = EkagraPendingSessionSaveStore.enqueue(
            this,
            PendingEkagraSessionSave(
                clientSessionId = metadata.clientSessionId,
                mode = mode.toApiMode(),
                startedAt = metadata.startedAt,
                endedAt = endedAt,
                plannedDurationMinutes = if (mode == TimerMode.STOPWATCH) 0 else (total + 59) / 60,
                actualDurationMinutes = (actual / 60.0).roundToInt(),
                actualDurationSeconds = actual,
                goalId = metadata.goalId,
                goalTitle = metadata.goalTitle,
                topicId = metadata.topicId,
                planId = metadata.planId,
                topicTitle = metadata.topicTitle,
                endReason = endReason,
                ownerId = sessionOwnerId,
                taskTitle = title,
                shieldEnabled = FocusShieldRepository.ShieldPrefs.isActive(this),
            ),
        )
        terminalSave = durableSave
        sessionSaveQueuedThisRun = true
        scope.launch {
            runCatching { durableSave.await() }.onSuccess {
                EkagraDiagnostics.record(applicationContext, "finalized", endReason)
                if (endReason == "completed") {
                    if (completedElapsedMs == null || elapsedRealtime() - completedElapsedMs in 0L..30_000L)
                        showCompletionNotification(mode)
                    _completedSession.value = EkagraSessionJournal.get(applicationContext)
                        .find(metadata.clientSessionId, savingOwner.orEmpty())
                }
                flushPendingSavesNow()
            }.onFailure { saveFailure = "Study time could not be saved. Please free up storage and retry." }
        }
    }

    /**
     * Tries to upload the just-queued session right away, while this foreground
     * service is still alive, instead of waiting for WorkManager to schedule the
     * job. Aggressive OEM battery managers (Xiaomi/Oppo/Vivo) routinely defer or
     * kill background work, which is how a completed session could end up sitting
     * in the local queue and never reaching the server. The durable queue and the
     * WorkManager job above stay in place as the retry path — this is purely a
     * best-effort fast path, so failures here are ignored (the queue keeps the
     * session and the worker will retry).
     */
    private fun flushPendingSavesNow() {
        scope.launch {
            runCatching { EkagraSessionSaveWorker.drainPendingSaves(applicationContext) }
        }
    }

    /**
     * Breaks were previously never persisted — no code path called a save when a
     * BREAK finished, so break time never showed up in history or analytics. The
     * backend already understands break sessions (TimerMode.BREAK.toApiMode() ==
     * "short", which /ekagra-sessions/save maps to session_type "short_break"), so
     * this only ever needed a client-side call site.
     *
     * Break metadata is deliberately independent of [autoSaveMetadata]: that holds
     * the *focus* session's identity (goal/topic links), which must not be credited
     * to a break, and it's cleared before an auto-break starts anyway.
     */
    private fun enqueueCompletedBreakSessionSave(
        actualSeconds: Int, plannedSeconds: Int, endedAtOverride: String? = null, sessionId: String? = null,
    ): Deferred<Unit>? {
        val actual = actualSeconds.coerceAtLeast(0)
        if (actual <= 0) return null
        val planned = plannedSeconds.coerceAtLeast(actual)
        val endedAt = endedAtOverride ?: Instant.now().toString()
        val saved = EkagraPendingSessionSaveStore.enqueue(
            this,
            PendingEkagraSessionSave(
                clientSessionId = sessionId ?: "ekagra-break-${UUID.randomUUID()}",
                mode = TimerMode.BREAK.toApiMode(),
                startedAt = Instant.parse(endedAt).minusSeconds(actual.toLong()).toString(),
                endedAt = endedAt,
                plannedDurationMinutes = (planned + 59) / 60,
                actualDurationMinutes = (actual / 60.0).roundToInt(),
                actualDurationSeconds = actual,
                // A break is never linked to a goal/topic — only focus time is.
                goalId = null,
                goalTitle = null,
                topicId = null,
                planId = null,
                topicTitle = null,
                taskTitle = "Break",
                ownerId = sessionOwnerId ?: signedInUserId,
                endReason = "completed",
                shieldEnabled = false,
            ),
        )
        EkagraSessionSaveWorker.enqueue(this)
        flushPendingSavesNow()
        return saved
    }

    private fun persistTimerState(periodic: Boolean = false) {
        if (!recoveryReady) return
        if (periodic) {
            val nowElapsed = elapsedRealtime()
            if (nowElapsed - lastPeriodicStateWriteElapsedMs < TIMER_STATE_WRITE_INTERVAL_MS) return
            lastPeriodicStateWriteElapsedMs = nowElapsed
        }
        reconcileElapsedTime()
        if (!sessionSaveQueuedThisRun) snapshot()?.let { checkpoint ->
            sessionOwnerId?.let { EkagraSessionJournal.get(this).checkpoint(checkpoint, it, recoveryState()) }
        }
        val total = if (_timerMode.value == TimerMode.STOPWATCH) _secondsLeft.value else _totalSeconds.value
        val remaining = if (_timerMode.value == TimerMode.STOPWATCH) _secondsLeft.value else _secondsLeft.value.coerceIn(0, total.coerceAtLeast(1))
        val shouldPersist = _isRunning.value || _targetPomodoroLoops.value > 0 ||
            (_timerMode.value == TimerMode.STOPWATCH && remaining > 0) || (remaining < total)
        if (!shouldPersist) return

        val suspended = suspendedFocusState
        timerStateWrites.trySend(
            TimerStateWrite.Save(
                total = total,
                remaining = remaining,
                mode = _timerMode.value.name,
                isRunning = _isRunning.value,
                savedAtMs = System.currentTimeMillis(),
                savedElapsedMs = elapsedRealtime(),
                savedBootCount = recoveryBootCount ?: -1,
                presenceSeconds = presenceSeconds,
                presenceDeadlineAtMs = _presenceCheckInDeadline.value ?: 0L,
                presenceExpired = presenceCheckInExpired,
                suspendedTotal = suspended?.totalSeconds ?: 0,
                suspendedRemaining = suspended?.remainingSeconds ?: 0,
                standardBreakSeconds = standardBreakSeconds,
                targetPomodoroLoops = _targetPomodoroLoops.value,
                completedPomodoroLoops = _pomodorosCompleted.value,
                pomodoroFocusSeconds = pomodoroFocusSeconds,
                pomodoroBreakSeconds = pomodoroBreakSeconds,
            ),
        )
    }

    private fun clearPersistedTimerState() {
        cancelQuickUnlockResume()
        autoSaveMetadata = null
        breakActionId = null
        timerStateWrites.trySend(TimerStateWrite.Clear)
    }

    private fun reconcileElapsedTime() {
        if (!_isRunning.value || reconcilingElapsedTime) return
        reconcilingElapsedTime = true
        try {
            val now = elapsedRealtime()
            val anchor = recoveryState().copy(
                checkpointElapsedMs = periodStartElapsed,
                checkpointAtMs = periodStartAtMs.takeIf { it > 0 } ?: (System.currentTimeMillis() - (now - periodStartElapsed)),
                checkpointBootCount = recoveryBootCount ?: 0,
                remainingSeconds = periodStartRemaining, presenceSeconds = presenceStartSeconds,
            )
            val result = advanceEkagraTimer(anchor, now, recoveryBootCount ?: 0, System.currentTimeMillis())
            if (result.periodFinished && !destroyingService) {
                applyElapsedTransition(result, now)
                return
            }
            val elapsed = result.elapsedSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            _secondsLeft.value = if (destroyingService && result.periodFinished)
                (periodStartRemaining - elapsed).coerceAtLeast(0) else result.state.remainingSeconds
            if (_timerMode.value != TimerMode.BREAK && _presenceCheckInDeadline.value == null && !presenceCheckInExpired) {
                presenceSeconds = presenceStartSeconds + elapsed.coerceAtMost(if (_timerMode.value == TimerMode.STOPWATCH) Int.MAX_VALUE else periodStartRemaining)
                if (presenceSeconds >= PRESENCE_REMINDER_INTERVAL_SECONDS) openPresenceCheckIn(presenceDueElapsed)
            }
        } finally {
            reconcilingElapsedTime = false
        }
    }

    internal fun scheduleTimerAlarms() {
        runCatching { scheduleTimerAlarmsInternal() }.onFailure { EkagraDiagnostics.record(this, "alarm_failure", it.javaClass.simpleName) }
    }

    private fun scheduleTimerAlarmsInternal() {
        alarmGeneration += 1
        if (!_isRunning.value) {
            val deadline = minOf(
                quickUnlockResume?.resumeAtElapsed ?: Long.MAX_VALUE,
                if (_presenceCheckInDeadline.value != null) presenceExpiryElapsed else Long.MAX_VALUE,
            )
            if (deadline != Long.MAX_VALUE) EkagraTimerAlarms.schedule(this, currentSessionId() ?: "break", alarmGeneration, deadline)
            else EkagraTimerAlarms.cancel(this)
            return
        }
        val now = elapsedRealtime()
        periodDueElapsed = if (_timerMode.value == TimerMode.STOPWATCH) Long.MAX_VALUE else periodStartElapsed + periodStartRemaining * 1000L
        presenceDueElapsed = periodStartElapsed + (PRESENCE_REMINDER_INTERVAL_SECONDS - presenceStartSeconds).coerceAtLeast(0) * 1000L
        val reminder = if (_presenceCheckInDeadline.value != null) presenceExpiryElapsed
            else if (_timerMode.value == TimerMode.BREAK || presenceCheckInExpired) Long.MAX_VALUE else presenceDueElapsed
        // A bounded recovery alarm also revives a killed foreground service. Time
        // accuracy comes from the durable clock, so idle-mode alarm delays cannot
        // shorten the saved session.
        val deadline = minOf(periodDueElapsed, reminder, now + 5 * 60_000L)
        if (deadline != Long.MAX_VALUE) EkagraTimerAlarms.schedule(this, currentSessionId() ?: "break", alarmGeneration, deadline)
    }

    internal fun onScheduledAlarm(id: String, generation: Long) {
        if (id != (currentSessionId() ?: "break") || generation != alarmGeneration) return
        if (resumeExpiredQuickUnlock()) return
        if (!_isRunning.value && _presenceCheckInDeadline.value == null) {
            scheduleTimerAlarms()
            return
        }
        reconcileElapsedTime()
        if (_presenceCheckInDeadline.value != null && elapsedRealtime() >= presenceExpiryElapsed) {
            _presenceCheckInDeadline.value = null
            presenceCheckInExpired = true
            cancelPresenceNotification()
        }
        persistTimerState()
        if (_isRunning.value && tickJob?.isActive != true) launchTicker()
        // The shared reducer already committed the boundary transition above.
        if (_secondsLeft.value > 0 || _timerMode.value == TimerMode.STOPWATCH) scheduleTimerAlarms()
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun clearPresence() {
        presenceExpiryJob?.cancel()
        _presenceCheckInDeadline.value = null
        presenceCheckInExpired = false
        cancelPresenceNotification()
        presenceSeconds = 0
        presenceStartSeconds = 0
        presenceExpiryElapsed = 0L
    }

    private fun openPresenceCheckIn(dueElapsed: Long = elapsedRealtime()) {
        if (_presenceCheckInDeadline.value != null || presenceCheckInExpired) return
        presenceExpiryElapsed = dueElapsed + PRESENCE_RESPONSE_WINDOW_MS
        val deadline = System.currentTimeMillis() + (presenceExpiryElapsed - elapsedRealtime())
        _presencePromptDismissed.value = false
        _presenceCheckInDeadline.value = deadline
        syncRankedFocus()
        persistTimerState()
        if (deadline > System.currentTimeMillis()) showPresenceReminderNotification()
        schedulePresenceExpiry(deadline)
        scheduleTimerAlarms()
    }

    private fun schedulePresenceExpiry(deadline: Long) {
        presenceExpiryJob?.cancel()
        presenceExpiryJob = scope.launch {
            delay((deadline - System.currentTimeMillis()).coerceAtLeast(0L))
            if (_presenceCheckInDeadline.value == deadline) {
                _presenceCheckInDeadline.value = null
                presenceCheckInExpired = true
                cancelPresenceNotification()
                persistTimerState()
                scheduleTimerAlarms()
            }
        }
    }

    private fun ekagraAlertChannel(): String = when (timerAlertStyle) {
        TimerAlertStyle.SOUND -> SafarNotificationChannels.EKAGRA_ALERT
        TimerAlertStyle.VIBRATE -> SafarNotificationChannels.EKAGRA_VIBRATE_ALERT
    }

    private fun showPresenceReminderNotification() {
        runCatching { acquireCompletionWakeLock() }
        val alertChannel = ekagraAlertChannel()
        val openIntent = PendingIntent.getActivity(
            this,
            5,
            NotificationDeepLinkHandler.activityIntent(this, "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val confirmIntent = PendingIntent.getService(
            this,
            6,
            Intent(this, TimerService::class.java).apply { action = ACTION_CONFIRM_PRESENCE; putExtra("session", currentSessionId()); putExtra("presenceDeadline", _presenceCheckInDeadline.value ?: -1L) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val declineIntent = PendingIntent.getService(
            this, 9,
            Intent(this, TimerService::class.java).apply {
                action = ACTION_DECLINE_PRESENCE
                putExtra("session", currentSessionId())
                putExtra("presenceDeadline", _presenceCheckInDeadline.value ?: -1L)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, alertChannel)
            .setContentTitle(getString(R.string.ekagra_presence_question))
            .setContentText(getString(R.string.ekagra_presence_ranked_body))
            .setShowWhen(false)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentIntent(openIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setOngoing(false)
            .setTimeoutAfter((_presenceCheckInDeadline.value!! - System.currentTimeMillis()).coerceAtLeast(1L))
            .addAction(android.R.drawable.ic_menu_send, getString(R.string.common_yes), confirmIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.ekagra_presence_no), declineIntent)
            .build()

        runCatching {
            getSystemService(NotificationManager::class.java)
                .notify(PRESENCE_NOTIFICATION_ID, notification)
        }
    }

    fun declinePresenceReminder() {
        if (_presenceCheckInDeadline.value == null || _presencePromptDismissed.value) return
        // Dismiss both surfaces without confirming attendance or changing the personal timer.
        // Keep the original grace deadline so ranked eligibility still expires normally.
        _presencePromptDismissed.value = true
        cancelPresenceNotification()
    }

    fun confirmPresenceReminder() {
        if (_presencePromptDismissed.value) return
        val deadline = _presenceCheckInDeadline.value ?: return
        if (System.currentTimeMillis() >= deadline) {
            _presenceCheckInDeadline.value = null
            presenceCheckInExpired = true
            cancelPresenceNotification()
            persistTimerState()
            return
        }
        presenceExpiryJob?.cancel()
        _presenceCheckInDeadline.value = null
        syncRankedFocus(confirm = true)
        presenceSeconds = 0
        presenceStartSeconds = -((elapsedRealtime() - periodStartElapsed) / 1000L).toInt()
        scheduleTimerAlarms()
        persistTimerState()
        cancelPresenceNotification()
    }

    private fun cancelPresenceNotification() {
        runCatching {
            getSystemService(NotificationManager::class.java)
                .cancel(PRESENCE_NOTIFICATION_ID)
        }
    }

    private fun buildNotification(): Notification {
        notificationBuildCount++
        val openIntent = PendingIntent.getActivity(
            this, 0,
            NotificationDeepLinkHandler.activityIntent(this, "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply { action = ACTION_PLAY_PAUSE; putExtra("session", currentSessionId()) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply { action = ACTION_END_SAVE; putExtra("session", currentSessionId()) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val s    = _secondsLeft.value
        val mode = getString(_timerMode.value.labelRes)
        val time = "%02d:%02d".format(s / 60, s % 60)
        val notificationText = when {
            _isRunning.value && (_timerMode.value == TimerMode.FOCUS || _timerMode.value == TimerMode.STOPWATCH || _timerMode.value == TimerMode.POMODORO) ->
                if (_timerMode.value == TimerMode.POMODORO) "Pomodoro in progress" else "Ekagra in progress"
            _isRunning.value -> "Break in progress - KAVACH paused"
            else -> "Timer paused"
        }

        val views = RemoteViews(packageName, R.layout.notification_ekagra_timer).apply {
            setTextViewText(R.id.ekagra_notification_mode, mode)
            if (_isRunning.value) {
                val base = if (_timerMode.value == TimerMode.STOPWATCH)
                    periodStartElapsed - periodStartRemaining * 1000L
                else periodStartElapsed + periodStartRemaining * 1000L
                setChronometer(R.id.ekagra_notification_clock, base, null, true)
                setChronometerCountDown(R.id.ekagra_notification_clock, _timerMode.value != TimerMode.STOPWATCH)
            } else {
                setChronometer(R.id.ekagra_notification_clock, 0, null, false)
                setTextViewText(R.id.ekagra_notification_clock, time)
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .setCustomBigContentView(views)
            .setContentTitle(mode)
            .setContentText(personalizeNotificationBody(notificationText))
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentIntent(openIntent)
            .setOngoing(_isRunning.value)
            .setOnlyAlertOnce(true)
            .addAction(
                if (_isRunning.value) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (_isRunning.value) "Pause" else "Resume",
                playPauseIntent
            )
            .addAction(android.R.drawable.ic_menu_save, "End & save", resetIntent)
            .build()
    }

    private fun personalizeNotificationBody(body: String): String = body

    private fun startsWithPersonalGreeting(body: String): Boolean {
        return Regex(
            pattern = "^\\s*(hi|hey|hello|good morning|good afternoon|good evening)\\b",
            option = RegexOption.IGNORE_CASE,
        ).containsMatchIn(body)
    }

    private fun showCompletionNotification(completedMode: TimerMode = _timerMode.value) {

        // Acquire a brief WakeLock so the CPU and audio subsystem are awake to
        // deliver the OS notification sound even when the screen is off.
        runCatching { acquireCompletionWakeLock() }

        // Sound and vibration are now handled entirely by the OS notification channel:
        //   SOUND   → Android plays the phone's selected notification sound.
        //   VIBRATE → vibration-only channel, with no notification sound.
        val alertChannel = ekagraAlertChannel()

        scope.launch {
            val body = when (completedMode) {
                TimerMode.FOCUS,
                TimerMode.POMODORO -> "Ekagra session complete. Great work - take a mindful break."
                TimerMode.BREAK,
                TimerMode.STOPWATCH -> "Break finished. Ready for your next session?"
            }
            SafarNotificationManager(this@TimerService).show(
                title = if (completedMode == TimerMode.FOCUS || completedMode == TimerMode.POMODORO) "Ekagra session complete" else "Break finished",
                body = body,
                channelId = alertChannel,
                deepLink = "safar://ekagra",
                notificationId = COMPLETION_NOTIFICATION_ID,
                priority = NotificationCompat.PRIORITY_HIGH,
            )
        }
    }

    private fun showPomodoroTransitionNotification(title: String, body: String, canStart: Boolean) {
        // Acquire a brief WakeLock so the CPU and audio subsystem are awake to
        // deliver the OS notification sound even when the screen is off.
        runCatching { acquireCompletionWakeLock() }

        // Channel selection mirrors showCompletionNotification:
        //   SOUND → phone notification sound only; VIBRATE → vibration only
        val alertChannel = ekagraAlertChannel()

        scope.launch {
            if (canStart && _isRunning.value) return@launch

            val openIntent = PendingIntent.getActivity(
                this@TimerService, 9,
                NotificationDeepLinkHandler.activityIntent(this@TimerService, "safar://ekagra"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val builder = NotificationCompat.Builder(this@TimerService, alertChannel)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this@TimerService))
                .setContentIntent(openIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            if (canStart) {
                val startIntent = PendingIntent.getService(
                    this@TimerService, 10,
                    Intent(this@TimerService, TimerService::class.java).apply { action = ACTION_PLAY_PAUSE; putExtra("session", currentSessionId()) },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.addAction(android.R.drawable.ic_media_play, "Start", startIntent)
            }
            runCatching {
                getSystemService(NotificationManager::class.java)
                    .notify(POMODORO_TRANSITION_NOTIFICATION_ID, builder.build())
            }
        }
    }

    private fun updateNotification() {
        if (timerSessionActive || _isRunning.value || isActive()) {
            val presentation = listOf(currentSessionId(), _timerMode.value, _isRunning.value,
                periodStartElapsed, periodStartRemaining, if (_isRunning.value) null else _secondsLeft.value,
                timerAlertStyle, notificationGeneration)
            if (presentation != lastNotificationPresentation) {
                lastNotificationPresentation = presentation
                notificationUpdates.trySend(NotificationUpdate(notificationGeneration, buildNotification()))
            }
        } else {
            stopForegroundCompat()
        }
    }

    private fun stopForegroundCompat(): Deferred<Unit> {
        lastNotificationPresentation = null
        val removed = CompletableDeferred<Unit>()
        val update = NotificationUpdate(++notificationGeneration, null, removed)
        if (notificationUpdates.trySend(update).isFailure) removed.complete(Unit)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else { @Suppress("DEPRECATION") stopForeground(true) }
        return removed
    }
}
