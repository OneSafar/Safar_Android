package com.safarparmar.app.feature.youtube.overlay

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.feature.youtube.policy.BlockReason

/**
 * Possible states for the guard overlay UI.
 */
sealed interface OverlayState {
    data object Hidden : OverlayState

    data class Verifying(
        val channelName: String = "",
        val title: String = "",
    ) : OverlayState

    data class Blocked(
        val channelName: String,
        val channelHandle: String,
        val reason: BlockReason,
        val onBack: () -> Unit = {},
    ) : OverlayState

    data class ShortsBlocked(
        val onBack: () -> Unit = {},
    ) : OverlayState

    data class AskUser(
        val channelName: String,
        val channelHandle: String,
        val onClassify: (String, ChannelPolicy) -> Unit,
        val onBack: () -> Unit = {},
    ) : OverlayState
}
