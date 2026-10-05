package com.safarparmar.app.feature.toppersbatch

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
internal enum class BatchEventKind(val title: String, val symbol: String) {
    CLASS("Class", "◷"), PLANNED("Planned lectures", "▣"), DONE("Completed", "✓"), REVISION("Revision", "↻"), MILESTONE("Key dates", "⚑")
}
internal data class BatchCalendarEvent(val date: String, val kind: BatchEventKind, val title: String,
    val lecture: BatchLecture? = null, val subject: BatchSubject? = null, val completed: Boolean = false)
internal data class BatchSubjectDates(val subject: BatchSubject, val nextClass: BatchLecture?,
    val firstClass: String?, val firstCompletion: String?, val nextPersonal: BatchLecture?)
internal fun BatchOverview.calendarSubjects(today: String): List<BatchSubjectDates> =
    subjects.filter { it.enabled && it.key in setOf("english", "mathematics", "reasoning", "gk") }.map { subject ->
        val own = lectures.filter { it.subjectId == subject.id }
        val scheduled = own.filter { calendarDate(it.scheduledFor) != null }
            .sortedWith(compareBy({ calendarDate(it.scheduledFor) }, { it.classTime.orEmpty().padStart(5, '0') }, { it.order }))
        BatchSubjectDates(subject, scheduled.firstOrNull { calendarDate(it.scheduledFor)!! >= today },
            scheduled.firstOrNull()?.let { calendarDate(it.scheduledFor) },
            own.mapNotNull { completionDay(it.completedAt) }.minOrNull(),
            own.sortedBy { it.order }.firstOrNull { it.completedAt == null && !it.isLocked(today) })
    }
internal fun BatchOverview.calendarEvents(): List<BatchCalendarEvent> {
    val active = subjects.filter { it.enabled && it.key in setOf("english", "mathematics", "reasoning", "gk") }.associateBy { it.id }
    val events = lectures.flatMap { row ->
        val subject = active[row.subjectId] ?: return@flatMap emptyList()
        buildList {
            calendarDate(row.scheduledFor)?.let { add(BatchCalendarEvent(it, BatchEventKind.CLASS, row.displayTopic, row, subject)) }
            calendarDate(row.studyPlannedFor)?.let { add(BatchCalendarEvent(it, BatchEventKind.PLANNED, row.displayTopic, row, subject)) }
            completionDay(row.completedAt)?.let { add(BatchCalendarEvent(it, BatchEventKind.DONE, row.displayTopic, row, subject)) }
            if (row.completedAt != null) {
                if (row.sessions.isNotEmpty()) row.sessions.forEach { session ->
                    val date = if (session.completedAt != null) completionDay(session.completedAt) else calendarDate(session.date)
                    date?.let { add(BatchCalendarEvent(it, BatchEventKind.REVISION, row.displayTopic, row, subject, session.completedAt != null)) }
                } else calendarDate(row.revisionDate)?.let { scheduled ->
                    val date = completionDay(row.revisionCompletedAt) ?: scheduled
                    add(BatchCalendarEvent(date, BatchEventKind.REVISION, row.displayTopic, row, subject, row.revisionCompletedAt != null))
                }
            }
        }
    }.toMutableList()
    calendarDate(course.officialStartDate)?.let { events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "Batch starts")) }
    calendarDate(studyPlan?.startDate)?.let { events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "Your plan starts")) }
    events.filter { it.kind == BatchEventKind.DONE }.minOfOrNull { it.date }?.let {
        events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "First lecture completed"))
    }
    calendarDate(studyPlan?.targetDate)?.let { events.add(BatchCalendarEvent(it, BatchEventKind.MILESTONE, "Your target finish")) }
    return events.sortedWith(compareBy({ it.date }, { it.kind.ordinal }, { it.lecture?.classTime.orEmpty().padStart(5, '0') }, { it.subject?.name.orEmpty() }))
}
