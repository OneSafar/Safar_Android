package com.safarparmar.app.feature.youtubestudyv2

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class YoutubeV2Node(
    val text: String? = null,
    val contentDescription: String? = null,
    val viewId: String? = null,
    val className: String? = null,
    val visibleToUser: Boolean = false,
    val clickable: Boolean = false,
    val selected: Boolean = false,
    val parentIndex: Int? = null,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerY: Int get() = top + height / 2
}

data class YoutubeV2Snapshot(
    val packageName: String,
    val density: Float,
    val screenWidth: Int,
    val screenHeight: Int,
    val nodes: List<YoutubeV2Node>,
)

enum class YoutubeV2ContentKind { VIDEO, SHORTS, NON_PLAYBACK }

data class YoutubeV2Observation(
    val kind: YoutubeV2ContentKind,
    val watchScreenConfirmed: Boolean = false,
    val title: String? = null,
    val exactHandle: String? = null,
    val displayName: String? = null,
    val adPlaying: Boolean = false,
) {
    val hasOwnerEvidence: Boolean
        get() = !exactHandle.isNullOrBlank() || !displayName.isNullOrBlank()

    val stableKey: String
        get() = listOf(kind.name, title.orEmpty(), exactHandle.orEmpty(), displayName.orEmpty(), adPlaying.toString())
            .joinToString("|") { it.trim().lowercase() }
}

/**
 * V2 parser: accessibility detects a watch surface and owner-row evidence, but
 * never converts display text into identity itself. The repository may resolve
 * an exact display alias only when it maps to one cached Channel ID; ambiguity
 * remains fail-closed.
 */
object YoutubeStudyV2Parser {
    private val watchIds = listOf("watch_player", "movie_player", "fullscreen_player", "reel_watch")
    private val miniPlayerIds = listOf("modern_miniplayer", "miniplayer_container")
    private val titleIds = listOf("video_title", "watch_title", "title_text")
    private val ownerIds = listOf("video_owner", "owner_row", "channel_row", "channel_info")
    private val shortsIds = listOf("reel_watch", "shorts_player", "shorts_video")
    private val handleRegex = Regex("@[\\p{L}\\p{N}_.-]{3,30}")
    private val leadingHandleRegex = Regex("^\\s*(@[\\p{L}\\p{N}_.-]{3,30})(?=\\s|$)")

    fun parse(snapshot: YoutubeV2Snapshot): YoutubeV2Observation {
        if (snapshot.packageName != YOUTUBE_PACKAGE) return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        val nodes = snapshot.nodes
        val visible = nodes.indices.filter { nodes[it].visibleToUser }
        if (visible.any { index -> miniPlayerIds.any(nodes[index].viewId.orEmpty().lowercase()::contains) }) {
            return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        }

        val watchMarker = visible.asSequence().map(nodes::get).filter { node ->
            val id = node.viewId.orEmpty().lowercase()
            watchIds.any(id::contains)
        }.maxByOrNull { it.width * it.height }
            ?: return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        val surface = visible.map(nodes::get).filter(::isVideoSurface).maxByOrNull { it.width * it.height }
        // Current tablet YouTube builds expose the real player as a large
        // watch_player FrameLayout and deliberately omit the rendering surface
        // from accessibility. The explicit-tap session gate still prevents a
        // feed preview from becoming a viewing session.
        val playbackRegion = surface ?: watchMarker
        val fullVideoSurface = playbackRegion.width >= snapshot.screenWidth * MIN_SURFACE_WIDTH_RATIO &&
            playbackRegion.height >= snapshot.screenHeight * MIN_SURFACE_HEIGHT_RATIO
        if (!fullVideoSurface) return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)

        val kind = if (visible.any { index ->
                val id = nodes[index].viewId.orEmpty().lowercase()
                shortsIds.any(id::contains)
            }
        ) YoutubeV2ContentKind.SHORTS else YoutubeV2ContentKind.VIDEO

        val title = visible.asSequence()
            .map(nodes::get)
            .firstOrNull { node -> titleIds.any(node.viewId.orEmpty().lowercase()::contains) }
            ?.let { cleanText(it.text ?: it.contentDescription) }

