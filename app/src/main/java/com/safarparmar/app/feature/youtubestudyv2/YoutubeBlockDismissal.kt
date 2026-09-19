package com.safarparmar.app.feature.youtubestudyv2

/** Floating playback created by dismissal belongs to the block already acknowledged. */
internal class YoutubeBlockDismissal {
    var suppressFloatingSheet: Boolean = false
        private set
    fun onReturnHome() { suppressFloatingSheet = true }
    fun onVideoEntered() { suppressFloatingSheet = false }
}
