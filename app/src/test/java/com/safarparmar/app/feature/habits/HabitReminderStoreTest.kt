package com.safarparmar.app.feature.habits

import android.content.Context
import android.content.SharedPreferences
import com.safarparmar.app.feature.habits.data.*
import com.safarparmar.app.feature.habits.reminders.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test

class HabitReminderStoreTest {
    private class Disk {
        val values = mutableMapOf<String, String?>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val prefs = mockk<SharedPreferences>()
        val context = mockk<Context>()
        init {
            every { context.getSharedPreferences(any(), any()) } returns prefs
            every { prefs.getString(any(), any()) } answers { values[firstArg()] ?: secondArg() }
            every { prefs.edit() } returns editor
            every { editor.putString(any(), any()) } answers { values[firstArg()] = secondArg(); editor }
            every { editor.clear() } answers { values.clear(); editor }
            every { editor.commit() } returns true
        }
    }
    private val habit = HabitRemoteRecord(id = "read", name = "Read हिन्दी", reminderTime = "08:30", targetDays = listOf(1, 3),
        revisions = listOf(HabitRemoteRevision("2026-10-19", listOf(5), false)),
        completions = listOf(HabitRemoteCompletion("2026-10-19", true)))

    @Test fun processRecreationRetainsConfirmedSnapshotAndDeliveredDate() {
        val disk = Disk()
        val original = HabitReminderStore(disk.context)
        val snapshot = HabitReminderSnapshot("alice", listOf(habit))
        original.replace(snapshot)
        original.markDelivered("alice", "read", "2026-10-19")
        val restored = HabitReminderStore(disk.context)
        assertEquals(snapshot, restored.read())
        assertEquals("2026-10-19", restored.delivered("alice", "read"))
        assertNull(restored.delivered("bob", "read"))
        assertEquals(original.token(habit), restored.token(restored.read()!!.habits.single()))
    }
    @Test fun staleTokensChangeForEditsCompletionAndRecurrence() {
        val store = HabitReminderStore(Disk().context)
        val token = store.token(habit)
        listOf(habit.copy(reminderTime = "09:00"), habit.copy(completions = emptyList()),
            habit.copy(isActive = false), habit.copy(revisions = emptyList()), habit.copy(deleted = true)).forEach {
            assertNotEquals(token, store.token(it))
        }
    }
    @Test fun clearingAccountErasesProjectionAndDeliveryMarkers() {
        val store = HabitReminderStore(Disk().context)
        store.replace(HabitReminderSnapshot("alice", listOf(habit)))
        store.markDelivered("alice", "read", "2026-10-19")
        store.clear()
        assertNull(store.read())
        assertNull(store.delivered("alice", "read"))
    }
    @Test fun corruptCacheCannotCreatePhantomReminders() {
        val disk = Disk()
        listOf("invalid-json", "{}", "{\"owner\":\"alice\"}").forEach {
            disk.values["snapshot"] = it
            assertNull(HabitReminderStore(disk.context).read())
        }
    }
    @Test fun failedDiskCommitIsNotReportedAsSuccessful() {
        val disk = Disk()
        every { disk.editor.commit() } returns false
        assertThrows(IllegalStateException::class.java) {
            HabitReminderStore(disk.context).replace(HabitReminderSnapshot("alice", listOf(habit)))
        }
    }
}
