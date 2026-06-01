package com.safarparmar.app.notifications

import com.safarparmar.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
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
}
