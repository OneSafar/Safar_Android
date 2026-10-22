package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

/** Presentation preference: both modes retain the same saved plans and completion history. */
enum class BatchStudyMode(@androidx.annotation.StringRes val titleRes: Int) {
    OFFICIAL(R.string.toppers_batch_official_schedule), PERSONAL(R.string.toppers_batch_my_own_pace)
}

internal fun BatchOverview.defaultStudyMode(): BatchStudyMode =
    if (lectures.any { it.studyPlannedFor != null }) BatchStudyMode.PERSONAL else BatchStudyMode.OFFICIAL

internal fun BatchOverview.officialLecturesFrom(day: String): List<BatchLecture> {
    val active = subjects.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    return lectures.filter { it.subjectId in active && calendarDate(it.scheduledFor)?.let { date -> date >= day } == true }
        .sortedWith(compareBy({ calendarDate(it.scheduledFor) }, { it.classTime.orEmpty().padStart(5, '0') }, { it.order }, { it.lectureNumber }))
}

/** Official order ignores personal dates; personal order follows assigned dates, including missed plans. */
internal fun BatchOverview.nextStudyLecture(subjectId: String, mode: BatchStudyMode, day: String): BatchLecture? {
    if (subjects.none { it.id == subjectId && it.enabled }) return null
    return if (mode == BatchStudyMode.OFFICIAL) officialLecturesFrom(day)
        .firstOrNull { it.subjectId == subjectId && it.completedAt == null }
    else lectures.filter { it.subjectId == subjectId && it.completedAt == null && calendarDate(it.studyPlannedFor) != null }
        .minWithOrNull(compareBy({ calendarDate(it.studyPlannedFor) }, { it.order }, { it.lectureNumber }))
}

internal fun BatchOverview.calendarEvents(mode: BatchStudyMode): List<BatchCalendarEvent> =
    calendarEvents().filterNot { mode == BatchStudyMode.OFFICIAL &&
        (it.kind == BatchEventKind.PLANNED || (it.kind == BatchEventKind.MILESTONE && it.milestone in setOf(BatchMilestone.PLAN_START, BatchMilestone.TARGET_FINISH))) }
