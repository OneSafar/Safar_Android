package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class BlockedMediaEnforcerTest {
    private val context = mockk<Context>(relaxed = true)
    private val manager = mockk<MediaSessionManager>()
    private val blockedPlayer = mockk<MediaController>(relaxed = true)
    private val lessonPlayer = mockk<MediaController>(relaxed = true)

    @Before fun setUp() {
        mockkObject(KavachYoutubePolicy, FocusShieldPermissionHelper)
        every { context.packageName } returns "com.safarparmar.app"
        every { KavachYoutubePolicy.isManagedByYoutubeFocus(context, any()) } returns false
        every { FocusShieldPermissionHelper.hasNotificationListenerAccess(context) } returns true
        every { context.getSystemService(MediaSessionManager::class.java) } returns manager
        every { blockedPlayer.packageName } returns "com.example.blocked"
        every { lessonPlayer.packageName } returns "com.google.android.youtube"
        every { manager.getActiveSessions(any()) } returns listOf(blockedPlayer, lessonPlayer)
    }

    @After fun tearDown() {
        unmockkObject(KavachYoutubePolicy, FocusShieldPermissionHelper)
    }

    @Test fun `blocking another app only pauses that apps player`() {
        BlockedMediaEnforcer.stop(context, "com.example.blocked")
        verify(exactly = 1) { blockedPlayer.transportControls.pause() }
        verify(exactly = 0) { lessonPlayer.transportControls.pause() }
    }

    @Test fun `YouTube owned by Focus is never paused by Kavach`() {
        every { KavachYoutubePolicy.isManagedByYoutubeFocus(context, "com.google.android.youtube") } returns true
        BlockedMediaEnforcer.stop(context, "com.google.android.youtube")
        verify(exactly = 0) { manager.getActiveSessions(any()) }
        verify(exactly = 0) { lessonPlayer.transportControls.pause() }
    }

    @Test fun `missing matching session never dispatches global media keys`() {
        every { manager.getActiveSessions(any()) } returns listOf(lessonPlayer)
        BlockedMediaEnforcer.stop(context, "com.example.blocked")
        verify(exactly = 0) { lessonPlayer.transportControls.pause() }
        verify(exactly = 0) { context.getSystemService(Context.AUDIO_SERVICE) }
    }

    @Test fun `without notification access app blocking cannot pause an unrelated lesson`() {
        every { FocusShieldPermissionHelper.hasNotificationListenerAccess(context) } returns false
        BlockedMediaEnforcer.stop(context, "com.example.blocked")
        verify(exactly = 0) { manager.getActiveSessions(any()) }
        verify(exactly = 0) { context.getSystemService(Context.AUDIO_SERVICE) }
    }
}
