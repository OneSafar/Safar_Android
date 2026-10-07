package com.safarparmar.app.feature.toppersbatch

/** Release status is independent of the student's completion or personal watch date. */
internal fun BatchLecture.isReleasedForProgress(day: String): Boolean = isAvailable ?: when (releaseState) {
    "locked" -> false
    "released" -> true
    else -> calendarDate(scheduledFor)?.let { it <= day } ?: false
}

internal data class ReleasedSubjectProgress(val subject: BatchSubject, val lectures: List<BatchLecture>) {
    val releasedCount get() = lectures.size
    val completedCount get() = lectures.count { it.completedAt != null }
    val remaining get() = lectures.filter { it.completedAt == null }
}

internal data class BatchReleaseProgress(val subjects: List<ReleasedSubjectProgress>) {
    val releasedCount get() = subjects.sumOf { it.releasedCount }
    val completedCount get() = subjects.sumOf { it.completedCount }
    val remainingCount get() = releasedCount - completedCount
    val fraction get() = if (releasedCount == 0) 0f else completedCount.toFloat() / releasedCount
}

/** Compare the same released lecture set for every student, regardless of joining date or study mode. */
internal fun BatchOverview.releaseProgress(day: String): BatchReleaseProgress = BatchReleaseProgress(
    planningSubjects().map { subject ->
        ReleasedSubjectProgress(subject, lectures.filter {
            it.subjectId == subject.id && !it.userAdded && it.isReleasedForProgress(day)
        }.sortedWith(compareBy({ it.lectureNumber.takeIf { number -> number > 0 } ?: Int.MAX_VALUE }, { it.order }, { it.id })))
    },
)
