package com.safarparmar.app.feature.habits.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safarparmar.app.data.local.SafarDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.habitIconData by preferencesDataStore("habit_icons")

/** Local icon metadata keyed by account and stable server UUID, never by transient numeric IDs. */
@Singleton
class HabitIconStore internal constructor(private val dataStore: DataStore<Preferences>, private val owner: Flow<String?>) {
    @Inject constructor(@ApplicationContext context: Context, accounts: SafarDataStore) : this(
        context.habitIconData,
        combine(accounts.userId, accounts.isLoggedIn) { id, loggedIn -> id?.takeIf { loggedIn && it.isNotBlank() } }.distinctUntilChanged()
    )
    private val gson = Gson()
    private fun key(user: String) = stringPreferencesKey("icons_$user")
    private fun decode(json: String?): Map<String, String> = if (json == null) emptyMap() else
        gson.fromJson(json, object : TypeToken<Map<String, String>>() {}.type)
    val icons: Flow<Map<String, String>> = combine(owner, dataStore.data) { user, prefs ->
        if (user == null) emptyMap() else decode(prefs[key(user)])
    }
    suspend fun save(user: String, remoteId: String, emoji: String) {
        require(emoji.isNotBlank() && emoji.length <= 64) { "Choose an emoji for your habit." }
        check(owner.first() == user) { "Account changed. Try again." }
        dataStore.edit { prefs ->
            check(owner.first() == user) { "Account changed. Try again." }
            prefs[key(user)] = gson.toJson(decode(prefs[key(user)]) + (remoteId to emoji))
        }
    }
}
