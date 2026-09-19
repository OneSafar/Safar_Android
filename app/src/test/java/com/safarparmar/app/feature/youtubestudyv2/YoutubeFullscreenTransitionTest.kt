package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.*
import org.junit.Test

class YoutubeFullscreenTransitionTest {
    private val portrait = YoutubeV2Observation(YoutubeV2ContentKind.VIDEO, true,
        title = "Lesson", exactHandle = "@teacher")
    private val full = portrait.copy(fullscreen = true, exactHandle = null)

    @Test fun `instant fullscreen preserves pending owner and decision key`() {
        val state = YoutubeFullscreenTransition()
        state.observe(portrait, 100)
        assertEquals(portrait.stableKey, state.observe(full.copy(title = null), 200).stableKey)
        assertEquals("@teacher", state.observe(full, 9000).exactHandle)
    }
    @Test fun `new title cannot inherit productive owner`() {
        val state = YoutubeFullscreenTransition()
        state.observe(portrait, 100)
        assertNull(state.observe(full.copy(title = "Different video"), 200).exactHandle)
    }
    @Test fun `new video tap clears even an identical title`() {
        val state = YoutubeFullscreenTransition()
        state.observe(portrait, 100)
        state.clear()
        assertNull(state.observe(full, 200).exactHandle)
    }
    @Test fun `hidden metadata does not inherit owner indefinitely`() {
        val state = YoutubeFullscreenTransition()
        state.observe(portrait, 100)
        assertNull(state.observe(full.copy(title = null), 3000).exactHandle)
    }
}
