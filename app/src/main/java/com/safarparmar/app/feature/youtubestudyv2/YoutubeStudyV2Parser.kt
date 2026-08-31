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

        val owner = findOwnerRow(snapshot, visible, playbackRegion.bottom)
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

    private fun findOwnerRow(snapshot: YoutubeV2Snapshot, visible: List<Int>, playerBottom: Int): OwnerEvidence? {
        val nodes = snapshot.nodes
        val density = snapshot.density.coerceAtLeast(1f)
        // Recent YouTube watch pages expose an exact @handle beside the title
        // before their semantic owner card has finished rendering. Restrict the
        // search to the watch-metadata band so handles in comments, descriptions,
        // or recommendations can never become the video owner.
        val metadataHandle = exactHandleInMetadataBand(nodes, visible, playerBottom, density)
        semanticOwnerCard(snapshot, visible, playerBottom)?.let { evidence ->
            return evidence.copy(handle = metadataHandle ?: evidence.handle)
        }
        val avatars = visible.filter { index ->
            val node = nodes[index]
            val className = node.className.orEmpty()
            val id = node.viewId.orEmpty().lowercase()
            val widthDp = node.width / density
            val heightDp = node.height / density
            (className.contains("ImageView", true) || id.contains("avatar") || id.contains("channel_image")) &&
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

        val best = candidates.maxByOrNull { it.first } ?: return semanticOwner(nodes, visible)
            ?.let { evidence -> evidence.copy(handle = metadataHandle ?: evidence.handle) }
            ?: metadataHandle?.let { OwnerEvidence(it, null) }
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
        return OwnerEvidence(metadataHandle ?: structuralHandle, display)
    }

    private fun exactHandleInMetadataBand(
        nodes: List<YoutubeV2Node>,
        visible: List<Int>,
        playerBottom: Int,
        density: Float,
    ): String? = visible.asSequence()
        .map(nodes::get)
        .filter { node ->
            node.bottom >= playerBottom - OWNER_BAND_TOP_SLOP_DP * density &&
                node.top <= playerBottom + OWNER_HANDLE_MAX_OFFSET_DP * density
        }
        .flatMap { node -> sequenceOf(node.text, node.contentDescription) }
        .filterNotNull()
        .mapNotNull(::cleanText)
        .firstOrNull(handleRegex::matches)
        ?.let(YoutubeStudyV2Repository::normalizeHandle)

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
                    hasImageDescendant(nodes, index)
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
            .replace(SUBSCRIBER_SUFFIX, "")
            .trim(' ', '.', ',', '·', '-')
            .takeIf { it.isNotBlank() }
    }

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    private const val MIN_SURFACE_WIDTH_RATIO = 0.65f
    private const val MIN_SURFACE_HEIGHT_RATIO = 0.12f
    private const val AVATAR_MIN_DP = 24f
    private const val AVATAR_MAX_DP = 72f
    private const val MAX_AVATAR_ASPECT = 1.25f
    private const val MIN_HORIZONTAL_GAP_DP = -4f
    private const val MAX_HORIZONTAL_GAP_DP = 48f
    private const val MIN_VERTICAL_OVERLAP = 0.45f
    private const val OWNER_CARD_MAX_OFFSET_DP = 220f
    private const val OWNER_HANDLE_MAX_OFFSET_DP = 220f
    private const val OWNER_BAND_TOP_SLOP_DP = 8f
    private const val OWNER_CARD_MIN_HEIGHT_DP = 32f
    private const val OWNER_CARD_MAX_HEIGHT_DP = 100f
    private const val OWNER_CARD_MAX_WIDTH_RATIO = 0.75f
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
    private val SUBSCRIBER_SUFFIX = Regex(
        "\\s+[\\d.,\\u00a0]+(?:\\s*(?:k|m|b|lakh|crore))?\\s+subscribers?\\b.*$",
        RegexOption.IGNORE_CASE,
    )
}
