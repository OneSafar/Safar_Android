package com.safarparmar.app.data.local.entity

/**
 * Classification policy for a YouTube channel.
 * PRODUCTIVE: Content is permitted during focus sessions.
 * DISTRACTING: Content is blocked by YouTubeGuardService overlay.
 */
enum class ChannelPolicy {
    PRODUCTIVE,
    DISTRACTING,
}
