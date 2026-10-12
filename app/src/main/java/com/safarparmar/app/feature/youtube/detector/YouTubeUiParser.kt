package com.safarparmar.app.feature.youtube.detector

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.safarparmar.app.feature.youtube.model.CanonicalYouTubeContent
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.DetectionConfidence
import com.safarparmar.app.feature.youtube.model.VideoFingerprint
import com.safarparmar.app.feature.youtube.policy.VideoIdentity

/**
 * Screen type classification based on structural accessibility subtree analysis.
 */
enum class ScreenType {
    WATCH,
    SHORTS,
    CHANNEL_PAGE,
    FEED,
    SEARCH,
    UNKNOWN,
}

/**
 * Snapshot of structural UI parse result.
 */
data class ParsedUiSnapshot(
    val screenType: ScreenType,
    val title: String = "",
    val channelName: String = "",
    val channelHandle: String = "",
    val channelIdentity: ChannelIdentity = ChannelIdentity.EMPTY,
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
    val isChannelPage: Boolean = false,
    val hasActivePlayer: Boolean = false,
    val confidence: DetectionConfidence = DetectionConfidence.UNKNOWN,
) {
    fun toVideoIdentity(): VideoIdentity = VideoIdentity(
        videoId = null,
        title = title,
        channelIdentity = channelIdentity,
        isShorts = isShorts,
        isAd = isAd,
        isNewVideoSwitch = false,
        isChannelPage = isChannelPage,
        explicitFingerprint = toCanonicalContent().fingerprint,
    )

    fun toCanonicalContent(): CanonicalYouTubeContent = CanonicalYouTubeContent(
        fingerprint = VideoFingerprint.create(
            mediaId = null,
            title = title,
            channelIdentity = channelIdentity,
        ),
        channelIdentity = channelIdentity,
        title = title,
        isShorts = isShorts,
        isAd = isAd,
        isChannelPage = isChannelPage,
        confidence = confidence,
    )

    fun toLegacySnapshot(): YouTubeSnapshot = YouTubeSnapshot(
        title = title,
        channelName = channelIdentity.displayName.ifEmpty { channelName },
        channelHandle = channelIdentity.verifiedHandle.orEmpty().ifEmpty { channelHandle },
        isShorts = isShorts,
        isAd = isAd,
        confidence = when (confidence) {
            DetectionConfidence.HIGH -> com.safarparmar.app.feature.youtube.detector.DetectionConfidence.HIGH
            DetectionConfidence.MEDIUM -> com.safarparmar.app.feature.youtube.detector.DetectionConfidence.MEDIUM
            DetectionConfidence.LOW -> com.safarparmar.app.feature.youtube.detector.DetectionConfidence.LOW
            DetectionConfidence.UNKNOWN -> com.safarparmar.app.feature.youtube.detector.DetectionConfidence.UNKNOWN
        },
    )

    companion object {
        val EMPTY = ParsedUiSnapshot(screenType = ScreenType.UNKNOWN)
    }
}

/**
 * Structural Subtree Parser for YouTube Accessibility UI hierarchies.
 *
 * Enforces strict subtree isolation:
 * - Watch metadata (title, creator name, verified @handle) is extracted exclusively
 *   from the active watch layout.
 * - Recommendations / suggestions containers (`related_videos`, `compact_video_renderer`, `watch_list`)
 *   and comments containers (`comments_entry_point_layout`, `comment_thread`) are pruned
 *   at subtree boundaries, preventing comment @mentions and suggestion titles from polluting
 *   active video identity.
 * - Genuine handles are extracted ONLY when real '@' is present; synthetic fake handles
 *   (such as `@${channelName}`) are strictly eliminated.
 * - In-stream ads are detected strictly inside the active [PlayerSubtree], avoiding feed ads.
 */
object YouTubeUiParser {

    private const val TAG = "YouTubeUiParser"
    private const val YT_PKG = "com.google.android.youtube:id/"

    // Structural container IDs
    val WATCH_METADATA_CONTAINERS = setOf(
        "${YT_PKG}watch_metadata",
        "${YT_PKG}video_owner",
        "${YT_PKG}owner_row",
        "${YT_PKG}watch_header",
        "${YT_PKG}metadata_layout",
        "${YT_PKG}watch_metadata_container",
        "${YT_PKG}watch_panel",
        "${YT_PKG}video_metadata",
        "${YT_PKG}channel_name_and_sub_count",
    )

    val PLAYER_CONTAINERS = setOf(
        "${YT_PKG}watch_player",
        "${YT_PKG}movie_player",
        "${YT_PKG}fullscreen_player",
        "${YT_PKG}player_fragment_container",
        "${YT_PKG}inline_player",
        "${YT_PKG}player_view",
        "${YT_PKG}player_control_layer",
    )

