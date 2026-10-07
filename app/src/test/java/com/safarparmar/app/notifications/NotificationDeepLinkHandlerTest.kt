package com.safarparmar.app.notifications

import android.net.Uri
import com.safarparmar.app.ui.navigation.Routes
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDeepLinkHandlerTest {
    @Test
    fun `live session deep link maps to player route`() {
        val route = NotificationDeepLinkHandler.routeFor("safar://live/session/abc-123")
        assertEquals("live/session/abc-123", route)
    }

    @Test
    fun `live session path without session id falls back to sessions list`() {
        val route = NotificationDeepLinkHandler.routeFor("safar://live/session")
        assertEquals(Routes.LIVE_SESSIONS_ROOT, route)
    }

    @Test
    fun deepLinkMapping_mapsStudyPlannerRoute() {
        assertEquals(Routes.STUDY_PLANNER, NotificationDeepLinkHandler.routeFor("safar://studyplanner"))
    }

    @Test
    fun `all youtube study links open the final channel mode`() {
        assertEquals(
            Routes.YOUTUBE_STUDY_MODE_V2,
            NotificationDeepLinkHandler.routeFor("safar://youtube_study_mode?section=channels"),
        )
        assertEquals(
            Routes.YOUTUBE_STUDY_MODE_V2,
            NotificationDeepLinkHandler.routeFor("safar://youtube_study_mode/analytics"),
        )
        assertEquals(
            Routes.YOUTUBE_STUDY_MODE_V2,
            NotificationDeepLinkHandler.routeFor("safar://youtube_focus"),
        )
    }

    @Test
    fun `https links are opened externally while unsupported schemes are rejected`() {
        // Android URI parsing is stubbed in local JVM tests. Supply URI fields so
        // these assertions exercise the handler's policy, not the Android stub.
        mockkStatic(Uri::class)
        try {
            every { Uri.parse(any()) } answers {
                val parsed = java.net.URI(firstArg<String>())
                mockk<Uri> {
                    every { scheme } returns parsed.scheme
                    every { host } returns parsed.host
                    every { path } returns parsed.path
                }
            }
            assertTrue(NotificationDeepLinkHandler.isExternalWebLink("https://safar.parmarssc.in/updates"))
            assertFalse(NotificationDeepLinkHandler.isExternalWebLink("http://safar.parmarssc.in/updates"))
            assertFalse(NotificationDeepLinkHandler.isExternalWebLink("javascript:alert(1)"))
        } finally {
            unmockkStatic(Uri::class)
        }
    }
}
