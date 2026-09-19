package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeFullscreenPolicyTest {
    @Test fun `same productive video retains permission when fullscreen title remains visible`() {
        assertTrue(canRetainFullscreenPermission("Lesson", "Lesson", YoutubeChannelClassification.PRODUCTIVE))
    }
    @Test fun `hidden HUD retains the verified productive video`() {
        assertTrue(canRetainFullscreenPermission("Lesson", null, YoutubeChannelClassification.PRODUCTIVE))
    }
    @Test fun `next video with a different title requires fresh identity`() {
        assertFalse(canRetainFullscreenPermission("Lesson", "Entertainment", YoutubeChannelClassification.PRODUCTIVE))
        assertFalse(canRetainFullscreenPermission(null, "Lesson", YoutubeChannelClassification.PRODUCTIVE))
    }
    @Test fun `distracting or unknown classification never retains permission`() {
        assertFalse(canRetainFullscreenPermission("Lesson", "Lesson", YoutubeChannelClassification.DISTRACTING))
        assertFalse(canRetainFullscreenPermission("Lesson", "Lesson", null))
    }
    @Test fun `only actual next-video controls invalidate fullscreen permission`() {
        assertTrue(isFullscreenPlaybackChangeControl("player_control_next_button", "Next video"))
        assertTrue(isFullscreenPlaybackChangeControl(null, "Play next video: Entertainment"))
        assertFalse(isFullscreenPlaybackChangeControl("player_control_play_pause", "Pause"))
        assertFalse(isFullscreenPlaybackChangeControl(null, "Show player controls"))
        assertFalse(isFullscreenPlaybackChangeControl(null, "More videos"))
    }
    @Test fun `current player transport is not mistaken for a new video card`() {
        assertTrue(isCurrentVideoTransportControl("player_control_play_pause_replay_button", "Play video"))
        assertTrue(isCurrentVideoTransportControl(null, "Pause video"))
        assertFalse(isVideoCardAccessibilityLabel("Play video"))
        assertTrue(isVideoCardAccessibilityLabel("Physics lesson – 12 minutes – play video"))
    }
}
