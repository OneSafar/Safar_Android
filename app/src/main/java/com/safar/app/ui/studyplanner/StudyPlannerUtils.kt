package com.safar.app.ui.studyplanner

import com.safar.app.domain.model.studyplanner.PlanProgress
import com.safar.app.domain.model.studyplanner.StudyChapter
import com.safar.app.domain.model.studyplanner.StudyPlan
import com.safar.app.domain.model.studyplanner.StudySubject
import com.safar.app.domain.model.studyplanner.StudyTopic
import com.safar.app.domain.model.studyplanner.TopicStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

data class TopicRef(
    val subject: StudySubject,
    val chapter: StudyChapter,
    val topic: StudyTopic,
)

fun StudyPlan.flattenTopics(): List<TopicRef> = subjects.flatMap { subject ->
    subject.chapters.flatMap { chapter ->
        chapter.topics.map { topic -> TopicRef(subject, chapter, topic) }
    }
}

fun StudyPlan.rollup(): PlanProgress {
    val topics = flattenTopics().map { it.topic }
    val total = topics.size
    val done = topics.count { it.status == TopicStatus.DONE }
    val inProgress = topics.count { it.status == TopicStatus.IN_PROGRESS }
    val revision = topics.count { it.status == TopicStatus.REVISION_NEEDED }
    val percent = if (total == 0) 0 else ((done.toFloat() / total) * 100).roundToInt()
    return progress ?: PlanProgress(
        totalTopics = total,
        doneTopics = done,
        inProgressTopics = inProgress,
        revisionTopics = revision,
        completionPercent = percent,
        remainingPercent = 100 - percent,
    )
}

fun StudySubject.percentDone(): Int {
    val topics = chapters.flatMap { it.topics }
    if (topics.isEmpty()) return 0
    return ((topics.count { it.status == TopicStatus.DONE }.toFloat() / topics.size) * 100).roundToInt()
}

fun StudyChapter.percentDone(): Int {
    if (topics.isEmpty()) return 0
    return ((topics.count { it.status == TopicStatus.DONE }.toFloat() / topics.size) * 100).roundToInt()
}

fun todayKey(): String = LocalDate.now(ZoneId.systemDefault()).toString()

fun parsePlannerDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

fun daysUntil(value: String?): Long? {
    val date = parsePlannerDate(value) ?: return null
    return ChronoUnit.DAYS.between(LocalDate.now(ZoneId.systemDefault()), date)
}

fun readableDate(value: String?): String {
    val date = parsePlannerDate(value) ?: return "Not set"
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

fun parseBulkSyllabus(text: String): List<Pair<String, List<String>>> {
    val result = mutableListOf<Pair<String, MutableList<String>>>()
    var currentChapter = "General"
    fun ensureChapter(): MutableList<String> {
        val existing = result.firstOrNull { it.first == currentChapter }
        if (existing != null) return existing.second
        val topics = mutableListOf<String>()
        result += currentChapter to topics
        return topics
    }
    text.lineSequence()
        .map { it.trim().trimStart('-', '*', '•').trim() }
        .filter { it.isNotBlank() }
        .forEach { line ->
            val lower = line.lowercase(Locale.US)
            if (line.endsWith(":") || lower.startsWith("chapter ")) {
                currentChapter = line.removeSuffix(":").trim().ifBlank { "General" }
            } else {
                ensureChapter() += line
            }
        }
    return result.map { it.first to it.second.toList() }.filter { it.second.isNotEmpty() }
}
