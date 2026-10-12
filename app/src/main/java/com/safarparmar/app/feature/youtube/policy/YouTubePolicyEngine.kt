package com.safarparmar.app.feature.youtube.policy

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import com.safarparmar.app.feature.youtube.model.CanonicalYouTubeContent
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.VideoFingerprint
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deterministic state machine and policy engine governing YouTube content blocking.
 *
 * Core Invariants:
 * 1. Default to [PolicyState.Verifying] immediately on ANY video identity change (fail-closed before DB lookup).
 * 2. Shorts are evaluated and blocked independently of channel policy ([PolicyState.ShortsBlocked]).
 * 3. Ad transitions retain previous verified channel's policy for the same video session,
 *    or stay in [PolicyState.Verifying] fail-closed on a new video switch or cold start.
 * 4. Stale lookups are discarded using monotonic generation tokens ([currentGeneration]).
 */
@Singleton
class YouTubePolicyEngine(
    private val repository: YouTubeChannelRepository,
    private val scope: CoroutineScope,
) {
    /**
     * Primary constructor for Hilt dependency injection.
     * Uses [SupervisorJob] with [Dispatchers.Default] for production execution.
     */
    @Inject
    constructor(repository: YouTubeChannelRepository) : this(
        repository = repository,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val _state = MutableStateFlow<PolicyState>(PolicyState.Verifying(null))
    val state: StateFlow<PolicyState> = _state.asStateFlow()

    private val currentGeneration = AtomicLong(0L)
    private var activeLookupJob: Job? = null

    private var currentIdentity: VideoIdentity? = null
    private var currentFingerprint: VideoFingerprint = VideoFingerprint.EMPTY

    private var lastVerifiedPolicy: ChannelPolicy? = null
    private var lastVerifiedChannel: String? = null
    private var lastVerifiedFingerprint: VideoFingerprint? = null

    /**
     * Called whenever fused content is extracted from AccessibilityService or MediaSessionManager.
     */
    fun onContentDetected(content: CanonicalYouTubeContent) {
        val identity = VideoIdentity(
            videoId = content.fingerprint.mediaId,
            title = content.title,
            channelHandle = content.channelIdentity.verifiedHandle.orEmpty(),
            channelName = content.channelIdentity.displayName,
            isShorts = content.isShorts,
            isAd = content.isAd,
            isChannelPage = content.isChannelPage,
            explicitFingerprint = content.fingerprint,
        )
        onVideoDetected(identity, content.fingerprint)
    }

    /**
     * Intercepts Shorts detection or navigation click immediately, entering ShortsBlocked.
     */
    fun onShortsDetected() {
        activeLookupJob?.cancel()
        currentGeneration.incrementAndGet()
        val shortsIdentity = VideoIdentity(isShorts = true)
        currentIdentity = shortsIdentity
        currentFingerprint = shortsIdentity.fingerprint
        _state.value = PolicyState.ShortsBlocked(shortsIdentity, BlockReason.SHORTS)
    }

    /**
     * Called whenever video information is extracted from AccessibilityService or MediaSessionManager.
     */
    fun onVideoDetected(identity: VideoIdentity, fingerprint: VideoFingerprint? = null) {
        val resolvedFingerprint = fingerprint ?: identity.fingerprint

        // Prevent duplicate processing if identical video is already resolved
        if (identity.isSameVideo(currentIdentity) && _state.value !is PolicyState.Verifying) {
            return
        }

        // Cancel active lookup job and advance generation token
        activeLookupJob?.cancel()
        val gen = currentGeneration.incrementAndGet()

        currentIdentity = identity
        currentFingerprint = resolvedFingerprint

        // When switching to a new non-ad video, or explicit video switch, clear verified session policy
        if (!identity.isAd || identity.isNewVideoSwitch) {
            lastVerifiedPolicy = null
            lastVerifiedChannel = null
            lastVerifiedFingerprint = null
        }

        // INVARIANT 1: Default to 'Verifying' immediately on ANY video identity change (fail-closed)
        _state.value = PolicyState.Verifying(identity)

        activeLookupJob = scope.launch {
            resolvePolicy(identity, resolvedFingerprint, gen)
        }
    }

    private suspend fun resolvePolicy(
        identity: VideoIdentity,
        fingerprint: VideoFingerprint,
        gen: Long,
    ) {
        // Discard if newer generation token exists
        if (gen != currentGeneration.get()) return

        // INVARIANT 2: Shorts are evaluated and blocked independently of channel policy
        if (identity.isShorts) {
            if (gen != currentGeneration.get()) return
            _state.value = PolicyState.ShortsBlocked(identity, BlockReason.SHORTS)
            return
        }

        // INVARIANT 3: Ad transitions
        if (identity.isAd) {
            if (gen != currentGeneration.get()) return

            val isNewSwitch = identity.isNewVideoSwitch ||
                (identity.explicitFingerprint != null &&
                    lastVerifiedFingerprint != null &&
                    identity.explicitFingerprint.key.isNotEmpty() &&
                    identity.explicitFingerprint.key != lastVerifiedFingerprint?.key)

            if (isNewSwitch || lastVerifiedPolicy == null) {
                // Pre-roll ad on new video switch or cold start: fail-closed, stay in Verifying
                return
            }

            // Same video session mid-roll ad: retain verified policy without querying advertiser
            when (lastVerifiedPolicy) {
                ChannelPolicy.PRODUCTIVE -> {
                    _state.value = PolicyState.Allowed(
                        identity = identity,
                        channelHandle = lastVerifiedChannel ?: identity.channelHandle,
                    )
                }
                ChannelPolicy.DISTRACTING -> {
                    _state.value = PolicyState.Blocked(
                        identity = identity,
                        channelHandle = lastVerifiedChannel ?: identity.channelHandle,
                        reason = BlockReason.DISTRACTING_CHANNEL,
                    )
                }
                null -> Unit
            }
            return
        }

        // Standard Video: Look up classification in repository
        val channelId = identity.channelIdentity
        val handle = if (channelId.hasHandle) {
            channelId.verifiedHandle.orEmpty()
        } else {
            identity.channelHandle.trim().ifEmpty { identity.channelName.trim() }
        }

        if (handle.isEmpty() && channelId.canonicalKey.isEmpty()) {
            // Cannot verify without channel identity; remain fail-closed in Verifying
            return
        }

        val policy = if (channelId.isValid) {
            repository.getPolicy(channelId)
        } else {
            repository.getChannelPolicy(handle)
        }

        // Discard stale DB lookup if generation token changed while suspending
        if (gen != currentGeneration.get()) return

        when (policy) {
            ChannelPolicy.PRODUCTIVE -> {
                lastVerifiedPolicy = ChannelPolicy.PRODUCTIVE
                lastVerifiedChannel = handle.ifEmpty { channelId.canonicalKey }
                lastVerifiedFingerprint = fingerprint
                _state.value = PolicyState.Allowed(identity, handle)
            }
            ChannelPolicy.DISTRACTING -> {
                lastVerifiedPolicy = ChannelPolicy.DISTRACTING
                lastVerifiedChannel = handle.ifEmpty { channelId.canonicalKey }
                lastVerifiedFingerprint = fingerprint
                _state.value = PolicyState.Blocked(identity, handle, BlockReason.DISTRACTING_CHANNEL)
            }
            null -> {
                // Unknown channel: prompt user via Compose overlay
                _state.value = PolicyState.AskUser(identity, handle)
            }
        }
    }

    /**
     * Called when the user classifies an unclassified channel via the AskUser overlay dialog.
     */
    fun onUserClassified(channelHandle: String, policy: ChannelPolicy) {
        val current = currentIdentity

        // Only update active state if the classification corresponds to the current video
        val currentHandle = current?.channelHandle?.trim()?.ifEmpty { current?.channelName?.trim() }
        val matchesCurrent = current != null && (
            channelHandle.equals(currentHandle, ignoreCase = true) ||
            channelHandle.removePrefix("@").equals(currentHandle?.removePrefix("@"), ignoreCase = true) ||
            channelHandle.equals(current.channelName.trim(), ignoreCase = true)
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            if (matchesCurrent && current.channelIdentity.isValid) {
                repository.savePolicy(current.channelIdentity, policy)
            } else {
                repository.saveChannelPolicy(
                    channelHandle = channelHandle,
                    channelName = if (matchesCurrent) current.channelName else "",
                    policy = policy,
                )
            }
        }

        if (matchesCurrent && !current.isShorts) {
            activeLookupJob?.cancel()
            currentGeneration.incrementAndGet()

            lastVerifiedPolicy = policy
            lastVerifiedChannel = channelHandle
            lastVerifiedFingerprint = currentFingerprint

            when (policy) {
                ChannelPolicy.PRODUCTIVE -> {
                    _state.value = PolicyState.Allowed(current, channelHandle)
                }
                ChannelPolicy.DISTRACTING -> {
                    _state.value = PolicyState.Blocked(current, channelHandle, BlockReason.DISTRACTING_CHANNEL)
                }
            }
        }
    }

    /**
     * Called when playback exits or YouTube stops playing.
     */
    fun onPlaybackStopped() {
        activeLookupJob?.cancel()
        currentGeneration.incrementAndGet()
        currentIdentity = null
        currentFingerprint = VideoFingerprint.EMPTY
        lastVerifiedPolicy = null
        lastVerifiedChannel = null
        lastVerifiedFingerprint = null
        _state.value = PolicyState.Verifying(null)
    }
}
