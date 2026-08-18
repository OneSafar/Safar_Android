package com.safarparmar.app.feature.youtubeinsights

data class YoutubeUiNode(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val className: String? = null,
    val selected: Boolean = false,
    val clickable: Boolean = false,
)

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

enum class YoutubeBlockScope(val wire: String) {
    OFF("off"), PROTECTED("protected"), ALWAYS("always");

    fun applies(protectedNow: Boolean): Boolean = this == ALWAYS || (this == PROTECTED && protectedNow)

    companion object {
        fun fromWire(value: String?): YoutubeBlockScope = entries.firstOrNull { it.wire == value } ?: OFF
    }
}

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
