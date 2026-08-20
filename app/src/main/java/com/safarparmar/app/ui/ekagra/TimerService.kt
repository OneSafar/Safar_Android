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
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
import androidx.core.app.NotificationCompat
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.local.TimerAlertStyle
import com.safarparmar.app.data.remote.api.FocusApi
import com.safarparmar.app.data.remote.dto.FocusPresenceRequest
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
import kotlinx.coroutines.Dispatchers
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
        const val ACTION_PLAY_PAUSE = "com.safar.ekagra.ACTION_PLAY_PAUSE"
        const val ACTION_PAUSE = "com.safar.ekagra.ACTION_PAUSE"
        const val ACTION_RESET      = "com.safar.ekagra.ACTION_RESET"
        const val ACTION_FOCUS_SHIELD_BLOCKED = "com.safar.ekagra.ACTION_FOCUS_SHIELD_BLOCKED"
        private const val TIMER_STATE_PREFS = "ekagra_timer_state_prefs"
        private const val KEY_HAS_STATE = "has_state"
        private const val KEY_MODE = "mode"
        private const val KEY_TOTAL_SECONDS = "total_seconds"
        private const val KEY_REMAINING_SECONDS = "remaining_seconds"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_SAVED_AT_MS = "saved_at_ms"
        private const val KEY_SUSPENDED_TOTAL_SECONDS = "suspended_total_seconds"
        private const val KEY_SUSPENDED_REMAINING_SECONDS = "suspended_remaining_seconds"
        private const val KEY_STANDARD_BREAK_SECONDS = "standard_break_seconds"
        private const val KEY_TARGET_POMODORO_LOOPS = "target_pomodoro_loops"
        private const val KEY_COMPLETED_POMODORO_LOOPS = "completed_pomodoro_loops"
        private const val KEY_POMODORO_FOCUS_SECONDS = "pomodoro_focus_seconds"
        private const val KEY_POMODORO_BREAK_SECONDS = "pomodoro_break_seconds"
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
            val modeStr = prefs.getString(KEY_MODE, TimerMode.FOCUS.name)
            if (
                modeStr != TimerMode.FOCUS.name &&
                modeStr != TimerMode.STOPWATCH.name &&
                modeStr != TimerMode.POMODORO.name
            ) return false

            if (modeStr == TimerMode.STOPWATCH.name) return true

            val total = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60).coerceAtLeast(1)
            val savedRemaining = prefs.getInt(KEY_REMAINING_SECONDS, total).coerceIn(0, total)
            val savedAtMs = prefs.getLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
            val elapsed = ((System.currentTimeMillis() - savedAtMs) / 1000L).toInt().coerceAtLeast(0)
            return savedRemaining - elapsed > 0
        }

        /** Notification Shield remains active through every focus, break, and paused period. */
        fun isKavachNotificationSuppressionActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(TIMER_STATE_PREFS, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_HAS_STATE, false)) return false

            val modeStr = prefs.getString(KEY_MODE, TimerMode.FOCUS.name)
            val isBreak = modeStr == TimerMode.BREAK.name
            val isFocusPeriod = modeStr == TimerMode.FOCUS.name ||
                modeStr == TimerMode.STOPWATCH.name ||
                modeStr == TimerMode.POMODORO.name
            if (!isFocusPeriod && !isBreak) return false
            if (prefs.getBoolean(KEY_IS_RUNNING, false) && modeStr != TimerMode.STOPWATCH.name) {
                val total = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60).coerceAtLeast(1)
                val savedRemaining = prefs.getInt(KEY_REMAINING_SECONDS, total).coerceIn(0, total)
                val savedAtMs = prefs.getLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
                val elapsed = ((System.currentTimeMillis() - savedAtMs) / 1000L).toInt().coerceAtLeast(0)
                if (savedRemaining - elapsed <= 0) return false
            }
            return true
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder  = TimerBinder()
    private val scope   = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val notificationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notificationUpdates = Channel<Notification>(Channel.CONFLATED)
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
    private var pomodoroFocusSeconds = 25 * 60
    private var pomodoroBreakSeconds = 5 * 60

    val secondsLeft:  StateFlow<Int>       = _secondsLeft
    val totalSeconds: StateFlow<Int>       = _totalSeconds
    val isRunning:    StateFlow<Boolean>   = _isRunning
    val timerMode:    StateFlow<TimerMode> = _timerMode
    val isMuted:      StateFlow<Boolean>   = _isMuted
    val targetPomodoroLoops: StateFlow<Int> = _targetPomodoroLoops
    val pomodorosCompleted: StateFlow<Int> = _pomodorosCompleted
    
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

    private fun clearTheme() {
        themePrefs().edit().clear().apply()
    }

    // ── Audio player (lives in the service — survives navigation) ─────────────
    private var musicPlayer: MediaPlayer? = null
    private var completionSoundPlayer: MediaPlayer? = null
    private var currentMusicUrl: String   = ""

    fun setMute(mute: Boolean) {
        _isMuted.value = mute
        if (_isRunning.value && musicPlayer != null) {
            val volume = if (mute) 0f else 0.7f
            musicPlayer?.setVolume(volume, volume)
        }
    }

    fun startPomodoroSession(loops: Int, focusMinutes: Int, breakMinutes: Int) {
        val focusSeconds = focusMinutes.coerceAtLeast(1) * 60
        val restSeconds = breakMinutes.coerceAtLeast(1) * 60
        setDuration(TimerMode.POMODORO, focusSeconds)
        _targetPomodoroLoops.value = loops.coerceAtLeast(1)
        _pomodorosCompleted.value = 0
        pomodoroFocusSeconds = focusSeconds
        pomodoroBreakSeconds = restSeconds
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
            musicPlayer = MediaPlayer().apply {
                setDataSource(this@TimerService, MediaFileCache.uriFor(this@TimerService, url))
                isLooping = true
                val volume = if (_isMuted.value) 0f else 0.7f
                setVolume(volume, volume)
                setOnPreparedListener { start() }
                prepareAsync()
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun releaseMusic() {
        val player = musicPlayer
        musicPlayer = null
        player?.let {
            kotlin.concurrent.thread {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
        }
    }

    private fun releaseCompletionSound() {
        val player = completionSoundPlayer
        completionSoundPlayer = null
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
    }

    private fun playCompletionSound() {
        releaseCompletionSound()
        val player = MediaPlayer()
        completionSoundPlayer = player

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            resources.openRawResourceFd(R.raw.timer_completion_twinkle).use { sound ->
                player.setDataSource(sound.fileDescriptor, sound.startOffset, sound.length)
            }
            player.setOnCompletionListener { completedPlayer ->
                if (completionSoundPlayer === completedPlayer) completionSoundPlayer = null
                runCatching { completedPlayer.release() }
            }
            player.setOnErrorListener { failedPlayer, _, _ ->
                if (completionSoundPlayer === failedPlayer) completionSoundPlayer = null
                runCatching { failedPlayer.release() }
                true
            }
            player.prepare()
            player.start()
        } catch (error: Exception) {
            if (completionSoundPlayer === player) completionSoundPlayer = null
            runCatching { player.release() }
            error.printStackTrace()
        }
    }

    private fun timerVibrator(): Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun vibrateForTimerCompletion() {
        val vibrator = timerVibrator()
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0L, 250L, 120L, 350L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
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
            )
        } else {
            TimerBubbleOverlay.hide()
        }
    }

    override fun onCreate() {
        super.onCreate()
        SafarNotificationChannels.createAll(this)
        notificationScope.launch {
            val notificationManager = getSystemService(NotificationManager::class.java)
            for (notification in notificationUpdates) {
                // notify() is a synchronous Binder call. Some OEM notification services can
                // stall it long enough to ANR the app, so never execute it on the main thread.
                runCatching { notificationManager.notify(NOTIFICATION_ID, notification) }
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
                timerAlertStyle = style
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
        restorePersistedTimerState()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_PAUSE -> if (_isRunning.value) pause()
            ACTION_RESET      -> reset()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopBecauseTaskWasRemoved()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        (application as? Application)?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        TimerBubbleOverlay.hide()
        releaseMusic()
        releaseCompletionSound()
        runCatching { timerVibrator().cancel() }
        clearTheme()
        focusPresenceJob?.cancel()
        notificationUpdates.close()
        notificationScope.cancel()
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
        themePrefs().edit()
            .putInt("theme_index", themeIndex)
            .putString("song_name", songName)
            .apply()
    }

    fun setDuration(mode: TimerMode, seconds: Int, breakSeconds: Int = 5 * 60) {
        // Reconfiguring the timer abandons whatever was running — an explicit choice.
        endKavachAnalyticsSession(KavachSessionOutcome.ENDED_EARLY)
        _timerMode.value    = mode
        val initialSeconds  = if (mode == TimerMode.STOPWATCH) 0 else seconds
        _secondsLeft.value  = initialSeconds
        _totalSeconds.value = initialSeconds
        standardBreakSeconds = breakSeconds
        _isRunning.value    = false
        _targetPomodoroLoops.value = 0
        _pomodorosCompleted.value = 0
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
        _secondsLeft.value = remainingSeconds.coerceIn(0, _totalSeconds.value)
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
        val current = autoSaveMetadata
        val shouldCreate = forceNew || current == null
        autoSaveMetadata = AutoSaveMetadata(
            clientSessionId = if (shouldCreate) "ekagra-${UUID.randomUUID()}" else current.clientSessionId,
            startedAt = if (shouldCreate) Instant.now().toString() else current.startedAt,
            taskTitle = taskTitle?.trim()?.takeIf { it.isNotBlank() } ?: current?.taskTitle,
            goalId = goalId?.takeIf { it.isNotBlank() && !it.startsWith("named:") } ?: current?.goalId,
            goalTitle = goalTitle?.trim()?.takeIf { it.isNotBlank() } ?: current?.goalTitle,
            topicId = topicId?.takeIf { it.isNotBlank() } ?: current?.topicId,
            planId = planId?.takeIf { it.isNotBlank() } ?: current?.planId,
            topicTitle = topicTitle?.trim()?.takeIf { it.isNotBlank() } ?: current?.topicTitle,
        )
        sessionSaveQueuedThisRun = false
        persistAutoSaveMetadata()
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

    internal fun focusProgressSnapshot(): FocusProgressSnapshot = calculateFocusProgress(
        mode = _timerMode.value,
        currentPeriodTotalSeconds = _totalSeconds.value,
        currentPeriodRemainingSeconds = _secondsLeft.value,
        pomodoroFocusSeconds = pomodoroFocusSeconds,
        targetPomodoroLoops = _targetPomodoroLoops.value,
        completedPomodoroLoops = _pomodorosCompleted.value,
    )

    fun startBreak(mode: TimerMode, seconds: Int): Boolean {
        if (mode == TimerMode.FOCUS || mode == TimerMode.POMODORO || seconds <= 0) return false

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

    fun start() {
        if (_secondsLeft.value <= 0 && _timerMode.value != TimerMode.STOPWATCH) return
        val resumingPausedSession = timerSessionActive && !_isRunning.value && _timerMode.value != TimerMode.BREAK
        _isRunning.value = true
        // Set before below — its polling loop's while
        // condition reads this on a freshly launched coroutine, so it must already
        // be true before that coroutine is scheduled, not after.
        timerSessionActive = true
        persistTimerState()

        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // Android 12+ prevents starting foreground services from the background.
            // If this happens (e.g., system recreates service), gracefully pause the timer instead of crashing.
            pause()
            return
        }

        syncNotificationShieldForTimerSession()
        if (resumingPausedSession) {
            focusShieldRepository().endQuickUnlockForEkagraResume()
        }
        focusShieldRepository().activateForSession(
            plannedSeconds = if (_timerMode.value == TimerMode.STOPWATCH) 0 else _totalSeconds.value,
            isFocusPeriod = _timerMode.value != TimerMode.BREAK,
        )
        startMusic(currentMusicUrl)
        lastTickElapsedMs = SystemClock.elapsedRealtime()
        // A session is now live. The pill shows only while SAFAR is backgrounded (syncBubble).
        syncBubble()
        tickJob?.cancel()
        tickJob = scope.launch {
            while (_isRunning.value && (_timerMode.value == TimerMode.STOPWATCH || _secondsLeft.value > 0)) {
                delay(1000L)
                val now = SystemClock.elapsedRealtime()
                val elapsedSeconds = ((now - lastTickElapsedMs) / 1000L).toInt().coerceAtLeast(1)
                lastTickElapsedMs = now
                if (_timerMode.value == TimerMode.STOPWATCH) {
                    _secondsLeft.value = _secondsLeft.value + elapsedSeconds
                } else {
                    _secondsLeft.value = (_secondsLeft.value - elapsedSeconds).coerceAtLeast(0)
                }
                persistTimerState()
                updateNotification()
                // Refresh the floating pill each tick (no-op when SAFAR is foregrounded).
                TimerBubbleOverlay.update(
                    secondsLeft  = _secondsLeft.value,
                    totalSeconds = _totalSeconds.value,
                    kavachActive = FocusShieldRepository.ShieldPrefs.isActive(applicationContext),
                    isRunning    = true,
                )
            }
            if (_secondsLeft.value == 0) {
                timerSessionActive = false
                TimerBubbleOverlay.hide()
                val focusState = suspendedFocusState
                if (focusState != null && _timerMode.value != TimerMode.FOCUS) {
                    // A mid-session break ("Take break") ran to completion and we're
                    // about to restore the suspended focus session. Save the break
                    // first — this path returns early, so without this the break time
                    // would be silently dropped.
                    if (_timerMode.value == TimerMode.BREAK) {
                        enqueueCompletedBreakSessionSave(
                            actualSeconds = _totalSeconds.value,
                            plannedSeconds = _totalSeconds.value,
                        )
                    }
                    suspendedFocusState = null
                    _timerMode.value = TimerMode.FOCUS
                    _totalSeconds.value = focusState.totalSeconds
                    _secondsLeft.value = focusState.remainingSeconds.coerceIn(1, focusState.totalSeconds)
                    _isRunning.value = false
                    persistTimerState()
                    releaseMusic()
                    // A manually started break has ended. Do not leave KAVACH's
                    // break protection marked active while the restored focus timer is paused.
                    updateNotification()
                    return@launch
                }
                
                _isRunning.value = false
                val completedMode = _timerMode.value
                if (completedMode == TimerMode.FOCUS || completedMode == TimerMode.POMODORO) {
                    val completedProgress = focusProgressSnapshot()
                    releaseMusic()
                    clearTheme()
                    showCompletionNotification()

                    // Handle next session based on Pomodoro mode
                    if (completedMode == TimerMode.POMODORO) {
                        _pomodorosCompleted.value += 1
                        if (_pomodorosCompleted.value >= _targetPomodoroLoops.value) {
                            // Save once for the whole logical Pomodoro series. Saving each
                            // focus loop independently creates duplicate history rows and
                            // makes an interrupted later loop lose earlier completed time.
                            enqueueCompletedFocusSessionSave(
                                totalSeconds = completedProgress.plannedSeconds,
                                actualSeconds = completedProgress.actualSeconds,
                                mode = TimerMode.POMODORO,
                            )
                            // The whole Pomodoro series finished — this is the one
                            // point where the Kavach session is genuinely complete.
                            endKavachAnalyticsSession(KavachSessionOutcome.COMPLETED)
                            clearPersistedTimerState()
                            _targetPomodoroLoops.value = 0
                            _pomodorosCompleted.value = 0
                            _timerMode.value = TimerMode.FOCUS // Reset to standard focus
                            _totalSeconds.value = pomodoroFocusSeconds
                            _secondsLeft.value = pomodoroFocusSeconds
                            persistTimerState()
                            // The visible timer is reset for the next session, but this
                            // Pomodoro set is complete so Notification Shield must end here.
                            NotificationShieldPrefs.clear(this@TimerService)
                            return@launch
                        }

                        _timerMode.value = TimerMode.BREAK
                        val breakLength = if (_pomodorosCompleted.value % 4 == 0) 15 * 60 else pomodoroBreakSeconds
                        _totalSeconds.value = breakLength
                        _secondsLeft.value = breakLength
                        persistTimerState()
                        start() // Pomodoro always auto-starts break
                        return@launch
                    } else {
                        enqueueCompletedFocusSessionSave(
                            totalSeconds = completedProgress.plannedSeconds,
                            actualSeconds = completedProgress.actualSeconds,
                            mode = completedMode,
                        )
                        endKavachAnalyticsSession(KavachSessionOutcome.COMPLETED)
                        clearPersistedTimerState()
                        // Standard: respect user's auto-start break preference
                        _timerMode.value = TimerMode.BREAK
                        _totalSeconds.value = standardBreakSeconds
                        _secondsLeft.value = standardBreakSeconds
                        persistTimerState()
                        if (autoStartBreak) {
                            start()
                        } else {
                            // Leave timer paused — user presses play when ready
                            _isRunning.value = false
                        }
                        return@launch
                    }
                }
                
                // If it was a BREAK and we are in the middle of a Pomodoro loop
                if (_timerMode.value == TimerMode.BREAK && _targetPomodoroLoops.value > 0) {
                    // The break ran to completion, so its elapsed time is its full length.
                    enqueueCompletedBreakSessionSave(
                        actualSeconds = _totalSeconds.value,
                        plannedSeconds = _totalSeconds.value,
                    )
                    _timerMode.value = TimerMode.POMODORO
                    _totalSeconds.value = pomodoroFocusSeconds
                    _secondsLeft.value = pomodoroFocusSeconds
                    persistTimerState()
                    start()
                    return@launch
                }
                
                // A standalone break just ran to completion — persist it before the
                // state below is reset, so break time actually lands in history.
                if (_timerMode.value == TimerMode.BREAK) {
                    enqueueCompletedBreakSessionSave(
                        actualSeconds = _totalSeconds.value,
                        plannedSeconds = _totalSeconds.value,
                    )
                }

                // The break just finished with nothing to resume into (not a
                // Pomodoro loop, no suspended focus session) — reset secondsLeft
                // back to the full break length so the UI offers a fresh "Start"
                // instead of a dead "Resume" that start() would silently reject
                // (start() bails out whenever secondsLeft <= 0).
                _secondsLeft.value = _totalSeconds.value
                clearPersistedTimerState()
                releaseMusic()
                clearTheme()
                showCompletionNotification()
                updateNotification()
            }
        }
    }

    fun pause() {
        _isRunning.value = false
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
    }

    fun reset() {
        // The student pressed Reset/End: an explicit early finish, counted once.
        endKavachAnalyticsSession(KavachSessionOutcome.ENDED_EARLY)
        _isRunning.value   = false
        _secondsLeft.value = if (_timerMode.value == TimerMode.STOPWATCH) 0 else _totalSeconds.value
        _targetPomodoroLoops.value = 0
        _pomodorosCompleted.value = 0
        sessionSaveQueuedThisRun = false
        suspendedFocusState = null
        tickJob?.cancel()
        clearPersistedTimerState()
        releaseMusic()
        clearTheme()
        updateNotification()
        // Session ended — remove the pill.
        timerSessionActive = false
        TimerBubbleOverlay.hide()
    }

    fun isActive(): Boolean = _isRunning.value || (if (_timerMode.value == TimerMode.STOPWATCH) _secondsLeft.value > 0 else _secondsLeft.value < _totalSeconds.value)

    private fun stopBecauseTaskWasRemoved() {
        val isPomodoroSeries = _targetPomodoroLoops.value > 0
        if ((_timerMode.value == TimerMode.FOCUS || _timerMode.value == TimerMode.STOPWATCH || isPomodoroSeries) && !sessionSaveQueuedThisRun) {
            val progress = focusProgressSnapshot()
            enqueueCompletedFocusSessionSave(
                totalSeconds = progress.plannedSeconds,
                actualSeconds = progress.actualSeconds,
                mode = if (isPomodoroSeries) TimerMode.POMODORO else _timerMode.value,
            )
        }
        // The task was swiped away, not ended from the timer. That is a failure of
        // the process, not of the student, so it is reported as interrupted.
        endKavachAnalyticsSession(KavachSessionOutcome.INTERRUPTED)
        _isRunning.value = false
        tickJob?.cancel()
        suspendedFocusState = null
        clearPersistedTimerState()
        releaseMusic()
        clearTheme()
        stopForegroundCompat()
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        stopSelf()
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
        timerStatePrefs().edit()
            .putString(KEY_AUTO_SAVE_CLIENT_SESSION_ID, metadata.clientSessionId)
            .putString(KEY_AUTO_SAVE_STARTED_AT, metadata.startedAt)
            .putString(KEY_AUTO_SAVE_TASK_TITLE, metadata.taskTitle)
            .putString(KEY_AUTO_SAVE_GOAL_ID, metadata.goalId)
            .putString(KEY_AUTO_SAVE_GOAL_TITLE, metadata.goalTitle)
            .putString(KEY_AUTO_SAVE_TOPIC_ID, metadata.topicId)
            .putString(KEY_AUTO_SAVE_PLAN_ID, metadata.planId)
            .putString(KEY_AUTO_SAVE_TOPIC_TITLE, metadata.topicTitle)
            .apply()
    }

    private fun restoreAutoSaveMetadata(prefs: SharedPreferences) {
        val clientSessionId = prefs.getString(KEY_AUTO_SAVE_CLIENT_SESSION_ID, null)
            ?.takeIf { it.isNotBlank() }
        val startedAt = prefs.getString(KEY_AUTO_SAVE_STARTED_AT, null)
            ?.takeIf { it.isNotBlank() }
        autoSaveMetadata = if (clientSessionId != null && startedAt != null) {
            AutoSaveMetadata(
                clientSessionId = clientSessionId,
                startedAt = startedAt,
                taskTitle = prefs.getString(KEY_AUTO_SAVE_TASK_TITLE, null)?.takeIf { it.isNotBlank() },
                goalId = prefs.getString(KEY_AUTO_SAVE_GOAL_ID, null)?.takeIf { it.isNotBlank() },
                goalTitle = prefs.getString(KEY_AUTO_SAVE_GOAL_TITLE, null)?.takeIf { it.isNotBlank() },
                topicId = prefs.getString(KEY_AUTO_SAVE_TOPIC_ID, null)?.takeIf { it.isNotBlank() },
                planId = prefs.getString(KEY_AUTO_SAVE_PLAN_ID, null)?.takeIf { it.isNotBlank() },
                topicTitle = prefs.getString(KEY_AUTO_SAVE_TOPIC_TITLE, null)?.takeIf { it.isNotBlank() },
            )
        } else {
            null
        }
    }

    private fun enqueueCompletedFocusSessionSave(
        totalSeconds: Int,
        actualSeconds: Int,
        mode: TimerMode,
    ) {
        val total = totalSeconds.coerceAtLeast(1)
        val actual = actualSeconds.coerceIn(0, total)
        if (actual == 0) return
        val endedAt = Instant.now().toString()
        val metadata = autoSaveMetadata ?: AutoSaveMetadata(
            clientSessionId = "ekagra-${UUID.randomUUID()}",
            startedAt = Instant.now().minusSeconds(actual.toLong()).toString(),
            taskTitle = null,
            goalId = null,
            goalTitle = null,
            topicId = null,
            planId = null,
            topicTitle = null,
        )
        // Natural timer completion is always preserved as a plain Untitled
        // Ekagra session. Goal linking is an optional later action from History.
        val title = DEFAULT_UNTITLED_SESSION_TITLE
        EkagraPendingSessionSaveStore.enqueue(
            this,
            PendingEkagraSessionSave(
                clientSessionId = metadata.clientSessionId,
                mode = mode.toApiMode(),
                startedAt = metadata.startedAt,
                endedAt = endedAt,
                plannedDurationMinutes = if (mode == TimerMode.STOPWATCH) 0 else (total + 59) / 60,
                actualDurationMinutes = (actual / 60.0).roundToInt(),
                actualDurationSeconds = actual,
                goalId = null,
                goalTitle = null,
                topicId = null,
                planId = null,
                topicTitle = null,
                taskTitle = title,
                shieldEnabled = FocusShieldRepository.ShieldPrefs.isActive(this),
            ),
        )
        EkagraSessionSaveWorker.enqueue(this)
        flushPendingSavesNow()
        sessionSaveQueuedThisRun = true
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
    private fun enqueueCompletedBreakSessionSave(actualSeconds: Int, plannedSeconds: Int) {
        val actual = actualSeconds.coerceAtLeast(0)
        if (actual <= 0) return
        val planned = plannedSeconds.coerceAtLeast(actual)
        EkagraPendingSessionSaveStore.enqueue(
            this,
            PendingEkagraSessionSave(
                clientSessionId = "ekagra-break-${UUID.randomUUID()}",
                mode = TimerMode.BREAK.toApiMode(),
                startedAt = Instant.now().minusSeconds(actual.toLong()).toString(),
                endedAt = Instant.now().toString(),
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
                shieldEnabled = false,
            ),
        )
        EkagraSessionSaveWorker.enqueue(this)
        flushPendingSavesNow()
    }

    private fun persistTimerState() {
        val total = if (_timerMode.value == TimerMode.STOPWATCH) _secondsLeft.value else _totalSeconds.value
        val remaining = if (_timerMode.value == TimerMode.STOPWATCH) _secondsLeft.value else _secondsLeft.value.coerceIn(0, total.coerceAtLeast(1))
        val shouldPersist = _isRunning.value || (_timerMode.value == TimerMode.STOPWATCH && remaining > 0) || (remaining < total)
        if (!shouldPersist) {
            clearPersistedTimerState()
            return
        }

        val suspended = suspendedFocusState
        timerStatePrefs().edit()
            .putBoolean(KEY_HAS_STATE, true)
            .putString(KEY_MODE, _timerMode.value.name)
            .putInt(KEY_TOTAL_SECONDS, total)
            .putInt(KEY_REMAINING_SECONDS, remaining)
            .putBoolean(KEY_IS_RUNNING, _isRunning.value)
            .putLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
            .putInt(KEY_SUSPENDED_TOTAL_SECONDS, suspended?.totalSeconds ?: 0)
            .putInt(KEY_SUSPENDED_REMAINING_SECONDS, suspended?.remainingSeconds ?: 0)
            .putInt(KEY_STANDARD_BREAK_SECONDS, standardBreakSeconds)
            .putInt(KEY_TARGET_POMODORO_LOOPS, _targetPomodoroLoops.value)
            .putInt(KEY_COMPLETED_POMODORO_LOOPS, _pomodorosCompleted.value)
            .putInt(KEY_POMODORO_FOCUS_SECONDS, pomodoroFocusSeconds)
            .putInt(KEY_POMODORO_BREAK_SECONDS, pomodoroBreakSeconds)
            .apply()
    }

    private fun restorePersistedTimerState() {
        val prefs = timerStatePrefs()
        if (!prefs.getBoolean(KEY_HAS_STATE, false)) return
        restoreAutoSaveMetadata(prefs)

        val mode = runCatching {
            TimerMode.valueOf(prefs.getString(KEY_MODE, TimerMode.FOCUS.name) ?: TimerMode.FOCUS.name)
        }.getOrDefault(TimerMode.FOCUS)
        val total = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60).coerceAtLeast(60)
        val savedRemaining = prefs.getInt(KEY_REMAINING_SECONDS, total).coerceIn(0, total)
        val wasRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val savedAtMs = prefs.getLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
        val elapsedWhileRunning = if (wasRunning) {
            ((System.currentTimeMillis() - savedAtMs) / 1000L).toInt().coerceAtLeast(0)
        } else {
            0
        }
        val remaining = (savedRemaining - elapsedWhileRunning).coerceIn(0, total)
        val suspendedTotal = prefs.getInt(KEY_SUSPENDED_TOTAL_SECONDS, 0)
        val suspendedRemaining = prefs.getInt(KEY_SUSPENDED_REMAINING_SECONDS, 0)
        standardBreakSeconds = prefs.getInt(KEY_STANDARD_BREAK_SECONDS, 5 * 60)
        _targetPomodoroLoops.value = prefs.getInt(KEY_TARGET_POMODORO_LOOPS, 0).coerceAtLeast(0)
        _pomodorosCompleted.value = prefs.getInt(KEY_COMPLETED_POMODORO_LOOPS, 0)
            .coerceIn(0, _targetPomodoroLoops.value)
        pomodoroFocusSeconds = prefs.getInt(KEY_POMODORO_FOCUS_SECONDS, 25 * 60).coerceAtLeast(1)
        pomodoroBreakSeconds = prefs.getInt(KEY_POMODORO_BREAK_SECONDS, 5 * 60).coerceAtLeast(1)

        _timerMode.value = mode
        _totalSeconds.value = total
        _secondsLeft.value = remaining
        _isRunning.value = false
        suspendedFocusState = if (suspendedTotal > 0 && suspendedRemaining > 0) {
            SuspendedFocusState(
                totalSeconds = suspendedTotal,
                remainingSeconds = suspendedRemaining.coerceIn(1, suspendedTotal),
            )
        } else {
            null
        }

        if (remaining <= 0) {
            if (wasRunning) {
                if (_targetPomodoroLoops.value > 0) {
                    // A process restart ends the in-memory loop chain. Preserve every
                    // completed loop plus the just-finished current loop as one entry.
                    val progress = focusProgressSnapshot()
                    enqueueCompletedFocusSessionSave(
                        totalSeconds = progress.plannedSeconds,
                        actualSeconds = progress.actualSeconds,
                        mode = TimerMode.POMODORO,
                    )
                } else if (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH) {
                    enqueueCompletedFocusSessionSave(
                        totalSeconds = total,
                        actualSeconds = total,
                        mode = mode,
                    )
                }
            }
            clearPersistedTimerState()
        } else if (wasRunning) {
            start()
        } else {
            updateNotification()
        }
    }

    private fun clearPersistedTimerState() {
        autoSaveMetadata = null
        timerStatePrefs().edit().clear().apply()
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            NotificationDeepLinkHandler.activityIntent(this, "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resetIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply { action = ACTION_RESET },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val s    = _secondsLeft.value
        val mode = _timerMode.value.label
        val time = "%02d:%02d".format(s / 60, s % 60)
        val notificationText = when {
            _isRunning.value && (_timerMode.value == TimerMode.FOCUS || _timerMode.value == TimerMode.STOPWATCH) -> "Ekagra in progress"
            _isRunning.value -> "Break in progress - KAVACH paused"
            else -> "Timer paused"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$mode \u00b7 $time")
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
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetIntent)
            .build()
    }

    private fun personalizeNotificationBody(body: String): String = body

    private fun startsWithPersonalGreeting(body: String): Boolean {
        return Regex(
            pattern = "^\\s*(hi|hey|hello|good morning|good afternoon|good evening)\\b",
            option = RegexOption.IGNORE_CASE,
        ).containsMatchIn(body)
    }

    private fun showCompletionNotification() {
        val completedMode = _timerMode.value

        when (timerAlertStyle) {
            TimerAlertStyle.SOUND -> playCompletionSound()
            TimerAlertStyle.VIBRATE -> {
                releaseCompletionSound()
                vibrateForTimerCompletion()
            }
            TimerAlertStyle.OFF -> releaseCompletionSound()
        }

        scope.launch {
            if (!safarDataStore.notificationsEnabled.first() ||
                !safarDataStore.focusTimerNotificationsEnabled.first()
            ) return@launch

            val body = when (completedMode) {
                TimerMode.FOCUS,
                TimerMode.POMODORO -> "Ekagra session complete. Great work - take a mindful break."
                TimerMode.BREAK,
                TimerMode.STOPWATCH -> "Break finished. Ready for your next session?"
            }
            SafarNotificationManager(this@TimerService).show(
                title = if (completedMode == TimerMode.FOCUS || completedMode == TimerMode.POMODORO) "Ekagra session complete" else "Break finished",
                body = body,
                channelId = SafarNotificationChannels.FOCUS_TIMER,
                deepLink = "safar://ekagra",
                notificationId = COMPLETION_NOTIFICATION_ID,
            )
        }
    }

    private fun updateNotification() {
        if (_isRunning.value || _secondsLeft.value < _totalSeconds.value) {
            // Timer ticks arrive once per second. A conflated channel retains only the newest
            // notification if the system notification service is slow, avoiding both a main-
            // thread Binder stall and an unbounded backlog of obsolete timer values.
            notificationUpdates.trySend(buildNotification())
        } else {
            stopForegroundCompat()
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
}
