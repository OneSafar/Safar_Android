package com.safarparmar.app.feature.toppersbatch

import android.content.Context
import android.content.SharedPreferences
import com.safarparmar.app.data.local.SafarDataStore
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BatchStudyModePreferenceTest {
    @Test fun choicesAreScopedToTheSignedInAccountAndSurviveCacheInvalidation() = runTest {
        val user = MutableStateFlow<String?>("a")
        val accounts = mockk<SafarDataStore>()
        every { accounts.userId } returns user
        val prefs = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { prefs.getString("study-mode:a", null) } returns "PERSONAL"
        every { prefs.getString("study-mode:b", null) } returns "OFFICIAL"
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        val cache = BatchOverviewCache(context, accounts)
        assertEquals(BatchStudyMode.PERSONAL, cache.studyMode())
        cache.saveStudyMode(BatchStudyMode.PERSONAL)
        verify { editor.putString("study-mode:a", "PERSONAL") }
        cache.clear()
        verify { editor.remove("a") }
        verify(exactly = 0) { editor.remove(match { it.startsWith("study-mode:") }) }
        user.value = "b"
        assertEquals(BatchStudyMode.OFFICIAL, cache.studyMode())
        cache.saveStudyMode(BatchStudyMode.OFFICIAL)
        verify { editor.putString("study-mode:b", "OFFICIAL") }
    }

    @Test fun missingOrInvalidPreferencesAndSignedOutAccountsHaveNoSavedMode() = runTest {
        val user = MutableStateFlow<String?>("a")
        val accounts = mockk<SafarDataStore>()
        every { accounts.userId } returns user
        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.getString("study-mode:a", null) } returns "invalid"
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        val cache = BatchOverviewCache(context, accounts)
        assertNull(cache.studyMode())
        user.value = null
        assertNull(cache.studyMode())
        cache.saveStudyMode(BatchStudyMode.PERSONAL)
        verify(exactly = 0) { prefs.edit() }
    }
}
