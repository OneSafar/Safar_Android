package com.safarparmar.app.feature.habits.reminders

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.safarparmar.app.feature.habits.data.HabitRemoteRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Keep
data class HabitReminderSnapshot(val owner: String, val habits: List<HabitRemoteRecord>)

/** Durable projection of confirmed server data, never a second writable habit database. */
@Singleton
class HabitReminderStore @Inject constructor(@ApplicationContext context: Context) {
    // Application injection must not open preference files on a service's startup thread.
    private val prefs by lazy { context.getSharedPreferences("habit_reminders_v1", Context.MODE_PRIVATE) }
    private val gson = Gson()

    fun read(): HabitReminderSnapshot? = runCatching {
        prefs.getString("snapshot", null)?.let {
            gson.fromJson(it, HabitReminderSnapshot::class.java).also { snapshot ->
                require(snapshot.owner.isNotBlank())
                require(snapshot.habits.all { habit -> habit.id.isNotBlank() && habit.name.isNotBlank() })
            }
        }
    }.getOrNull()

    fun replace(snapshot: HabitReminderSnapshot) {
        check(prefs.edit().putString("snapshot", gson.toJson(snapshot)).commit()) { "Could not persist habit reminders" }
    }

    fun delivered(owner: String, id: String): String? = prefs.getString(key(owner, id), null)
    fun markDelivered(owner: String, id: String, date: String) {
        check(prefs.edit().putString(key(owner, id), date).commit()) { "Could not persist reminder delivery" }
    }

    fun clear() { check(prefs.edit().clear().commit()) { "Could not clear habit reminders" } }
    private fun key(owner: String, id: String) = "delivered:$owner:$id"

    fun token(habit: HabitRemoteRecord): String = MessageDigest.getInstance("SHA-256")
        .digest(gson.toJson(habit).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
