package com.safarparmar.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safar_prefs")

@Singleton
class SafarDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun tourDoneKey(section: String): Preferences.Key<Boolean> =
        if (section == "nishtha") Keys.TOUR_DONE
        else booleanPreferencesKey("tour_done_${section.lowercase()}")

    private fun studyPlannerOnboardingCompletedStepsKey(planId: String): Preferences.Key<Set<String>> =
        stringSetPreferencesKey("study_planner_onboarding_v2_${planId}_completed_steps")

    private fun plannerPlanningModeKey(planId: String): Preferences.Key<String> =
        stringPreferencesKey("study_planner_planning_mode_$planId")

    private fun plannerPreferredStrategyKey(planId: String?): Preferences.Key<String> =
        stringPreferencesKey("study_planner_preferred_strategy_${planId ?: "default"}")

    private val securePrefs: SharedPreferences by lazy {
        try {
            createSecurePrefs()
        } catch (e: Exception) {
            try {
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (_: Exception) {}
            try {
                if (android.os.Build.VERSION.SDK_INT >= 24) {
                    context.deleteSharedPreferences("safar_secure_auth")
                } else {
                    context.getSharedPreferences("safar_secure_auth", Context.MODE_PRIVATE).edit().clear().commit()
                }
            } catch (_: Exception) {}
            try {
                createSecurePrefs()
            } catch (e2: Exception) {
                context.getSharedPreferences("safar_secure_auth_fallback", Context.MODE_PRIVATE)
            }
        }
    }

    private fun createSecurePrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "safar_secure_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private object SecureKeys {
        const val AUTH_TOKEN = "auth_token"
        const val REFRESH_TOKEN = "refresh_token"
    }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            migratePlainAuthTokensToEncryptedStorage()
        }
    }

    private object Keys {
        val IS_LOGGED_IN          = booleanPreferencesKey("is_logged_in")
        val AUTH_TOKEN            = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN         = stringPreferencesKey("refresh_token")
        val USER_ID               = stringPreferencesKey("user_id")
        val USER_NAME             = stringPreferencesKey("user_name")
        val USER_EMAIL            = stringPreferencesKey("user_email")
        val USER_AVATAR           = stringPreferencesKey("user_avatar")
        val IS_ADMIN              = booleanPreferencesKey("is_admin")
        val IS_ONBOARDING_DONE    = booleanPreferencesKey("is_onboarding_done")
        val IS_DARK_THEME         = booleanPreferencesKey("is_dark_theme")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DAILY_REMINDER_TIME   = stringPreferencesKey("daily_reminder_time")
        val FCM_TOKEN             = stringPreferencesKey("fcm_token")
        val FOCUS_TIMER_NOTIFICATIONS_ENABLED = booleanPreferencesKey("focus_timer_notifications_enabled")
        val SHOW_EKAGRA_DURATION_PROMPT = booleanPreferencesKey("show_ekagra_duration_prompt")
        val DAILY_STUDY_REMINDER_ENABLED      = booleanPreferencesKey("daily_study_reminder_enabled")
        val STREAK_REMINDER_ENABLED           = booleanPreferencesKey("streak_reminder_enabled")
        val COURSE_UPDATES_ENABLED            = booleanPreferencesKey("course_updates_enabled")
        val ACHIEVEMENTS_ENABLED              = booleanPreferencesKey("achievements_enabled")
        val COMMUNITY_REPLIES_ENABLED         = booleanPreferencesKey("community_replies_enabled")
        val ANNOUNCEMENTS_ENABLED             = booleanPreferencesKey("announcements_enabled")
        val WEEKLY_SUMMARY_ENABLED            = booleanPreferencesKey("weekly_summary_enabled")
        val QUIET_HOURS_START                 = stringPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END                   = stringPreferencesKey("quiet_hours_end")
        val DEVICE_TOKEN_LAST_SYNC_AT         = longPreferencesKey("device_token_last_sync_at")
        val LAST_REGISTERED_FCM_TOKEN         = stringPreferencesKey("last_registered_fcm_token")
        val LAST_SYNC             = longPreferencesKey("last_sync")
        val TOUR_DONE             = booleanPreferencesKey("tour_done")
        val WELCOME_SEEN          = booleanPreferencesKey("welcome_seen")
        val NOTIFIED_ACHIEVEMENTS = stringSetPreferencesKey("notified_achievements")
        val PLANNER_ALERT_DEDUPE_KEYS = stringSetPreferencesKey("planner_alert_dedupe_keys")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val PREMIUM_PLAN_TYPE = stringPreferencesKey("premium_plan_type")
        val PREMIUM_EXPIRES_AT = stringPreferencesKey("premium_expires_at")
        val PREMIUM_FEATURE_MEHFIL_DM = booleanPreferencesKey("premium_feature_mehfil_dm")
        val PREMIUM_FEATURE_STUDY_PLANNER_INSIGHTS = booleanPreferencesKey("premium_feature_study_planner_insights")
        val PREMIUM_FEATURE_NISHTHA_ANALYTICS = booleanPreferencesKey("premium_feature_nishtha_analytics")
        val PREMIUM_FEATURE_FOCUS_ANALYTICS = booleanPreferencesKey("premium_feature_focus_analytics")

        // Focus Shield
        val FOCUS_SHIELD_ENABLED          = booleanPreferencesKey("focus_shield_enabled")
        val FOCUS_SHIELD_STRICT_MODE      = booleanPreferencesKey("focus_shield_strict_mode")
        val FOCUS_SHIELD_EMERGENCY_UNLOCK = booleanPreferencesKey("focus_shield_allow_emergency_unlock")
        val FOCUS_SHIELD_BLOCKED_PACKAGES = stringPreferencesKey("focus_shield_blocked_packages")
        val FOCUS_SHIELD_LAST_BLOCK_COUNT = intPreferencesKey("focus_shield_last_block_count")
        val FOCUS_SHIELD_EMERGENCY_UNLOCKS_PER_SESSION = intPreferencesKey("focus_shield_emergency_unlocks_per_session")
        val FOCUS_SHIELD_EMERGENCY_UNLOCK_SECONDS      = intPreferencesKey("focus_shield_emergency_unlock_seconds")

        val LAUNCH_USAGE_QUESTIONNAIRE_COMPLETED = booleanPreferencesKey("launch_usage_questionnaire_completed")
        val APP_USAGE_MODE = stringPreferencesKey("app_usage_mode")

        val NOTIFICATION_BELL_LAST_SEEN_AT = stringPreferencesKey("notification_bell_last_seen_at")
    }

    // ── Flows ─────────────────────────────────────────────────────────────────

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_LOGGED_IN] ?: false }

    val authToken: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> securePrefs.getString(SecureKeys.AUTH_TOKEN, null) ?: prefs[Keys.AUTH_TOKEN] }

    val refreshToken: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> securePrefs.getString(SecureKeys.REFRESH_TOKEN, null) ?: prefs[Keys.REFRESH_TOKEN] }

    val userId: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_ID] }

    val userName: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_NAME] }

    val userEmail: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_EMAIL] }

    val userAvatar: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_AVATAR] }

    val isAdmin: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_ADMIN] ?: false }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_ONBOARDING_DONE] ?: false }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_DARK_THEME] ?: false }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val fcmToken: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.FCM_TOKEN] }

    val dailyReminderTime: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.DAILY_REMINDER_TIME] ?: "19:00" }

    val focusTimerNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.FOCUS_TIMER_NOTIFICATIONS_ENABLED] ?: true }

    val showEkagraDurationPrompt: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.SHOW_EKAGRA_DURATION_PROMPT] ?: true }

    val dailyStudyReminderEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.DAILY_STUDY_REMINDER_ENABLED] ?: false }

    val streakReminderEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.STREAK_REMINDER_ENABLED] ?: true }

    val courseUpdatesEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.COURSE_UPDATES_ENABLED] ?: true }

    val achievementsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.ACHIEVEMENTS_ENABLED] ?: false }

    val communityRepliesEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.COMMUNITY_REPLIES_ENABLED] ?: true }

    val announcementsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.ANNOUNCEMENTS_ENABLED] ?: true }

    val weeklySummaryEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.WEEKLY_SUMMARY_ENABLED] ?: false }

    val quietHoursStart: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.QUIET_HOURS_START] ?: "22:00" }

    val quietHoursEnd: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.QUIET_HOURS_END] ?: "07:00" }

    val deviceTokenLastSyncAt: Flow<Long> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.DEVICE_TOKEN_LAST_SYNC_AT] ?: 0L }

    val lastRegisteredFcmToken: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.LAST_REGISTERED_FCM_TOKEN] }

    val isTourDone: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.TOUR_DONE] ?: false }

    /** Whether Titli's tutorial prompt has been answered for a specific app section. */
    fun isTourDone(section: String): Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[tourDoneKey(section)] ?: false }

    val isWelcomeSeen: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.WELCOME_SEEN] ?: false }

    val notifiedAchievements: Flow<Set<String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.NOTIFIED_ACHIEVEMENTS] ?: emptySet() }

    // ── Focus Shield Flows ────────────────────────────────────────────────────

    val focusShieldEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.FOCUS_SHIELD_ENABLED] ?: false }

    val focusShieldStrictMode: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.FOCUS_SHIELD_STRICT_MODE] ?: false }

    val focusShieldEmergencyUnlock: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.FOCUS_SHIELD_EMERGENCY_UNLOCK] ?: true }

    val focusShieldBlockedPackages: Flow<Set<String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val raw = prefs[Keys.FOCUS_SHIELD_BLOCKED_PACKAGES] ?: ""
            if (raw.isBlank()) emptySet() else raw.split(",").toSet()
        }

    val focusShieldEmergencyUnlocksPerSession: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.FOCUS_SHIELD_EMERGENCY_UNLOCKS_PER_SESSION] ?: 3 }

    val focusShieldEmergencyUnlockSeconds: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.FOCUS_SHIELD_EMERGENCY_UNLOCK_SECONDS] ?: 60 }

    val launchUsageQuestionnaireCompleted: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.LAUNCH_USAGE_QUESTIONNAIRE_COMPLETED] ?: false }

    val appUsageMode: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.APP_USAGE_MODE] }

    val notificationBellLastSeenAt: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.NOTIFICATION_BELL_LAST_SEEN_AT] }

    val plannerAlertDedupeKeys: Flow<Set<String>> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PLANNER_ALERT_DEDUPE_KEYS] ?: emptySet() }

    val isPremium: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_PREMIUM] ?: false }

    val premiumPlanType: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PREMIUM_PLAN_TYPE] }

    val premiumExpiresAt: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PREMIUM_EXPIRES_AT] }

    val premiumFeatureMehfilDm: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PREMIUM_FEATURE_MEHFIL_DM] ?: false }

    val premiumFeatureStudyPlannerInsights: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PREMIUM_FEATURE_STUDY_PLANNER_INSIGHTS] ?: false }

    val premiumFeatureNishthaAnalytics: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PREMIUM_FEATURE_NISHTHA_ANALYTICS] ?: false }

    val premiumFeatureFocusAnalytics: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PREMIUM_FEATURE_FOCUS_ANALYTICS] ?: false }

    // ── Setters ───────────────────────────────────────────────────────────────

    suspend fun setLoggedIn(value: Boolean) = context.dataStore.edit { it[Keys.IS_LOGGED_IN] = value }
    suspend fun setAuthToken(token: String) {
        securePrefs.edit().putString(SecureKeys.AUTH_TOKEN, token).apply()
        context.dataStore.edit { it.remove(Keys.AUTH_TOKEN) }
    }

    suspend fun setRefreshToken(token: String) {
        securePrefs.edit().putString(SecureKeys.REFRESH_TOKEN, token).apply()
        context.dataStore.edit { it.remove(Keys.REFRESH_TOKEN) }
    }
    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[Keys.IS_ONBOARDING_DONE] = done }
    suspend fun setDarkTheme(enabled: Boolean) = context.dataStore.edit { it[Keys.IS_DARK_THEME] = enabled }
    suspend fun setNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setFcmToken(token: String) = context.dataStore.edit { it[Keys.FCM_TOKEN] = token }
    suspend fun setDailyReminderTime(time: String) = context.dataStore.edit { it[Keys.DAILY_REMINDER_TIME] = time }
    suspend fun setFocusTimerNotificationsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.FOCUS_TIMER_NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setShowEkagraDurationPrompt(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_EKAGRA_DURATION_PROMPT] = show }
    suspend fun setDailyStudyReminderEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.DAILY_STUDY_REMINDER_ENABLED] = enabled }
    suspend fun setStreakReminderEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.STREAK_REMINDER_ENABLED] = enabled }
    suspend fun setCourseUpdatesEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.COURSE_UPDATES_ENABLED] = enabled }
    suspend fun setAchievementsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.ACHIEVEMENTS_ENABLED] = enabled }
    suspend fun setCommunityRepliesEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.COMMUNITY_REPLIES_ENABLED] = enabled }
    suspend fun setAnnouncementsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.ANNOUNCEMENTS_ENABLED] = enabled }
    suspend fun setWeeklySummaryEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.WEEKLY_SUMMARY_ENABLED] = enabled }
    suspend fun setQuietHours(start: String, end: String) = context.dataStore.edit {
        it[Keys.QUIET_HOURS_START] = start
        it[Keys.QUIET_HOURS_END] = end
    }
    suspend fun setDeviceTokenLastSyncAt(time: Long) = context.dataStore.edit {
        it[Keys.DEVICE_TOKEN_LAST_SYNC_AT] = time
    }
    suspend fun setLastRegisteredFcmToken(token: String) = context.dataStore.edit {
        it[Keys.LAST_REGISTERED_FCM_TOKEN] = token
    }
    suspend fun setLastSync(time: Long) = context.dataStore.edit { it[Keys.LAST_SYNC] = time }
    suspend fun setTourDone(done: Boolean) = context.dataStore.edit { it[Keys.TOUR_DONE] = done }
    suspend fun setNotificationBellLastSeenAt(iso: String) = context.dataStore.edit { it[Keys.NOTIFICATION_BELL_LAST_SEEN_AT] = iso }
    suspend fun setTourDone(section: String, done: Boolean) = context.dataStore.edit {
        it[tourDoneKey(section)] = done
    }

    fun studyPlannerOnboardingCompletedSteps(planId: String): Flow<Set<String>> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[studyPlannerOnboardingCompletedStepsKey(planId)] ?: emptySet() }

    /**
     * Per-plan planning mode: "flex" (allow over-goal days when building the
     * schedule) or "strict" (keep exactly to the daily goal). Stored on-device
     * so it persists without a server round-trip. Defaults to "flex".
     */
    fun plannerPlanningMode(planId: String): Flow<String> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[plannerPlanningModeKey(planId)] ?: "flex" }

    suspend fun setPlannerPlanningMode(planId: String, mode: String) =
        context.dataStore.edit { it[plannerPlanningModeKey(planId)] = mode }

    /**
     * "How do you like to study" preference: "interleaved" (mix subjects daily) or
     * "sequential" (finish topics in order). Stored on-device, same pattern as
     * [plannerPlanningMode] — purely a UI default for the Build Planner strategy
     * picker, not a new server-side field. Defaults to "interleaved".
     *
     * [planId] is null for the choice made during plan *creation* (no plan exists yet,
     * so it's recorded under a shared "default" bucket) and set once a plan exists, so
     * the Build Planner sheet for that specific plan can remember its own choice
     * afterwards.
     */
    fun plannerPreferredStrategy(planId: String? = null): Flow<String> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[plannerPreferredStrategyKey(planId)] ?: "interleaved" }

    suspend fun setPlannerPreferredStrategy(planId: String?, strategy: String) =
        context.dataStore.edit { it[plannerPreferredStrategyKey(planId)] = strategy }

    suspend fun setStudyPlannerOnboardingStepDone(planId: String, step: String, done: Boolean) =
        context.dataStore.edit { prefs ->
            val key = studyPlannerOnboardingCompletedStepsKey(planId)
            val current = prefs[key].orEmpty()
            prefs[key] = if (done) current + step else current - step
        }

    suspend fun setWelcomeSeen(seen: Boolean) = context.dataStore.edit { it[Keys.WELCOME_SEEN] = seen }

    suspend fun setPremiumStatus(
        isPremium: Boolean,
        planType: String?,
        expiresAt: String?,
        mehfilDm: Boolean = false,
        studyPlannerInsights: Boolean = false,
        nishthaAnalytics: Boolean = false,
        focusAnalytics: Boolean = false,
    ) = context.dataStore.edit { prefs ->
        prefs[Keys.IS_PREMIUM] = isPremium
        if (planType.isNullOrBlank()) prefs.remove(Keys.PREMIUM_PLAN_TYPE) else prefs[Keys.PREMIUM_PLAN_TYPE] = planType
        if (expiresAt.isNullOrBlank()) prefs.remove(Keys.PREMIUM_EXPIRES_AT) else prefs[Keys.PREMIUM_EXPIRES_AT] = expiresAt
        prefs[Keys.PREMIUM_FEATURE_MEHFIL_DM] = mehfilDm
        prefs[Keys.PREMIUM_FEATURE_STUDY_PLANNER_INSIGHTS] = studyPlannerInsights
        prefs[Keys.PREMIUM_FEATURE_NISHTHA_ANALYTICS] = nishthaAnalytics
        prefs[Keys.PREMIUM_FEATURE_FOCUS_ANALYTICS] = focusAnalytics
    }

    suspend fun addNotifiedAchievement(achievementId: String) = context.dataStore.edit { prefs ->
        val current = prefs[Keys.NOTIFIED_ACHIEVEMENTS] ?: emptySet()
        prefs[Keys.NOTIFIED_ACHIEVEMENTS] = current + achievementId
    }

    suspend fun hasPlannerAlertDedupeKey(key: String): Boolean {
        val keys = plannerAlertDedupeKeys.first()
        return key in keys
    }

    suspend fun addPlannerAlertDedupeKey(key: String, maxKeys: Int = 500) = context.dataStore.edit { prefs ->
        val current = (prefs[Keys.PLANNER_ALERT_DEDUPE_KEYS] ?: emptySet()).toMutableSet()
        current += key
        if (current.size > maxKeys) {
            val trimmed = current.toList().takeLast(maxKeys).toSet()
            prefs[Keys.PLANNER_ALERT_DEDUPE_KEYS] = trimmed
        } else {
            prefs[Keys.PLANNER_ALERT_DEDUPE_KEYS] = current
        }
    }

    // ── Focus Shield Setters ─────────────────────────────────────────────────

    suspend fun setFocusShieldEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.FOCUS_SHIELD_ENABLED] = enabled }
    suspend fun setFocusShieldStrictMode(enabled: Boolean) = context.dataStore.edit { it[Keys.FOCUS_SHIELD_STRICT_MODE] = enabled }
    suspend fun setFocusShieldEmergencyUnlock(allow: Boolean) = context.dataStore.edit { it[Keys.FOCUS_SHIELD_EMERGENCY_UNLOCK] = allow }
    suspend fun setFocusShieldBlockedPackages(packages: Set<String>) = context.dataStore.edit {
        it[Keys.FOCUS_SHIELD_BLOCKED_PACKAGES] = packages.joinToString(",")
    }
    suspend fun setFocusShieldLastBlockCount(count: Int) = context.dataStore.edit { it[Keys.FOCUS_SHIELD_LAST_BLOCK_COUNT] = count }
    suspend fun setFocusShieldEmergencyUnlocksPerSession(count: Int) = context.dataStore.edit { it[Keys.FOCUS_SHIELD_EMERGENCY_UNLOCKS_PER_SESSION] = count.coerceAtLeast(0) }
    suspend fun setFocusShieldEmergencyUnlockSeconds(seconds: Int) = context.dataStore.edit { it[Keys.FOCUS_SHIELD_EMERGENCY_UNLOCK_SECONDS] = seconds.coerceAtLeast(10) }

    suspend fun setLaunchUsageQuestionnaireCompleted(completed: Boolean) = context.dataStore.edit {
        it[Keys.LAUNCH_USAGE_QUESTIONNAIRE_COMPLETED] = completed
    }

    suspend fun setAppUsageMode(mode: String?) = context.dataStore.edit { prefs ->
        if (mode != null) prefs[Keys.APP_USAGE_MODE] = mode else prefs.remove(Keys.APP_USAGE_MODE)
    }

    suspend fun setUserId(id: String?) {
        id?.let { context.dataStore.edit { prefs -> prefs[Keys.USER_ID] = it } }
    }

    suspend fun setUserName(name: String) = context.dataStore.edit { it[Keys.USER_NAME] = name }

    suspend fun setUserEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email != null) prefs[Keys.USER_EMAIL] = email else prefs.remove(Keys.USER_EMAIL)
        }
    }

    suspend fun setUserAvatar(avatar: String?) {
        context.dataStore.edit { prefs ->
            if (avatar != null) prefs[Keys.USER_AVATAR] = avatar else prefs.remove(Keys.USER_AVATAR)
        }
    }

    suspend fun setIsAdmin(isAdmin: Boolean) = context.dataStore.edit { it[Keys.IS_ADMIN] = isAdmin }

    suspend fun clearSession() {
        securePrefs.edit()
            .remove(SecureKeys.AUTH_TOKEN)
            .remove(SecureKeys.REFRESH_TOKEN)
            .apply()
        context.dataStore.edit {
            it.remove(Keys.IS_LOGGED_IN)
            it.remove(Keys.AUTH_TOKEN)
            it.remove(Keys.REFRESH_TOKEN)
            it.remove(Keys.USER_ID)
            it.remove(Keys.USER_EMAIL)
            it.remove(Keys.IS_ADMIN)
            it.remove(Keys.IS_PREMIUM)
            it.remove(Keys.PREMIUM_PLAN_TYPE)
            it.remove(Keys.PREMIUM_EXPIRES_AT)
            it.remove(Keys.PREMIUM_FEATURE_MEHFIL_DM)
            it.remove(Keys.PREMIUM_FEATURE_STUDY_PLANNER_INSIGHTS)
            it.remove(Keys.PREMIUM_FEATURE_NISHTHA_ANALYTICS)
            it.remove(Keys.PREMIUM_FEATURE_FOCUS_ANALYTICS)
        }
    }

    private suspend fun migratePlainAuthTokensToEncryptedStorage() {
        val prefs = context.dataStore.data.catch { emit(emptyPreferences()) }.first()
        val plainAuthToken = prefs[Keys.AUTH_TOKEN]
        val plainRefreshToken = prefs[Keys.REFRESH_TOKEN]
        if (!plainAuthToken.isNullOrBlank() && securePrefs.getString(SecureKeys.AUTH_TOKEN, null).isNullOrBlank()) {
            securePrefs.edit().putString(SecureKeys.AUTH_TOKEN, plainAuthToken).apply()
        }
        if (!plainRefreshToken.isNullOrBlank() && securePrefs.getString(SecureKeys.REFRESH_TOKEN, null).isNullOrBlank()) {
            securePrefs.edit().putString(SecureKeys.REFRESH_TOKEN, plainRefreshToken).apply()
        }
        if (!plainAuthToken.isNullOrBlank() || !plainRefreshToken.isNullOrBlank()) {
            context.dataStore.edit {
                it.remove(Keys.AUTH_TOKEN)
                it.remove(Keys.REFRESH_TOKEN)
            }
        }
    }
}
