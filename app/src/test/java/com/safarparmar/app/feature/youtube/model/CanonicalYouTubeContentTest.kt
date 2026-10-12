package com.safarparmar.app.feature.youtube.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalYouTubeContentTest {

    @Test
    fun `default values match expected idle state`() {
        val content = CanonicalYouTubeContent()
        assertEquals(VideoFingerprint.EMPTY, content.fingerprint)
        assertEquals(ChannelIdentity.EMPTY, content.channelIdentity)
        assertEquals("", content.title)
        assertFalse(content.isShorts)
        assertFalse(content.isAd)
        assertFalse(content.isChannelPage)
        assertEquals(DetectionConfidence.UNKNOWN, content.confidence)
    }

    @Test
    fun `custom values are preserved correctly`() {
        val channel = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")
        val fingerprint = VideoFingerprint.create(mediaId = "12345", channelIdentity = channel)

        val content = CanonicalYouTubeContent(
            fingerprint = fingerprint,
            channelIdentity = channel,
            title = "The Science of Thinking",
            isShorts = false,
            isAd = false,
            isChannelPage = false,
            confidence = DetectionConfidence.HIGH,
        )

        assertEquals("media:12345", content.fingerprint.key)
        assertEquals("handle:@veritasium", content.channelIdentity.canonicalKey)
        assertEquals("The Science of Thinking", content.title)
        assertEquals(DetectionConfidence.HIGH, content.confidence)
    }

    @Test
    fun `all DetectionConfidence levels exist and are distinct`() {
        val levels = DetectionConfidence.values()
        assertEquals(4, levels.size)
        assertTrue(levels.contains(DetectionConfidence.HIGH))
        assertTrue(levels.contains(DetectionConfidence.MEDIUM))
        assertTrue(levels.contains(DetectionConfidence.LOW))
        assertTrue(levels.contains(DetectionConfidence.UNKNOWN))
    }
}
