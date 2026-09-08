package com.safarparmar.app.feature.youtubestudyv2

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeStudyV2Preferences @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val _enabled = MutableStateFlow(preferences.getBoolean(KEY_ENABLED, false) && isDisclosureAccepted())
    val enabled: StateFlow<Boolean> = _enabled
    private val _setupStep = MutableStateFlow(preferences.getInt(KEY_SETUP_STEP, 1).coerceIn(1, 2))
    val setupStep: StateFlow<Int> = _setupStep
    private val _setupCompleted = MutableStateFlow(preferences.getBoolean(KEY_SETUP_COMPLETED, false))
    val setupCompleted: StateFlow<Boolean> = _setupCompleted
    private val _bannerDismissed = MutableStateFlow(preferences.getBoolean(KEY_BANNER_DISMISSED, false))
    val bannerDismissed: StateFlow<Boolean> = _bannerDismissed

    fun dismissBanner() {
        preferences.edit().putBoolean(KEY_BANNER_DISMISSED, true).apply()
        _bannerDismissed.value = true
    }

    fun setEnabled(requested: Boolean) {
        val value = requested && isDisclosureAccepted()
        preferences.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
        if (!value) {
            preferences.edit().remove(KEY_ACCESSIBILITY_HEARTBEAT).apply()
            lastPersistedHeartbeatMs = 0L
        }
    }

    private var lastPersistedHeartbeatMs = 0L

    @Synchronized
    fun recordAccessibilityHeartbeat(nowMs: Long = System.currentTimeMillis()) {
        // Content events can arrive many times per second. Five-second writes
        // remain well within the health monitor's 90-second freshness window.
        if (lastPersistedHeartbeatMs != 0L && nowMs >= lastPersistedHeartbeatMs &&
            nowMs - lastPersistedHeartbeatMs < 5_000L
        ) return
        preferences.edit().putLong(KEY_ACCESSIBILITY_HEARTBEAT, nowMs).apply()
        lastPersistedHeartbeatMs = nowMs
    }

    fun lastAccessibilityHeartbeatMs(): Long = preferences.getLong(KEY_ACCESSIBILITY_HEARTBEAT, 0L)

    fun acceptDisclosure() {
        preferences.edit()
            .putInt(KEY_DISCLOSURE_VERSION, DISCLOSURE_VERSION)
            .putLong("accessibility_disclosure_accepted_at_ms", System.currentTimeMillis())
            .apply()
    }

    fun isDisclosureAccepted(): Boolean = preferences.getInt(KEY_DISCLOSURE_VERSION, 0) == DISCLOSURE_VERSION

    fun setSetupStep(step: Int) {
        val safeStep = step.coerceIn(1, 2)
        preferences.edit().putInt(KEY_SETUP_STEP, safeStep).apply()
        _setupStep.value = safeStep
    }

    fun completeSetup() {
        preferences.edit()
            .putInt(KEY_SETUP_STEP, 2)
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .apply()
        _setupStep.value = 2
        _setupCompleted.value = true
    }

    companion object {
        private const val FILE_NAME = "youtube_study_v2"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_ACCESSIBILITY_HEARTBEAT = "accessibility_heartbeat_ms"
        private const val KEY_DISCLOSURE_VERSION = "accessibility_disclosure_version"
        private const val DISCLOSURE_VERSION = 2
        private const val KEY_SETUP_STEP = "setup_step"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_BANNER_DISMISSED = "banner_dismissed"
        fun isEnabled(context: Context): Boolean {
            val storage = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            return storage.getBoolean(KEY_ENABLED, false) &&
                storage.getInt(KEY_DISCLOSURE_VERSION, 0) == DISCLOSURE_VERSION
        }
    }
}
