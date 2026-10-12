package com.safarparmar.app.feature.youtube.detector

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Parses an AccessibilityNodeInfo tree from the YouTube app into a clean [YouTubeSnapshot].
 *
 * @deprecated Use [YouTubeUiParser] directly instead.
 */
@Deprecated(
    message = "Use YouTubeUiParser directly instead. YouTubeSnapshotParser is maintained for legacy compatibility.",
    replaceWith = ReplaceWith("YouTubeUiParser.parse(root).toLegacySnapshot()"),
)
object YouTubeSnapshotParser {

    /**
     * Parse the root [AccessibilityNodeInfo] into a [YouTubeSnapshot] via [YouTubeUiParser].
     */
    fun parse(root: AccessibilityNodeInfo?): YouTubeSnapshot {
        if (root == null) return YouTubeSnapshot.EMPTY
        return YouTubeUiParser.parse(root).toLegacySnapshot()
    }
}
