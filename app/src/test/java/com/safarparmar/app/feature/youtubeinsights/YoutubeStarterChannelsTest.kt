package com.safarparmar.app.feature.youtubeinsights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeStarterChannelsTest {
    @Test
    fun `starter list contains the six approved channels with unique normalized keys`() {
        assertEquals(6, YoutubeInsightsRepository.STARTER_CHANNELS.size)
        assertEquals(6, YoutubeInsightsRepository.STARTER_CHANNEL_KEYS.size)
        assertTrue("parmar academy" in YoutubeInsightsRepository.STARTER_CHANNEL_KEYS)
        assertTrue("safar parmar" in YoutubeInsightsRepository.STARTER_CHANNEL_KEYS)
    }

    @Test
    fun `channel notification ids are stable and channel specific`() {
        val first = YoutubeChannelNotifications.notificationId("physics wallah")
        assertEquals(first, YoutubeChannelNotifications.notificationId("physics wallah"))
        assertTrue(first != YoutubeChannelNotifications.notificationId("unacademy"))
    }
}