    val SHORTS_CONTAINERS = setOf(
        "${YT_PKG}reel_watch_player",
        "${YT_PKG}reel_watch_player_layout",
        "${YT_PKG}reel_player_page",
        "${YT_PKG}reel_scrim_shorts",
        "${YT_PKG}shorts_container",
        "${YT_PKG}reel_recycler",
    )

    val CHANNEL_HEADER_CONTAINERS = setOf(
        "${YT_PKG}channel_header",
        "${YT_PKG}channel_page_header",
        "${YT_PKG}c3_channel_header",
        "${YT_PKG}channel_profile_header",
        "${YT_PKG}channel_header_container",
        "${YT_PKG}channel_banner",
        "${YT_PKG}profile_header",
        "${YT_PKG}channel_tool_bar",
        "${YT_PKG}channel_title_container",
    )

    val EXCLUDED_SUBTREES = setOf(
        "${YT_PKG}watch_list",
        "${YT_PKG}related_videos",
        "${YT_PKG}results_list",
        "${YT_PKG}compact_video_renderer",
        "${YT_PKG}watch_next",
        "${YT_PKG}suggested_videos",
        "${YT_PKG}comments_entry_point_layout",
        "${YT_PKG}comment_thread",
        "${YT_PKG}comment_item",
        "${YT_PKG}comments_list",
        "${YT_PKG}comments_container",
        "${YT_PKG}comment_replies",
    )

    val SEARCH_CONTAINERS = setOf(
        "${YT_PKG}search_results",
        "${YT_PKG}search_recycler",
        "${YT_PKG}search_box",
        "${YT_PKG}search_view",
        "${YT_PKG}search_results_container",
    )

    val FEED_CONTAINERS = setOf(
        "${YT_PKG}feed_recycler",
        "${YT_PKG}home_feed",
        "${YT_PKG}pane_fragment_container",
        "${YT_PKG}feed_filter_bar",
    )

    // Title IDs
    val TITLE_IDS = setOf(
        "${YT_PKG}title",
        "${YT_PKG}video_title",
        "${YT_PKG}watch_video_title",
        "${YT_PKG}reel_watch_player_title",
        "${YT_PKG}player_view_title",
        "${YT_PKG}playlist_name",
    )

    // Channel name IDs
    val CHANNEL_NAME_IDS = setOf(
        "${YT_PKG}channel_name",
        "${YT_PKG}channel_title",
        "${YT_PKG}uploader_name",
        "${YT_PKG}author",
        "${YT_PKG}creator_name",
        "${YT_PKG}reel_channel_name",
        "${YT_PKG}channel_button",
    )

    // In-stream Ad IDs (strictly inside player)
    val IN_STREAM_AD_IDS = setOf(
        "${YT_PKG}skip_ad_button",
        "${YT_PKG}ad_skip_button",
        "${YT_PKG}skip_next_button",
        "${YT_PKG}ad_badge",
        "${YT_PKG}ad_text_badge",
        "${YT_PKG}ad_countdown",
    )

    private val SUBSCRIBE_TO_REGEX = Regex("""^Subscribe to (.+?)\.?$""", RegexOption.IGNORE_CASE)
    private val SUBSCRIBERS_REGEX = Regex("""^(.+?)\s+\d+([.,]\d+)?\s*[KMBkmb]?\s+subscribers""", RegexOption.IGNORE_CASE)

