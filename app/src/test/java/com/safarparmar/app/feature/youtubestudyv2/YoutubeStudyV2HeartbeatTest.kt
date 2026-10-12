package com.safarparmar.app.feature.youtubestudyv2

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class YoutubeStudyV2HeartbeatTest {
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val storage = mockk<SharedPreferences>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private fun preferences(): YoutubeStudyV2Preferences {
        every { context.getSharedPreferences(any(), any()) } returns storage
        every { storage.edit() } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        return YoutubeStudyV2Preferences(context)
    }

    @Test fun `event storm persists one heartbeat per five seconds`() {
        val preferences = preferences()
        repeat(1_000) { preferences.recordAccessibilityHeartbeat(10_000L + it * 10L) }
        verify(exactly = 2) { editor.putLong(any(), any()) }
    }

    @Test fun `reenabling allows immediate heartbeat after clearing health state`() {
        val preferences = preferences()
        preferences.recordAccessibilityHeartbeat(10_000L)
        preferences.setEnabled(false)
        preferences.setEnabled(true)
        preferences.recordAccessibilityHeartbeat(10_100L)
        verify(exactly = 2) { editor.putLong(any(), any()) }
    }

    @Test fun `clock moving backwards does not suppress new heartbeats`() {
        val preferences = preferences()
        preferences.recordAccessibilityHeartbeat(10_000L)
        preferences.recordAccessibilityHeartbeat(1_000L)
        verify(exactly = 2) { editor.putLong(any(), any()) }
    }
}
