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

enum class YoutubeV2ContentKind { VIDEO, SHORTS, MINI_PLAYER, NON_PLAYBACK }

enum class YoutubeFullscreenSidePanel { NONE, SPONSORED, LIVE_CHAT }

data class YoutubeV2Observation(
    val kind: YoutubeV2ContentKind,
    val watchScreenConfirmed: Boolean = false,
    val title: String? = null,
    val exactHandle: String? = null,
    val displayName: String? = null,
    val adPlaying: Boolean = false,
    val canResumeWatchSession: Boolean = false,
    val exactChannelId: String? = null,
    val fullscreen: Boolean = false,
    val fullscreenSidePanel: YoutubeFullscreenSidePanel = YoutubeFullscreenSidePanel.NONE,
) {
    val hasOwnerEvidence: Boolean
        get() = exactChannelId != null || !exactHandle.isNullOrBlank() || !displayName.isNullOrBlank()

    val stableKey: String
        get() = listOf(kind.name, title.orEmpty(), exactChannelId.orEmpty(), exactHandle.orEmpty(), displayName.orEmpty(), adPlaying.toString())
            .joinToString("|") { it.trim().lowercase() }
}

/**
 * Accessibility-only parser. It identifies the uploader text exposed on this
 * device; the local repository compares that exact normalized handle or name
 * with the user's allow list.
 */
object YoutubeStudyV2Parser {
    private val regularWatchIds = listOf("watch_player", "movie_player", "fullscreen_player")
    private val shortsWatchIds = listOf(
        "reel_watch",
        "reel_player",
        "reel_recycler",
        "reel_container",
        "reel_view",
        "reel_pager",
        "shorts_player",
        "shorts_video",
        "shorts_container",
        "shorts_surface",
    )
    private val miniPlayerIds = listOf("modern_miniplayer", "miniplayer_container")
    private val titleIds = listOf("video_title", "watch_title", "title_text")
    private val ownerIds = listOf("video_owner", "owner_row", "channel_row", "channel_info")
    private val handleRegex = Regex("@[\\p{L}\\p{N}_.-]{3,30}")
    private val leadingHandleRegex = Regex("^\\s*(@[\\p{L}\\p{N}_.-]{3,30})(?=\\s|$)")

    internal fun isMiniPlayerId(viewId: String?): Boolean =
        miniPlayerIds.any(viewId.orEmpty().lowercase()::contains)

    internal fun isMiniPlayerCloseControl(viewId: String?, label: String?): Boolean {
        val id = viewId.orEmpty().lowercase()
        val normalizedLabel = label?.trim()?.lowercase()
        return ((id.contains("miniplayer") || id.contains("mini_player")) &&
            (id.contains("close") || id.contains("dismiss"))) ||
            normalizedLabel in setOf(
                "close player",
                "close minimised player",
                "close minimized player",
                "dismiss minimised player",
                "dismiss minimized player",
                "मिनी प्लेयर बंद करें",
            )
    }

