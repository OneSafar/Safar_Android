package com.safarparmar.app.feature.youtubestudyv2

import java.net.URLDecoder
import java.text.Normalizer

/**
 * Turns whatever a user types or pastes into the "add channel" box into
 * something the runtime matcher can compare:
 * - `https://youtube.com/@Foo/videos?si=x`, `m.youtube.com/@Foo` and `@Foo/featured` give `@Foo`
 * - plain names and handles are cleaned (NFKC, invisible characters, spacing)
 * - links that cannot identify a channel's handle are rejected with a clear message
 */
internal object YoutubeChannelReference {
    private val INVISIBLE = Regex("[\\u00AD\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]")
    private val YOUTUBE_LINK = Regex(
        "^(?:https?://)?(?:(?:www|m|music)\\.)?(?:youtube\\.com|youtu\\.be)(?:[/?#]|$)",
        RegexOption.IGNORE_CASE,
    )
    private val LINK_HANDLE = Regex("^(?:https?://)?(?:(?:www|m|music)\\.)?youtube\\.com/(@[^/?#\\s]+)", RegexOption.IGNORE_CASE)
    private val LINK_CHANNEL_ID = Regex("youtube\\.com/channel/", RegexOption.IGNORE_CASE)
    private val LINK_LEGACY_NAME = Regex("youtube\\.com/(?:c|user)/", RegexOption.IGNORE_CASE)
    private val VIDEO_LINK = Regex("youtu\\.be/|/watch|/shorts/|/live/|/embed/", RegexOption.IGNORE_CASE)
    private val GENERIC_LINK = Regex("^(?:https?://|www\\.)", RegexOption.IGNORE_CASE)

    const val CHANNEL_ID_LINK_MESSAGE =
        "Channel ID links can't be matched. Open the channel in YouTube and enter the @handle shown under its name."
    const val LEGACY_LINK_MESSAGE =
        "Old /c/ or /user/ links can't be matched. Enter the channel's @handle or its name exactly as YouTube shows it."
    const val VIDEO_LINK_MESSAGE =
        "That is a video link. Enter the channel's @handle (shown under the channel name) instead."
    const val NO_HANDLE_LINK_MESSAGE =
        "This link has no channel @handle. Paste a link like youtube.com/@channel, or type the @handle."
    const val OTHER_LINK_MESSAGE = "Enter a YouTube channel's @handle or name, not a link to another site."

    /** NFKC plus removal of invisible formatting characters, then whitespace collapse. */
    fun clean(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace(INVISIBLE, "")
            .trim()
            .replace(Regex("\\s+"), " ")

    /**
     * Returns `@handle` for handle links and handle paths, otherwise the cleaned text.
     * Throws [IllegalArgumentException] with a user-facing message for links
     * that cannot be turned into a handle.
     */
    fun canonical(input: String): String {
        val cleaned = clean(input)
        if (YOUTUBE_LINK.containsMatchIn(cleaned)) {
            LINK_HANDLE.find(cleaned)?.groupValues?.get(1)?.let { return decode(it) }
            require(!LINK_CHANNEL_ID.containsMatchIn(cleaned)) { CHANNEL_ID_LINK_MESSAGE }
            require(!LINK_LEGACY_NAME.containsMatchIn(cleaned)) { LEGACY_LINK_MESSAGE }
            require(!VIDEO_LINK.containsMatchIn(cleaned)) { VIDEO_LINK_MESSAGE }
            throw IllegalArgumentException(NO_HANDLE_LINK_MESSAGE)
        }
        require(!GENERIC_LINK.containsMatchIn(cleaned)) { OTHER_LINK_MESSAGE }
        if (cleaned.startsWith('@')) {
            // "@Foo/videos" or "@Foo?si=..." copied from an address bar.
            val handle = cleaned.substringBefore('/').substringBefore('?').substringBefore('#')
            if (!handle.contains(' ')) return decode(handle)
        }
        return cleaned
    }

    private fun decode(handle: String): String =
        runCatching { URLDecoder.decode(handle, "UTF-8") }.getOrDefault(handle).let(::clean)
}
