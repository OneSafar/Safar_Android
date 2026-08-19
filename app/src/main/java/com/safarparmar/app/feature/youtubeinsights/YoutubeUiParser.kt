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
    // These IDs can coexist with YouTube's collapsed mini-player. In that state the
    // user is browsing, not viewing the mini-player's channel, so content blocking
    // must not react to whatever metadata remains in the accessibility tree.
    private val browseSurfaceIds = listOf(
        "search_results", "results_list", "browse_fragment", "browse_content",
    )
    private val selectedBrowseDestinationIds = listOf(
        "pivot_home", "pivot_subscriptions", "pivot_library", "pivot_you",
    )
    private val definitiveWatchPlayerIds = listOf(
        "watch_player", "movie_player", "single_loop_watch_panel", "fullscreen_player",
    )
    private val miniPlayerIds = listOf("modern_miniplayer", "miniplayer_container")
    // Only IDs that explicitly describe the owner. `channel_title` and
    // `metadata_channel` are not stable across YouTube versions and have exposed
    // the video title on some builds, which made title words look like a channel.
    private val channelIds = listOf("channel_name", "owner_name", "video_owner", "uploader")
    private val adIds = listOf("player_ad", "ad_progress", "skip_ad", "ad_badge")
    private val shortsWords = setOf("shorts", "short")
    private val pauseWords = setOf("pause", "रोकें", "रोकना")
    private val playWords = setOf("play", "चलाएं", "चलाएँ")

    fun parse(snapshot: YoutubeUiSnapshot): YoutubeDetection {
        if (snapshot.packageName != YOUTUBE_PACKAGE) return YoutubeDetection(YoutubeContentKind.NON_PLAYBACK)
        val nodes = snapshot.nodes
        // Scheduled premieres/upcoming streams create a full watch hierarchy but
        // have no active media. Never count or block their player shell.
        // `playerless_thumbnail` remains in the hierarchy (hidden) during normal
        // playback on current YouTube builds, so resource ID alone is not proof.
        val isPlayerless = nodes.any { node ->
            sequenceOf(node.contentDescription, node.text).filterNotNull().any { value ->
                value.equals("Notify me", ignoreCase = true) ||
                    value.equals("Upcoming", ignoreCase = true) ||
                    value.startsWith("Live in ", ignoreCase = true)
            }
        }
        if (isPlayerless) return YoutubeDetection(YoutubeContentKind.NON_PLAYBACK)
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

        val hasDefinitiveWatchPlayer = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            definitiveWatchPlayerIds.any(id::contains)
        }
        val hasMiniPlayer = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            miniPlayerIds.any(id::contains)
        }
        val isBrowsing = nodes.any { node ->
            val id = node.viewId.orEmpty().lowercase()
            browseSurfaceIds.any(id::contains) ||
                (node.selected && selectedBrowseDestinationIds.any(id::contains))
        }
        // The collapsed mini-player retains a complete watch_player subtree, so it
        // must override even "definitive" player IDs. It is still a browse screen.
        if (!isShorts && (hasMiniPlayer || (isBrowsing && !hasDefinitiveWatchPlayer))) {
            return YoutubeDetection(YoutubeContentKind.NON_PLAYBACK)
        }

        // Scope channel extraction strictly to the active watch header.
        // Everything at or below the Comments section (e.g. 'Comments 75', related videos,
        // Up Next recommendations, suggestions) belongs to other videos and must NEVER be
        // inspected for the currently playing video's owner.
        val relevantNodes = if (isShorts) nodes else activeWatchHeaderNodes(nodes)

        val channel = relevantNodes.filterNot(::isPlayVideoCard).firstNotNullOfOrNull { node ->
            // First check for handle in metadata line (e.g. "@parmarssc 250 likes 4,437 views...")
            extractHandle(node.text) ?: extractHandle(node.contentDescription)
        } ?: relevantNodes.filterNot(::isPlayVideoCard).firstNotNullOfOrNull { node ->
            val id = node.viewId.orEmpty().lowercase()
            if (channelIds.any(id::contains)) cleanChannel(node.text ?: node.contentDescription) else null
        } ?: relevantNodes.filterNot(::isPlayVideoCard).firstNotNullOfOrNull { node ->
            semanticChannel(node.contentDescription ?: node.text)
        } ?: findChannelByAvatarPattern(relevantNodes)

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

    /**
     * Isolates the active video's header (player, title, owner/subscribe row, metadata)
     * and stops scanning as soon as the Comments section or related/recommended video list begins.
     */
    private fun activeWatchHeaderNodes(nodes: List<YoutubeUiNode>): List<YoutubeUiNode> {
        val result = mutableListOf<YoutubeUiNode>()
        for (node in nodes) {
            val id = node.viewId.orEmpty().lowercase()
            val text = (node.contentDescription ?: node.text).orEmpty().trim().lowercase()
            if (isCommentsOrRelatedBoundary(id, text)) {
                break
            }
            result.add(node)
        }
        return result
    }

    private fun isCommentsOrRelatedBoundary(id: String, text: String): Boolean {
        return id.contains("comment") ||
            id.contains("related_video") ||
            id.contains("related_item") ||
            id.contains("suggested") ||
            text.startsWith("comments") ||
            text.startsWith("comment ") ||
            text.startsWith("टिप्पणियां") ||
            text.startsWith("कमेंट") ||
            text.startsWith("up next")
    }

    private fun extractHandle(value: CharSequence?): String? {
        val str = value?.toString()?.trim().orEmpty()
        if (str.isBlank()) return null
        return HANDLE_IN_LINE.find(str)?.groupValues?.getOrNull(1)?.let(::cleanChannel)
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
        if (!isClickedVideoCard(description)) return null
        return CLICKED_CHANNEL_ACTION.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)
            ?: CLICKED_CHANNEL_BEFORE_METADATA.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)
            ?: semanticChannel(description)
    }

    /** True only for an accessibility label belonging to a tappable video card. */
    fun isClickedVideoCard(value: CharSequence?): Boolean =
        isPlayVideoCard(value?.toString().orEmpty())

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
        HANDLE_WITH_ENGAGEMENT.find(description)?.groupValues?.getOrNull(1)?.let(::cleanChannel)?.let { return it }
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
        "(?:go to channel|चैनल पर जाएं)\\s+([^-–—•|]+?)(?=\\s+[–—•|-]|\\s+\\d+[kKmMbB]?\\s+views|\\s+play video|$)",
        setOf(RegexOption.IGNORE_CASE),
    )
    // Current YouTube cards often omit "Go to channel" and expose metadata as:
    // `Title – duration – – NBA - 30K views - ... – play video` or
    // `Title – – – PARMAR SSC - Scheduled for ... - Upcoming – play video`.
    // Anchoring the capture immediately before views/scheduling metadata prevents
    // words in the title (including "SSC" or "by") from becoming a channel.
    private val CLICKED_CHANNEL_BEFORE_METADATA = Regex(
        "[–—]\\s*([^-–—]+?)\\s+-\\s+(?=(?:[\\d.,]+\\s*(?:[kKmMbB]|lakh|crore)?\\s+views\\b|Scheduled for\\b))",
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
    private val HANDLE_WITH_ENGAGEMENT = Regex(
        "^(@[\\p{L}\\p{N}_.-]+)\\s+(?:[\\d.,]+\\s*(?:[kKmMbB]|lakh|crore)?\\s+)?(?:likes?|views?)\\b",
        setOf(RegexOption.IGNORE_CASE),
    )
    private fun findChannelByAvatarPattern(relevantNodes: List<YoutubeUiNode>): String? {
        val avatars = relevantNodes.filter { node ->
            val cls = node.className.orEmpty()
            val id = node.viewId.orEmpty().lowercase()
            (cls.contains("ImageView", true) || id.contains("avatar") || id.contains("channel_avatar") || id.contains("channel_image")) &&
                (node.isSquare || (node.width in 20..300 && node.height in 20..300))
        }
        for (avatar in avatars) {
            val candidate = relevantNodes.firstOrNull { textNode ->
                textNode !== avatar &&
                    !isPlayVideoCard(textNode) &&
                    !textNode.text.isNullOrBlank() &&
                    isValidChannelText(textNode.text) &&
                    (avatar.boundsTop == 0 || kotlin.math.abs(textNode.boundsTop - avatar.boundsTop) < 120)
            }
            if (candidate?.text != null) {
                val cleaned = cleanChannel(candidate.text)
                if (cleaned != null) return cleaned
            }
        }
        return null
    }

    private fun isValidChannelText(value: String?): Boolean {
        val text = value?.trim().orEmpty().lowercase()
        if (text.length < 2 || text.length > 80) return false
        val invalidTokens = listOf(
            "views", "likes", "subscribers", "subscribe", "subscribed", "comments",
            "play video", "share", "download", "save", "remix", "thanks", "clip",
            "more", "ago", "hours", "minutes", "seconds", "days", "months", "years",
            "सदस्यता", "टिप्पणियां", "शेयर", "डाउनलोड"
        )
        return invalidTokens.none { text.contains(it) }
    }

    private val HANDLE_IN_LINE = Regex(
        "(@[\\p{L}\\p{N}_.-]{3,})",
        setOf(RegexOption.IGNORE_CASE),
    )
}
