package com.safarparmar.app.ui.ekagra.focusshield

import com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository.ShieldPrefs.isWithinSchedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KavachScheduleTest {

    @Test
    fun sameDaySchedule_correctlyEnforcesRange() {
        val startMinute = 9 * 60 // 09:00 AM = 540
        val endMinute = 22 * 60  // 10:00 PM = 1320

        // Inside active window
        assertTrue(isWithinSchedule(9 * 60, startMinute, endMinute))
        assertTrue(isWithinSchedule(14 * 60, startMinute, endMinute))
        assertTrue(isWithinSchedule(22 * 60, startMinute, endMinute))

        // Outside active window
        assertFalse(isWithinSchedule(8 * 60, startMinute, endMinute))
        assertFalse(isWithinSchedule(23 * 60, startMinute, endMinute))
        assertFalse(isWithinSchedule(2 * 60, startMinute, endMinute))
    }

    @Test
    fun overnightSchedule_correctlyEnforcesRange() {
        val startMinute = 22 * 60 // 10:00 PM = 1320
        val endMinute = 6 * 60    // 06:00 AM = 360

        // Inside active overnight window
        assertTrue(isWithinSchedule(23 * 60, startMinute, endMinute))
        assertTrue(isWithinSchedule(2 * 60, startMinute, endMinute))
        assertTrue(isWithinSchedule(6 * 60, startMinute, endMinute))
        assertTrue(isWithinSchedule(22 * 60, startMinute, endMinute))

        // Outside active overnight window
        assertFalse(isWithinSchedule(7 * 60, startMinute, endMinute))
        assertFalse(isWithinSchedule(12 * 60, startMinute, endMinute))
        assertFalse(isWithinSchedule(21 * 60, startMinute, endMinute))
    }
}
