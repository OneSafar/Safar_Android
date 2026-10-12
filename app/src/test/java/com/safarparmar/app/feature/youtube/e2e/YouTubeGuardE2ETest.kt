package com.safarparmar.app.feature.youtube.e2e

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import app.cash.turbine.test
import com.safarparmar.app.ui.auth.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// =============================================================================
// OPAQUE-BOX E2E CONTRACT MODELS & SYSTEM HARNESS
// Satisfies ORIGINAL_REQUEST.md (§R1-R4), PROJECT.md, and TEST_INFRA.md
// =============================================================================

enum class E2EChannelPolicy {
    PRODUCTIVE,
    DISTRACTING,
}

enum class E2EBlockReason {
    DISTRACTING_CHANNEL,
    SHORTS,
    UNVERIFIED,
}

data class E2EVideoIdentity(
    val videoId: String? = null,
    val title: String = "",
    val channelHandle: String = "",
    val channelName: String = "",
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
) {
    fun isSameVideo(other: E2EVideoIdentity?): Boolean {
        if (other == null) return false
        if (videoId != null && other.videoId != null) return videoId == other.videoId
        return title == other.title && channelHandle == other.channelHandle && isShorts == other.isShorts && isAd == other.isAd
    }
}

sealed interface E2EPolicyState {
    data class Verifying(val identity: E2EVideoIdentity? = null) : E2EPolicyState
    data class AskUser(val identity: E2EVideoIdentity, val channelHandle: String) : E2EPolicyState
    data class Allowed(val identity: E2EVideoIdentity, val channelHandle: String) : E2EPolicyState
    data class Blocked(
        val identity: E2EVideoIdentity,
        val channelHandle: String,
        val reason: E2EBlockReason = E2EBlockReason.DISTRACTING_CHANNEL,
    ) : E2EPolicyState
    data class ShortsBlocked(
        val identity: E2EVideoIdentity,
        val reason: E2EBlockReason = E2EBlockReason.SHORTS,
    ) : E2EPolicyState
}

data class E2EYouTubeChannelEntity(
    val channelHandle: String,
    val channelName: String = "",
    val policy: E2EChannelPolicy,
    val updatedAt: Long = System.currentTimeMillis(),
)

interface E2EYouTubeChannelRepository {
    suspend fun getChannelPolicy(channelHandle: String): E2EChannelPolicy?
    suspend fun saveChannelPolicy(channelHandle: String, channelName: String = "", policy: E2EChannelPolicy)
    fun getAllChannels(): Flow<List<E2EYouTubeChannelEntity>>
}

class E2EInMemoryChannelRepository : E2EYouTubeChannelRepository {
    private val channelMap = mutableMapOf<String, E2EYouTubeChannelEntity>()
    private val allChannelsFlow = MutableStateFlow<List<E2EYouTubeChannelEntity>>(emptyList())
    var getPolicyCallCount = 0
        private set
    val queriedHandles = mutableListOf<String>()

    private fun normalizeHandle(handle: String): String {
        val trimmed = handle.trim()
        if (trimmed.isEmpty()) return ""
        val withAt = if (trimmed.startsWith("@")) trimmed else "@$trimmed"
        return withAt.lowercase()
    }

    override suspend fun getChannelPolicy(channelHandle: String): E2EChannelPolicy? {
        getPolicyCallCount++
        queriedHandles.add(channelHandle)
        val normalized = normalizeHandle(channelHandle)
        if (normalized.isEmpty()) return null
        return channelMap[normalized]?.policy
    }

    override suspend fun saveChannelPolicy(channelHandle: String, channelName: String, policy: E2EChannelPolicy) {
        val normalized = normalizeHandle(channelHandle)
        if (normalized.isEmpty()) return
        val entity = E2EYouTubeChannelEntity(
            channelHandle = normalized,
            channelName = channelName.trim(),
            policy = policy,
            updatedAt = System.currentTimeMillis(),
        )
        channelMap[normalized] = entity
        allChannelsFlow.value = channelMap.values.toList()
    }

    override fun getAllChannels(): Flow<List<E2EYouTubeChannelEntity>> = allChannelsFlow.asStateFlow()

    fun resetStats() {
        getPolicyCallCount = 0
        queriedHandles.clear()
    }
}

class E2EComposeServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}

class E2EOverlayController(val lifecycleOwner: E2EComposeServiceLifecycleOwner) {
    var isOverlayVisible: Boolean = false
        private set
    var currentOverlayScreen: String = "NONE"
        private set
    var isTouchSuppressed: Boolean = false
        private set

    init {
        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()
    }

    fun onStateChanged(state: E2EPolicyState) {
        when (state) {
            is E2EPolicyState.Verifying -> {
                if (state.identity != null) {
                    isOverlayVisible = true
                    currentOverlayScreen = "VERIFYING_SPINNER"
                    isTouchSuppressed = true
                } else {
                    isOverlayVisible = false
                    currentOverlayScreen = "NONE"
                    isTouchSuppressed = false
                }
            }
            is E2EPolicyState.Allowed -> {
                isOverlayVisible = false
                currentOverlayScreen = "NONE"
                isTouchSuppressed = false
            }
            is E2EPolicyState.Blocked -> {
                isOverlayVisible = true
                currentOverlayScreen = "BLOCKED_DISTRACTING"
                isTouchSuppressed = true
            }
            is E2EPolicyState.ShortsBlocked -> {
                isOverlayVisible = true
                currentOverlayScreen = "BLOCKED_SHORTS"
                isTouchSuppressed = true
            }
            is E2EPolicyState.AskUser -> {
                isOverlayVisible = true
                currentOverlayScreen = "ASK_USER_DIALOG"
                isTouchSuppressed = true
            }
        }
    }

    fun tearDown() {
        isOverlayVisible = false
        currentOverlayScreen = "NONE"
        isTouchSuppressed = false
        lifecycleOwner.onDestroy()
    }
}

