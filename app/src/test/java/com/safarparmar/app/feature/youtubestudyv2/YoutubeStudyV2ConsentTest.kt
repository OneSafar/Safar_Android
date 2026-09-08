package com.safarparmar.app.feature.youtubestudyv2

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class YoutubeStudyV2ConsentTest {
    private fun preferences(version: Int, oldConsent: Boolean = true): YoutubeStudyV2Preferences {
        val storage = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns storage
        every { storage.getBoolean(any(), any()) } returns oldConsent
        every { storage.getInt("accessibility_disclosure_version", 0) } returns version
        every { storage.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        return YoutubeStudyV2Preferences(context)
    }

    @Test fun `legacy consent cannot restore or enable collection`() {
        val prefs = preferences(0)
        assertFalse(prefs.enabled.value)
        assertFalse(prefs.isDisclosureAccepted())
        prefs.setEnabled(true)
        assertFalse(prefs.enabled.value)
    }

    @Test fun `current consent restores enabled feature and allows disabling`() {
        val prefs = preferences(2)
        assertTrue(prefs.isDisclosureAccepted())
        assertTrue(prefs.enabled.value)
        prefs.setEnabled(false)
        assertFalse(prefs.enabled.value)
    }

    @Test fun `unrecognized consent version cannot enable collection`() {
        val prefs = preferences(1)
        prefs.setEnabled(true)
        assertFalse(prefs.enabled.value)
    }
}
