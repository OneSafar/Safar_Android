package com.safarparmar.app.ui.ekagra.focusshield

import android.app.ActivityManager
import android.content.Context
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class BackgroundRunningAccessTest {
    @After fun cleanup() { unmockkAll() }

    @Test fun sharedGrantRequiresExemptionAndNoExplicitBackgroundRestriction() {
        val context = mockk<Context>()
        mockkObject(FocusShieldPermissionHelper)
        for (exempt in listOf(false, true)) for (restricted in listOf(false, true)) {
            every { FocusShieldPermissionHelper.isIgnoringBatteryOptimizations(context) } returns exempt
            every { FocusShieldPermissionHelper.isBackgroundRestricted(context) } returns restricted
            assertEquals(exempt && !restricted, FocusShieldPermissionHelper.hasBackgroundRunningAccess(context))
        }
    }
    @Test fun android28AndLaterReadRealBackgroundRestriction() {
        val manager = mockk<ActivityManager>()
        val context = mockk<Context> { every { getSystemService(Context.ACTIVITY_SERVICE) } returns manager }
        every { manager.isBackgroundRestricted } returns true
        assertTrue(FocusShieldPermissionHelper.readBackgroundRestriction(context))
        every { manager.isBackgroundRestricted } returns false
        assertFalse(FocusShieldPermissionHelper.readBackgroundRestriction(context))
    }
    @Test fun unsupportedSdkDoesNotCallRestrictionApi() {
        assertFalse(FocusShieldPermissionHelper.isBackgroundRestricted(mockk()))
    }
}
