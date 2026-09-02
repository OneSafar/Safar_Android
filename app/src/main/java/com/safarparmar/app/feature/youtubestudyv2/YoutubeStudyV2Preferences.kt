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
    private val _enabled = MutableStateFlow(preferences.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled
    private val _setupStep = MutableStateFlow(preferences.getInt(KEY_SETUP_STEP, 1).coerceIn(1, 2))
    val setupStep: StateFlow<Int> = _setupStep
    private val _setupCompleted = MutableStateFlow(preferences.getBoolean(KEY_SETUP_COMPLETED, false))
    val setupCompleted: StateFlow<Boolean> = _setupCompleted
    fun setEnabled(value: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
        if (!value) preferences.edit().remove(KEY_ACCESSIBILITY_HEARTBEAT).apply()
    }

    fun recordAccessibilityHeartbeat(nowMs: Long = System.currentTimeMillis()) {
        preferences.edit().putLong(KEY_ACCESSIBILITY_HEARTBEAT, nowMs).apply()
    }

    fun lastAccessibilityHeartbeatMs(): Long = preferences.getLong(KEY_ACCESSIBILITY_HEARTBEAT, 0L)

    fun acceptDisclosure() {
        preferences.edit().putBoolean(KEY_DISCLOSURE_ACCEPTED, true).apply()
    }

    fun isDisclosureAccepted(): Boolean = preferences.getBoolean(KEY_DISCLOSURE_ACCEPTED, false)

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
        private const val KEY_DISCLOSURE_ACCEPTED = "accessibility_disclosure_accepted"
        private const val KEY_SETUP_STEP = "setup_step"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        fun isEnabled(context: Context): Boolean = context
            .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }
}
