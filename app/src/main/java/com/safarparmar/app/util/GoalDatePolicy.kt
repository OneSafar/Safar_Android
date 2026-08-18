package com.safarparmar.app.util

import com.safarparmar.app.domain.model.Goal

/**
 * One source of truth for placing a goal in Today, Upcoming, Missed, or History.
 *
 * A goal's assigned day never changes merely because it was completed late.
 * `completedAt` records when the work was actually finished; `assignedDateKey`
 * records the day whose plan/outcome the goal belongs to.
 */
fun Goal.assignedDateKey(): String? =
    IstDateUtils.getDateKey(scheduledDate)
        ?: IstDateUtils.getDateKey(createdAt)
        ?: IstDateUtils.getDateKey(startedAt)

fun Goal.isGoalCompleted(): Boolean =
    completed || !completedAt.isNullOrBlank() || status.equals("completed", ignoreCase = true)

fun Goal.isHiddenFromActiveGoals(): Boolean =
    lifecycleStatus.equals("abandoned", ignoreCase = true) ||
        lifecycleStatus.equals("rolled_over", ignoreCase = true) ||
        status.equals("cancelled", ignoreCase = true) ||
        status.equals("rolled_over", ignoreCase = true)

fun Goal.isMissedGoal(todayKey: String = IstDateUtils.todayKey()): Boolean {
    if (isGoalCompleted() || isHiddenFromActiveGoals()) return false
    val explicitlyMissed = lifecycleStatus.equals("missed", ignoreCase = true) ||
        status.equals("missed", ignoreCase = true) ||
        status.equals("expired", ignoreCase = true)
    val assignedBeforeToday = assignedDateKey()?.let { it < todayKey } == true
    return explicitlyMissed || assignedBeforeToday
}

fun Goal.isTodayGoal(todayKey: String = IstDateUtils.todayKey()): Boolean {
    if (isGoalCompleted() || isHiddenFromActiveGoals() || isMissedGoal(todayKey)) return false
    val assigned = assignedDateKey()
    // Very old records can lack every timestamp. Keep them actionable instead of
    // making them disappear from all tabs.
    return assigned == null || assigned == todayKey
}

fun Goal.isUpcomingGoal(todayKey: String = IstDateUtils.todayKey()): Boolean {
    if (isGoalCompleted() || isHiddenFromActiveGoals()) return false
    return assignedDateKey()?.let { it > todayKey } == true
}
