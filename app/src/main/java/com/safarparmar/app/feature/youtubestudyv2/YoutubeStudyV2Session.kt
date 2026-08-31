package com.safarparmar.app.feature.youtubestudyv2

/** Pure tap-gated watch session. Home previews and autoplay never create one. */
class YoutubeStudyV2Session {
    enum class State { BROWSING, VIDEO_TAPPED, MONITORING }

    var state: State = State.BROWSING
        private set
    private var tappedAtMs = 0L
    private var monitoredKey: String? = null

    fun onVideoTap(nowMs: Long) {
        state = State.VIDEO_TAPPED
        tappedAtMs = nowMs
        monitoredKey = null
    }

    fun onBrowsing() {
        state = State.BROWSING
        tappedAtMs = 0L
        monitoredKey = null
    }

    fun acceptStable(observation: YoutubeV2Observation, nowMs: Long): Boolean {
        if (!observation.watchScreenConfirmed) {
            if (state != State.VIDEO_TAPPED || nowMs - tappedAtMs > TAP_MAX_AGE_MS) onBrowsing()
            return false
        }
        if (state == State.VIDEO_TAPPED && nowMs - tappedAtMs > TAP_MAX_AGE_MS) {
            onBrowsing()
            return false
        }
        if (state == State.BROWSING) return false
        // Once a user explicitly enters a watch session, YouTube autoplay/next
        // may change the title and owner without another accessibility click.
        // Keep monitoring that watch session and re-evaluate the new stable key.
        state = State.MONITORING
        monitoredKey = observation.stableKey
        return true
    }

    fun isAlreadyEvaluated(key: String, lastEvaluatedKey: String?): Boolean =
        state == State.MONITORING && key == lastEvaluatedKey

    companion object { const val TAP_MAX_AGE_MS = 10_000L }
}
