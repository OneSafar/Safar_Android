package com.safarparmar.app.ui.ekagra

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.room.*
import com.google.gson.Gson
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.UUID

@Entity(tableName = "sessions", primaryKeys = ["owner", "id"])
internal data class JournalSession(
    val owner: String,
    val id: String,
    val payload: String,
    val state: String, // active, pending, acknowledged, synced (deduplication tombstone)
    val checkpointMs: Long,
    val runtime: String,
    val serverId: String? = null,
)

@Dao
internal interface EkagraJournalDao {
    @Query("SELECT * FROM sessions WHERE owner = :owner")
    suspend fun rows(owner: String): List<JournalSession>
    @Query("SELECT * FROM sessions WHERE owner = :owner AND id = :id")
    suspend fun row(owner: String, id: String): JournalSession?
    @Query("SELECT * FROM sessions WHERE owner = :owner AND state IN ('pending', 'acknowledged')")
    fun observe(owner: String): Flow<List<JournalSession>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(row: JournalSession)
}

@Database(entities = [JournalSession::class], version = 1, exportSchema = false)
internal abstract class EkagraJournalDatabase : RoomDatabase() {
    abstract fun journal(): EkagraJournalDao
}

/** One ordered writer owns checkpoint/finalize/upload acknowledgements. No disk work on Main. */
internal class EkagraSessionJournal internal constructor(
    private val context: Context,
    private val database: EkagraJournalDatabase = Room.databaseBuilder(context, EkagraJournalDatabase::class.java, "ekagra-journal.db").build(),
    private val accounts: Flow<String?> = SafarDataStore(context).userId,
    private val runtime: String = RUNTIME,
    private val platformEffects: Boolean = true,
) {
    private val dao = database.journal()
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var initializedOwner: String? = null
    val errors = MutableStateFlow<String?>(null)

    init { scope.launch { for (command in commands) command() } }

    private val failedWrites = mutableListOf<suspend () -> Unit>()

    private fun <T> ordered(retryable: Boolean = false, block: suspend () -> T): Deferred<T> {
        val result = CompletableDeferred<T>()
        commands.trySend {
            try { result.complete(block()) }
            catch (error: Exception) {
                if (retryable) failedWrites.add { block(); Unit }
                errors.value = "Study time could not be saved on this phone. Free up storage and retry."
                EkagraDiagnostics.record(context, "journal_failure", error.javaClass.simpleName)
                result.completeExceptionally(error)
            }
        }.getOrThrow()
        return result
    }

    private suspend fun owner(): String = accounts.first().orEmpty()

    private suspend fun initialize(account: String) {
        if (account.isBlank() || initializedOwner == account) return
        // Legacy preferences have no account field. Adopt them once for the signed-in account.
        var recoveredLegacy = false
        val migration = context.getSharedPreferences("ekagra_journal_migration", Context.MODE_PRIVATE)
        if (!migration.getBoolean("done", false)) {
            val pending = EkagraPendingSessionSaveStore.legacySessions(context)
            val active = legacyCheckpoint(context)
            recoveredLegacy = active != null
            database.withTransaction {
                for (session in pending + listOfNotNull(active)) {
                    if (dao.row(account, session.clientSessionId) == null) {
                        dao.put(JournalSession(account, session.clientSessionId, gson.toJson(session), "pending", System.currentTimeMillis(), runtime))
                    }
                }
            }
            check(migration.edit().putBoolean("done", true).commit())
            // Originals remain as migration backups; the marker prevents re-import.
        }
        val interrupted = database.withTransaction {
            val rows = dao.rows(account).filter { it.state == "active" && it.runtime != runtime }
            rows.forEach { row ->
                val session = decode(row).copy(endReason = "interrupted")
                dao.put(row.copy(payload = gson.toJson(session), state = "pending"))
                EkagraDiagnostics.record(context, "interrupted", "checkpoint_age_ms=${(System.currentTimeMillis() - row.checkpointMs).coerceAtLeast(0)}")
            }
            rows.isNotEmpty()
        }
        initializedOwner = account
        if ((interrupted || recoveredLegacy) && platformEffects) EkagraTimerAlarms.showRecovered(context)
    }

    fun checkpoint(session: PendingEkagraSessionSave, account: String): Deferred<Unit> = ordered {
        if (account.isBlank()) return@ordered
        initialize(account)
        database.withTransaction {
            val old = dao.row(account, session.clientSessionId)
            if (old == null || old.state == "active") {
                dao.put(JournalSession(account, session.clientSessionId, gson.toJson(session), "active", System.currentTimeMillis(), runtime))
            }
        }
    }

    fun enqueue(session: PendingEkagraSessionSave, account: String? = null): Deferred<Unit> {
        var boundAccount = account
        return ordered(retryable = true) {
        val user = boundAccount ?: owner().also { boundAccount = it }
        check(user.isNotBlank()) { "A signed-in account is required" }
        initialize(user)
        database.withTransaction {
            val old = dao.row(user, session.clientSessionId)
            // Finalization is idempotent, including callbacks arriving after an upload.
            if (old == null || old.state == "active") {
                dao.put(JournalSession(user, session.clientSessionId, gson.toJson(session), "pending", System.currentTimeMillis(), runtime))
            }
        }
        if (platformEffects) EkagraSessionSaveWorker.enqueue(context)
        }
    }

    suspend fun find(id: String, account: String): PendingEkagraSessionSave? = ordered {
        if (account != owner()) return@ordered null
        dao.row(account, id)?.let { decode(it).copy(ownerId = account, serverId = it.serverId) }
    }.await()

    suspend fun pending(): List<PendingEkagraSessionSave> = ordered {
        val account = owner()
        initialize(account)
        dao.rows(account).filter { it.state == "pending" }.map { decode(it).copy(ownerId = account) }
            .filter { (it.actualDurationSeconds ?: it.actualDurationMinutes * 60) > 0 }
    }.await()

    suspend fun acknowledge(session: PendingEkagraSessionSave, serverId: String) = ordered {
        val account = session.ownerId ?: return@ordered
        val row = dao.row(account, session.clientSessionId) ?: return@ordered
        if (row.state != "synced") dao.put(row.copy(state = "acknowledged", serverId = serverId))
    }.await()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<List<PendingEkagraSessionSave>> = accounts.flatMapLatest { id ->
        val account = id.orEmpty()
        flow {
            runCatching { ordered { initialize(account) }.await() }
            emitAll(dao.observe(account).map { rows -> rows.map { decode(it).copy(ownerId = account, serverId = it.serverId) }
                .filter { (it.actualDurationSeconds ?: it.actualDurationMinutes * 60) > 0 } }
                .retryWhen { _, _ ->
                    errors.value = "Study history could not be read on this phone. Free up storage and retry."
                    delay(5000)
                    true
                })
        }
    }

    fun retryWrites(): Deferred<Unit> = ordered {
        val retry = failedWrites.toList()
        retry.forEach { it() }
        failedWrites.clear()
        errors.value = null
    }

    fun reconcile(serverIds: Set<String>): Deferred<Unit> = ordered {
        val account = owner()
        database.withTransaction {
            dao.rows(account).filter { it.state != "active" && (it.id in serverIds || it.serverId in serverIds) }
                .forEach { dao.put(it.copy(state = "synced")) }
        }
    }

    fun recover(): Deferred<Unit> = ordered { initialize(owner()) }
    private fun decode(row: JournalSession): PendingEkagraSessionSave = gson.fromJson(row.payload, PendingEkagraSessionSave::class.java)

    companion object {
        private val RUNTIME = UUID.randomUUID().toString()
        @Volatile private var instance: EkagraSessionJournal? = null
        fun get(context: Context): EkagraSessionJournal = instance ?: synchronized(this) {
            instance ?: EkagraSessionJournal(context.applicationContext).also { instance = it }
        }
    }
}

