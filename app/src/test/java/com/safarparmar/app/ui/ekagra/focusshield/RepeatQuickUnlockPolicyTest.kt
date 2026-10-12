package com.safarparmar.app.ui.ekagra.focusshield

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatQuickUnlockPolicyTest {
    @Test fun firstBreakNeedsNoReminder() {
        assertEquals(0, repeatQuickUnlockMinutes(null, 0, "2026-09-26"))
    }

    @Test fun repeatedBreakRemembersPreviousDuration() {
        for (minutes in listOf(5, 10, 15, 20)) {
            assertEquals(minutes, repeatQuickUnlockMinutes("2026-09-26", minutes, "2026-09-26"))
        }
    }

    @Test fun newDayAllowsFreshFirstBreak() {
        assertEquals(0, repeatQuickUnlockMinutes("2026-09-26", 10, "2026-09-27"))
    }

    @Test fun invalidHistoryDoesNotDisplayNegativeMinutes() {
        assertEquals(0, repeatQuickUnlockMinutes("2026-09-26", -5, "2026-09-26"))
    }
}
