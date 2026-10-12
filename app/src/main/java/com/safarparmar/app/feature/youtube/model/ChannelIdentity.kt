package com.safarparmar.app.feature.youtube.model

/**
 * Represents the verified canonical identity of a YouTube channel.
 *
 * Requirements:
 * - [verifiedHandle]: ONLY real '@handle' detected from UI/metadata. Must be null if no verified
 *   handle is present. Strictly bans synthetic or fabricated handles.
 * - [displayName]: Display name of the channel.
 * - [canonicalKey]: Computed deterministically:
 *   - If verifiedHandle is non-blank -> "handle:${normalizeHandle(verifiedHandle)}"
 *   - Else if displayName is non-blank -> "name:${normalizeDisplayName(displayName)}"
 *   - Else -> ""
 */
data class ChannelIdentity(
    val verifiedHandle: String? = null,
    val displayName: String = "",
) {
    val canonicalKey: String
        get() {
            val handle = verifiedHandle?.let { normalizeHandle(it) }?.takeIf { it.isNotEmpty() }
            if (handle != null) {
                return "handle:$handle"
            }
            val name = normalizeDisplayName(displayName).takeIf { it.isNotEmpty() }
            if (name != null) {
                return "name:$name"
            }
            return ""
        }

    val hasHandle: Boolean
        get() = !verifiedHandle.isNullOrBlank() && normalizeHandle(verifiedHandle).isNotEmpty()

    val hasDisplayName: Boolean
        get() = displayName.isNotBlank()

    val isValid: Boolean
        get() = canonicalKey.isNotEmpty()

    companion object {
        val EMPTY = ChannelIdentity()

        /**
         * Normalizes a YouTube handle to ensure it is lowercase and starts with a leading '@'.
         * Strips leading standard '@' and fullwidth '＠'. Rejects handles containing whitespace
         * or characters outside valid YouTube handle characters ([a-zA-Z0-9._-]).
         * Returns empty string if the handle contains no meaningful or valid identifier characters.
         *
         * E.g.: "  @Veritasium  " -> "@veritasium"
         *       "khanacademy"     -> "@khanacademy"
         *       "@"               -> ""
         *       "   "             -> ""
         *       "Khan Academy"    -> ""
         *       "＠veritasium"    -> "@veritasium"
         */
        fun normalizeHandle(handle: String): String {
            val trimmed = handle.trim()
            if (trimmed.isEmpty()) return ""
            if (trimmed.any { it.isWhitespace() }) return ""
            val stripped = trimmed.trimStart('@', '＠', '\uFF20').trim()
            if (stripped.isEmpty()) return ""
            if (stripped.any { it.isWhitespace() }) return ""
            if (!stripped.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '.' || it == '_' || it == '-' }) {
                return ""
            }
            return "@${stripped.lowercase()}"
        }

        /**
         * Normalizes a channel display name by trimming leading/trailing whitespace,
         * collapsing internal consecutive whitespace sequences to a single space,
         * and converting to lowercase.
         *
         * E.g.: "  Khan   Academy  " -> "khan academy"
         */
        fun normalizeDisplayName(displayName: String): String {
            return displayName.trim().replace(Regex("\\s+"), " ").lowercase()
        }

        /**
         * Creates a [ChannelIdentity] from raw inputs, strictly rejecting synthetic fake handles.
         * A handle is only treated as verified if it is non-blank and normalizable.
         */
        fun from(
            handle: String?,
            displayName: String = "",
        ): ChannelIdentity {
            val normHandle = handle?.let { normalizeHandle(it) }?.takeIf { it.isNotEmpty() }
            val cleanDisplayName = displayName.trim()
            return ChannelIdentity(
                verifiedHandle = normHandle,
                displayName = cleanDisplayName,
            )
        }
    }
}
