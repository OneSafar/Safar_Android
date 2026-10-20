package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Exercises production preference reads and writes against committed in-memory storage. */
class ShieldPrefsStudyBreakTest {
    private val context = mockk<Context>()
    private val storage: SharedPreferences = MemoryPreferences()
    private val youtube = "com.google.android.youtube"
    private val other = "com.instagram.android"
    private val deadline = System.currentTimeMillis() + 600_000L
    private val shield = FocusShieldRepository.ShieldPrefs

    @Before fun setUp() {
        every { context.getSharedPreferences("focus_shield_session", Context.MODE_PRIVATE) } returns storage
        mockkObject(QuickUnlockNotification)
        every { QuickUnlockNotification.show(any(), any(), any(), any(), any()) } just Runs
        every { QuickUnlockNotification.cancel(any()) } just Runs
    }

    @After fun tearDown() {
        unmockkObject(QuickUnlockNotification)
    }

    private fun startStudyBreak() {
        shield.applyEmergencyUnlock(context, deadline, minutes = 10, packageName = youtube,
            origin = shield.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY)
    }

    private fun assertStudyBreakPreserved() {
        assertEquals(deadline, shield.getGraceUntilMs(context))
        assertEquals(shield.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY, shield.quickUnlockOrigin(context))
        assertTrue(shield.isInGracePeriodForPackage(context, youtube))
        assertFalse(shield.isInGracePeriodForPackage(context, other))
        verify(exactly = 0) { QuickUnlockNotification.cancel(context) }
    }

    @Test fun `starting a strict Kavach session keeps Study break and clears app-only unlock`() {
        shield.applyOneTimeUnlock(context, other)
        startStudyBreak()

        shield.write(context, active = true, packages = setOf(other), strict = true, resetUnlocks = true)

        assertTrue(shield.isActive(context))
        assertTrue(shield.isStrict(context))
        assertEquals(setOf(other), shield.getPackages(context))
        assertFalse(shield.isOneTimeUnlockedPackage(context, other))
        assertStudyBreakPreserved()
    }

    @Test fun `stopping Kavach keeps Study break and its notification`() {
        shield.write(context, active = true, packages = setOf(other), resetUnlocks = false)
        startStudyBreak()

        shield.clear(context)

        assertFalse(shield.isActive(context))
        assertTrue(shield.getPackages(context).isEmpty())
        assertStudyBreakPreserved()
    }

    @Test fun `scoped Kavach clear preserves Study break`() {
        startStudyBreak()

        shield.clearKavachQuickUnlock(context)

        assertStudyBreakPreserved()
    }

    @Test fun `explicit Study break ending still clears grace and cancels notification`() {
        startStudyBreak()

        shield.clearQuickUnlock(context)

        assertEquals(0L, shield.getGraceUntilMs(context))
        assertNull(shield.quickUnlockOrigin(context))
        assertFalse(shield.isInGracePeriodForPackage(context, youtube))
        verify(exactly = 1) { QuickUnlockNotification.cancel(context) }
    }

    @Test fun `Kavach session reset clears Kavach and legacy grace`() {
        listOf(false, true).forEach { legacy ->
            shield.applyEmergencyUnlock(context, deadline, minutes = 10, packageName = other)
            if (legacy) storage.edit().remove("grace_origin").apply()

            shield.write(context, active = true, packages = setOf(other), resetUnlocks = true)

            assertEquals(0L, shield.getGraceUntilMs(context))
            assertFalse(shield.isInGracePeriodForPackage(context, other))
            assertEquals(0, shield.consumeQuickUnlockJustExpired(context))
        }
        verify(exactly = 2) { QuickUnlockNotification.cancel(context) }
    }

    @Test fun `scoped Kavach clear removes app grace without changing blocking settings`() {
        shield.write(context, active = true, packages = setOf(other), strict = true, resetUnlocks = false)
        shield.applyEmergencyUnlock(context, deadline, minutes = 10, packageName = other)

        shield.clearKavachQuickUnlock(context)

        assertTrue(shield.isActive(context))
        assertTrue(shield.isStrict(context))
        assertEquals(setOf(other), shield.getPackages(context))
        assertEquals(0L, shield.getGraceUntilMs(context))
        verify(exactly = 1) { QuickUnlockNotification.cancel(context) }
    }

    @Test fun `stopping Kavach clears legacy grace and its expiration marker`() {
        shield.applyEmergencyUnlock(context, deadline, minutes = 10)
        storage.edit().remove("grace_origin").apply()

        shield.clear(context)

        assertFalse(shield.isActive(context))
        assertEquals(0L, shield.getGraceUntilMs(context))
        assertEquals(0, shield.consumeQuickUnlockJustExpired(context))
        verify(exactly = 1) { QuickUnlockNotification.cancel(context) }
    }

    @Test fun `expired Study break metadata survives a Kavach reset until it is consumed`() {
        shield.applyEmergencyUnlock(context, System.currentTimeMillis() - 1L, minutes = 10,
            packageName = youtube, origin = shield.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY)

        shield.write(context, active = true, packages = setOf(other), resetUnlocks = true)

        assertEquals(10, shield.consumeQuickUnlockJustExpired(context))
        assertEquals(0, shield.consumeQuickUnlockJustExpired(context))
        verify(exactly = 0) { QuickUnlockNotification.cancel(context) }
    }

    private class MemoryPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun edit(): SharedPreferences.Editor = Editor()

        private inner class Editor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private var clearFirst = false
            private fun put(key: String?, value: Any?): SharedPreferences.Editor = apply {
                pending[requireNotNull(key)] = value
            }
            override fun putString(key: String?, value: String?) = put(key, value)
            override fun putStringSet(key: String?, values: MutableSet<String>?) = put(key, values?.toSet())
            override fun putInt(key: String?, value: Int) = put(key, value)
            override fun putLong(key: String?, value: Long) = put(key, value)
            override fun putFloat(key: String?, value: Float) = put(key, value)
            override fun putBoolean(key: String?, value: Boolean) = put(key, value)
            override fun remove(key: String?) = put(key, null)
            override fun clear(): SharedPreferences.Editor = apply { clearFirst = true }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearFirst) values.clear()
                pending.forEach { (key, value) ->
                    if (value == null) values.remove(key) else values[key] = value
                }
            }
        }
    }
}
