package com.safarparmar.app.feature.youtube.detector

import com.safarparmar.app.feature.youtube.policy.VideoIdentity

/**
 * Confidence level of the content detector's parsed result.
 */
enum class DetectionConfidence {
    HIGH,       // channelHandle/@handle + title extracted from stable view IDs
    MEDIUM,     // channelName extracted, no handle
    LOW,        // only partial title, insufficient channel data
    UNKNOWN,    // No meaningful data found in the UI tree
}

/**
 * Clean parsed snapshot of what YouTube is currently showing.
 * Produced by [YouTubeSnapshotParser] from the accessibility node tree.
 */
data class YouTubeSnapshot(
    val title: String = "",
    val channelName: String = "",
    val channelHandle: String = "",
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
    val confidence: DetectionConfidence = DetectionConfidence.UNKNOWN,
) {
    fun toVideoIdentity(): VideoIdentity = VideoIdentity(
        videoId = null,
        title = title,
        channelHandle = channelHandle,
        channelName = channelName,
        isShorts = isShorts,
        isAd = isAd,
    )

    companion object {
        val EMPTY = YouTubeSnapshot()
    }
}