    fun parse(snapshot: YoutubeV2Snapshot): YoutubeV2Observation {
        if (snapshot.packageName != YOUTUBE_PACKAGE) return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        val nodes = snapshot.nodes
        val visible = nodes.indices.filter { nodes[it].visibleToUser }
        // Fast-path 1: detect if user is on the Shorts tab in bottom navigation
        val isShortsTabSelected = visible.any { index ->
            val node = nodes[index]
            val id = node.viewId.orEmpty().lowercase()
            val text = node.text?.trim()?.lowercase().orEmpty()
            val desc = node.contentDescription?.trim()?.lowercase().orEmpty()
            node.selected && (id.contains("pivot_shorts") || text == "shorts" || desc == "shorts" || text == "शॉर्ट्स" || desc == "शॉर्ट्स")
        }
        if (isShortsTabSelected) {
            return YoutubeV2Observation(
                kind = YoutubeV2ContentKind.SHORTS,
                watchScreenConfirmed = true,
            )
        }

        // Check if any feed tab is selected (Home, Subscriptions, Library/You, Explore, Trending)
        val feedSelected = visible.any { index ->
            val node = nodes[index]
            node.selected && listOf("pivot_home", "pivot_subscriptions", "pivot_library", "pivot_trending", "pivot_explore")
                .any(node.viewId.orEmpty().lowercase()::contains)
        }

        val surface = visible.map(nodes::get).filter(::isVideoSurface).maxByOrNull { it.width * it.height }

        // Fast-path 2: detect active Shorts viewer containers or controls (prioritized over regular video!)
        // When not on a feed tab, active Shorts controls or viewer container confirm Shorts playback.
        val shortsWatchMarker = visible.asSequence().map(nodes::get).filter { node ->
            val id = node.viewId.orEmpty().lowercase()
            shortsWatchIds.any(id::contains)
        }.maxByOrNull { it.width * it.height }

        val hasShortsControls = visible.any { index ->
            val node = nodes[index]
            val desc = node.contentDescription?.trim()?.lowercase().orEmpty()
            val text = node.text?.trim()?.lowercase().orEmpty()
            desc.contains("dislike this short") ||
                desc.contains("remix this short") ||
                desc.contains("like this short") ||
                desc.contains("share this short") ||
                desc == "shorts player" || desc == "शॉर्ट्स प्लेयर" ||
                desc.contains("comments for short") ||
                desc.contains("sound used in short") ||
                text == "remix"
        }

        val shortsPlaybackRegion = surface ?: shortsWatchMarker
        if ((shortsWatchMarker != null && hasShortsControls || !feedSelected && (shortsWatchMarker != null || hasShortsControls)) &&
            shortsPlaybackRegion != null &&
            shortsPlaybackRegion.width >= snapshot.screenWidth * MIN_SURFACE_WIDTH_RATIO &&
            shortsPlaybackRegion.height >= snapshot.screenHeight * MIN_SURFACE_HEIGHT_RATIO
        ) {
            return YoutubeV2Observation(
                kind = YoutubeV2ContentKind.SHORTS,
                watchScreenConfirmed = true,
            )
        }

        if (visible.any { index -> isMiniPlayerId(nodes[index].viewId) }) {
            // A floating player is real playback, not an idle feed. Do not use
            // channel names from recommendations underneath it as owner evidence.
            return YoutubeV2Observation(YoutubeV2ContentKind.MINI_PLAYER)
        }

        // If any feed navigation tab is selected, this is feed browsing (Home, Subscriptions, Library).
        // Autoplay previews or recommendation carousels on feeds are NEVER video watch screens.
        if (feedSelected) {
            return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
        }

        // Check for regular video player
        val regularWatchMarker = visible.asSequence().map(nodes::get).filter { node ->
            val id = node.viewId.orEmpty().lowercase()
            regularWatchIds.any(id::contains)
        }.maxByOrNull { it.width * it.height }

        val regularPlaybackRegion = surface ?: regularWatchMarker
        val isRegularWatchScreen = regularWatchMarker != null && regularPlaybackRegion != null &&
            regularPlaybackRegion.width >= snapshot.screenWidth * MIN_SURFACE_WIDTH_RATIO &&
            regularPlaybackRegion.height >= snapshot.screenHeight * MIN_SURFACE_HEIGHT_RATIO

        if (isRegularWatchScreen) {
            val fullscreen = regularWatchMarker.viewId.orEmpty().contains("fullscreen", true) ||
                (snapshot.screenWidth > snapshot.screenHeight &&
                    regularPlaybackRegion.width >= snapshot.screenWidth * 0.85f &&
                    regularPlaybackRegion.height >= snapshot.screenHeight * 0.85f)
            val fullscreenSidePanel = if (fullscreen) detectFullscreenSidePanel(snapshot, visible) else YoutubeFullscreenSidePanel.NONE
            val watchMetadata = watchHeaderMetadata(nodes)
            val hudMetadata = if (fullscreen) fullscreenHudMetadata(snapshot, visible) else null
            // YouTube can leave a visible title-only portrait watch header in
            // the tree after rotation. Merge that partial result with the
            // fullscreen HUD instead of allowing it to suppress the HUD handle.
            val metadata = when {
                watchMetadata == null -> hudMetadata
                hudMetadata == null -> watchMetadata
                else -> WatchHeaderMetadata(
                    title = hudMetadata.title ?: watchMetadata.title,
                    handle = hudMetadata.handle ?: watchMetadata.handle,
                )
            }
            val title = if (metadata != null) metadata.title else visible.asSequence()
                .map(nodes::get)
                .firstOrNull { node -> titleIds.any(node.viewId.orEmpty().lowercase()::contains) }
                ?.let { cleanText(it.text ?: it.contentDescription) }
                ?.takeUnless { isDisallowedTitleText(it.lowercase()) }
                ?: run {
                    // Some builds expose the title with no resource id at all. Fall back to
                    // the first substantial text node directly under the player that isn't
                    // itself uploader/engagement metadata.
                    visible.asSequence()
                        .map(nodes::get)
                        .filter { node ->
                            node.top >= regularPlaybackRegion!!.bottom - 8 * snapshot.density &&
                                node.top <= regularPlaybackRegion.bottom + 120 * snapshot.density &&
                                node.className.orEmpty().let { it.contains("TextView", true) || it.contains("ViewGroup", true) }
                        }
                        .mapNotNull { node -> cleanText(node.text ?: node.contentDescription) }
                        .firstOrNull { value ->
                            val lower = value.lowercase()
                            value.length > 8 && !handleRegex.containsMatchIn(value) &&
                                !hasUploaderMetadataProof(value) &&
                                !isDisallowedTitleText(lower)
                        }
                }

            val owner = findOwnerRow(snapshot, visible.filterNot { isExcludedMetadataNode(nodes, it) }, regularPlaybackRegion!!.bottom, title)
            val ownerChannelIds = visible.asSequence().filter { index ->
                val node = nodes[index]
                node.top >= regularPlaybackRegion.bottom - 24 * snapshot.density &&
                    node.top <= regularPlaybackRegion.bottom + 260 * snapshot.density &&
                    (ownerIds.any(node.viewId.orEmpty()::contains) || hasOwnerAncestor(nodes, index))
            }.flatMap { index -> sequenceOf(nodes[index].text, nodes[index].contentDescription) }
                .filterNotNull().mapNotNull { value ->
                    Regex("(?:https?://(?:www\\.)?youtube\\.com)?/channel/(UC[A-Za-z0-9_-]{22})(?:[/?#\\s]|$)")
                        .find(value)?.groupValues?.get(1)
                        ?: value.trim().takeIf { Regex("UC[A-Za-z0-9_-]{22}").matches(it) }
                }.distinct().toList()
            val ownerChannelId = ownerChannelIds.singleOrNull()
            return YoutubeV2Observation(
                kind = YoutubeV2ContentKind.VIDEO,
                watchScreenConfirmed = true,
                fullscreen = fullscreen,
                title = title,
                exactHandle = if (metadata != null) metadata.handle else owner?.handle,
                exactChannelId = ownerChannelId,
                displayName = owner?.displayName,
                adPlaying = isAdPlayback(nodes, visible, regularPlaybackRegion),
                canResumeWatchSession = (metadata?.handle != null || owner != null || ownerChannelId != null) && !title.isNullOrBlank(),
                fullscreenSidePanel = fullscreenSidePanel,
            )
        }

        return YoutubeV2Observation(YoutubeV2ContentKind.NON_PLAYBACK)
    }

