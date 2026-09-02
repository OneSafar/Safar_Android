package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeStudyV2ScreenLogicTest {
    private val channels = listOf(
        channel("parmar", "@parmarssc"),
        channel("safar", "@safarparmar"),
        channel("maths", "@mathsclass"),
        channel("news", "@newschannel"),
    )

    @Test
    fun `starter suggestions contain only Parmar SSC and SAFAR PARMAR in that order`() {
        assertEquals(listOf("parmar", "safar"), starterChannels(channels.reversed()).map { it.channelId })
    }

    @Test
    fun `available filters separate productive and distracting channels`() {
        val classifications = mapOf(
            "maths" to YoutubeChannelClassification.PRODUCTIVE,
            "news" to YoutubeChannelClassification.DISTRACTING,
        )

        assertEquals(
            listOf("maths"),
            filterAvailableChannels(channels, classifications, AvailableChannelFilter.PRODUCTIVE).map { it.channelId },
        )
        assertEquals(
            listOf("news"),
            filterAvailableChannels(channels, classifications, AvailableChannelFilter.DISTRACTING).map { it.channelId },
        )
    }

    @Test
    fun `productive filter includes productive starter channels`() {
        val filtered = filterAvailableChannels(
            channels,
            mapOf(
                "parmar" to YoutubeChannelClassification.PRODUCTIVE,
                "safar" to YoutubeChannelClassification.PRODUCTIVE,
            ),
            AvailableChannelFilter.PRODUCTIVE,
        )

        assertEquals(listOf("parmar", "safar"), filtered.map { it.channelId })
    }

    private fun channel(id: String, handle: String) = ResolvedYoutubeChannelDto(
        channelId = id,
        handle = handle,
        displayName = id,
    )
}
