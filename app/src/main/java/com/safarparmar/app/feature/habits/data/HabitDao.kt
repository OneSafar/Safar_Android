package com.safarparmar.app.feature.habits.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 AND is_archived = 0 ORDER BY `order` ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query(
        """
        SELECT * FROM habits
        WHERE isActive = 1
          AND scheduled_since <= :endDate
          AND (
              is_archived = 0
              OR archived_at IS NULL
              OR archived_at >= :startDate
          )
        ORDER BY `order` ASC
        """
    )
    fun observeHabitsForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitEntity>>

    @Query(
        """
        SELECT * FROM habits
        WHERE isActive = 1
          AND scheduled_since <= :endDate
          AND (
              is_archived = 0
              OR archived_at IS NULL
              OR archived_at >= :startDate
          )
        ORDER BY `order` ASC
        """
    )
    suspend fun getHabitsForRange(startDate: LocalDate, endDate: LocalDate): List<HabitEntity>

    @Query("SELECT * FROM habits ORDER BY `order` ASC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): HabitEntity?

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("UPDATE habits SET is_archived = 1, archived_at = :archivedAt WHERE id = :habitId")
    suspend fun archiveHabit(habitId: Long, archivedAt: LocalDate)

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun observeCompletionsForDate(date: LocalDate): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :start AND :end")
    fun observeCompletionsBetween(start: LocalDate, end: LocalDate): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE date BETWEEN :start AND :end")
    suspend fun getCompletionsBetween(start: LocalDate, end: LocalDate): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND completed = 1 ORDER BY date DESC")
    suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun clearCompletion(habitId: Long, date: LocalDate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScheduleRevision(revision: HabitScheduleRevisionEntity)

    @Query(
        "SELECT r.* FROM habit_schedule_revisions r " +
            "INNER JOIN habits h ON h.id = r.habitId " +
            "WHERE h.isActive = 1 AND h.is_archived = 0 AND r.effective_from <= :end " +
            "ORDER BY r.habitId ASC, r.effective_from ASC"
    )
    fun observeActiveScheduleRevisionsThrough(end: LocalDate): Flow<List<HabitScheduleRevisionEntity>>

    @Query(
        """
        SELECT r.* FROM habit_schedule_revisions r
        INNER JOIN habits h ON h.id = r.habitId
        WHERE h.isActive = 1
          AND h.scheduled_since <= :endDate
          AND (h.is_archived = 0 OR h.archived_at IS NULL OR h.archived_at >= :startDate)
          AND r.effective_from <= :endDate
        ORDER BY r.habitId ASC, r.effective_from ASC
        """
    )
    fun observeScheduleRevisionsForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitScheduleRevisionEntity>>

    @Query(
        """
        SELECT r.* FROM habit_schedule_revisions r
        INNER JOIN habits h ON h.id = r.habitId
        WHERE h.isActive = 1
          AND h.scheduled_since <= :endDate
          AND (h.is_archived = 0 OR h.archived_at IS NULL OR h.archived_at >= :startDate)
          AND r.effective_from <= :endDate
        ORDER BY r.habitId ASC, r.effective_from ASC
        """
    )
    suspend fun getScheduleRevisionsForRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitScheduleRevisionEntity>

    @Query("SELECT * FROM habit_schedule_revisions WHERE habitId = :habitId ORDER BY effective_from ASC")
    suspend fun getScheduleRevisionsForHabit(habitId: Long): List<HabitScheduleRevisionEntity>

    @Query("SELECT * FROM habits WHERE isActive = 1 AND is_archived = 0 AND owner_user_id = :owner ORDER BY `order` ASC")
    fun observeActiveHabitsForOwner(owner: String): Flow<List<HabitEntity>>

    @Query("""
        SELECT c.* FROM habit_completions c
        INNER JOIN habits h ON h.id = c.habitId
        WHERE c.date = :date AND h.owner_user_id = :owner
    """)
    fun observeCompletionsForDateForOwner(owner: String, date: LocalDate): Flow<List<HabitCompletionEntity>>

    @Query(
        "SELECT r.* FROM habit_schedule_revisions r " +
            "INNER JOIN habits h ON h.id = r.habitId " +
            "WHERE h.isActive = 1 AND h.is_archived = 0 AND h.owner_user_id = :owner AND r.effective_from <= :end " +
            "ORDER BY r.habitId ASC, r.effective_from ASC"
    )
    fun observeActiveScheduleRevisionsThroughForOwner(owner: String, end: LocalDate): Flow<List<HabitScheduleRevisionEntity>>

    @Query("SELECT * FROM habits WHERE owner_user_id = :owner ORDER BY `order` ASC")
    suspend fun getOwnedHabits(owner: String): List<HabitEntity>

    @Query("SELECT COUNT(*) FROM habit_pending_mutations WHERE owner_user_id = :owner")
    suspend fun pendingMutationCount(owner: String): Int

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
    suspend fun getAllCompletionsForHabit(habitId: Long): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueMutation(mutation: HabitPendingMutationEntity)

    @Query("SELECT * FROM habit_pending_mutations WHERE owner_user_id = :owner ORDER BY sequence ASC")
    suspend fun getPendingMutations(owner: String): List<HabitPendingMutationEntity>

    @Query("DELETE FROM habit_pending_mutations WHERE mutation_id IN (:mutationIds)")
    suspend fun deletePendingMutations(mutationIds: List<String>)

    @Query("SELECT * FROM habits WHERE owner_user_id = :owner AND remote_id = :remoteId LIMIT 1")
    suspend fun getHabitByRemoteId(owner: String, remoteId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: HabitSyncStateEntity)

    @Query("SELECT * FROM habit_sync_state WHERE owner_user_id = :owner LIMIT 1")
    suspend fun getSyncState(owner: String): HabitSyncStateEntity?

    @Query("DELETE FROM habit_schedule_revisions WHERE habitId = :habitId")
    suspend fun deleteScheduleRevisionsForHabit(habitId: Long)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteCompletionsForHabit(habitId: Long)

    @Query("""
        SELECT * FROM habits
        WHERE isActive = 1
          AND owner_user_id = :owner
          AND scheduled_since <= :endDate
          AND (
              is_archived = 0
              OR archived_at IS NULL
              OR archived_at >= :startDate
          )
        ORDER BY `order` ASC
    """)
    fun observeHabitsForRangeForOwner(owner: String, startDate: LocalDate, endDate: LocalDate): Flow<List<HabitEntity>>

    @Query("""
        SELECT c.* FROM habit_completions c
        INNER JOIN habits h ON h.id = c.habitId
        WHERE c.date BETWEEN :start AND :end AND h.owner_user_id = :owner
    """)
    fun observeCompletionsBetweenForOwner(owner: String, start: LocalDate, end: LocalDate): Flow<List<HabitCompletionEntity>>

    @Query("""
        SELECT r.* FROM habit_schedule_revisions r
        INNER JOIN habits h ON h.id = r.habitId
        WHERE h.isActive = 1
          AND h.owner_user_id = :owner
          AND h.scheduled_since <= :endDate
          AND (h.is_archived = 0 OR h.archived_at IS NULL OR h.archived_at >= :startDate)
          AND r.effective_from <= :endDate
        ORDER BY r.habitId ASC, r.effective_from ASC
    """)
    fun observeScheduleRevisionsForRangeForOwner(
        owner: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HabitScheduleRevisionEntity>>
}
