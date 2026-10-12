package com.safarparmar.app.feature.toppersbatch

/** Mirrors the server projection only for immediate ticks/undo and cached views. */
internal fun BatchOverview.projectStudyWorkflow(today: String): Pair<List<BatchSubjectWatch>, BatchStudyWorkflow> {
    val activities = mutableListOf<BatchStudyActivity>()
    val reasons = mutableMapOf<String, String>()
    var revised = 0
    val completedToday = mutableListOf<String>()
    val watch = subjects.filter { it.enabled }.map { subject ->
        val own = lectures.filter { it.subjectId == subject.id }.sortedWith(compareBy({ it.order }, { it.lectureNumber }))
        val furthest = own.indexOfLast { it.completedAt != null }
        own.forEachIndexed { index, row ->
            if (!row.completionDateUnknown && completionDay(row.completedAt) == today) completedToday += row.id
            if (row.completedAt == null && row.studyPlannedFor != null)
                activities += BatchStudyActivity(row.id, "watch", row.studyPlannedFor, row.studyReminderTime, waitingForRelease = row.isLocked(today))
            if (row.completedAt == null && !row.isLocked(today)) {
                val reason = when {
                    row.backlogAddedAt != null && row.backlogResolvedAt == null -> "manual"
                    row.studyPlannedFor != null && row.studyPlannedFor < today -> "missed"
                    index < furthest && !(row.studyPlannedFor != null && row.studyPlannedFor >= today) -> "skipped"
                    else -> null
                }
                if (reason != null) reasons[row.id] = reason
            }
            if (row.completedAt != null) {
                val sessions = row.sessions.ifEmpty { row.revisionDate?.let { listOf(ReviewSession(it, row.revisionCompletedAt)) }.orEmpty() }
                sessions.forEachIndexed { index, session ->
                    if (session.completedAt != null) revised++
                    else activities += BatchStudyActivity(row.id, "revision", session.date, row.revisionReminderTime,
                        sessionIndex = if (row.sessions.isEmpty()) null else index)
                }
            }
        }
        BatchSubjectWatch(subject.id, own.firstOrNull { it.completedAt == null && !it.isLocked(today) &&
            (it.studyPlannedFor == null || it.studyPlannedFor <= today) }?.id, own.filter { it.id in reasons }.map { it.id })
    }
    return watch to BatchStudyWorkflow(activities.filter { it.date == today }.sortedBy { it.reminderTime },
        activities.filter { it.date < today }.sortedBy { it.date }, reasons, revised, completedToday)
}
