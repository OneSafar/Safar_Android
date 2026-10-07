package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.assertEquals
import org.junit.Test

class BatchReminderTimeTest {
    @Test fun malformedSavedTimesCannotCrashTheReminderPicker() {
        listOf(null, "", "invalid", "19:xx", "99:00", "12:99").forEach {
            val time = normalizedStudyReminderTime(it)
            assertEquals("19:00", time)
            assertEquals(19, time.take(2).toInt())
            assertEquals(0, time.takeLast(2).toInt())
        }
    }

    @Test fun legacyTimesWithSecondsRetainTheirHourAndMinute() {
        assertEquals("08:45", normalizedStudyReminderTime("08:45:30"))
        assertEquals("23:59", normalizedStudyReminderTime(" 23:59 "))
        assertEquals("00:00", normalizedStudyReminderTime("00:00"))
    }
}
