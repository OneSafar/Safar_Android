package com.safarparmar.app.feature.youtubestudyv2

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class YoutubeChannelReferenceTest {
    private fun canonical(value: String) = YoutubeChannelReference.canonical(value)
    private fun rejection(value: String) =
        assertThrows(IllegalArgumentException::class.java) { canonical(value) }.message

    @Test fun `handle links become handles`() {
        assertEquals("@PhysicsWallah", canonical("https://www.youtube.com/@PhysicsWallah"))
        assertEquals("@PhysicsWallah", canonical("https://m.youtube.com/@PhysicsWallah/videos?si=abc"))
        assertEquals("@PhysicsWallah", canonical("youtube.com/@PhysicsWallah"))
        assertEquals("@PhysicsWallah", canonical("  HTTPS://YouTube.com/@PhysicsWallah#about "))
    }

    @Test fun `percent encoded unicode handle is decoded`() {
        assertEquals("@अलख", canonical("https://www.youtube.com/@%E0%A4%85%E0%A4%B2%E0%A4%96"))
    }

    @Test fun `handle with copied path or query is trimmed`() {
        assertEquals("@EZVAL789", canonical("@EZVAL789/featured"))
        assertEquals("@EZVAL789", canonical("@EZVAL789?si=xyz"))
    }

    @Test fun `invisible characters and full-width forms are cleaned`() {
        assertEquals("@EZVAL789", canonical("​@EZVAL789﻿"))
        assertEquals("@ezval789", canonical("＠ｅｚｖａｌ７８９"))
        assertEquals("Physics Wallah", canonical("  Physics    Wallah "))
    }

    @Test fun `plain names are kept`() {
        assertEquals("Physics Wallah - Alakh Pandey", canonical("Physics Wallah - Alakh Pandey"))
        assertEquals("@Physics Wallah", canonical("@Physics Wallah"))
    }

    @Test fun `links that cannot give a handle are rejected with a reason`() {
        assertEquals(YoutubeChannelReference.CHANNEL_ID_LINK_MESSAGE,
            rejection("https://www.youtube.com/channel/UCiGyWN6DEbnj2alu7iapuKQ"))
        assertEquals(YoutubeChannelReference.LEGACY_LINK_MESSAGE, rejection("https://youtube.com/c/PhysicsWallah"))
        assertEquals(YoutubeChannelReference.LEGACY_LINK_MESSAGE, rejection("youtube.com/user/oldname"))
        assertEquals(YoutubeChannelReference.VIDEO_LINK_MESSAGE, rejection("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(YoutubeChannelReference.VIDEO_LINK_MESSAGE, rejection("https://www.youtube.com/watch?v=abc"))
        assertEquals(YoutubeChannelReference.VIDEO_LINK_MESSAGE, rejection("https://youtube.com/shorts/abc"))
        assertEquals(YoutubeChannelReference.NO_HANDLE_LINK_MESSAGE, rejection("https://www.youtube.com/"))
        assertEquals(YoutubeChannelReference.OTHER_LINK_MESSAGE, rejection("https://instagram.com/physicswallah"))
    }

    @Test fun `runtime normalization ignores invisible characters`() {
        assertEquals("@ezval789", YoutubeStudyV2Repository.normalizeHandle("@EZVAL​789"))
        assertEquals("ezval", YoutubeStudyV2Repository.normalizeDisplay("EZ​VAL "))
    }
}
