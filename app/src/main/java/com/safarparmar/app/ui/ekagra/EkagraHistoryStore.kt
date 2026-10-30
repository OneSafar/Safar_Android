package com.safarparmar.app.ui.ekagra

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A disposable read model. It is deliberately separate from the upload/recovery journal. */
@Entity(tableName = "history_snapshots")
internal data class EkagraHistorySnapshot(
    @PrimaryKey val owner: String,
    val payload: String,
    val fetchedAtMs: Long,
    val formatVersion: Int = 1,
    val stale: Boolean = false,
    val suppressedIds: String = "[]",
)

@Dao
internal interface EkagraHistoryDao {
    @Query("SELECT * FROM history_snapshots WHERE owner = :owner")
    suspend fun snapshot(owner: String): EkagraHistorySnapshot?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(snapshot: EkagraHistorySnapshot)
}

@Database(entities = [EkagraHistorySnapshot::class], version = 1, exportSchema = false)
internal abstract class EkagraHistoryDatabase : RoomDatabase() {
    abstract fun history(): EkagraHistoryDao
}

data class EkagraHistoryState(
    val owner: String? = null,
    val data: EkagraAnalyticsStats = EkagraAnalyticsStats(),
    val lastRefreshAtMs: Long? = null,
    val refreshing: Boolean = false,
    val stale: Boolean = false,
    val offline: Boolean = false,
    val cacheError: Boolean = false,
    val suppressedSessionIds: Set<String> = emptySet(),
)

internal data class EkagraHistoryRequest(val owner: String, val generation: Long)
internal enum class HistoryCommit { REJECTED, STORED, MEMORY_ONLY }

