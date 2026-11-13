package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeFullscreenCarryOverTest {
    private val verified = YoutubeVerifiedVideo(title = "Lecture 1", handle = "@physicswallah")
    private val productive = YoutubeChannelClassification.PRODUCTIVE

    private fun fullscreen(
        title: String? = "Lecture 2",
        handle: String? = null,
        ad: Boolean = false,
        isFullscreen: Boolean = true,
    ) = YoutubeV2Observation(
        kind = YoutubeV2ContentKind.VIDEO,
        watchScreenConfirmed = true,
        fullscreen = isFullscreen,
        title = title,
        exactHandle = handle,
        adPlaying = ad,
    )

    @Test fun `autoplay after a verified productive video keeps playing while identifying`() {
        assertTrue(canCarryFullscreenPermission(fullscreen(), verified, productive, carriedFromNextTap = false))
    }

    @Test fun `next tapped in fullscreen after a productive video carries over`() {
        assertTrue(canCarryFullscreenPermission(fullscreen(), null, null, carriedFromNextTap = true))
    }

    @Test fun `no carry-over without a productive verified video`() {
        assertFalse(canCarryFullscreenPermission(fullscreen(), null, productive, false))
        assertFalse(canCarryFullscreenPermission(fullscreen(), verified, null, false))
        assertFalse(canCarryFullscreenPermission(fullscreen(), verified, YoutubeChannelClassification.DISTRACTING, false))
    }

    @Test fun `owner evidence, ads and portrait use the normal path`() {
        assertFalse(canCarryFullscreenPermission(fullscreen(handle = "@other"), verified, productive, false))
        assertFalse(canCarryFullscreenPermission(fullscreen(ad = true), verified, productive, false))
        assertFalse(canCarryFullscreenPermission(fullscreen(isFullscreen = false), verified, productive, true))
    }
}