    /**
     * Parses the accessibility root node into a structured [ParsedUiSnapshot].
     */
    fun parse(root: AccessibilityNodeInfo?): ParsedUiSnapshot {
        if (root == null) return ParsedUiSnapshot.EMPTY

        // 1. Identify Shorts structurally
        val shortsSubtree = extractShortsSubtree(root)
        if (shortsSubtree != null) {
            val title = extractTitleFromSubtree(shortsSubtree)
            val channelIdentity = extractChannelIdentityFromSubtree(shortsSubtree)
            return ParsedUiSnapshot(
                screenType = ScreenType.SHORTS,
                title = title,
                channelName = channelIdentity.displayName,
                channelHandle = channelIdentity.verifiedHandle.orEmpty(),
                channelIdentity = channelIdentity,
                isShorts = true,
                isAd = false,
                confidence = DetectionConfidence.HIGH,
            )
        }

        // 2. Active player and in-stream ad detection
        val playerSubtree = extractPlayerSubtree(root)
        val hasActivePlayer = playerSubtree != null
        val isAd = if (playerSubtree != null) detectInStreamAd(playerSubtree) else false

        // 3. Channel profile page header (checked when no active player)
        val channelHeaderSubtree = extractChannelHeaderSubtree(root)
        if (channelHeaderSubtree != null && !hasActivePlayer) {
            val channelIdentity = extractChannelIdentityFromSubtree(channelHeaderSubtree)
            val title = extractTitleFromSubtree(channelHeaderSubtree).ifBlank { channelIdentity.displayName }
            return ParsedUiSnapshot(
                screenType = ScreenType.CHANNEL_PAGE,
                title = title,
                channelName = channelIdentity.displayName,
                channelHandle = channelIdentity.verifiedHandle.orEmpty(),
                channelIdentity = channelIdentity,
                isShorts = false,
                isAd = false,
                isChannelPage = true,
                hasActivePlayer = false,
                confidence = if (channelIdentity.isValid) DetectionConfidence.HIGH else DetectionConfidence.MEDIUM,
            )
        }

        // 4. Watch metadata subtree
        val watchSubtree = extractWatchSubtree(root)
        if (watchSubtree != null) {
            val title = extractTitleFromSubtree(watchSubtree)
            val channelIdentity = extractChannelIdentityFromSubtree(watchSubtree)
            val hasMetadata = title.isNotBlank() || channelIdentity.isValid

            if (hasMetadata || hasActivePlayer) {
                val confidence = when {
                    channelIdentity.hasHandle && title.isNotBlank() -> DetectionConfidence.HIGH
                    channelIdentity.isValid -> DetectionConfidence.MEDIUM
                    title.isNotBlank() -> DetectionConfidence.LOW
                    else -> DetectionConfidence.UNKNOWN
                }
                return ParsedUiSnapshot(
                    screenType = ScreenType.WATCH,
                    title = title,
                    channelName = channelIdentity.displayName,
                    channelHandle = channelIdentity.verifiedHandle.orEmpty(),
                    channelIdentity = channelIdentity,
                    isShorts = false,
                    isAd = isAd,
                    hasActivePlayer = hasActivePlayer,
                    confidence = confidence,
                )
            }
        }

        // 5. Search screen
        if (findSubtree(root, SEARCH_CONTAINERS) != null && !hasActivePlayer) {
            return ParsedUiSnapshot(
                screenType = ScreenType.SEARCH,
                confidence = DetectionConfidence.LOW,
            )
        }

        // 6. Active player without metadata (e.g. fullscreen)
        if (hasActivePlayer) {
            return ParsedUiSnapshot(
                screenType = ScreenType.WATCH,
                isShorts = false,
                isAd = isAd,
                hasActivePlayer = true,
                confidence = DetectionConfidence.LOW,
            )
        }

        // 7. Feed screen
        if (findSubtree(root, FEED_CONTAINERS) != null) {
            return ParsedUiSnapshot(
                screenType = ScreenType.FEED,
                confidence = DetectionConfidence.LOW,
            )
        }

        return ParsedUiSnapshot(
            screenType = ScreenType.UNKNOWN,
            confidence = DetectionConfidence.UNKNOWN,
        )
    }

    /**
     * Determines screen type by structural inspection.
     */
    fun determineScreenType(root: AccessibilityNodeInfo): ScreenType {
        if (extractShortsSubtree(root) != null) return ScreenType.SHORTS
        val player = extractPlayerSubtree(root)
        val channelHeader = extractChannelHeaderSubtree(root)
        if (channelHeader != null && player == null) return ScreenType.CHANNEL_PAGE
        val watch = extractWatchSubtree(root)
        if (watch != null || player != null) return ScreenType.WATCH
        if (findSubtree(root, SEARCH_CONTAINERS) != null) return ScreenType.SEARCH
        if (findSubtree(root, FEED_CONTAINERS) != null) return ScreenType.FEED
        return ScreenType.UNKNOWN
    }

    /**
     * Locates Watch metadata subtree, strictly excluding suggestions and comments containers.
     */
    fun extractWatchSubtree(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val metadataContainer = findSubtree(root, WATCH_METADATA_CONTAINERS)
        if (metadataContainer != null) return metadataContainer

        // If it's a channel header without a player, it is not watch metadata
        if (extractChannelHeaderSubtree(root) != null && extractPlayerSubtree(root) == null) {
            return null
        }

        if (hasWatchMetadataSignals(root)) {
            return root
        }

        return null
    }

    /**
     * Locates active video player container.
     */
    fun extractPlayerSubtree(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findSubtree(root, PLAYER_CONTAINERS)
    }

