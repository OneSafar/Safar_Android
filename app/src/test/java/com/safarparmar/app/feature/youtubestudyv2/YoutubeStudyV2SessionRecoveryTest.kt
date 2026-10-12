package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeStudyV2SessionRecoveryTest {
    private val watch = YoutubeV2Observation(
        kind = YoutubeV2ContentKind.VIDEO, watchScreenConfirmed = true,
        title = "Lecture", exactHandle = "@parmarssc", canResumeWatchSession = true,
    )

    @Test fun restoredWatchPageDoesNotNeedAnotherTap() {
        assertTrue(YoutubeStudyV2Session().acceptStable(watch, 100))
    }

    @Test fun previewOrMissingOwnerCannotRestoreSession() {
        for (observation in listOf(
            watch.copy(canResumeWatchSession = false),
            watch.copy(watchScreenConfirmed = false),
            watch.copy(exactHandle = null),
            watch.copy(adPlaying = true),
        )) assertFalse(YoutubeStudyV2Session().acceptStable(observation, 100))
    }

    @Test fun invalidatedDecisionIsEvaluatedAgainForSameVideo() {
        val session = YoutubeStudyV2Session()
        session.acceptStable(watch, 100)
        assertTrue(session.isAlreadyEvaluated(watch.stableKey, watch.stableKey))
        assertFalse(session.isAlreadyEvaluated(watch.stableKey, null))
    }
}
