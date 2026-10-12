package com.safarparmar.app.ui.ekagra

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroConfigTest {
    @Test fun `three loop configuration keeps selected rhythm`() {
        assertEquals(
            PomodoroConfig(PomodoroStyle.CUSTOM, loops = 3, focusSeconds = 25 * 60, breakSeconds = 5 * 60, finalBreakSeconds = 5 * 60),
            normalizePomodoroConfig(PomodoroStyle.CUSTOM, loops = 3, focusMinutes = 25, breakMinutes = 5),
        )
    }

    @Test fun `configuration is bounded before timer arithmetic`() {
        assertEquals(
            PomodoroConfig(PomodoroStyle.CUSTOM, loops = 12, focusSeconds = 120 * 60, breakSeconds = 60 * 60, finalBreakSeconds = 60 * 60),
            normalizePomodoroConfig(PomodoroStyle.CUSTOM, Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE),
        )
    }

    @Test fun `traditional configuration is fixed and ends with a long break`() {
        assertEquals(
            PomodoroConfig(PomodoroStyle.TRADITIONAL, loops = 4, focusSeconds = 25 * 60, breakSeconds = 5 * 60, finalBreakSeconds = 15 * 60),
            normalizePomodoroConfig(PomodoroStyle.TRADITIONAL, loops = 9, focusMinutes = 90, breakMinutes = 30),
        )
    }

    @Test fun `custom uses the same break after every round including final`() {
        val config = normalizePomodoroConfig(PomodoroStyle.CUSTOM, 4, 60, 10)
        assertEquals(PomodoroBreakTransition(10 * 60, false), pomodoroBreakTransition(1, config.loops, config.breakSeconds, config.finalBreakSeconds))
        assertEquals(PomodoroBreakTransition(10 * 60, true), pomodoroBreakTransition(4, config.loops, config.breakSeconds, config.finalBreakSeconds))
    }

    @Test fun `traditional uses fifteen minute final break`() {
        val config = normalizePomodoroConfig(PomodoroStyle.TRADITIONAL, 4, 25, 5)
        assertEquals(PomodoroBreakTransition(5 * 60, false), pomodoroBreakTransition(3, config.loops, config.breakSeconds, config.finalBreakSeconds))
        assertEquals(PomodoroBreakTransition(15 * 60, true), pomodoroBreakTransition(4, config.loops, config.breakSeconds, config.finalBreakSeconds))
    }
}
