package com.safarparmar.app.feature.youtubeinsights

/** Pure parser so YouTube UI variants can be fixture-tested without Android services. */
object YoutubeUiParser {
    // Do not match a generic `shorts` id: the permanent bottom-navigation tab is
    // present on normal videos and was the reason every YouTube screen could be
    // mistaken for a playing Short.
    private val shortsIds = listOf("reel_watch", "shorts_player", "shorts_video")
    private val playerIds = listOf(
        "watch_player", "player_view", "video_player", "movie_player", "miniplayer",
        "player_control", "player_overlays", "time_bar",
    )
    private val channelIds = listOf(
        "channel_name", "channel_title", "owner_name", "video_owner", "metadata_channel", "uploader",
    )
    private val adIds = listOf("player_ad", "ad_progress", "skip_ad", "ad_badge")
    private val shortsWords = setOf("shorts", "short")
    private val pauseWords = setOf("pause", "रोकें", "रोकना")
    private val playWords = setOf("play", "चलाएं", "चलाएँ")

    fun parse(snapshot: YoutubeUiSnapshot): YoutubeDetection {
        if (snapshot.packageName != YOUTUBE_PACKAGE) return YoutubeDetection(YoutubeContentKind.NON_PLAYBACK)
        val nodes = snapshot.nodes
        val isAd = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            adIds.any(id::contains) || node.contentDescription.orEmpty().trim().lowercase().let {
                it == "advertisement" || it == "ad" || it == "विज्ञापन"
            }
        }
        if (isAd) return YoutubeDetection(YoutubeContentKind.UNKNOWN)

        val isShorts = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            shortsIds.any(id::contains) ||
                (node.selected && tokens(node).any { it in shortsWords })
        }
        val hasPlayer = isShorts || nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            playerIds.any(id::contains)
        }
        if (!hasPlayer) return YoutubeDetection(YoutubeContentKind.NON_PLAYBACK)

        val channel = nodes.firstNotNullOfOrNull { node ->
            val id = node.viewId.orEmpty().lowercase()
            if (channelIds.any(id::contains)) cleanChannel(node.text ?: node.contentDescription) else null
        } ?: nodes.firstNotNullOfOrNull { node ->
            semanticChannel(node.contentDescription ?: node.text)
        }

        val controls = nodes.flatMap(::tokens).toSet()
        // A visible Pause control means playback is currently running; a visible Play
        // control means it is paused. Shorts often hide controls, so foreground Shorts
        // are treated as playing unless an explicit Play control is visible.
        // Hidden player controls are the normal playing state. An explicit Play
        // control is the reliable signal that playback is paused.
        val playing = controls.none { it in playWords } || controls.any { it in pauseWords }
        return YoutubeDetection(
            kind = if (isShorts) YoutubeContentKind.SHORTS else YoutubeContentKind.VIDEO,
            channelName = channel,
            isPlaying = playing,
        )
    }

    fun normalizeChannel(value: String): String = value.trim().lowercase()
        .removePrefix("@").replace(Regex("\\s+"), " ")

    /**
     * YouTube feed cards expose the owner before the player opens, for example:
     * `Title – 12 minutes – Go to channel Khan Academy – ... – play video`.
     * Capturing it from TYPE_VIEW_CLICKED lets the service classify the first player
     * frame even when the player hierarchy initially omits its owner controls.
     */
    fun channelFromClickedVideo(value: CharSequence?): String? {
        val description = value?.toString()?.trim().orEmpty()
        if (!description.contains("play video", ignoreCase = true)) return null
        return semanticChannel(description)
    }

    private fun semanticChannel(value: CharSequence?): String? {
        val description = value?.toString()?.trim().orEmpty()
        if (description.isBlank()) return null
        val directPrefixes = listOf("Channel ", "चैनल ")
        directPrefixes.firstOrNull { description.startsWith(it, true) }?.let { prefix ->
            return cleanChannel(description.substring(prefix.length))
        }
        CHANNEL_ACTION.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)?.let { return it }
        if (description.contains(", Official Artist Channel", true)) {
            return cleanChannel(description.substringBefore(", Official Artist Channel", ""))
        }
        // Some subscribed channel headers expose only "@handle, 1.2M subscribers".
        HANDLE_WITH_SUBSCRIBERS.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)?.let { return it }
        return null
    }

    private fun cleanChannel(value: String?): String? = value?.trim()
        ?.trimEnd('.', ',', '–', '—', '-')
        ?.trim()
        ?.takeIf { it.length in 1..120 }

    private fun tokens(node: YoutubeUiNode): List<String> = sequenceOf(node.text, node.contentDescription)
        .filterNotNull().flatMap { it.lowercase().split(Regex("[^\\p{L}\\p{N}_@]+" )).asSequence() }
        .filter { it.isNotBlank() }.toList()

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    private val CHANNEL_ACTION = Regex(
        "(?:^|[\\s–—-])(?:go to channel|subscribe to|subscribed to)\\s+(.+?)(?=\\s+[–—]\\s+|[.]?$)",
        setOf(RegexOption.IGNORE_CASE),
    )
    private val HANDLE_WITH_SUBSCRIBERS = Regex(
        "^(@[\\p{L}\\p{N}_.-]+)\\s*,?\\s+.+\\bsubscribers?\\b",
        setOf(RegexOption.IGNORE_CASE),
    )
}
