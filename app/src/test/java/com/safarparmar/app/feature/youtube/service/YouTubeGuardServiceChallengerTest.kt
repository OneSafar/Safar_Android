package com.safarparmar.app.feature.youtube.service

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import com.safarparmar.app.feature.youtube.overlay.OverlayState
import com.safarparmar.app.feature.youtube.overlay.YouTubeGuardOverlayManager
import com.safarparmar.app.feature.youtube.policy.PolicyState
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
 * Adversarial stress test suite for YouTubeGuardService click interception and policy routing.
 *
 * Authored by: Milestone 3 Challenger 1
 */
class YouTubeGuardServiceChallengerTest {

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
    fun `click interception - null source does not crash and returns false`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns null

        val handled = service.handleViewClicked(event)

        assertFalse("handleViewClicked must return false when source is null", handled)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `click interception - deeply nested icon inside Shorts tab triggers ShortsBlocked`() {
        // Tree: tab_shorts (grandparent) -> tab_layout (parent) -> icon_view (source)
        val tabShorts = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { tabShorts.viewIdResourceName } returns "com.google.android.youtube:id/tab_shorts"
        every { tabShorts.parent } returns null

        val tabLayout = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { tabLayout.parent } returns tabShorts

        val iconView = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { iconView.parent } returns tabLayout

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns iconView

        val handled = service.handleViewClicked(event)

        assertTrue("handleViewClicked must walk up parent hierarchy and detect Shorts tab", handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
        assertTrue(policyEngine.state.value is PolicyState.ShortsBlocked)
    }

    @Test
    fun `click interception - child inside Shorts shelf card triggers ShortsBlocked`() {
        // Tree: shelf_card (source) -> child thumbnail
        val thumbnailChild = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { thumbnailChild.viewIdResourceName } returns "com.google.android.youtube:id/shorts_card"

        val shelfContainer = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { shelfContainer.childCount } returns 1
        every { shelfContainer.getChild(0) } returns thumbnailChild

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns shelfContainer

        val handled = service.handleViewClicked(event)

        assertTrue("handleViewClicked must detect Shorts card in child hierarchy", handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
        assertTrue(policyEngine.state.value is PolicyState.ShortsBlocked)
    }

    @Test
    fun `click interception - suggested video promoted renderer triggers Verifying overlay`() {
        val promotedNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { promotedNode.viewIdResourceName } returns "com.google.android.youtube:id/compact_promoted_video_renderer"

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns promotedNode

        val handled = service.handleViewClicked(event)

        assertTrue(handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)
    }

    @Test
    fun `onAccessibilityEvent - outside package ignored completely`() {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.android.systemui"

        service.onAccessibilityEvent(event)

        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `click interception - Remix button clicked immediately blocks Shorts`() {
        val remixNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { remixNode.contentDescription } returns "Remix"

        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns remixNode

        val handled = service.handleViewClicked(event)

        assertTrue(handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
        assertTrue(policyEngine.state.value is PolicyState.ShortsBlocked)
    }
}