    /**
     * Locates Shorts reel container or detects Shorts reel signals.
     */
    fun extractShortsSubtree(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val container = findSubtree(root, SHORTS_CONTAINERS)
        if (container != null) return container

        if (hasShortsSignals(root)) return root

        return null
    }

    /**
     * Locates Channel profile header container.
     */
    fun extractChannelHeaderSubtree(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return findSubtree(root, CHANNEL_HEADER_CONTAINERS)
    }

    /**
     * Extracts video or channel title from a subtree, strictly ignoring pruned subtrees.
     */
    fun extractTitleFromSubtree(node: AccessibilityNodeInfo): String {
        val titleNode = findNodeMatching(node) { current ->
            matchesAny(current.viewIdResourceName, TITLE_IDS) && !current.text.isNullOrBlank()
        }
        if (titleNode != null) {
            val t = titleNode.text?.toString()?.trim()
            if (!t.isNullOrBlank()) return t
        }

        val descNode = findNodeMatching(node) { current ->
            matchesAny(current.viewIdResourceName, TITLE_IDS) && !current.contentDescription.isNullOrBlank()
        }
        if (descNode != null) {
            val d = descNode.contentDescription?.toString()?.trim()
            if (!d.isNullOrBlank()) return d
        }

        return extractFallbackTitle(node)
    }

    /**
     * Extracts channel identity from a subtree.
     * Guaranteed never to synthesize a fake handle.
     */
    fun extractChannelIdentityFromSubtree(node: AccessibilityNodeInfo): ChannelIdentity {
        var rawChannelName = ""
        var rawHandle: String? = null

        collectChannelMetadata(node) { nameCandidate, handleCandidate ->
            if (rawChannelName.isBlank() && nameCandidate.isNotBlank()) {
                rawChannelName = nameCandidate
            }
            if (rawHandle == null && handleCandidate != null) {
                rawHandle = handleCandidate
            }
        }

        val verifiedHandle = if (rawHandle != null) {
            ChannelIdentity.normalizeHandle(rawHandle).takeIf { it.isNotEmpty() }
        } else if (rawChannelName.startsWith("@") || rawChannelName.startsWith("＠")) {
            ChannelIdentity.normalizeHandle(rawChannelName).takeIf { it.isNotEmpty() }
        } else {
            null // Strict requirement: NO fake handle synthesis
        }

        val displayName = if (rawChannelName.startsWith("@") && verifiedHandle != null) {
            rawChannelName.trim()
        } else {
            rawChannelName.trim()
        }

        return ChannelIdentity(
            verifiedHandle = verifiedHandle,
            displayName = displayName,
        )
    }

    // ---------------------------------------------------------------------------
    // Internal & Private Helpers
    // ---------------------------------------------------------------------------

