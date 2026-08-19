package com.safarparmar.app.feature.youtubeinsights

data class YoutubeUiNode(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val className: String? = null,
    val selected: Boolean = false,
    val clickable: Boolean = false,
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
) {
    val width: Int get() = (boundsRight - boundsLeft).coerceAtLeast(0)
    val height: Int get() = (boundsBottom - boundsTop).coerceAtLeast(0)
    val isSquare: Boolean get() = height > 0 && width.toFloat() / height in 0.75f..1.33f
}

data class YoutubeUiSnapshot(
    val nodes: List<YoutubeUiNode>,
    val packageName: String,
)

enum class YoutubeContentKind { SHORTS, VIDEO, NON_PLAYBACK, UNKNOWN }

data class YoutubeDetection(
    val kind: YoutubeContentKind,
    val channelName: String? = null,
    val isPlaying: Boolean = false,
)


data class YoutubeTotals(
    val productiveSeconds: Int = 0,
    val distractingSeconds: Int = 0,
    val shortsSeconds: Int = 0,
    val unidentifiedSeconds: Int = 0,
    val protectedProductiveSeconds: Int = 0,
    val protectedDistractingSeconds: Int = 0,
    val protectedShortsSeconds: Int = 0,
    val protectedUnidentifiedSeconds: Int = 0,
)