    private data class WatchHeaderMetadata(val title: String?, val handle: String?)

    /**
     * YouTube can replace the normal landscape HUD with a right-side sponsored
     * card or live-chat pane. Handles inside live chat belong to commenters and
     * must never be treated as uploader evidence.
     */
    private fun detectFullscreenSidePanel(
        snapshot: YoutubeV2Snapshot,
        visible: List<Int>,
    ): YoutubeFullscreenSidePanel {
        val rightSideLabels = visible.asSequence()
            .map(snapshot.nodes::get)
            .filter { node -> node.right > snapshot.screenWidth * FULLSCREEN_SIDE_PANEL_LEFT_RATIO }
            .flatMap { node -> sequenceOf(node.text, node.contentDescription, node.viewId).filterNotNull() }
            .map { it.trim().lowercase() }
            .toList()
        return when {
            rightSideLabels.any { value -> LIVE_CHAT_PANEL_MARKERS.any(value::contains) } ->
                YoutubeFullscreenSidePanel.LIVE_CHAT
            rightSideLabels.any { value -> SPONSORED_PANEL_MARKERS.any(value::contains) } ->
                YoutubeFullscreenSidePanel.SPONSORED
            else -> YoutubeFullscreenSidePanel.NONE
        }
    }

    /**
     * In landscape YouTube moves the uploader into the player's top-left HUD.
     * That HUD is outside watch_list and usually exposes a bare @handle without
     * views/likes metadata, so the portrait owner-row rules cannot identify it.
     * Accept the handle only when it forms YouTube's title-over-handle pair in
     * the upper-left HUD; this keeps @mentions in captions and controls out.
     */
    private fun fullscreenHudMetadata(
        snapshot: YoutubeV2Snapshot,
        visible: List<Int>,
    ): WatchHeaderMetadata? {
        val nodes = snapshot.nodes
        val density = snapshot.density.coerceAtLeast(1f)
        val candidates = visible.asSequence().mapNotNull { index ->
            val node = nodes[index]
            val label = cleanText(node.text) ?: cleanText(node.contentDescription) ?: return@mapNotNull null
            if (!handleRegex.matches(label)) return@mapNotNull null
            if (node.left > snapshot.screenWidth * FULLSCREEN_HUD_MAX_LEFT_RATIO ||
                node.top > snapshot.screenHeight * FULLSCREEN_HUD_MAX_TOP_RATIO ||
                isExcludedMetadataNode(nodes, index)
            ) return@mapNotNull null

            val title = visible.asSequence()
                .map(nodes::get)
                .filter { candidate ->
                    candidate.bottom <= node.top + 4 * density &&
                        node.top - candidate.bottom <= FULLSCREEN_HUD_TITLE_GAP_DP * density &&
                        abs(candidate.left - node.left) <= FULLSCREEN_HUD_ALIGNMENT_DP * density
                }
                .mapNotNull { cleanText(it.text) ?: cleanText(it.contentDescription) }
                .filter { value ->
                    value.length > 8 && !handleRegex.containsMatchIn(value) &&
                        !hasUploaderMetadataProof(value) && !isDisallowedTitleText(value.lowercase())
                }
                .maxByOrNull(String::length)
                ?: return@mapNotNull null
            WatchHeaderMetadata(title, YoutubeStudyV2Repository.normalizeHandle(label))
        }.toList()
        // Compose-based YouTube builds may expose the same drawn HUD string
        // through more than one semantic node. Reject conflicting handles, but
        // accept duplicates that resolve to one uploader identity.
        val distinctHandles = candidates.mapNotNull(WatchHeaderMetadata::handle).distinct()
        if (distinctHandles.size != 1) return null
        return candidates
            .filter { it.handle == distinctHandles.single() }
            .maxByOrNull { it.title?.length ?: 0 }
    }