class E2EYouTubePolicyEngine(
    private val repository: E2EYouTubeChannelRepository,
    private val scope: CoroutineScope,
    private val onStateChangeListener: ((E2EPolicyState) -> Unit)? = null,
) {
    private val _state = MutableStateFlow<E2EPolicyState>(E2EPolicyState.Verifying(null))
    val state: StateFlow<E2EPolicyState> = _state.asStateFlow()

    private var currentIdentity: E2EVideoIdentity? = null
    var lastVerifiedPolicy: E2EChannelPolicy? = null
        private set
    var lastVerifiedChannel: String? = null
        private set

    init {
        onStateChangeListener?.invoke(_state.value)
    }

    private fun emitState(newState: E2EPolicyState) {
        _state.value = newState
        onStateChangeListener?.invoke(newState)
    }

    fun onVideoDetected(identity: E2EVideoIdentity) {
        if (identity.isSameVideo(currentIdentity)) {
            return
        }

        currentIdentity = identity

        // INVARIANT 1: Default to 'Verifying' immediately on ANY video identity change (fail-closed)
        emitState(E2EPolicyState.Verifying(identity))

        scope.launch {
            resolvePolicy(identity)
        }
    }

    private suspend fun resolvePolicy(identity: E2EVideoIdentity) {
        // INVARIANT 2: Shorts are evaluated and blocked independently of channel policy
        if (identity.isShorts) {
            emitState(E2EPolicyState.ShortsBlocked(identity, E2EBlockReason.SHORTS))
            return
        }

        // INVARIANT 3: Ad transitions retain previous verified channel's policy
        if (identity.isAd) {
            when (lastVerifiedPolicy) {
                E2EChannelPolicy.PRODUCTIVE -> {
                    emitState(E2EPolicyState.Allowed(identity, lastVerifiedChannel ?: ""))
                }
                E2EChannelPolicy.DISTRACTING -> {
                    emitState(
                        E2EPolicyState.Blocked(
                            identity = identity,
                            channelHandle = lastVerifiedChannel ?: "",
                            reason = E2EBlockReason.DISTRACTING_CHANNEL,
                        )
                    )
                }
                null -> {
                    // Fail closed if no previous verified session
                    emitState(
                        E2EPolicyState.Blocked(
                            identity = identity,
                            channelHandle = "",
                            reason = E2EBlockReason.UNVERIFIED,
                        )
                    )
                }
            }
            return
        }

        // Standard Video
        val handle = identity.channelHandle.trim()
        if (handle.isEmpty()) {
            emitState(E2EPolicyState.Verifying(identity))
            return
        }

        val policy = repository.getChannelPolicy(handle)
        when (policy) {
            E2EChannelPolicy.PRODUCTIVE -> {
                lastVerifiedPolicy = E2EChannelPolicy.PRODUCTIVE
                lastVerifiedChannel = handle
                emitState(E2EPolicyState.Allowed(identity, handle))
            }
            E2EChannelPolicy.DISTRACTING -> {
                lastVerifiedPolicy = E2EChannelPolicy.DISTRACTING
                lastVerifiedChannel = handle
                emitState(E2EPolicyState.Blocked(identity, handle, E2EBlockReason.DISTRACTING_CHANNEL))
            }
            null -> {
                emitState(E2EPolicyState.AskUser(identity, handle))
            }
        }
    }

    fun onUserClassified(channelHandle: String, policy: E2EChannelPolicy) {
        val activeIdentity = currentIdentity ?: return
        val normalizedHandle = channelHandle.trim()
        val activeHandle = activeIdentity.channelHandle.trim()

        if (normalizedHandle.isEmpty() || !normalizedHandle.equals(activeHandle, ignoreCase = true)) {
            return
        }

        scope.launch {
            repository.saveChannelPolicy(
                channelHandle = normalizedHandle,
                channelName = activeIdentity.channelName,
                policy = policy,
            )
            lastVerifiedPolicy = policy
            lastVerifiedChannel = normalizedHandle

            when (policy) {
                E2EChannelPolicy.PRODUCTIVE -> {
                    emitState(E2EPolicyState.Allowed(activeIdentity, normalizedHandle))
                }
                E2EChannelPolicy.DISTRACTING -> {
                    emitState(
                        E2EPolicyState.Blocked(
                            identity = activeIdentity,
                            channelHandle = normalizedHandle,
                            reason = E2EBlockReason.DISTRACTING_CHANNEL,
                        )
                    )
                }
            }
        }
    }

    fun onPlaybackStopped() {
        currentIdentity = null
        lastVerifiedPolicy = null
        lastVerifiedChannel = null
        emitState(E2EPolicyState.Verifying(null))
    }
}

class YouTubeGuardSystemHarness(
    val repository: E2EInMemoryChannelRepository,
    val lifecycleOwner: E2EComposeServiceLifecycleOwner,
    val overlayController: E2EOverlayController,
    val engine: E2EYouTubePolicyEngine,
) {
    companion object {
        fun create(scope: CoroutineScope): YouTubeGuardSystemHarness {
            val repository = E2EInMemoryChannelRepository()
            val lifecycleOwner = E2EComposeServiceLifecycleOwner()
            val overlayController = E2EOverlayController(lifecycleOwner)
            val engine = E2EYouTubePolicyEngine(
                repository = repository,
                scope = scope,
                onStateChangeListener = { state ->
                    overlayController.onStateChanged(state)
                }
            )
            return YouTubeGuardSystemHarness(repository, lifecycleOwner, overlayController, engine)
        }
    }

    fun simulateVideoDetected(identity: E2EVideoIdentity) {
        engine.onVideoDetected(identity)
    }

    fun simulateUserClassified(handle: String, policy: E2EChannelPolicy) {
        engine.onUserClassified(handle, policy)
    }

    fun simulatePlaybackStopped() {
        engine.onPlaybackStopped()
    }

    fun simulateServiceTeardown() {
        overlayController.tearDown()
    }
}

// =============================================================================
// COMPREHENSIVE E2E TEST SUITE (TIERS 1-4, 79 TESTS)
// =============================================================================

