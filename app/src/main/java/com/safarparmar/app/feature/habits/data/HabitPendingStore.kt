package com.safarparmar.app.feature.habits.data

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Keep
data class HabitPendingBatch(val owner: String, val key: String, val mutations: List<JsonObject>)

/** Only unconfirmed operations are journaled; the server remains the source of truth. Call on IO. */
@Singleton
class HabitPendingStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs by lazy { context.getSharedPreferences("habit_pending_v1", Context.MODE_PRIVATE) }
    private val gson = Gson()
    private fun slot(owner: String, key: String) = MessageDigest.getInstance("SHA-256")
        .digest("$owner:$key".toByteArray()).joinToString("") { "%02x".format(it) }

    @Synchronized fun prepare(owner: String, key: String, mutations: List<JsonObject>): List<JsonObject> {
        val slot = slot(owner, key)
        val prior = prefs.getString(slot, null)?.let { gson.fromJson(it, HabitPendingBatch::class.java) }
        if (prior != null) {
            check(prior.owner == owner && prior.key == key) { "Invalid pending habit operation" }
            return prior.mutations
        }
        check(prefs.edit().putString(slot, gson.toJson(HabitPendingBatch(owner, key, mutations))).commit()) {
            "Could not protect this save for retry. Free some device storage and try again."
        }
        return mutations
    }

    @Synchronized fun pending(owner: String): List<HabitPendingBatch> = prefs.all.values.mapNotNull { raw ->
        (raw as? String)?.let { gson.fromJson(it, HabitPendingBatch::class.java) }?.takeIf { it.owner == owner }
    }

    @Synchronized fun acknowledge(owner: String, key: String) {
        check(prefs.edit().remove(slot(owner, key)).commit()) { "Could not clear the confirmed habit operation" }
    }
}
