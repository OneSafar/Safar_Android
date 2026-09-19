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

    @Query("SELECT * FROM habit_schedule_revisions WHERE habitId = :habitId ORDER BY effective_from ASC")
    suspend fun getScheduleRevisionsForHabit(habitId: Long): List<HabitScheduleRevisionEntity>
}
