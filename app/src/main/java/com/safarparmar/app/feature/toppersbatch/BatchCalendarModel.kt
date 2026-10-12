package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.R

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal val batchCalendarZone: ZoneId = ZoneId.of("Asia/Kolkata")
internal fun completionDay(value: String?): String? = value?.let {
    runCatching { Instant.parse(it).atZone(batchCalendarZone).toLocalDate().toString() }.getOrNull()
}
internal fun calendarDate(value: String?): String? = value?.take(10)?.let {
    runCatching { LocalDate.parse(it).toString() }.getOrNull()
}
internal enum class BatchEventKind(@androidx.annotation.StringRes val titleRes: Int, val symbol: String) {
    CLASS(R.string.toppers_batch_class, "◷"), PLANNED(R.string.toppers_batch_planned_lectures, "▣"), DONE(R.string.toppers_batch_completed, "✓"), REVISION(R.string.toppers_batch_revision, "↻"), MILESTONE(R.string.toppers_batch_key_dates, "⚑")
}
internal enum class BatchMilestone(@androidx.annotation.StringRes val labelRes: Int) {
    BATCH_START(R.string.toppers_batch_batch_start), PLAN_START(R.string.toppers_batch_my_start_date),
    FIRST_COMPLETION(R.string.toppers_batch_first_lecture_done), TARGET_FINISH(R.string.toppers_batch_goal_date)
}
internal data class BatchCalendarEvent(val date: String, val kind: BatchEventKind, val title: String,
    val lecture: BatchLecture? = null, val subject: BatchSubject? = null, val completed: Boolean = false, val actualCompletedDate: String? = null, val milestone: BatchMilestone? = null)
internal data class BatchSubjectDates(val subject: BatchSubject, val nextClass: BatchLecture?,
    val firstClass: String?, val firstCompletion: String?, val nextPersonal: BatchLecture?)
internal fun BatchOverview.calendarSubjects(today: String): List<BatchSubjectDates> =
    subjects.filter { it.enabled && it.key in setOf("english", "mathematics", "reasoning", "gk") }.map { subject ->
        val own = lectures.filter { it.subjectId == subject.id }
        val scheduled = own.filter { calendarDate(it.scheduledFor) != null }
            .sortedWith(compareBy({ calendarDate(it.scheduledFor) }, { it.classTime.orEmpty().padStart(5, '0') }, { it.order }))
        BatchSubjectDates(subject, scheduled.firstOrNull { calendarDate(it.scheduledFor)!! >= today },
            scheduled.firstOrNull()?.let { calendarDate(it.scheduledFor) },
            own.filterNot { it.completionDateUnknown }.mapNotNull { completionDay(it.completedAt) }.minOrNull(),
            own.sortedBy { it.order }.firstOrNull { it.completedAt == null && !it.isLocked(today) && (it.studyPlannedFor == null || it.studyPlannedFor <= today) })
    }
internal fun BatchOverview.calendarEvents(): List<BatchCalendarEvent> {
    val active = subjects.filter { it.enabled && it.key in setOf("english", "mathematics", "reasoning", "gk") }.associateBy { it.id }
    val events = lectures.flatMap { row ->
        val subject = active[row.subjectId] ?: return@flatMap emptyList()
        buildList {
            calendarDate(row.scheduledFor)?.let { add(BatchCalendarEvent(it, BatchEventKind.CLASS, row.displayTopic, row, subject)) }
            calendarDate(row.studyPlannedFor)?.let { add(BatchCalendarEvent(it, BatchEventKind.PLANNED, row.displayTopic, row, subject, row.completedAt != null)) }
            completionDay(row.completedAt?.takeUnless { row.completionDateUnknown })?.let { add(BatchCalendarEvent(it, BatchEventKind.DONE, row.displayTopic, row, subject)) }
            if (row.completedAt != null) {
                if (row.sessions.isNotEmpty()) row.sessions.forEach { session ->
                    val date = calendarDate(session.date)
                    date?.let { add(BatchCalendarEvent(it, BatchEventKind.REVISION, row.displayTopic, row, subject, session.completedAt != null, completionDay(session.completedAt))) }
                    val actual = completionDay(session.completedAt)
                    if (actual != null && actual != date) add(BatchCalendarEvent(actual, BatchEventKind.REVISION, row.displayTopic, row, subject, true, actual))
                } else calendarDate(row.revisionDate)?.let { scheduled ->
                    val date = scheduled
                    add(BatchCalendarEvent(date, BatchEventKind.REVISION, row.displayTopic, row, subject, row.revisionCompletedAt != null, completionDay(row.revisionCompletedAt)))
                    completionDay(row.revisionCompletedAt)?.takeIf { it != date }?.let { add(BatchCalendarEvent(it, BatchEventKind.REVISION, row.displayTopic, row, subject, true, it)) }
                }
            }
        }
    }.toMutableList()
    calendarDate(course.officialStartDate)?.let { events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "Batch starts", milestone = BatchMilestone.BATCH_START)) }
    calendarDate(studyPlan?.startDate)?.let { events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "Your plan starts", milestone = BatchMilestone.PLAN_START)) }
    events.filter { it.kind == BatchEventKind.DONE }.minOfOrNull { it.date }?.let {
        events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "First lecture completed", milestone = BatchMilestone.FIRST_COMPLETION))
    }
    calendarDate(studyPlan?.targetDate)?.let { events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "Your target finish", milestone = BatchMilestone.TARGET_FINISH)) }
    return events.sortedWith(compareBy({ it.date }, { it.kind.ordinal }, { it.lecture?.classTime.orEmpty().padStart(5, '0') }, { it.subject?.name.orEmpty() }))
}

/** Group repeated sessions so the menu lists each lecture once. */
internal fun pendingCalendarRevisions(events: List<BatchCalendarEvent>): List<Pair<BatchCalendarEvent, Int>> =
    events.filter { it.kind == BatchEventKind.REVISION && !it.completed && it.lecture != null }
        .groupBy { it.lecture!!.id }.values.map { it.first() to it.size }

/** Today's official classes only; historical classes remain in the selected-day agenda. */
internal fun BatchLecture.officialClassStart(): Instant? =
    runCatching { Instant.parse(liveWindow?.startsAt ?: classStartsAt) }.getOrNull()
        ?: runCatching { LocalDate.parse(scheduledFor).atTime(java.time.LocalTime.parse(classTime)).atZone(batchCalendarZone).toInstant() }.getOrNull()
internal fun BatchLecture.officialClassEnd(): Instant? =
    runCatching { Instant.parse(liveWindow?.endsAt) }.getOrNull() ?: officialClassStart()?.plusSeconds(7200)
internal fun BatchOverview.remainingClasses(now: Instant): List<BatchSubjectDates> {
    val today = now.atZone(batchCalendarZone).toLocalDate().toString()
    return calendarSubjects(today).mapNotNull { info ->
        val row = lectures.filter { it.subjectId == info.subject.id && calendarDate(it.scheduledFor) == today &&
            it.releaseState != "locked" && (it.officialClassEnd()?.isAfter(now) != false) }
            .minByOrNull { it.officialClassStart() ?: Instant.MAX } ?: return@mapNotNull null
        info.copy(nextClass = row)
    }.sortedBy { it.nextClass?.officialClassStart() ?: Instant.MAX }
}
