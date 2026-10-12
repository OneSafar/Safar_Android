package com.safarparmar.app.feature.youtube.policy

import app.cash.turbine.test
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import com.safarparmar.app.feature.youtube.model.CanonicalYouTubeContent
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.DetectionConfidence
import com.safarparmar.app.feature.youtube.model.VideoFingerprint
import com.safarparmar.app.ui.auth.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit test suite for [YouTubePolicyEngine] verifying all deterministic state transitions
 * and core security/integrity invariants specified in Milestone 1.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YouTubePolicyEngineTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeYouTubeChannelRepository
    private lateinit var engine: YouTubePolicyEngine

    @Before
    fun setUp() {
        fakeRepo = FakeYouTubeChannelRepository()
    }

    // =========================================================================
    // INVARIANT 1: Default to 'Verifying' Immediately on Any Video Identity Change
    // =========================================================================

    @Test
    fun `new video identity always transitions to Verifying before resolving to Allowed`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE, "Veritasium")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val identity = VideoIdentity(
            videoId = "vid_101",
            title = "The Science of Thinking",
            channelHandle = "@veritasium",
            channelName = "Veritasium",
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            // Initial state upon engine creation
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(identity)

            // Step 1: Immediate emission of Verifying with identity (Invariant 1)
            val verifyingState = awaitItem()
            assertTrue("Expected Verifying on identity change, got $verifyingState", verifyingState is PolicyState.Verifying)
            assertEquals(identity, (verifyingState as PolicyState.Verifying).identity)

            // Step 2: Resolves to Allowed after repository lookup
            val allowedState = awaitItem()
            assertTrue("Expected Allowed after policy lookup, got $allowedState", allowedState is PolicyState.Allowed)
            val allowed = allowedState as PolicyState.Allowed
            assertEquals("@veritasium", allowed.channelHandle)
            assertEquals(identity, allowed.identity)
        }
    }

    @Test
    fun `new video identity always transitions to Verifying before resolving to Blocked`() = runTest {
        fakeRepo.setChannelPolicy("@distracting_hub", ChannelPolicy.DISTRACTING, "Gaming Distractions")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val identity = VideoIdentity(
            videoId = "vid_102",
            title = "Endless Memes",
            channelHandle = "@distracting_hub",
            channelName = "Gaming Distractions",
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(identity)

            // Step 1: Must enter Verifying first
            val verifyingState = awaitItem()
            assertTrue("Expected Verifying first, got $verifyingState", verifyingState is PolicyState.Verifying)
            assertEquals(identity, (verifyingState as PolicyState.Verifying).identity)

            // Step 2: Resolves to Blocked
            val blockedState = awaitItem()
            assertTrue("Expected Blocked after policy lookup, got $blockedState", blockedState is PolicyState.Blocked)
            val blocked = blockedState as PolicyState.Blocked
            assertEquals("@distracting_hub", blocked.channelHandle)
            assertEquals(BlockReason.DISTRACTING_CHANNEL, blocked.reason)
            assertEquals(identity, blocked.identity)
        }
    }

    @Test
    fun `unclassified video identity transitions to Verifying before resolving to AskUser`() = runTest {
        // Channel has no policy in repository
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val identity = VideoIdentity(
            videoId = "vid_103",
            title = "New Unclassified Lecture",
            channelHandle = "@new_lecturer",
            channelName = "Professor X",
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(identity)

            val verifyingState = awaitItem()
            assertTrue("Expected Verifying first, got $verifyingState", verifyingState is PolicyState.Verifying)

            val askUserState = awaitItem()
            assertTrue("Expected AskUser for unclassified channel, got $askUserState", askUserState is PolicyState.AskUser)
            val askUser = askUserState as PolicyState.AskUser
            assertEquals("@new_lecturer", askUser.channelHandle)
            assertEquals(identity, askUser.identity)
        }
    }

    @Test
    fun `slow repository lookup keeps engine in Verifying state fail-closed`() = runTest {
        fakeRepo.enableDelay(1000)
        fakeRepo.setChannelPolicy("@slow_channel", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val identity = VideoIdentity(
            videoId = "vid_slow",
            title = "Slow Loading Video",
            channelHandle = "@slow_channel",
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(identity)

            val verifyingState = awaitItem()
            assertTrue(verifyingState is PolicyState.Verifying)
            // While lookup is pending in the background, state remains Verifying
            assertTrue(engine.state.value is PolicyState.Verifying)

            // Advance virtual time to complete delayed lookup
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(1001)
            val allowedState = awaitItem()
            assertTrue("Resolves to Allowed once repository finishes", allowedState is PolicyState.Allowed)
        }
    }

    @Test
    fun `duplicate detection of identical video does not retrigger Verifying transition`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val identity = VideoIdentity(
            videoId = "vid_dedupe",
            title = "Constant Video",
            channelHandle = "@veritasium",
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(identity)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Dispatch identical video detection event (same videoId, title, handle)
            engine.onVideoDetected(identity)

            // Expect no new emissions; state remains Allowed without flicking back to Verifying
            expectNoEvents()
        }
    }

    @Test
    fun `empty or missing channel handle fails closed and remains in Verifying state`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val unparsedIdentity = VideoIdentity(
            videoId = "vid_blank_handle",
            title = "Loading...",
            channelHandle = "", // Handle not yet extracted by Accessibility parser
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(unparsedIdentity)

            val verifyingState = awaitItem()
            assertTrue("Must transition to Verifying with identity", verifyingState is PolicyState.Verifying)
            assertEquals(unparsedIdentity, (verifyingState as PolicyState.Verifying).identity)

            // Verify it does NOT evaluate to Allowed or AskUser when handle is empty
            expectNoEvents()
            assertTrue(engine.state.value is PolicyState.Verifying)
        }
    }

    // =========================================================================
    // INVARIANT 2: Shorts Evaluated and Blocked Independently of Channel Policy
    // =========================================================================

    @Test
    fun `shorts from PRODUCTIVE channel are blocked with ShortsBlocked`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val shortsIdentity = VideoIdentity(
            videoId = "short_001",
            title = "Quick Physics Teaser",
            channelHandle = "@veritasium",
            isShorts = true,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(shortsIdentity)

            assertTrue(awaitItem() is PolicyState.Verifying)

            val blockedState = awaitItem()
            assertTrue("Shorts must resolve to ShortsBlocked even if channel is PRODUCTIVE", blockedState is PolicyState.ShortsBlocked)
            val shortsBlocked = blockedState as PolicyState.ShortsBlocked
            assertEquals(BlockReason.SHORTS, shortsBlocked.reason)
            assertEquals(shortsIdentity, shortsBlocked.identity)
        }
    }

    @Test
    fun `shorts from DISTRACTING channel are blocked with ShortsBlocked`() = runTest {
        fakeRepo.setChannelPolicy("@distracting_channel", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val shortsIdentity = VideoIdentity(
            videoId = "short_002",
            title = "Funny Cat Clip",
            channelHandle = "@distracting_channel",
            isShorts = true,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(shortsIdentity)

            assertTrue(awaitItem() is PolicyState.Verifying)

            val blockedState = awaitItem()
            assertTrue(blockedState is PolicyState.ShortsBlocked)
            assertEquals(BlockReason.SHORTS, (blockedState as PolicyState.ShortsBlocked).reason)
        }
    }

    @Test
    fun `shorts from unclassified channel are blocked immediately without prompting AskUser`() = runTest {
        // Channel has NO database entry
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val shortsIdentity = VideoIdentity(
            videoId = "short_003",
            title = "Viral Dance",
            channelHandle = "@viral_unknown",
            isShorts = true,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(shortsIdentity)

            assertTrue(awaitItem() is PolicyState.Verifying)

            val blockedState = awaitItem()
            assertTrue("Unclassified Shorts must never emit AskUser", blockedState is PolicyState.ShortsBlocked)
            assertEquals(BlockReason.SHORTS, (blockedState as PolicyState.ShortsBlocked).reason)
        }
    }

    @Test
    fun `shorts detection does not query channel repository`() = runTest {
        fakeRepo.setChannelPolicy("@popular_creator", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val shortsIdentity = VideoIdentity(
            videoId = "short_bypass_db",
            title = "Bypassing DB Query",
            channelHandle = "@popular_creator",
            isShorts = true,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(shortsIdentity)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.ShortsBlocked)

            // Crucial verification: Repository was never queried
            assertEquals(0, fakeRepo.getPolicyCallCount)
            assertFalse(fakeRepo.queriedHandles.contains("@popular_creator"))
        }
    }

    @Test
    fun `transition from normal productive video to shorts enters ShortsBlocked`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(videoId = "vid_full", title = "Full Documentary", channelHandle = "@veritasium", isShorts = false)
        val short = VideoIdentity(videoId = "vid_short", title = "Quick Clip", channelHandle = "@veritasium", isShorts = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Video 1: Allowed
            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Transition: User swipes to Shorts
            engine.onVideoDetected(short)
            assertTrue("Must re-enter Verifying on transition to Shorts", awaitItem() is PolicyState.Verifying)
            assertTrue("Must resolve to ShortsBlocked", awaitItem() is PolicyState.ShortsBlocked)
        }
    }

    // =========================================================================
    // INVARIANT 3: Ad Transitions Retain Previous Verified Channel's Policy
    // =========================================================================

    @Test
    fun `ad transition during PRODUCTIVE video retains Allowed state without evaluating ad channel`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@ad_advertiser", ChannelPolicy.DISTRACTING) // Even if ad sponsor handle is marked DISTRACTING
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val primaryVideo = VideoIdentity(
            videoId = "primary_video",
            title = "Quantum Mechanics",
            channelHandle = "@veritasium",
            isShorts = false,
            isAd = false
        )

        val midRollAd = VideoIdentity(
            videoId = "ad_001",
            title = "Buy This Smartphone Now!",
            channelHandle = "@ad_advertiser",
            isShorts = false,
            isAd = true
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // 1. Primary productive video starts
            engine.onVideoDetected(primaryVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val primaryAllowed = awaitItem()
            assertTrue(primaryAllowed is PolicyState.Allowed)
            assertEquals("@veritasium", (primaryAllowed as PolicyState.Allowed).channelHandle)

            // 2. Mid-roll ad begins
            engine.onVideoDetected(midRollAd)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Must retain Allowed state using previous verified channel policy
            val adAllowed = awaitItem()
            assertTrue("Ad during productive session must retain Allowed policy", adAllowed is PolicyState.Allowed)
            assertEquals("@veritasium", (adAllowed as PolicyState.Allowed).channelHandle)
            assertEquals(midRollAd, (adAllowed as PolicyState.Allowed).identity)

            // Advertiser handle was NOT looked up in repository
            assertFalse(fakeRepo.queriedHandles.contains("@ad_advertiser"))
        }
    }

    @Test
    fun `ad transition during DISTRACTING video retains Blocked state`() = runTest {
        fakeRepo.setChannelPolicy("@game_streamer", ChannelPolicy.DISTRACTING)
        fakeRepo.setChannelPolicy("@charity_ad", ChannelPolicy.PRODUCTIVE) // Even if ad is marked PRODUCTIVE
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val distractingVideo = VideoIdentity(
            videoId = "distract_vid",
            title = "Game Highlights",
            channelHandle = "@game_streamer",
            isShorts = false,
            isAd = false
        )

        val ad = VideoIdentity(
            videoId = "charity_ad_vid",
            title = "Help Build Libraries",
            channelHandle = "@charity_ad",
            isShorts = false,
            isAd = true
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // 1. Primary distracting video starts
            engine.onVideoDetected(distractingVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val primaryBlocked = awaitItem()
            assertTrue(primaryBlocked is PolicyState.Blocked)
            assertEquals("@game_streamer", (primaryBlocked as PolicyState.Blocked).channelHandle)

            // 2. Ad begins during distracting video
            engine.onVideoDetected(ad)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Must retain Blocked state
            val adBlocked = awaitItem()
            assertTrue("Ad during distracting session must retain Blocked policy", adBlocked is PolicyState.Blocked)
            assertEquals("@game_streamer", (adBlocked as PolicyState.Blocked).channelHandle)
            assertEquals(BlockReason.DISTRACTING_CHANNEL, (adBlocked as PolicyState.Blocked).reason)

            assertFalse(fakeRepo.queriedHandles.contains("@charity_ad"))
        }
    }

    @Test
    fun `ad playback does not query repository for advertiser handle`() = runTest {
        fakeRepo.setChannelPolicy("@khanacademy", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(title = "Calculus 1", channelHandle = "@khanacademy")
        val ad = VideoIdentity(title = "Car Insurance Ad", channelHandle = "@car_insurance", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            val countBeforeAd = fakeRepo.getPolicyCallCount

            engine.onVideoDetected(ad)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Query count must remain identical; ad does not trigger DB lookup
            assertEquals(countBeforeAd, fakeRepo.getPolicyCallCount)
        }
    }

    @Test
    fun `ad transition never emits AskUser even if advertiser is unclassified`() = runTest {
        fakeRepo.setChannelPolicy("@mitocw", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(title = "Algorithms", channelHandle = "@mitocw")
        val adWithUnknownHandle = VideoIdentity(title = "Startup Ad", channelHandle = "@unknown_ad_startup", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            engine.onVideoDetected(adWithUnknownHandle)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val state = awaitItem()

            assertTrue("Expected Allowed, not AskUser", state is PolicyState.Allowed)
            assertFalse("Ad must never trigger AskUser", state is PolicyState.AskUser)
        }
    }

    @Test
    fun `cold start directly into an ad without prior verified video fails closed in Verifying state`() = runTest {
        // App launched or session started directly on an ad with no prior verified channel
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val coldAd = VideoIdentity(
            videoId = "cold_ad",
            title = "Unprecedented Ad",
            channelHandle = "@cold_advertiser",
            isShorts = false,
            isAd = true
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(coldAd)
            val verifyingState = awaitItem()
            assertTrue("Cold ad without prior verified policy must enter Verifying", verifyingState is PolicyState.Verifying)
            assertEquals(coldAd, (verifyingState as PolicyState.Verifying).identity)

            // When no prior verified channel exists, the policy engine stays in Verifying fail-closed
            expectNoEvents()
            assertEquals(0, fakeRepo.getPolicyCallCount)
            assertTrue(engine.state.value is PolicyState.Verifying)
        }
    }

    @Test
    fun `pre-roll ad on video switch stays in Verifying until real video resolves`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@distracting_gaming", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video1 = VideoIdentity(videoId = "v1", title = "Science", channelHandle = "@veritasium")
        val video2 = VideoIdentity(videoId = "v2", title = "Gaming Stream", channelHandle = "@distracting_gaming")
        val preRollAd = VideoIdentity(videoId = "ad_switch", title = "Car Ad", channelHandle = "@car_sponsor", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // 1. Primary video 1 plays and resolves to Allowed
            engine.onVideoDetected(video1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // 2. User switches to video 2
            engine.onVideoDetected(video2)
            val v2Verifying = awaitItem()
            assertTrue("Must enter Verifying on video switch", v2Verifying is PolicyState.Verifying)
            assertEquals(video2, (v2Verifying as PolicyState.Verifying).identity)

            // 3. Pre-roll ad for video 2 begins before video 2 resolves
            engine.onVideoDetected(preRollAd)
            val adVerifying = awaitItem()
            assertTrue("Pre-roll ad on video switch must be in Verifying", adVerifying is PolicyState.Verifying)
            assertEquals(preRollAd, (adVerifying as PolicyState.Verifying).identity)

            // Pre-roll ad must NOT inherit video 1's Allowed policy; it stays in Verifying
            expectNoEvents()
            assertTrue(engine.state.value is PolicyState.Verifying)

            // 4. Real video 2 content arrives and resolves to Blocked
            engine.onVideoDetected(video2)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val v2Blocked = awaitItem()
            assertTrue("Video 2 must resolve to Blocked", v2Blocked is PolicyState.Blocked)
            assertEquals("@distracting_gaming", (v2Blocked as PolicyState.Blocked).channelHandle)
        }
    }

    @Test
    fun `pre-roll ad with isNewVideoSwitch stays in Verifying and does not adopt old video policy`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video1 = VideoIdentity(videoId = "v1", title = "Science", channelHandle = "@veritasium")
        val preRollAdExplicit = VideoIdentity(
            videoId = "ad_new_switch",
            title = "Promo Ad",
            channelHandle = "@ad_sponsor",
            isAd = true,
            isNewVideoSwitch = true
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Pre-roll ad explicitly flagged as new video switch
            engine.onVideoDetected(preRollAdExplicit)
            val verifying = awaitItem()
            assertTrue("Must enter Verifying on new switch ad", verifying is PolicyState.Verifying)
            assertEquals(preRollAdExplicit, (verifying as PolicyState.Verifying).identity)

            // Must NOT inherit video 1's policy; stays in Verifying
            expectNoEvents()
            assertTrue(engine.state.value is PolicyState.Verifying)
        }
    }

    @Test
    fun `post-ad return to main video maintains Allowed state`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(videoId = "vid_main", title = "Math Wonder", channelHandle = "@veritasium", isShorts = false, isAd = false)
        val ad = VideoIdentity(videoId = "vid_ad", title = "Sponsored Break", channelHandle = "@sponsor", isShorts = false, isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // 1. Main video
            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // 2. Ad begins
            engine.onVideoDetected(ad)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // 3. Ad finishes, returns to main video
            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val returnAllowed = awaitItem()
            assertTrue(returnAllowed is PolicyState.Allowed)
            assertEquals("@veritasium", (returnAllowed as PolicyState.Allowed).channelHandle)
        }
    }

    // =========================================================================
    // USER CLASSIFICATION FLOW
    // =========================================================================

    @Test
    fun `unclassified video prompts AskUser and user selection of PRODUCTIVE transitions to Allowed and persists in Room`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val unclassifiedVideo = VideoIdentity(
            videoId = "vid_new_teacher",
            title = "Linear Algebra 1",
            channelHandle = "@new_math_channel",
            channelName = "New Math Teacher",
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(unclassifiedVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)

            val askUserState = awaitItem()
            assertTrue(askUserState is PolicyState.AskUser)
            val askUser = askUserState as PolicyState.AskUser
            assertEquals("@new_math_channel", askUser.channelHandle)

            // User clicks "Productive" in Compose Overlay
            engine.onUserClassified("@new_math_channel", ChannelPolicy.PRODUCTIVE)

            val allowedState = awaitItem()
            assertTrue("State must immediately transition to Allowed", allowedState is PolicyState.Allowed)
            val allowed = allowedState as PolicyState.Allowed
            assertEquals("@new_math_channel", allowed.channelHandle)
            assertEquals(unclassifiedVideo, allowed.identity)

            // Verify persistence in repository
            assertEquals(ChannelPolicy.PRODUCTIVE, fakeRepo.getChannelPolicy("@new_math_channel"))
            assertEquals(1, fakeRepo.savePolicyCallCount)
        }
    }

    @Test
    fun `unclassified video prompts AskUser and user selection of DISTRACTING transitions to Blocked and persists in Room`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val unclassifiedVideo = VideoIdentity(
            videoId = "vid_clickbait",
            title = "10 Shocking Secrets",
            channelHandle = "@clickbait_network",
            channelName = "Viral Clickbait",
            isShorts = false,
            isAd = false
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(unclassifiedVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)

            val askUserState = awaitItem()
            assertTrue(askUserState is PolicyState.AskUser)

            // User clicks "Distracting" in Compose Overlay
            engine.onUserClassified("@clickbait_network", ChannelPolicy.DISTRACTING)

            val blockedState = awaitItem()
            assertTrue("State must immediately transition to Blocked", blockedState is PolicyState.Blocked)
            val blocked = blockedState as PolicyState.Blocked
            assertEquals("@clickbait_network", blocked.channelHandle)
            assertEquals(BlockReason.DISTRACTING_CHANNEL, blocked.reason)

            // Verify persistence in repository
            assertEquals(ChannelPolicy.DISTRACTING, fakeRepo.getChannelPolicy("@clickbait_network"))
            assertEquals(1, fakeRepo.savePolicyCallCount)
        }
    }

    @Test
    fun `user classification updates lastVerifiedPolicy for subsequent mid-roll ads`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(title = "History Lecture", channelHandle = "@history_channel")
        val ad = VideoIdentity(title = "Shoe Commercial", channelHandle = "@shoe_brand", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Video detected -> AskUser
            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.AskUser)

            // User classifies channel as PRODUCTIVE
            engine.onUserClassified("@history_channel", ChannelPolicy.PRODUCTIVE)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Now a mid-roll ad arrives
            engine.onVideoDetected(ad)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val adState = awaitItem()

            // State retains Allowed because lastVerifiedPolicy was set during onUserClassified
            assertTrue("Ad must retain Allowed policy updated by user classification", adState is PolicyState.Allowed)
            assertEquals("@history_channel", (adState as PolicyState.Allowed).channelHandle)
        }
    }

    @Test
    fun `classification for mismatched channel handle does not mutate active video state`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val activeVideo = VideoIdentity(videoId = "vid_active", title = "Active Vid", channelHandle = "@veritasium")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(activeVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Stale classification event arrives for an old/different channel
            engine.onUserClassified("@unrelated_channel", ChannelPolicy.DISTRACTING)

            // Repository saves the policy
            assertEquals(ChannelPolicy.DISTRACTING, fakeRepo.getChannelPolicy("@unrelated_channel"))

            // But active video state remains Allowed for @veritasium without transitioning to Blocked
            expectNoEvents()
            assertTrue(engine.state.value is PolicyState.Allowed)
            assertEquals("@veritasium", (engine.state.value as PolicyState.Allowed).channelHandle)
        }
    }

    // =========================================================================
    // CONSECUTIVE VIDEO TRANSITIONS & AUTOPLAY
    // =========================================================================

    @Test
    fun `consecutive allowed videos must re-enter Verifying on each video transition`() = runTest {
        fakeRepo.setChannelPolicy("@khanacademy", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@mitocw", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video1 = VideoIdentity(videoId = "v1", title = "Math 101", channelHandle = "@khanacademy")
        val video2 = VideoIdentity(videoId = "v2", title = "Physics 101", channelHandle = "@mitocw")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Video 1
            engine.onVideoDetected(video1)
            val verifying1 = awaitItem()
            assertTrue(verifying1 is PolicyState.Verifying)
            assertEquals(video1, (verifying1 as PolicyState.Verifying).identity)
            val allowed1 = awaitItem()
            assertTrue(allowed1 is PolicyState.Allowed)
            assertEquals("@khanacademy", (allowed1 as PolicyState.Allowed).channelHandle)

            // Video 2: Different video identity
            engine.onVideoDetected(video2)
            val verifying2 = awaitItem()
            assertTrue("Must re-enter Verifying on second video", verifying2 is PolicyState.Verifying)
            assertEquals(video2, (verifying2 as PolicyState.Verifying).identity)
            val allowed2 = awaitItem()
            assertTrue("Resolves to Allowed for second channel", allowed2 is PolicyState.Allowed)
            assertEquals("@mitocw", (allowed2 as PolicyState.Allowed).channelHandle)
        }
    }

    @Test
    fun `transition from PRODUCTIVE to DISTRACTING drops Allowed and enters Blocked through Verifying`() = runTest {
        fakeRepo.setChannelPolicy("@khanacademy", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@gaming_channel", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val eduVideo = VideoIdentity(videoId = "edu", title = "Calculus", channelHandle = "@khanacademy")
        val gamingVideo = VideoIdentity(videoId = "game", title = "Let's Play", channelHandle = "@gaming_channel")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Educational video
            engine.onVideoDetected(eduVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // User clicks autoplay into a gaming video
            engine.onVideoDetected(gamingVideo)
            val verifying = awaitItem()
            assertTrue("Must enter Verifying when leaving productive video", verifying is PolicyState.Verifying)
            assertEquals(gamingVideo, (verifying as PolicyState.Verifying).identity)

            val blocked = awaitItem()
            assertTrue("Must resolve to Blocked for distracting channel", blocked is PolicyState.Blocked)
            assertEquals("@gaming_channel", (blocked as PolicyState.Blocked).channelHandle)
        }
    }

    @Test
    fun `transition from DISTRACTING to PRODUCTIVE clears Blocked and enters Allowed through Verifying`() = runTest {
        fakeRepo.setChannelPolicy("@distracting_channel", ChannelPolicy.DISTRACTING)
        fakeRepo.setChannelPolicy("@mitocw", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val distractVideo = VideoIdentity(videoId = "d1", title = "Meme Compilation", channelHandle = "@distracting_channel")
        val eduVideo = VideoIdentity(videoId = "e1", title = "Linear Algebra", channelHandle = "@mitocw")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Start on distracting video
            engine.onVideoDetected(distractVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Blocked)

            // User navigates to productive video
            engine.onVideoDetected(eduVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val allowed = awaitItem()
            assertTrue("Must transition from Blocked to Allowed", allowed is PolicyState.Allowed)
            assertEquals("@mitocw", (allowed as PolicyState.Allowed).channelHandle)
        }
    }

    @Test
    fun `consecutive autoplay sequence across mixed policies`() = runTest {
        fakeRepo.setChannelPolicy("@edu_channel", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@distract_channel", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val eduVideo = VideoIdentity(videoId = "s1", title = "Edu 1", channelHandle = "@edu_channel")
        val adInEdu = VideoIdentity(videoId = "ad1", title = "Ad Break", channelHandle = "@sponsor", isAd = true)
        val distractVideo = VideoIdentity(videoId = "s2", title = "Distract 1", channelHandle = "@distract_channel")
        val adInDistract = VideoIdentity(videoId = "ad2", title = "Ad 2", channelHandle = "@ad_firm", isAd = true)
        val unclassifiedVideo = VideoIdentity(videoId = "s3", title = "Unknown", channelHandle = "@newbie")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // 1. Edu Video -> Allowed
            engine.onVideoDetected(eduVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // 2. Ad in Edu -> Allowed
            engine.onVideoDetected(adInEdu)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // 3. Distracting Video -> Blocked
            engine.onVideoDetected(distractVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Blocked)

            // 4. Ad in Distracting -> Blocked
            engine.onVideoDetected(adInDistract)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Blocked)

            // 5. Unclassified Video -> AskUser
            engine.onVideoDetected(unclassifiedVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val askState = awaitItem()
            assertTrue(askState is PolicyState.AskUser)
            assertEquals("@newbie", (askState as PolicyState.AskUser).channelHandle)

            // 6. User Classifies as PRODUCTIVE -> Allowed
            engine.onUserClassified("@newbie", ChannelPolicy.PRODUCTIVE)
            val finalAllowed = awaitItem()
            assertTrue(finalAllowed is PolicyState.Allowed)
            assertEquals("@newbie", (finalAllowed as PolicyState.Allowed).channelHandle)
        }
    }

    @Test
    fun `playback stopped resets active identity and session state`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(videoId = "vid_stop", title = "Video", channelHandle = "@veritasium")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Playback stopped (e.g. YouTube dismissed or back button pressed)
            engine.onPlaybackStopped()

            val resetState = awaitItem()
            assertTrue(resetState is PolicyState.Verifying)
            assertNull("Identity must be cleared upon playback stop", (resetState as PolicyState.Verifying).identity)
        }
    }

    // =========================================================================
    // ADVERSARIAL STRESS TESTS & INVARIANT ATTACK HARNESS
    // =========================================================================

    @Test
    fun `adversarial - onUserClassified for current Shorts channel never transitions state to Allowed`() = runTest(dispatcherRule.testDispatcher) {
        fakeRepo.setChannelPolicy("@productive_creator", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val shorts = VideoIdentity(
            videoId = "short_exploit",
            title = "Shorts Video",
            channelHandle = "@productive_creator",
            isShorts = true,
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(shorts)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val shortsBlocked = awaitItem()
            assertTrue("Shorts must be ShortsBlocked", shortsBlocked is PolicyState.ShortsBlocked)

            // Adversarial probe: user classification event arrives while Shorts is playing
            engine.onUserClassified("@productive_creator", ChannelPolicy.PRODUCTIVE)

            // Invariant 2: Shorts can NEVER be allowed under any permutation of channel policies or user classification
            expectNoEvents()
            val postClassifyState = engine.state.value
            assertFalse("Shorts must NEVER transition to Allowed! Got: $postClassifyState", postClassifyState is PolicyState.Allowed)
            assertTrue("Shorts must remain ShortsBlocked! Got: $postClassifyState", postClassifyState is PolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `adversarial - onUserClassified when playback stopped does not emit Allowed`() = runTest(dispatcherRule.testDispatcher) {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onPlaybackStopped()
            // Should remain Verifying(null)
            expectNoEvents()

            // Delayed classification event arrives when no video is playing
            engine.onUserClassified("@some_channel", ChannelPolicy.PRODUCTIVE)

            // The database should save the policy
            assertEquals(ChannelPolicy.PRODUCTIVE, fakeRepo.getChannelPolicy("@some_channel"))

            // But active engine state must NOT become Allowed with a dummy identity!
            val stateAfterClassify = engine.state.value
            assertFalse("Must not transition to Allowed when playback stopped! Got: $stateAfterClassify", stateAfterClassify is PolicyState.Allowed)
            assertEquals("State must remain Verifying(null) when playback stopped", PolicyState.Verifying(null), stateAfterClassify)
        }
    }

    @Test
    fun `adversarial - shorts with isAd flag true is blocked as ShortsBlocked and never Allowed`() = runTest(dispatcherRule.testDispatcher) {
        fakeRepo.setChannelPolicy("@productive_ad_sponsor", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        // A sponsored Short
        val sponsoredShort = VideoIdentity(
            videoId = "short_ad_001",
            title = "Sponsored Short",
            channelHandle = "@productive_ad_sponsor",
            isShorts = true,
            isAd = true,
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(sponsoredShort)
            assertTrue(awaitItem() is PolicyState.Verifying)

            val resolved = awaitItem()
            assertTrue("Sponsored Shorts must resolve to ShortsBlocked", resolved is PolicyState.ShortsBlocked)
            assertEquals(BlockReason.SHORTS, (resolved as PolicyState.ShortsBlocked).reason)
        }
    }

    @Test
    fun `adversarial - extreme video titles and injection handles remain fail-closed or resolve safely`() = runTest(dispatcherRule.testDispatcher) {
        fakeRepo.setChannelPolicy("@valid_handle", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val extremeTitles = listOf(
            "A".repeat(10_000), // Massive title string
            "'; DROP TABLE youtube_channels; --", // SQL injection payload in title
            "<script>alert('xss')</script>", // XSS payload
            "Emoji explosion \uD83D\uDE00\uD83D\uDE02\uD83E\uDD2A\uD83D\uDD25\uD83D\uDCA5\uD83D\uDC80",
            "\u0000\u0001\u0002\r\n\t\b", // Control characters
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            for (title in extremeTitles) {
                val identity = VideoIdentity(
                    videoId = "extreme_id_${title.hashCode()}",
                    title = title,
                    channelHandle = "@valid_handle",
                )
                engine.onVideoDetected(identity)
                assertTrue(awaitItem() is PolicyState.Verifying)
                val resolved = awaitItem()
                assertTrue("Must safely resolve to Allowed for @valid_handle regardless of extreme title, got $resolved", resolved is PolicyState.Allowed)
            }
        }
    }

    @Test
    fun `adversarial - SQL injection and adversarial handles in unclassified video emit AskUser safely`() = runTest(dispatcherRule.testDispatcher) {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val sqlInjectionHandle = "'; DELETE FROM youtube_channels WHERE '1'='1"
        val identity = VideoIdentity(
            videoId = "sql_inj",
            title = "Testing Handle Sanitization",
            channelHandle = sqlInjectionHandle,
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(identity)
            assertTrue(awaitItem() is PolicyState.Verifying)

            val state = awaitItem()
            assertTrue("Must enter AskUser safely for unknown injection handle, got $state", state is PolicyState.AskUser)
            assertEquals(sqlInjectionHandle, (state as PolicyState.AskUser).channelHandle)
        }
    }

    @Test
    fun `adversarial - whitespace-only handle and whitespace-only name remains fail-closed in Verifying`() = runTest(dispatcherRule.testDispatcher) {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val whitespaceIdentities = listOf(
            VideoIdentity(videoId = "ws1", channelHandle = "   ", channelName = ""),
            VideoIdentity(videoId = "ws2", channelHandle = "", channelName = "   "),
            VideoIdentity(videoId = "ws3", channelHandle = "\t\r\n", channelName = "  \n  "),
            VideoIdentity(videoId = "ws4", channelHandle = "", channelName = ""),
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            for (identity in whitespaceIdentities) {
                engine.onVideoDetected(identity)
                val state = awaitItem()
                assertTrue("Whitespace-only channel identity must remain fail-closed in Verifying, got $state", state is PolicyState.Verifying)
                expectNoEvents()
            }
        }
    }

    @Test
    fun `adversarial - slow query cancelled by new video detection never overwrites newer video state`() = runTest {
        fakeRepo.enableDelay(500)
        fakeRepo.setChannelPolicy("@slow_allowed", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@fast_blocked", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val slowVideo = VideoIdentity(videoId = "slow_vid", title = "Slow Video", channelHandle = "@slow_allowed")
        val fastVideo = VideoIdentity(videoId = "fast_vid", title = "Fast Video", channelHandle = "@fast_blocked")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Video 1 detected (takes 500ms)
            engine.onVideoDetected(slowVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Advance 100ms: Video 1 still in progress
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(100)
            assertTrue(engine.state.value is PolicyState.Verifying)

            // Rapid switch: User clicks Video 2 (fast video)
            engine.onVideoDetected(fastVideo)
            val verifyingFast = awaitItem()
            assertTrue(verifyingFast is PolicyState.Verifying)
            assertEquals(fastVideo, (verifyingFast as PolicyState.Verifying).identity)

            // Advance 500ms to allow all jobs to complete
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(501)

            val finalState = awaitItem()
            assertTrue("Fast video must resolve to Blocked, got $finalState", finalState is PolicyState.Blocked)
            val blocked = finalState as PolicyState.Blocked
            assertEquals("@fast_blocked", blocked.channelHandle)
            assertEquals(fastVideo, blocked.identity)

            // Ensure no further emissions occur (slow video must NOT overwrite fast video with Allowed)
            expectNoEvents()
        }
    }

    @Test
    fun `adversarial - rapid sequential video hopping always emits Verifying for every video`() = runTest {
        fakeRepo.setChannelPolicy("@ch_1", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@ch_2", ChannelPolicy.DISTRACTING)
        fakeRepo.setChannelPolicy("@ch_3", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val v1 = VideoIdentity(videoId = "hop_1", title = "V1", channelHandle = "@ch_1")
        val v2 = VideoIdentity(videoId = "hop_2", title = "V2", channelHandle = "@ch_2")
        val v3 = VideoIdentity(videoId = "hop_3", title = "V3", channelHandle = "@ch_3")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(v1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            engine.onVideoDetected(v2)
            assertTrue("Must emit Verifying on hop to v2", awaitItem() is PolicyState.Verifying)
            assertTrue("Must resolve to Blocked for v2", awaitItem() is PolicyState.Blocked)

            engine.onVideoDetected(v3)
            assertTrue("Must emit Verifying on hop to v3", awaitItem() is PolicyState.Verifying)
            assertTrue("Must resolve to Allowed for v3", awaitItem() is PolicyState.Allowed)
        }
    }

    @Test
    fun `adversarial - consecutive ad pod during productive video all retain Allowed and never query DB`() = runTest {
        fakeRepo.setChannelPolicy("@main_channel", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val mainVideo = VideoIdentity(videoId = "main", title = "Lecture", channelHandle = "@main_channel")
        val ad1 = VideoIdentity(videoId = "ad_1", title = "Ad 1", channelHandle = "@ad_firm_1", isAd = true)
        val ad2 = VideoIdentity(videoId = "ad_2", title = "Ad 2", channelHandle = "@ad_firm_2", isAd = true)
        val ad3 = VideoIdentity(videoId = "ad_3", title = "Ad 3", channelHandle = "@ad_firm_3", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(mainVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            val callCountAfterMain = fakeRepo.getPolicyCallCount

            // Ad 1 of 3
            engine.onVideoDetected(ad1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val s1 = awaitItem()
            assertTrue(s1 is PolicyState.Allowed)
            assertEquals("@main_channel", (s1 as PolicyState.Allowed).channelHandle)

            // Ad 2 of 3
            engine.onVideoDetected(ad2)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val s2 = awaitItem()
            assertTrue(s2 is PolicyState.Allowed)
            assertEquals("@main_channel", (s2 as PolicyState.Allowed).channelHandle)

            // Ad 3 of 3
            engine.onVideoDetected(ad3)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val s3 = awaitItem()
            assertTrue(s3 is PolicyState.Allowed)
            assertEquals("@main_channel", (s3 as PolicyState.Allowed).channelHandle)

            // Total repository queries must not increase during any of the 3 ads
            assertEquals(callCountAfterMain, fakeRepo.getPolicyCallCount)
        }
    }

    // =========================================================================
    // MILESTONE 2: Generation Tokens, Canonical Models & Ad Switch Verification
    // =========================================================================

    @Test
    fun `generation token prevents race conditions when rapid lookups complete out of order`() = runTest {
        fakeRepo.enableDelay(500)
        fakeRepo.setChannelPolicy("@slow_ch", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@fast_ch", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val v1 = VideoIdentity(videoId = "v1", title = "Video 1", channelHandle = "@slow_ch")
        val v2 = VideoIdentity(videoId = "v2", title = "Video 2", channelHandle = "@fast_ch")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Video 1 detected (Gen 1, delayed 500ms)
            engine.onVideoDetected(v1)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // 100ms later, rapid video hop to Video 2 (Gen 2)
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(100)
            engine.onVideoDetected(v2)
            val v2Verifying = awaitItem()
            assertTrue(v2Verifying is PolicyState.Verifying)
            assertEquals(v2, (v2Verifying as PolicyState.Verifying).identity)

            // Fast forward 600ms so all background lookups complete
            dispatcherRule.testDispatcher.scheduler.advanceTimeBy(600)

            val finalState = awaitItem()
            assertTrue("Video 2 must resolve to Blocked", finalState is PolicyState.Blocked)
            assertEquals("@fast_ch", (finalState as PolicyState.Blocked).channelHandle)

            // Crucial: Gen 1 (v1) delayed lookup must NOT overwrite Gen 2
            expectNoEvents()
            assertEquals(PolicyState.Blocked(v2, "@fast_ch", BlockReason.DISTRACTING_CHANNEL), engine.state.value)
        }
    }

    @Test
    fun `onContentDetected seamlessly maps CanonicalYouTubeContent to deterministic policy state`() = runTest {
        fakeRepo.setChannelPolicy("@veritasium", ChannelPolicy.PRODUCTIVE, "Veritasium")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val canonicalContent = CanonicalYouTubeContent(
            fingerprint = VideoFingerprint(
                mediaId = "media_canon_01",
                normalizedTitle = "how trees work",
                channelCanonicalKey = "handle:@veritasium",
            ),
            channelIdentity = ChannelIdentity(
                verifiedHandle = "@veritasium",
                displayName = "Veritasium",
            ),
            title = "How Trees Work",
            isShorts = false,
            isAd = false,
            confidence = DetectionConfidence.HIGH,
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onContentDetected(canonicalContent)

            val verifyingState = awaitItem()
            assertTrue("Must enter Verifying on CanonicalContent", verifyingState is PolicyState.Verifying)
            val identity = (verifyingState as PolicyState.Verifying).identity
            assertNotNull(identity)
            assertEquals("How Trees Work", identity?.title)
            assertEquals("@veritasium", identity?.channelHandle)
            assertEquals("Veritasium", identity?.channelName)

            val allowedState = awaitItem()
            assertTrue("Must resolve to Allowed", allowedState is PolicyState.Allowed)
            val allowed = allowedState as PolicyState.Allowed
            assertEquals("@veritasium", allowed.channelHandle)
            assertEquals(identity, allowed.identity)
            assertEquals("handle:@veritasium", allowed.channelIdentity.canonicalKey)
        }
    }

    @Test
    fun `channel with displayName only without handle resolves policy through ChannelIdentity`() = runTest {
        fakeRepo.setChannelPolicy("name:khan academy", ChannelPolicy.PRODUCTIVE, "Khan Academy")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val nameOnlyIdentity = VideoIdentity(
            videoId = "vid_name_only",
            title = "Calculus Derivatives",
            channelIdentity = ChannelIdentity(
                verifiedHandle = null, // Strictly no fake handle!
                displayName = "Khan Academy",
            ),
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(nameOnlyIdentity)

            val verifying = awaitItem()
            assertTrue(verifying is PolicyState.Verifying)

            val allowed = awaitItem()
            assertTrue("Channel with name only should resolve policy without fake handle", allowed is PolicyState.Allowed)
            assertEquals("name:khan academy", (allowed as PolicyState.Allowed).channelIdentity.canonicalKey)
        }
    }

    @Test
    fun `touch blocker and verifying state are maintained across rapid video navigation`() = runTest {
        fakeRepo.setChannelPolicy("@ch_edu", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@ch_distract", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val v1 = VideoIdentity(videoId = "hop1", title = "Hop 1", channelHandle = "@ch_edu")
        val v2 = VideoIdentity(videoId = "hop2", title = "Hop 2", channelHandle = "@ch_distract")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(v1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Switch to v2 immediately emits Verifying
            engine.onVideoDetected(v2)
            val verifying2 = awaitItem()
            assertTrue("Switch must immediately emit Verifying", verifying2 is PolicyState.Verifying)
            assertEquals(v2, (verifying2 as PolicyState.Verifying).identity)
            assertTrue("Resolves to Blocked", awaitItem() is PolicyState.Blocked)
        }
    }
}

// =============================================================================
// Test Double: In-Memory Fake Repository
// =============================================================================

class FakeYouTubeChannelRepository : YouTubeChannelRepository {
    private val channelMap = mutableMapOf<String, YouTubeChannelEntity>()
    private val channelFlow = MutableStateFlow<List<YouTubeChannelEntity>>(emptyList())
    private var delayMillis: Long = 0

    var getPolicyCallCount: Int = 0
        private set
    var savePolicyCallCount: Int = 0
        private set
    val queriedHandles = mutableListOf<String>()

    fun setChannelPolicy(handle: String, policy: ChannelPolicy, name: String = "") {
        channelMap[handle] = YouTubeChannelEntity(
            channelHandle = handle,
            channelName = name,
            policy = policy
        )
        channelFlow.value = channelMap.values.toList()
    }

    fun enableDelay(millis: Long) {
        delayMillis = millis
    }

    override suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy? {
        if (delayMillis > 0) {
            kotlinx.coroutines.delay(delayMillis)
        }
        getPolicyCallCount++
        queriedHandles.add(channelHandle)
        return channelMap[channelHandle]?.policy
            ?: channelMap.values.firstOrNull { it.channelName.equals(channelHandle, ignoreCase = true) }?.policy
            ?: channelMap["handle:${channelHandle.lowercase()}"]?.policy
            ?: channelMap["name:${channelHandle.lowercase()}"]?.policy
    }

    override suspend fun saveChannelPolicy(channelHandle: String, channelName: String, policy: ChannelPolicy) {
        savePolicyCallCount++
        channelMap[channelHandle] = YouTubeChannelEntity(
            channelHandle = channelHandle,
            channelName = channelName,
            policy = policy
        )
        channelFlow.value = channelMap.values.toList()
    }

    override fun getAllChannels(): Flow<List<YouTubeChannelEntity>> = channelFlow.asStateFlow()

    override suspend fun getChannel(channelHandle: String): YouTubeChannelEntity? = channelMap[channelHandle]

    override suspend fun deleteChannel(channelHandle: String) {
        channelMap.remove(channelHandle)
        channelFlow.value = channelMap.values.toList()
    }

    fun clear() {
        channelMap.clear()
        channelFlow.value = emptyList()
        getPolicyCallCount = 0
        savePolicyCallCount = 0
        queriedHandles.clear()
        delayMillis = 0
    }
}