    /** The first watch-list item is the expandable title/uploader header.
     * Its descendants remain owner metadata even when the drawing surface
     * overlaps them. Later list items are actions, comments or recommendations.
     */
    private fun watchHeaderMetadata(nodes: List<YoutubeV2Node>): WatchHeaderMetadata? {
        val list = nodes.indexOfFirst { it.viewId?.substringAfterLast('/') == "watch_list" }
        if (list < 0) return null
        val header = nodes.indices.firstOrNull { nodes[it].parentIndex == list } ?: return null
        if (!nodes[header].visibleToUser || nodes[header].height <= 0) return null
        val members = nodes.indices.filter { index ->
            var ancestor: Int? = index
            var found = false
            repeat(8) {
                if (ancestor == header) found = true
                ancestor = ancestor?.takeIf(nodes.indices::contains)?.let { nodes[it].parentIndex }
            }
            found && nodes[index].visibleToUser && !isExcludedMetadataNode(nodes, index)
        }
        val handles = members.mapNotNull { index ->
            val node = nodes[index]
            val label = cleanText(node.text ?: node.contentDescription) ?: return@mapNotNull null
            val bare = label.takeIf(handleRegex::matches)
            val handle = if (bare != null && members.any { siblingIndex ->
                if (siblingIndex == index) false else {
                    val sibling = nodes[siblingIndex]
                    val overlap = min(node.bottom, sibling.bottom) - max(node.top, sibling.top)
                    overlap > 0 && overlap.toFloat() / min(node.height, sibling.height).coerceAtLeast(1) >= MIN_VERTICAL_OVERLAP &&
                        sequenceOf(sibling.text, sibling.contentDescription).filterNotNull().any(::hasUploaderMetadataProof)
                }
            }) bare else verifiedUploaderHandle(label)
            handle?.let { index to YoutubeStudyV2Repository.normalizeHandle(it) }
        }
        val handle = handles.map { it.second }.distinct().singleOrNull()
        val handleTop = handles.minOfOrNull { nodes[it.first].top }
        val title = members.asSequence().map(nodes::get)
            .filter { node -> handleTop == null || node.bottom <= handleTop }
            .mapNotNull { cleanText(it.text ?: it.contentDescription) }
            .firstOrNull { value -> value.length > 8 && !handleRegex.containsMatchIn(value) &&
                !hasUploaderMetadataProof(value) && !isDisallowedTitleText(value.lowercase()) }
        // Require a real metadata header, not a scrolled recommendation/action item.
        if (handle == null && title == null) return null
        return WatchHeaderMetadata(title, handle)
    }