    private fun findSubtree(
        node: AccessibilityNodeInfo,
        targetIds: Set<String>,
        excludeIds: Set<String> = EXCLUDED_SUBTREES,
    ): AccessibilityNodeInfo? {
        if (isExcluded(node, excludeIds)) return null
        if (matchesAny(node.viewIdResourceName, targetIds)) return node

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            val result = findSubtree(child, targetIds, excludeIds)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeMatching(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityNodeInfo? {
        if (isExcluded(node)) return null
        if (predicate(node)) return node

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            val match = findNodeMatching(child, predicate)
            if (match != null) return match
        }
        return null
    }

    private fun collectChannelMetadata(
        node: AccessibilityNodeInfo,
        onFound: (channelName: String, handle: String?) -> Unit,
    ) {
        if (isExcluded(node)) return

        val id = node.viewIdResourceName
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()

        var nameFound = ""
        var handleFound: String? = null

        // Genuine @handle detection
        if (text.startsWith("@") || text.startsWith("＠")) {
            val norm = ChannelIdentity.normalizeHandle(text)
            if (norm.isNotEmpty()) {
                handleFound = norm
            }
        } else if (desc.startsWith("@") || desc.startsWith("＠")) {
            val norm = ChannelIdentity.normalizeHandle(desc)
            if (norm.isNotEmpty()) {
                handleFound = norm
            }
        }

        // Channel name ID detection
        if (matchesAny(id, CHANNEL_NAME_IDS)) {
            if (text.isNotBlank() && !text.startsWith("@")) {
                nameFound = text
            } else if (desc.isNotBlank() && !desc.startsWith("@")) {
                nameFound = desc
            }
        }

        // Description patterns: "Subscribe to <Channel>"
        if (nameFound.isBlank() && desc.isNotBlank()) {
            val subMatch = SUBSCRIBE_TO_REGEX.matchEntire(desc)
            if (subMatch != null) {
                val extracted = subMatch.groupValues[1].trim().removeSuffix(".")
                if (isValidChannelName(extracted)) {
                    nameFound = extracted
                }
            }
        }

        // Description patterns: "<Channel> 1.2M subscribers"
        if (nameFound.isBlank() && desc.isNotBlank()) {
            val subsCountMatch = SUBSCRIBERS_REGEX.find(desc)
            if (subsCountMatch != null) {
                val extracted = subsCountMatch.groupValues[1].trim()
                if (isValidChannelName(extracted)) {
                    nameFound = extracted
                }
            }
        }

        if (nameFound.isNotBlank() || handleFound != null) {
            onFound(nameFound, handleFound)
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            collectChannelMetadata(child, onFound)
        }
    }

    private fun detectInStreamAd(playerSubtree: AccessibilityNodeInfo): Boolean {
        val id = playerSubtree.viewIdResourceName
        if (matchesAny(id, IN_STREAM_AD_IDS)) return true

        val text = playerSubtree.text?.toString()?.trim() ?: ""
        if (text.startsWith("Ad ·") || text.startsWith("Ad 1 of") || text.startsWith("Ad 2 of") ||
            text.equals("Skip Ad", ignoreCase = true) || text.equals("Skip Ads", ignoreCase = true)
        ) {
            return true
        }

        for (i in 0 until playerSubtree.childCount) {
            val child = try { playerSubtree.getChild(i) } catch (_: Exception) { null } ?: continue
            if (detectInStreamAd(child)) return true
        }
        return false
    }

    private fun extractFallbackTitle(node: AccessibilityNodeInfo): String {
        val candidates = mutableListOf<String>()
        collectFallbackTitleCandidates(node, candidates)
        return candidates.firstOrNull() ?: ""
    }

    private fun collectFallbackTitleCandidates(node: AccessibilityNodeInfo, out: MutableList<String>) {
        if (isExcluded(node)) return

        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank() && text.length in 5..200 &&
            !text.startsWith("@") && !text.contains("subscribers", ignoreCase = true)
        ) {
            val lower = text.lowercase()
            val isUtility = lower in setOf("shorts", "home", "subscriptions", "you", "library", "search", "explore", "remix")
            if (!isUtility) {
                out.add(text)
            }
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            collectFallbackTitleCandidates(child, out)
        }
    }

    private fun hasWatchMetadataSignals(node: AccessibilityNodeInfo): Boolean {
        if (isExcluded(node)) return false
        val id = node.viewIdResourceName
        if (matchesAny(id, TITLE_IDS) || matchesAny(id, CHANNEL_NAME_IDS)) return true

        val desc = node.contentDescription?.toString()?.trim() ?: ""
        if (SUBSCRIBE_TO_REGEX.matches(desc) || SUBSCRIBERS_REGEX.containsMatchIn(desc)) return true

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            if (hasWatchMetadataSignals(child)) return true
        }
        return false
    }

    private fun hasShortsSignals(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val id = node.viewIdResourceName ?: ""

        if (desc.equals("Remix", ignoreCase = true) || text.equals("Remix", ignoreCase = true)) {
            return true
        }
        if (node.isSelected && (desc.equals("Shorts", ignoreCase = true) || text.equals("Shorts", ignoreCase = true))) {
            return true
        }
        if (id.contains("reel_scrim_shorts", ignoreCase = true) ||
            id.contains("reel_player_page", ignoreCase = true) ||
            id.contains("reel_watch_player", ignoreCase = true)
        ) {
            return true
        }
        if (desc.contains("Dislike this short", ignoreCase = true) ||
            desc.contains("Sound used in this short", ignoreCase = true)
        ) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
            if (hasShortsSignals(child)) return true
        }
        return false
    }

    private fun isExcluded(
        node: AccessibilityNodeInfo,
        excludeIds: Set<String> = EXCLUDED_SUBTREES,
    ): Boolean {
        val id = node.viewIdResourceName ?: return false
        return matchesAny(id, excludeIds)
    }

    private fun matchesAny(nodeId: String?, targetIds: Set<String>): Boolean {
        if (nodeId == null) return false
        if (nodeId in targetIds) return true
        val bareId = nodeId.substringAfter(":id/")
        return targetIds.any { it.substringAfter(":id/") == bareId }
    }

    private fun isValidChannelName(name: String): Boolean {
        if (name.isBlank() || name.length !in 2..50) return false
        val lower = name.lowercase()
        if (lower.contains("favorite") || lower.contains("channel") || lower.contains("notified")) {
            return false
        }
        return true
    }
}
