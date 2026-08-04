package com.safarparmar.app.feature.live

import com.safarparmar.app.feature.live.model.LiveSession
import com.safarparmar.app.feature.live.presentation.DEFAULT_LIVE_CHAT_COOLDOWN_SECONDS
import com.safarparmar.app.feature.live.presentation.LiveChatUiState
import com.safarparmar.app.feature.live.presentation.isChatOpen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Live comments exist only for the duration of a broadcast. These pin down that
 * rule on the client; the server enforces the same one on `live:message`.
 */
class LiveChatGateTest {

    private fun session(status: String, chatEnabled: Boolean = true) = LiveSession(
        id = "live-1",
        title = "Biology Live",
        description = null,
        courseId = "course-1",
        teacherId = "teacher-1",
        scheduledStartAt = null,
        scheduledEndAt = null,
        status = status,
        youtubeVideoId = null,
        youtubeWatchUrl = null,
        youtubeEmbedUrl = null,
        thumbnailUrl = null,
        isChatEnabled = chatEnabled,
        isRecordingAvailable = false,
        recordingVideoId = null,
        resources = emptyList(),
        canManage = false,
    )

    @Test
    fun `chat is open only while the session is live`() {
        assertTrue(isChatOpen(session("live")))
        assertFalse(isChatOpen(session("scheduled")))
        assertFalse(isChatOpen(session("ended")))
        assertFalse(isChatOpen(session("cancelled")))
    }

    @Test
    fun `a host can switch comments off without ending the broadcast`() {
        assertFalse(isChatOpen(session("live", chatEnabled = false)))
    }

    @Test
    fun `no session means no chat`() {
        assertFalse(isChatOpen(null))
    }

    @Test
    fun `status casing from the API does not close chat`() {
        assertTrue(isChatOpen(session("LIVE")))
    }

    @Test
    fun `sending needs both an open chat and an elapsed cooldown`() {
        assertTrue(LiveChatUiState(isChatOpen = true, cooldownRemainingSeconds = 0).canSend)
        assertFalse(LiveChatUiState(isChatOpen = true, cooldownRemainingSeconds = 3).canSend)
        assertFalse(LiveChatUiState(isChatOpen = false, cooldownRemainingSeconds = 0).canSend)
    }

    @Test
    fun `the default gap matches the server's seven seconds`() {
        assertEquals(7, DEFAULT_LIVE_CHAT_COOLDOWN_SECONDS)
        assertEquals(
            DEFAULT_LIVE_CHAT_COOLDOWN_SECONDS,
            LiveChatUiState().cooldownSeconds,
        )
    }

    @Test
    fun `a fresh chat state starts closed so the composer is never shown too early`() {
        assertFalse(LiveChatUiState().isChatOpen)
        assertFalse(LiveChatUiState().canSend)
    }
}