@Singleton
class EkagraHistoryStore internal constructor(
    private val database: EkagraHistoryDatabase,
    private val accounts: Flow<String?>,
    private val reportFailure: (Throwable) -> Unit = {},
) {
    @Inject constructor(@ApplicationContext context: Context, accounts: SafarDataStore) : this(
        Room.databaseBuilder(context, EkagraHistoryDatabase::class.java, "ekagra-history-cache.db").build(),
        accounts.userId,
        { EkagraDiagnostics.recordFailure(context, "history_cache_failed", it) },
    )

    private val mutex = Mutex()
    private val gson = Gson()
    private var generation = 0L
    private val _state = MutableStateFlow(EkagraHistoryState())
    val state = _state.asStateFlow()
    private var suppressed = emptySet<String>()

    suspend fun account(): String? = accounts.first()?.takeIf { it.isNotBlank() }

    internal suspend fun activate(owner: String?): EkagraHistoryState = mutex.withLock {
        if (_state.value.owner == owner && owner != null) return@withLock _state.value
        generation++
        suppressed = emptySet()
        _state.value = EkagraHistoryState(owner = owner)
        if (owner.isNullOrBlank() || account() != owner) return@withLock _state.value
        try {
            val row = withContext(Dispatchers.IO) { database.history().snapshot(owner) }
            if (account() != owner) return@withLock _state.value
            if (row != null && row.formatVersion == 1) {
                val data = requireNotNull(gson.fromJson(row.payload, EkagraAnalyticsStats::class.java))
                suppressed = gson.fromJson<Set<String>>(row.suppressedIds, object : TypeToken<Set<String>>() {}.type)
                _state.value = EkagraHistoryState(owner, filter(data), row.fetchedAtMs, stale = true, suppressedSessionIds = suppressed)
            }
        } catch (error: Exception) {
            reportFailure(error)
            _state.value = _state.value.copy(cacheError = true, stale = true)
        }
        _state.value
    }

    internal suspend fun beginRefresh(owner: String): EkagraHistoryRequest? = mutex.withLock {
        if (account() != owner || _state.value.owner != owner) return@withLock null
        _state.value = _state.value.copy(refreshing = true)
        EkagraHistoryRequest(owner, ++generation)
    }

    private suspend fun accepts(request: EkagraHistoryRequest): Boolean =
        request.generation == generation && _state.value.owner == request.owner && account() == request.owner

    /** Persist first. Only STORED permits the caller to reconcile journal acknowledgments. */
    internal suspend fun commit(request: EkagraHistoryRequest, data: EkagraAnalyticsStats): HistoryCommit = mutex.withLock {
        if (!accepts(request)) return@withLock HistoryCommit.REJECTED
        val now = System.currentTimeMillis()
        try {
            withContext(Dispatchers.IO) {
                database.history().put(EkagraHistorySnapshot(request.owner, gson.toJson(data), now, suppressedIds = gson.toJson(suppressed)))
            }
            if (!accepts(request)) return@withLock HistoryCommit.REJECTED
            _state.value = EkagraHistoryState(request.owner, filter(data), now, suppressedSessionIds = suppressed)
            HistoryCommit.STORED
        } catch (error: Exception) {
            reportFailure(error)
            _state.value = _state.value.copy(data = filter(data), refreshing = false, stale = true, cacheError = true)
            HistoryCommit.MEMORY_ONLY
        }
    }

    internal suspend fun failed(request: EkagraHistoryRequest) = mutex.withLock {
        if (accepts(request)) _state.value = _state.value.copy(refreshing = false, stale = true, offline = true)
    }

    /** Only confirmed server writes may patch this read cache; nothing is enqueued/uploaded here. */
    internal suspend fun confirmedEdit(owner: String?, id: String, title: String? = null, goalId: String? = null) =
        mutate(owner) { data ->
            val aliases = data.focusSessions.filter { it.id == id || it.sourceSessionId == id }
                .flatMap { listOfNotNull(it.id, it.sourceSessionId) }.toSet() + id
            data.copy(
                focusSessions = data.focusSessions.map { row ->
                    if (row.id !in aliases && row.sourceSessionId !in aliases) row else row.copy(
                        taskText = title ?: row.taskText,
                        associatedGoalId = goalId ?: row.associatedGoalId,
                        isGoalLinked = if (goalId != null) true else row.isGoalLinked,
                    )
                },
                recentSessions = data.recentSessions.map { row ->
                    if (row.id !in aliases) row else row.copy(taskText = title ?: row.taskText,
                        associatedGoalId = goalId ?: row.associatedGoalId,
                        isGoalLinked = if (goalId != null) true else row.isGoalLinked)
                },
            )
        }

    internal suspend fun confirmedDeletion(owner: String?, id: String) = mutate(owner) { data ->
        suppressed = suppressed + id + data.focusSessions.filter { it.id == id || it.sourceSessionId == id }
            .flatMap { listOfNotNull(it.id, it.sourceSessionId) }
        filter(data)
    }

    private suspend fun mutate(owner: String?, patch: (EkagraAnalyticsStats) -> EkagraAnalyticsStats) = mutex.withLock {
        if (owner.isNullOrBlank() || account() != owner || _state.value.owner != owner) return@withLock
        generation++ // Reject a refresh started before the confirmed write.
        val current = _state.value
        val updated = current.copy(data = patch(current.data), stale = true, refreshing = false, suppressedSessionIds = suppressed)
        _state.value = updated
        // With no successful snapshot yet, do not manufacture authoritative empty history.
        val fetchedAt = current.lastRefreshAtMs ?: return@withLock
        try {
            withContext(Dispatchers.IO) {
                database.history().put(EkagraHistorySnapshot(owner, gson.toJson(updated.data), fetchedAt,
                    stale = true, suppressedIds = gson.toJson(suppressed)))
            }
        } catch (error: Exception) {
            reportFailure(error)
            _state.value = updated.copy(cacheError = true)
        }
    }

    private fun filter(data: EkagraAnalyticsStats) = data.copy(
        focusSessions = data.focusSessions.filter { it.id !in suppressed && it.sourceSessionId !in suppressed },
        recentSessions = data.recentSessions.filter { it.id !in suppressed },
    )
}
