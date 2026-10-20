package com.safarparmar.app.ui.ekagra.focusshield

import android.app.NotificationManager
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.feature.kavachanalytics.data.KavachAnalyticsRecorder
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2GuardService
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2HealthMonitor
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2Preferences
import com.safarparmar.app.ui.auth.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusModeSwitchingTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()
    private val youtube = "com.google.android.youtube"
    private val other = "com.instagram.android"
    private val context = mockk<Context>(relaxed = true)
    private val dataStore = mockk<SafarDataStore>(relaxed = true)
    private val youtubePreferences = mockk<YoutubeStudyV2Preferences>(relaxed = true)
    private val youtubeEnabled = MutableStateFlow(false)
    private val packages = MutableStateFlow(setOf(youtube, other))
    private val kavachEnabled = MutableStateFlow(true)
    private val alwaysOn = MutableStateFlow(false)
    private lateinit var repository: FocusShieldRepository

    @Before fun setUp() {
        mockkObject(YoutubeStudyV2AccessibilityService.Companion, YoutubeStudyV2GuardService.Companion, YoutubeStudyV2HealthMonitor.Companion,
            KavachAlwaysOnService.Companion, FocusShieldPermissionHelper)
        every { YoutubeStudyV2GuardService.stop(any()) } just Runs
        every { YoutubeStudyV2GuardService.start(any()) } just Runs
        every { YoutubeStudyV2AccessibilityService.isConnected } returns true
        every { context.getSystemService(NotificationManager::class.java) } returns mockk(relaxed = true)
        every { YoutubeStudyV2HealthMonitor.isAccessibilityEnabled(any()) } returns true
        every { KavachAlwaysOnService.start(any(), any(), any()) } returns true
        every { FocusShieldPermissionHelper.hasUsageStatsPermission(any()) } returns true
        every { FocusShieldPermissionHelper.hasOverlayPermission(any()) } returns true
        val storage = mockk<SharedPreferences>(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns storage
        every { storage.edit() } returns mockk(relaxed = true)
        every { dataStore.focusShieldEnabled } returns kavachEnabled
        every { dataStore.focusShieldBlockedPackages } returns packages
        every { dataStore.focusShieldStrictMode } returns MutableStateFlow(false)
        every { dataStore.focusShieldAlwaysOnMode } returns alwaysOn
        every { dataStore.appUsageMode } returns MutableStateFlow("standard")
        every { dataStore.focusShieldScheduleEnabled } returns MutableStateFlow(false)
        every { dataStore.focusShieldScheduleStartMinute } returns MutableStateFlow(540)
        every { dataStore.focusShieldScheduleEndMinute } returns MutableStateFlow(1320)
        every { youtubePreferences.enabled } returns youtubeEnabled
        every { youtubePreferences.isDisclosureAccepted() } returns true
        every { youtubePreferences.setEnabled(any()) } answers { youtubeEnabled.value = firstArg() }
        coEvery { dataStore.setFocusShieldBlockedPackages(any()) } coAnswers {
            packages.value = firstArg()
            mockk(relaxed = true)
        }
        coEvery { dataStore.setFocusShieldEnabled(any()) } coAnswers {
            kavachEnabled.value = firstArg()
            mockk(relaxed = true)
        }
        coEvery { dataStore.setFocusShieldAlwaysOnMode(any()) } coAnswers {
            alwaysOn.value = firstArg()
            mockk(relaxed = true)
        }
        repository = FocusShieldRepository(dataStore, mockk<HomeRepository>(relaxed = true),
            mockk<KavachAnalyticsRecorder>(relaxed = true), context, youtubePreferences)
    }

    @After fun tearDown() {
        unmockkObject(YoutubeStudyV2AccessibilityService.Companion, YoutubeStudyV2GuardService.Companion, YoutubeStudyV2HealthMonitor.Companion,
            KavachAlwaysOnService.Companion, FocusShieldPermissionHelper)
        FocusShieldRepository.Snapshot.active = false
        FocusShieldRepository.Snapshot.packages = emptySet()
    }

    @Test fun `enabling Focus persists YouTube deselection and preserves all other blocks`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
        assertEquals(setOf(other), repository.blockedPackages.value)
        assertTrue(kavachEnabled.value)
        verify { youtubePreferences.recordAccessibilityHeartbeat(any()) }
        assertEquals(setOf(other), repository.setBlockedPackages(setOf(youtube, other)))
        runCurrent()
        assertEquals(setOf(other), packages.value)
    }

    @Test fun `turning Focus off does not silently reselect YouTube`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setYoutubeFocusEnabled(false)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
        assertEquals(setOf(youtube, other), repository.setBlockedPackages(setOf(youtube, other)))
    }

    @Test fun `enabling Kavach preserves Focus and its guard`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setEnabled(true)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        verify(exactly = 0) { YoutubeStudyV2GuardService.stop(context) }
        assertTrue(kavachEnabled.value)
        assertEquals(setOf(other), packages.value)
    }

    @Test fun `Always On activation keeps Focus managing YouTube`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setAlwaysOnMode(true)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        repository.reportProtectionActive()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertTrue(alwaysOn.value)
        assertEquals(setOf(other), packages.value)
    }

    @Test fun `Kavach enable does not cancel pending Focus persistence`() = runTest {
        runCurrent()
        val persist = CompletableDeferred<Unit>()
        coEvery { dataStore.setFocusShieldBlockedPackages(any()) } coAnswers {
            persist.await()
            packages.value = firstArg()
            mockk(relaxed = true)
        }
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setEnabled(true)
        persist.complete(Unit)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
    }

    @Test fun `missing disclosure does not remove YouTube or enable Focus`() = runTest {
        runCurrent()
        every { youtubePreferences.isDisclosureAccepted() } returns false
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(youtube, other), packages.value)
    }

    @Test fun `editing apps while Kavach runs does not switch Focus off`() = runTest {
        alwaysOn.value = true
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setBlockedPackages(setOf(youtube, other, "com.example.blocked"))
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other, "com.example.blocked"), packages.value)
    }

    @Test fun `new timer Kavach session preserves Focus`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.activateForSession()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        repository.reportProtectionActive()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), repository.sessionBlockedPackages.value)
    }

    @Test fun `continuing an existing timer session preserves a later Focus action`() = runTest {
        runCurrent()
        repository.activateForSession()
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.activateForSession(isFocusPeriod = false)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
    }

    @Test fun `existing enabled Focus cleans up saved YouTube selection on startup`() = runTest {
        youtubeEnabled.value = true
        val restored = FocusShieldRepository(dataStore, mockk(relaxed = true),
            mockk(relaxed = true), context, youtubePreferences)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
        assertEquals(setOf(other), restored.blockedPackages.value)
    }

    @Test fun `failed Kavach activation does not turn Focus off`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        every { FocusShieldPermissionHelper.hasOverlayPermission(any()) } returns false
        repository.setEnabled(true)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertNotNull(repository.activationBlockedReason.value)
    }

    @Test fun `missing accessibility permission preserves the selected YouTube app block`() = runTest {
        runCurrent()
        every { YoutubeStudyV2HealthMonitor.isAccessibilityEnabled(any()) } returns false
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(youtube, other), packages.value)
        assertNotNull(repository.youtubeFocusActivationError.value)
        coVerify(exactly = 0) { dataStore.setFocusShieldBlockedPackages(any()) }
    }

    @Test fun `permission without a connected monitor never transfers protection`() = runTest {
        runCurrent()
        every { YoutubeStudyV2AccessibilityService.isConnected } returns false
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        advanceTimeBy(5_100)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(youtube, other), packages.value)
        assertNotNull(repository.youtubeFocusActivationError.value)
    }

    @Test fun `initial accessibility connection can complete before transfer`() = runTest {
        runCurrent()
        var connected = false
        every { YoutubeStudyV2AccessibilityService.isConnected } answers { connected }
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertTrue(youtube in packages.value)
        connected = true
        advanceTimeBy(150)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
    }

    @Test fun `rejected Kavach service start preserves working Focus`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        every { KavachAlwaysOnService.start(any(), any(), any()) } returns false
        repository.activateForSession()
        runCurrent()
        assertFalse(repository.sessionActive.value)
        assertNotNull(repository.activationBlockedReason.value)
        assertTrue(youtubeEnabled.value)
    }

    @Test fun `empty Always On profile is rejected before disabling Focus`() = runTest {
        runCurrent()
        repository.setBlockedPackages(setOf(youtube))
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setKavachProfile(com.safarparmar.app.ui.launch.AppUsageMode.ALWAYS_ON)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertFalse(alwaysOn.value)
        assertNotNull(repository.activationBlockedReason.value)
    }

    @Test fun `accepted start that times out preserves Focus until actual protection`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setAlwaysOnMode(true)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        advanceTimeBy(5_100)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertFalse(repository.protectionActive.value)
        assertNotNull(repository.activationBlockedReason.value)
    }

    @Test fun `later Focus action wins over an earlier pending Kavach acknowledgement`() = runTest {
        runCurrent()
        repository.setAlwaysOnMode(true)
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.reportProtectionActive()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
    }

    @Test fun `canceling pending Kavach leaves Focus running after a late acknowledgement`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setAlwaysOnMode(true)
        runCurrent()
        repository.setEnabled(false)
        runCurrent()
        repository.reportProtectionActive()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertFalse(repository.protectionActive.value)
    }

    @Test fun `lost permission during persistence restores the previous selection`() = runTest {
        runCurrent()
        val persist = CompletableDeferred<Unit>()
        coEvery { dataStore.setFocusShieldBlockedPackages(any()) } coAnswers {
            persist.await()
            packages.value = firstArg()
            mockk(relaxed = true)
        }
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        every { YoutubeStudyV2HealthMonitor.isAccessibilityEnabled(any()) } returns false
        persist.complete(Unit)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(youtube, other), packages.value)
    }

    @Test fun `new app edits made during initial selection read are preserved`() = runTest {
        runCurrent()
        val read = CompletableDeferred<Unit>()
        every { dataStore.focusShieldBlockedPackages } returns flow {
            read.await()
            emit(setOf(youtube, other))
        }
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setBlockedPackages(setOf(youtube, other, "com.example.new"))
        read.complete(Unit)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other, "com.example.new"), packages.value)
    }

    @Test fun `canceled Focus request does not keep YouTube disabled in picker`() = runTest {
        runCurrent()
        every { YoutubeStudyV2AccessibilityService.isConnected } returns false
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setYoutubeFocusEnabled(false)
        runCurrent()
        advanceTimeBy(200)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertTrue(youtube in repository.setBlockedPackages(setOf(youtube, other)))
    }

    @Test fun `persistence failure leaves old protection and reports activation error`() = runTest {
        runCurrent()
        coEvery { dataStore.setFocusShieldBlockedPackages(any()) } throws java.io.IOException("write failed")
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(youtube, other), packages.value)
        assertNotNull(repository.youtubeFocusActivationError.value)
    }

    @Test fun `changing away from pending Always On preserves the later profile choice`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setKavachProfile(com.safarparmar.app.ui.launch.AppUsageMode.ALWAYS_ON)
        runCurrent()
        repository.setKavachProfile(com.safarparmar.app.ui.launch.AppUsageMode.FOCUSED)
        runCurrent()
        repository.reportProtectionActive()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertFalse(alwaysOn.value)
    }

    @Test fun `Always On can activate after Kavach was manually disabled`() = runTest {
        runCurrent()
        repository.setEnabled(false)
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.setAlwaysOnMode(true)
        runCurrent()
        repository.reportProtectionActive()
        runCurrent()
        assertTrue(repository.isEnabled.value)
        assertTrue(repository.protectionActive.value)
        assertTrue(youtubeEnabled.value)
    }

    @Test fun `confirmed whole app choice selects YouTube and preserves other selections`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.selectYoutubeForKavach()
        assertTrue(repository.youtubeSelectionBusy.value)
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), repository.blockedPackages.value)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertFalse(repository.youtubeSelectionBusy.value)
        assertEquals(setOf(youtube, other), repository.blockedPackages.value)
        assertEquals(setOf(youtube, other), packages.value)
        assertTrue(kavachEnabled.value)
        assertFalse(alwaysOn.value)
        verify { YoutubeStudyV2GuardService.stop(context) }
    }

    @Test fun `whole app choice waits for service to load YouTube before stopping Focus`() = runTest {
        alwaysOn.value = true
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        val starts = mutableListOf<Long>()
        every { KavachAlwaysOnService.start(any(), any(), capture(starts)) } returns true
        repository.selectYoutubeForKavach()
        runCurrent()
        assertTrue(repository.youtubeSelectionBusy.value)
        assertTrue(youtubeEnabled.value)
        repository.reportProtectionActive(setOf(other), starts.last())
        runCurrent()
        assertTrue(youtubeEnabled.value)
        repository.reportProtectionActive(setOf(youtube, other), starts.last() - 1)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        repository.reportProtectionActive(setOf(youtube, other), starts.last())
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertFalse(repository.youtubeSelectionBusy.value)
        assertEquals(setOf(youtube, other), repository.blockedPackages.value)
    }

    @Test fun `sole YouTube whole app choice restarts active Kavach`() = runTest {
        alwaysOn.value = true
        runCurrent()
        repository.setBlockedPackages(setOf(youtube))
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        assertTrue(packages.value.isEmpty())
        val starts = mutableListOf<Long>()
        every { KavachAlwaysOnService.start(any(), any(), capture(starts)) } returns true
        repository.selectYoutubeForKavach()
        runCurrent()
        assertTrue(starts.isNotEmpty())
        assertTrue(youtubeEnabled.value)
        repository.reportProtectionActive(setOf(youtube), starts.last())
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(youtube), repository.blockedPackages.value)
    }

    @Test fun `failed whole app service startup keeps Focus and previous selections`() = runTest {
        alwaysOn.value = true
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        every { KavachAlwaysOnService.start(any(), any(), any()) } returns false
        repository.selectYoutubeForKavach()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
        assertEquals(setOf(other), repository.blockedPackages.value)
        assertFalse(repository.youtubeSelectionBusy.value)
        assertNotNull(repository.youtubeFocusActivationError.value)
    }

    @Test fun `whole app acknowledgement timeout restores Focus selection`() = runTest {
        alwaysOn.value = true
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.selectYoutubeForKavach()
        runCurrent()
        advanceTimeBy(5_600)
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
        assertFalse(repository.youtubeSelectionBusy.value)
        assertNotNull(repository.youtubeFocusActivationError.value)
    }

    @Test fun `whole app persistence failure leaves Focus and old choice unchanged`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        coEvery { dataStore.setFocusShieldBlockedPackages(any()) } throws java.io.IOException("write failed")
        repository.selectYoutubeForKavach()
        runCurrent()
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
        assertEquals(setOf(other), repository.blockedPackages.value)
        assertFalse(repository.youtubeSelectionBusy.value)
        assertNotNull(repository.youtubeFocusActivationError.value)
    }

    @Test fun `newer Focus choice promptly cancels a waiting whole app override`() = runTest {
        alwaysOn.value = true
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        repository.selectYoutubeForKavach()
        runCurrent()
        assertTrue(repository.youtubeSelectionBusy.value)
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        assertFalse(repository.youtubeSelectionBusy.value)
        assertTrue(youtubeEnabled.value)
        assertEquals(setOf(other), packages.value)
        assertNull(repository.youtubeFocusActivationError.value)
    }

    @Test fun `new picker selections are retained while whole app override is pending`() = runTest {
        runCurrent()
        repository.setYoutubeFocusEnabled(true)
        runCurrent()
        val persist = CompletableDeferred<Unit>()
        coEvery { dataStore.setFocusShieldBlockedPackages(any()) } coAnswers {
            persist.await()
            packages.value = firstArg()
            mockk(relaxed = true)
        }
        repository.selectYoutubeForKavach()
        runCurrent()
        repository.setBlockedPackages(setOf(other, "com.example.new"))
        persist.complete(Unit)
        runCurrent()
        assertFalse(youtubeEnabled.value)
        assertEquals(setOf(youtube, other, "com.example.new"), packages.value)
        assertEquals(packages.value, repository.blockedPackages.value)
    }
}
