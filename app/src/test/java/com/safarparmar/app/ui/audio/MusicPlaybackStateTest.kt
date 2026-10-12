package com.safarparmar.app.ui.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPlaybackStateTest {
    @Test fun `prepared music waits for focus when a call delays the request`() {
        val state = MusicPlaybackState()
        state.prepared()
        assertFalse(state.canPlay)
        state.gainedFocus()
        assertTrue(state.canPlay)
    }

    @Test fun `call arriving during preparation cannot be bypassed by prepared callback`() {
        val state = MusicPlaybackState()
        state.gainedFocus()
        state.lostFocus(permanent = false)
        state.prepared()
        assertFalse(state.canPlay)
        state.gainedFocus()
        assertTrue(state.canPlay)
    }

    @Test fun `transient loss pauses music until the system returns focus`() {
        val state = MusicPlaybackState()
        state.prepared()
        state.gainedFocus()
        assertTrue(state.canPlay)
        state.lostFocus(permanent = false)
        assertFalse(state.canPlay)
        state.gainedFocus()
        assertTrue(state.canPlay)
    }

    @Test fun `permanent focus loss never restarts music on a late gain`() {
        val state = MusicPlaybackState()
        state.prepared()
        state.gainedFocus()
        state.lostFocus(permanent = true)
        state.gainedFocus()
        assertFalse(state.canPlay)
    }

    @Test fun `released or replaced track ignores late preparation and focus gain`() {
        val state = MusicPlaybackState()
        state.released()
        state.prepared()
        state.gainedFocus()
        assertFalse(state.canPlay)
    }
}
