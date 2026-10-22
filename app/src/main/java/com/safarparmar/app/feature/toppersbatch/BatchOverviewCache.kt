package com.safarparmar.app.feature.toppersbatch

import android.content.Context
import com.google.gson.Gson
import com.safarparmar.app.data.local.SafarDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Last successful snapshot for this signed-in account; API refresh remains authoritative. */
@Singleton
class BatchOverviewCache @Inject constructor(@ApplicationContext context: Context, private val accounts: SafarDataStore) {
    private val prefs = context.getSharedPreferences("toppers_overview_v1", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val writes = Mutex()
    private val revision = AtomicLong()
    fun version() = revision.get()
    suspend fun account(): String? = accounts.userId.first()?.takeIf { it.isNotBlank() }
    suspend fun studyMode(courseId: String = TOPPERS_COURSE_ID): BatchStudyMode? = withContext(Dispatchers.IO) {
        val user = account() ?: return@withContext null
        val value = prefs.getString("study-mode:${cacheKey(user, courseId)}", null)
        if (account() != user) return@withContext null
        BatchStudyMode.entries.firstOrNull { it.name == value }
    }
    suspend fun saveStudyMode(mode: BatchStudyMode, courseId: String = TOPPERS_COURSE_ID) = withContext(Dispatchers.IO) {
        val user = account() ?: return@withContext
        writes.withLock {
            if (account() == user) prefs.edit().putString("study-mode:${cacheKey(user, courseId)}", mode.name).apply()
        }
    }
    suspend fun load(courseId: String = TOPPERS_COURSE_ID): BatchOverview? = withContext(Dispatchers.IO) {
        val user = account() ?: return@withContext null
        val saved = prefs.getString(cacheKey(user, courseId), null) ?: return@withContext null
        val snapshot = runCatching { gson.fromJson(saved, Snapshot::class.java) }.getOrNull() ?: return@withContext null
        if (account() != user || System.currentTimeMillis() - snapshot.savedAt !in 0..MAX_AGE_MS) return@withContext null
        runCatching { snapshot.overview.takeIf { it.course.id.ifBlank { TOPPERS_COURSE_ID } == courseId }?.officialOnly() }.getOrNull()
    }
    suspend fun save(user: String?, overview: BatchOverview, version: Long) = withContext(Dispatchers.IO) {
        writes.withLock {
            if (user != null && account() == user && version == revision.get())
                prefs.edit().putString(cacheKey(user, overview.course.id), gson.toJson(Snapshot(System.currentTimeMillis(), overview))).apply()
        }
    }
    suspend fun clear(courseId: String = TOPPERS_COURSE_ID) = withContext(Dispatchers.IO) {
        writes.withLock {
            revision.incrementAndGet()
            account()?.let { prefs.edit().remove(cacheKey(it, courseId)).apply() }
        }
    }
    private fun cacheKey(user: String, courseId: String) = if (courseId.isBlank() || courseId == TOPPERS_COURSE_ID) user else "$user:$courseId"
    private data class Snapshot(val savedAt: Long, val overview: BatchOverview)
    private companion object { const val MAX_AGE_MS = 24 * 60 * 60 * 1000L }
}
