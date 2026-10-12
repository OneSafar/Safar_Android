package com.safarparmar.app.feature.youtubestudyv2

/** PiP has no reliable owner row: only an identified productive channel or a live break may play. */
internal fun shouldBlockYoutubePip(
    quickUnlockActive: Boolean,
    classification: YoutubeChannelClassification?,
): Boolean = classification != YoutubeChannelClassification.PRODUCTIVE

/** Accessibility overlays must never cover the lock screen, calls, or another app. */
internal fun shouldKeepYoutubeBlockOverlay(
    screenAvailable: Boolean,
    youtubeForeground: Boolean,
): Boolean = screenAvailable && youtubeForeground
