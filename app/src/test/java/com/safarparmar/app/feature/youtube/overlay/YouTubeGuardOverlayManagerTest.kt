package com.safarparmar.app.feature.youtube.overlay

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.feature.youtube.policy.BlockReason
import com.safarparmar.app.feature.youtube.policy.PolicyState
import com.safarparmar.app.feature.youtube.policy.VideoIdentity
import com.safarparmar.app.feature.youtube.service.YouTubeGuardService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [YouTubeGuardOverlayManager] and [OverlayState].
 */
class YouTubeGuardOverlayManagerTest {

    private lateinit var overlayManager: YouTubeGuardOverlayManager

    @Before
    fun setUp() {
        overlayManager = YouTubeGuardOverlayManager()
    }

    @Test
    fun `initial state is Hidden`() {
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `showVerifying sets overlay state to Verifying with channel and title`() {
        overlayManager.showVerifying("Veritasium", "The Science of Thinking")

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Verifying, got $state", state is OverlayState.Verifying)
        val verifying = state as OverlayState.Verifying
        assertEquals("Veritasium", verifying.channelName)
        assertEquals("The Science of Thinking", verifying.title)
    }

    @Test
    fun `showBlocked sets overlay state to Blocked`() {
        overlayManager.showBlocked(
            channelName = "Distracting Hub",
            channelHandle = "@distracting_hub",
            reason = BlockReason.DISTRACTING_CHANNEL,
        )

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Blocked, got $state", state is OverlayState.Blocked)
        val blocked = state as OverlayState.Blocked
        assertEquals("Distracting Hub", blocked.channelName)
        assertEquals("@distracting_hub", blocked.channelHandle)
        assertEquals(BlockReason.DISTRACTING_CHANNEL, blocked.reason)
    }

    @Test
    fun `showShortsBlocked sets overlay state to ShortsBlocked`() {
        overlayManager.showShortsBlocked()

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.ShortsBlocked, got $state", state is OverlayState.ShortsBlocked)
    }

    @Test
    fun `showAskUser sets overlay state to AskUser`() {
        overlayManager.showAskUser(
            channelName = "New Channel",
            channelHandle = "@new_channel",
            onClassify = { _, _ -> },
        )

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.AskUser, got $state", state is OverlayState.AskUser)
        val askUser = state as OverlayState.AskUser
        assertEquals("New Channel", askUser.channelName)
        assertEquals("@new_channel", askUser.channelHandle)
    }

    @Test
    fun `hide sets overlay state to Hidden`() {
        overlayManager.showVerifying("Veritasium", "Title")
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        overlayManager.hide()
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `policy state Verifying with null identity hides overlay`() {
        val service = YouTubeGuardService()
        service.overlayManager = overlayManager

        // Pre-set overlay to Verifying to ensure hide() transitions it to Hidden
        overlayManager.showVerifying("Prior Channel", "Prior Title")
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        // Verifying with null identity (e.g. cold start, playback stopped) must hide overlay
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `policy state Verifying with valid identity shows OverlayState Verifying`() {
        val service = YouTubeGuardService()
        service.overlayManager = overlayManager

        val validIdentity = VideoIdentity(
            videoId = "vid_42",
            title = "The Science of Thinking",
            channelHandle = "@veritasium",
            channelName = "Veritasium",
        )

        // Verifying with valid identity must show Verifying overlay
        service.handlePolicyState(PolicyState.Verifying(identity = validIdentity))

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Verifying, got $state", state is OverlayState.Verifying)
        val verifying = state as OverlayState.Verifying
        assertEquals("Veritasium", verifying.channelName)
        assertEquals("The Science of Thinking", verifying.title)
    }
}
