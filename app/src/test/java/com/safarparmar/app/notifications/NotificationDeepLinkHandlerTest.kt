package com.safarparmar.app.notifications

import android.net.Uri
import com.safarparmar.app.ui.navigation.Routes
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
    fun `youtube study links open management and analytics destinations`() {
        assertEquals(
            Routes.YOUTUBE_STUDY_MODE,
            NotificationDeepLinkHandler.routeFor("safar://youtube_study_mode?section=channels"),
        )
        assertEquals(
            Routes.YOUTUBE_STUDY_ANALYTICS,
            NotificationDeepLinkHandler.routeFor("safar://youtube_study_mode/analytics"),
        )
    }

    @Test
    fun `https links are opened externally while unsupported schemes are rejected`() {
        assertTrue(NotificationDeepLinkHandler.isExternalWebLink("https://safar.parmarssc.in/updates"))
        assertFalse(NotificationDeepLinkHandler.isExternalWebLink("http://safar.parmarssc.in/updates"))
        assertFalse(NotificationDeepLinkHandler.isExternalWebLink("javascript:alert(1)"))
    }
}
