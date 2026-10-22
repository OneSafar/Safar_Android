package com.safarparmar.app.feature.toppersbatch

internal data class TodayPlanTask(
    val lecture: BatchLecture,
    val kind: String,
    val completed: Boolean,
    val reminderTime: String?,
    val sessionIndex: Int? = null,
    val earlierRevisionDate: String? = null,
)

internal data class TodayPlan(val tasks: List<TodayPlanTask>) {
    val total get() = tasks.size
    val completed get() = tasks.count { it.completed }
    val remaining get() = total - completed
    val lectures get() = tasks.count { it.kind == "watch" }
    val revisions get() = tasks.count { it.kind == "revision" }
}

/** Retained planned dates keep the denominator stable after completion and undo. */
internal fun BatchOverview.todayPlan(day: String): TodayPlan {
    val active = subjects.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    val tasks = lectures.filter { it.subjectId in active }.flatMap { lecture ->
        buildList {
            if (lecture.studyPlannedFor == day) add(TodayPlanTask(
                lecture, "watch", lecture.completedAt != null, lecture.studyReminderTime,
            ))
            if (lecture.completedAt != null) {
                val sessions = lecture.sessions.ifEmpty {
                    lecture.revisionDate?.let { listOf(ReviewSession(it, lecture.revisionCompletedAt)) }.orEmpty()
                }
                val firstPending = sessions.indexOfFirst { it.completedAt == null }
                sessions.forEachIndexed { index, session ->
                    if (session.date == day) add(TodayPlanTask(
                        lecture, "revision", session.completedAt != null, lecture.revisionReminderTime,
                        sessionIndex = if (lecture.sessions.isEmpty()) null else index,
                        earlierRevisionDate = sessions.getOrNull(firstPending)?.date
                            ?.takeIf { session.completedAt == null && firstPending < index },
                    ))
                }
            }
        }
    }.sortedWith(compareBy({ it.completed }, { it.reminderTime.orEmpty() }, { it.lecture.order }, { it.lecture.lectureNumber }))
    return TodayPlan(tasks)
}

internal fun BatchOverview.planningCandidates(day: String): List<BatchLecture> {
    val active = subjects.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    return lectures.filter {
        it.subjectId in active && it.completedAt == null && !it.isLocked(day) &&
            (it.studyPlannedFor == null || it.studyPlannedFor < day)
    }.sortedWith(compareBy({ it.lectureNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }, { it.order }, { it.id }))
}

internal fun BatchOverview.nextPlanningLectures(day: String): Map<String, BatchLecture> =
    planningCandidates(day).groupBy { it.subjectId }.mapValues { (_, rows) -> rows.first() }

internal data class TodaySubjectTasks(val subject: BatchSubject, val tasks: List<TodayPlanTask>)

/** Shared subject order for both the checklist and one-tap lecture suggestions. */
internal fun BatchOverview.planningSubjects(): List<BatchSubject> = subjects.filter { it.enabled }
    .sortedWith(compareBy<BatchSubject>({ when (it.key) {
        "english" -> 0; "reasoning" -> 1; "mathematics" -> 2; "gk" -> 3; else -> 4
    } }, { it.name.lowercase(java.util.Locale.ROOT) }, { it.id }))

internal fun TodayPlan.subjectGroups(subjects: List<BatchSubject>, completed: Boolean): List<TodaySubjectTasks> =
    subjects.mapNotNull { subject ->
        val own = tasks.filter { it.lecture.subjectId == subject.id && it.completed == completed }
            .sortedWith(compareBy({ it.lecture.lectureNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE },
                { it.lecture.order }, { if (it.kind == "watch") 0 else 1 }, { it.sessionIndex ?: -1 }, { it.lecture.id }))
        own.takeIf { it.isNotEmpty() }?.let { TodaySubjectTasks(subject, it) }
    }

/** Upcoming lectures include every chosen watch/revision date, even when an earlier lecture is unfinished. */
internal fun BatchOverview.upcomingStudyActivities(day: String): List<BatchStudyActivity> {
    val active = subjects.filter { it.enabled }.mapTo(mutableSetOf()) { it.id }
    return lectures.filter { it.subjectId in active }.flatMap { lecture ->
        buildList {
            if (lecture.completedAt == null && calendarDate(lecture.studyPlannedFor)?.let { it > day } == true) {
                add(BatchStudyActivity(lecture.id, "watch", lecture.studyPlannedFor!!, lecture.studyReminderTime))
            }
            if (lecture.completedAt != null) {
                val sessions = lecture.sessions.ifEmpty {
                    lecture.revisionDate?.let { listOf(ReviewSession(it, lecture.revisionCompletedAt)) }.orEmpty()
                }
                sessions.forEachIndexed { index, session ->
                    if (session.completedAt == null && calendarDate(session.date)?.let { it > day } == true) {
                        add(BatchStudyActivity(lecture.id, "revision", session.date, lecture.revisionReminderTime,
                            sessionIndex = if (lecture.sessions.isEmpty()) null else index))
                    }
                }
            }
        }
    }.sortedWith(compareBy({ it.date }, { it.reminderTime.orEmpty() }, { it.lectureId }, { it.sessionIndex ?: -1 }))
}
