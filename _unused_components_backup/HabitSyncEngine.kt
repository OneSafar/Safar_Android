package com.safarparmar.app.feature.habits.data

import androidx.room.withTransaction
import android.util.Log
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.reminders.HabitReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitSyncEngine @Inject constructor(
    private val dao: HabitDao,
    private val database: HabitDatabase,
    private val api: HabitApi,
    private val dataStore: SafarDataStore,
    private val reminderScheduler: HabitReminderScheduler
) {
    private val syncMutex = Mutex()

    suspend fun activateCurrentAccount() {
        val owner = currentOwner() ?: return
        claimLegacy(owner)
        reminderScheduler.rescheduleAll(owner)
    }

    suspend fun sync(): Boolean = syncMutex.withLock {
        val owner = currentOwner() ?: return true
        claimLegacy(owner)
        while (true) {
            if (currentOwner() != owner) return true
            val pending = dao.pendingMutations(owner, 100)
            if (pending.isEmpty()) break
            val acknowledgement = api.mutate(owner, HabitBatchRequest(pending.map { HabitMutationJson.decode(it.payload) }))
            val sent = pending.map { it.mutationId }.toSet()
            if (acknowledgement.acknowledged.toSet() != sent) error("Incomplete habit mutation acknowledgement")
            database.withTransaction { dao.removeAcknowledgedMutations(owner, acknowledgement.acknowledged) }
        }

        // A mutation on another device may change the version midway through pages.
        // Never apply a partial or mixed-version snapshot.
        repeat(3) {
            val knownVersion = dao.getSyncState(owner)?.version
            val records = mutableListOf<HabitRemoteRecord>()
            var cursor: String? = null
            var snapshotVersion: Long? = null
            var restartSnapshot = false
            try {
                do {
                    if (currentOwner() != owner) return true
                    val page = api.snapshot(owner, if (cursor == null) knownVersion else snapshotVersion, cursor)
                    if (page.unchanged) {
                        dao.upsertSyncState(HabitSyncStateEntity(owner, page.version,
                            lastSyncedAt = System.currentTimeMillis()))
                        reminderScheduler.rescheduleAll(owner)
                        return true
                    }
                    if (snapshotVersion != null && page.version != snapshotVersion) {
                        restartSnapshot = true
                        break
                    }
                    snapshotVersion = page.version
                    records.addAll(page.habits)
                    cursor = page.nextCursor
                } while (cursor != null)
            } catch (error: HttpException) {
                if (error.code() == 409) return@repeat
                throw error
            }
            if (restartSnapshot) return@repeat
            if (currentOwner() != owner) return true
            var removedIds = emptyList<String>()
            val applied = database.withTransaction {
                if (dao.pendingMutationCount(owner) > 0) return@withTransaction false
                val existing = dao.getOwnedHabits(owner).associateBy { it.remoteId }
                val retained = mutableSetOf<String>()
                for (remote in records) {
                    if (remote.deleted) continue
                    retained.add(remote.id)
                    val habit = HabitEntity(
                        id = existing[remote.id]?.id ?: 0,
                        name = remote.name,
                        isActive = remote.isActive,
                        targetDays = remote.targetDays.map(DayOfWeek::of).toSet(),
                        isEveryDay = remote.isEveryDay,
                        scheduledSince = LocalDate.parse(remote.scheduledSince),
                        order = remote.order,
                        isArchived = remote.archivedAt != null,
                        archivedAt = remote.archivedAt?.let(LocalDate::parse),
                        remoteId = remote.id,
                        ownerUserId = owner,
                        reminderTime = remote.reminderTime
                    )
                    val habitId = if (habit.id == 0L) dao.insertHabit(habit) else {
                        dao.updateHabit(habit)
                        dao.deleteRevisionsForHabit(habit.id)
                        dao.deleteCompletionsForHabit(habit.id)
                        habit.id
                    }
                    for (revision in remote.revisions) dao.upsertScheduleRevision(HabitScheduleRevisionEntity(
                        habitId = habitId,
                        effectiveFrom = LocalDate.parse(revision.effectiveFrom),
                        targetDays = revision.targetDays.map(DayOfWeek::of).toSet(),
                        isEveryDay = revision.isEveryDay
                    ))
                    for (completion in remote.completions) if (completion.completed) {
                        dao.upsertCompletion(HabitCompletionEntity(habitId, LocalDate.parse(completion.date), true))
                    }
                }
                val removed = existing.values.filter { it.remoteId !in retained }
                removedIds = removed.mapNotNull { it.remoteId }
                removed.forEach { dao.deleteHabit(it) }
                dao.upsertSyncState(HabitSyncStateEntity(owner, snapshotVersion!!,
                    lastSyncedAt = System.currentTimeMillis()))
                true
            }
            if (!applied) return false
            removedIds.forEach { reminderScheduler.cancelForHabit(owner, it) }
            reminderScheduler.rescheduleAll(owner)
            return true
        }
        false
    }

    suspend fun noteFailure() {
        val owner = currentOwner() ?: return
        val current = dao.getSyncState(owner)
        dao.upsertSyncState((current ?: HabitSyncStateEntity(owner, 0)).copy(
            lastError = "Synchronization will retry"))
    }

    suspend fun deleteLocalAccount(owner: String) {
        try {
            reminderScheduler.cancelAllForOwner(owner)
        } catch (error: Exception) {
            Log.w("HabitReminder", "Unable to cancel alarms during account deletion", error)
        }
        database.withTransaction {
            dao.deleteOwnedHabits(owner)
            dao.deletePendingMutations(owner)
            dao.deleteSyncState(owner)
        }
    }

    private suspend fun claimLegacy(owner: String) {
        database.withTransaction {
            for (habit in dao.getOwnerlessHabits()) {
                val remoteId = UUID.randomUUID().toString()
                if (dao.claimOwnerlessHabit(habit.id, owner, remoteId) == 0) continue
                val claimed = habit.copy(ownerUserId = owner, remoteId = remoteId)
                queue(owner, HabitMutationJson.create(claimed))
                val revisions = dao.getScheduleRevisionsForHabit(habit.id)
                for (revision in revisions) {
                    queue(owner, HabitMutationJson.schedule(remoteId, revision.effectiveFrom,
                        revision.targetDays, revision.isEveryDay))
                }
                for (completion in dao.getAllCompletionsForHabit(habit.id)) if (completion.completed) {
                    val date = completion.date
                    val days = revisions.lastOrNull { !it.effectiveFrom.isAfter(date) }?.targetDays ?: habit.targetDays
                    if (!date.isAfter(LocalDate.now()) && !date.isBefore(habit.scheduledSince) &&
                        (!habit.isArchived || habit.archivedAt == null || !date.isAfter(habit.archivedAt)) &&
                        date.dayOfWeek in days) {
                        queue(owner, HabitMutationJson.completion(remoteId, date, true))
                    }
                }
                if (habit.isArchived) queue(owner, HabitMutationJson.archive(remoteId, habit.archivedAt ?: LocalDate.now()))
            }
        }
    }

    private suspend fun queue(owner: String, json: com.google.gson.JsonObject) {
        dao.enqueueMutation(HabitPendingMutationEntity(
            mutationId = json.get("mutationId").asString,
            ownerUserId = owner,
            payload = json.toString(),
            createdAt = System.currentTimeMillis()
        ))
    }

    private suspend fun currentOwner(): String? = if (dataStore.isLoggedIn.first()) {
        dataStore.userId.first()?.takeIf { it.isNotBlank() }
    } else null
}
