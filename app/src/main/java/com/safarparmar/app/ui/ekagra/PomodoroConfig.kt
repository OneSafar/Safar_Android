package com.safarparmar.app.ui.ekagra

internal enum class PomodoroStyle { TRADITIONAL, CUSTOM }

internal data class PomodoroStartRequest(
    val style: PomodoroStyle,
    val loops: Int,
    val focusMinutes: Int,
    val breakMinutes: Int,
)

internal data class PomodoroConfig(
    val style: PomodoroStyle,
    val loops: Int,
    val focusSeconds: Int,
    val breakSeconds: Int,
    val finalBreakSeconds: Int,
)

internal data class PomodoroBreakTransition(val breakSeconds: Int, val completesAfterBreak: Boolean)

internal fun pomodoroBreakTransition(
    completedLoops: Int,
    targetLoops: Int,
    breakSeconds: Int,
    finalBreakSeconds: Int,
): PomodoroBreakTransition {
    val isFinal = completedLoops >= targetLoops.coerceAtLeast(1)
    return PomodoroBreakTransition(
        breakSeconds = if (isFinal) finalBreakSeconds else breakSeconds,
        completesAfterBreak = isFinal,
    )
}

internal fun normalizePomodoroConfig(
    style: PomodoroStyle,
    loops: Int,
    focusMinutes: Int,
    breakMinutes: Int,
): PomodoroConfig = when (style) {
    PomodoroStyle.TRADITIONAL -> PomodoroConfig(
        style = style,
        loops = 4,
        focusSeconds = 25 * 60,
        breakSeconds = 5 * 60,
        finalBreakSeconds = 15 * 60,
    )
    PomodoroStyle.CUSTOM -> {
        val safeBreakSeconds = breakMinutes.coerceIn(1, 60) * 60
        PomodoroConfig(
            style = style,
            loops = loops.coerceIn(1, 12),
            focusSeconds = focusMinutes.coerceIn(1, 120) * 60,
            breakSeconds = safeBreakSeconds,
            finalBreakSeconds = safeBreakSeconds,
        )
    }
}
