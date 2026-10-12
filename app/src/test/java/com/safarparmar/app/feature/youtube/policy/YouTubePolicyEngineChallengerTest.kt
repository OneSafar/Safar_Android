package com.safarparmar.app.feature.youtube.policy

import app.cash.turbine.test
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.feature.youtube.model.CanonicalYouTubeContent
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.DetectionConfidence
import com.safarparmar.app.feature.youtube.model.VideoFingerprint
import com.safarparmar.app.feature.youtube.overlay.OverlayState
import com.safarparmar.app.feature.youtube.overlay.YouTubeGuardOverlayManager
import com.safarparmar.app.ui.auth.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
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
 * Adversarial Challenger Test Suite for Milestone 2.
 * Rigorously stress-tests:
 * 1. Generation tokens, concurrency, and rapid out-of-order video switches.
 * 2. Ad policy retention vs new video switches vs cold starts.
 * 3. Fullscreen / autoplay transitions to fail-closed OverlayState.Verifying.
 * 4. Pointer touch consumption and overlay state lifecycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class YouTubePolicyEngineChallengerTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeYouTubeChannelRepository
    private lateinit var engine: YouTubePolicyEngine
    private lateinit var overlayManager: YouTubeGuardOverlayManager

    @Before
    fun setUp() {
        fakeRepo = FakeYouTubeChannelRepository()
        overlayManager = YouTubeGuardOverlayManager()
    }

    // =========================================================================
    // 1. GENERATION TOKENS & RAPID VIDEO SWITCHING CONCURRENCY
    // =========================================================================

    @Test
    fun `challenger - rapid out-of-order lookup sequence strictly honors newest generation token`() = runTest {
        // Setup 5 channels with alternating policies and increasing DB delays
        fakeRepo.setChannelPolicy("@channel_1", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@channel_2", ChannelPolicy.DISTRACTING)
        fakeRepo.setChannelPolicy("@channel_3", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@channel_4", ChannelPolicy.DISTRACTING)
        fakeRepo.setChannelPolicy("@channel_5", ChannelPolicy.PRODUCTIVE)

        fakeRepo.enableDelay(300)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val v1 = VideoIdentity(videoId = "v1", title = "Video 1", channelHandle = "@channel_1")
        val v2 = VideoIdentity(videoId = "v2", title = "Video 2", channelHandle = "@channel_2")
        val v3 = VideoIdentity(videoId = "v3", title = "Video 3", channelHandle = "@channel_3")
        val v4 = VideoIdentity(videoId = "v4", title = "Video 4", channelHandle = "@channel_4")
        val v5 = VideoIdentity(videoId = "v5", title = "Video 5", channelHandle = "@channel_5")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Rapidly switch through v1 -> v2 -> v3 -> v4 -> v5 spaced by 20ms
            engine.onVideoDetected(v1)
            assertTrue(awaitItem() is PolicyState.Verifying)

            advanceTimeBy(20)
            engine.onVideoDetected(v2)
            val v2Verifying = awaitItem()
            assertTrue(v2Verifying is PolicyState.Verifying)
            assertEquals(v2, (v2Verifying as PolicyState.Verifying).identity)

            advanceTimeBy(20)
            engine.onVideoDetected(v3)
            val v3Verifying = awaitItem()
            assertTrue(v3Verifying is PolicyState.Verifying)
            assertEquals(v3, (v3Verifying as PolicyState.Verifying).identity)

            advanceTimeBy(20)
            engine.onVideoDetected(v4)
            val v4Verifying = awaitItem()
            assertTrue(v4Verifying is PolicyState.Verifying)
            assertEquals(v4, (v4Verifying as PolicyState.Verifying).identity)

            advanceTimeBy(20)
            engine.onVideoDetected(v5)
            val v5Verifying = awaitItem()
            assertTrue(v5Verifying is PolicyState.Verifying)
            assertEquals(v5, (v5Verifying as PolicyState.Verifying).identity)

            // Let all delays complete
            advanceTimeBy(500)

            // Only v5 should resolve to Allowed
            val finalItem = awaitItem()
            assertTrue("Only the latest video (v5) should resolve to Allowed, got $finalItem", finalItem is PolicyState.Allowed)
            assertEquals("@channel_5", (finalItem as PolicyState.Allowed).channelHandle)
            assertEquals(v5, (finalItem as PolicyState.Allowed).identity)

            // Ensure no further events emitted (v1..v4 lookups must have been completely discarded)
            expectNoEvents()
        }
    }

    @Test
    fun `challenger - rapid flapping between two videos preserves latest token and policy`() = runTest {
        fakeRepo.enableDelay(200)
        fakeRepo.setChannelPolicy("@allowed_vid", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@blocked_vid", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val allowedVid = VideoIdentity(videoId = "v_allowed", title = "Study Session", channelHandle = "@allowed_vid")
        val blockedVid = VideoIdentity(videoId = "v_blocked", title = "Distraction", channelHandle = "@blocked_vid")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Start on Allowed video (Gen 1)
            engine.onVideoDetected(allowedVid)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Switch to Blocked video (Gen 2) at 50ms
            advanceTimeBy(50)
            engine.onVideoDetected(blockedVid)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Switch back to Allowed video (Gen 3) at 100ms
            advanceTimeBy(50)
            engine.onVideoDetected(allowedVid)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Switch back to Blocked video (Gen 4) at 150ms
            advanceTimeBy(50)
            engine.onVideoDetected(blockedVid)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Complete all queries
            advanceTimeBy(400)

            // The final state must be Blocked for Gen 4
            val finalState = awaitItem()
            assertTrue("Final state must be Blocked for @blocked_vid, got $finalState", finalState is PolicyState.Blocked)
            assertEquals("@blocked_vid", (finalState as PolicyState.Blocked).channelHandle)
            expectNoEvents()
        }
    }

    @Test
    fun `challenger - delayed lookup after onPlaybackStopped never mutates Verifying null`() = runTest {
        fakeRepo.enableDelay(300)
        fakeRepo.setChannelPolicy("@channel_slow", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(videoId = "vid_stop_race", title = "Lecture", channelHandle = "@channel_slow")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Advance 100ms: DB lookup in progress
            advanceTimeBy(100)

            // User leaves YouTube / playback stopped
            engine.onPlaybackStopped()
            // Reset to Verifying(null) is emitted
            val resetState = awaitItem()
            assertEquals(PolicyState.Verifying(null), resetState)

            // Advance 400ms: delayed lookup finishes
            advanceTimeBy(400)

            // State must remain Verifying(null), NOT transition to Allowed
            expectNoEvents()
            assertEquals(PolicyState.Verifying(null), engine.state.value)
        }
    }

    // =========================================================================
    // 2. AD TRANSITIONS & POLICY RETENTION UNDER STRESS
    // =========================================================================

    @Test
    fun `challenger - mid-roll ad on PRODUCTIVE video retains Allowed without advertiser DB lookup`() = runTest {
        fakeRepo.setChannelPolicy("@khan_academy", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@advertiser_distracting", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val mainVideo = VideoIdentity(videoId = "main_01", title = "Calculus", channelHandle = "@khan_academy")
        val midRollAd = VideoIdentity(videoId = "ad_01", title = "Buy Fast Car", channelHandle = "@advertiser_distracting", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Main video starts -> Allowed
            engine.onVideoDetected(mainVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            val queriesBeforeAd = fakeRepo.getPolicyCallCount

            // Mid-roll ad begins
            engine.onVideoDetected(midRollAd)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Ad during productive session must retain Allowed", adState is PolicyState.Allowed)
            assertEquals("@khan_academy", (adState as PolicyState.Allowed).channelHandle)

            // Advertiser channel was NOT queried
            assertEquals(queriesBeforeAd, fakeRepo.getPolicyCallCount)
            assertFalse(fakeRepo.queriedHandles.contains("@advertiser_distracting"))
        }
    }

    @Test
    fun `challenger - mid-roll ad on DISTRACTING video retains Blocked without advertiser DB lookup`() = runTest {
        fakeRepo.setChannelPolicy("@gaming_hub", ChannelPolicy.DISTRACTING)
        fakeRepo.setChannelPolicy("@wholesome_charity", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val mainVideo = VideoIdentity(videoId = "distract_01", title = "Fortnite Clips", channelHandle = "@gaming_hub")
        val midRollAd = VideoIdentity(videoId = "ad_charity", title = "Help Plant Trees", channelHandle = "@wholesome_charity", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Main video starts -> Blocked
            engine.onVideoDetected(mainVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Blocked)

            // Mid-roll ad begins
            engine.onVideoDetected(midRollAd)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Ad during distracting session must retain Blocked", adState is PolicyState.Blocked)
            assertEquals("@gaming_hub", (adState as PolicyState.Blocked).channelHandle)
            assertEquals(BlockReason.DISTRACTING_CHANNEL, (adState as PolicyState.Blocked).reason)

            assertFalse(fakeRepo.queriedHandles.contains("@wholesome_charity"))
        }
    }

    @Test
    fun `challenger - ad pod with 3 consecutive ads retains verified policy throughout`() = runTest {
        fakeRepo.setChannelPolicy("@edu_channel", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video = VideoIdentity(videoId = "v_edu", title = "Physics 1", channelHandle = "@edu_channel")
        val ad1 = VideoIdentity(videoId = "ad1", title = "Ad 1", channelHandle = "@brand1", isAd = true)
        val ad2 = VideoIdentity(videoId = "ad2", title = "Ad 2", channelHandle = "@brand2", isAd = true)
        val ad3 = VideoIdentity(videoId = "ad3", title = "Ad 3", channelHandle = "@brand3", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Sequence of 3 ads
            for (ad in listOf(ad1, ad2, ad3)) {
                engine.onVideoDetected(ad)
                assertTrue("Ad must transition through Verifying", awaitItem() is PolicyState.Verifying)
                val s = awaitItem()
                assertTrue("Ad must retain Allowed policy", s is PolicyState.Allowed)
                assertEquals("@edu_channel", (s as PolicyState.Allowed).channelHandle)
            }

            // Return to main video
            engine.onVideoDetected(video)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)
        }
    }

    @Test
    fun `challenger - cold start directly into ad stays in Verifying fail-closed`() = runTest {
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val coldAd = VideoIdentity(videoId = "cold_ad", title = "Commercial", channelHandle = "@brand", isAd = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(coldAd)
            val verifying = awaitItem()
            assertTrue(verifying is PolicyState.Verifying)
            assertEquals(coldAd, (verifying as PolicyState.Verifying).identity)

            // Remains fail-closed in Verifying
            expectNoEvents()
            assertTrue(engine.state.value is PolicyState.Verifying)
        }
    }

    @Test
    fun `challenger - pre-roll ad on video switch with distinct fingerprint stays in Verifying`() = runTest {
        fakeRepo.setChannelPolicy("@first_channel", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val v1Fingerprint = VideoFingerprint("vid_1", "biology 101", "handle:@first_channel")
        val v2Fingerprint = VideoFingerprint("vid_2", "gaming highlights", "handle:@second_channel")

        val v1 = VideoIdentity(
            videoId = "vid_1",
            title = "Biology 101",
            channelHandle = "@first_channel",
            explicitFingerprint = v1Fingerprint,
        )

        val preRollAdV2 = VideoIdentity(
            videoId = "ad_v2",
            title = "Product Ad",
            channelHandle = "@sponsor",
            isAd = true,
            explicitFingerprint = v2Fingerprint, // Distinct fingerprint indicates new video switch
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Video 1 plays -> Allowed
            engine.onVideoDetected(v1, v1Fingerprint)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Pre-roll ad for Video 2 arrives with distinct fingerprint
            engine.onVideoDetected(preRollAdV2, v2Fingerprint)
            val adVerifying = awaitItem()
            assertTrue("Pre-roll ad for new video fingerprint must stay in Verifying", adVerifying is PolicyState.Verifying)
            assertEquals(preRollAdV2, (adVerifying as PolicyState.Verifying).identity)

            // Must NOT inherit Video 1's Allowed policy!
            expectNoEvents()
            assertTrue(engine.state.value is PolicyState.Verifying)
        }
    }

    @Test
    fun `challenger - Shorts with isAd true is blocked as ShortsBlocked never Allowed`() = runTest {
        fakeRepo.setChannelPolicy("@productive_creator", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val shortsAd = VideoIdentity(
            videoId = "short_ad_x",
            title = "Sponsored Short",
            channelHandle = "@productive_creator",
            isShorts = true,
            isAd = true,
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(shortsAd)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val s = awaitItem()
            assertTrue("Sponsored Shorts must resolve to ShortsBlocked", s is PolicyState.ShortsBlocked)
            assertEquals(BlockReason.SHORTS, (s as PolicyState.ShortsBlocked).reason)
        }
    }

    // =========================================================================
    // 3. FULLSCREEN & AUTOPLAY FAIL-CLOSED TRANSITIONS
    // =========================================================================

    @Test
    fun `challenger - autoplay transition from Allowed to unclassified channel enters Verifying then AskUser`() = runTest {
        fakeRepo.setChannelPolicy("@mit_lectures", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video1 = VideoIdentity(videoId = "mit_01", title = "Physics 8.01", channelHandle = "@mit_lectures")
        val autoplayVideo = VideoIdentity(videoId = "unknown_01", title = "Random Talk", channelHandle = "@unknown_speaker")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Autoplay kicks in to unknown channel
            engine.onVideoDetected(autoplayVideo)
            val verifying = awaitItem()
            assertTrue("Autoplay transition must enter Verifying fail-closed", verifying is PolicyState.Verifying)
            assertEquals(autoplayVideo, (verifying as PolicyState.Verifying).identity)

            val askUser = awaitItem()
            assertTrue("Unclassified autoplay video must prompt AskUser", askUser is PolicyState.AskUser)
            assertEquals("@unknown_speaker", (askUser as PolicyState.AskUser).channelHandle)
        }
    }

    @Test
    fun `challenger - autoplay transition from Allowed to Distracting enters Verifying then Blocked`() = runTest {
        fakeRepo.setChannelPolicy("@mit_lectures", ChannelPolicy.PRODUCTIVE)
        fakeRepo.setChannelPolicy("@clickbait_network", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video1 = VideoIdentity(videoId = "mit_01", title = "Physics 8.01", channelHandle = "@mit_lectures")
        val distractingAutoplay = VideoIdentity(videoId = "clickbait_01", title = "You Won't Believe This", channelHandle = "@clickbait_network")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Autoplay transitions to distracting video
            engine.onVideoDetected(distractingAutoplay)
            val verifying = awaitItem()
            assertTrue("Autoplay transition to distracting must enter Verifying fail-closed", verifying is PolicyState.Verifying)
            assertEquals(distractingAutoplay, (verifying as PolicyState.Verifying).identity)

            val blocked = awaitItem()
            assertTrue("Must resolve to Blocked", blocked is PolicyState.Blocked)
            assertEquals("@clickbait_network", (blocked as PolicyState.Blocked).channelHandle)
        }
    }

    @Test
    fun `challenger - autoplay transition from Allowed to Shorts enters Verifying then ShortsBlocked`() = runTest {
        fakeRepo.setChannelPolicy("@mit_lectures", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val video1 = VideoIdentity(videoId = "mit_01", title = "Physics 8.01", channelHandle = "@mit_lectures")
        val shortsVideo = VideoIdentity(videoId = "short_01", title = "Quick Reel", channelHandle = "@mit_lectures", isShorts = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(video1)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Transition to Shorts
            engine.onVideoDetected(shortsVideo)
            val verifying = awaitItem()
            assertTrue("Transition to Shorts must enter Verifying", verifying is PolicyState.Verifying)

            val shortsBlocked = awaitItem()
            assertTrue("Must resolve to ShortsBlocked", shortsBlocked is PolicyState.ShortsBlocked)
            assertEquals(BlockReason.SHORTS, (shortsBlocked as PolicyState.ShortsBlocked).reason)
        }
    }

    // =========================================================================
    // 4. OVERLAY MANAGER STATE MACHINE & TOUCH CONSUMPTION INVARIANTS
    // =========================================================================

    @Test
    fun `challenger - overlay manager transitions through all required blocking and verifying states`() {
        // Initial state is Hidden
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)

        // 1. Show Verifying
        overlayManager.showVerifying("Khan Academy", "Calculus Chapter 1")
        val s1 = overlayManager.overlayState.value
        assertTrue(s1 is OverlayState.Verifying)
        assertEquals("Khan Academy", (s1 as OverlayState.Verifying).channelName)
        assertEquals("Calculus Chapter 1", (s1 as OverlayState.Verifying).title)

        // 2. Show Blocked
        var backClicked = false
        overlayManager.showBlocked(
            channelName = "Gaming Stream",
            channelHandle = "@gaming_stream",
            reason = BlockReason.DISTRACTING_CHANNEL,
            onBack = { backClicked = true },
        )
        val s2 = overlayManager.overlayState.value
        assertTrue(s2 is OverlayState.Blocked)
        assertEquals("Gaming Stream", (s2 as OverlayState.Blocked).channelName)
        assertEquals("@gaming_stream", (s2 as OverlayState.Blocked).channelHandle)
        (s2 as OverlayState.Blocked).onBack()
        assertTrue("onBack callback must be callable", backClicked)

        // 3. Show ShortsBlocked
        var shortsBackClicked = false
        overlayManager.showShortsBlocked(onBack = { shortsBackClicked = true })
        val s3 = overlayManager.overlayState.value
        assertTrue(s3 is OverlayState.ShortsBlocked)
        (s3 as OverlayState.ShortsBlocked).onBack()
        assertTrue(shortsBackClicked)

        // 4. Show AskUser
        var classifiedHandle = ""
        var classifiedPolicy: ChannelPolicy? = null
        overlayManager.showAskUser(
            channelName = "Professor X",
            channelHandle = "@prof_x",
            onClassify = { handle, policy ->
                classifiedHandle = handle
                classifiedPolicy = policy
            },
        )
        val s4 = overlayManager.overlayState.value
        assertTrue(s4 is OverlayState.AskUser)
        (s4 as OverlayState.AskUser).onClassify("@prof_x", ChannelPolicy.PRODUCTIVE)
        assertEquals("@prof_x", classifiedHandle)
        assertEquals(ChannelPolicy.PRODUCTIVE, classifiedPolicy)

        // 5. Hide (Allowed)
        overlayManager.hide()
        assertEquals(OverlayState.Hidden, overlayManager.overlayState.value)
    }

    @Test
    fun `challenger - classifying old channel after switching to new video does not alter active video policy`() = runTest {
        fakeRepo.setChannelPolicy("@distracting_stream", ChannelPolicy.DISTRACTING)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val unclassifiedVideo = VideoIdentity(videoId = "v_unclass", title = "Unclassified", channelHandle = "@unclassified_chan")
        val newVideo = VideoIdentity(videoId = "v_distract", title = "Gaming", channelHandle = "@distracting_stream")

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Video 1 -> AskUser
            engine.onVideoDetected(unclassifiedVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            val askState = awaitItem()
            assertTrue(askState is PolicyState.AskUser)
            assertEquals("@unclassified_chan", (askState as PolicyState.AskUser).channelHandle)

            // User switches to Video 2 before classifying Video 1
            engine.onVideoDetected(newVideo)
            val v2Verifying = awaitItem()
            assertTrue(v2Verifying is PolicyState.Verifying)
            val v2Blocked = awaitItem()
            assertTrue(v2Blocked is PolicyState.Blocked)
            assertEquals("@distracting_stream", (v2Blocked as PolicyState.Blocked).channelHandle)

            // User clicks "Productive" on the old dialog for @unclassified_chan
            engine.onUserClassified("@unclassified_chan", ChannelPolicy.PRODUCTIVE)

            // Old channel policy is persisted in repository
            assertEquals(ChannelPolicy.PRODUCTIVE, fakeRepo.getChannelPolicy("@unclassified_chan"))

            // BUT active state must NOT be changed to Allowed for Video 2!
            expectNoEvents()
            val currentPolicy = engine.state.value
            assertTrue("Active state must remain Blocked for Video 2, got $currentPolicy", currentPolicy is PolicyState.Blocked)
            assertEquals("@distracting_stream", (currentPolicy as PolicyState.Blocked).channelHandle)
        }
    }

    @Test
    fun `challenger - switching from allowed video to new video with pre-roll ad and isNewVideoSwitch stays in Verifying`() = runTest {
        fakeRepo.setChannelPolicy("@edu_allowed", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val allowedVideo = VideoIdentity(videoId = "v_edu", title = "Math", channelHandle = "@edu_allowed")
        val preRollAdNewSwitch = VideoIdentity(
            videoId = "ad_new",
            title = "Promo",
            channelHandle = "@ad_sponsor",
            isAd = true,
            isNewVideoSwitch = true,
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // Primary video allowed
            engine.onVideoDetected(allowedVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Switched to new video starting with pre-roll ad
            engine.onVideoDetected(preRollAdNewSwitch)
            val adVerifying = awaitItem()
            assertTrue(adVerifying is PolicyState.Verifying)
            assertEquals(preRollAdNewSwitch, (adVerifying as PolicyState.Verifying).identity)

            // Stays fail-closed in Verifying (does not inherit @edu_allowed Allowed status)
            expectNoEvents()
            assertTrue(engine.state.value is PolicyState.Verifying)
        }
    }

    @Test
    fun `challenger - rapid switching between shorts and normal videos always blocks shorts immediately`() = runTest {
        fakeRepo.setChannelPolicy("@allowed_channel", ChannelPolicy.PRODUCTIVE)
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val normalVideo = VideoIdentity(videoId = "norm_1", title = "Normal Video", channelHandle = "@allowed_channel")
        val shortVideo = VideoIdentity(videoId = "short_1", title = "Short Reel", channelHandle = "@allowed_channel", isShorts = true)

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            // 1. Normal video -> Allowed
            engine.onVideoDetected(normalVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // 2. Short -> ShortsBlocked
            engine.onVideoDetected(shortVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.ShortsBlocked)

            // 3. Normal video -> Allowed
            engine.onVideoDetected(normalVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // 4. Short -> ShortsBlocked
            engine.onVideoDetected(shortVideo)
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `challenger - display name only channel with spaces resolves correctly and avoids fake handle synthesis`() = runTest {
        fakeRepo.setChannelPolicy("name:mit opencourseware", ChannelPolicy.PRODUCTIVE, "MIT OpenCourseWare")
        engine = YouTubePolicyEngine(fakeRepo, backgroundScope)

        val nameOnlyIdentity = VideoIdentity(
            videoId = "vid_mit_spaces",
            title = "Linear Algebra",
            channelIdentity = ChannelIdentity(
                verifiedHandle = null,
                displayName = "MIT OpenCourseWare",
            ),
        )

        engine.state.test {
            assertEquals(PolicyState.Verifying(null), awaitItem())

            engine.onVideoDetected(nameOnlyIdentity)
            assertTrue(awaitItem() is PolicyState.Verifying)

            val allowed = awaitItem()
            assertTrue("Display name channel should resolve through canonical key without synthetic handle", allowed is PolicyState.Allowed)
            val allowedState = allowed as PolicyState.Allowed
            assertEquals("name:mit opencourseware", allowedState.channelIdentity.canonicalKey)
            assertNull("verifiedHandle must be null", allowedState.channelIdentity.verifiedHandle)
        }
    }
}

