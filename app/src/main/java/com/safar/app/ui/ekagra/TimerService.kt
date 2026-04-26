package com.safar.app.ui.ekagra

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.safar.app.MainActivity
import com.safar.app.data.local.SafarDataStore
import com.safar.app.ui.ekagra.focusshield.FocusShieldOverlayService
import com.safar.app.ui.ekagra.focusshield.FocusShieldRepository
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
        const val CHANNEL_ID        = "ekagra_timer_channel"
        const val NOTIFICATION_ID   = 1001
        const val ACTION_PLAY_PAUSE = "com.safar.ekagra.ACTION_PLAY_PAUSE"
        const val ACTION_RESET      = "com.safar.ekagra.ACTION_RESET"
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
        android.util.Log.w("FocusShieldA11y", "TimerService.setFocusShieldConfig(${packages.size} pkgs, strict=$strict)")
    }

    fun enableFocusShieldForSession() {
        _focusShieldActive.value = true

        val pkgs = _blockedPackages.value
        val strict = _strictMode.value

        // Write to SharedPreferences (survives process death!)
        FocusShieldRepository.ShieldPrefs.write(
            this, true, pkgs, strict,
        )

        // Also update volatile snapshot for in-process fast access
        FocusShieldRepository.Snapshot.active = true
        FocusShieldRepository.Snapshot.packages = pkgs
        FocusShieldRepository.Snapshot.strict = strict

        // Start overlay blocking service
        FocusShieldOverlayService.start(this)

        android.util.Log.w("FocusShieldA11y", "TimerService.enableFocusShieldForSession() → DONE")
        android.util.Log.w("FocusShieldA11y", "  packages=$pkgs")
    }

    fun disableFocusShieldForSession() {
        android.util.Log.w("FocusShieldA11y", "TimerService.disableFocusShieldForSession()")
        _focusShieldActive.value = false
        _blockedPackages.value = emptySet()

        // Clear SharedPreferences
        shieldActivationJob?.cancel()
        FocusShieldRepository.ShieldPrefs.clear(this)

        // Clear volatile snapshot
        FocusShieldRepository.Snapshot.active = false
        FocusShieldRepository.Snapshot.packages = emptySet()

        // Stop overlay service
        FocusShieldOverlayService.stop(this)
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
                android.util.Log.w(
                    "FocusShieldA11y",
                    "TimerService.start() -> activating from persisted settings: $packages",
                )
                setFocusShieldConfig(packages = packages, strict = strict)
                enableFocusShieldForSession()
            } else {
                android.util.Log.w(
                    "FocusShieldA11y",
                    "TimerService.start() -> shield not activated (enabled=$enabled, pkgs=${packages.size})",
                )
                disableFocusShieldForSession()
            }
        }
    }

    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─
    private fun themePrefs() = getSharedPreferences("ekagra_theme_prefs", MODE_PRIVATE)

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
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_RESET      -> reset()
        }
        return START_STICKY
    }

    override fun onDestroy() {
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
        tickJob?.cancel()
        releaseMusic()
        stopForegroundCompat()
    }

    fun restoreSession(mode: TimerMode, totalSeconds: Int, remainingSeconds: Int, running: Boolean) {
        tickJob?.cancel()
        _timerMode.value = mode
        _totalSeconds.value = totalSeconds.coerceAtLeast(60)
        _secondsLeft.value = remainingSeconds.coerceIn(0, _totalSeconds.value)
        _isRunning.value = false
        releaseMusic()
        if (running && _secondsLeft.value > 0) start() else updateNotification()
    }

    fun togglePlayPause() {
        if (_isRunning.value) pause() else start()
    }

    fun start() {
        if (_secondsLeft.value <= 0) return
        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildNotification())
        activateFocusShieldFromSettingsIfNeeded()
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
                _isRunning.value = false
                releaseMusic()
                clearTheme()
                disableFocusShieldForSession()
                updateNotification()
            }
        }
    }

    fun pause() {
        _isRunning.value = false
        tickJob?.cancel()
        releaseMusic()
        updateNotification()
    }

    fun reset() {
        _isRunning.value   = false
        _secondsLeft.value = _totalSeconds.value
        tickJob?.cancel()
        releaseMusic()
        clearTheme()
        disableFocusShieldForSession()
        updateNotification()
    }

    fun isActive(): Boolean = _isRunning.value || _secondsLeft.value < _totalSeconds.value

    // ── Notification ──────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active focus timer"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$mode · $time")
            .setContentText(if (_isRunning.value) "Focus in progress" else "Timer paused")
            .setSmallIcon(android.R.drawable.ic_media_play)
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
