package com.safarparmar.app.feature.youtubestudyv2

/**
 * A verified productive video keeps playing while YouTube merely hides its HUD.
 * A different visible title is evidence that playback changed and must be checked.
 */
internal fun canRetainFullscreenPermission(
    verifiedTitle: String?,
    currentTitle: String?,
    classification: YoutubeChannelClassification?,
): Boolean = classification == YoutubeChannelClassification.PRODUCTIVE &&
    !verifiedTitle.isNullOrBlank() && (currentTitle.isNullOrBlank() || verifiedTitle == currentTitle)

/** Controls that actually replace fullscreen playback, rather than toggling the HUD. */
internal fun isFullscreenPlaybackChangeControl(viewId: String?, label: String?): Boolean {
    val id = viewId.orEmpty().lowercase()
    val normalizedLabel = label.orEmpty().trim().lowercase()
    return id.contains("next_video") || id.contains("next_button") ||
        id.contains("player_control_next") ||
        normalizedLabel == "next video" || normalizedLabel.startsWith("play next video") ||
        normalizedLabel == "अगला वीडियो"
}

/** A transport Play/Pause click controls the current video; it is not a card selection. */
internal fun isCurrentVideoTransportControl(viewId: String?, label: String?): Boolean {
    val id = viewId.orEmpty().lowercase()
    val normalizedLabel = label.orEmpty().trim().lowercase()
    return id.contains("player_control_play_pause") || id.contains("play_pause_replay") ||
        normalizedLabel in setOf("play video", "pause video", "वीडियो चलाएं", "वीडियो चलाएँ", "वीडियो रोकें")
}

internal fun isVideoCardAccessibilityLabel(value: CharSequence?): Boolean {
    val label = value?.toString()?.trim()?.lowercase().orEmpty()
    return (label.contains("play video") && label != "play video") ||
        (label.contains("वीडियो चलाएं") && label != "वीडियो चलाएं") ||
        (label.contains("वीडियो चलाएँ") && label != "वीडियो चलाएँ")
}
