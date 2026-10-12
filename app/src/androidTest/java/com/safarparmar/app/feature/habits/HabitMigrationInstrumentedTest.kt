package com.safarparmar.app.feature.habits

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.feature.habits.data.HabitDatabase
import com.safarparmar.app.feature.habits.data.MIGRATION_4_5
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitMigrationInstrumentedTest {
    @Test fun existingHabitAndHistorySurviveVersionFiveMigration() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "habit-migration-fixture.db"
        context.deleteDatabase(name)
        val raw = context.openOrCreateDatabase(name, 0, null)
        raw.execSQL("""CREATE TABLE habits (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            target_days TEXT NOT NULL,
            isActive INTEGER NOT NULL,
            `order` INTEGER NOT NULL,
            is_archived INTEGER NOT NULL,
            archived_at TEXT,
            scheduled_since TEXT NOT NULL,
            is_every_day INTEGER NOT NULL
        )""".trimIndent())
        raw.execSQL("""CREATE TABLE habit_completions (
            habitId INTEGER NOT NULL,
            date TEXT NOT NULL,
            completed INTEGER NOT NULL,
            PRIMARY KEY(habitId, date),
            FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        raw.execSQL("CREATE INDEX index_habit_completions_habitId ON habit_completions(habitId)")
        raw.execSQL("CREATE INDEX index_habit_completions_date ON habit_completions(date)")
        raw.execSQL("""CREATE TABLE habit_schedule_revisions (
            revisionId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            habitId INTEGER NOT NULL,
            effective_from TEXT NOT NULL,
            target_days TEXT NOT NULL,
            is_every_day INTEGER NOT NULL,
            FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent())
        raw.execSQL("CREATE UNIQUE INDEX index_habit_schedule_revisions_habitId_effective_from ON habit_schedule_revisions(habitId, effective_from)")
        raw.execSQL("CREATE INDEX index_habit_schedule_revisions_effective_from ON habit_schedule_revisions(effective_from)")
        raw.execSQL("INSERT INTO habits(id,name,target_days,isActive,`order`,is_archived,archived_at,scheduled_since,is_every_day) VALUES (7,'Read','1,3,5',1,0,0,NULL,'2026-09-21',0)")
        raw.execSQL("INSERT INTO habit_completions(habitId,date,completed) VALUES (7,'2026-09-21',1)")
        raw.execSQL("INSERT INTO habit_schedule_revisions(habitId,effective_from,target_days,is_every_day) VALUES (7,'2026-09-21','1,3,5',0)")
        raw.version = 4
        raw.close()

        val db = Room.databaseBuilder(context, HabitDatabase::class.java, name)
            .addMigrations(MIGRATION_4_5).build()
        try {
            val dao = db.habitDao()
            val habits = dao.getOwnerlessHabits()
            assertEquals(1, habits.size)
            assertEquals("Read", habits.single().name)
            assertNull(habits.single().ownerUserId)
            assertNull(habits.single().remoteId)
            assertEquals(1, dao.getAllCompletionsForHabit(7).size)
            assertEquals(1, dao.getScheduleRevisionsForHabit(7).size)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
