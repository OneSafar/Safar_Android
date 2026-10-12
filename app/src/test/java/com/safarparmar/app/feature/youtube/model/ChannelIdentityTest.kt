package com.safarparmar.app.feature.youtube.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelIdentityTest {

    @Test
    fun `canonicalKey uses normalized handle when verifiedHandle is present`() {
        val identity = ChannelIdentity(
            verifiedHandle = "@Veritasium",
            displayName = "Veritasium",
        )
        assertEquals("handle:@veritasium", identity.canonicalKey)
        assertTrue(identity.hasHandle)
        assertTrue(identity.hasDisplayName)
        assertTrue(identity.isValid)
    }

    @Test
    fun `canonicalKey normalizes handle lacking leading at-sign`() {
        val identity = ChannelIdentity(
            verifiedHandle = "veritasium",
            displayName = "Veritasium",
        )
        assertEquals("handle:@veritasium", identity.canonicalKey)
    }

    @Test
    fun `canonicalKey uses normalized display name when verifiedHandle is null`() {
        val identity = ChannelIdentity(
            verifiedHandle = null,
            displayName = "Khan Academy",
        )
        assertEquals("name:khan academy", identity.canonicalKey)
        assertFalse(identity.hasHandle)
        assertTrue(identity.hasDisplayName)
        assertTrue(identity.isValid)
        assertNull(identity.verifiedHandle) // Verifies no fake handle
    }

    @Test
    fun `canonicalKey collapses multiple spaces and trims display name`() {
        val identity = ChannelIdentity(
            verifiedHandle = null,
            displayName = "   Khan    Academy   ",
        )
        assertEquals("name:khan academy", identity.canonicalKey)
    }

    @Test
    fun `canonicalKey falls back to display name when verifiedHandle is blank or whitespace`() {
        val identity = ChannelIdentity(
            verifiedHandle = "   ",
            displayName = "Kurzgesagt",
        )
        assertEquals("name:kurzgesagt", identity.canonicalKey)
    }

    @Test
    fun `canonicalKey falls back to display name when verifiedHandle contains only at-symbol`() {
        val identity = ChannelIdentity(
            verifiedHandle = " @ ",
            displayName = "3Blue1Brown",
        )
        assertEquals("name:3blue1brown", identity.canonicalKey)
    }

    @Test
    fun `canonicalKey is empty when both handle and displayName are blank`() {
        val emptyIdentity = ChannelIdentity(verifiedHandle = null, displayName = "")
        assertEquals("", emptyIdentity.canonicalKey)
        assertFalse(emptyIdentity.isValid)
        assertFalse(emptyIdentity.hasHandle)
        assertFalse(emptyIdentity.hasDisplayName)

        val whitespaceIdentity = ChannelIdentity(verifiedHandle = "   ", displayName = "   ")
        assertEquals("", whitespaceIdentity.canonicalKey)
        assertFalse(whitespaceIdentity.isValid)
    }

    @Test
    fun `normalizeHandle handles edge cases correctly`() {
        assertEquals("@channel", ChannelIdentity.normalizeHandle("channel"))
        assertEquals("@channel", ChannelIdentity.normalizeHandle("@channel"))
        assertEquals("@channel", ChannelIdentity.normalizeHandle("  @Channel  "))
        assertEquals("", ChannelIdentity.normalizeHandle(""))
        assertEquals("", ChannelIdentity.normalizeHandle("   "))
        assertEquals("", ChannelIdentity.normalizeHandle("@"))
        assertEquals("", ChannelIdentity.normalizeHandle("  @  "))
    }

    @Test
    fun `normalizeDisplayName handles edge cases correctly`() {
        assertEquals("channel name", ChannelIdentity.normalizeDisplayName("  Channel   Name  "))
        assertEquals("", ChannelIdentity.normalizeDisplayName(""))
        assertEquals("", ChannelIdentity.normalizeDisplayName("   \t\n  "))
    }

    @Test
    fun `from factory creates valid ChannelIdentity without synthesizing handles`() {
        val withHandle = ChannelIdentity.from("veritasium", "Veritasium")
        assertEquals("@veritasium", withHandle.verifiedHandle)
        assertEquals("Veritasium", withHandle.displayName)
        assertEquals("handle:@veritasium", withHandle.canonicalKey)

        val withoutHandle = ChannelIdentity.from("", "Khan Academy")
        assertNull(withoutHandle.verifiedHandle) // Must be null, NEVER fake @khanacademy
        assertEquals("Khan Academy", withoutHandle.displayName)
        assertEquals("name:khan academy", withoutHandle.canonicalKey)
    }

    @Test
    fun `normalizeHandle rejects handles containing whitespace or invalid characters`() {
        assertEquals("", ChannelIdentity.normalizeHandle("  @channel with spaces  "))
        assertEquals("", ChannelIdentity.normalizeHandle("Khan Academy"))
        assertEquals("", ChannelIdentity.normalizeHandle("@bad/handle"))
        assertEquals("", ChannelIdentity.normalizeHandle("@bad#handle"))
    }

    @Test
    fun `normalizeHandle strips fullwidth at symbol and preserves valid characters`() {
        assertEquals("@veritasium", ChannelIdentity.normalizeHandle("＠veritasium"))
        assertEquals("@user.name-123_test", ChannelIdentity.normalizeHandle("@user.name-123_test"))
    }

    @Test
    fun `hasHandle returns false for invalid or bare at symbols`() {
        val bareAt = ChannelIdentity(verifiedHandle = "@", displayName = "Veritasium")
        assertFalse(bareAt.hasHandle)

        val spacesInHandle = ChannelIdentity(verifiedHandle = "Khan Academy", displayName = "Khan Academy")
        assertFalse(spacesInHandle.hasHandle)

        val validHandle = ChannelIdentity(verifiedHandle = "@veritasium", displayName = "Veritasium")
        assertTrue(validHandle.hasHandle)
    }
}
