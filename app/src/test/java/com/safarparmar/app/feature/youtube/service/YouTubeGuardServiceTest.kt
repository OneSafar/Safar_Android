package com.safarparmar.app.feature.youtube.service

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import com.safarparmar.app.feature.youtube.overlay.OverlayState
import com.safarparmar.app.feature.youtube.overlay.YouTubeGuardOverlayManager
import com.safarparmar.app.feature.youtube.policy.BlockReason
import com.safarparmar.app.feature.youtube.policy.PolicyState
import com.safarparmar.app.feature.youtube.policy.VideoIdentity
import com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite for [YouTubeGuardService] policy state dispatching and click interception.
 *
 * Verifies:
 * 1. Policy state handling: Verifying with null vs non-null identity, Allowed, Blocked, ShortsBlocked, AskUser.
 * 2. TYPE_VIEW_CLICKED interception on bottom nav Shorts tab and Shorts shelf items.
 * 3. TYPE_VIEW_CLICKED interception on suggested video cards.
 * 4. Channel profile page blocking when classified as DISTRACTING.
 */
class YouTubeGuardServiceTest {

    private lateinit var service: YouTubeGuardService
    private lateinit var overlayManager: YouTubeGuardOverlayManager
    private lateinit var policyEngine: YouTubePolicyEngine
    private lateinit var repository: YouTubeChannelRepository

    @Before
    fun setUp() {
        service = YouTubeGuardService()
        overlayManager = YouTubeGuardOverlayManager()
        service.overlayManager = overlayManager

        repository = mockk(relaxed = true)
        policyEngine = YouTubePolicyEngine(
            repository = repository,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        service.policyEngine = policyEngine
    }

    @Test
    fun `handlePolicyState Verifying with null identity hides overlay`() {
        // Given an overlay that was previously active
        overlayManager.showVerifying("Previous Channel", "Previous Title")
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        // When a Verifying state with null identity arrives (cold start / playback stopped)
        service.handlePolicyState(PolicyState.Verifying(identity = null))

        // Then the overlay must be hidden to prevent locking out home/browse feed
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `handlePolicyState Verifying with valid identity shows verifying overlay`() {
        val identity = VideoIdentity(
            videoId = "v_123",
            title = "The Science of Thinking",
            channelHandle = "@veritasium",
            channelName = "Veritasium",
        )

        // When Verifying state with valid identity arrives
        service.handlePolicyState(PolicyState.Verifying(identity = identity))

        // Then overlay must be in Verifying state with title and channel name
        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Verifying, got $state", state is OverlayState.Verifying)
        val verifying = state as OverlayState.Verifying
        assertEquals("Veritasium", verifying.channelName)
        assertEquals("The Science of Thinking", verifying.title)
    }

    @Test
    fun `cold start transition with initial Verifying null identity leaves overlay hidden`() {
        // Initial state before any video is detected is Verifying(identity = null)
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `playback stopped transition with Verifying null identity dismisses verifying overlay`() {
        val identity = VideoIdentity(
            videoId = "v_456",
            title = "Calculus 1",
            channelHandle = "@3blue1brown",
            channelName = "3Blue1Brown",
        )

        // 1. Video undergoing verification
        service.handlePolicyState(PolicyState.Verifying(identity = identity))
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        // 2. Playback stopped (user returns to home feed or closes video)
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `handlePolicyState Allowed hides overlay`() {
        val identity = VideoIdentity(
            title = "Allowed Video",
            channelHandle = "@allowed",
            channelName = "Allowed Channel",
        )
        overlayManager.showVerifying("Allowed Channel", "Allowed Video")

        service.handlePolicyState(PolicyState.Allowed(identity = identity, channelHandle = "@allowed"))

        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `handlePolicyState Blocked shows blocked overlay`() {
        val identity = VideoIdentity(
            title = "Distracting Video",
            channelHandle = "@distracting",
            channelName = "Distracting Channel",
        )

        service.handlePolicyState(
            PolicyState.Blocked(
                identity = identity,
                channelHandle = "@distracting",
                reason = BlockReason.DISTRACTING_CHANNEL,
            )
        )

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Blocked, got $state", state is OverlayState.Blocked)
        val blocked = state as OverlayState.Blocked
        assertEquals("Distracting Channel", blocked.channelName)
        assertEquals("@distracting", blocked.channelHandle)
        assertEquals(BlockReason.DISTRACTING_CHANNEL, blocked.reason)
    }

    @Test
    fun `handlePolicyState ShortsBlocked shows shorts blocked overlay`() {
        val shortsIdentity = VideoIdentity(
            title = "Viral Short",
            channelHandle = "@shorts_creator",
            isShorts = true,
        )
        service.handlePolicyState(PolicyState.ShortsBlocked(identity = shortsIdentity))

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.ShortsBlocked, got $state", state is OverlayState.ShortsBlocked)
    }

    @Test
    fun `handlePolicyState AskUser shows ask user overlay`() {
        val identity = VideoIdentity(
            title = "Unknown Video",
            channelHandle = "@unknown",
            channelName = "Unknown Channel",
        )

        service.handlePolicyState(
            PolicyState.AskUser(
                identity = identity,
                channelHandle = "@unknown",
            )
        )

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.AskUser, got $state", state is OverlayState.AskUser)
        val askUser = state as OverlayState.AskUser
        assertEquals("Unknown Channel", askUser.channelName)
        assertEquals("@unknown", askUser.channelHandle)
    }

    // ---------------------------------------------------------------------------
    // Phase 7: TYPE_VIEW_CLICKED Interception Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `handleViewClicked on bottom nav Shorts tab immediately blocks Shorts and notifies engine`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.viewIdResourceName } returns "com.google.android.youtube:id/tab_shorts"
        every { node.contentDescription } returns "Shorts"

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns node

        val handled = service.handleViewClicked(event)

        assertTrue("handleViewClicked must return true for Shorts tab click", handled)
        val overlayState = overlayManager.overlayState.value
        assertTrue("Overlay must immediately transition to ShortsBlocked", overlayState is OverlayState.ShortsBlocked)
        assertTrue("Policy engine must enter ShortsBlocked", policyEngine.state.value is PolicyState.ShortsBlocked)
    }

    @Test
    fun `handleViewClicked on Shorts shelf card immediately blocks Shorts and notifies engine`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.viewIdResourceName } returns "com.google.android.youtube:id/shorts_shelf"

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns node

        val handled = service.handleViewClicked(event)

        assertTrue(handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
        assertTrue(policyEngine.state.value is PolicyState.ShortsBlocked)
    }

    @Test
    fun `handleViewClicked on suggested video card immediately shows Verifying overlay`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.viewIdResourceName } returns "com.google.android.youtube:id/compact_video_renderer"

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns node

        val handled = service.handleViewClicked(event)

        assertTrue("handleViewClicked must return true for suggested video click", handled)
        assertTrue("Overlay must immediately show Verifying overlay", overlayManager.overlayState.value is OverlayState.Verifying)
    }

    @Test
    fun `handleViewClicked on unrelated view does not intercept`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.viewIdResourceName } returns "com.google.android.youtube:id/subscribe_button"
        every { node.contentDescription } returns "Subscribe"

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns node

