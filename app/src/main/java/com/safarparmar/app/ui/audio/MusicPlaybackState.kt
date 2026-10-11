package com.safarparmar.app.ui.audio

/** Prepared media may play only while its current audio-focus request owns focus. */
internal class MusicPlaybackState {
    private var prepared = false
    private var focused = false
    private var mayResume = true
    val canPlay: Boolean get() = prepared && focused && mayResume

    fun prepared() { prepared = true }
    fun gainedFocus() { if (mayResume) focused = true }
    fun lostFocus(permanent: Boolean) {
        focused = false
        if (permanent) mayResume = false
    }
    fun released() {
        prepared = false
        focused = false
        mayResume = false
    }
}
