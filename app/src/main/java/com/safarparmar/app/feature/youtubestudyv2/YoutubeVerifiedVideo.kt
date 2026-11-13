package com.safarparmar.app.feature.youtubestudyv2

/**
 * The video that was last allowed, plus every piece of owner evidence seen
 * for it so far. YouTube shows the @handle and the channel name in separate
 * nodes. Scrolling or re-rendering the watch page often hides one of them,
 * which changes [YoutubeV2Observation.stableKey] without changing the video.
 */
internal data class YoutubeVerifiedVideo(
    val title: String,
    val handle: String? = null,
    val displayName: String? = null,
    val channelId: String? = null,
) {
    /** Fills in evidence that was missing at verification time. Never overwrites it. */
    fun mergedWith(observation: YoutubeV2Observation): YoutubeVerifiedVideo = copy(
        handle = handle ?: observation.exactHandle.normalizedHandleOrNull(),
        displayName = displayName ?: observation.displayName.normalizedDisplayOrNull(),
        channelId = channelId ?: observation.exactChannelId?.takeIf { it.isNotBlank() },
    )

    companion object {
        fun from(observation: YoutubeV2Observation): YoutubeVerifiedVideo? {
            val title = observation.title?.takeIf { it.isNotBlank() } ?: return null
            return YoutubeVerifiedVideo(title = title).mergedWith(observation)
        }
    }
}

/**
 * True when [observation] is still the allowed video. In that case it must not
 * be evaluated again just because some owner evidence disappeared.
 *
 * Re-evaluation still happens when the title changes (autoplay, Next, a new
 * tap) or when any visible owner value contradicts what was verified.
 */
internal fun retainsVerifiedPermission(
    verified: YoutubeVerifiedVideo?,
    observation: YoutubeV2Observation,
    classification: YoutubeChannelClassification?,
): Boolean {
    if (verified == null || classification != YoutubeChannelClassification.PRODUCTIVE) return false
    if (observation.kind != YoutubeV2ContentKind.VIDEO || !observation.watchScreenConfirmed) return false
    if (observation.adPlaying) return false
    if (observation.title.isNullOrBlank() || observation.title != verified.title) return false
    fun consistent(seen: String?, current: String?) = seen == null || current == null || seen == current
    return consistent(verified.handle, observation.exactHandle.normalizedHandleOrNull()) &&
        consistent(verified.displayName, observation.displayName.normalizedDisplayOrNull()) &&
        consistent(verified.channelId, observation.exactChannelId?.takeIf { it.isNotBlank() })
}

private fun String?.normalizedHandleOrNull(): String? =
    this?.takeIf { it.isNotBlank() }?.let(YoutubeStudyV2Repository::normalizeHandle)

private fun String?.normalizedDisplayOrNull(): String? =
    this?.let(YoutubeStudyV2Repository::normalizeDisplay)?.takeIf { it.isNotBlank() }
