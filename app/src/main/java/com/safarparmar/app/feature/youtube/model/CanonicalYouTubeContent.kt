package com.safarparmar.app.feature.youtube.model

/**
 * Fused representation of detected YouTube content across Accessibility and MediaSession.
 *
 * @property fingerprint Fingerprint representing this specific video/playback session.
 * @property channelIdentity Canonical channel identity (real handle or display name).
 * @property title Raw or display title of the content.
 * @property isShorts Whether content is detected as a YouTube Short.
 * @property isAd Whether content is currently an advertisement.
 * @property isChannelPage Whether the user is viewing a channel profile/header page.
 * @property confidence Confidence level of the detection.
 */
data class CanonicalYouTubeContent(
    val fingerprint: VideoFingerprint = VideoFingerprint.EMPTY,
    val channelIdentity: ChannelIdentity = ChannelIdentity.EMPTY,
    val title: String = "",
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
    val isChannelPage: Boolean = false,
    val confidence: DetectionConfidence = DetectionConfidence.UNKNOWN,
)
