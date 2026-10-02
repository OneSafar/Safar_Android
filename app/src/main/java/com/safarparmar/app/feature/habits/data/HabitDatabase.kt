package com.safarparmar.app.feature.habits.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class, HabitScheduleRevisionEntity::class, HabitPendingMutationEntity::class, HabitSyncStateEntity::class],
    version = 6,
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

/** Upgrade older local-only installs without losing any habit or completion rows. */
val MIGRATION_4_6 = object : Migration(4, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE habits ADD COLUMN remote_id TEXT")
        db.execSQL("ALTER TABLE habits ADD COLUMN owner_user_id TEXT")
        db.execSQL("ALTER TABLE habits ADD COLUMN reminder_time TEXT")
        db.execSQL("CREATE UNIQUE INDEX index_habits_owner_user_id_remote_id ON habits(owner_user_id, remote_id)")
        db.execSQL("CREATE INDEX index_habits_owner_user_id ON habits(owner_user_id)")
        db.execSQL("""CREATE TABLE habit_pending_mutations (
            sequence INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            mutation_id TEXT NOT NULL, owner_user_id TEXT NOT NULL,
            payload TEXT NOT NULL, created_at INTEGER NOT NULL
        )""")
        db.execSQL("CREATE UNIQUE INDEX index_habit_pending_mutations_mutation_id ON habit_pending_mutations(mutation_id)")
        db.execSQL("CREATE INDEX index_habit_pending_mutations_owner_user_id ON habit_pending_mutations(owner_user_id)")
        db.execSQL("""CREATE TABLE habit_sync_state (
            owner_user_id TEXT NOT NULL PRIMARY KEY, version INTEGER NOT NULL,
            last_synced_at INTEGER, last_error TEXT
        )""")
    }
}

/** Version five already has this schema. Keep its metadata and queued changes intact. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
}
