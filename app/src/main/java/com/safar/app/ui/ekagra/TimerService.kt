package com.safar.app.ui.ekagra

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.safar.app.BuildConfig
import com.safar.app.R
import com.safar.app.data.local.SafarDataStore
import com.safar.app.notifications.NotificationDeepLinkHandler
import com.safar.app.notifications.SafarNotificationChannels
import com.safar.app.notifications.SafarNotificationManager
import com.safar.app.ui.ekagra.focusshield.BlockedAppActivity
import com.safar.app.ui.ekagra.focusshield.FocusShieldRepository
import com.safar.app.ui.ekagra.focusshield.FocusShieldPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID        = SafarNotificationChannels.FOCUS_TIMER
        const val NOTIFICATION_ID   = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
        const val FOCUS_SHIELD_BLOCKED_NOTIFICATION_ID = 1003
        const val FOCUS_SHIELD_ACTIVE_NOTIFICATION_ID = 1004
        const val ACTION_PLAY_PAUSE = "com.safar.ekagra.ACTION_PLAY_PAUSE"
        const val ACTION_RESET      = "com.safar.ekagra.ACTION_RESET"
        const val ACTION_FOCUS_SHIELD_BLOCKED = "com.safar.ekagra.ACTION_FOCUS_SHIELD_BLOCKED"
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
    private var shieldMonitorJob: Job? = null
    private var suspendedFocusState: SuspendedFocusState? = null

    private data class SuspendedFocusState(
        val totalSeconds: Int,
        val remainingSeconds: Int,
    )

    // ── Exposed state ─────────────────────────────────────────────────────────
    private val _secondsLeft  = MutableStateFlow(25 * 60)
    private val _totalSeconds = MutableStateFlow(25 * 60)
    private val _isRunning    = MutableStateFlow(false)
    private val _timerMode    = MutableStateFlow(TimerMode.FOCUS)

    val secondsLeft:  StateFlow<Int>       = _secondsLeft
    val totalSeconds: StateFlow<Int>       = _totalSeconds
    val isRunning:    StateFlow<Boolean>   = _isRunning
    val timerMode:    StateFlow<TimerMode> = _timerMode

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
            debugFocusShield("Focus Shield not enabled: Usage access missing")
            disableFocusShieldForSession()
            return
        }
        if (!FocusShieldPermissionHelper.hasAccessibilityService(this)) {
            debugFocusShield("Focus Shield not enabled: Accessibility service missing")
            disableFocusShieldForSession()
            return
        }

        _focusShieldActive.value = true
        val strict = _strictMode.value

        // Write to SharedPreferences (survives process death!)
        FocusShieldRepository.ShieldPrefs.write(
            this, true, pkgs, strict,
        )

        // Also update volatile snapshot for in-process fast access
        FocusShieldRepository.Snapshot.active = true
        FocusShieldRepository.Snapshot.packages = pkgs
        FocusShieldRepository.Snapshot.strict = strict

        debugFocusShield("TimerService.enableFocusShieldForSession()")
        showFocusShieldActiveNotification()
    }

    fun disableFocusShieldForSession() {
        debugFocusShield("TimerService.disableFocusShieldForSession()")
        _focusShieldActive.value = false

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
        if (_timerMode.value != TimerMode.FOCUS) {
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
        if (_timerMode.value != TimerMode.FOCUS || !_isRunning.value) {
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
            !FocusShieldPermissionHelper.hasAccessibilityService(this)
        ) {
            disableFocusShieldForSession()
            return
        }

        val shouldUpdate = !_focusShieldActive.value ||
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

        debugFocusShield("Blocked intent received for $blockedPackage")

        if (!FocusShieldRepository.ShieldPrefs.isActive(this)) return
        val blockedPackages = FocusShieldRepository.ShieldPrefs.getPackages(this)
        if (blockedPackage !in blockedPackages) return

        // On Android 13/14, background activity launches are often delayed/denied unless user-initiated.
        // The on-top UI is handled by the Accessibility overlay; here we only provide a notification.
        showFocusShieldBlockedNotification(blockedPackage)
    }

    private fun showFocusShieldBlockedNotification(blockedPackage: String) {
        val appName = labelForPackage(blockedPackage)
        val focusPendingIntent = PendingIntent.getActivity(
            this,
            4,
            NotificationDeepLinkHandler.activityIntent(this, "safar://ekagra"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, SafarNotificationChannels.FOCUS_SHIELD_BLOCKED)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentTitle("Kavach is active")
            .setContentText("$appName is blocked until your focus timer or Study Session ends.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$appName is blocked until your focus timer or Study Session ends. Tap to return to SAFAR."),
            )
            .setContentIntent(focusPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_revert,
                "Return to Focus",
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

        val notification = NotificationCompat.Builder(this, SafarNotificationChannels.FOCUS_SHIELD_STATUS)
            .setSmallIcon(SafarNotificationManager.SafarNotificationStyle.smallIconRes(this))
            .setColor(SafarNotificationManager.SafarNotificationStyle.brandColor(this))
            .setContentTitle("Kavach is active")
            .setContentText("Selected distracting apps are blocked until your focus timer or Study Session ends.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Selected distracting apps are blocked until your focus timer or Study Session ends. SAFAR uses Accessibility only to detect opened app package names for this feature."),
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

    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─
    private fun themePrefs() = getSharedPreferences("ekagra_theme_prefs", MODE_PRIVATE)

    private fun debugFocusShield(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("FocusShield", message)
    }

    private fun clearTheme() {
        themePrefs().edit().clear().apply()
    }

    // ── Audio player (lives in the service — survives navigation) ─────────────
    private var musicPlayer: MediaPlayer? = null
    private var currentMusicUrl: String   = ""

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
                setVolume(0.7f, 0.7f)
                setOnPreparedListener { start() }
                prepareAsync()
            }
        } catch (e: Exception) { /* ignore */ }
    }

    private fun releaseMusic() {
        runCatching { musicPlayer?.stop() }
        runCatching { musicPlayer?.release() }
        musicPlayer = null
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        SafarNotificationChannels.createAll(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_RESET      -> reset()
            ACTION_FOCUS_SHIELD_BLOCKED -> handleFocusShieldBlockedIntent(intent)
        }
        return START_STICKY
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
        _secondsLeft.value  = seconds
        _totalSeconds.value = seconds
        _isRunning.value    = false
        suspendedFocusState = null
        tickJob?.cancel()
        releaseMusic()
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
        stopForegroundCompat()
    }

    fun restoreSession(mode: TimerMode, totalSeconds: Int, remainingSeconds: Int, running: Boolean) {
        tickJob?.cancel()
        _timerMode.value = mode
        _totalSeconds.value = totalSeconds.coerceAtLeast(60)
        _secondsLeft.value = remainingSeconds.coerceIn(0, _totalSeconds.value)
        _isRunning.value = false
        suspendedFocusState = null
        releaseMusic()
        if (running && _secondsLeft.value > 0) start() else updateNotification()
    }

    fun togglePlayPause() {
        if (_isRunning.value) pause() else start()
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
        start()
        return true
    }

    fun start() {
        if (_secondsLeft.value <= 0) return
        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildNotification())
        activateFocusShieldFromSettingsIfNeeded()
        startFocusShieldMonitor()
        startMusic(currentMusicUrl)
        lastTickElapsedMs = SystemClock.elapsedRealtime()
        tickJob?.cancel()
        tickJob = scope.launch {
            while (_secondsLeft.value > 0 && _isRunning.value) {
                delay(1000L)
                val now = SystemClock.elapsedRealtime()
                val elapsedSeconds = ((now - lastTickElapsedMs) / 1000L).toInt().coerceAtLeast(1)
                lastTickElapsedMs = now
                if (elapsedSeconds > 10 * 60) {
                    pause()
                    break
                }
                _secondsLeft.value = (_secondsLeft.value - elapsedSeconds).coerceAtLeast(0)
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
                    releaseMusic()
                    scope.launch { start() }
                    return@launch
                }
                _isRunning.value = false
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
        releaseMusic()
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
        updateNotification()
    }

    fun reset() {
        _isRunning.value   = false
        _secondsLeft.value = _totalSeconds.value
        suspendedFocusState = null
        tickJob?.cancel()
        releaseMusic()
        clearTheme()
        disableFocusShieldForSession()
        stopFocusShieldMonitor()
        updateNotification()
    }

    fun isActive(): Boolean = _isRunning.value || _secondsLeft.value < _totalSeconds.value

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
            _isRunning.value && _timerMode.value == TimerMode.FOCUS -> "Focus in progress"
            _isRunning.value -> "Break in progress - Kavach paused"
            else -> "Timer paused"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$mode \u00b7 $time")
            .setContentText(notificationText)
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

    private fun showCompletionNotification() {
        scope.launch {
            if (!safarDataStore.notificationsEnabled.first() ||
                !safarDataStore.focusTimerNotificationsEnabled.first()
            ) return@launch

            val mode = _timerMode.value
            val body = when (mode) {
                TimerMode.FOCUS -> "Focus session complete. Great work - take a mindful break."
                TimerMode.BREAK,
                TimerMode.LONG_BREAK -> "Break finished. Ready for your next session?"
            }
            SafarNotificationManager(this@TimerService).show(
                title = if (mode == TimerMode.FOCUS) "Focus session complete" else "Break finished",
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
