package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus

/** A scheduled review stays visible after the API advances plannedDate to its next session. */
fun StudyTopic.completedReviewForDay(date: String): String? =
    revisionCompletedDates.orEmpty().firstOrNull { it.take(10) == date }?.take(10)
        ?: revisionCompletionLog.orEmpty().lastOrNull { it.completedDate.take(10) == date }?.sessionDate?.take(10)

fun StudyTopic.isVisibleToday(date: String): Boolean =
    plannedDate?.take(10) == date || completedReviewForDay(date) != null ||
        (status == TopicStatus.REVISION_NEEDED && revisionReminderDates.any { it.take(10) == date })

fun StudyTopic.isCompletedToday(date: String): Boolean =
    status == TopicStatus.DONE || completedReviewForDay(date) != null

/** Stored IDs are only trusted after matching the signed-in user's current list. */
fun plannerLandingPlan(plans: List<StudyPlan>, activeId: String?): StudyPlan? =
    plans.firstOrNull { it.id == activeId } ?: plans.singleOrNull()

/** Keep work moved to Missed in the stopped day's total; stopping is not completion. */
data class DailyAgendaSummary(val completed: Int, val total: Int, val stopped: Boolean) {
    val isComplete: Boolean get() = !stopped && total > 0 && completed == total
}

fun dailyAgendaSummary(topics: List<StudyTopic>, date: String, stopped: Boolean): DailyAgendaSummary {
    val visible = topics.filter { it.isVisibleToday(date) }
    val moved = if (stopped) topics.count {
        !it.isVisibleToday(date) && it.isMissed(date) && it.originalPlannedDate?.take(10) == date
    } else 0
    return DailyAgendaSummary(visible.count { it.isCompletedToday(date) }, visible.size + moved, stopped)
}
