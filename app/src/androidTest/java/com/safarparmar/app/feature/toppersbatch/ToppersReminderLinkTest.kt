package com.safarparmar.app.feature.toppersbatch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToppersReminderLinkTest {
    @Test fun classReminderOpensAndroidTracker() {
        val url = "https://safar.parmarssc.in/study/toppers-batch"
        val intent = NotificationDeepLinkHandler.activityIntent(
            InstrumentationRegistry.getInstrumentation().targetContext, url,
        )
        assertNotNull(intent.component)
        assertEquals(Routes.TOPPERS_BATCH, intent.getStringExtra(NotificationDeepLinkHandler.EXTRA_ROUTE))
        assertEquals(Routes.TOPPERS_BATCH, NotificationDeepLinkHandler.routeFor("safar://toppers_batch"))
    }

    @Test fun unrelatedWebLinksRemainExternal() {
        val url = "https://example.com/study/toppers-batch"
        val intent = NotificationDeepLinkHandler.activityIntent(
            InstrumentationRegistry.getInstrumentation().targetContext, url,
        )
        assertEquals(null, intent.component)
    }
}