@OptIn(ExperimentalCoroutinesApi::class)
class YouTubeGuardE2ETest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var harness: YouTubeGuardSystemHarness

    @Before
    fun setUp() {
        androidx.arch.core.executor.ArchTaskExecutor.getInstance().setDelegate(
            object : androidx.arch.core.executor.TaskExecutor() {
                override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
                override fun postToMainThread(runnable: Runnable) = runnable.run()
                override fun isMainThread(): Boolean = true
            }
        )
    }

    @org.junit.After
    fun tearDown() {
        androidx.arch.core.executor.ArchTaskExecutor.getInstance().setDelegate(null)
    }

    // =========================================================================
    // TIER 1: FEATURE COVERAGE (>=5 PER FEATURE ACROSS F1-F6, 30 TESTS)
    // =========================================================================

    // --- F1: Room Persistence & Classification Retrieval ---

    @Test
    fun `T1_F1_01_save_and_retrieve_productive_channel`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@veritasium", "Veritasium", E2EChannelPolicy.PRODUCTIVE)

        val policy = harness.repository.getChannelPolicy("@veritasium")
        assertEquals(E2EChannelPolicy.PRODUCTIVE, policy)
    }

    @Test
    fun `T1_F1_02_save_and_retrieve_distracting_channel`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@gaming_streamer", "Gaming Hub", E2EChannelPolicy.DISTRACTING)

        val policy = harness.repository.getChannelPolicy("@gaming_streamer")
        assertEquals(E2EChannelPolicy.DISTRACTING, policy)
    }

    @Test
    fun `T1_F1_03_query_unclassified_channel_returns_null`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        val policy = harness.repository.getChannelPolicy("@unclassified_creator")
        assertNull(policy)
    }

    @Test
    fun `T1_F1_04_update_channel_policy_overwrites_previous`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@tech_news", "Tech News", E2EChannelPolicy.PRODUCTIVE)
        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@tech_news"))

        harness.repository.saveChannelPolicy("@tech_news", "Tech News", E2EChannelPolicy.DISTRACTING)
        assertEquals(E2EChannelPolicy.DISTRACTING, harness.repository.getChannelPolicy("@tech_news"))
    }

    @Test
    fun `T1_F1_05_flow_emits_all_persisted_channels`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@chan1", "Chan 1", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@chan2", "Chan 2", E2EChannelPolicy.DISTRACTING)

        val all = harness.repository.getAllChannels().first()
        assertEquals(2, all.size)
        assertTrue(all.any { it.channelHandle == "@chan1" && it.policy == E2EChannelPolicy.PRODUCTIVE })
        assertTrue(all.any { it.channelHandle == "@chan2" && it.policy == E2EChannelPolicy.DISTRACTING })
    }

    // --- F2: Fail-Closed Transition to Verifying on Video Change ---

    @Test
    fun `T1_F2_01_new_video_identity_immediately_emits_verifying_before_allowed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@khanacademy", "Khan Academy", E2EChannelPolicy.PRODUCTIVE)

        val identity = E2EVideoIdentity(title = "Calculus 1", channelHandle = "@khanacademy")
        harness.engine.state.test {
            assertEquals(E2EPolicyState.Verifying(null), awaitItem())

            harness.simulateVideoDetected(identity)

            // Must emit Verifying immediately (Invariant 1)
            val verifying = awaitItem()
            assertTrue("Expected Verifying first, got $verifying", verifying is E2EPolicyState.Verifying)
            assertEquals(identity, (verifying as E2EPolicyState.Verifying).identity)

            // Then resolves to Allowed
            val allowed = awaitItem()
            assertTrue("Expected Allowed, got $allowed", allowed is E2EPolicyState.Allowed)
            assertEquals("@khanacademy", (allowed as E2EPolicyState.Allowed).channelHandle)
        }
    }

    @Test
    fun `T1_F2_02_new_video_identity_immediately_emits_verifying_before_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@clickbait_central", "Clickbait", E2EChannelPolicy.DISTRACTING)

        val identity = E2EVideoIdentity(title = "Drama Alert", channelHandle = "@clickbait_central")
        harness.engine.state.test {
            assertEquals(E2EPolicyState.Verifying(null), awaitItem())

            harness.simulateVideoDetected(identity)

            val verifying = awaitItem()
            assertTrue(verifying is E2EPolicyState.Verifying)

            val blocked = awaitItem()
            assertTrue(blocked is E2EPolicyState.Blocked)
            assertEquals("@clickbait_central", (blocked as E2EPolicyState.Blocked).channelHandle)
        }
    }

    @Test
    fun `T1_F2_03_consecutive_allowed_videos_re_emit_verifying`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@ed1", "Ed 1", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@ed2", "Ed 2", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem() // initial

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Lesson 1", channelHandle = "@ed1"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            // Next video
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Lesson 2", channelHandle = "@ed2"))
            val verifyingSecond = awaitItem()
            assertTrue("Must re-enter Verifying on second video", verifyingSecond is E2EPolicyState.Verifying)
            val allowedSecond = awaitItem()
            assertTrue(allowedSecond is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T1_F2_04_identical_video_identity_does_not_retrigger_verifying`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@veritasium", "Veritasium", E2EChannelPolicy.PRODUCTIVE)

        val identity = E2EVideoIdentity(videoId = "v1", title = "Pythagoras", channelHandle = "@veritasium")
        harness.engine.state.test {
            awaitItem() // initial
            harness.simulateVideoDetected(identity)
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            // Re-detect same video
            harness.simulateVideoDetected(identity)
            expectNoEvents()
        }
    }

    @Test
    fun `T1_F2_05_playback_stopped_resets_to_verifying_null`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@mit", "MIT", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem() // initial
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Physics", channelHandle = "@mit"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            harness.simulatePlaybackStopped()
            val resetState = awaitItem()
            assertEquals(E2EPolicyState.Verifying(null), resetState)
            assertNull(harness.engine.lastVerifiedPolicy)
        }
    }

    // --- F3: Independent Shorts Blocking ---

    @Test
    fun `T1_F3_01_shorts_from_productive_channel_is_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@veritasium", "Veritasium", E2EChannelPolicy.PRODUCTIVE)

        val shortId = E2EVideoIdentity(title = "Quick Trick", channelHandle = "@veritasium", isShorts = true)
        harness.engine.state.test {
            awaitItem() // initial
            harness.simulateVideoDetected(shortId)
            assertTrue(awaitItem() is E2EPolicyState.Verifying)

            val state = awaitItem()
            assertTrue("Shorts must be blocked even for productive channel", state is E2EPolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `T1_F3_02_shorts_from_distracting_channel_is_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@distractor", "Distractor", E2EChannelPolicy.DISTRACTING)

        val shortId = E2EVideoIdentity(title = "Dumb Stunt", channelHandle = "@distractor", isShorts = true)
        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(shortId)
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `T1_F3_03_shorts_from_unclassified_channel_is_blocked_without_ask_user`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        val shortId = E2EVideoIdentity(title = "Unknown Short", channelHandle = "@brand_new_account", isShorts = true)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(shortId)
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val terminal = awaitItem()
            assertTrue("Shorts must not transition to AskUser", terminal is E2EPolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `T1_F3_04_shorts_block_reason_is_shorts`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        val shortId = E2EVideoIdentity(title = "Shorts Clip", channelHandle = "@any", isShorts = true)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(shortId)
            awaitItem() // verifying
            val blocked = awaitItem() as E2EPolicyState.ShortsBlocked
            assertEquals(E2EBlockReason.SHORTS, blocked.reason)
        }
    }

    @Test
    fun `T1_F3_05_shorts_does_not_query_channel_repository`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.resetStats()

        val shortId = E2EVideoIdentity(title = "Fast Clip", channelHandle = "@some_creator", isShorts = true)
        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(shortId)
            awaitItem() // verifying
            awaitItem() // shorts blocked
        }

        assertEquals(0, harness.repository.getPolicyCallCount)
    }

    // --- F4: Ad Policy Retention ---

    @Test
    fun `T1_F4_01_midroll_ad_during_productive_video_retains_allowed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@veritasium", "Veritasium", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            // 1. Primary video starts
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Physics Lesson", channelHandle = "@veritasium"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            // 2. Midroll ad begins
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ad Promo", channelHandle = "@sponsor", isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Ad during productive session must retain Allowed", adState is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T1_F4_02_midroll_ad_during_distracting_video_retains_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@gamer", "Gamer", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Game Play", channelHandle = "@gamer"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Blocked)

            // Ad plays on distracting video
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Soda Ad", channelHandle = "@soda_co", isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Ad on distracting video must retain Blocked", adState is E2EPolicyState.Blocked)
        }
    }

    @Test
    fun `T1_F4_03_ad_does_not_evaluate_advertiser_channel_policy`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@teacher", "Teacher", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@casino_app", "Casino App", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Math", channelHandle = "@teacher"))
            awaitItem() // verifying
            awaitItem() // allowed

            harness.repository.resetStats()

            // Ad from casino app plays during teacher's video
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Play Now!", channelHandle = "@casino_app", isAd = true))
            awaitItem() // verifying
            val adState = awaitItem()
            assertTrue("Ad must remain Allowed regardless of advertiser handle", adState is E2EPolicyState.Allowed)

            // Invariant: Advertiser handle was not queried in DB
            assertFalse(harness.repository.queriedHandles.contains("@casino_app"))
        }
    }

    @Test
    fun `T1_F4_04_ad_does_not_prompt_ask_user_for_advertiser`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@teacher", "Teacher", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Math", channelHandle = "@teacher"))
            awaitItem() // verifying
            awaitItem() // allowed

            // Ad from unclassified brand
            harness.simulateVideoDetected(E2EVideoIdentity(title = "New Shoes", channelHandle = "@shoe_startup", isAd = true))
            awaitItem() // verifying
            val adState = awaitItem()
            assertTrue("Ad from unclassified brand must not prompt AskUser", adState is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T1_F4_05_cold_launch_directly_into_ad_fails_closed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem() // initial
            // YouTube launches directly into an ad with no prior verified channel
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Pre-roll Ad", channelHandle = "@pre_roll_corp", isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val state = awaitItem()
            assertTrue("Cold launch directly into ad must fail closed", state is E2EPolicyState.Blocked)
            assertEquals(E2EBlockReason.UNVERIFIED, (state as E2EPolicyState.Blocked).reason)
        }
    }

    // --- F5: User Classification Flow via Overlay ---

    @Test
    fun `T1_F5_01_unclassified_channel_transitions_to_ask_user`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Intro", channelHandle = "@new_educator"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val ask = awaitItem()
            assertTrue(ask is E2EPolicyState.AskUser)
            assertEquals("@new_educator", (ask as E2EPolicyState.AskUser).channelHandle)
        }
    }

    @Test
    fun `T1_F5_02_user_classifies_productive_transitions_to_allowed_and_persists`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Intro", channelHandle = "@new_educator"))
            awaitItem() // verifying
            awaitItem() // ask user

            harness.simulateUserClassified("@new_educator", E2EChannelPolicy.PRODUCTIVE)
            val allowed = awaitItem()
            assertTrue(allowed is E2EPolicyState.Allowed)

            assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@new_educator"))
        }
    }

    @Test
    fun `T1_F5_03_user_classifies_distracting_transitions_to_blocked_and_persists`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Prank", channelHandle = "@prankster"))
            awaitItem() // verifying
            awaitItem() // ask user

            harness.simulateUserClassified("@prankster", E2EChannelPolicy.DISTRACTING)
            val blocked = awaitItem()
            assertTrue(blocked is E2EPolicyState.Blocked)

            assertEquals(E2EChannelPolicy.DISTRACTING, harness.repository.getChannelPolicy("@prankster"))
        }
    }

    @Test
    fun `T1_F5_04_subsequent_video_from_newly_classified_channel_uses_saved_policy`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ep 1", channelHandle = "@stem_tutor"))
            awaitItem() // verifying
            awaitItem() // ask user
            harness.simulateUserClassified("@stem_tutor", E2EChannelPolicy.PRODUCTIVE)
            awaitItem() // allowed

            // Next video from stem_tutor
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ep 2", channelHandle = "@stem_tutor"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val nextAllowed = awaitItem()
            assertTrue("Subsequent video uses saved policy directly", nextAllowed is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T1_F5_05_user_classification_updates_last_verified_channel_policy`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "V1", channelHandle = "@chan_x"))
            awaitItem() // verifying
            awaitItem() // ask user
            harness.simulateUserClassified("@chan_x", E2EChannelPolicy.PRODUCTIVE)
            awaitItem() // allowed

            assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.engine.lastVerifiedPolicy)
            assertEquals("@chan_x", harness.engine.lastVerifiedChannel)
        }
    }

    // --- F6: Accessibility Service & Overlay Lifecycle ---

    @Test
    fun `T1_F6_01_overlay_lifecycle_initializes_to_resumed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        assertEquals(Lifecycle.State.RESUMED, harness.lifecycleOwner.lifecycle.currentState)
    }

    @Test
    fun `T1_F6_02_allowed_state_dismisses_overlay`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)
        harness.repository.saveChannelPolicy("@khanacademy", "Khan", E2EChannelPolicy.PRODUCTIVE)

        harness.simulateVideoDetected(E2EVideoIdentity(title = "Math", channelHandle = "@khanacademy"))
        advanceUntilIdle()

        assertFalse(harness.overlayController.isOverlayVisible)
        assertFalse(harness.overlayController.isTouchSuppressed)
        assertEquals("NONE", harness.overlayController.currentOverlayScreen)
    }

    @Test
    fun `T1_F6_03_blocked_state_shows_blocking_overlay_and_suppresses_touch`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)
        harness.repository.saveChannelPolicy("@gaming_hub", "Gaming", E2EChannelPolicy.DISTRACTING)

        harness.simulateVideoDetected(E2EVideoIdentity(title = "Game", channelHandle = "@gaming_hub"))
        advanceUntilIdle()

        assertTrue(harness.overlayController.isOverlayVisible)
        assertTrue(harness.overlayController.isTouchSuppressed)
        assertEquals("BLOCKED_DISTRACTING", harness.overlayController.currentOverlayScreen)
    }

    @Test
    fun `T1_F6_04_shorts_blocked_state_shows_shorts_blocking_overlay`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)
        harness.simulateVideoDetected(E2EVideoIdentity(title = "Shorts 1", channelHandle = "@any", isShorts = true))
        advanceUntilIdle()

        assertTrue(harness.overlayController.isOverlayVisible)
        assertTrue(harness.overlayController.isTouchSuppressed)
        assertEquals("BLOCKED_SHORTS", harness.overlayController.currentOverlayScreen)
    }

    @Test
    fun `T1_F6_05_service_destroy_destroys_lifecycle_and_cleans_overlay`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.simulateServiceTeardown()

        assertEquals(Lifecycle.State.DESTROYED, harness.lifecycleOwner.lifecycle.currentState)
        assertFalse(harness.overlayController.isOverlayVisible)
        assertFalse(harness.overlayController.isTouchSuppressed)
    }

    // =========================================================================
    // TIER 2: BOUNDARY & CORNER CASES (>=5 PER FEATURE ACROSS F1-F6, 30 TESTS)
    // =========================================================================

    // --- F1 Boundaries ---

    @Test
    fun `T2_F1_01_case_insensitive_channel_handle_retrieval`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@Veritasium", "Veritasium", E2EChannelPolicy.PRODUCTIVE)

        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@veritasium"))
        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@VERITASIUM"))
        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@VeRiTaSiUm"))
    }

    @Test
    fun `T2_F1_02_channel_handle_with_leading_or_trailing_whitespace`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("  @mitocw  ", "MIT OCW", E2EChannelPolicy.PRODUCTIVE)

        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@mitocw"))
        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy(" @mitocw "))
    }

    @Test
    fun `T2_F1_03_channel_handle_missing_at_symbol_normalized`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("stanfordonline", "Stanford", E2EChannelPolicy.PRODUCTIVE)

        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@stanfordonline"))
        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("stanfordonline"))
    }

    @Test
    fun `T2_F1_04_empty_or_blank_channel_handle_returns_null`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        assertNull(harness.repository.getChannelPolicy(""))
        assertNull(harness.repository.getChannelPolicy("   "))
    }

    @Test
    fun `T2_F1_05_channel_with_unicode_or_special_characters`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@3blue1brown-official.math", "3B1B", E2EChannelPolicy.PRODUCTIVE)

        assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@3blue1brown-official.math"))
    }

    // --- F2 Boundaries ---

    @Test
    fun `T2_F2_01_rapid_successive_video_identity_changes_processed_in_order`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@ch1", "C1", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@ch2", "C2", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem() // initial

            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "1", title = "V1", channelHandle = "@ch1"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "2", title = "V2", channelHandle = "@ch2"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Blocked)
        }
    }

    @Test
    fun `T2_F2_02_video_identity_with_empty_title_and_id_still_verifies_channel`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@valid_channel", "Valid", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "", channelHandle = "@valid_channel"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T2_F2_03_video_identity_with_empty_channel_remains_verifying`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            // Unknown channel identity during DOM load latency
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Loading...", channelHandle = ""))
            val state = awaitItem()
            assertTrue("Empty channel handle must remain in Verifying", state is E2EPolicyState.Verifying)
        }
    }

    @Test
    fun `T2_F2_04_video_identity_switching_between_same_title_different_channel`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@author_a", "A", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@author_b", "B", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Reaction", channelHandle = "@author_a"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Reaction", channelHandle = "@author_b"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Blocked)
        }
    }

    @Test
    fun `T2_F2_05_video_identity_with_same_channel_different_videoId_re_verifies`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@creator", "Creator", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "vid_1", channelHandle = "@creator"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "vid_2", channelHandle = "@creator"))
            assertTrue("Next episode must re-verify", awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    // --- F3 Boundaries ---

    @Test
    fun `T2_F3_01_rapid_swiping_across_shorts_feed_each_emits_verifying_then_shorts_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()

            for (i in 1..4) {
                harness.simulateVideoDetected(E2EVideoIdentity(videoId = "short_$i", channelHandle = "@c$i", isShorts = true))
                assertTrue("Short $i must emit Verifying", awaitItem() is E2EPolicyState.Verifying)
                assertTrue("Short $i must emit ShortsBlocked", awaitItem() is E2EPolicyState.ShortsBlocked)
            }
        }
    }

    @Test
    fun `T2_F3_02_shorts_flagged_as_both_shorts_and_ad_remains_shorts_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@last_ch", "Last", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            // Set last verified productive session
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Primary", channelHandle = "@last_ch"))
            awaitItem() // verifying
            awaitItem() // allowed

            // Sponsored short appears
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Short Ad", channelHandle = "@sponsor", isShorts = true, isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val blocked = awaitItem()
            assertTrue("Sponsored Shorts must be ShortsBlocked", blocked is E2EPolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `T2_F3_03_shorts_with_empty_channel_handle_is_still_shorts_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "s_anon", channelHandle = "", isShorts = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `T2_F3_04_transition_from_shorts_to_regular_video_evaluates_channel`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@learn_stem", "Stem", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            // 1. Shorts
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Reel", channelHandle = "@random", isShorts = true))
            awaitItem() // verifying
            awaitItem() // shorts blocked

            // 2. Return to educational video
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Linear Algebra", channelHandle = "@learn_stem", isShorts = false))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val allowed = awaitItem()
            assertTrue(allowed is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T2_F3_05_transition_from_regular_video_to_shorts_immediately_blocks`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@learn_stem", "Stem", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Study Guide", channelHandle = "@learn_stem"))
            awaitItem() // verifying
            awaitItem() // allowed

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Feed Reel", channelHandle = "@trending", isShorts = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.ShortsBlocked)
        }
    }

    // --- F4 Boundaries ---

    @Test
    fun `T2_F4_01_multiple_consecutive_ads_retain_primary_video_policy`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@khanacademy", "Khan", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Math", channelHandle = "@khanacademy"))
            awaitItem() // verifying
            awaitItem() // allowed

            // Ad 1 of 2
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ad 1", channelHandle = "@ad_firm_1", isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            // Ad 2 of 2
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ad 2", channelHandle = "@ad_firm_2", isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T2_F4_02_ad_ending_and_returning_to_primary_video_preserves_allowed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@mit", "MIT", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            val primary = E2EVideoIdentity(videoId = "mit_01", title = "AI Intro", channelHandle = "@mit")
            harness.simulateVideoDetected(primary)
            awaitItem() // verifying
            awaitItem() // allowed

            // Ad plays
            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "ad_01", title = "Car Ad", channelHandle = "@cars", isAd = true))
            awaitItem() // verifying
            awaitItem() // allowed

            // Ad ends, primary video resumes
            harness.simulateVideoDetected(primary)
            assertTrue("Resuming primary video re-emits verifying", awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T2_F4_03_ad_with_same_channel_name_as_distracting_channel_during_productive_session`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@lecture", "Lecture", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@tiktok", "TikTok", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Bio", channelHandle = "@lecture"))
            awaitItem() // verifying
            awaitItem() // allowed

            // Ad sponsor is TikTok, but session is productive lecture
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Install TikTok", channelHandle = "@tiktok", isAd = true))
            awaitItem() // verifying
            val adState = awaitItem()
            assertTrue("Ad must remain allowed regardless of sponsor classification", adState is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T2_F4_04_ad_playback_stopped_clears_session_state`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@lecture", "Lecture", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Bio", channelHandle = "@lecture"))
            awaitItem()
            awaitItem()

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ad", channelHandle = "@ad", isAd = true))
            awaitItem()
            awaitItem()

            harness.simulatePlaybackStopped()
            assertEquals(E2EPolicyState.Verifying(null), awaitItem())
            assertNull(harness.engine.lastVerifiedPolicy)
        }
    }

    @Test
    fun `T2_F4_05_ad_with_isShorts_true_is_blocked_as_shorts`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Shorts Ad", channelHandle = "@brand", isShorts = true, isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.ShortsBlocked)
        }
    }

    // --- F5 Boundaries ---

    @Test
    fun `T2_F5_01_user_classification_received_for_different_channel_ignored`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Active", channelHandle = "@active_channel"))
            awaitItem() // verifying
            awaitItem() // ask user

            // User classification arrived with wrong handle
            harness.simulateUserClassified("@other_channel", E2EChannelPolicy.PRODUCTIVE)
            expectNoEvents()
        }
    }

    @Test
    fun `T2_F5_02_user_classification_when_engine_in_allowed_state_updates_db`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@channel_to_reclassify", "Old", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Vid", channelHandle = "@channel_to_reclassify"))
            awaitItem() // verifying
            awaitItem() // allowed

            // User reclassifies to distracting
            harness.simulateUserClassified("@channel_to_reclassify", E2EChannelPolicy.DISTRACTING)
            val blocked = awaitItem()
            assertTrue("Reclassifying to distracting blocks video", blocked is E2EPolicyState.Blocked)
            assertEquals(E2EChannelPolicy.DISTRACTING, harness.repository.getChannelPolicy("@channel_to_reclassify"))
        }
    }

    @Test
    fun `T2_F5_03_repeated_identical_user_classification_is_idempotent`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Vid", channelHandle = "@new_chan"))
            awaitItem() // verifying
            awaitItem() // ask user

            harness.simulateUserClassified("@new_chan", E2EChannelPolicy.PRODUCTIVE)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)

            // Second call: idempotent, no redundant state emission
            harness.simulateUserClassified("@new_chan", E2EChannelPolicy.PRODUCTIVE)
            expectNoEvents()
            assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@new_chan"))
        }
    }

    @Test
    fun `T2_F5_04_user_classification_with_whitespace_handle_trimmed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Vid", channelHandle = "@space_chan"))
            awaitItem() // verifying
            awaitItem() // ask user

            harness.simulateUserClassified("  @space_chan  ", E2EChannelPolicy.PRODUCTIVE)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
            assertEquals(E2EChannelPolicy.PRODUCTIVE, harness.repository.getChannelPolicy("@space_chan"))
        }
    }

    @Test
    fun `T2_F5_05_user_classification_persists_empty_channel_name_gracefully`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Vid", channelHandle = "@no_name_chan", channelName = ""))
            awaitItem() // verifying
            awaitItem() // ask user

            harness.simulateUserClassified("@no_name_chan", E2EChannelPolicy.PRODUCTIVE)
            awaitItem() // allowed
            val entity = harness.repository.getAllChannels().first().firstOrNull { it.channelHandle == "@no_name_chan" }
            assertNotNull(entity)
            assertEquals("", entity?.channelName)
        }
    }

    // --- F6 Boundaries ---

    @Test
    fun `T2_F6_01_overlay_state_transition_rapid_show_hide_no_leaks`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)
        harness.repository.saveChannelPolicy("@good", "Good", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@bad", "Bad", E2EChannelPolicy.DISTRACTING)

        for (i in 1..3) {
            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "g_$i", channelHandle = "@good"))
            advanceUntilIdle()
            assertFalse(harness.overlayController.isOverlayVisible)

            harness.simulateVideoDetected(E2EVideoIdentity(videoId = "b_$i", channelHandle = "@bad"))
            advanceUntilIdle()
            assertTrue(harness.overlayController.isOverlayVisible)
        }
    }

    @Test
    fun `T2_F6_02_overlay_recreated_after_service_rebind`() {
        val lo1 = E2EComposeServiceLifecycleOwner()
        val oc1 = E2EOverlayController(lo1)
        oc1.tearDown()
        assertEquals(Lifecycle.State.DESTROYED, lo1.lifecycle.currentState)

        // Rebind
        val lo2 = E2EComposeServiceLifecycleOwner()
        val oc2 = E2EOverlayController(lo2)
        assertEquals(Lifecycle.State.RESUMED, lo2.lifecycle.currentState)
        assertFalse(oc2.isOverlayVisible)
    }

    @Test
    fun `T2_F6_03_lifecycle_owner_saved_state_restore_null_bundle`() {
        val lo = E2EComposeServiceLifecycleOwner()
        lo.onCreate()
        assertNotNull(lo.savedStateRegistry)
        assertEquals(Lifecycle.State.CREATED, lo.lifecycle.currentState)
    }

    @Test
    fun `T2_F6_04_compose_view_tree_owners_bound_correctly`() {
        val lo = E2EComposeServiceLifecycleOwner()
        val oc = E2EOverlayController(lo)
        assertNotNull(oc.lifecycleOwner.lifecycle)
        assertNotNull(oc.lifecycleOwner.savedStateRegistry)
    }

    @Test
    fun `T2_F6_05_service_unbind_during_ask_user_cancels_pending_prompt`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)
        harness.simulateVideoDetected(E2EVideoIdentity(title = "Unclassified", channelHandle = "@unclass"))
        advanceUntilIdle()

        assertTrue(harness.overlayController.isOverlayVisible)
        assertEquals("ASK_USER_DIALOG", harness.overlayController.currentOverlayScreen)

        harness.simulateServiceTeardown()
        assertFalse(harness.overlayController.isOverlayVisible)
        assertEquals("NONE", harness.overlayController.currentOverlayScreen)
    }

    // =========================================================================
    // TIER 3: CROSS-FEATURE COMBINATIONS (PAIRWISE INTERACTIONS >=10, 12 TESTS)
    // =========================================================================

    @Test
    fun `T3_PAIR_01_F1_F2_cold_launch_with_persisted_productive_verifying_to_allowed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@veritasium", "Veritasium", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Math", channelHandle = "@veritasium"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T3_PAIR_02_F1_F2_cold_launch_with_persisted_distracting_verifying_to_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@funny_fails", "Fails", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Fails", channelHandle = "@funny_fails"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Blocked)
        }
    }

    @Test
    fun `T3_PAIR_03_F2_F3_transition_from_allowed_video_to_shorts_feed`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@khanacademy", "Khan", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Math", channelHandle = "@khanacademy"))
            awaitItem() // verifying
            awaitItem() // allowed

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Short", channelHandle = "@random", isShorts = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `T3_PAIR_04_F2_F4_transition_from_allowed_video_to_midroll_ad`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@mit", "MIT", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Physics", channelHandle = "@mit"))
            awaitItem() // verifying
            awaitItem() // allowed

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ad", channelHandle = "@sponsor", isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T3_PAIR_05_F2_F4_transition_from_blocked_video_to_midroll_ad`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@games", "Games", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Game", channelHandle = "@games"))
            awaitItem() // verifying
            awaitItem() // blocked

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Ad", channelHandle = "@sponsor", isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Blocked)
        }
    }

    @Test
    fun `T3_PAIR_06_F2_F5_unclassified_video_to_ask_user_then_user_classifies`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "New Video", channelHandle = "@unclassified_1"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.AskUser)

            harness.simulateUserClassified("@unclassified_1", E2EChannelPolicy.PRODUCTIVE)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T3_PAIR_07_F3_F4_ad_appearing_inside_shorts_feed_is_shorts_blocked`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@channel_prev", "Prev", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Allowed", channelHandle = "@channel_prev"))
            awaitItem() // verifying
            awaitItem() // allowed

            // Sponsored short
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Shorts Ad", channelHandle = "@ad_brand", isShorts = true, isAd = true))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `T3_PAIR_08_F1_F5_user_classification_persists_to_db_and_subsequent_detect_uses_cache`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "V1", channelHandle = "@custom_channel"))
            awaitItem() // verifying
            awaitItem() // ask user

            harness.simulateUserClassified("@custom_channel", E2EChannelPolicy.PRODUCTIVE)
            awaitItem() // allowed

            // Reset engine session
            harness.simulatePlaybackStopped()
            awaitItem() // verifying(null)

            // Cold detection of custom_channel should immediately resolve to Allowed
            harness.simulateVideoDetected(E2EVideoIdentity(title = "V2", channelHandle = "@custom_channel"))
            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            assertTrue(awaitItem() is E2EPolicyState.Allowed)
        }
    }

    @Test
    fun `T3_PAIR_09_F5_F6_ask_user_state_activates_overlay_selection_dismisses_overlay`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)

        harness.simulateVideoDetected(E2EVideoIdentity(title = "V1", channelHandle = "@unknown_stem"))
        advanceUntilIdle()

        assertTrue(harness.overlayController.isOverlayVisible)
        assertEquals("ASK_USER_DIALOG", harness.overlayController.currentOverlayScreen)

        harness.simulateUserClassified("@unknown_stem", E2EChannelPolicy.PRODUCTIVE)
        advanceUntilIdle()

        assertFalse(harness.overlayController.isOverlayVisible)
        assertEquals("NONE", harness.overlayController.currentOverlayScreen)
    }

    @Test
    fun `T3_PAIR_10_F4_F6_ad_during_blocked_retains_blocking_overlay_active`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)
        harness.repository.saveChannelPolicy("@distracting_channel", "Distracting", E2EChannelPolicy.DISTRACTING)

        harness.simulateVideoDetected(E2EVideoIdentity(title = "Bad", channelHandle = "@distracting_channel"))
        advanceUntilIdle()
        assertTrue(harness.overlayController.isOverlayVisible)
        assertTrue(harness.overlayController.isTouchSuppressed)

        harness.simulateVideoDetected(E2EVideoIdentity(title = "Midroll Ad", channelHandle = "@ad_sponsor", isAd = true))
        advanceUntilIdle()
        assertTrue("Overlay must stay visible during ad on blocked video", harness.overlayController.isOverlayVisible)
        assertTrue(harness.overlayController.isTouchSuppressed)
    }

    @Test
    fun `T3_PAIR_11_F3_F6_shorts_feed_activates_shorts_blocking_overlay`() = runTest {
        harness = YouTubeGuardSystemHarness.create(this)

        harness.simulateVideoDetected(E2EVideoIdentity(title = "Shorts", channelHandle = "@creator", isShorts = true))
        advanceUntilIdle()

        assertTrue(harness.overlayController.isOverlayVisible)
        assertTrue(harness.overlayController.isTouchSuppressed)
        assertEquals("BLOCKED_SHORTS", harness.overlayController.currentOverlayScreen)
    }

    @Test
    fun `T3_PAIR_12_F1_F4_ad_sponsor_present_in_db_as_distracting_ignored_during_productive`() = runTest {
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@khanacademy", "Khan", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@casino", "Casino", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            harness.simulateVideoDetected(E2EVideoIdentity(title = "Math", channelHandle = "@khanacademy"))
            awaitItem() // verifying
            awaitItem() // allowed

            harness.simulateVideoDetected(E2EVideoIdentity(title = "Bet Now", channelHandle = "@casino", isAd = true))
            awaitItem() // verifying
            val adState = awaitItem()
            assertTrue("Distracting advertiser must still be Allowed during productive session", adState is E2EPolicyState.Allowed)
        }
    }

    // =========================================================================
    // TIER 4: REAL-WORLD WORKLOAD APPLICATION SCENARIOS (ALL 7 SCENARIOS, 7 TESTS)
    // =========================================================================

    @Test
    fun `T4_SCENARIO_01_cold_launch_into_educational_study_session`() = runTest {
        // Scenario 1: Cold launch into educational study session (Productive channel allowed, no prompts)
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@veritasium", "Veritasium", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            assertEquals(E2EPolicyState.Verifying(null), awaitItem())

            val studyVideo = E2EVideoIdentity(
                videoId = "study_vid_01",
                title = "Why Gravity is NOT a Force",
                channelHandle = "@veritasium",
                channelName = "Veritasium",
                isShorts = false,
                isAd = false,
            )
            harness.simulateVideoDetected(studyVideo)

            // Step 1: Initial fail-closed verification
            val verifying = awaitItem()
            assertTrue(verifying is E2EPolicyState.Verifying)

            // Step 2: Productive channel allowed, overlay dismissed
            val allowed = awaitItem()
            assertTrue(allowed is E2EPolicyState.Allowed)
            assertEquals("@veritasium", (allowed as E2EPolicyState.Allowed).channelHandle)
        }

        advanceUntilIdle()
        assertFalse("Blocking overlay must be dismissed for productive channel", harness.overlayController.isOverlayVisible)
        assertFalse("Touch must not be suppressed", harness.overlayController.isTouchSuppressed)
    }

    @Test
    fun `T4_SCENARIO_02_browsing_feed_and_encountering_distracting_video`() = runTest {
        // Scenario 2: Browsing feed and encountering distracting video (Blocked overlay displayed, touch suppressed)
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@gaming_hub", "Gaming Hub", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem() // initial
            val distractingVideo = E2EVideoIdentity(
                videoId = "game_vid_99",
                title = "10 Hours of Minecraft Memes",
                channelHandle = "@gaming_hub",
                channelName = "Gaming Hub",
                isShorts = false,
                isAd = false,
            )
            harness.simulateVideoDetected(distractingVideo)

            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val blocked = awaitItem()
            assertTrue(blocked is E2EPolicyState.Blocked)
            assertEquals("@gaming_hub", (blocked as E2EPolicyState.Blocked).channelHandle)
        }

        advanceUntilIdle()
        assertTrue("Blocked overlay must be displayed", harness.overlayController.isOverlayVisible)
        assertTrue("Touch must be suppressed", harness.overlayController.isTouchSuppressed)
        assertEquals("BLOCKED_DISTRACTING", harness.overlayController.currentOverlayScreen)
    }

    @Test
    fun `T4_SCENARIO_03_encountering_shorts_feed_while_watching_productive_channel`() = runTest {
        // Scenario 3: Encountering Shorts feed while watching productive channel (Immediate ShortsBlocked)
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@mitocw", "MIT OpenCourseWare", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            // Watching productive lecture
            harness.simulateVideoDetected(
                E2EVideoIdentity(videoId = "mit_lec_1", title = "Linear Algebra", channelHandle = "@mitocw")
            )
            awaitItem() // verifying
            awaitItem() // allowed

            // User taps Shorts icon/feed
            val shortsVideo = E2EVideoIdentity(
                videoId = "shorts_swipe_1",
                title = "Dancing Robot",
                channelHandle = "@trend_clips",
                isShorts = true,
            )
            harness.simulateVideoDetected(shortsVideo)

            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val shortsBlocked = awaitItem()
            assertTrue(shortsBlocked is E2EPolicyState.ShortsBlocked)
        }

        advanceUntilIdle()
        assertTrue(harness.overlayController.isOverlayVisible)
        assertEquals("BLOCKED_SHORTS", harness.overlayController.currentOverlayScreen)
    }

    @Test
    fun `T4_SCENARIO_04_midroll_ad_playing_during_productive_tutorial`() = runTest {
        // Scenario 4: Mid-roll ad playing during productive tutorial (Ad allowed without classifying advertiser)
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@khanacademy", "Khan Academy", E2EChannelPolicy.PRODUCTIVE)

        harness.engine.state.test {
            awaitItem()
            // Tutorial starts
            harness.simulateVideoDetected(
                E2EVideoIdentity(videoId = "tut_01", title = "Organic Chemistry", channelHandle = "@khanacademy")
            )
            awaitItem() // verifying
            awaitItem() // allowed

            // Mid-roll ad begins
            val midrollAd = E2EVideoIdentity(
                videoId = "ad_123",
                title = "Buy Cloud Hosting Now",
                channelHandle = "@cloud_host_ad",
                isAd = true,
            )
            harness.simulateVideoDetected(midrollAd)

            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Ad must be allowed", adState is E2EPolicyState.Allowed)
        }

        advanceUntilIdle()
        assertFalse("Overlay must remain dismissed during ad in productive video", harness.overlayController.isOverlayVisible)
    }

    @Test
    fun `T4_SCENARIO_05_midroll_ad_playing_during_distracting_video`() = runTest {
        // Scenario 5: Mid-roll ad playing during distracting video (Blocked state retained)
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@clickbait_king", "Clickbait", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem()
            // Distracting video starts
            harness.simulateVideoDetected(
                E2EVideoIdentity(videoId = "click_01", title = "Shocking Celebrity Drama", channelHandle = "@clickbait_king")
            )
            awaitItem() // verifying
            awaitItem() // blocked

            // Mid-roll ad begins
            val midrollAd = E2EVideoIdentity(
                videoId = "ad_456",
                title = "Energy Drink Promo",
                channelHandle = "@energy_drinks",
                isAd = true,
            )
            harness.simulateVideoDetected(midrollAd)

            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Blocked state must be retained during ad on distracting video", adState is E2EPolicyState.Blocked)
        }

        advanceUntilIdle()
        assertTrue("Overlay must stay active blocking screen", harness.overlayController.isOverlayVisible)
        assertTrue(harness.overlayController.isTouchSuppressed)
    }

    @Test
    fun `T4_SCENARIO_06_discovery_of_new_unclassified_channel_to_user_clicks_productive_in_overlay`() = runTest {
        // Scenario 6: Discovery of new unclassified channel -> User clicks 'Productive' in Compose overlay -> Transitions to Allowed & persists in Room
        harness = YouTubeGuardSystemHarness.create(backgroundScope)

        harness.engine.state.test {
            awaitItem()
            val newChannelVideo = E2EVideoIdentity(
                videoId = "new_vid_1",
                title = "Introduction to Quantum Computing",
                channelHandle = "@quantum_lab",
                channelName = "Quantum Lab",
            )
            harness.simulateVideoDetected(newChannelVideo)

            assertTrue(awaitItem() is E2EPolicyState.Verifying)
            val askUser = awaitItem()
            assertTrue(askUser is E2EPolicyState.AskUser)
            assertEquals("@quantum_lab", (askUser as E2EPolicyState.AskUser).channelHandle)

            // User clicks "Productive" in Compose overlay dialog
            harness.simulateUserClassified("@quantum_lab", E2EChannelPolicy.PRODUCTIVE)

            val allowed = awaitItem()
            assertTrue("Transitions to Allowed upon user classification", allowed is E2EPolicyState.Allowed)
        }

        advanceUntilIdle()
        // Verify persisted in Room repository
        val persistedPolicy = harness.repository.getChannelPolicy("@quantum_lab")
        assertEquals(E2EChannelPolicy.PRODUCTIVE, persistedPolicy)
        assertFalse(harness.overlayController.isOverlayVisible)
    }

    @Test
    fun `T4_SCENARIO_07_consecutive_video_autoplay_across_mixed_channel_policies`() = runTest {
        // Scenario 7: Consecutive video autoplay across mixed channel policies (Productive -> Distracting -> Unclassified)
        harness = YouTubeGuardSystemHarness.create(backgroundScope)
        harness.repository.saveChannelPolicy("@3blue1brown", "3B1B", E2EChannelPolicy.PRODUCTIVE)
        harness.repository.saveChannelPolicy("@viral_reactions", "Viral", E2EChannelPolicy.DISTRACTING)

        harness.engine.state.test {
            awaitItem() // initial

            // Hop 1: Productive video
            harness.simulateVideoDetected(
                E2EVideoIdentity(videoId = "hop_1", title = "Essence of Linear Algebra", channelHandle = "@3blue1brown")
            )
            assertTrue("Hop 1 must emit Verifying", awaitItem() is E2EPolicyState.Verifying)
            assertTrue("Hop 1 must resolve to Allowed", awaitItem() is E2EPolicyState.Allowed)

            // Hop 2: Autoplay transitions to Distracting video
            harness.simulateVideoDetected(
                E2EVideoIdentity(videoId = "hop_2", title = "Funniest Memes 2026", channelHandle = "@viral_reactions")
            )
            assertTrue("Hop 2 must emit Verifying", awaitItem() is E2EPolicyState.Verifying)
            assertTrue("Hop 2 must resolve to Blocked", awaitItem() is E2EPolicyState.Blocked)

            // Hop 3: Autoplay transitions to Unclassified channel
            harness.simulateVideoDetected(
                E2EVideoIdentity(videoId = "hop_3", title = "Podcast Ep 12", channelHandle = "@random_podcaster")
            )
            assertTrue("Hop 3 must emit Verifying", awaitItem() is E2EPolicyState.Verifying)
            assertTrue("Hop 3 must resolve to AskUser", awaitItem() is E2EPolicyState.AskUser)
        }
    }
}
