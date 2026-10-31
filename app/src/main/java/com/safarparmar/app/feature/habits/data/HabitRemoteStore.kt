package com.safarparmar.app.feature.habits.data

import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.safarparmar.app.data.local.SafarDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import android.util.Log
import java.io.IOException
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class HabitSaveException(message: String, val uncertain: Boolean, cause: Throwable? = null) : IOException(message, cause)

data class HabitHistory(val habits: List<HabitEntity>, val completions: List<HabitCompletionEntity>,
    val revisions: List<HabitScheduleRevisionEntity>)

data class HabitSyncStatus(val loading: Boolean = true, val error: String? = null, val migrating: Boolean = false)

/** Backend is authoritative. This projection lives only in memory; Room is used only to import old installs. */
@Singleton
class HabitRemoteStore @Inject constructor(
    private val api: HabitApi,
    private val accounts: SafarDataStore,
    private val legacyDatabase: HabitDatabase,
    private val reminders: com.safarparmar.app.feature.habits.reminders.HabitReminderScheduler? = null,
    private val pendingStore: HabitPendingStore? = null,
    private val icons: HabitIconStore? = null
) {
    private data class Projection(
        val owner: String? = null,
        val version: Long? = null,
        val headVersion: Long = -1,
        val habits: List<HabitEntity> = emptyList(),
        val completions: List<HabitCompletionEntity> = emptyList(),
        val revisions: List<HabitScheduleRevisionEntity> = emptyList(),
        val status: HabitSyncStatus = HabitSyncStatus(),
        val remoteRecords: List<HabitRemoteRecord> = emptyList(),
        val completionsByDate: Map<LocalDate, List<HabitCompletionEntity>> = emptyMap(),
        val revisionsByHabit: Map<Long, List<HabitScheduleRevisionEntity>> = emptyMap()
    )
    private val state = MutableStateFlow(Projection())
    private val refreshMutex = Mutex()
    private val writeMutex = Mutex()
    private val projectionMutex = Mutex()
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private data class ReminderWork(val owner: String, val records: List<HabitRemoteRecord>)
    private val reminderWork = MutableStateFlow<ReminderWork?>(null)
    private fun trace(stage: String, started: Long, operation: String? = null) {
        runCatching { Log.i("HabitSync", "stage=$stage operation=$operation durationMs=${(System.nanoTime() - started) / 1_000_000}") }
    }
    private val gson = GsonBuilder().serializeNulls().create()
    private data class ResolvedRecord(val source: HabitRemoteRecord, val date: LocalDate,
        val habit: HabitEntity, val completions: List<HabitCompletionEntity>, val revisions: List<HabitScheduleRevisionEntity>)
    private val resolved = mutableMapOf<String, ResolvedRecord>()
    private val identities = mutableMapOf<String, Long>()
    private var nextIdentity = 1L
    private val retryBatches = mutableMapOf<String, List<JsonObject>>()
    private val createIds = mutableMapOf<String, String>()
    private val ownerFlow = combine(accounts.userId, accounts.isLoggedIn) { id, loggedIn ->
        id?.takeIf { loggedIn && it.isNotBlank() }
    }.distinctUntilChanged()
    private val visible = combine(ownerFlow, state) { owner, value ->
        if (owner != null && owner == value.owner) value else Projection(owner = owner,
            status = HabitSyncStatus(loading = owner != null, error = if (owner == null) "Sign in to load your habits." else null))
    }
    val syncStatus: Flow<HabitSyncStatus> = visible.map { it.status }.distinctUntilChanged()

    init {
        background.launch {
            combine(ownerFlow, reminderWork) { owner, work -> work?.takeIf { it.owner == owner } }
                .filterNotNull().collectLatest { work ->
                while (true) {
                    try {
                        checkOwner(work.owner)
                        val started = System.nanoTime()
                        reminders?.replace(work.owner, work.records)
                        trace("reminders", started)
                        break
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) {
                        if (ownerFlow.first() != work.owner) break
                        runCatching { Log.w("HabitSync", "Reminder reconciliation deferred; saved habits are unaffected") }
                        delay(30_000)
                    }
                }
            }
        }
    }

    private suspend fun owner(): String = ownerFlow.first() ?: error("Sign in to save your habits.")
    private suspend fun checkOwner(expected: String) {
        check(ownerFlow.first() == expected) { "Account changed. Open Habit Tracker again." }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        // Coalesce screen/startup/retry polls. A network read never owns the write lock.
        if (!refreshMutex.tryLock()) return@withContext
        val started = System.nanoTime()
        var expected: String? = null
        try {
            withTimeout(45_000) {
                val owner = owner()
                expected = owner
                projectionMutex.withLock {
                    if (state.value.owner != owner) {
                        state.value = Projection(owner = owner)
                        resolved.clear()
                    }
                }
                val legacy = legacyDatabase.habitDao()
                val importing = legacy.getLegacyHabits(owner).isNotEmpty() || legacy.pendingMutationCount(owner) > 0
                val before = readSnapshot(owner, if (importing) null else state.value.version)
                if (before != null) applySnapshot(owner, before)
                else projectionMutex.withLock {
                    checkOwner(owner)
                    state.value = state.value.copy(status = HabitSyncStatus(false))
                }
                if (importing) {
                    setStatus(owner, HabitSyncStatus(false, migrating = true))
                    // Expose migration explicitly rather than letting a create wait behind it.
                    writeMutex.withLock { applySnapshot(owner, migrateLegacy(owner, before!!.habits)) }
                }
                if (writeMutex.tryLock()) {
                    try {
                        for (pending in pendingStore?.pending(owner).orEmpty()) {
                            withTimeout(15_000) { sendPrepared(owner, pending.key, pending.mutations) }
                        }
                    } finally { writeMutex.unlock() }
                }
            }
        } catch (e: TimeoutCancellationException) {
            expected?.let { setStatus(it, HabitSyncStatus(false, "Habit refresh timed out. Retry when connected.")) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            expected?.let { setStatus(it, HabitSyncStatus(false, habitError(e))) }
            throw e
        } finally {
            expected?.let { owner ->
                // A cancelled migration must not leave the tracker permanently locked.
                if (state.value.owner == owner && state.value.status.migrating)
                    state.value = state.value.copy(status = HabitSyncStatus(false, "Habit import paused. Tap Retry to continue."))
            }
            trace("refresh", started)
            refreshMutex.unlock()
        }
    }

    private suspend fun setStatus(owner: String, status: HabitSyncStatus) = projectionMutex.withLock {
        if (ownerFlow.first() == owner && state.value.owner == owner) state.value = state.value.copy(status = status)
    }

    private fun habitError(error: Exception): String = when (error) {
        is HttpException -> when (error.code()) {
            400 -> "This habit change was rejected. Check the details and try again."
            401, 403 -> "Sign in again to save your habits."
            409 -> "This habit changed elsewhere. Refresh and retry."
            else -> "Could not confirm the save. Retry to check the same operation safely."
        }
        is IllegalStateException -> error.message ?: "Reload your habits and retry."
        else -> "Could not confirm the save. Retry to check the same operation safely."
    }

    private suspend fun readSnapshot(owner: String, knownVersion: Long?): HabitSnapshotResponse? {
        repeat(3) { attempt ->
            try {
                val started = System.nanoTime()
                val first = api.snapshot(owner, if (attempt == 0) knownVersion else null)
                trace("snapshot", started)
                checkOwner(owner)
                if (first.unchanged) return null
                var pages = 1
                val rows = first.habits.toMutableList()
                var cursor = first.nextCursor
                val seen = mutableSetOf<String>()
                while (cursor != null) {
                    check(seen.add(cursor)) { "Repeated habit snapshot cursor" }
                    val page = api.snapshot(owner, first.version, cursor)
                    checkOwner(owner)
                    check(page.version == first.version && !page.unchanged) { "Habit snapshot changed" }
                    pages++
                    rows.addAll(page.habits)
                    cursor = page.nextCursor
                }
                runCatching { Log.i("HabitSync", "snapshotPages=$pages records=${rows.size}") }
                return first.copy(habits = rows, nextCursor = null)
            } catch (e: HttpException) {
                if (e.code() != 409 || attempt == 2) throw e
            }
        }
        error("Habit snapshot changed repeatedly")
    }

    private suspend fun applySnapshot(owner: String, snapshot: HabitSnapshotResponse) = projectionMutex.withLock {
        publishSnapshot(owner, snapshot)
    }

    private suspend fun publishSnapshot(owner: String, snapshot: HabitSnapshotResponse, coveredVersion: Long = snapshot.version) {
        checkOwner(owner)
        if (state.value.owner != owner || snapshot.version < state.value.headVersion) return
        val started = System.nanoTime()
        val today = LocalDate.now()
        val records = snapshot.habits.filterNot { it.deleted }
        resolved.keys.retainAll(records.map { it.id }.toSet())
        val rows = records.map { remote ->
            resolved[remote.id]?.takeIf { it.source === remote && it.date == today } ?: run {
                val id = identities.getOrPut("$owner:${remote.id}") { nextIdentity++ }
                val history = remote.revisions.map { revision ->
                    HabitScheduleRevisionEntity(habitId = id, effectiveFrom = LocalDate.parse(revision.effectiveFrom),
                        targetDays = schedule(revision.targetDays, revision.isEveryDay), isEveryDay = revision.isEveryDay)
                }.sortedBy { it.effectiveFrom }
                val completions = remote.completions.map { HabitCompletionEntity(id, LocalDate.parse(it.date), it.completed) }
                val current = history.lastOrNull { !it.effectiveFrom.isAfter(today) }
                val habit = HabitEntity(id = id, remoteId = remote.id, ownerUserId = owner, name = remote.name,
                    targetDays = current?.targetDays ?: schedule(remote.targetDays, remote.isEveryDay),
                    isEveryDay = current?.isEveryDay ?: remote.isEveryDay, isActive = remote.isActive,
                    order = remote.order, isArchived = remote.archivedAt != null,
                    archivedAt = remote.archivedAt?.let(LocalDate::parse),
                    scheduledSince = LocalDate.parse(remote.scheduledSince), reminderTime = remote.reminderTime)
                ResolvedRecord(remote, today, habit, completions, history).also { resolved[remote.id] = it }
            }
        }
        val habits = rows.map { it.habit }
        val completions = rows.flatMap { it.completions }
        val revisions = rows.flatMap { it.revisions }
        checkOwner(owner)
        state.value = Projection(owner = owner, version = coveredVersion, headVersion = snapshot.version,
            habits = habits, completions = completions, revisions = revisions,
            status = HabitSyncStatus(false), remoteRecords = snapshot.habits,
            completionsByDate = completions.groupBy { it.date }, revisionsByHabit = revisions.groupBy { it.habitId })
        reminderWork.value = ReminderWork(owner, snapshot.habits)
        trace("projection", started)
    }

    private fun schedule(days: List<Int>, everyDay: Boolean): Set<DayOfWeek> =
        if (everyDay) DayOfWeek.entries.toSet() else days.map(DayOfWeek::of).toSet()
    private fun mutation(id: String, type: String, fields: Map<String, Any?> = emptyMap()): JsonObject =
        gson.toJsonTree(fields + mapOf("habitId" to id, "type" to type)).asJsonObject

    private suspend fun send(owner: String, mutations: List<JsonObject>, operationKey: String? = null) {
        checkOwner(owner)
        val key = operationKey ?: mutations.toString()
        val batch = retryBatches.getOrPut("$owner:$key") {
            mutations.map { it.deepCopy().apply { addProperty("mutationId", UUID.randomUUID().toString()) } }
        }
        // Journal before sending; a cancelled/lost response is replayed with the same payload/IDs.
        val prepared = pendingStore?.prepare(owner, key, batch) ?: batch
        sendPrepared(owner, key, prepared)
    }

    private suspend fun sendPrepared(owner: String, key: String, batch: List<JsonObject>) {
        checkOwner(owner)
        val started = System.nanoTime()
        val result = try { api.mutate(owner, HabitBatchRequest(batch)) }
        catch (e: HttpException) {
            // These statuses are definitive rejections, not ambiguous lost responses.
            if (e.code() in setOf(400, 401, 403, 409)) {
                pendingStore?.acknowledge(owner, key)
                retryBatches.remove("$owner:$key")
            }
            throw e
        }
        trace("mutation", started, batch.firstOrNull()?.get("mutationId")?.asString)
        checkOwner(owner)
        if (!result.acknowledged.toSet().containsAll(batch.map { it.get("mutationId").asString }))
            throw HabitSaveException("The server did not confirm this change. Retry confirmation safely.", true)
        try {
            val records = result.habits
            if (records != null) {
                projectionMutex.withLock {
                    checkOwner(owner)
                    val current = state.value
                    if (current.owner == owner && result.version >= current.headVersion) {
                        val changed = records.associateBy { it.id }
                        val merged = current.remoteRecords.filterNot { it.id in changed } + records
                        // A delta cannot certify unseen changes made by another phone. Keep the
                        // last fully covered version so the next poll downloads those changes.
                        val covered = if (current.version == result.previousVersion || current.version == result.version)
                            result.version else current.version ?: 0
                        publishSnapshot(owner, HabitSnapshotResponse(result.version, merged), covered)
                    }
                }
            } else {
                // Compatibility with an older backend: project only the acknowledged operations,
                // then reconcile independently. The save never waits for a full GET.
                projectionMutex.withLock {
                    checkOwner(owner)
                    val current = state.value
                    if (current.owner == owner) {
                        publishSnapshot(owner, HabitSnapshotResponse(maxOf(result.version, current.headVersion),
                            applyAccepted(current.remoteRecords, batch)), current.version ?: 0)
                    }
                }
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            // The server has accepted this operation. A malformed projection must not
            // turn that fact into a failed save; retain the journal and reconcile later.
            setStatus(owner, HabitSyncStatus(false, "Saved. Tap Retry to refresh the habit list."))
            runCatching { Log.w("HabitSync", "Accepted save needs projection reconciliation") }
            background.launch { runCatching { refresh() } }
            return
        }
        // Failure to clean a durable receipt is harmless: replay is idempotent, not a failed save.
        runCatching { pendingStore?.acknowledge(owner, key) }
            .onFailure { runCatching { Log.w("HabitSync", "Confirmed operation retained for safe cleanup") } }
        retryBatches.remove("$owner:$key")
    }

    private fun applyAccepted(records: List<HabitRemoteRecord>, batch: List<JsonObject>): List<HabitRemoteRecord> {
        val values = records.associateBy { it.id }.toMutableMap()
        for (m in batch) {
            val id = m.get("habitId").asString
            val old = values[id]
            if (old?.deleted == true) continue
            fun days() = m.getAsJsonArray("targetDays").map { it.asInt }
            when (m.get("type").asString) {
                "create" -> if (old == null) values[id] = HabitRemoteRecord(id = id, name = m.get("name").asString,
                    targetDays = days(), isEveryDay = m.get("isEveryDay").asBoolean,
                    isActive = m.get("isActive").asBoolean, scheduledSince = m.get("scheduledSince").asString,
                    order = m.get("order").asInt, reminderTime = m.get("reminderTime")?.takeUnless { it.isJsonNull }?.asString,
                    revisions = listOf(HabitRemoteRevision(m.get("scheduledSince").asString, days(), m.get("isEveryDay").asBoolean)))
                "patch" -> if (old != null) {
                    val f = m.getAsJsonObject("fields")
                    values[id] = old.copy(name = f.get("name")?.asString ?: old.name,
                        order = f.get("order")?.asInt ?: old.order, isActive = f.get("isActive")?.asBoolean ?: old.isActive,
                        reminderTime = if (f.has("reminderTime")) f.get("reminderTime").takeUnless { it.isJsonNull }?.asString else old.reminderTime)
                }
                "schedule" -> if (old != null) {
                    val revision = HabitRemoteRevision(m.get("effectiveFrom").asString, days(), m.get("isEveryDay").asBoolean)
                    values[id] = old.copy(revisions = (old.revisions.filterNot { it.effectiveFrom == revision.effectiveFrom } + revision).sortedBy { it.effectiveFrom })
                }
                "completion" -> if (old != null) {
                    val date = m.get("date").asString
                    values[id] = old.copy(completions = old.completions.filterNot { it.date == date } +
                        if (m.get("completed").asBoolean) listOf(HabitRemoteCompletion(date, true)) else emptyList())
                }
                "archive" -> if (old != null) {
                    val date = m.get("archivedAt").asString
                    values[id] = old.copy(archivedAt = date, completions = old.completions.filter { it.date <= date })
                }
                "delete" -> if (old != null) values[id] = old.copy(deleted = true, reminderTime = null, revisions = emptyList(), completions = emptyList())
            }
        }
        return values.values.toList()
    }

    private suspend fun <T> write(block: suspend (String) -> T): T = withContext(Dispatchers.IO) {
        val started = System.nanoTime()
        val owner = owner()
        try {
            withTimeout(18_000) {
                check(!state.value.status.migrating) { "Importing your previous habits. Please wait or retry the import." }
                writeMutex.withLock {
                    trace("write_queue", started)
                    check(state.value.owner == owner && state.value.version != null) { "Load your habits before saving changes." }
                    check(!state.value.status.migrating) { "Importing your previous habits. Please wait or retry the import." }
                    // Never replay an older uncertain edit after accepting a newer one.
                    for (pending in pendingStore?.pending(owner).orEmpty())
                        sendPrepared(owner, pending.key, pending.mutations)
                    block(owner)
                }
            }
        } catch (e: TimeoutCancellationException) {
            val message = "This save timed out. Retry to confirm it safely; it may already be saved."
            setStatus(owner, HabitSyncStatus(false, message))
            throw HabitSaveException(message, true, e)
        } catch (e: CancellationException) { throw e }
        catch (e: HabitSaveException) {
            setStatus(owner, HabitSyncStatus(false, e.message))
            throw e
        }
        catch (e: Exception) {
            setStatus(owner, HabitSyncStatus(false, habitError(e)))
            throw HabitSaveException(habitError(e), e !is IllegalStateException &&
                !(e is HttpException && e.code() in setOf(400, 401, 403, 409)), e)
        } finally { trace("save_total", started) }
    }

    suspend fun insertHabit(habit: HabitEntity, operationId: String? = null, emoji: String? = null): Long = write { owner ->
        // A background refresh may change the display order after a lost response.
        // Keep the same remote identity when the student retries the same form.
        val key = owner + gson.toJson(mapOf("name" to habit.name.trim(),
            "days" to habit.targetDays.map { it.value }.sorted(), "everyDay" to habit.isEveryDay,
            "reminderTime" to habit.reminderTime))
        val id = operationId ?: createIds.getOrPut(key) { UUID.randomUUID().toString() }
        if (emoji != null) icons?.save(owner, id, emoji)
        send(owner, listOf(mutation(id, "create", mapOf("name" to habit.name.trim(),
            "targetDays" to habit.targetDays.map { it.value }.sorted(), "isEveryDay" to habit.isEveryDay,
            "isActive" to habit.isActive, "scheduledSince" to habit.scheduledSince.toString(),
            "order" to habit.order, "reminderTime" to habit.reminderTime))), "create:${operationId ?: key}")
        createIds.remove(key)
        state.value.habits.firstOrNull { it.remoteId == id }?.id ?: 0L
    }

    suspend fun updateHabit(habit: HabitEntity, emoji: String? = null) = write { owner ->
        val previous = owned(habit.id, owner)
        val fields = mutableMapOf<String, Any?>()
        if (previous.name != habit.name) fields["name"] = habit.name.trim()
        if (previous.reminderTime != habit.reminderTime) fields["reminderTime"] = habit.reminderTime
        if (previous.order != habit.order) fields["order"] = habit.order
        if (previous.isActive != habit.isActive) fields["isActive"] = habit.isActive
        val mutations = mutableListOf<JsonObject>()
        if (fields.isNotEmpty()) mutations += mutation(previous.remoteId!!, "patch", mapOf("fields" to fields))
        if (previous.targetDays != habit.targetDays || previous.isEveryDay != habit.isEveryDay)
            mutations += mutation(previous.remoteId!!, "schedule", mapOf("effectiveFrom" to LocalDate.now().toString(),
                "targetDays" to habit.targetDays.map { it.value }.sorted(), "isEveryDay" to habit.isEveryDay))
        if (mutations.isNotEmpty()) send(owner, mutations)
        if (emoji != null) icons?.save(owner, previous.remoteId!!, emoji)
    }
    suspend fun upsertCompletion(completion: HabitCompletionEntity) = write { owner ->
        val existing = state.value.completions.any { it.habitId == completion.habitId && it.date == completion.date && it.completed }
        if (existing == completion.completed) return@write
        send(owner, listOf(mutation(owned(completion.habitId, owner).remoteId!!, "completion",
            mapOf("date" to completion.date.toString(), "completed" to completion.completed))))
    }
    suspend fun clearCompletion(habitId: Long, date: LocalDate) = upsertCompletion(HabitCompletionEntity(habitId, date, false))
    suspend fun archiveHabit(habitId: Long, date: LocalDate) = write { owner ->
        send(owner, listOf(mutation(owned(habitId, owner).remoteId!!, "archive", mapOf("archivedAt" to date.toString()))))
    }
    suspend fun deleteHabit(habit: HabitEntity) = write { owner ->
        send(owner, listOf(mutation(owned(habit.id, owner).remoteId!!, "delete")))
    }
    private fun owned(id: Long, owner: String) = state.value.habits.firstOrNull { it.id == id && it.ownerUserId == owner }
        ?: error("Habit no longer exists in this account.")

    /** One immutable projection per emission: a refresh cannot mix three history versions. */
    fun observeHistory(start: LocalDate, end: LocalDate, activeOnly: Boolean = false) = visible.map { p ->
        val habits = p.habits.filter { h -> if (activeOnly) h.isActive && !h.isArchived else inRange(h, start, end) }
            .sortedBy { it.order }
        val ids = habits.map { it.id }.toSet()
        HabitHistory(habits,
            p.completionsByDate.filterKeys { it in start..end }.values.flatten().filter { it.habitId in ids },
            habits.flatMap { p.revisionsByHabit[it.id].orEmpty() }.filter { it.effectiveFrom <= end })
    }.distinctUntilChanged()

    fun observeAllHabits() = visible.map { it.habits.sortedBy { h -> h.order } }.distinctUntilChanged()
    fun observeActiveHabits() = visible.map { it.habits.filter { h -> h.isActive && !h.isArchived }.sortedBy { h -> h.order } }.distinctUntilChanged()
    fun observeCompletionsForDate(date: LocalDate) = visible.map { it.completions.filter { c -> c.date == date } }.distinctUntilChanged()
    fun observeCompletionsBetween(start: LocalDate, end: LocalDate) = visible.map { it.completions.filter { c -> c.date in start..end } }.distinctUntilChanged()
    fun observeActiveScheduleRevisionsThrough(end: LocalDate) = visible.map { p ->
        val ids = p.habits.filter { it.isActive && !it.isArchived }.map { it.id }.toSet()
        p.revisions.filter { it.habitId in ids && it.effectiveFrom <= end }
    }.distinctUntilChanged()
    private fun inRange(h: HabitEntity, start: LocalDate, end: LocalDate) = h.isActive && h.scheduledSince <= end &&
        (!h.isArchived || h.archivedAt == null || h.archivedAt >= start)
    fun observeHabitsForRange(start: LocalDate, end: LocalDate) = visible.map { it.habits.filter { h -> inRange(h, start, end) }.sortedBy { h -> h.order } }.distinctUntilChanged()
    fun observeScheduleRevisionsForRange(start: LocalDate, end: LocalDate) = visible.map { p ->
        val ids = p.habits.filter { inRange(it, start, end) }.map { it.id }.toSet()
        p.revisions.filter { it.habitId in ids && it.effectiveFrom <= end }
    }.distinctUntilChanged()
    suspend fun getHabitsForRange(start: LocalDate, end: LocalDate) = observeHabitsForRange(start, end).first()
    suspend fun getCompletionsBetween(start: LocalDate, end: LocalDate) = observeCompletionsBetween(start, end).first()
    suspend fun getScheduleRevisionsForRange(start: LocalDate, end: LocalDate) = observeScheduleRevisionsForRange(start, end).first()
    suspend fun getHabitById(id: Long) = visible.first().habits.firstOrNull { it.id == id }
    suspend fun getCompletionsForHabit(id: Long) = visible.first().completions.filter { it.habitId == id && it.completed }
    suspend fun getScheduleRevisionsForHabit(id: Long) = visible.first().revisions.filter { it.habitId == id }

    /** Durable receipts protect the one-time import if the process dies after a server commit. */
    private suspend fun migrateLegacy(owner: String, remote: List<HabitRemoteRecord>): HabitSnapshotResponse {
        val dao = legacyDatabase.habitDao()
        legacyDatabase.withTransaction {
            for (habit in dao.getLegacyHabits(owner)) {
                checkOwner(owner)
                if (habit.ownerUserId == null) {
                    val claimed = habit.copy(ownerUserId = owner, remoteId = habit.remoteId ?: UUID.randomUUID().toString())
                    dao.updateHabit(claimed)
                }
            }
            val queued = dao.getPendingMutations(owner).map { gson.fromJson(it.payload, JsonObject::class.java).get("habitId").asString }.toSet()
            for (habit in dao.getOwnedHabits(owner)) {
                val id = habit.remoteId ?: UUID.randomUUID().toString().also { dao.updateHabit(habit.copy(remoteId = it)) }
                if (id in queued || remote.any { it.id == id }) continue
                val revisions = dao.getScheduleRevisionsForHabit(habit.id)
                val completions = dao.getAllCompletionsForHabit(habit.id)
                val start = (listOf(habit.scheduledSince) + revisions.map { it.effectiveFrom } + completions.map { it.date }).min()
                val initial = revisions.firstOrNull { it.effectiveFrom <= start }
                val changes = mutableListOf(mutation(id, "create", mapOf("name" to habit.name.trim(),
                    "targetDays" to (initial?.targetDays ?: habit.targetDays).map { it.value }.sorted(),
                    "isEveryDay" to (initial?.isEveryDay ?: habit.isEveryDay), "isActive" to habit.isActive,
                    "scheduledSince" to start.toString(), "order" to habit.order.coerceIn(0, 100000), "reminderTime" to habit.reminderTime)))
                changes += revisions.map { mutation(id, "schedule", mapOf("effectiveFrom" to it.effectiveFrom.toString(),
                    "targetDays" to it.targetDays.map { d -> d.value }.sorted(), "isEveryDay" to it.isEveryDay)) }
                changes += completions.map { mutation(id, "completion", mapOf("date" to it.date.toString(), "completed" to it.completed)) }
                habit.archivedAt?.let { changes += mutation(id, "archive", mapOf("archivedAt" to it.toString())) }
                for (change in changes) {
                    val mutationId = UUID.randomUUID().toString()
                    change.addProperty("mutationId", mutationId)
                    dao.enqueueMutation(HabitPendingMutationEntity(mutationId = mutationId, ownerUserId = owner,
                        payload = change.toString(), createdAt = System.currentTimeMillis()))
                }
            }
        }
        while (true) {
            checkOwner(owner)
            val queued = dao.getPendingMutations(owner).take(100)
            if (queued.isEmpty()) break
            val response = api.mutate(owner, HabitBatchRequest(queued.map { gson.fromJson(it.payload, JsonObject::class.java) }))
            checkOwner(owner)
            val ids = queued.map { it.mutationId }
            check(response.acknowledged.toSet().containsAll(ids)) { "Legacy habit import was not acknowledged" }
            dao.deletePendingMutations(ids)
        }
        // Read back before removing the only remaining local legacy copy.
        val confirmed = readSnapshot(owner, null)!!
        val confirmedIds = confirmed.habits.map { it.id }.toSet()
        legacyDatabase.withTransaction {
            checkOwner(owner)
            dao.getOwnedHabits(owner).filter { it.remoteId in confirmedIds }.forEach { dao.deleteHabit(it) }
        }
        return confirmed
    }
}
