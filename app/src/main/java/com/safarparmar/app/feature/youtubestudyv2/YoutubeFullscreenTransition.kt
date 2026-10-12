package com.safarparmar.app.feature.youtubestudyv2

/** Keep owner evidence across a layout transition, never across a new video tap. */
internal class YoutubeFullscreenTransition {
    private var owner: YoutubeV2Observation? = null
    private var observedAt = 0L
    private var verified = false

    fun clear() {
        owner = null
        verified = false
    }

    /** Only an applied allow-list decision can outlive the short layout grace. */
    fun markVerified(observation: YoutubeV2Observation) {
        if (owner?.stableKey == observation.stableKey) verified = true
    }

    fun observe(current: YoutubeV2Observation, now: Long): YoutubeV2Observation {
        if (current.kind != YoutubeV2ContentKind.VIDEO || !current.watchScreenConfirmed || current.adPlaying) {
            clear()
            return current
        }
        val previous = owner
        if (previous != null) {
            val titleChanged = !current.title.isNullOrBlank() && current.title != previous.title
            val handleChanged = current.exactHandle != null && previous.exactHandle != null &&
                !current.exactHandle.equals(previous.exactHandle, ignoreCase = true)
            val channelChanged = current.exactChannelId != null && previous.exactChannelId != null &&
                current.exactChannelId != previous.exactChannelId
            val sameExactOwner = (current.exactHandle != null &&
                current.exactHandle.equals(previous.exactHandle, ignoreCase = true)) ||
                (current.exactChannelId != null && current.exactChannelId == previous.exactChannelId)
            val sameDisplay = current.displayName != null && previous.displayName != null &&
                YoutubeStudyV2Repository.normalizeDisplay(current.displayName) ==
                    YoutubeStudyV2Repository.normalizeDisplay(previous.displayName)
            val unrelatedOwner = current.hasOwnerEvidence && !sameExactOwner && !sameDisplay
            val displayChanged = current.displayName != null && previous.displayName != null &&
                YoutubeStudyV2Repository.normalizeDisplay(current.displayName) !=
                    YoutubeStudyV2Repository.normalizeDisplay(previous.displayName) && !sameExactOwner
            if (titleChanged || handleChanged || channelChanged || displayChanged || unrelatedOwner) {
                clear()
            } else if (!current.fullscreen || !current.title.isNullOrBlank() ||
                current.hasOwnerEvidence || verified || now - observedAt in 0..2_500L) {
                // The player hides its title/owner on HUD timeout and live-chat
                // updates. Keep missing fields of this same video, including
                // across portrait/fullscreen layouts with different owner nodes.
                val merged = current.copy(
                    title = current.title ?: previous.title,
                    exactHandle = current.exactHandle ?: previous.exactHandle,
                    exactChannelId = current.exactChannelId ?: previous.exactChannelId,
                    displayName = current.displayName ?: previous.displayName,
                )
                owner = merged
                if (current.hasOwnerEvidence) observedAt = now
                return merged
            } else {
                return current
            }
        }
        if (current.hasOwnerEvidence) {
            owner = current
            observedAt = now
        }
        return current
    }
}
