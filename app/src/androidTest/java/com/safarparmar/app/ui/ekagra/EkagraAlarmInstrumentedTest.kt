package com.safarparmar.app.ui.ekagra

import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import com.safarparmar.app.notifications.SafarNotificationChannels
import org.junit.Assert.*
import org.junit.Test
import java.io.FileInputStream

class EkagraAlarmInstrumentedTest {
    @Test fun systemAlarmIsRegisteredWithAndWithoutExactAccess() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        // Runs twice via the test runner: default (denied) and shell-granted access.
        val expected = InstrumentationRegistry.getArguments().getString("expectedExact")?.toBooleanStrictOrNull()
        if (expected != null) assertEquals(expected, EkagraTimerAlarms.exactAllowed(context))
        try {
            EkagraTimerAlarms.schedule(context, "alarm-registration-test", 5, SystemClock.elapsedRealtime() + 3600_000L)
            val descriptor = instrumentation.uiAutomation.executeShellCommand("dumpsys alarm")
            val dump = FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
            descriptor.close()
            assertTrue("The system must own a deadline alarm", dump.contains("com.safar.ekagra.DEADLINE"))
            assertTrue(dump.contains(context.packageName))
        } finally { EkagraTimerAlarms.cancel(context) }
    }

    @Test fun ekagraChannelsUseDeviceSoundOrVibration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SafarNotificationChannels.createAll(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = manager.getNotificationChannel(SafarNotificationChannels.EKAGRA_ALERT)
        val vibration = manager.getNotificationChannel(SafarNotificationChannels.EKAGRA_VIBRATE_ALERT)
        assertEquals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), sound.sound)
        assertFalse(sound.shouldVibrate())
        assertNull(vibration.sound)
        assertTrue(vibration.shouldVibrate())
    }
}
