package com.safarparmar.app.feature.habits.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate

/** A single trackable habit. */
@Entity(tableName = "habits")
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
    val isEveryDay: Boolean = true
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
