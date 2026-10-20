package com.safarparmar.app.feature.youtubestudyv2

/** Room may deliver an older first snapshot after a channel has just been allowed. */
internal suspend fun currentYoutubePlaybackClassification(
    channelId: String?,
    previous: YoutubeChannelClassification?,
    observedRules: Map<String, YoutubeChannelClassification>,
    reload: suspend (String) -> YoutubeChannelClassification,
): YoutubeChannelClassification? = if (channelId == null) previous else observedRules[channelId] ?: reload(channelId)
