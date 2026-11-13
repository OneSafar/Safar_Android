package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeFloatingPlaybackDecisionTest {
    @Test fun `productive floating playback is allowed`() {
        assertEquals(FloatingPlaybackDecision.ALLOW,
            floatingPlaybackDecision(YoutubeChannelClassification.PRODUCTIVE, null))
    }

    @Test fun `minimising before evaluation finishes waits for the result`() {
        assertEquals(FloatingPlaybackDecision.WAIT, floatingPlaybackDecision(null, 0L))
        assertEquals(FloatingPlaybackDecision.WAIT, floatingPlaybackDecision(null, FLOATING_IDENTITY_GRACE_MS))
    }

    @Test fun `grace expiry blocks`() {
        assertEquals(FloatingPlaybackDecision.BLOCK, floatingPlaybackDecision(null, FLOATING_IDENTITY_GRACE_MS + 1))
    }

    @Test fun `no owner seen blocks immediately`() {
        assertEquals(FloatingPlaybackDecision.BLOCK, floatingPlaybackDecision(null, null))
    }

    @Test fun `known non-productive result never waits`() {
        assertEquals(FloatingPlaybackDecision.BLOCK, floatingPlaybackDecision(YoutubeChannelClassification.OTHERS, 10L))
        assertEquals(FloatingPlaybackDecision.BLOCK, floatingPlaybackDecision(YoutubeChannelClassification.DISTRACTING, 10L))
    }
}
