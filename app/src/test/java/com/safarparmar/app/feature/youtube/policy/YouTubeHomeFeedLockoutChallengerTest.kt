package com.safarparmar.app.feature.youtube.policy

import app.cash.turbine.test
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.feature.youtube.overlay.OverlayState
import com.safarparmar.app.feature.youtube.overlay.YouTubeGuardOverlayManager
import com.safarparmar.app.feature.youtube.service.YouTubeGuardService
import com.safarparmar.app.ui.auth.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Adversarial Challenger Test Suite for Milestone 2 (Iteration 2).
 *
 * Specifically stress-tests:
 * 1. Cold start transitions: policy engine initialization with Verifying(identity = null)
 *    ensuring the overlay remains Hidden and does NOT lock out YouTube Home/browse feed.
 * 2. Active video transitions: immediate fail-closed OverlayState.Verifying with touch suppression
 *    whenever a video is detected, before resolving to Allowed, Blocked, ShortsBlocked, or AskUser.
 * 3. Playback stop transitions: navigation back to Home feed or stopping playback immediately
 *    hides the overlay (OverlayState.Hidden) and unblocks user touches.
 * 4. Rapid switching into/out of Home feed: rapid interleaving of video detection and playback
 *    stopped events without state desynchronization or race conditions.
 * 5. Stale asynchronous DB lookups: slow DB lookups from a closed video discarded after
 *    returning to Home feed without popping up unrequested blocking or classification scrims.
 * 6. Edge cases: Ad transitions returning to Home feed, empty identities, Shorts exit to Home feed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YouTubeHomeFeedLockoutChallengerTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeYouTubeChannelRepository
    private lateinit var engine: YouTubePolicyEngine
    private lateinit var overlayManager: YouTubeGuardOverlayManager
    private lateinit var service: YouTubeGuardService

    @Before
    fun setUp() {
        fakeRepo = FakeYouTubeChannelRepository()
        overlayManager = YouTubeGuardOverlayManager()
        service = YouTubeGuardService()
        service.overlayManager = overlayManager
    }

    private fun setupConnectedPipeline(engine: YouTubePolicyEngine) {
        service.policyEngine = engine
    }

    // =========================================================================
    // 1. COLD START & HOME FEED IDLE INVARIANTS
    // =========================================================================

    @Test
    fun `challenger - cold start initializes in Hidden overlay state with zero lockout on home feed`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        // Simulate service observing policy state on connect
        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // On cold start, engine state is Verifying(null)
        assertEquals(PolicyState.Verifying(null), engine.state.value)

        // Overlay MUST be Hidden — never Verifying or Blocked
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
        job.cancel()
    }

    @Test
    fun `challenger - repeated home feed idle events keep overlay strictly Hidden`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // User scrolling home feed: onPlaybackStopped called repeatedly
        repeat(5) {
            engine.onPlaybackStopped()
            assertEquals(PolicyState.Verifying(null), engine.state.value)
            assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
        }
        job.cancel()
    }

    // =========================================================================
    // 2. ACTIVE VIDEO TRANSITIONS & FAIL-CLOSED ENFORCEMENT
    // =========================================================================

    @Test
    fun `challenger - launching productive video enforces Verifying overlay before resolving to Hidden`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE, "Veritasium")
        fakeRepo.enableDelay(100)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        val identity = VideoIdentity(
            videoId = "v_101",
            title = "The Science of Thinking",
            channelHandle = "@veritasium",
            channelName = "Veritasium",
        )

        // Trigger video detection
        engine.onVideoDetected(identity)
        runCurrent()

        // Invariant: Immediately in Verifying state with non-null identity
        val currentOverlay = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Verifying, got $currentOverlay", currentOverlay is OverlayState.Verifying)
        val verifying = currentOverlay as OverlayState.Verifying
        assertEquals("Veritasium", verifying.channelName)
        assertEquals("The Science of Thinking", verifying.title)

        // During the 100ms DB lookup delay, overlay MUST remain Verifying (fail-closed)
        advanceTimeBy(50)
        runCurrent()
        assertTrue("Overlay must remain Verifying during lookup", overlayManager.overlayState.value is OverlayState.Verifying)

        // Complete DB lookup
        advanceTimeBy(60)
        runCurrent()

        // Resolved to Allowed -> overlay must now be Hidden
        assertTrue("Policy state must be Allowed", engine.state.value is PolicyState.Allowed)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    @Test
    fun `challenger - launching distracting video enforces Verifying overlay before resolving to Blocked`() = runTest {
        fakeRepo.setChannelPolicy("@distracting", ChannelPolicy.DISTRACTING, "Distracting Media")
        fakeRepo.enableDelay(100)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        val identity = VideoIdentity(
            videoId = "v_bad",
            title = "Endless Scrolling",
            channelHandle = "@distracting",
            channelName = "Distracting Media",
        )

        engine.onVideoDetected(identity)
        runCurrent()

        // Fail-closed verifying overlay immediately
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        // Advance past DB lookup
        advanceTimeBy(110)
        runCurrent()

        // Resolved to Blocked -> overlay must be Blocked
        val blockedState = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Blocked, got $blockedState", blockedState is OverlayState.Blocked)
        val blocked = blockedState as OverlayState.Blocked
        assertEquals("Distracting Media", blocked.channelName)
        assertEquals("@distracting", blocked.channelHandle)

        job.cancel()
    }

    @Test
    fun `challenger - launching unknown video enforces Verifying overlay before resolving to AskUser`() = runTest {
        // Unknown channel with 50ms delay
        fakeRepo.enableDelay(50)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        val identity = VideoIdentity(
            videoId = "v_unknown",
            title = "Indie Creator Vlog",
            channelHandle = "@indie_dev",
            channelName = "Indie Dev",
        )

        engine.onVideoDetected(identity)
        runCurrent()

        // Immediately Verifying
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        advanceTimeBy(60)
        runCurrent()

        // Resolved to AskUser
        val askState = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.AskUser, got $askState", askState is OverlayState.AskUser)
        val askUser = askState as OverlayState.AskUser
        assertEquals("Indie Dev", askUser.channelName)
        assertEquals("@indie_dev", askUser.channelHandle)

        job.cancel()
    }

    // =========================================================================
    // 3. PLAYBACK STOP TRANSITIONS (HOME FEED RETURN)
    // =========================================================================

    @Test
    fun `challenger - navigating from Allowed video back to Home feed hides overlay immediately`() = runTest {
        fakeRepo.setChannelPolicy("@allowed", ChannelPolicy.PRODUCTIVE, "Allowed Channel")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // 1. Play video
        engine.onVideoDetected(VideoIdentity(title = "Good Video", channelHandle = "@allowed", channelName = "Allowed Channel"))
        advanceTimeBy(10)
        runCurrent()
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 2. User presses back to Home feed (playback stopped)
        engine.onPlaybackStopped()
        runCurrent()

        // Engine emits Verifying(null) -> handlePolicyState hides overlay
        assertEquals(PolicyState.Verifying(null), engine.state.value)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    @Test
    fun `challenger - navigating from Blocked video back to Home feed immediately dismisses blocked overlay`() = runTest {
        fakeRepo.setChannelPolicy("@blocked", ChannelPolicy.DISTRACTING, "Blocked Channel")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // 1. Enter blocked video
        engine.onVideoDetected(VideoIdentity(title = "Bad Video", channelHandle = "@blocked", channelName = "Blocked Channel"))
        advanceTimeBy(10)
        runCurrent()
        assertTrue(overlayManager.overlayState.value is OverlayState.Blocked)

        // 2. User taps Go Back / navigates back to Home feed
        engine.onPlaybackStopped()
        runCurrent()

        // Overlay MUST be Hidden, not stuck in Blocked
        assertEquals(PolicyState.Verifying(null), engine.state.value)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    @Test
    fun `challenger - navigating from Shorts back to Home feed immediately dismisses shorts overlay`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // 1. Open Shorts
        engine.onVideoDetected(VideoIdentity(title = "Short 1", channelHandle = "@shorts_creator", isShorts = true))
        advanceTimeBy(10)
        runCurrent()
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)

        // 2. Exit Shorts to Home feed
        engine.onPlaybackStopped()
        runCurrent()

        // Overlay MUST be Hidden
        assertEquals(PolicyState.Verifying(null), engine.state.value)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    // =========================================================================
    // 4. STALE DB LOOKUP PROTECTION (CANCELLATION & DISCARD)
    // =========================================================================

    @Test
    fun `challenger - slow DB lookup finishing after user returns to Home feed is discarded and does not hijack screen`() = runTest {
        // Setup a slow 300ms DB lookup for a distracting channel
        fakeRepo.setChannelPolicy("@distracting", ChannelPolicy.DISTRACTING, "Distracting Media")
        fakeRepo.enableDelay(300)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // 1. User starts watching Video
        engine.onVideoDetected(VideoIdentity(title = "Slow Video", channelHandle = "@distracting", channelName = "Distracting Media"))
        runCurrent()
        assertTrue("Overlay must enter Verifying", overlayManager.overlayState.value is OverlayState.Verifying)

        // 2. User quickly exits back to Home feed after 50ms (before DB returns)
        advanceTimeBy(50)
        engine.onPlaybackStopped()
        runCurrent()

        // Home feed: overlay is Hidden
        assertEquals(PolicyState.Verifying(null), engine.state.value)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 3. Advance clock past the original 300ms lookup duration
        advanceTimeBy(300)
        runCurrent()

        // INVARIANT: The delayed lookup must NOT have overwritten state or shown Blocked overlay!
        assertEquals("Engine state must remain Verifying(null)", PolicyState.Verifying(null), engine.state.value)
        assertEquals("Overlay must remain Hidden on home feed", OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    @Test
    fun `challenger - slow AskUser DB lookup finishing after playback stopped does not show classification popup on home feed`() = runTest {
        // Unknown channel with slow 200ms lookup
        fakeRepo.enableDelay(200)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        engine.onVideoDetected(VideoIdentity(title = "Unknown", channelHandle = "@unknown", channelName = "Unknown"))
        runCurrent()
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        advanceTimeBy(50)
        // User backs out
        engine.onPlaybackStopped()
        runCurrent()
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // Wait for slow lookup
        advanceTimeBy(250)
        runCurrent()

        // Must still be Hidden
        assertEquals(PolicyState.Verifying(null), engine.state.value)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    // =========================================================================
    // 5. RAPID FLAPPING & INTERLEAVED VIDEO / HOME FEED HOPS
    // =========================================================================

    @Test
    fun `challenger - rapid flapping between video and home feed preserves correct overlay state at every transition`() = runTest {
        fakeRepo.setChannelPolicy("@productive", ChannelPolicy.PRODUCTIVE, "Productive Channel")
        fakeRepo.setChannelPolicy("@distracting", ChannelPolicy.DISTRACTING, "Distracting Channel")
        fakeRepo.enableDelay(50)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        val productive = VideoIdentity(title = "Math Lesson", channelHandle = "@productive", channelName = "Productive Channel")
        val distracting = VideoIdentity(title = "Gossip", channelHandle = "@distracting", channelName = "Distracting Channel")

        // 10 rapid cycles of: Video -> Home -> Video -> Home
        for (i in 1..10) {
            val video = if (i % 2 == 0) productive else distracting
            val isProductive = (i % 2 == 0)

            // 1. Enter video
            engine.onVideoDetected(video)
            runCurrent()
            // Immediately Verifying (fail-closed while DB query is pending)
            assertTrue("Cycle $i: immediate Verifying", overlayManager.overlayState.value is OverlayState.Verifying)

            // Advance time to allow DB lookup to resolve
            advanceTimeBy(60)
            runCurrent()
            if (isProductive) {
                assertEquals("Cycle $i: resolved to Hidden", OverlayState.Hidden, overlayManager.overlayState.value)
            } else {
                assertTrue("Cycle $i: resolved to Blocked", overlayManager.overlayState.value is OverlayState.Blocked)
            }

            // 2. Return to Home feed
            engine.onPlaybackStopped()
            runCurrent()
            assertEquals("Cycle $i: home feed must be Hidden", OverlayState.Hidden, overlayManager.overlayState.value)
            assertEquals("Cycle $i: engine must be Verifying(null)", PolicyState.Verifying(null), engine.state.value)
        }

        job.cancel()
    }

    // =========================================================================
    // 6. ADVERTISEMENTS & HOME FEED TRANSITIONS
    // =========================================================================

    @Test
    fun `challenger - exiting pre-roll ad to home feed immediately clears verifying overlay`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // Video switch into pre-roll ad on cold start
        val adIdentity = VideoIdentity(
            title = "Car Commercial",
            channelHandle = "@car_maker",
            channelName = "Car Maker",
            isAd = true,
            isNewVideoSwitch = true,
        )

        engine.onVideoDetected(adIdentity)
        runCurrent()
        advanceTimeBy(10)
        runCurrent()

        // Pre-roll ad remains in Verifying (fail-closed)
        assertTrue("Pre-roll ad must stay Verifying", overlayManager.overlayState.value is OverlayState.Verifying)

        // User closes ad/video and returns to Home feed
        engine.onPlaybackStopped()
        runCurrent()

        // Overlay MUST hide
        assertEquals(PolicyState.Verifying(null), engine.state.value)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    @Test
    fun `challenger - mid-roll ad on productive video followed by home feed exit leaves overlay hidden`() = runTest {
        fakeRepo.setChannelPolicy("@productive", ChannelPolicy.PRODUCTIVE, "Productive Channel")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        // 1. Play productive video
        val parentVideo = VideoIdentity(title = "Physics 101", channelHandle = "@productive", channelName = "Productive Channel")
        engine.onVideoDetected(parentVideo)
        advanceTimeBy(10)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 2. Mid-roll ad starts (isNewVideoSwitch = false)
        val midRollAd = VideoIdentity(title = "Drink Coke", channelHandle = "@coca_cola", channelName = "Coca Cola", isAd = true, isNewVideoSwitch = false)
        engine.onVideoDetected(midRollAd)
        advanceTimeBy(10)
        // Mid-roll ad retains Allowed
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 3. User closes to Home feed
        engine.onPlaybackStopped()
        assertEquals(PolicyState.Verifying(null), engine.state.value)
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    // =========================================================================
    // 7. DIRECT HANDLEPOLICYSTATE MATRIX VERIFICATION
    // =========================================================================

    @Test
    fun `challenger - handlePolicyState complete matrix transitions`() {
        val dummyIdentity = VideoIdentity(title = "T", channelHandle = "@h", channelName = "N")

        // 1. Verifying with null -> Hidden
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 2. Verifying with identity -> Verifying
        service.handlePolicyState(PolicyState.Verifying(identity = dummyIdentity))
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        // 3. Allowed -> Hidden
        service.handlePolicyState(PolicyState.Allowed(dummyIdentity, "@h"))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 4. Blocked -> Blocked
        service.handlePolicyState(PolicyState.Blocked(dummyIdentity, "@h", BlockReason.DISTRACTING_CHANNEL))
        assertTrue(overlayManager.overlayState.value is OverlayState.Blocked)

        // 5. Back to Verifying with null -> Hidden
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 6. ShortsBlocked -> ShortsBlocked
        service.handlePolicyState(PolicyState.ShortsBlocked(dummyIdentity))
        assertTrue(overlayManager.overlayState.value is OverlayState.ShortsBlocked)

        // 7. Back to Verifying with null -> Hidden
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 8. AskUser -> AskUser
        service.handlePolicyState(PolicyState.AskUser(dummyIdentity, "@h"))
        assertTrue(overlayManager.overlayState.value is OverlayState.AskUser)

        // 9. Back to Verifying with null -> Hidden
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    // =========================================================================
    // 8. ADDITIONAL ADVERSARIAL EDGE CASES
    // =========================================================================

    @Test
    fun `challenger - rapid video hopping without returning to home feed strictly honors latest video`() = runTest {
        // Video A (Allowed, slow 200ms DB) immediately followed by Video B (Distracting, fast 10ms DB)
        fakeRepo.setChannelPolicy("@slow_allowed", ChannelPolicy.PRODUCTIVE, "Slow Allowed")
        fakeRepo.setChannelPolicy("@fast_blocked", ChannelPolicy.DISTRACTING, "Fast Blocked")
        fakeRepo.enableDelay(200)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        val videoA = VideoIdentity(title = "Video A", channelHandle = "@slow_allowed", channelName = "Slow Allowed")
        val videoB = VideoIdentity(title = "Video B", channelHandle = "@fast_blocked", channelName = "Fast Blocked")

        // 1. Launch Video A
        engine.onVideoDetected(videoA)
        runCurrent()
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        // 2. Before Video A resolves, user clicks Video B
        advanceTimeBy(50)
        fakeRepo.enableDelay(10)
        engine.onVideoDetected(videoB)
        runCurrent()
        assertTrue("Must be Verifying for Video B", overlayManager.overlayState.value is OverlayState.Verifying)

        // 3. Allow Video B to resolve (10ms)
        advanceTimeBy(15)
        runCurrent()
        assertTrue("Must resolve to Blocked for Video B", overlayManager.overlayState.value is OverlayState.Blocked)

        // 4. Advance past Video A's original delay
        advanceTimeBy(300)
        runCurrent()

        // Video A's result must be discarded: overlay must remain Blocked for Video B, NOT Allowed
        assertTrue("Overlay must remain Blocked for Video B", overlayManager.overlayState.value is OverlayState.Blocked)

        // 5. Exit Video B to Home feed
        engine.onPlaybackStopped()
        runCurrent()
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    @Test
    fun `challenger - classifying channel on active video transitions to Allowed and hides overlay, returning to home feed keeps it hidden`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)
        setupConnectedPipeline(engine)

        val job = engine.state
            .onEach { service.handlePolicyState(it) }
            .launchIn(backgroundScope)

        val unknownVideo = VideoIdentity(title = "Unknown Tech", channelHandle = "@tech_creator", channelName = "Tech Creator")

        // 1. Unknown video detected -> enters Verifying then AskUser
        engine.onVideoDetected(unknownVideo)
        runCurrent()
        advanceTimeBy(10)
        runCurrent()
        assertTrue(overlayManager.overlayState.value is OverlayState.AskUser)

        // 2. User classifies as PRODUCTIVE
        engine.onUserClassified("@tech_creator", ChannelPolicy.PRODUCTIVE)
        runCurrent()
        advanceTimeBy(10)
        runCurrent()

        // Now Allowed -> overlay hides
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 3. User navigates back to Home feed -> overlay remains Hidden
        engine.onPlaybackStopped()
        runCurrent()
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        job.cancel()
    }

    @Test
    fun `challenger - empty fields in VideoIdentity still enforce fail-closed Verifying overlay`() {
        val blankIdentity = VideoIdentity(title = "", channelHandle = "", channelName = "")

        // When a video is detected even with blank fields (e.g. title not yet parsed),
        // it must still show Verifying overlay (fail-closed)
        service.handlePolicyState(PolicyState.Verifying(identity = blankIdentity))

        val state = overlayManager.overlayState.value
        assertTrue("Expected OverlayState.Verifying, got $state", state is OverlayState.Verifying)
        val verifying = state as OverlayState.Verifying
        assertEquals("", verifying.channelName)
        assertEquals("", verifying.title)

        // But when identity is null (no video), overlay must hide
        service.handlePolicyState(PolicyState.Verifying(identity = null))
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `challenger - repeated rapid hide calls are idempotent and do not fail`() {
        overlayManager.showVerifying("Channel", "Title")
        assertTrue(overlayManager.overlayState.value is OverlayState.Verifying)

        // Rapid multiple hides (e.g. service disconnect, playback stop, window focus change)
        repeat(10) {
            overlayManager.hide()
            assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
        }
    }
}

