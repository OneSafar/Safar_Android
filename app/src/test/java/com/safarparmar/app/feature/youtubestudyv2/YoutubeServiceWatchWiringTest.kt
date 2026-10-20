package com.safarparmar.app.feature.youtubestudyv2

import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class YoutubeServiceWatchWiringTest {
    private val watch = YoutubeV2Observation(YoutubeV2ContentKind.VIDEO, true,
        title = "Lesson", exactHandle = "@teacher", canResumeWatchSession = true)

    private fun service(): YoutubeStudyV2AccessibilityService =
        spyk(YoutubeStudyV2AccessibilityService(), recordPrivateCalls = true).also { service ->
            service.preferences = mockk {
                every { enabled } returns MutableStateFlow(true)
            }
            every { service["readObservation"]() } returns watch
            every { service["decide"](any<YoutubeV2Observation>(), any<Long>()) } answers { Unit }
        }

    private fun field(name: String) =
        YoutubeStudyV2AccessibilityService::class.java.getDeclaredField(name).apply { isAccessible = true }

    private fun confirm(service: YoutubeStudyV2AccessibilityService, observation: YoutubeV2Observation) {
        YoutubeStudyV2AccessibilityService::class.java
            .getDeclaredMethod("captureConfirmation", YoutubeV2Observation::class.java, Long::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(service, observation, 0L)
    }

    @Test fun `actual service confirmation enters monitoring before skipping the applied decision`() {
        val service = service()
        val session = field("session").get(service) as YoutubeStudyV2Session
        session.onVideoTap(0L)
        field("lastEvaluatedKey").set(service, watch.stableKey)

        confirm(service, watch)

        assertEquals(YoutubeStudyV2Session.State.MONITORING, session.state)
        verify(exactly = 0) { service["decide"](any<YoutubeV2Observation>(), any<Long>()) }
    }

    @Test fun `actual service confirmation reevaluates a changed video`() {
        val service = service()
        val session = field("session").get(service) as YoutubeStudyV2Session
        session.acceptStable(watch, 0L)
        field("lastEvaluatedKey").set(service, watch.stableKey)
        val next = watch.copy(title = "Next video", exactHandle = "@other")
        every { service["readObservation"]() } returns next

        confirm(service, next)

        verify(exactly = 1) { service["decide"](next, 0L) }
    }

    @Test fun `actual service confirmation reevaluates invalidated allow rules`() {
        val service = service()
        val session = field("session").get(service) as YoutubeStudyV2Session
        session.acceptStable(watch, 0L)
        field("lastEvaluatedKey").set(service, null)

        confirm(service, watch)

        verify(exactly = 1) { service["decide"](watch, 0L) }
    }
}
