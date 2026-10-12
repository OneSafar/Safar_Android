package com.safarparmar.app.feature.youtube.model

/**
 * Unique fingerprint of a video or content item playing in YouTube.
 * Used for deduplicating events across AccessibilityService and MediaSessionManager
 * and for deterministic state transitions.
 *
 * Requirements:
 * - [mediaId]: Media ID from MediaSession or player if present (highest priority).
 * - [normalizedTitle]: Normalized video title.
 * - [channelCanonicalKey]: Canonical key of the channel identity.
 * - [key]:
 *   - If !mediaId.isNullOrBlank() -> "media:$mediaId"
 *   - Else if normalizedTitle.isNotBlank() || channelCanonicalKey.isNotBlank() ->
 *     "content:$normalizedTitle|$channelCanonicalKey"
 *   - Else ""
 */
data class VideoFingerprint(
    val mediaId: String? = null,
    val normalizedTitle: String = "",
    val channelCanonicalKey: String = "",
) {
    val key: String
        get() {
            val cleanMediaId = mediaId?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals("null", ignoreCase = true) && !it.equals("undefined", ignoreCase = true)
            }
            return when {
                cleanMediaId != null -> "media:$cleanMediaId"
                normalizedTitle.isNotBlank() || channelCanonicalKey.isNotBlank() ->
                    "content:$normalizedTitle|$channelCanonicalKey"
                else -> ""
            }
        }

    val isValid: Boolean
        get() = key.isNotEmpty()

    companion object {
        val EMPTY = VideoFingerprint()

        /**
         * Normalizes video title by trimming whitespace, collapsing multiple spaces,
         * and converting to lowercase.
         */
        fun normalizeTitle(title: String): String {
            return title.trim().replace(Regex("\\s+"), " ").lowercase()
        }

        fun create(
            mediaId: String? = null,
            title: String = "",
            channelIdentity: ChannelIdentity = ChannelIdentity.EMPTY,
        ): VideoFingerprint {
            val cleanMediaId = mediaId?.trim()?.takeIf {
                it.isNotEmpty() && !it.equals("null", ignoreCase = true) && !it.equals("undefined", ignoreCase = true)
            }
            val normTitle = normalizeTitle(title)
            return VideoFingerprint(
                mediaId = cleanMediaId,
                normalizedTitle = normTitle,
                channelCanonicalKey = channelIdentity.canonicalKey,
            )
        }
    }
}
