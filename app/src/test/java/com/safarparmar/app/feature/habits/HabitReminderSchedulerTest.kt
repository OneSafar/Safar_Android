package com.safarparmar.app.feature.habits

import android.app.*
import android.content.Context
import android.content.Intent
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.feature.habits.data.*
import com.safarparmar.app.feature.habits.reminders.*
import com.safarparmar.app.notifications.*
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Exercises real orchestration with only Android system services replaced. No emulator. */
class HabitReminderSchedulerTest {
    private val owner = MutableStateFlow<String?>("alice")
    private val loggedIn = MutableStateFlow(true)
    private val enabled = MutableStateFlow(true)
    private val accounts = mockk<SafarDataStore> {
        every { userId } returns owner
        every { isLoggedIn } returns loggedIn
        every { notificationsEnabled } returns enabled
    }
    private val alarms = mockk<AlarmManager>(relaxed = true)
    private val notifications = mockk<NotificationManager>(relaxed = true)
    private val context = mockk<Context>(relaxed = true) {
        every { getSystemService(AlarmManager::class.java) } returns alarms
        every { getSystemService(NotificationManager::class.java) } returns notifications
    }
    private val cache = mockk<HabitReminderStore>(relaxed = true)
    private val pending = mockk<PendingIntent>(relaxed = true)
    private val intent = mockk<Intent>(relaxed = true)
    private val habit = HabitRemoteRecord("read", name = "Read", isEveryDay = true, reminderTime = "00:00")
    private var snapshot: HabitReminderSnapshot? = HabitReminderSnapshot("alice", listOf(habit))
    private var delivered: String? = null
    private val scheduler = HabitReminderScheduler(context, cache, accounts)

    @Before fun setUp() {
        mockkStatic(PendingIntent::class)
        mockkObject(HabitReminderScheduler.Companion, SafarNotificationChannels)
        mockkConstructor(SafarNotificationManager::class)
        every { HabitReminderScheduler.alarmIntent(any(), any(), any()) } returns intent
        every { PendingIntent.getBroadcast(any(), any(), any(), any()) } returns pending
        every { SafarNotificationChannels.createAll(any()) } just Runs
        every { anyConstructed<SafarNotificationManager>().evaluateNotificationAvailability(any()) } returns
            NotificationAvailability(true, NotificationAvailabilityReason.allowed)
        every { anyConstructed<SafarNotificationManager>().buildNotification(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk()
        every { cache.read() } answers { snapshot }
        every { cache.replace(any()) } answers { snapshot = firstArg() }
        every { cache.clear() } answers { snapshot = null; delivered = null }
        every { cache.token(any()) } answers { firstArg<HabitRemoteRecord>().reminderTime ?: "disabled" }
        every { cache.delivered(any(), any()) } answers { delivered }
        every { cache.markDelivered(any(), any(), any()) } answers { delivered = thirdArg() }
    }
    @After fun tearDown() { unmockkAll() }

    @Test fun deliveryPostsOnceAndRequeuesNextOccurrence() = runTest {
        val date = LocalDate.now().toString()
        scheduler.deliver("alice", "read", date, "00:00")
        scheduler.deliver("alice", "read", date, "00:00")
        verify(exactly = 1) { notifications.notify("habit:alice:read", 0, any()) }
        assertEquals(date, delivered)
        verify(exactly = 2) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
    }
    @Test fun completedHabitSuppressesNotificationAndStillSchedulesNextDay() = runTest {
        snapshot = snapshot!!.copy(habits = listOf(habit.copy(completions = listOf(HabitRemoteCompletion(LocalDate.now().toString(), true)))))
        scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
        verify(exactly = 0) { notifications.notify(any<String>(), any(), any()) }
        verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
    }
    @Test fun staleAlarmAfterTimeEditCannotNotify() = runTest {
        snapshot = snapshot!!.copy(habits = listOf(habit.copy(reminderTime = "01:00")))
        scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
        verify(exactly = 0) { notifications.notify(any<String>(), any(), any()) }
        verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
    }
    @Test fun differentAccountCancelsOldAlarmAndNotification() = runTest {
        owner.value = "bob"
        scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
        verify { alarms.cancel(pending) }
        verify { notifications.cancel("habit:alice:read", 0) }
        verify(exactly = 0) { notifications.notify(any<String>(), any(), any()) }
    }
    @Test fun deletingHabitCancelsPendingAlarmAndPostedNotification() = runTest {
        scheduler.replace("alice", emptyList())
        assertTrue(snapshot!!.habits.isEmpty())
        verify { alarms.cancel(pending) }
        verify { notifications.cancel("habit:alice:read", 0) }
    }
    @Test fun globalOptOutCancelsAlarmsAndRetainsCacheForReenable() = runTest {
        enabled.value = false
        scheduler.reconcile()
        assertNotNull(snapshot)
        verify { alarms.cancel(pending) }
        clearMocks(alarms)
        enabled.value = true
        scheduler.reconcile()
        verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
    }
    @Test fun logoutClearsReminderProjectionAndCancelsAlarms() = runTest {
        loggedIn.value = false
        scheduler.reconcile()
        assertNull(snapshot)
        verify { alarms.cancel(pending) }
    }
    @Test fun blockedNotificationChannelDoesNotPostButStillRequeues() = runTest {
        every { anyConstructed<SafarNotificationManager>().evaluateNotificationAvailability(any()) } returns
            NotificationAvailability(false, NotificationAvailabilityReason.channel_blocked)
        scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
        verify(exactly = 0) { notifications.notify(any<String>(), any(), any()) }
        verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
    }
    @Test fun permissionRevocationDuringPostingStillRequeues() = runTest {
        every { notifications.notify(any<String>(), any(), any()) } throws SecurityException("Permission revoked")
        scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
        assertNull(delivered)
        verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
    }
    @Test fun accountChangeDuringPostingRemovesNotificationImmediately() = runTest {
        every { notifications.notify(any<String>(), any(), any()) } answers { owner.value = "bob" }
        scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
        verify { notifications.cancel("habit:alice:read", 0) }
        verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
        verify { alarms.cancel(pending) }
    }

    @Test fun deliveryMarkerDiskFailureDoesNotLoseNextAlarm() = runTest {
        every { cache.markDelivered(any(), any(), any()) } throws IllegalStateException("Disk full")
        try {
            scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
            fail("Persistence failure must remain visible to the receiver")
        } catch (_: IllegalStateException) { }
        verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
    }

    @Test fun nextAlarmIsPersistedBeforeNotificationPostingStarts() = runTest {
        every { notifications.notify(any<String>(), any(), any()) } answers {
            verify { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, any(), pending) }
        }
        scheduler.deliver("alice", "read", LocalDate.now().toString(), "00:00")
        verify(exactly = 1) { notifications.notify("habit:alice:read", 0, any()) }
    }

}
