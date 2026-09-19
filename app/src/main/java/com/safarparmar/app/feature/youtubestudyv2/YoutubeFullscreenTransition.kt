package com.safarparmar.app.feature.youtubestudyv2

/** Keep owner evidence across a layout transition, never across a new video tap. */
internal class YoutubeFullscreenTransition {
    private var owner: YoutubeV2Observation? = null
    private var observedAt = 0L

    fun clear() { owner = null }

    fun observe(current: YoutubeV2Observation, now: Long): YoutubeV2Observation {
        if (current.kind != YoutubeV2ContentKind.VIDEO || !current.watchScreenConfirmed) return current
        if (!current.fullscreen) {
            if (current.exactHandle != null || current.exactChannelId != null) {
                owner = current
                observedAt = now
            }
            return current
        }
        val previous = owner ?: return current
        if ((!current.title.isNullOrBlank() && current.title != previous.title) ||
            (current.exactHandle != null && current.exactHandle != previous.exactHandle) ||
            (current.exactChannelId != null && current.exactChannelId != previous.exactChannelId)) {
            clear()
            return current
        }
        if (current.title.isNullOrBlank() && now - observedAt !in 0..2_500L) return current
        return current.copy(
            title = current.title ?: previous.title,
            exactHandle = current.exactHandle ?: previous.exactHandle,
            exactChannelId = current.exactChannelId ?: previous.exactChannelId,
            displayName = current.displayName ?: previous.displayName,
        )
    }
}
