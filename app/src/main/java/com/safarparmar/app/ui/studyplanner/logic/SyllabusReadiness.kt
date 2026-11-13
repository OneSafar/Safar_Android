package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudyTopic

/** Same rule as Today's "Missed": a past study date and not studied yet. */
fun StudyTopic.isLate(today: String = todayKey()): Boolean =
    !status.isStudied && (plannedDate?.take(10) ?: "9999") < today

/**
 * The one thing the Syllabus readiness card tells the student, picked by
 * priority. Exactly one status is shown, with at most one action, so the tab
 * never stacks several competing banners again.
 */
sealed interface SyllabusReadinessStatus {
    /** "Reorder first" was chosen in the reschedule flow; dates are still pending. */
    data object ReorderThenCreateDates : SyllabusReadinessStatus
    data object ExamDatePassed : SyllabusReadinessStatus
    data object NoExamDate : SyllabusReadinessStatus
    data class NeedsDates(val unscheduledCount: Int) : SyllabusReadinessStatus
    data object RatingsChanged : SyllabusReadinessStatus
    /** [chapterName] is the chapter with the most late topics — the one "Catch up" opens. */
    data class Late(
        val lateCount: Int,
        val subjectId: String,
        val chapterId: String,
        val chapterName: String,
    ) : SyllabusReadinessStatus
    data class FinishAfterExam(val forecastDate: String, val daysAfterExam: Int) : SyllabusReadinessStatus
    /** [forecastDate] is null when there is no recent pace to project from. */
    data class OnTrack(val forecastDate: String?) : SyllabusReadinessStatus
    data object Finished : SyllabusReadinessStatus
}

/**
 * Late and unscheduled use the same rules as Today ("Missed" and the
 * "Not Assigned Topics" banner), so the two tabs always report the same numbers.
 */
fun StudyPlan.syllabusReadiness(
    today: String = todayKey(),
    rebuildArmed: Boolean,
    ratingsChanged: Boolean,
    velocityForecastDate: String?,
): SyllabusReadinessStatus {
    val refs = flattenTopics()
    if (refs.isNotEmpty() && refs.all { it.topic.status.isStudied }) return SyllabusReadinessStatus.Finished
    if (rebuildArmed) return SyllabusReadinessStatus.ReorderThenCreateDates

    val examDay = examDate?.take(10)?.takeIf { it.isNotBlank() }
    if (examDay == null) return SyllabusReadinessStatus.NoExamDate
    if (examDay < today) return SyllabusReadinessStatus.ExamDatePassed

    val unscheduled = refs.count { it.topic.isUnscheduled() }
    if (unscheduled > 0) return SyllabusReadinessStatus.NeedsDates(unscheduled)

    if (ratingsChanged) return SyllabusReadinessStatus.RatingsChanged

    val late = refs.filter { it.topic.isLate(today) }
    if (late.isNotEmpty()) {
        // Same tie-break as courseMapNudge(): most late topics, then earliest date.
        val worst = late.groupBy { it.subject.id to it.chapter.id }.values.sortedWith(
            compareByDescending<List<TopicRef>> { it.size }
                .thenBy { group -> group.minOf { it.topic.plannedDate?.take(10).orEmpty() } },
        ).first().first()
        return SyllabusReadinessStatus.Late(late.size, worst.subject.id, worst.chapter.id, worst.chapter.name)
    }

    val forecast = velocityForecastDate?.take(10)?.takeIf { it.isNotBlank() }
    if (forecast != null && forecast > examDay) {
        val daysAfter = runCatching {
            java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(examDay), java.time.LocalDate.parse(forecast),
            ).toInt()
        }.getOrNull()
        if (daysAfter != null && daysAfter > 0) return SyllabusReadinessStatus.FinishAfterExam(forecast, daysAfter)
    }
    return SyllabusReadinessStatus.OnTrack(forecast)
}

/** A chapter that just reached 100%, plus the plan-wide chapter tally for the message. */
data class ChapterFinished(val chapterName: String, val finishedChapters: Int, val totalChapters: Int)

/**
 * Compares two versions of the SAME plan and returns the chapter that became
 * fully studied between them, or null. It only fires when that chapter's
 * studied count actually went up, so deleting a chapter's last open topic or
 * switching plans never shows a "finished" message.
 */
fun chapterFinishedBetween(before: StudyPlan?, after: StudyPlan?): ChapterFinished? {
    if (before == null || after == null || before.id != after.id) return null
    val oldChapters = before.subjects.flatMap { it.chapters }.associateBy { it.id }
    val newChapters = after.subjects.flatMap { it.chapters }
    fun com.safarparmar.app.domain.model.studyplanner.StudyChapter.isFinished() =
        topics.isNotEmpty() && topics.all { it.status.isStudied }
    val finished = newChapters.firstOrNull { chapter ->
        val old = oldChapters[chapter.id] ?: return@firstOrNull false
        chapter.isFinished() && !old.isFinished() &&
            chapter.topics.count { it.status.isStudied } > old.topics.count { it.status.isStudied }
    } ?: return null
    return ChapterFinished(finished.name, newChapters.count { it.isFinished() }, newChapters.size)
}
