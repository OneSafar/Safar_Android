package com.safarparmar.app.feature.youtube.policy

import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import com.safarparmar.app.feature.youtube.model.VideoFingerprint

/**
 * Represents the extracted metadata identity of a video playing in YouTube.
 */
data class VideoIdentity(
    val videoId: String? = null,
    val title: String = "",
    val channelHandle: String = "",
    val channelName: String = "",
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
    val isNewVideoSwitch: Boolean = false,
    val isChannelPage: Boolean = false,
    val explicitFingerprint: VideoFingerprint? = null,
) {
    /**
     * Secondary constructor accepting canonical [ChannelIdentity].
     */
    constructor(
        videoId: String? = null,
        title: String = "",
        channelIdentity: ChannelIdentity,
        isShorts: Boolean = false,
        isAd: Boolean = false,
        isNewVideoSwitch: Boolean = false,
        isChannelPage: Boolean = false,
        explicitFingerprint: VideoFingerprint? = null,
    ) : this(
        videoId = videoId,
        title = title,
        channelHandle = channelIdentity.verifiedHandle.orEmpty(),
        channelName = channelIdentity.displayName,
        isShorts = isShorts,
        isAd = isAd,
        isNewVideoSwitch = isNewVideoSwitch,
        isChannelPage = isChannelPage,
        explicitFingerprint = explicitFingerprint,
    )

    val channelIdentity: ChannelIdentity
        get() = ChannelIdentity.from(
            handle = channelHandle.takeIf { it.isNotBlank() },
            displayName = channelName,
        )

    val fingerprint: VideoFingerprint
        get() = explicitFingerprint ?: VideoFingerprint.create(
            mediaId = videoId,
            title = title,
            channelIdentity = channelIdentity,
        )

    /**
     * Determines whether another [VideoIdentity] represents the identical playing content.
     * Distinguishes between video playback, ad playback, and shorts.
     */
    fun isSameVideo(other: VideoIdentity?): Boolean {
        if (other == null) return false
        if (isShorts != other.isShorts || isAd != other.isAd || isChannelPage != other.isChannelPage) return false
        if (!videoId.isNullOrBlank() && !other.videoId.isNullOrBlank()) {
            return videoId == other.videoId
        }
        val myKey = fingerprint.key
        val otherKey = other.fingerprint.key
        if (myKey.isNotEmpty() && otherKey.isNotEmpty()) {
            return myKey == otherKey
        }
        return title.equals(other.title, ignoreCase = true) &&
            channelHandle.equals(other.channelHandle, ignoreCase = true)
    }
}