    private fun isExcludedMetadataNode(nodes: List<YoutubeV2Node>, index: Int): Boolean {
        var ancestor: Int? = index
        repeat(8) {
            val node = ancestor?.takeIf(nodes.indices::contains)?.let(nodes::get) ?: return false
            if (NON_OWNER_TEXT_IDS.any(node.viewId.orEmpty().lowercase()::contains)) return true
            ancestor = node.parentIndex
        }
        return false
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
                val isTextHolder = textNode.className.orEmpty().let { it.contains("TextView", true) || it.contains("ViewGroup", true) }
                if (!isTextHolder) return@forEach
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
            // A same-row sibling can supply the engagement/time proof that a
            // bare-handle node lacks on its own. Only siblings vertically aligned
            // with this node (same metadata row) count — never the whole band.
            val siblingProof = hasSiblingUploaderMetadataProof(nodes, index, node)
            val valueAndHandle = values.mapNotNull { value ->
                verifiedUploaderHandle(value, semanticOwnerProof || siblingProof)?.let { value to it }
            }.firstOrNull() ?: return@mapNotNull null
            val (rawValue, handle) = valueAndHandle
            val uploaderMetadataProof = hasUploaderMetadataProof(rawValue) || siblingProof
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
     * A bare handle node (e.g. "@akshathsharma") often sits beside, not inside,
     * the text carrying likes/views/ago. Accept proof from a same-parent sibling
     * that vertically overlaps this node — i.e. the same metadata row — so the
     * handle and its proof can live in separate accessibility nodes.
     */
    private fun hasSiblingUploaderMetadataProof(
        nodes: List<YoutubeV2Node>,
        index: Int,
        node: YoutubeV2Node,
    ): Boolean {
        val parent = node.parentIndex ?: return false
        return nodes.indices.any { siblingIndex ->
            if (siblingIndex == index) return@any false
            val sibling = nodes[siblingIndex]
            if (sibling.parentIndex != parent) return@any false
            val overlap = min(node.bottom, sibling.bottom) - max(node.top, sibling.top)
            if (overlap <= 0 || overlap.toFloat() / min(node.height, sibling.height).coerceAtLeast(1) < MIN_VERTICAL_OVERLAP) {
                return@any false
            }
            sequenceOf(sibling.text, sibling.contentDescription).filterNotNull().any(::hasUploaderMetadataProof)
        }
    }

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

    private val METRIC_COUNT_REGEX = Regex("\\b\\d+(?:\\.\\d+)?\\s*(?:k|m|b|lakh|crore)\\b", RegexOption.IGNORE_CASE)

    private fun hasUploaderMetadataProof(value: String): Boolean {
        val lower = value.lowercase()
        return UPLOADER_ENGAGEMENT_MARKERS.any(lower::contains) ||
            UPLOADER_CONTEXT_MARKERS.any(lower::contains) ||
            ((value.contains("·") || value.contains("•")) && value.any { it.isDigit() } &&
                (lower.contains("k") || lower.contains("m") || lower.contains("b") || lower.contains("lakh") || lower.contains("crore") ||
                    lower.contains("view") || lower.contains("like") || lower.contains("ago") || lower.contains("दृश्य") || lower.contains("पहले"))) ||
            METRIC_COUNT_REGEX.containsMatchIn(value)
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

    internal fun isPlausibleOwnerLabel(value: String): Boolean {
        if (value.length !in 2..120) return false
        val lower = value.lowercase()
        if (lower in setOf("exit full screen", "enter full screen", "full screen", "fullscreen", "play", "pause", "next video", "previous video", "close player")) return false
        return INVALID_OWNER_WORDS.none(lower::contains)
    }

    private fun cleanText(value: String?): String? = value?.trim()?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotBlank() }

    internal fun cleanOwnerText(value: String?): String? {
        val cleaned = cleanText(value) ?: return null
        val withoutAction = cleaned.replace(Regex("^go to channel(?:\\s+|$)", RegexOption.IGNORE_CASE), "")
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
    private const val FULLSCREEN_HUD_MAX_LEFT_RATIO = 0.55f
    private const val FULLSCREEN_HUD_MAX_TOP_RATIO = 0.30f
    private const val FULLSCREEN_HUD_TITLE_GAP_DP = 48f
    private const val FULLSCREEN_HUD_ALIGNMENT_DP = 72f
    private const val FULLSCREEN_SIDE_PANEL_LEFT_RATIO = 0.52f
    private val INVALID_OWNER_WORDS = listOf(
        "views", "likes", "comments", "subscribe", "subscribed", "share", "download",
        "save", "more", "play video", "minutes", "hours", "advertiser", "ad panel",
        "sponsored", "install", "सदस्यता", "टिप्पणियां",
        "home", "shorts", "subscriptions", "library", "you", "explore", "trending",
        "होम", "शॉर्ट्स", "सदस्यताएं", "लाइब्रेरी", "आप", "एक्सप्लोर", "ट्रेंडिंग",
    )
    private val AD_PLAYBACK_MARKERS = listOf(
        "visit advertiser", "skip ad", "stop ad", "ad countdown", "ad badge",
    )
    private val SPONSORED_PANEL_MARKERS = listOf(
        "sponsored", "companion_ad", "companion ad", "ad_panel", "ad panel",
    )
    private val LIVE_CHAT_PANEL_MARKERS = listOf(
        "live chat", "live_chat", "chat panel", "chat_panel",
    )
    private val NON_OWNER_TEXT_IDS = listOf("comment", "recommend", "suggest", "transcript")
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
    private val ACTION_BUTTON_TEXTS = setOf(
        "subscribe", "subscribed", "join", "share", "download", "thanks", "remix", "save", "clip", "more",
        "सदस्यता लें", "सदस्यता ली गई", "शेयर करें", "डाउनलोड करें", "धन्यवाद",
    )
    private val PLAYER_CONTROL_LABELS = setOf(
        "like this video", "dislike this video", "exit full screen", "enter full screen",
        "play video", "pause video", "next video", "previous video", "close player",
    )
    private val DISALLOWED_TITLE_WORDS = setOf(
        "sponsored", "advertiser", "promoted", "विज्ञापन", "install", "open app",
    )
    private fun isActionButtonText(lower: String): Boolean {
        if (ACTION_BUTTON_TEXTS.contains(lower)) return true
        if (lower.startsWith("subscribe to ") || lower.startsWith("सदस्यता लें ")) return true
        return false
    }
    private fun isDisallowedTitleText(lower: String): Boolean {
        if (isActionButtonText(lower)) return true
        if (PLAYER_CONTROL_LABELS.any { lower == it || lower.startsWith("$it ") }) return true
        if (DISALLOWED_TITLE_WORDS.any { lower == it || lower.startsWith("$it ") || lower.endsWith(" $it") }) return true
        return false
    }

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
