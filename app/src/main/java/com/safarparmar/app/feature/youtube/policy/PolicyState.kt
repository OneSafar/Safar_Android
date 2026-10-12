package com.safarparmar.app.feature.youtube.policy

import com.safarparmar.app.feature.youtube.model.ChannelIdentity

/**
 * Reason why a video or stream is blocked.
 */
enum class BlockReason {
    DISTRACTING_CHANNEL,
    SHORTS,
    UNVERIFIED,
}

/**
 * Deterministic states for the YouTube content guard state machine.
 */
sealed interface PolicyState {
    /**
     * Initial transient state entered on ANY video identity change.
     * Content is fail-closed before Room DB lookup completes.
     */
    data class Verifying(
        val identity: VideoIdentity? = null,
    ) : PolicyState

    /**
     * Entered when a video's channel is unclassified in the database.
     * Triggers the Compose overlay prompt for user classification.
     */
    data class AskUser(
        val identity: VideoIdentity,
        val channelHandle: String,
    ) : PolicyState {
        val channelIdentity: ChannelIdentity get() = identity.channelIdentity
    }

    /**
     * Video playback is allowed. Overlay is dismissed.
     */
    data class Allowed(
        val identity: VideoIdentity,
        val channelHandle: String,
    ) : PolicyState {
        val channelIdentity: ChannelIdentity get() = identity.channelIdentity
    }

    /**
     * Video playback is blocked. Overlay covers screen.
     */
    data class Blocked(
        val identity: VideoIdentity,
        val channelHandle: String,
        val reason: BlockReason = BlockReason.DISTRACTING_CHANNEL,
    ) : PolicyState {
        val channelIdentity: ChannelIdentity get() = identity.channelIdentity
    }

    /**
     * YouTube Shorts format is detected and blocked independently of channel policy.
     */
    data class ShortsBlocked(
        val identity: VideoIdentity,
        val reason: BlockReason = BlockReason.SHORTS,
    ) : PolicyState
}
