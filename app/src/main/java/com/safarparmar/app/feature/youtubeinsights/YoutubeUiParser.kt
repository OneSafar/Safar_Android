package com.safarparmar.app.feature.youtubeinsights

/** Pure parser so YouTube UI variants can be fixture-tested without Android services. */
object YoutubeUiParser {
    // Do not match a generic `shorts` id: the permanent bottom-navigation tab is
    // present on normal videos and was the reason every YouTube screen could be
    // mistaken for a playing Short.
    private val shortsIds = listOf("reel_watch", "shorts_player", "shorts_video")
    private val watchPlayerIds = listOf(
        "watch_player", "movie_player", "watch_panel", "watch_sheet",
        "watch_layout", "watch_view", "single_loop_watch_panel", "fullscreen_player",
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
        val hasWatchPlayer = isShorts || nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            watchPlayerIds.any(id::contains)
        }
        if (!hasWatchPlayer) return YoutubeDetection(YoutubeContentKind.NON_PLAYBACK)

        // Ignore feed cards, search results and up-next thumbnails when extracting the active video's channel.
        // A node that offers the "play video" action is a clickable thumbnail card,
        // not the owner header of the currently active watch screen.
        val channel = nodes.filterNot(::isPlayVideoCard).firstNotNullOfOrNull { node ->
            val id = node.viewId.orEmpty().lowercase()
            if (channelIds.any(id::contains)) cleanChannel(node.text ?: node.contentDescription) else null
        } ?: nodes.filterNot(::isPlayVideoCard).firstNotNullOfOrNull { node ->
            semanticChannel(node.contentDescription ?: node.text)
        }

        val controlNodes = nodes.filterNot(::isPlayVideoCard)
        val hasExplicitPause = controlNodes.any { isPauseControl(it) }
        val hasExplicitPlay = controlNodes.any { isPlayControl(it) }
        val playing = when {
            hasExplicitPlay && !hasExplicitPause -> false
            else -> true
        }
        return YoutubeDetection(
            kind = if (isShorts) YoutubeContentKind.SHORTS else YoutubeContentKind.VIDEO,
            channelName = channel,
            isPlaying = playing,
        )
    }

    private fun isPauseControl(node: YoutubeUiNode): Boolean {
        val desc = (node.contentDescription ?: node.text)?.trim()?.lowercase().orEmpty()
        return desc in pauseWords || desc == "pause" || desc == "रोकें" || desc == "रोकना"
    }

    private fun isPlayControl(node: YoutubeUiNode): Boolean {
        val desc = (node.contentDescription ?: node.text)?.trim()?.lowercase().orEmpty()
        return desc in playWords || desc == "play" || desc == "चलाएं" || desc == "चलाएँ"
    }

    private fun isPlayVideoCard(node: YoutubeUiNode): Boolean {
        val text = (node.contentDescription ?: node.text).orEmpty().lowercase()
        return text.contains("play video") || text.contains("वीडियो चलाएं") || text.contains("वीडियो चलाएँ")
    }

    fun normalizeChannel(value: String): String = value.trim().lowercase()
        .removePrefix("@").replace(Regex("\\s+"), " ")

    /**
     * YouTube feed cards expose the owner before the player opens, for example:
     * `Title – 12 minutes – Go to channel Khan Academy – ... – play video` or
     * `Title – PARMAR SSC – 31K views – play video`.
     * Capturing it from TYPE_VIEW_CLICKED lets the service classify the first player
     * frame even when the player hierarchy initially omits its owner controls.
     */
    fun channelFromClickedVideo(value: CharSequence?): String? {
        val description = value?.toString()?.trim().orEmpty()
        if (!isPlayVideoCard(description)) return null
        return CLICKED_CHANNEL_ACTION.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)
            ?: semanticChannel(description)
    }

    private fun isPlayVideoCard(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("play video") || lower.contains("वीडियो चलाएं") || lower.contains("वीडियो चलाएँ")
    }

    private fun semanticChannel(value: CharSequence?): String? {
        val description = value?.toString()?.trim().orEmpty()
        if (description.isBlank()) return null
        // Do not match plural headers like "Channels that you watch" or "Channels from your search"
        if (description.startsWith("Channels", ignoreCase = true) || description.startsWith("चैनलें", ignoreCase = true)) {
            return null
        }
        val directPrefixes = listOf("Channel: ", "Channel - ", "चैनल: ", "चैनल - ")
        directPrefixes.firstOrNull { description.startsWith(it, true) }?.let { prefix ->
            return cleanChannel(description.substring(prefix.length))
        }
        WATCH_CHANNEL_ACTION.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)?.let { return it }
        if (description.contains(", Official Artist Channel", true)) {
            return cleanChannel(description.substringBefore(", Official Artist Channel", ""))
        }
        // Some subscribed channel headers expose only "@handle, 1.2M subscribers".
        HANDLE_WITH_SUBSCRIBERS.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)?.let { return it }
        return null
    }

    private fun cleanChannel(value: String?): String? = value?.trim()
        ?.trimEnd('.', ',', '–', '—', '-', '•', '|')
        ?.trim()
        ?.takeIf { it.length in 1..120 && !it.equals("Channels", ignoreCase = true) }

    private fun tokens(node: YoutubeUiNode): List<String> = sequenceOf(node.text, node.contentDescription)
        .filterNotNull().flatMap { it.lowercase().split(Regex("[^\\p{L}\\p{N}_@]+" )).asSequence() }
        .filter { it.isNotBlank() }.toList()

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"

    private val CLICKED_CHANNEL_ACTION = Regex(
        "(?:go to channel|by|चैनल)\\s+([^-–—•|]+?)(?=\\s+[–—•|-]|\\s+\\d+[kKmMbB]?\\s+views|\\s+play video|$)",
        setOf(RegexOption.IGNORE_CASE),
    )
    private val WATCH_CHANNEL_ACTION = Regex(
        "^(?:Subscribe to|Subscribed to|Go to channel|सदस्यता लें|सदस्यता ली गई|चैनल पर जाएं)\\s+(.+?)(?:\\.|\\s*[–—•].*)?$",
        setOf(RegexOption.IGNORE_CASE),
    )
    private val HANDLE_WITH_SUBSCRIBERS = Regex(
        "^(@[\\p{L}\\p{N}_.-]+)\\s*,?\\s+.+\\bsubscribers?\\b",
        setOf(RegexOption.IGNORE_CASE),
    )
}
