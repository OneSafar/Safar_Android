package com.safarparmar.app.feature.youtube.service

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.cash.turbine.test
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import com.safarparmar.app.feature.youtube.detector.ParsedUiSnapshot
import com.safarparmar.app.feature.youtube.detector.ScreenType
import com.safarparmar.app.feature.youtube.detector.YouTubeUiParser
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.overlay.OverlayState
import com.safarparmar.app.feature.youtube.overlay.YouTubeGuardOverlayManager
import com.safarparmar.app.feature.youtube.policy.BlockReason
import com.safarparmar.app.feature.youtube.policy.PolicyState
import com.safarparmar.app.feature.youtube.policy.VideoIdentity
import com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngine
import com.safarparmar.app.ui.auth.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Adversarial Challenger Test Suite for Milestone 3.
 *
 * Authored by: Milestone 3 Challenger 2
 * Focus: Click interception, rapid click race conditions, Shorts tab vs shelf sync blocking,
 * and channel profile page blocking and overlay lifecycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YouTubeClickInterceptionChallengerTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

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

    // ===========================================================================
    // 1. RAPID CLICK RACE CONDITIONS ON SUGGESTED VIDEO CARDS
    // ===========================================================================

    @Test
    fun `rapid consecutive clicks on suggested video cards are idempotent and keep overlay in Verifying`() {
        val suggestedCard = createNode(viewId = "com.google.android.youtube:id/compact_video_renderer")
        val clickEvent = createClickEvent(suggestedCard)

        // Rapid click burst (10 rapid clicks)
        repeat(10) {
            val handled = service.handleViewClicked(clickEvent)
            assertTrue("Click $it on suggested video card must be handled", handled)
            val state = overlayManager.overlayState.value
            assertTrue("Overlay must stay in Verifying on click $it", state is OverlayState.Verifying)
        }
    }

    @Test
    fun `suggested video click on nested child views traverses parents up to 3 levels`() {
        val compactCard = createNode(viewId = "com.google.android.youtube:id/compact_video_renderer")
        val container = createNode(viewId = "com.google.android.youtube:id/item_layout", parent = compactCard)
        val titleText = createNode(viewId = "com.google.android.youtube:id/video_title", text = "Next Video", parent = container)

        // User clicks the video title inside compact_video_renderer (depth 2)
        val event = createClickEvent(titleText)
        val handled = service.handleViewClicked(event)

        assertTrue("Nested click inside suggested video renderer must be intercepted", handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)
    }

    @Test
    fun `suggested video click followed by intermediate screen transition preserves fail-closed Verifying overlay`() {
        // Step 1: User is watching an Allowed video
        val allowedIdentity = VideoIdentity(
            title = "MIT Physics Lecture",
            channelHandle = "@mitocw",
            channelName = "MIT OpenCourseWare",
        )
        service.handlePolicyState(PolicyState.Allowed(allowedIdentity, "@mitocw"))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // Step 2: User clicks suggested video card
        val suggestedCard = createNode(viewId = "com.google.android.youtube:id/watch_next")
        val clickHandled = service.handleViewClicked(createClickEvent(suggestedCard))
        assertTrue(clickHandled)

        // Synchronous check: overlay is immediately showing Verifying
        val verifyingState = overlayManager.overlayState.value
        assertTrue("Overlay must be showing Verifying immediately after click", verifyingState is OverlayState.Verifying)

        // Step 3: Intermediate window state change arrives during layout inflation with active player
        val playerNode = createNode(viewId = "com.google.android.youtube:id/watch_player")
        val rootNode = createNode(children = listOf(playerNode))
        val intermediateSnapshot = YouTubeUiParser.parse(rootNode)
        assertEquals(ScreenType.WATCH, intermediateSnapshot.screenType)
        assertTrue(intermediateSnapshot.hasActivePlayer)

        // Policy engine receives intermediate content
        policyEngine.onContentDetected(intermediateSnapshot.toCanonicalContent())

        // Verify policy engine entered Verifying
        assertTrue(policyEngine.state.value is PolicyState.Verifying)
        // Overlay must remain in Verifying (fail-closed!)
        val stateAfterTransition = overlayManager.overlayState.value
        assertTrue("Overlay must stay in Verifying during intermediate transition", stateAfterTransition is OverlayState.Verifying)
    }

    @Test
    fun `suggested video click on various suggested containers are all intercepted`() {
        val containerIds = listOf(
            "com.google.android.youtube:id/compact_video_renderer",
            "com.google.android.youtube:id/watch_next",
            "com.google.android.youtube:id/related_videos",
            "com.google.android.youtube:id/compact_link",
            "com.google.android.youtube:id/compact_movie_renderer",
            "com.google.android.youtube:id/compact_promoted_video_renderer",
        )

        for (id in containerIds) {
            val node = createNode(viewId = id)
            val handled = service.handleViewClicked(createClickEvent(node))
            assertTrue("Expected click on $id to be intercepted", handled)
            assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)
        }
    }

    // ===========================================================================
    // 2. SHORTS SHELF VS BOTTOM TAB CLICKS (SYNCHRONOUS BLOCKING)
    // ===========================================================================

    @Test
    fun `bottom tab click synchronously triggers both overlayManager and policyEngine`() {
        val bottomTabVariants = listOf(
            createNode(viewId = "com.google.android.youtube:id/tab_shorts"),
            createNode(viewId = "com.google.android.youtube:id/shorts_tab"),
            createNode(viewId = "com.google.android.youtube:id/reel_tab"),
            createNode(viewId = "com.google.android.youtube:id/pivot_shorts"),
            createNode(contentDescription = "Shorts"),
            createNode(text = "Shorts"),
            createNode(contentDescription = "YouTube Shorts"),
            createNode(contentDescription = "Shorts tab"),
        )

        for ((idx, tabNode) in bottomTabVariants.withIndex()) {
            // Reset state
            overlayManager.hide()
            policyEngine.onPlaybackStopped()

            val event = createClickEvent(tabNode)
            val handled = service.handleViewClicked(event)

            assertTrue("Bottom tab variant $idx must be intercepted", handled)
            assertTrue(
                "Overlay must be ShortsBlocked for variant $idx",
                overlayManager.overlayState.value is OverlayState.ShortsBlocked,
            )
            assertTrue(
                "PolicyEngine must be ShortsBlocked for variant $idx",
                policyEngine.state.value is PolicyState.ShortsBlocked,
            )
        }
    }

    @Test
    fun `shorts shelf click synchronously triggers both overlayManager and policyEngine`() {
        val shelfVariants = listOf(
            createNode(viewId = "com.google.android.youtube:id/shorts_shelf"),
            createNode(viewId = "com.google.android.youtube:id/reel_shelf"),
            createNode(viewId = "com.google.android.youtube:id/reel_item"),
            createNode(viewId = "com.google.android.youtube:id/shorts_card"),
            createNode(viewId = "com.google.android.youtube:id/reel_item_renderer"),
        )

        for ((idx, shelfNode) in shelfVariants.withIndex()) {
            overlayManager.hide()
            policyEngine.onPlaybackStopped()

            val event = createClickEvent(shelfNode)
            val handled = service.handleViewClicked(event)

            assertTrue("Shorts shelf variant $idx must be intercepted", handled)
            assertTrue(
                "Overlay must be ShortsBlocked for variant $idx",
                overlayManager.overlayState.value is OverlayState.ShortsBlocked,
            )
            assertTrue(
                "PolicyEngine must be ShortsBlocked for variant $idx",
                policyEngine.state.value is PolicyState.ShortsBlocked,
            )
        }
    }

    @Test
    fun `deeply nested click inside shorts shelf traverses parents and blocks synchronously`() {
        val shelf = createNode(viewId = "com.google.android.youtube:id/shorts_shelf")
        val item = createNode(viewId = "com.google.android.youtube:id/reel_item", parent = shelf)
        val thumb = createNode(viewId = "com.google.android.youtube:id/thumbnail", parent = item)
        val overlayBadge = createNode(contentDescription = "Short video", parent = thumb)

        // Depth 3 click: user taps overlay badge inside thumbnail inside reel_item inside shorts_shelf
        val event = createClickEvent(overlayBadge)
        val handled = service.handleViewClicked(event)

        assertTrue("Deeply nested click inside shorts shelf must be intercepted", handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
        assertTrue(policyEngine.state.value is PolicyState.ShortsBlocked)
    }

    @Test
    fun `click on parent container with Shorts child is intercepted via child check`() {
        val child = createNode(contentDescription = "Shorts")
        val parent = createNode(children = listOf(child))

        val event = createClickEvent(parent)
        val handled = service.handleViewClicked(event)

        assertTrue("Click on container with Shorts child must be intercepted", handled)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
        assertTrue(policyEngine.state.value is PolicyState.ShortsBlocked)
    }

    @Test
    fun `remix button click is intercepted as Shorts entrypoint`() {
        val remixNodeDesc = createNode(contentDescription = "Remix")
        val handledDesc = service.handleViewClicked(createClickEvent(remixNodeDesc))
        assertTrue("Remix contentDescription click must be intercepted", handledDesc)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)

        overlayManager.hide()
        policyEngine.onPlaybackStopped()

        val remixNodeText = createNode(text = "Remix")
        val handledText = service.handleViewClicked(createClickEvent(remixNodeText))
        assertTrue("Remix text click must be intercepted", handledText)
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
    }

    @Test
    fun `rapid multi-click barrage on Shorts tab remains stable in ShortsBlocked`() {
        val shortsTab = createNode(viewId = "com.google.android.youtube:id/tab_shorts")
        val event = createClickEvent(shortsTab)

        repeat(50) {
            val handled = service.handleViewClicked(event)
            assertTrue("Click $it must be intercepted", handled)
            assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)
            assertTrue(policyEngine.state.value is PolicyState.ShortsBlocked)
        }
    }

    // ===========================================================================
    // 3. CHANNEL PROFILE PAGE BLOCKING & OVERLAY BEHAVIOR
    // ===========================================================================

    @Test
    fun `navigating to DISTRACTING channel profile page triggers OverlayState Blocked`() = runTest {
        val channelId = ChannelIdentity(verifiedHandle = "@distracting_vlogger", displayName = "Distracting Vlogger")
        coEvery { repository.getPolicy(channelId) } returns ChannelPolicy.DISTRACTING
        coEvery { repository.getChannelPolicy("@distracting_vlogger") } returns ChannelPolicy.DISTRACTING

        val testEngine = YouTubePolicyEngine(repository, backgroundScope)
        service.policyEngine = testEngine

        // Construct channel header node
        val titleNode = createNode(viewId = "com.google.android.youtube:id/channel_name", text = "Distracting Vlogger")
        val handleNode = createNode(text = "@distracting_vlogger")
        val headerNode = createNode(
            viewId = "com.google.android.youtube:id/channel_header",
            children = listOf(titleNode, handleNode),
        )
        val rootNode = createNode(children = listOf(headerNode))

        // Parse UI snapshot
        val snapshot = YouTubeUiParser.parse(rootNode)
        assertEquals(ScreenType.CHANNEL_PAGE, snapshot.screenType)
        assertTrue(snapshot.isChannelPage)
        assertEquals(channelId, snapshot.channelIdentity)

        // Observe state transitions
        testEngine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Service handles channel page detection
            testEngine.onContentDetected(snapshot.toCanonicalContent())

            // 1. Must enter Verifying first (fail-closed)
            val verifyingState = awaitItem()
            assertTrue(verifyingState is PolicyState.Verifying)
            service.handlePolicyState(verifyingState)
            assertTrue("Overlay must show Verifying during lookup", overlayManager.overlayState.value is OverlayState.Verifying)

            // 2. Resolves to Blocked after repository lookup
            val blockedState = awaitItem()
            assertTrue(blockedState is PolicyState.Blocked)
            service.handlePolicyState(blockedState)

            val overlayState = overlayManager.overlayState.value
            assertTrue("Overlay must be Blocked for distracting channel page", overlayState is OverlayState.Blocked)
            val blocked = overlayState as OverlayState.Blocked
            assertEquals("Distracting Vlogger", blocked.channelName)
            assertEquals("@distracting_vlogger", blocked.channelHandle)
            assertEquals(BlockReason.DISTRACTING_CHANNEL, blocked.reason)
        }
    }

    @Test
    fun `navigating to PRODUCTIVE channel profile page leaves overlay hidden`() = runTest {
        val channelId = ChannelIdentity(verifiedHandle = "@mitocw", displayName = "MIT OpenCourseWare")
        coEvery { repository.getPolicy(channelId) } returns ChannelPolicy.PRODUCTIVE
        coEvery { repository.getChannelPolicy("@mitocw") } returns ChannelPolicy.PRODUCTIVE

        val testEngine = YouTubePolicyEngine(repository, backgroundScope)
        service.policyEngine = testEngine

        val titleNode = createNode(viewId = "com.google.android.youtube:id/channel_title", text = "MIT OpenCourseWare")
        val handleNode = createNode(text = "@mitocw")
        val headerNode = createNode(
            viewId = "com.google.android.youtube:id/c3_channel_header",
            children = listOf(titleNode, handleNode),
        )
        val rootNode = createNode(children = listOf(headerNode))

        val snapshot = YouTubeUiParser.parse(rootNode)
        assertEquals(ScreenType.CHANNEL_PAGE, snapshot.screenType)

        testEngine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            testEngine.onContentDetected(snapshot.toCanonicalContent())

            // 1. Verifying
            val verifyingState = awaitItem()
            service.handlePolicyState(verifyingState)

            // 2. Allowed
            val allowedState = awaitItem()
            assertTrue(allowedState is PolicyState.Allowed)
            service.handlePolicyState(allowedState)

            // Overlay must be hidden
            assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
        }
    }

    @Test
    fun `navigating to UNKNOWN channel profile page triggers OverlayState AskUser`() = runTest {
        val channelId = ChannelIdentity(verifiedHandle = "@new_creator", displayName = "New Creator")
        coEvery { repository.getPolicy(channelId) } returns null
        coEvery { repository.getChannelPolicy("@new_creator") } returns null

        val testEngine = YouTubePolicyEngine(repository, backgroundScope)
        service.policyEngine = testEngine

        val headerNode = createNode(
            viewId = "com.google.android.youtube:id/channel_header",
            children = listOf(
                createNode(viewId = "com.google.android.youtube:id/channel_name", text = "New Creator"),
                createNode(text = "@new_creator"),
            ),
        )
        val snapshot = YouTubeUiParser.parse(createNode(children = listOf(headerNode)))
        assertEquals(ScreenType.CHANNEL_PAGE, snapshot.screenType)

        testEngine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            testEngine.onContentDetected(snapshot.toCanonicalContent())

            // Verifying
            val verifying = awaitItem()
            service.handlePolicyState(verifying)

            // AskUser
            val askUser = awaitItem()
            assertTrue(askUser is PolicyState.AskUser)
            service.handlePolicyState(askUser)

            val overlayState = overlayManager.overlayState.value
            assertTrue("Overlay must show AskUser for unknown channel page", overlayState is OverlayState.AskUser)
            val userPrompt = overlayState as OverlayState.AskUser
            assertEquals("New Creator", userPrompt.channelName)
            assertEquals("@new_creator", userPrompt.channelHandle)
        }
    }

    @Test
    fun `channel profile page without verified handle looks up by display name canonical key`() = runTest {
        val channelId = ChannelIdentity(verifiedHandle = null, displayName = "Khan Academy Hindi")
        coEvery { repository.getPolicy(channelId) } returns ChannelPolicy.PRODUCTIVE

        val testEngine = YouTubePolicyEngine(repository, backgroundScope)
        service.policyEngine = testEngine

        val headerNode = createNode(
            viewId = "com.google.android.youtube:id/channel_page_header",
            children = listOf(
                createNode(viewId = "com.google.android.youtube:id/channel_name", text = "Khan Academy Hindi"),
            ),
        )
        val snapshot = YouTubeUiParser.parse(createNode(children = listOf(headerNode)))
        assertEquals(ScreenType.CHANNEL_PAGE, snapshot.screenType)
        assertEquals(null, snapshot.channelIdentity.verifiedHandle)
        assertEquals("Khan Academy Hindi", snapshot.channelIdentity.displayName)

        testEngine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())
            testEngine.onContentDetected(snapshot.toCanonicalContent())

            val verifying = awaitItem()
            service.handlePolicyState(verifying)

            val allowed = awaitItem()
            assertTrue(allowed is PolicyState.Allowed)
            service.handlePolicyState(allowed)
            assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
        }
    }

    @Test
    fun `navigating away from Blocked channel profile page back to feed dismisses overlay`() {
        // Given channel page is Blocked
        val channelIdentity = VideoIdentity(
            channelHandle = "@bad_creator",
            channelName = "Bad Creator",
            isChannelPage = true,
        )
        service.handlePolicyState(PolicyState.Blocked(channelIdentity, "@bad_creator", BlockReason.DISTRACTING_CHANNEL))
        assertTrue(overlayManager.overlayState.value is OverlayState.Blocked)

        // When user navigates back to Home Feed (no active video or channel)
        service.handlePolicyState(PolicyState.Verifying(identity = null))

        // Then overlay is dismissed immediately
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    // ===========================================================================
    // 4. ADVERSARIAL EDGE CASES & FALSE POSITIVE STRESS TESTS
    // ===========================================================================

    @Test
    fun `unrelated action buttons do not get intercepted by click handler`() {
        val buttons = listOf(
            createNode(viewId = "com.google.android.youtube:id/subscribe_button", text = "Subscribe"),
            createNode(viewId = "com.google.android.youtube:id/like_button", contentDescription = "Like this video"),
            createNode(viewId = "com.google.android.youtube:id/dislike_button", contentDescription = "Dislike"),
            createNode(viewId = "com.google.android.youtube:id/share_button", contentDescription = "Share"),
            createNode(viewId = "com.google.android.youtube:id/comments_entry_point_layout", contentDescription = "Comments"),
            createNode(viewId = "com.google.android.youtube:id/comment_author", text = "RandomUser"),
            createNode(viewId = "com.google.android.youtube:id/description", text = "Video description"),
        )

        for (btn in buttons) {
            val handled = service.handleViewClicked(createClickEvent(btn))
            assertFalse("Button ${btn.viewIdResourceName ?: btn.contentDescription} must NOT be intercepted", handled)
            assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
        }
    }

    @Test
    fun `corrupted or null event source is handled safely without crashing`() {
        val nullSourceEvent = mockk<AccessibilityEvent>(relaxed = true)
        every { nullSourceEvent.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { nullSourceEvent.packageName } returns "com.google.android.youtube"
        every { nullSourceEvent.source } returns null

        val handled = service.handleViewClicked(nullSourceEvent)
        assertFalse(handled)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `node throwing exception during traversal is handled safely`() {
        val buggyNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { buggyNode.viewIdResourceName } throws RuntimeException("Corrupted node")
        every { buggyNode.contentDescription } returns null
        every { buggyNode.text } returns null
        every { buggyNode.parent } returns null
        every { buggyNode.childCount } returns 0

        val handled = service.handleViewClicked(createClickEvent(buggyNode))
        assertFalse(handled)
    }

    // ===========================================================================
    // Helpers
    // ===========================================================================

    private fun createClickEvent(sourceNode: AccessibilityNodeInfo): AccessibilityEvent {
        val event = mockk<AccessibilityEvent>(relaxed = true)
        every { event.eventType } returns AccessibilityEvent.TYPE_VIEW_CLICKED
        every { event.packageName } returns "com.google.android.youtube"
        every { event.source } returns sourceNode
        return event
    }

    private fun createNode(
        viewId: String? = null,
        text: String? = null,
        contentDescription: String? = null,
        isSelected: Boolean = false,
        parent: AccessibilityNodeInfo? = null,
        children: List<AccessibilityNodeInfo> = emptyList(),
    ): AccessibilityNodeInfo {
        val node = mockk<AccessibilityNodeInfo>(relaxed = true)
        every { node.viewIdResourceName } returns viewId
        every { node.text } returns text
        every { node.contentDescription } returns contentDescription
        every { node.isSelected } returns isSelected
        every { node.parent } returns parent
        every { node.childCount } returns children.size
        children.forEachIndexed { index, child ->
            every { node.getChild(index) } returns child
        }
        return node
    }
}
