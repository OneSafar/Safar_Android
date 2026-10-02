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

    @Test fun `scrolling portrait recommendations preserves the playing video`() {
        val state = YoutubeFullscreenTransition()
        state.observe(portrait.copy(exactHandle = null, displayName = "Teacher"), 100)
        val result = state.observe(portrait.copy(title = null, exactHandle = null), 60_000)
        assertEquals("Teacher", result.displayName)
        assertEquals("Lesson", result.title)
    }

    @Test fun `autoplay metadata replaces the previous portrait owner`() {
        val state = YoutubeFullscreenTransition()
        state.observe(portrait, 100)
        val next = portrait.copy(title = "Different video", exactHandle = "@other")
        assertEquals(next, state.observe(next, 200))
        assertEquals("@other", state.observe(next.copy(exactHandle = null), 300).exactHandle)
    }

    @Test fun `portrait title change without owner clears old identity`() {
        val state = YoutubeFullscreenTransition()
        state.observe(portrait, 100)
        assertNull(state.observe(portrait.copy(title = "Next lesson", exactHandle = null), 200).exactHandle)
        assertNull(state.observe(portrait.copy(title = null, exactHandle = null), 300).exactHandle)
    }

    @Test fun `new tap or leaving playback cannot inherit hidden portrait owner`() {
        val state = YoutubeFullscreenTransition()
        val hidden = portrait.copy(title = null, exactHandle = null)
        state.observe(portrait, 100)
        state.clear()
        assertFalse(state.observe(hidden, 200).hasOwnerEvidence)
        state.observe(portrait, 300)
        state.observe(YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK), 400)
        assertFalse(state.observe(hidden, 500).hasOwnerEvidence)
    }
}