        val owner = findOwnerRow(snapshot, visible, playbackRegion.bottom, title)
        return YoutubeV2Observation(
            kind = kind,
            watchScreenConfirmed = true,
            title = title,
            exactHandle = owner?.handle,
            displayName = owner?.displayName,
            adPlaying = isAdPlayback(nodes, visible, playbackRegion),
        )
    }

    private data class OwnerEvidence(val handle: String?, val displayName: String?)

    private fun findOwnerRow(
        snapshot: YoutubeV2Snapshot,
        visible: List<Int>,
        playerBottom: Int,
        titleText: String? = null,
    ): OwnerEvidence? {
        val nodes = snapshot.nodes
        val density = snapshot.density.coerceAtLeast(1f)
        // Recent YouTube watch pages expose an exact @handle beside the title
        // before their semantic owner card has finished rendering. Restrict the
        // search to the watch-metadata band so handles in comments, descriptions,
        // or recommendations can never become the video owner.
        val metadataHandle = exactHandleInMetadataBand(nodes, visible, playerBottom, density, titleText)
        semanticOwnerCard(snapshot, visible, playerBottom)?.let { evidence ->
            val matchedHandle = evidence.handle ?: metadataHandle?.takeIf { h ->
                evidence.displayName == null || isHandleCompatibleWithDisplay(h, evidence.displayName)
            }
            return evidence.copy(handle = matchedHandle)
        }
        val avatars = visible.filter { index ->
            val node = nodes[index]
            val className = node.className.orEmpty()
            val id = node.viewId.orEmpty().lowercase()
            val widthDp = node.width / density
            val heightDp = node.height / density
            (className.contains("ImageView", true) || id.contains("avatar") || id.contains("channel_image") ||
                node.contentDescription.orEmpty().startsWith("go to channel", true)) &&
                node.bottom >= playerBottom - OWNER_BAND_TOP_SLOP_DP * density &&
                node.top <= playerBottom + OWNER_HANDLE_MAX_OFFSET_DP * density &&
                widthDp in AVATAR_MIN_DP..AVATAR_MAX_DP && heightDp in AVATAR_MIN_DP..AVATAR_MAX_DP &&
                max(widthDp, heightDp) / min(widthDp, heightDp).coerceAtLeast(1f) <= MAX_AVATAR_ASPECT
        }

        val candidates = mutableListOf<Triple<Int, Int, Int>>() // score, avatar index, text index
        avatars.forEach { avatarIndex ->
            val avatar = nodes[avatarIndex]
            visible.forEach { textIndex ->
                if (textIndex == avatarIndex) return@forEach
                val textNode = nodes[textIndex]
                if (!textNode.className.orEmpty().contains("TextView", true)) return@forEach
                val value = cleanText(textNode.text ?: textNode.contentDescription) ?: return@forEach
                if (!isPlausibleOwnerLabel(value)) return@forEach
                val horizontalGapDp = (textNode.left - avatar.right) / density
                if (horizontalGapDp !in MIN_HORIZONTAL_GAP_DP..MAX_HORIZONTAL_GAP_DP) return@forEach
                val overlap = min(avatar.bottom, textNode.bottom) - max(avatar.top, textNode.top)
                if (overlap <= 0 || overlap.toFloat() / min(avatar.height, textNode.height).coerceAtLeast(1) < MIN_VERTICAL_OVERLAP) {
                    return@forEach
                }
                val sharedParent = avatar.parentIndex != null && avatar.parentIndex == textNode.parentIndex
                val sharedGrandparent = parentOf(nodes, avatar.parentIndex) != null &&
                    parentOf(nodes, avatar.parentIndex) == parentOf(nodes, textNode.parentIndex)
                val ownerAncestor = hasOwnerAncestor(nodes, textIndex) || hasOwnerAncestor(nodes, avatarIndex)
                if (!sharedParent && !sharedGrandparent && !ownerAncestor) return@forEach
                val score = (if (sharedParent) 100 else 0) + (if (ownerAncestor) 50 else 0) -
                    abs(avatar.centerY - textNode.centerY)
                candidates += Triple(score, avatarIndex, textIndex)
            }
        }

        val best = candidates.maxByOrNull { it.first } ?: run {
            val avatarDisplay = avatars.asSequence()
                .mapNotNull { index -> cleanOwnerText(nodes[index].contentDescription ?: nodes[index].text) }
                .firstOrNull(::isPlausibleOwnerLabel)
            return semanticOwner(nodes, visible)
                ?.let { evidence ->
                    val matchedHandle = evidence.handle ?: metadataHandle?.takeIf { h ->
                        evidence.displayName == null || isHandleCompatibleWithDisplay(h, evidence.displayName)
                    }
                    evidence.copy(handle = matchedHandle, displayName = evidence.displayName ?: avatarDisplay)
                }
                ?: avatarDisplay?.let { d ->
                    val matchedHandle = metadataHandle?.takeIf { h -> isHandleCompatibleWithDisplay(h, d) }
                    OwnerEvidence(matchedHandle, d)
                }
                ?: metadataHandle?.let { OwnerEvidence(it, null) }
        }
        val groupIndices = visible.filter { index ->
            val node = nodes[index]
            val textNode = nodes[best.third]
            node.parentIndex == textNode.parentIndex ||
                parentOf(nodes, node.parentIndex) == parentOf(nodes, textNode.parentIndex)
        }
        val structuralHandle = groupIndices.asSequence()
            .flatMap { index -> sequenceOf(nodes[index].text, nodes[index].contentDescription) }
            .filterNotNull()
            .mapNotNull { handleRegex.find(it)?.value }
            .firstOrNull()
            ?.let(YoutubeStudyV2Repository::normalizeHandle)
        val display = cleanText(nodes[best.third].text ?: nodes[best.third].contentDescription)
            ?.takeUnless { handleRegex.matches(it) }
        val resolvedHandle = structuralHandle ?: metadataHandle?.takeIf { h ->
            display == null || isHandleCompatibleWithDisplay(h, display)
        }
        return OwnerEvidence(resolvedHandle, display)
    }

    private fun exactHandleInMetadataBand(
        nodes: List<YoutubeV2Node>,
        visible: List<Int>,
        playerBottom: Int,
        density: Float,
        titleText: String? = null,
    ): String? = visible.asSequence()
        .map { index -> index to nodes[index] }
        .filter { (_, node) ->
            val id = node.viewId.orEmpty().lowercase()
            node.bottom >= playerBottom - OWNER_BAND_TOP_SLOP_DP * density &&
                node.top <= playerBottom + (OWNER_HANDLE_MAX_OFFSET_DP * 1.5f) * density &&
                NON_OWNER_TEXT_IDS.none(id::contains)
        }
        .mapNotNull { (index, node) ->
            val values = sequenceOf(node.text, node.contentDescription).filterNotNull()
            val id = node.viewId.orEmpty().lowercase()
            val semanticOwnerProof = hasOwnerAncestor(nodes, index) ||
                ownerIds.any(id::contains) ||
                node.contentDescription.orEmpty().startsWith("go to channel", true)
            val valueAndHandle = values.mapNotNull { value ->
                verifiedUploaderHandle(value, semanticOwnerProof)?.let { value to it }
            }.firstOrNull() ?: return@mapNotNull null
            val (rawValue, handle) = valueAndHandle
            val uploaderMetadataProof = hasUploaderMetadataProof(rawValue)
            // A title may contain @mentions. Position alone is never identity
            // proof: the candidate must look like YouTube's uploader metadata or
            // live inside a semantic owner container.
            if (!semanticOwnerProof && !uploaderMetadataProof) return@mapNotNull null
            val score =
                (if (hasOwnerAncestor(nodes, index)) 1_000 else 0) +
                (if (ownerIds.any(id::contains)) 600 else 0) +
                (if (node.contentDescription.orEmpty().startsWith("go to channel", true)) 400 else 0) +
                (if (uploaderMetadataProof) 250 else 0) +
                (if (node.className.orEmpty().contains("TextView", true)) 50 else 0) -
                ((node.top - playerBottom).coerceAtLeast(0) / density).toInt()
            score to YoutubeStudyV2Repository.normalizeHandle(handle)
        }
        .maxByOrNull { it.first }
        ?.second

    /**
     * YouTube sometimes merges title + uploader metadata into one accessibility
     * node. Split at each handle and accept only the segment that owns the
     * engagement/time metadata. A title mention before that segment is ignored.
     */
    internal fun verifiedUploaderHandle(value: String, semanticOwnerProof: Boolean = false): String? {
        val matches = handleRegex.findAll(value).toList()
        if (matches.isEmpty()) return null
        if (semanticOwnerProof) {
            return matches.last().value.let(YoutubeStudyV2Repository::normalizeHandle)
        }

        val leading = leadingHandleRegex.find(value)?.groupValues?.getOrNull(1)
        matches.forEachIndexed { index, match ->
            val segmentEnd = matches.getOrNull(index + 1)?.range?.first ?: value.length
            val segment = value.substring(match.range.last + 1, segmentEnd)
            val beginsUploaderLine = leading != null && match.range.first == value.indexOf(leading)
            val followsEarlierMention = index > 0
            if ((beginsUploaderLine || followsEarlierMention) && hasUploaderMetadataProof(segment)) {
                return YoutubeStudyV2Repository.normalizeHandle(match.value)
            }
        }
        return null
    }

    private fun hasUploaderMetadataProof(value: String): Boolean {
        val lower = value.lowercase()
        val hasNumbers = value.any { it.isDigit() }
        return UPLOADER_ENGAGEMENT_MARKERS.any(lower::contains) ||
            UPLOADER_CONTEXT_MARKERS.any(lower::contains) ||
            (hasNumbers && (value.contains("·") || value.contains("•") || value.contains("|") || lower.contains("k") || lower.contains("m") || lower.contains("lakh") || lower.contains("crore")))
    }

    private fun hasAncestorWithId(
        nodes: List<YoutubeV2Node>,
        node: YoutubeV2Node,
        markers: List<String>,
    ): Boolean {
        var index = node.parentIndex
        repeat(MAX_ANCESTOR_HOPS) {
            val current = index?.takeIf(nodes.indices::contains) ?: return false
            val id = nodes[current].viewId.orEmpty().lowercase()
            if (markers.any(id::contains)) return true
            index = nodes[current].parentIndex
        }
        return false
    }

    private fun isAdPlayback(
        nodes: List<YoutubeV2Node>,
        visible: List<Int>,
        playbackRegion: YoutubeV2Node,
    ): Boolean = visible.asSequence()
        .map(nodes::get)
        .filter { node -> node.top < playbackRegion.bottom && node.bottom > playbackRegion.top }
        .flatMap { node ->
            sequenceOf(node.text, node.contentDescription, node.viewId)
                .filterNotNull()
        }
        .map(String::lowercase)
        .any { value -> AD_PLAYBACK_MARKERS.any(value::contains) }

    private fun semanticOwnerCard(
        snapshot: YoutubeV2Snapshot,
        visible: List<Int>,
        playerBottom: Int,
    ): OwnerEvidence? {
        val nodes = snapshot.nodes
        val density = snapshot.density.coerceAtLeast(1f)
        return visible.asSequence()
            .filter { index ->
                val node = nodes[index]
                val heightDp = node.height / density
                node.clickable && node.top >= playerBottom - 8 * density &&
                    node.top <= playerBottom + OWNER_CARD_MAX_OFFSET_DP * density &&
                    heightDp in OWNER_CARD_MIN_HEIGHT_DP..OWNER_CARD_MAX_HEIGHT_DP &&
                    node.width <= snapshot.screenWidth * OWNER_CARD_MAX_WIDTH_RATIO &&
                    (hasImageDescendant(nodes, index) || node.contentDescription.orEmpty().startsWith("go to channel", true))
            }
            .mapNotNull { index -> cleanOwnerText(nodes[index].contentDescription ?: nodes[index].text) }
            .firstOrNull(::isPlausibleOwnerLabel)
            ?.let { value ->
                val handle = handleRegex.find(value)?.value?.let(YoutubeStudyV2Repository::normalizeHandle)
                OwnerEvidence(handle, value.takeUnless { handleRegex.matches(it) })
            }
    }

    private fun hasImageDescendant(nodes: List<YoutubeV2Node>, ancestor: Int): Boolean {
        var frontier = listOf(ancestor)
        repeat(OWNER_IMAGE_DESCENDANT_HOPS) {
            frontier = nodes.indices.filter { index -> nodes[index].parentIndex in frontier }
            if (frontier.any { index -> nodes[index].className.orEmpty().contains("ImageView", true) }) return true
            if (frontier.isEmpty()) return false
        }
        return false
    }

    private fun semanticOwner(nodes: List<YoutubeV2Node>, visible: List<Int>): OwnerEvidence? {
        visible.forEach { index ->
            if (!hasOwnerAncestor(nodes, index)) return@forEach
            sequenceOf(nodes[index].text, nodes[index].contentDescription).filterNotNull().forEach { value ->
                handleRegex.find(value)?.value?.let {
                    return OwnerEvidence(YoutubeStudyV2Repository.normalizeHandle(it), null)
                }
            }
        }
        return null
    }

    private fun hasOwnerAncestor(nodes: List<YoutubeV2Node>, start: Int): Boolean {
        var index: Int? = start
        repeat(MAX_ANCESTOR_HOPS) {
            val current = index?.takeIf(nodes.indices::contains) ?: return false
            val id = nodes[current].viewId.orEmpty().lowercase()
            if (ownerIds.any(id::contains)) return true
            index = nodes[current].parentIndex
        }
        return false
    }

    private fun parentOf(nodes: List<YoutubeV2Node>, index: Int?): Int? =
        index?.takeIf(nodes.indices::contains)?.let { nodes[it].parentIndex }

    private fun isVideoSurface(node: YoutubeV2Node): Boolean {
        val className = node.className.orEmpty()
        return className.contains("SurfaceView", true) || className.contains("TextureView", true)
    }

    private fun isPlausibleOwnerLabel(value: String): Boolean {
        if (value.length !in 2..120) return false
        val lower = value.lowercase()
        return INVALID_OWNER_WORDS.none(lower::contains)
    }

    private fun cleanText(value: String?): String? = value?.trim()?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotBlank() }

    internal fun cleanOwnerText(value: String?): String? {
        val cleaned = cleanText(value) ?: return null
        val withoutAction = cleaned.replace(Regex("^go to channel\\s+", RegexOption.IGNORE_CASE), "")
        return withoutAction
            .replace(METADATA_SUFFIX, "")
            .replace(SUBSCRIBER_SUFFIX, "")
            .trim(' ', '.', ',', '·', '-', '•', '|')
            .takeIf { it.isNotBlank() }
    }

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    private const val MIN_SURFACE_WIDTH_RATIO = 0.35f
    private const val MIN_SURFACE_HEIGHT_RATIO = 0.10f
    private const val AVATAR_MIN_DP = 20f
    private const val AVATAR_MAX_DP = 88f
    private const val MAX_AVATAR_ASPECT = 1.35f
    private const val MIN_HORIZONTAL_GAP_DP = -8f
    private const val MAX_HORIZONTAL_GAP_DP = 64f
    private const val MIN_VERTICAL_OVERLAP = 0.35f
    private const val OWNER_CARD_MAX_OFFSET_DP = 280f
    private const val OWNER_HANDLE_MAX_OFFSET_DP = 280f
    private const val OWNER_BAND_TOP_SLOP_DP = 16f
    private const val OWNER_CARD_MIN_HEIGHT_DP = 28f
    private const val OWNER_CARD_MAX_HEIGHT_DP = 140f
    private const val OWNER_CARD_MAX_WIDTH_RATIO = 0.90f
    private const val OWNER_IMAGE_DESCENDANT_HOPS = 4
    private const val MAX_ANCESTOR_HOPS = 5
    private val INVALID_OWNER_WORDS = listOf(
        "views", "likes", "comments", "subscribe", "subscribed", "share", "download",
        "save", "more", "play video", "minutes", "hours", "advertiser", "ad panel",
        "sponsored", "install", "सदस्यता", "टिप्पणियां",
    )
    private val AD_PLAYBACK_MARKERS = listOf(
        "visit advertiser", "skip ad", "stop ad", "ad countdown", "ad badge",
    )
    private val NON_OWNER_TEXT_IDS = listOf("comment", "description", "recommend", "suggest", "transcript")
    private val UPLOADER_ENGAGEMENT_MARKERS = listOf(" view", " views", " like", " likes")
    private val UPLOADER_CONTEXT_MARKERS = listOf(" ago", " watching", " subscriber", " subscribers")
    private val METADATA_SUFFIX = Regex(
        "\\s*(?:·|•|\\|)?\\s*[\\d.,\\u00a0]+(?:\\s*(?:k|m|b|lakh|crore))?\\s+(?:views?|likes?|subscribers?|watching|products?|ago).*$",
        RegexOption.IGNORE_CASE,
    )
    private val SUBSCRIBER_SUFFIX = Regex(
        "\\s+[\\d.,\\u00a0]+(?:\\s*(?:k|m|b|lakh|crore))?\\s+subscribers?\\b.*$",
        RegexOption.IGNORE_CASE,
    )

    internal fun isHandleCompatibleWithDisplay(handle: String, displayName: String): Boolean {
        val h = handle.trim().removePrefix("@").lowercase().filter { it.isLetterOrDigit() }
        val d = displayName.trim().lowercase().filter { it.isLetterOrDigit() }
        if (h.isEmpty() || d.isEmpty()) return true
        if (h == d || h.contains(d) || d.contains(h)) return true
        // Check word-by-word (e.g. "Tanmay Bhat" -> ["tanmay", "bhat"])
        val words = displayName.trim().lowercase().split(Regex("[\\s_.-]+")).filter { it.length >= 3 }
        if (words.isNotEmpty() && words.any { h.contains(it) }) return true
        return false
    }
}
