package com.safarparmar.app.feature.toppersbatch

import android.content.Context
import com.google.gson.Gson
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Imports older device-only progress once; all catalogue and subsequent edits use the API. */
@Singleton
class GkLectureStore @Inject constructor(
    @ApplicationContext context: Context, private val accounts: SafarDataStore, private val api: ToppersBatchApi,
) {
    private val prefs = context.getSharedPreferences("gk_daily_lectures", Context.MODE_PRIVATE)
    private val gson = com.google.gson.GsonBuilder().serializeNulls().create()
    private val writes = Mutex()

    suspend fun attach(overview: BatchOverview): BatchOverview = withContext(Dispatchers.IO) {
        writes.withLock {
            val user = accounts.userId.first() ?: return@withLock overview
            val accountKey = "$user:${overview.course.id}"
            val saved = prefs.getString(accountKey, null)?.let {
                gson.fromJson(it, Array<BatchLecture>::class.java).toList()
            }.orEmpty().associateBy { it.id }
            val imports = (overview.lectures + overview.removedLectures).mapNotNull { row ->
                val legacyId = row.legacyLocalId ?: return@mapNotNull null
                if (prefs.getBoolean("imported:$accountKey:${row.id}", false)) return@mapNotNull null
                val old = saved[legacyId] ?: return@mapNotNull null
                if (old.completedAt == null && old.backlogAddedAt == null && old.sessions.isEmpty() &&
                    old.color == null && old.section != "local-deleted" && old.displayTopic == old.originalTopic) return@mapNotNull null
                mapOf("id" to row.id, "legacyLocalId" to legacyId,
                    "completedAt" to old.completedAt, "backlogAddedAt" to old.backlogAddedAt,
                    "backlogResolvedAt" to old.backlogResolvedAt, "revisionMode" to old.revisionMode,
                    "revisionSessions" to old.sessions, "color" to old.color,
                    "removed" to (old.section == "local-deleted")) +
                    (if (old.displayTopic != old.originalTopic) mapOf("displayTopic" to old.displayTopic) else emptyMap())
            }
            if (imports.isEmpty()) return@withLock overview
            val body = gson.toJson(mapOf("lectures" to imports)).toRequestBody("application/json; charset=utf-8".toMediaType())
            when (val result = safeApiCall { api.importLocalProgress(body) }) {
                is Resource.Success -> {
                    check(prefs.edit().apply { imports.forEach { putBoolean("imported:$accountKey:${it["id"]}", true) } }.commit())
                    result.data.officialOnly()
                }
                is Resource.Error -> error(result.message ?: "Could not import saved lecture progress")
                else -> error("Could not import saved lecture progress")
            }
        }
    }
}
