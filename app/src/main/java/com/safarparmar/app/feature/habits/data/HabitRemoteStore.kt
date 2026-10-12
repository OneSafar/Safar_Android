package com.safarparmar.app.feature.habits.data

import androidx.room.withTransaction
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.safarparmar.app.data.local.SafarDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class HabitSyncStatus(val loading: Boolean = true, val error: String? = null)

/** Backend is authoritative. This projection lives only in memory; Room is used only to import old installs. */
@Singleton
class HabitRemoteStore @Inject constructor(
    private val api: HabitApi,
    private val accounts: SafarDataStore,
    private val legacyDatabase: HabitDatabase
) {
    private data class Projection(
        val owner: String? = null,
        val version: Long? = null,
        val habits: List<HabitEntity> = emptyList(),
        val completions: List<HabitCompletionEntity> = emptyList(),
        val revisions: List<HabitScheduleRevisionEntity> = emptyList(),
        val status: HabitSyncStatus = HabitSyncStatus()
    )
    private val state = MutableStateFlow(Projection())
    private val mutex = Mutex()
    private val gson = GsonBuilder().serializeNulls().create()
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

    private suspend fun owner(): String = ownerFlow.first() ?: error("Sign in to save your habits.")
    private suspend fun checkOwner(expected: String) {
        check(ownerFlow.first() == expected) { "Account changed. Open Habit Tracker again." }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val owner = owner()
            if (state.value.owner != owner) {
                state.value = Projection(owner = owner)
                retryBatches.clear()
                createIds.clear()
            }
            try {
                // Existing server records win over older local copies of the same remote habit.
                val legacy = legacyDatabase.habitDao()
                val importing = legacy.getLegacyHabits(owner).isNotEmpty() || legacy.pendingMutationCount(owner) > 0
                val before = readSnapshot(owner, if (importing) null else state.value.version)
                if (importing) applySnapshot(owner, migrateLegacy(owner, before!!.habits))
                else if (before != null) applySnapshot(owner, before)
                else state.value = state.value.copy(status = HabitSyncStatus(false))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (ownerFlow.first() == owner) state.value = state.value.copy(status = HabitSyncStatus(false,
                    "Could not load habits. Check your connection and retry."))
                throw e
            }
        }
    }

    private suspend fun readSnapshot(owner: String, knownVersion: Long?): HabitSnapshotResponse? {
        repeat(3) { attempt ->
            try {
                val first = api.snapshot(owner, if (attempt == 0) knownVersion else null)
                checkOwner(owner)
                if (first.unchanged) return null
                val rows = first.habits.toMutableList()
                var cursor = first.nextCursor
                val seen = mutableSetOf<String>()
                while (cursor != null) {
                    check(seen.add(cursor)) { "Repeated habit snapshot cursor" }
                    val page = api.snapshot(owner, first.version, cursor)
                    checkOwner(owner)
                    check(page.version == first.version && !page.unchanged) { "Habit snapshot changed" }
                    rows.addAll(page.habits)
                    cursor = page.nextCursor
                }
                return first.copy(habits = rows, nextCursor = null)
            } catch (e: HttpException) {
                if (e.code() != 409 || attempt == 2) throw e
            }
        }
        error("Habit snapshot changed repeatedly")
    }

    private suspend fun applySnapshot(owner: String, snapshot: HabitSnapshotResponse) {
        checkOwner(owner)
        val completions = mutableListOf<HabitCompletionEntity>()
        val revisions = mutableListOf<HabitScheduleRevisionEntity>()
        val habits = snapshot.habits.filterNot { it.deleted }.map { remote ->
            val id = identities.getOrPut("$owner:${remote.id}") { nextIdentity++ }
            val history = remote.revisions.map { revision ->
                HabitScheduleRevisionEntity(habitId = id, effectiveFrom = LocalDate.parse(revision.effectiveFrom),
                    targetDays = schedule(revision.targetDays, revision.isEveryDay), isEveryDay = revision.isEveryDay)
            }.sortedBy { it.effectiveFrom }
            revisions.addAll(history)
            completions.addAll(remote.completions.map { HabitCompletionEntity(id, LocalDate.parse(it.date), it.completed) })
            val current = history.lastOrNull { !it.effectiveFrom.isAfter(LocalDate.now()) }
            HabitEntity(id = id, remoteId = remote.id, ownerUserId = owner, name = remote.name,
                targetDays = current?.targetDays ?: schedule(remote.targetDays, remote.isEveryDay),
                isEveryDay = current?.isEveryDay ?: remote.isEveryDay, isActive = remote.isActive,
                order = remote.order, isArchived = remote.archivedAt != null,
                archivedAt = remote.archivedAt?.let(LocalDate::parse),
                scheduledSince = LocalDate.parse(remote.scheduledSince), reminderTime = remote.reminderTime)
        }
        state.value = Projection(owner, snapshot.version, habits, completions, revisions, HabitSyncStatus(false))
    }

    private fun schedule(days: List<Int>, everyDay: Boolean): Set<DayOfWeek> =
        if (everyDay) DayOfWeek.entries.toSet() else days.map(DayOfWeek::of).toSet()
    private fun mutation(id: String, type: String, fields: Map<String, Any?> = emptyMap()): JsonObject =
        gson.toJsonTree(fields + mapOf("habitId" to id, "type" to type)).asJsonObject

    private suspend fun send(owner: String, mutations: List<JsonObject>) {
        checkOwner(owner)
        val key = owner + mutations.toString()
        val batch = retryBatches.getOrPut(key) {
            mutations.map { it.deepCopy().apply { addProperty("mutationId", UUID.randomUUID().toString()) } }
        }
        val result = api.mutate(owner, HabitBatchRequest(batch))
        checkOwner(owner)
        check(result.acknowledged.toSet().containsAll(batch.map { it.get("mutationId").asString })) { "Habit save was not acknowledged" }
        applySnapshot(owner, readSnapshot(owner, null)!!)
        retryBatches.remove(key)
    }

    private suspend fun <T> write(block: suspend (String) -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            val owner = owner()
            check(state.value.owner == owner && state.value.version != null) { "Load your habits before saving changes." }
            try { block(owner) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (ownerFlow.first() == owner) state.value = state.value.copy(status = HabitSyncStatus(false,
                    "Could not confirm the save. Check your connection and retry."))
                throw e
            }
        }
    }

    suspend fun insertHabit(habit: HabitEntity): Long = write { owner ->
        // A background refresh may change the display order after a lost response.
        // Keep the same remote identity when the student retries the same form.
        val key = owner + gson.toJson(mapOf("name" to habit.name.trim(),
            "days" to habit.targetDays.map { it.value }.sorted(), "everyDay" to habit.isEveryDay,
            "reminderTime" to habit.reminderTime))
        val id = createIds.getOrPut(key) { UUID.randomUUID().toString() }
        send(owner, listOf(mutation(id, "create", mapOf("name" to habit.name.trim(),
            "targetDays" to habit.targetDays.map { it.value }.sorted(), "isEveryDay" to habit.isEveryDay,
            "isActive" to habit.isActive, "scheduledSince" to habit.scheduledSince.toString(),
            "order" to habit.order, "reminderTime" to habit.reminderTime))))
        createIds.remove(key)
        state.value.habits.first { it.remoteId == id }.id
    }

    suspend fun updateHabit(habit: HabitEntity) = write { owner ->
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
    }
    suspend fun upsertCompletion(completion: HabitCompletionEntity) = write { owner ->
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

    fun observeActiveHabits() = visible.map { it.habits.filter { h -> h.isActive && !h.isArchived }.sortedBy { h -> h.order } }
    fun observeCompletionsForDate(date: LocalDate) = visible.map { it.completions.filter { c -> c.date == date } }
    fun observeCompletionsBetween(start: LocalDate, end: LocalDate) = visible.map { it.completions.filter { c -> c.date in start..end } }
    fun observeActiveScheduleRevisionsThrough(end: LocalDate) = visible.map { p ->
        val ids = p.habits.filter { it.isActive && !it.isArchived }.map { it.id }.toSet()
        p.revisions.filter { it.habitId in ids && it.effectiveFrom <= end }
    }
    private fun inRange(h: HabitEntity, start: LocalDate, end: LocalDate) = h.isActive && h.scheduledSince <= end &&
        (!h.isArchived || h.archivedAt == null || h.archivedAt >= start)
    fun observeHabitsForRange(start: LocalDate, end: LocalDate) = visible.map { it.habits.filter { h -> inRange(h, start, end) }.sortedBy { h -> h.order } }
    fun observeScheduleRevisionsForRange(start: LocalDate, end: LocalDate) = visible.map { p ->
        val ids = p.habits.filter { inRange(it, start, end) }.map { it.id }.toSet()
        p.revisions.filter { it.habitId in ids && it.effectiveFrom <= end }
    }
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
