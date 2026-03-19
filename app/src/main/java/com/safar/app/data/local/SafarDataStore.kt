package com.safar.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safar_prefs")

@Singleton
class SafarDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val IS_ONBOARDING_DONE = booleanPreferencesKey("is_onboarding_done")
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val IS_NIGHT_MODE = booleanPreferencesKey("is_night_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DAILY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_LOGGED_IN] ?: false }

    val authToken: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.AUTH_TOKEN] }

    val refreshToken: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.REFRESH_TOKEN] }

    val userId: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.USER_ID] }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_ONBOARDING_DONE] ?: false }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_DARK_THEME] ?: false }

    val isNightMode: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.IS_NIGHT_MODE] ?: false }

    val language: Flow<String> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.LANGUAGE] ?: "en" }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setLoggedIn(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_LOGGED_IN] = value }
    }

    suspend fun setAuthToken(token: String) {
        context.dataStore.edit { it[Keys.AUTH_TOKEN] = token }
    }

    suspend fun setRefreshToken(token: String) {
        context.dataStore.edit { it[Keys.REFRESH_TOKEN] = token }
    }

    suspend fun setUserId(id: String?) {
        id?.let { context.dataStore.edit { prefs -> prefs[Keys.USER_ID] = it } }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONBOARDING_DONE] = done }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = enabled }
    }

    suspend fun setNightMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_NIGHT_MODE] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setFcmToken(token: String) {
        context.dataStore.edit { it[Keys.FCM_TOKEN] = token }
    }

    suspend fun setDailyReminderTime(time: String) {
        context.dataStore.edit { it[Keys.DAILY_REMINDER_TIME] = time }
    }

    suspend fun setLastSync(time: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = time }
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(Keys.IS_LOGGED_IN)
            it.remove(Keys.AUTH_TOKEN)
            it.remove(Keys.REFRESH_TOKEN)
            it.remove(Keys.USER_ID)
        }
    }
}
