package com.safarparmar.app.feature.youtubestudyv2

/** PiP has no reliable owner row: only an identified productive channel or a live break may play. */
internal fun shouldBlockYoutubePip(
    quickUnlockActive: Boolean,
    classification: YoutubeChannelClassification?,
): Boolean = !quickUnlockActive && classification != YoutubeChannelClassification.PRODUCTIVE
