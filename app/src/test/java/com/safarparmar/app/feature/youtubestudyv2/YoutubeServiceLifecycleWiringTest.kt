package com.safarparmar.app.feature.youtubestudyv2

import android.content.Intent
import io.mockk.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class YoutubeServiceLifecycleWiringTest {
    @After fun tearDown() {
        unmockkObject(YoutubeStudyV2GuardService.Companion, YoutubeStudyV2Preferences.Companion)
    }

    @Test fun `feedback interruption preserves the bound monitor and schedules a new read`() {
        mockkObject(YoutubeStudyV2GuardService.Companion)
        every { YoutubeStudyV2GuardService.stop(any()) } just Runs
        val service = spyk(YoutubeStudyV2AccessibilityService(), recordPrivateCalls = true)
        service.preferences = mockk { every { enabled } returns MutableStateFlow(true) }
        every { service["dismissBlockOverlaySilently"]() } answers { Unit }
        every { service["stopAnalytics"]() } answers { Unit }
        every { service["scheduleRead"](any<Long>()) } answers { Unit }
        every { service["clearPlaybackState"]() } answers { Unit }
        val enabledObserver = mockk<Job>(relaxed = true)
        val classificationObserver = mockk<Job>(relaxed = true)
        fun field(name: String) = YoutubeStudyV2AccessibilityService::class.java
            .getDeclaredField(name).apply { isAccessible = true }
        field("enabledObserver").set(service, enabledObserver)
        field("classificationObserver").set(service, classificationObserver)
        val connectedField = field("isConnected")
        val previousConnected = connectedField.getBoolean(null)
        connectedField.setBoolean(null, true)
        try {
            service.onInterrupt()
            assertTrue(YoutubeStudyV2AccessibilityService.isConnected)
            verify(exactly = 0) { enabledObserver.cancel(any()) }
            verify(exactly = 0) { classificationObserver.cancel(any()) }
            verify(exactly = 0) { YoutubeStudyV2GuardService.stop(any()) }
            verify(exactly = 1) { service["scheduleRead"](0L) }
        } finally {
            connectedField.setBoolean(null, previousConnected)
        }
    }

    @Test fun `guard acknowledges startup before rejecting a stale command with a newer start pending`() {
        mockkObject(YoutubeStudyV2Preferences.Companion)
        every { YoutubeStudyV2Preferences.isEnabled(any()) } returns true
        val guard = spyk(YoutubeStudyV2GuardService(), recordPrivateCalls = true)
        every { guard["promote"](false) } returns true
        every { guard["removeForegroundNotification"]() } answers { Unit }
        every { guard.stopSelfResult(any()) } returns false
        val staleIntent = mockk<Intent> {
            every { getLongExtra(any(), any()) } returns -1L
        }

        guard.onCreate()
        verify(exactly = 1) { guard["promote"](false) }
        guard.onStartCommand(staleIntent, 0, 1)

        verifyOrder {
            guard["promote"](false)
            guard.stopSelfResult(1)
        }
        verify(exactly = 0) { guard["promote"](true) }
        verify(exactly = 0) { guard["removeForegroundNotification"]() }
    }
}
