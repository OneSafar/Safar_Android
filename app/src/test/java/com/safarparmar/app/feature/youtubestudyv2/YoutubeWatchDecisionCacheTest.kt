package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.*
import org.junit.Test

class YoutubeWatchDecisionCacheTest {
    private val watch = YoutubeV2Observation(YoutubeV2ContentKind.VIDEO, true,
        title = "Lesson", exactHandle = "@teacher", canResumeWatchSession = true)

    @Test fun `confirmed video enters monitoring and skips the unchanged applied decision`() {
        val session = YoutubeStudyV2Session()
        session.onVideoTap(100L)
        assertTrue(session.acceptStable(watch, 200L))
        assertEquals(YoutubeStudyV2Session.State.MONITORING, session.state)
        assertTrue(session.isAlreadyEvaluated(watch.stableKey, watch.stableKey))
        assertTrue(session.acceptStable(watch, 60_000L))
        assertTrue(session.isAlreadyEvaluated(watch.stableKey, watch.stableKey))
    }

    @Test fun `autoplay and changed allow rules require a new evaluation`() {
        val session = YoutubeStudyV2Session()
        session.acceptStable(watch, 100L)
        val next = watch.copy(title = "Another video", exactHandle = "@other")
        assertTrue(session.acceptStable(next, 60_000L))
        assertFalse(session.isAlreadyEvaluated(next.stableKey, watch.stableKey))
        assertFalse(session.isAlreadyEvaluated(next.stableKey, null))
    }

    @Test fun `explicit add to productive can resume a long paused video with a hidden owner row`() {
        val session = YoutubeStudyV2Session()
        session.onVideoTap(100L)
        // The user's Add action starts a fresh watch interaction after the old tap expires.
        session.onVideoTap(60_000L)
        assertTrue(session.acceptStable(watch.copy(canResumeWatchSession = false), 60_000L))
        assertTrue(session.isAlreadyEvaluated(watch.stableKey, watch.stableKey))
    }
}
