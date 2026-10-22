package com.safarparmar.app.ui.ekagra.focusshield

import android.app.Application
import com.safarparmar.app.ui.auth.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusShieldAppPickerSaveTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    private val youtube = "com.google.android.youtube"
    private val instagram = "com.instagram.android"
    private val application = mockk<Application>(relaxed = true)
    private val repository = mockk<FocusShieldRepository>(relaxed = true)
    private val appsLoader = mockk<InstalledAppsLoader>()
    private val enabled = MutableStateFlow(true)
    private val focusEnabled = MutableStateFlow(true)
    private val youtubeSelectionBusy = MutableStateFlow(false)
    private val youtubeSelectionError = MutableStateFlow<String?>(null)
    private val packages = MutableStateFlow(setOf(instagram))
    private var latestSelection = setOf(instagram)
    private var pendingEnable = false
    private lateinit var viewModel: FocusShieldViewModel

    @Before fun setUp() {
        mockkObject(FocusShieldPermissionHelper)
        every { FocusShieldPermissionHelper.hasUsageStatsPermission(any()) } returns true
        every { FocusShieldPermissionHelper.hasOverlayPermission(any()) } returns true
        every { repository.isEnabled } returns enabled
        every { repository.youtubeFocusEnabled } returns focusEnabled
        every { repository.youtubeSelectionBusy } returns youtubeSelectionBusy
        every { repository.youtubeFocusActivationError } returns youtubeSelectionError
        every { repository.blockedPackages } returns packages
        every { repository.selectedPackages() } answers { latestSelection }
        every { repository.isAlwaysOnMode } returns MutableStateFlow(false)
        every { repository.isStrictMode } returns MutableStateFlow(false)
        every { repository.appUsageMode } returns MutableStateFlow("focused")
        every { repository.scheduleEnabled } returns MutableStateFlow(false)
        every { repository.scheduleStartMinute } returns MutableStateFlow(540)
        every { repository.scheduleEndMinute } returns MutableStateFlow(1320)
        every { repository.protectionActive } returns MutableStateFlow(true)
        every { repository.protectionStarting } returns MutableStateFlow(false)
        every { repository.activationBlockedReason } returns MutableStateFlow(null)
        every { repository.blockedHitCount } returns MutableStateFlow(0)
        every { repository.blockedHitsByPackage } returns MutableStateFlow(emptyMap())
        every { repository.pendingEnableAfterAppSelection } answers { pendingEnable }
        every { repository.pendingEnableAfterAppSelection = any() } answers {
            pendingEnable = firstArg()
        }
        every { repository.setBlockedPackages(any()) } answers {
            val requested = firstArg<Set<String>>()
            latestSelection = if (focusEnabled.value) requested - youtube else requested
            packages.value = latestSelection
            latestSelection
        }
        every { repository.setEnabled(any()) } answers {
            enabled.value = firstArg()
        }
        coEvery { appsLoader.loadLaunchableApps() } returns listOf(
            BlockedAppInfo(youtube, "YouTube", null),
            BlockedAppInfo(instagram, "Instagram", null),
        )
        viewModel = FocusShieldViewModel(application, repository, appsLoader)
    }

    @After fun tearDown() {
        unmockkObject(FocusShieldPermissionHelper)
    }

    @Test fun `viewing apps and returning preserves a later Focus activation`() = runTest {
        viewModel.loadApps()
        runCurrent()

        assertFalse(viewModel.saveAndEnableShieldIfReady())

        assertTrue(focusEnabled.value)
        verify(exactly = 0) { repository.setEnabled(any()) }
        verify(exactly = 0) { repository.setBlockedPackages(any()) }
    }

    @Test fun `editing an existing Kavach selection does not switch Focus off`() = runTest {
        viewModel.loadApps()
        runCurrent()
        viewModel.deselectAllApps()
        viewModel.toggleApp(instagram)

        assertFalse(viewModel.saveAndEnableShieldIfReady())

        assertEquals(setOf(instagram), latestSelection)
        assertTrue(focusEnabled.value)
        verify(exactly = 0) { repository.setEnabled(any()) }
    }

    @Test fun `ordinary app selection does not enable disabled Kavach`() = runTest {
        enabled.value = false
        viewModel.loadApps()
        runCurrent()

        assertFalse(viewModel.saveAndEnableShieldIfReady())

        assertFalse(enabled.value)
        assertTrue(focusEnabled.value)
        verify(exactly = 0) { repository.setEnabled(any()) }
    }

    @Test fun `explicit pending setup activates Kavach using latest selection`() = runTest {
        enabled.value = false
        pendingEnable = true
        // The immediate repository selection may precede the displayed StateFlow.
        packages.value = emptySet()

        assertTrue(viewModel.saveAndEnableShieldIfReady())

        assertTrue(enabled.value)
        assertTrue(focusEnabled.value)
        assertFalse(pendingEnable)
        verify(exactly = 1) { repository.setEnabled(true) }
        verify(exactly = 0) { repository.setBlockedPackages(any()) }
    }

    @Test fun `stale selected YouTube row cannot restore a removed block`() = runTest {
        enabled.value = false
        packages.value = setOf(youtube)
        latestSelection = setOf(youtube)
        viewModel.loadApps()
        runCurrent()
        assertTrue(viewModel.pickerState.value.allApps.first { it.packageName == youtube }.isBlocked)

        pendingEnable = true
        latestSelection = emptySet()
        packages.value = emptySet()

        assertFalse(viewModel.saveAndEnableShieldIfReady())

        assertTrue(focusEnabled.value)
        assertTrue(latestSelection.isEmpty())
        verify(exactly = 0) { repository.setEnabled(any()) }
        verify(exactly = 0) { repository.setBlockedPackages(any()) }
    }

    @Test fun `clearing the last selected app does not resurrect it on save`() = runTest {
        enabled.value = false
        pendingEnable = true
        viewModel.loadApps()
        runCurrent()
        viewModel.deselectAllApps()

        assertFalse(viewModel.saveAndEnableShieldIfReady())

        assertTrue(latestSelection.isEmpty())
        verify(exactly = 1) { repository.setBlockedPackages(emptySet()) }
        verify(exactly = 0) { repository.setEnabled(any()) }
    }

    @Test fun `pending setup without required permissions preserves Focus`() = runTest {
        enabled.value = false
        pendingEnable = true
        every { FocusShieldPermissionHelper.hasOverlayPermission(any()) } returns false

        assertFalse(viewModel.saveAndEnableShieldIfReady())

        assertTrue(pendingEnable)
        assertTrue(focusEnabled.value)
        verify(exactly = 0) { repository.setEnabled(any()) }
    }

    @Test fun `stale pending flag does not reactivate an already enabled Kavach`() = runTest {
        pendingEnable = true

        assertFalse(viewModel.saveAndEnableShieldIfReady())

        assertFalse(pendingEnable)
        assertTrue(focusEnabled.value)
        verify(exactly = 0) { repository.setEnabled(any()) }
    }

    @Test fun `loaded picker follows automatic YouTube deselection while keeping search`() = runTest {
        latestSelection = setOf(youtube, instagram)
        packages.value = latestSelection
        viewModel.loadApps()
        runCurrent()
        viewModel.setSearchQuery("YouTube")
        assertTrue(viewModel.pickerState.value.allApps.first { it.packageName == youtube }.isBlocked)

        latestSelection = setOf(instagram)
        packages.value = latestSelection
        runCurrent()

        val state = viewModel.pickerState.value
        assertFalse(state.isLoading)
        assertEquals("YouTube", state.searchQuery)
        assertFalse(state.allApps.first { it.packageName == youtube }.isBlocked)
        assertTrue(state.allApps.first { it.packageName == instagram }.isBlocked)
        verify(exactly = 0) { repository.setBlockedPackages(any()) }
    }

    @Test fun `confirmed YouTube override calls dedicated action without checking row early`() = runTest {
        viewModel.loadApps()
        runCurrent()

        viewModel.selectYoutubeForKavach()
        runCurrent()

        verify(exactly = 1) { repository.selectYoutubeForKavach() }
        verify(exactly = 0) { repository.setBlockedPackages(any()) }
        assertFalse(viewModel.pickerState.value.allApps.first { it.packageName == youtube }.isBlocked)
        assertTrue(focusEnabled.value)
    }

    @Test fun `persisted override emission checks YouTube and preserves other selection`() = runTest {
        viewModel.loadApps()
        runCurrent()
        viewModel.setSearchQuery("YouTube")
        viewModel.selectYoutubeForKavach()
        runCurrent()
        assertFalse(viewModel.pickerState.value.allApps.first { it.packageName == youtube }.isBlocked)

        // The repository has completed the override and now publishes the persisted selection.
        focusEnabled.value = false
        latestSelection = setOf(instagram, youtube)
        packages.value = latestSelection
        runCurrent()

        assertFalse(viewModel.pickerState.value.isLoading)
        assertEquals("YouTube", viewModel.pickerState.value.searchQuery)
        assertTrue(viewModel.pickerState.value.allApps.all { it.isBlocked })
        assertFalse(viewModel.youtubeFocusEnabled.value)
    }

    @Test fun `load completion uses latest selection and preserves query typed during loading`() = runTest {
        val loadedApps = CompletableDeferred<List<BlockedAppInfo>>()
        coEvery { appsLoader.loadLaunchableApps() } coAnswers { loadedApps.await() }
        viewModel.loadApps()
        runCurrent()
        viewModel.setSearchQuery("YouTube")

        latestSelection = setOf(instagram, youtube)
        packages.value = latestSelection
        runCurrent()
        assertTrue(viewModel.pickerState.value.isLoading)
        loadedApps.complete(listOf(
            BlockedAppInfo(youtube, "YouTube", null),
            BlockedAppInfo(instagram, "Instagram", null),
        ))
        runCurrent()

        assertFalse(viewModel.pickerState.value.isLoading)
        assertEquals("YouTube", viewModel.pickerState.value.searchQuery)
        assertTrue(viewModel.pickerState.value.allApps.all { it.isBlocked })
    }

    @Test fun `select all preserves Focus and uses filtered authoritative selection`() = runTest {
        viewModel.loadApps()
        runCurrent()

        viewModel.selectAllApps()
        runCurrent()

        assertTrue(focusEnabled.value)
        assertEquals(setOf(instagram), latestSelection)
        assertFalse(viewModel.pickerState.value.allApps.first { it.packageName == youtube }.isBlocked)
        assertTrue(viewModel.pickerState.value.allApps.first { it.packageName == instagram }.isBlocked)
        verify(exactly = 0) { repository.selectYoutubeForKavach() }
    }
}
