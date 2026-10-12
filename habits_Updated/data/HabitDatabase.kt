package com.safarparmar.app.feature.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class, HabitScheduleRevisionEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN is_archived INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE habits ADD COLUMN archived_at TEXT")
        db.execSQL("ALTER TABLE habits ADD COLUMN scheduled_since TEXT NOT NULL DEFAULT '2000-01-01'")
        db.execSQL("ALTER TABLE habits ADD COLUMN is_every_day INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * Adds real schedule versioning. Existing installs get one initial revision representing
 * the schedule already stored on the habit row. Future edits append/replace a revision
 * effective on the edit date rather than rewriting history.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `habit_schedule_revisions` (
                `revisionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `habitId` INTEGER NOT NULL,
                `effective_from` TEXT NOT NULL,
                `target_days` TEXT NOT NULL,
                `is_every_day` INTEGER NOT NULL,
                FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_schedule_revisions_habitId_effective_from` ON `habit_schedule_revisions` (`habitId`, `effective_from`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_schedule_revisions_effective_from` ON `habit_schedule_revisions` (`effective_from`)")
        db.execSQL(
            """
            INSERT OR IGNORE INTO habit_schedule_revisions(habitId, effective_from, target_days, is_every_day)
            SELECT id, scheduled_since, target_days, is_every_day FROM habits
            """.trimIndent()
        )
    }
}
