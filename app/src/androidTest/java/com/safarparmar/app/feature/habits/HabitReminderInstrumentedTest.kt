package com.safarparmar.app.feature.habits

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.HabitRemoteCompletion
import com.safarparmar.app.feature.habits.data.HabitRemoteRecord
import com.safarparmar.app.feature.habits.reminders.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HabitReminderInstrumentedTest {
    @Test fun reminderDeliveryChecksCompletionAndAccountAndSchedulesNextAlarm() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val owner = UUID.randomUUID().toString()
        val remoteId = UUID.randomUUID().toString()
        val isolated = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("$owner-$name", mode)
        }
        val accounts = SafarDataStore(context)
        val previousOwner = accounts.userId.first()
        val previousLogin = accounts.isLoggedIn.first()
        val previousNotifications = accounts.notificationsEnabled.first()
        val cache = HabitReminderStore(isolated)
        val manager = context.getSystemService(NotificationManager::class.java)
        val scheduler = HabitReminderScheduler(context, cache, accounts)
        val productionCache = HabitReminderStore(context)
        val previousProjection = productionCache.read()
        val tag = HabitReminderScheduler.tag(owner, remoteId)
        try {
            accounts.setUserId(owner)
            accounts.setLoggedIn(true)
            accounts.setNotificationsEnabled(true)
            val today = LocalDate.now()
            val habit = HabitRemoteRecord(id = remoteId, name = "Read", scheduledSince = today.toString(),
                isEveryDay = true, reminderTime = LocalTime.now().withSecond(0).withNano(0).toString())
            scheduler.replace(owner, listOf(habit))
            // A fresh instance must recover the confirmed snapshot and delivery markers from disk.
            val restoredCache = HabitReminderStore(isolated)
            val restored = HabitReminderScheduler(context, restoredCache, accounts)
            restored.deliver(owner, remoteId, today.toString(), restoredCache.token(habit))
            assertTrue(manager.activeNotifications.any { it.tag == tag })
            assertNotNull(PendingIntent.getBroadcast(context, 0, HabitReminderScheduler.alarmIntent(context, owner, remoteId),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
            manager.cancel(tag, 0)
            restored.deliver(owner, remoteId, today.toString(), restoredCache.token(habit))
            assertFalse(manager.activeNotifications.any { it.tag == tag }) // once per date, including after process recreation

            cache.clear()
            val completed = habit.copy(completions = listOf(HabitRemoteCompletion(today.toString(), true)))
            scheduler.replace(owner, listOf(completed))
            restored.deliver(owner, remoteId, today.toString(), restoredCache.token(completed))
            assertFalse(manager.activeNotifications.any { it.tag == tag })
            scheduler.replace(owner, listOf(habit))
            accounts.setUserId(UUID.randomUUID().toString())
            restored.deliver(owner, remoteId, today.toString(), restoredCache.token(habit))
            assertFalse(manager.activeNotifications.any { it.tag == tag })
            assertNull(PendingIntent.getBroadcast(context, 0, HabitReminderScheduler.alarmIntent(context, owner, remoteId),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        } finally {
            scheduler.cancelForHabit(owner, remoteId)
            cache.clear()
            accounts.setUserId(previousOwner)
            accounts.setLoggedIn(previousLogin)
            accounts.setNotificationsEnabled(previousNotifications)
            if (previousProjection != null && previousOwner == previousProjection.owner && previousLogin) {
                HabitReminderScheduler(context, productionCache, accounts).replace(previousProjection.owner, previousProjection.habits)
            }
        }
    }
}