        val handled = service.handleViewClicked(event)

        assertFalse("handleViewClicked must return false for unrelated button", handled)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `onAccessibilityEvent TYPE_VIEW_CLICKED on Shorts tab intercepts before layout inflation`() {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.contentDescription } returns "Shorts"

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns node

        service.onAccessibilityEvent(event)

        assertTrue("Overlay must show ShortsBlocked", overlayManager.overlayState.value is OverlayState.ShortsBlocked)
        assertTrue("Engine must enter ShortsBlocked", policyEngine.state.value is PolicyState.ShortsBlocked)
    }

    // ---------------------------------------------------------------------------
    // Phase 15: Channel Profile Page Tests
    // ---------------------------------------------------------------------------

    @Test
    fun `handlePolicyState Blocked on channel profile page shows blocked overlay`() {
        val channelIdentity = VideoIdentity(
            channelHandle = "@distracting_creator",
            channelName = "Distracting Creator",
            isChannelPage = true,
        )

        service.handlePolicyState(
            PolicyState.Blocked(
                identity = channelIdentity,
                channelHandle = "@distracting_creator",
                reason = BlockReason.DISTRACTING_CHANNEL,
            )
        )

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Blocked on channel profile page", state is OverlayState.Blocked)
        val blocked = state as OverlayState.Blocked
        assertEquals("Distracting Creator", blocked.channelName)
        assertEquals("@distracting_creator", blocked.channelHandle)
        assertEquals(BlockReason.DISTRACTING_CHANNEL, blocked.reason)
    }

    @Test
    fun `handlePolicyState Allowed on channel profile page leaves overlay hidden`() {
        val channelIdentity = VideoIdentity(
            channelHandle = "@productive_creator",
            channelName = "Productive Creator",
            isChannelPage = true,
        )

        service.handlePolicyState(
            PolicyState.Allowed(
                identity = channelIdentity,
                channelHandle = "@productive_creator",
            )
        )

        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }
}
