package com.safarparmar.app.feature.habits

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.feature.habits.data.*
import org.junit.Assert.assertEquals
import org.junit.Test

class HabitDatabaseRecoveryTest {
    @Test fun versionFiveDataSurvivesRecovery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "habit-recovery-test.db"
        context.deleteDatabase(name)
        val raw = context.openOrCreateDatabase(name, 0, null)
        try {
            raw.execSQL("""CREATE TABLE `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `target_days` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `order` INTEGER NOT NULL, `is_archived` INTEGER NOT NULL, `archived_at` TEXT, `scheduled_since` TEXT NOT NULL, `is_every_day` INTEGER NOT NULL, `remote_id` TEXT, `owner_user_id` TEXT, `reminder_time` TEXT)""")
            raw.execSQL("""CREATE TABLE `habit_completions` (`habitId` INTEGER NOT NULL, `date` TEXT NOT NULL, `completed` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
            raw.execSQL("""CREATE TABLE `habit_schedule_revisions` (`revisionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `effective_from` TEXT NOT NULL, `target_days` TEXT NOT NULL, `is_every_day` INTEGER NOT NULL, FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )""")
            raw.execSQL("""CREATE TABLE `habit_pending_mutations` (`sequence` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mutation_id` TEXT NOT NULL, `owner_user_id` TEXT NOT NULL, `payload` TEXT NOT NULL, `created_at` INTEGER NOT NULL)""")
            raw.execSQL("""CREATE TABLE `habit_sync_state` (`owner_user_id` TEXT NOT NULL, `version` INTEGER NOT NULL, `last_synced_at` INTEGER, `last_error` TEXT, PRIMARY KEY(`owner_user_id`))""")
            raw.execSQL("""CREATE UNIQUE INDEX `index_habits_owner_user_id_remote_id` ON `habits` (`owner_user_id`, `remote_id`)""")
            raw.execSQL("""CREATE INDEX `index_habits_owner_user_id` ON `habits` (`owner_user_id`)""")
            raw.execSQL("""CREATE INDEX `index_habit_completions_habitId` ON `habit_completions` (`habitId`)""")
            raw.execSQL("""CREATE INDEX `index_habit_completions_date` ON `habit_completions` (`date`)""")
            raw.execSQL("""CREATE UNIQUE INDEX `index_habit_schedule_revisions_habitId_effective_from` ON `habit_schedule_revisions` (`habitId`, `effective_from`)""")
            raw.execSQL("""CREATE INDEX `index_habit_schedule_revisions_effective_from` ON `habit_schedule_revisions` (`effective_from`)""")
            raw.execSQL("""CREATE UNIQUE INDEX `index_habit_pending_mutations_mutation_id` ON `habit_pending_mutations` (`mutation_id`)""")
            raw.execSQL("""CREATE INDEX `index_habit_pending_mutations_owner_user_id` ON `habit_pending_mutations` (`owner_user_id`)""")
            raw.execSQL("INSERT INTO habits(id,name,target_days,isActive,`order`,is_archived,scheduled_since,is_every_day,remote_id,owner_user_id,reminder_time) VALUES(7,'Read','1,3,5',1,0,0,'2026-09-21',0,'remote-7','owner-1','08:00')")
            raw.execSQL("INSERT INTO habit_completions VALUES(7,'2026-09-21',1)")
            raw.execSQL("INSERT INTO habit_schedule_revisions VALUES(1,7,'2026-09-21','1,3,5',0)")
            raw.execSQL("INSERT INTO habit_pending_mutations VALUES(1,'mutation-1','owner-1','{}',123)")
            raw.execSQL("INSERT INTO habit_sync_state VALUES('owner-1',9,123,NULL)")
            raw.version = 5
        } finally { raw.close() }
        val db = Room.databaseBuilder(context, HabitDatabase::class.java, name)
            .addMigrations(MIGRATION_5_6).build()
        try {
            val sqlite = db.openHelper.writableDatabase
            assertEquals(6, sqlite.version)
            sqlite.query("SELECT name,remote_id,owner_user_id,reminder_time FROM habits WHERE id=7").use {
                check(it.moveToFirst())
                assertEquals("Read", it.getString(0))
                assertEquals("remote-7", it.getString(1))
                assertEquals("owner-1", it.getString(2))
                assertEquals("08:00", it.getString(3))
            }
            listOf("habit_completions", "habit_schedule_revisions", "habit_pending_mutations", "habit_sync_state").forEach { table ->
                sqlite.query("SELECT COUNT(*) FROM $table").use { check(it.moveToFirst()); assertEquals(1, it.getInt(0)) }
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
