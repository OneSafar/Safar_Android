package com.safarparmar.app.feature.habits.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

/** A single trackable habit. */
@Entity(tableName = "habits", indices = [Index(value = ["owner_user_id", "remote_id"], unique = true), Index("owner_user_id")])
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "target_days")
    val targetDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val isActive: Boolean = true,
    val order: Int = 0,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "archived_at")
    val archivedAt: LocalDate? = null,
    /** Start date of the current schedule. Retained for backwards compatibility. */
    @ColumnInfo(name = "scheduled_since")
    val scheduledSince: LocalDate = LocalDate.of(2000, 1, 1),
    @ColumnInfo(name = "is_every_day")
    val isEveryDay: Boolean = true,
    // Retain version-five metadata when opening a database written by a sync-enabled build.
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String? = null,
    @ColumnInfo(name = "reminder_time") val reminderTime: String? = null
)

/** One completion record for a habit on a given calendar date. */
@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("date")]
)
data class HabitCompletionEntity(
    val habitId: Long,
    val date: LocalDate,
    val completed: Boolean
)

/**
 * Immutable schedule history for a habit.
 *
 * This prevents an edit made today from rewriting what the app believes was scheduled
 * last month. The row with the latest [effectiveFrom] on/before a date owns that date.
 */
@Entity(
    tableName = "habit_schedule_revisions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "effective_from"], unique = true),
        Index("effective_from")
    ]
)
data class HabitScheduleRevisionEntity(
    @PrimaryKey(autoGenerate = true)
    val revisionId: Long = 0,
    val habitId: Long,
    @ColumnInfo(name = "effective_from")
    val effectiveFrom: LocalDate,
    @ColumnInfo(name = "target_days")
    val targetDays: Set<DayOfWeek>,
    @ColumnInfo(name = "is_every_day")
    val isEveryDay: Boolean
)

/** Preserve queued sync state even in builds where sync is not active. */
@Entity(tableName = "habit_pending_mutations", indices = [Index(value = ["mutation_id"], unique = true), Index("owner_user_id")])
data class HabitPendingMutationEntity(
    @PrimaryKey(autoGenerate = true) val sequence: Long = 0,
    @ColumnInfo(name = "mutation_id") val mutationId: String,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(tableName = "habit_sync_state")
data class HabitSyncStateEntity(
    @PrimaryKey @ColumnInfo(name = "owner_user_id") val ownerUserId: String,
    val version: Long,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long? = null,
    @ColumnInfo(name = "last_error") val lastError: String? = null
)
