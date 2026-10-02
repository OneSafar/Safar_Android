package com.safarparmar.app.widget

import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.util.assignedDateKey
import com.safarparmar.app.util.isGoalCompleted
import com.safarparmar.app.util.isHiddenFromActiveGoals
import com.safarparmar.app.util.isVisibleInGoals
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

internal val widgetZone: ZoneId = ZoneId.of("Asia/Kolkata")

internal fun widgetFocusSeconds(sessions: List<EkagraAnalyticsFocusSession>, today: LocalDate): Long =
    sessions.filter { session ->
        session.timerMode !in listOf("short", "long") &&
            runCatching { Instant.parse(session.endedAt ?: session.startedAt).atZone(widgetZone).toLocalDate() }
                .getOrNull() == today
    }.sumOf { if (it.actualSeconds > 0) it.actualSeconds.toLong() else it.actualMinutes.coerceAtLeast(0).toLong() * 60 }

internal data class WidgetGoalCounts(val completed: Int, val pending: Int)

internal fun widgetGoalCounts(goals: List<Goal>, today: LocalDate): WidgetGoalCounts {
    val visible = goals.filter {
        it.deletedAt.isNullOrBlank() && it.isVisibleInGoals() && !it.isHiddenFromActiveGoals() &&
            (it.assignedDateKey() == today.toString() || (it.assignedDateKey() == null && !it.isGoalCompleted()))
    }
    val completed = visible.count { it.isGoalCompleted() }
    return WidgetGoalCounts(completed, visible.size - completed)
}

/** Merge overlapping sources, clamp to the day, and never extrapolate beyond a heartbeat. */
internal fun widgetProtectedSeconds(windows: List<Pair<Long, Long>>, from: Long, until: Long): Long {
    val clipped = windows.map { maxOf(from, it.first) to minOf(until, it.second) }
        .filter { it.second > it.first }.sortedBy { it.first }
    var end = from
    var total = 0L
    for ((start, stop) in clipped) {
        total += (stop - maxOf(start, end)).coerceAtLeast(0)
        end = maxOf(end, stop)
    }
    return total / 1_000L
}
