package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.SharedPreferences
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2AccessibilityService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KavachYoutubePolicyTest {
    private val youtube = "com.google.android.youtube"

    @Test fun `connected YouTube Focus owns YouTube while both modes run`() {
        assertTrue(KavachYoutubePolicy.isManagedByYoutubeFocus(youtube, true, true))
    }

    @Test fun `turning Focus off or losing its service restores Kavach ownership`() {
        assertFalse(KavachYoutubePolicy.isManagedByYoutubeFocus(youtube, false, true))
        assertFalse(KavachYoutubePolicy.isManagedByYoutubeFocus(youtube, true, false))
        assertFalse(KavachYoutubePolicy.isManagedByYoutubeFocus(youtube, false, false))
    }

    @Test fun `Focus never exempts other apps from Kavach`() {
        for (pkg in listOf("com.instagram.android", "com.example.blocked", "com.google.android.youtube.music")) {
            assertFalse(KavachYoutubePolicy.isManagedByYoutubeFocus(pkg, true, true))
        }
    }

    @Test fun `runtime reads current enabled and connection state without changing saved app selection`() {
        val context = mockk<Context>()
        val preferences = mockk<SharedPreferences>()
        var enabled = true
        var connected = true
        every { context.getSharedPreferences("youtube_study_v2", Context.MODE_PRIVATE) } returns preferences
        every { preferences.getBoolean("enabled", false) } answers { enabled }
        every { preferences.getInt("accessibility_disclosure_version", 0) } returns 2
        mockkObject(YoutubeStudyV2AccessibilityService.Companion)
        try {
            every { YoutubeStudyV2AccessibilityService.isConnected } answers { connected }
            assertTrue(KavachYoutubePolicy.isManagedByYoutubeFocus(context, youtube))
            enabled = false
            assertFalse(KavachYoutubePolicy.isManagedByYoutubeFocus(context, youtube))
            enabled = true
            connected = false
            assertFalse(KavachYoutubePolicy.isManagedByYoutubeFocus(context, youtube))
            connected = true
            assertTrue(KavachYoutubePolicy.isManagedByYoutubeFocus(context, youtube))
            assertFalse(KavachYoutubePolicy.isManagedByYoutubeFocus(context, "com.example.blocked"))
            verify(exactly = 0) { preferences.edit() }
        } finally {
            unmockkObject(YoutubeStudyV2AccessibilityService.Companion)
        }
    }
}