internal object EkagraDiagnostics {
    fun record(context: Context, event: String, detail: String = "") {
        val exit = if (Build.VERSION.SDK_INT >= 30) runCatching {
            context.getSystemService(ActivityManager::class.java).getHistoricalProcessExitReasons(null, 0, 1).firstOrNull()?.reason
        }.getOrNull() else null
        val line = "event=$event detail=$detail build=${BuildConfig.VERSION_CODE} device=${Build.MANUFACTURER}/${Build.MODEL} sdk=${Build.VERSION.SDK_INT} exit=$exit"
        Log.i("EkagraReliability", line)
        runCatching { com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log(line) }
    }
}

internal fun legacyCheckpoint(context: Context): PendingEkagraSessionSave? {
    val p = context.getSharedPreferences("ekagra_timer_state_prefs", Context.MODE_PRIVATE)
    if (!p.getBoolean("has_state", false)) return null
    val mode = runCatching { TimerMode.valueOf(p.getString("mode", "FOCUS")!!) }.getOrDefault(TimerMode.FOCUS)
    val suspended = p.getInt("suspended_total_seconds", 0)
    val loops = p.getInt("target_pomodoro_loops", 0)
    if (mode == TimerMode.BREAK && suspended == 0 && loops == 0) return null
    val progress = calculateFocusProgress(
        if (suspended > 0) if (loops > 0) TimerMode.POMODORO else TimerMode.FOCUS else mode,
        if (suspended > 0) suspended else p.getInt("total_seconds", 0),
        if (suspended > 0) p.getInt("suspended_remaining_seconds", 0) else p.getInt("remaining_seconds", 0),
        p.getInt("pomodoro_focus_seconds", 1500), loops, p.getInt("completed_pomodoro_loops", 0),
    )
    if (progress.actualSeconds <= 0) return null
    val end = Instant.ofEpochMilli(p.getLong("saved_at_ms", System.currentTimeMillis()))
    return PendingEkagraSessionSave(
        clientSessionId = p.getString("auto_save_client_session_id", null) ?: "legacy-${UUID.nameUUIDFromBytes((p.getLong("saved_at_ms", 0).toString() + ":" + progress.actualSeconds + ":" + mode).toByteArray())}",
        mode = (if (loops > 0) TimerMode.POMODORO else if (suspended > 0) TimerMode.FOCUS else mode).toApiMode(),
        startedAt = p.getString("auto_save_started_at", null) ?: end.minusSeconds(progress.actualSeconds.toLong()).toString(),
        endedAt = end.toString(), plannedDurationMinutes = if (mode == TimerMode.STOPWATCH) 0 else (progress.plannedSeconds + 59) / 60,
        actualDurationMinutes = kotlin.math.round(progress.actualSeconds / 60.0).toInt(), actualDurationSeconds = progress.actualSeconds,
        goalId = p.getString("auto_save_goal_id", null), goalTitle = p.getString("auto_save_goal_title", null),
        topicId = p.getString("auto_save_topic_id", null), planId = p.getString("auto_save_plan_id", null), topicTitle = p.getString("auto_save_topic_title", null),
        taskTitle = p.getString("auto_save_task_title", null) ?: "Untitled", shieldEnabled = false, endReason = "interrupted",
    )
}
