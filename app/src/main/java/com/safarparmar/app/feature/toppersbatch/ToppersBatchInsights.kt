package com.safarparmar.app.feature.toppersbatch

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

enum class LibraryFilter(val title: String) {
    ALL("All"), PENDING("Unfinished"), BACKLOG("Backlog"), REVISION("Revision"), DONE("Completed")
}

internal fun BatchLecture.completionDay(): LocalDate? = completedAt?.let {
    runCatching { Instant.parse(it).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate() }.getOrNull()
}

internal fun BatchOverview.libraryRows(query: String, filter: LibraryFilter, today: String = ToppersBatchViewModel.indiaDay()): List<BatchLecture> {
    val active = subjects.filter { it.enabled }.associateBy { it.id }
    val backlog = watchList.flatMap { it.backlogLectureIds }.toSet()
    return lectures.filter { lecture ->
        val subject = active[lecture.subjectId]
        subject != null && (query.isBlank() ||
            "${subject.name} ${lecture.displayTopic} ${lecture.lectureNumber} ${lecture.sourceMonth} ${lecture.sourceWeek}"
                .contains(query.trim(), ignoreCase = true)) && when (filter) {
            LibraryFilter.ALL -> true
            LibraryFilter.PENDING -> lecture.completedAt == null
            LibraryFilter.BACKLOG -> lecture.completedAt == null && !lecture.isLocked(today) && lecture.id in backlog
            LibraryFilter.REVISION -> lecture.pendingRevision
            LibraryFilter.DONE -> lecture.completedAt != null
        }
    }.sortedWith(compareBy<BatchLecture> {
        if (filter == LibraryFilter.REVISION) it.revisionDate.orEmpty() else it.subjectKey
    }.thenBy { it.order }.thenBy { it.lectureNumber })
}

internal data class PersonalInsights(
    val total: Int, val completed: Int, val remaining: Int,
    val firstCompletion: LocalDate?, val syllabusCompleted: LocalDate?,
    val recentCompleted: Int, val recentDays: Int, val activeDays: Int,
    val streak: Int, val revisionDue: Int, val officialGap: Int,
    val projectedFinish: LocalDate?, val requiredPerWeek: Int?, val targetMissed: Boolean,
)

/** Completion facts use India dates. Forecasts cover only the current tracked syllabus. */
internal fun BatchOverview.personalInsights(today: LocalDate): PersonalInsights {
    val active = subjects.filter { it.enabled }.map { it.id }.toSet()
    val own = lectures.filter { it.subjectId in active }
    val completed = own.count { it.completedAt != null }
    val dates = own.mapNotNull { it.completionDay() }.filter { it <= today }
    val start = studyPlan?.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val windowStart = maxOf(today.minusDays(6), start ?: today.minusDays(6)).coerceAtMost(today)
    val recentDays = ChronoUnit.DAYS.between(windowStart, today).toInt() + 1
    val recent = dates.count { it >= windowStart }
    val remaining = own.size - completed
    val target = studyPlan?.targetDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val targetDays = target?.let { ChronoUnit.DAYS.between(today, it).toInt() + 1 }
    val activity = dates.toSet()
    var cursor = if (today in activity) today else today.minusDays(1)
    var streak = 0
    while (cursor in activity) { streak++; cursor = cursor.minusDays(1) }
    return PersonalInsights(
        total = own.size, completed = completed, remaining = remaining,
        firstCompletion = dates.minOrNull(),
        syllabusCompleted = dates.maxOrNull().takeIf { own.isNotEmpty() && remaining == 0 },
        recentCompleted = recent, recentDays = recentDays, activeDays = activity.count { it >= windowStart },
        streak = streak,
        revisionDue = own.count { it.pendingRevision && it.revisionDate!! <= today.toString() },
        officialGap = own.count { it.completedAt == null && !it.isLocked(today.toString()) && it.scheduledFor != null && it.scheduledFor < today.toString() },
        projectedFinish = if (remaining > 0 && recent > 0) today.plusDays(ceil(remaining.toDouble() * recentDays / recent).toLong()) else null,
        requiredPerWeek = if (remaining > 0 && targetDays != null && targetDays > 0)
            ceil(remaining.toDouble() * 7 / targetDays).toInt() else null,
        targetMissed = remaining > 0 && target != null && target < today,
    )
}
