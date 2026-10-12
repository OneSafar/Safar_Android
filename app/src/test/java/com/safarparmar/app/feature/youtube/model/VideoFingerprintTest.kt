package com.safarparmar.app.feature.youtube.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFingerprintTest {

    @Test
    fun `key prioritizes mediaId when present`() {
        val fingerprint = VideoFingerprint(
            mediaId = "dQw4w9WgXcQ",
            normalizedTitle = "never gonna give you up",
            channelCanonicalKey = "handle:@rickastley",
        )
        assertEquals("media:dQw4w9WgXcQ", fingerprint.key)
        assertTrue(fingerprint.isValid)
    }

    @Test
    fun `key trims mediaId whitespace`() {
        val fingerprint = VideoFingerprint(
            mediaId = "   dQw4w9WgXcQ   ",
            normalizedTitle = "video",
        )
        assertEquals("media:dQw4w9WgXcQ", fingerprint.key)
    }

    @Test
    fun `key formats content key when mediaId is null or blank`() {
        val fingerprint = VideoFingerprint(
            mediaId = null,
            normalizedTitle = "quantum computing explained",
            channelCanonicalKey = "handle:@veritasium",
        )
        assertEquals("content:quantum computing explained|handle:@veritasium", fingerprint.key)
        assertTrue(fingerprint.isValid)
    }

    @Test
    fun `key formats content key with only title when channel key is empty`() {
        val fingerprint = VideoFingerprint(
            mediaId = "",
            normalizedTitle = "autonomous robotics demo",
            channelCanonicalKey = "",
        )
        assertEquals("content:autonomous robotics demo|", fingerprint.key)
        assertTrue(fingerprint.isValid)
    }

    @Test
    fun `key formats content key with only channel key when title is empty`() {
        val fingerprint = VideoFingerprint(
            mediaId = null,
            normalizedTitle = "",
            channelCanonicalKey = "name:khan academy",
        )
        assertEquals("content:|name:khan academy", fingerprint.key)
        assertTrue(fingerprint.isValid)
    }

    @Test
    fun `key is empty and invalid when all components are empty`() {
        val empty = VideoFingerprint()
        assertEquals("", empty.key)
        assertFalse(empty.isValid)

        val whitespaceOnly = VideoFingerprint(mediaId = "  ", normalizedTitle = "", channelCanonicalKey = "")
        assertEquals("", whitespaceOnly.key)
        assertFalse(whitespaceOnly.isValid)
    }

    @Test
    fun `create factory helper normalizes title and links channel identity`() {
        val channel = ChannelIdentity(verifiedHandle = "@Veritasium", displayName = "Veritasium")
        val fingerprint = VideoFingerprint.create(
            mediaId = null,
            title = "  How   Quantum   Computers   Work  ",
            channelIdentity = channel,
        )

        assertEquals("how quantum computers work", fingerprint.normalizedTitle)
        assertEquals("handle:@veritasium", fingerprint.channelCanonicalKey)
        assertEquals("content:how quantum computers work|handle:@veritasium", fingerprint.key)
        assertTrue(fingerprint.isValid)
    }

    @Test
    fun `normalizeTitle collapses spaces and lowercases title`() {
        assertEquals("quantum physics 101", VideoFingerprint.normalizeTitle("  Quantum   Physics   101  "))
        assertEquals("", VideoFingerprint.normalizeTitle(""))
        assertEquals("", VideoFingerprint.normalizeTitle("   \n\t  "))
    }

    @Test
    fun `create and key treat literal string null and undefined as empty`() {
        val channel = ChannelIdentity(verifiedHandle = "@creator", displayName = "Creator")
        val fpNull = VideoFingerprint.create(
            mediaId = "null",
            title = "My Video",
            channelIdentity = channel,
        )
        assertEquals("content:my video|handle:@creator", fpNull.key)

        val fpUndefined = VideoFingerprint.create(
            mediaId = "undefined",
            title = "My Video",
            channelIdentity = channel,
        )
        assertEquals("content:my video|handle:@creator", fpUndefined.key)

        val fpDirectNull = VideoFingerprint(
            mediaId = "null",
            normalizedTitle = "direct title",
            channelCanonicalKey = "handle:@creator",
        )
        assertEquals("content:direct title|handle:@creator", fpDirectNull.key)
    }
}
