package com.safarparmar.app.feature.habits

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.HabitCompletionEntity
import com.safarparmar.app.feature.habits.data.HabitDatabase
import com.safarparmar.app.feature.habits.data.HabitEntity
import com.safarparmar.app.feature.habits.reminders.HabitReminderReceiver
import com.safarparmar.app.feature.habits.reminders.HabitReminderScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HabitReminderInstrumentedTest {
    @Test fun reminderDeliveryChecksCompletionAndAccountAndSchedulesNextAlarm() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = UUID.randomUUID().toString()
        val remoteId = UUID.randomUUID().toString()
        val store = SafarDataStore(context)
        store.setUserId(owner)
        store.setLoggedIn(true)
        val db = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java).build()
        val manager = context.getSystemService(NotificationManager::class.java)
        val scheduler = HabitReminderScheduler(context, db.habitDao(), store)
        try {
            val today = LocalDate.now()
            val habitId = db.habitDao().insertHabit(HabitEntity(name = "Read", ownerUserId = owner,
                remoteId = remoteId, scheduledSince = today,
                targetDays = DayOfWeek.entries.toSet(), reminderTime = "08:30"))
            scheduler.deliver(owner, remoteId, today.toString())
            assertTrue(manager.activeNotifications.any { it.tag == remoteId })

            val alarmIntent = Intent(context, HabitReminderReceiver::class.java).apply {
                data = Uri.Builder().scheme("safar-habit-alarm").authority("reminder")
                    .appendPath(owner).appendPath(remoteId).build()
            }
            assertNotNull(PendingIntent.getBroadcast(context, 0, alarmIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))

            manager.cancel(remoteId, 0)
            db.habitDao().upsertCompletion(HabitCompletionEntity(habitId, today, true))
            scheduler.deliver(owner, remoteId, today.toString())
            assertFalse(manager.activeNotifications.any { it.tag == remoteId })

            db.habitDao().clearCompletion(habitId, today)
            store.setUserId(UUID.randomUUID().toString())
            scheduler.deliver(owner, remoteId, today.toString())
            assertFalse(manager.activeNotifications.any { it.tag == remoteId })
        } finally {
            manager.cancel(remoteId, 0)
            scheduler.cancelForHabit(owner, remoteId)
            db.close()
            store.setLoggedIn(false)
            store.setUserId(null)
        }
    }
}
