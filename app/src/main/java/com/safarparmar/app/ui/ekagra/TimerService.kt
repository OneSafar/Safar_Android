package com.safarparmar.app.ui.ekagra

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.MainActivity
import com.safarparmar.app.R
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.notifications.SafarNotificationManager
import com.safarparmar.app.ui.ekagra.focusshield.BlockedAppActivity
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldEntryPoint
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository
import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class TimerService : Service() {

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
        private const val KEY_AUTO_SAVE_CLIENT_SESSION_ID = "auto_save_client_session_id"
        private const val KEY_AUTO_SAVE_STARTED_AT = "auto_save_started_at"
        private const val KEY_AUTO_SAVE_TASK_TITLE = "auto_save_task_title"
        private const val KEY_AUTO_SAVE_GOAL_ID = "auto_save_goal_id"
        private const val KEY_AUTO_SAVE_GOAL_TITLE = "auto_save_goal_title"
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
            if (modeStr != TimerMode.FOCUS.name && modeStr != TimerMode.STOPWATCH.name) return false

            val total = prefs.getInt(KEY_TOTAL_SECONDS, 25 * 60).coerceAtLeast(1)
            val savedRemaining = prefs.getInt(KEY_REMAINING_SECONDS, total).coerceIn(0, total)
            val savedAtMs = prefs.getLong(KEY_SAVED_AT_MS, System.currentTimeMillis())
            val elapsed = ((System.currentTimeMillis() - savedAtMs) / 1000L).toInt().coerceAtLeast(0)
            return savedRemaining - elapsed > 0
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    private val binder  = TimerBinder()
    private val scope   = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickJob: Job? = null
    private var shieldActivationJob: Job? = null
    private var lastTickElapsedMs: Long = 0L
    private val safarDataStore by lazy { SafarDataStore(applicationContext) }
    private val homePackages: Set<String> by lazy { resolveHomePackages() }
    private var shieldMonitorJob: Job? = null
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

    // ── Focus Shield state ─────────────────────────────────────────────────
    private val _focusShieldActive  = MutableStateFlow(false)
    private val _blockedPackages    = MutableStateFlow<Set<String>>(emptySet())
    private val _strictMode         = MutableStateFlow(false)

    val focusShieldActive: StateFlow<Boolean>      = _focusShieldActive
    val blockedPackages:   StateFlow<Set<String>>  = _blockedPackages
    val strictMode:        StateFlow<Boolean>      = _strictMode

    /**
     * Called by EkagraScreen/ViewModel when a focus session starts
     * and Focus Shield is enabled.
     */
    fun setFocusShieldConfig(
        packages: Set<String>,
        strict: Boolean,
    ) {
        _blockedPackages.value = packages
        _strictMode.value = strict
        debugFocusShield("TimerService.setFocusShieldConfig(${packages.size} pkgs, strict=$strict)")
    }

    fun enableFocusShieldForSession() {
        val pkgs = _blockedPackages.value
        if (pkgs.isEmpty()) {
            disableFocusShieldForSession()
            return
        }
        if (!FocusShieldPermissionHelper.hasUsageStatsPermission(this)) {
            debugFocusShield("Ekagra Shield not enabled: Usage access missing")
            disableFocusShieldForSession()
            return
        }
        if (
            FocusShieldPermissionHelper.isAccessibilityFeatureEnabled() &&
            !FocusShieldPermissionHelper.hasAccessibilityService(this)
        ) {
            debugFocusShield("Ekagra Shield not enabled: Accessibility service missing")
            disableFocusShieldForSession()
            return
        }
        if (!FocusShieldPermissionHelper.hasNotificationListenerAccess(this)) {
            debugFocusShield("Ekagra Shield not enabled: notification listener access missing")
            disableFocusShieldForSession()
            return
        }
        FocusShieldPermissionHelper.requestNotificationListenerRebind(this)

        _focusShieldActive.value = true
        val strict = _strictMode.value

        // Read emergency unlock policy synchronously from DataStore so block UI can honour it.
        scope.launch {
            val allowEmergency = safarDataStore.focusShieldEmergencyUnlock.first()
            val unlockLimit = if (allowEmergency && !strict)
                safarDataStore.focusShieldEmergencyUnlocksPerSession.first()
            else 0
            val unlockSeconds = safarDataStore.focusShieldEmergencyUnlockSeconds.first()
            // Write to SharedPreferences (survives process death!). Reset session counters.
            FocusShieldRepository.ShieldPrefs.write(
                ctx = this@TimerService,
                active = true,
                packages = pkgs,
                strict = strict,
                unlockLimit = unlockLimit,
                unlockSeconds = unlockSeconds,
                resetUnlocks = true,
            )
        }

        // Also update volatile snapshot for in-process fast access
        FocusShieldRepository.Snapshot.active = true
        FocusShieldRepository.Snapshot.packages = pkgs
        FocusShieldRepository.Snapshot.strict = strict

        debugFocusShield("TimerService.enableFocusShieldForSession()")
        focusShieldRepo().activateForSession()
        showFocusShieldActiveNotification()
    }

    fun disableFocusShieldForSession(force: Boolean = false) {
        debugFocusShield("TimerService.disableFocusShieldForSession()")
        val repo = focusShieldRepo()

        _focusShieldActive.value = false

        repo.deactivateSession()

        // Clear SharedPreferences
        shieldActivationJob?.cancel()
        FocusShieldRepository.ShieldPrefs.clear(this)

        // Clear volatile snapshot
        FocusShieldRepository.Snapshot.active = false
        FocusShieldRepository.Snapshot.packages = emptySet()

        getSystemService(NotificationManager::class.java)
            .cancel(FOCUS_SHIELD_ACTIVE_NOTIFICATION_ID)

    }

    private fun activateFocusShieldFromSettingsIfNeeded() {
        if (_timerMode.value != TimerMode.FOCUS && _timerMode.value != TimerMode.STOPWATCH) {
            disableFocusShieldForSession()
            return
        }

        shieldActivationJob?.cancel()
        shieldActivationJob = scope.launch {
            val enabled = safarDataStore.focusShieldEnabled.first()
            val packages = safarDataStore.focusShieldBlockedPackages.first()
            val strict = safarDataStore.focusShieldStrictMode.first()

            if (enabled && packages.isNotEmpty()) {
                debugFocusShield("TimerService.start() activating from persisted settings: ${packages.size} packages")
                setFocusShieldConfig(packages = packages, strict = strict)
                enableFocusShieldForSession()
            } else {
                debugFocusShield("TimerService.start() shield not activated (enabled=$enabled, pkgs=${packages.size})")
                disableFocusShieldForSession()
            }
        }
    }

    private fun startFocusShieldMonitor() {
        shieldMonitorJob?.cancel()
        shieldMonitorJob = scope.launch {
            while (_isRunning.value) {
                syncFocusShieldState()
                delay(1_500L)
            }
        }
    }

    private fun stopFocusShieldMonitor() {
        shieldMonitorJob?.cancel()
        shieldMonitorJob = null
    }

    private suspend fun syncFocusShieldState() {
        if ((_timerMode.value != TimerMode.FOCUS && _timerMode.value != TimerMode.STOPWATCH) || !_isRunning.value) {
            disableFocusShieldForSession()
            return
        }

        val enabled = safarDataStore.focusShieldEnabled.first()
        val packages = safarDataStore.focusShieldBlockedPackages.first()
        val strict = safarDataStore.focusShieldStrictMode.first()

        if (
            !enabled ||
            packages.isEmpty() ||
            !FocusShieldPermissionHelper.hasUsageStatsPermission(this) ||
            (
                FocusShieldPermissionHelper.isAccessibilityFeatureEnabled() &&
                    !FocusShieldPermissionHelper.hasAccessibilityService(this)
                )
        ) {
            disableFocusShieldForSession()
            return
        }

        val shouldUpdate = !_focusShieldActive.value ||
            !FocusShieldRepository.ShieldPrefs.isActive(this) ||
            packages != _blockedPackages.value ||
            strict != _strictMode.value

        if (shouldUpdate) {
            setFocusShieldConfig(packages = packages, strict = strict)
            enableFocusShieldForSession()
        }
    }

    private fun handleFocusShieldBlockedIntent(intent: Intent?) {
        val blockedPackage = intent
            ?.getStringExtra(BlockedAppActivity.EXTRA_BLOCKED_PACKAGE)
            .orEmpty()
        if (blockedPackage.isBlank()) return
        if (isHomePackage(blockedPackage)) return

        debugFocusShield("Blocked intent received for $blockedPackage")

        if (!FocusShieldRepository.ShieldPrefs.isActive(this)) return
        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)
        if (blockedPackage !in blockedPackages) return

        // The block UI is launched by the accessibility service; here we only provide a safe return notification.
        showFocusShieldBlockedNotification(blockedPackage)
    }

    private fun showFocusShieldBlockedNotification(blockedPackage: String) {
        if (isHomePackage(blockedPackage)) return
        val appName = labelForPackage(blockedPackage)
        val strict = FocusShieldRepository.ShieldPrefs.isStrict(this)
        val openEkagra = isFocusTimerRunning(this)
        val returnLabel = if (strict) "Return to Ekagra" else "Return to SAFAR"
        val focusPendingIntent = PendingIntent.getActivity(
            this,
            4,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.EXTRA_FOCUS_SHIELD_BLOCKED_PACKAGE, blockedPackage)
                putExtra(MainActivity.EXTRA_FOCUS_SHIELD_BLOCKED_APP_NAME, appName)
                putExtra(MainActivity.EXTRA_FOCUS_SHIELD_STRICT, strict)
                putExtra(
                    MainActivity.EXTRA_FOCUS_SHIELD_UNLOCKS_REMAINING,
                    FocusShieldRepository.ShieldPrefs.getUnlocksRemaining(this@TimerService),
                )
                putExtra(
                    MainActivity.EXTRA_FOCUS_SHIELD_UNLOCK_SECONDS,
                    FocusShieldRepository.ShieldPrefs.getUnlockSeconds(this@TimerService),
                )
                putExtra(MainActivity.EXTRA_FOCUS_SHIELD_OPEN_EKAGRA, openEkagra)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val blockedText = personalizeNotificationBody("$appName is blocked while KAVACH is active.")
        val blockedLongText = personalizeNotificationBody("$appName is blocked while KAVACH is active. Tap to return to SAFAR.")

        val notification = NotificationCompat.Builder(this, SafarNotificationChannels.FOCUS_SHIELD_BLOCKED)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentTitle("KAVACH is active")
            .setContentText(blockedText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(blockedLongText),
            )
            .setContentIntent(focusPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_revert,
                returnLabel,
                focusPendingIntent,
            )
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(FOCUS_SHIELD_BLOCKED_NOTIFICATION_ID, notification)
    }

    private fun showFocusShieldActiveNotification() {
        val focusPendingIntent = PendingIntent.getActivity(
            this,
            4,
            NotificationDeepLinkHandler.activityIntent(this, "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val activeText = personalizeNotificationBody("Selected distracting apps are blocked while KAVACH is active.")
        val activeLongText = personalizeNotificationBody("Selected distracting apps are blocked while KAVACH is active. SAFAR uses Accessibility only to detect opened app package names for this feature.")

        val notification = NotificationCompat.Builder(this, SafarNotificationChannels.FOCUS_SHIELD_STATUS)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentTitle("KAVACH is active")
            .setContentText(activeText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(activeLongText),
            )
            .setContentIntent(focusPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(FOCUS_SHIELD_ACTIVE_NOTIFICATION_ID, notification)
    }

    private fun labelForPackage(packageName: String): String {
        if (packageName.isBlank()) return "This app"
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault("This app")
    }

    private fun isHomePackage(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        return packageName in homePackages ||
            packageName.contains("launcher", ignoreCase = true)
    }

    private fun resolveHomePackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val queried = packageManager.queryIntentActivities(homeIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
        val resolved = packageManager.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
        return (queried + listOfNotNull(resolved) + KNOWN_HOME_PACKAGES).toSet()
    }

    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─
    private fun themePrefs() = getSharedPreferences("ekagra_theme_prefs", MODE_PRIVATE)

    private fun debugFocusShield(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("FocusShield", message)
    }

    private fun focusShieldRepo(): FocusShieldRepository =
        EntryPointAccessors.fromApplication(applicationContext, FocusShieldEntryPoint::class.java)
            .focusShieldRepository()

    private fun clearTheme() {
        themePrefs().edit().clear().commit()
    }

    // ── Audio player (lives in the service — survives navigation) ─────────────
    private var musicPlayer: MediaPlayer? = null
    private var currentMusicUrl: String   = ""

    fun setMute(mute: Boolean) {
        _isMuted.value = mute
        if (_isRunning.value && musicPlayer != null) {
            val volume = if (mute) 0f else 0.7f
            musicPlayer?.setVolume(volume, volume)
        }
    }

    fun startPomodoroSession(loops: Int, focusMinutes: Int, breakMinutes: Int) {
        _targetPomodoroLoops.value = loops
        _pomodorosCompleted.value = 0
        pomodoroFocusSeconds = focusMinutes * 60
        pomodoroBreakSeconds = breakMinutes * 60
        setDuration(TimerMode.POMODORO, pomodoroFocusSeconds)
        start()
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
                setDataSource(this@TimerService, Uri.parse(url))
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        SafarNotificationChannels.createAll(this)
        scope.launch {
            safarDataStore.userName.collect { name ->
                cachedUserName = name?.trim().orEmpty()
            }
        }
        restorePersistedTimerState()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_PAUSE -> if (_isRunning.value) pause()
            ACTION_RESET      -> reset()
            ACTION_FOCUS_SHIELD_BLOCKED -> handleFocusShieldBlockedIntent(intent)
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopBecauseTaskWasRemoved()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
        releaseMusic()
        clearTheme()
        scope.cancel()
        super.onDestroy()
    }

    // ── Public API ────────────────────────────────────────────────────────────
    fun saveTheme(themeIndex: Int, songName: String) {
        themePrefs().edit()
            .putInt("theme_index", themeIndex)
            .putString("song_name", songName)
            .apply()
    }

    fun setDuration(mode: TimerMode, seconds: Int) {
        _timerMode.value    = mode
        val initialSeconds  = if (mode == TimerMode.STOPWATCH) 0 else seconds
        _secondsLeft.value  = initialSeconds
        _totalSeconds.value = initialSeconds
        _isRunning.value    = false
        suspendedFocusState = null
        tickJob?.cancel()
        releaseMusic()
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
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
        )
        sessionSaveQueuedThisRun = false
        persistAutoSaveMetadata()
    }

    fun togglePlayPause() {
        if (_isRunning.value) pause() else start()
    }

    fun switchToFocusFromBreak(): Boolean {
        val focusState = suspendedFocusState ?: return _timerMode.value == TimerMode.FOCUS
        tickJob?.cancel()
        suspendedFocusState = null
        _timerMode.value = TimerMode.FOCUS
        _totalSeconds.value = focusState.totalSeconds
        _secondsLeft.value = focusState.remainingSeconds.coerceIn(1, focusState.totalSeconds)
        _isRunning.value = false
        releaseMusic()
        stopFocusShieldMonitor()
        disableFocusShieldForSession()
        persistTimerState()
        updateNotification()
        return true
    }

    fun startBreak(mode: TimerMode, seconds: Int): Boolean {
        if (mode == TimerMode.FOCUS || seconds <= 0) return false

        val focusState = when {
            _timerMode.value == TimerMode.FOCUS && _secondsLeft.value > 0 -> {
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
        stopFocusShieldMonitor()
        disableFocusShieldForSession()
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
        _isRunning.value = true
        persistTimerState()
        
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // Android 12+ prevents starting foreground services from the background.
            // If this happens (e.g., system recreates service), gracefully pause the timer instead of crashing.
            pause()
            return
        }
        
        activateFocusShieldFromSettingsIfNeeded()
        startFocusShieldMonitor()
        startMusic(currentMusicUrl)
        lastTickElapsedMs = SystemClock.elapsedRealtime()
        tickJob?.cancel()
        tickJob = scope.launch {
            while (_isRunning.value && (_timerMode.value == TimerMode.STOPWATCH || _secondsLeft.value > 0)) {
                delay(1000L)
                val now = SystemClock.elapsedRealtime()
                val elapsedSeconds = ((now - lastTickElapsedMs) / 1000L).toInt().coerceAtLeast(1)
                lastTickElapsedMs = now
                if (elapsedSeconds > 10 * 60) {
                    pause()
                    break
                }
                if (_timerMode.value == TimerMode.STOPWATCH) {
                    _secondsLeft.value = _secondsLeft.value + elapsedSeconds
                } else {
                    _secondsLeft.value = (_secondsLeft.value - elapsedSeconds).coerceAtLeast(0)
                }
                persistTimerState()
                updateNotification()
            }
            if (_secondsLeft.value == 0) {
                val focusState = suspendedFocusState
                if (focusState != null && _timerMode.value != TimerMode.FOCUS) {
                    suspendedFocusState = null
                    _timerMode.value = TimerMode.FOCUS
                    _totalSeconds.value = focusState.totalSeconds
                    _secondsLeft.value = focusState.remainingSeconds.coerceIn(1, focusState.totalSeconds)
                    _isRunning.value = false
                    persistTimerState()
                    releaseMusic()
                    updateNotification()
                    return@launch
                }
                
                _isRunning.value = false
                if (_timerMode.value == TimerMode.FOCUS || _timerMode.value == TimerMode.POMODORO) {
                    enqueueCompletedFocusSessionSave(
                        totalSeconds = _totalSeconds.value,
                        actualSeconds = _totalSeconds.value,
                        mode = _timerMode.value,
                    )
                    
                    clearPersistedTimerState()
                    releaseMusic()
                    clearTheme()
                    disableFocusShieldForSession()
                    stopFocusShieldMonitor()
                    showCompletionNotification()
                    
                    // Handle next session based on Pomodoro mode
                    if (_timerMode.value == TimerMode.POMODORO) {
                        _pomodorosCompleted.value += 1
                        if (_pomodorosCompleted.value >= _targetPomodoroLoops.value) {
                            // Target loops reached, end completely
                            _targetPomodoroLoops.value = 0
                            _timerMode.value = TimerMode.FOCUS // Reset to standard focus
                            _totalSeconds.value = pomodoroFocusSeconds
                            _secondsLeft.value = pomodoroFocusSeconds
                            persistTimerState()
                            return@launch
                        }
                        
                        _timerMode.value = TimerMode.BREAK
                        val breakLength = if (_pomodorosCompleted.value % 4 == 0) 15 * 60 else pomodoroBreakSeconds
                        _totalSeconds.value = breakLength
                        _secondsLeft.value = breakLength
                        persistTimerState()
                        start()
                        return@launch
                    } else {
                        // Standard auto-start a 5-minute break
                        _timerMode.value = TimerMode.BREAK
                        _totalSeconds.value = 5 * 60
                        _secondsLeft.value = 5 * 60
                        persistTimerState()
                        start()
                        return@launch
                    }
                }
                
                // If it was a BREAK and we are in the middle of a Pomodoro loop
                if (_timerMode.value == TimerMode.BREAK && _targetPomodoroLoops.value > 0) {
                    _timerMode.value = TimerMode.POMODORO
                    _totalSeconds.value = pomodoroFocusSeconds
                    _secondsLeft.value = pomodoroFocusSeconds
                    persistTimerState()
                    start()
                    return@launch
                }
                
                clearPersistedTimerState()
                releaseMusic()
                clearTheme()
                disableFocusShieldForSession()
                stopFocusShieldMonitor()
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
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
        updateNotification()
    }

    fun reset() {
        _isRunning.value   = false
        _secondsLeft.value = if (_timerMode.value == TimerMode.STOPWATCH) 0 else _totalSeconds.value
        suspendedFocusState = null
        tickJob?.cancel()
        clearPersistedTimerState()
        releaseMusic()
        clearTheme()
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
        updateNotification()
    }

    fun isActive(): Boolean = _isRunning.value || (if (_timerMode.value == TimerMode.STOPWATCH) _secondsLeft.value > 0 else _secondsLeft.value < _totalSeconds.value)

    private fun stopBecauseTaskWasRemoved() {
        if ((_timerMode.value == TimerMode.FOCUS || _timerMode.value == TimerMode.STOPWATCH) && !sessionSaveQueuedThisRun) {
            enqueueFocusSessionSaveIfElapsed(
                totalSeconds = _totalSeconds.value,
                remainingSeconds = _secondsLeft.value,
                mode = _timerMode.value,
            )
        }
        _isRunning.value = false
        tickJob?.cancel()
        suspendedFocusState = null
        clearPersistedTimerState()
        releaseMusic()
        clearTheme()
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
        stopForegroundCompat()
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        stopSelf()
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
        val total = totalSeconds.coerceAtLeast(60)
        val actual = actualSeconds.coerceIn(60, total)
        val endedAt = Instant.now().toString()
        val metadata = autoSaveMetadata ?: AutoSaveMetadata(
            clientSessionId = "ekagra-${UUID.randomUUID()}",
            startedAt = Instant.now().minusSeconds(actual.toLong()).toString(),
            taskTitle = null,
            goalId = null,
            goalTitle = null,
        )
        val title = metadata.taskTitle
            ?: metadata.goalTitle
            ?: DEFAULT_UNTITLED_SESSION_TITLE
        EkagraPendingSessionSaveStore.enqueue(
            this,
            PendingEkagraSessionSave(
                clientSessionId = metadata.clientSessionId,
                mode = mode.toApiMode(),
                startedAt = metadata.startedAt,
                endedAt = endedAt,
                plannedDurationMinutes = if (mode == TimerMode.STOPWATCH) 0 else (total + 59) / 60,
                actualDurationMinutes = (actual + 59) / 60,
                goalId = metadata.goalId,
                goalTitle = metadata.goalTitle,
                taskTitle = title,
                shieldEnabled = _focusShieldActive.value || FocusShieldRepository.ShieldPrefs.isActive(this),
            ),
        )
        EkagraSessionSaveWorker.enqueue(this)
        sessionSaveQueuedThisRun = true
    }

    private fun enqueueFocusSessionSaveIfElapsed(
        totalSeconds: Int,
        remainingSeconds: Int,
        mode: TimerMode,
    ) {
        val total = totalSeconds.coerceAtLeast(60)
        val elapsed = if (mode == TimerMode.STOPWATCH) remainingSeconds else (total - remainingSeconds.coerceIn(0, total)).coerceAtLeast(0)
        if (elapsed <= 0) return
        enqueueCompletedFocusSessionSave(
            totalSeconds = if (mode == TimerMode.STOPWATCH) remainingSeconds else total,
            actualSeconds = elapsed,
            mode = mode,
        )
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
            if (wasRunning && (mode == TimerMode.FOCUS || mode == TimerMode.STOPWATCH)) {
                enqueueCompletedFocusSessionSave(
                    totalSeconds = total,
                    actualSeconds = total,
                    mode = mode,
                )
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
        timerStatePrefs().edit().clear().commit()
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

        val showPipIntent = PendingIntent.getActivity(
            this,
            3,
            NotificationDeepLinkHandler.activityIntent(
                context = this,
                deepLink = "safar://ekagra",
                enterTimerPip = true,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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
            .apply {
                if (_isRunning.value) {
                    addAction(
                        android.R.drawable.ic_menu_view,
                        "Show floating timer",
                        showPipIntent,
                    )
                }
            }
            .addAction(android.R.drawable.ic_menu_revert, "Reset", resetIntent)
            .build()
    }

    private fun personalizeNotificationBody(body: String): String {
        val name = cachedUserName
        if (name.isBlank() || startsWithPersonalGreeting(body)) return body
        return "Hi $name, $body"
    }

    private fun startsWithPersonalGreeting(body: String): Boolean {
        return Regex(
            pattern = "^\\s*(hi|hey|hello|good morning|good afternoon|good evening)\\b",
            option = RegexOption.IGNORE_CASE,
        ).containsMatchIn(body)
    }

    private fun showCompletionNotification() {
        val completedMode = _timerMode.value
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
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification())
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
