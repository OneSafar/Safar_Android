package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KavachNotificationMediaEnforcementTest {
    private val youtube = "com.google.android.youtube"
    private val other = "com.example.blocked"
    private val context = mockk<Context>()
    private val preferences = mockk<SharedPreferences>()
    private var graceUntilMs = 0L
    private var unlockedPackage: String? = null
    private var origin: String? = null

    @Before fun setUp() {
        mockkObject(BlockedMediaEnforcer)
        every { BlockedMediaEnforcer.stop(any(), any()) } returns Unit
        every { context.getSharedPreferences("focus_shield_session", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getLong("grace_until_ms", 0L) } answers { graceUntilMs }
        every { preferences.getString("grace_package", null) } answers { unlockedPackage }
        every { preferences.getString("grace_origin", null) } answers { origin }
    }

    @After fun tearDown() {
        unmockkObject(BlockedMediaEnforcer)
    }

    private fun unlock(packageName: String? = youtube, source: String = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_KAVACH) {
        graceUntilMs = System.currentTimeMillis() + 60_000L
        unlockedPackage = packageName
        origin = source
    }

    @Test fun `normal package Quick Unlock keeps notification updates from pausing playback`() = runTest {
        unlock()
        pauseKavachNotificationMediaIfBlocked(context, youtube, strictMode = { false }, isStillBlocked = { true })
        verify(exactly = 0) { BlockedMediaEnforcer.stop(any(), any()) }
    }

    @Test fun `a different unlocked package does not exempt blocked media`() = runTest {
        unlock(other)
        pauseKavachNotificationMediaIfBlocked(context, youtube, strictMode = { false }, isStillBlocked = { true })
        verify(exactly = 1) { BlockedMediaEnforcer.stop(context, youtube) }
    }

    @Test fun `Study Mode unlock cannot bypass Kavach notification media blocking`() = runTest {
        unlock(source = FocusShieldRepository.ShieldPrefs.QUICK_UNLOCK_ORIGIN_YOUTUBE_STUDY)
        pauseKavachNotificationMediaIfBlocked(context, youtube, strictMode = { false }, isStillBlocked = { true })
        verify(exactly = 1) { BlockedMediaEnforcer.stop(context, youtube) }
    }

    @Test fun `strict mode ignores a remaining normal-mode unlock`() = runTest {
        unlock()
        // The strict argument comes from DataStore even when Always On has no timer snapshot.
        pauseKavachNotificationMediaIfBlocked(context, youtube, strictMode = { true }, isStillBlocked = { true })
        verify(exactly = 1) { BlockedMediaEnforcer.stop(context, youtube) }
    }

    @Test fun `expired unlock restores notification media blocking`() = runTest {
        unlock()
        graceUntilMs = System.currentTimeMillis() - 1L
        pauseKavachNotificationMediaIfBlocked(context, youtube, strictMode = { false }, isStillBlocked = { true })
        verify(exactly = 1) { BlockedMediaEnforcer.stop(context, youtube) }
    }

    @Test fun `legacy unscoped normal unlock remains compatible`() = runTest {
        unlock(packageName = null)
        origin = null
        pauseKavachNotificationMediaIfBlocked(context, youtube, strictMode = { false }, isStillBlocked = { true })
        verify(exactly = 0) { BlockedMediaEnforcer.stop(any(), any()) }
    }

    @Test fun `pending notification cannot pause after its app is removed from blocking`() = runTest {
        val strictMode = CompletableDeferred<Boolean>()
        var stillBlocked = true
        val enforcement = launch {
            pauseKavachNotificationMediaIfBlocked(context, youtube, { strictMode.await() }, { stillBlocked })
        }
        runCurrent()
        stillBlocked = false
        strictMode.complete(false)
        enforcement.join()
        verify(exactly = 0) { BlockedMediaEnforcer.stop(any(), any()) }
    }

    @Test fun `unlock granted during settings read is honored before media pause`() = runTest {
        val strictMode = CompletableDeferred<Boolean>()
        val enforcement = launch {
            pauseKavachNotificationMediaIfBlocked(context, youtube, { strictMode.await() }, { true })
        }
        runCurrent()
        unlock()
        strictMode.complete(false)
        enforcement.join()
        verify(exactly = 0) { BlockedMediaEnforcer.stop(any(), any()) }
    }
}
