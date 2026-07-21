package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.domain.model.studyplanner.effortPoints
import com.safarparmar.app.domain.model.studyplanner.progressPercentValue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
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

/** Never scheduled. Explicitly disjoint from Missed. */
fun StudyTopic.isUnscheduled(): Boolean =
    status != TopicStatus.DONE && plannedDate.isNullOrBlank() && missedReason.isNullOrBlank()

/** Previously due, including topics deferred with Done for the Day. */
fun StudyTopic.isMissed(today: String = todayKey()): Boolean {
    if (status == TopicStatus.DONE) return false
    if (!missedReason.isNullOrBlank()) return true
    val dateKey = plannedDate?.take(10).orEmpty()
    return dateKey.isNotBlank() && dateKey < today
}

/**
 * The one shared effort-weighted completion formula, mirroring the server's
 * rollupProgress: Σ(points × progress) ÷ Σ points, where points come from the
 * topic size (small=1, medium=2, big=4, chapter rating as fallback) and
 * progress is the partial completion (done = 100). Every progress surface
 * (status card, dashboard, syllabus bars, insights, calendar, export) should
 * derive its percentage through this so they never disagree.
 */
fun weightedCompletionPercent(topics: List<Pair<StudyTopic, StudyChapter?>>): Int {
    var totalPoints = 0f
    var earnedPoints = 0f
    for ((topic, chapter) in topics) {
        val points = topic.effortPoints(chapter).toFloat()
        totalPoints += points
        earnedPoints += points * topic.progressPercentValue() / 100f
    }
    if (totalPoints <= 0f) return 0
    return ((earnedPoints / totalPoints) * 100).roundToInt()
}

fun StudyPlan.rollup(): PlanProgress {
    val refs = flattenTopics()
    val topics = refs.map { it.topic }
    val total = topics.size
    val done = topics.count { it.status == TopicStatus.DONE }
    val revision = topics.count { it.status == TopicStatus.REVISION_NEEDED }
    val percent = weightedCompletionPercent(refs.map { it.topic to it.chapter })
    val dailyTodosList = dailyTodos.orEmpty()
    val hasDailyTodos = dailyTodosList.isNotEmpty()
    val dailyPercent = if (!hasDailyTodos) {
        0
    } else {
        val todayStr = todayKey()
        val logsForToday = dailyTodoLogs?.get(todayStr).orEmpty()
        val completedCount = dailyTodosList.count { it.id in logsForToday }
        ((completedCount.toFloat() / dailyTodosList.size) * 100).roundToInt()
    }
    
    // Exam progress and recurring daily to-dos are deliberately independent.
    // Recompute these fields locally even when an older cached server rollup is
    // present, so an app update cannot keep showing the former blended value.
    return (progress ?: PlanProgress()).copy(
        totalTopics = total,
        doneTopics = done,
        revisionTopics = revision,
        completionPercent = percent,
        remainingPercent = 100 - percent,
        plannerProgressPercent = percent,
        dailyTodoProgressPercent = dailyPercent,
        overallProgressPercent = percent,
    )
}

fun StudySubject.percentDone(): Int =
    weightedCompletionPercent(chapters.flatMap { ch -> ch.topics.map { it to ch } })

fun StudyChapter.percentDone(): Int =
    weightedCompletionPercent(topics.map { it to this })

/** Returns a user-facing error, or null if [rawName] is acceptable to submit. */
fun validateSyllabusNodeName(rawName: String): String? =
    if (rawName.trim().isBlank()) "Please type a name first" else null

/** Case-insensitive sibling-name collision check (e.g. "Physics" vs "physics"). */
fun findDuplicateSiblingName(rawName: String, siblingNames: List<String>): Boolean =
    siblingNames.any { it.trim().equals(rawName.trim(), ignoreCase = true) }

data class SubjectDeleteImpact(val chapterCount: Int, val topicCount: Int, val scheduledTopicCount: Int)

fun StudySubject.deleteImpact(): SubjectDeleteImpact = SubjectDeleteImpact(
    chapterCount = chapters.size,
    topicCount = chapters.sumOf { it.topics.size },
    scheduledTopicCount = chapters.sumOf { ch -> ch.topics.count { !it.plannedDate.isNullOrBlank() } },
)

data class ChapterDeleteImpact(val topicCount: Int, val scheduledTopicCount: Int)

fun StudyChapter.deleteImpact(): ChapterDeleteImpact = ChapterDeleteImpact(
    topicCount = topics.size,
    scheduledTopicCount = topics.count { !it.plannedDate.isNullOrBlank() },
)

fun todayKey(): String = LocalDate.now(ZoneId.systemDefault()).toString()

fun parsePlannerDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    return runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}

fun daysUntil(value: String?, today: LocalDate = LocalDate.now(ZoneId.systemDefault())): Long? {
    val date = parsePlannerDate(value) ?: return null
    return ChronoUnit.DAYS.between(today, date)
}

/**
 * Single-line exam context for headers (never interpolates Kotlin `null` as the string "null").
 */
fun plannerExamSubtitle(examDateIso: String?, today: LocalDate = LocalDate.now(ZoneId.systemDefault())): String {
    val days = daysUntil(examDateIso, today) ?: return "Set exam date"
    val datePart = readableDate(examDateIso).takeUnless { it == "Not set" }.orEmpty()
    return when {
        days < 0 -> listOf("Exam passed", datePart).filter { it.isNotBlank() }.joinToString(" • ")
        days == 0L -> listOf("Exam today!", datePart).filter { it.isNotBlank() }.joinToString(" • ")
        else -> {
            val lead = if (days == 1L) "1 day left" else "$days days left"
            listOf(lead, datePart).filter { it.isNotBlank() }.joinToString(" • ")
        }
    }
}

/** Large numeric area for calendar-style hero cards (non-positive uses supportive caption via [plannerExamCountdownCaption]). */
fun plannerExamCountdownHeroNumber(days: Long?): String = when {
    days == null || days < 0L -> "—"
    else -> days.toString()
}

fun plannerExamCountdownCaption(days: Long?): String = when {
    days == null -> "Set exam date"
    days < 0L -> "Exam passed"
    days == 0L -> "Exam today!"
    else -> "DAYS"
}

fun plannerExamCountdownCaptionSecondary(days: Long?): String = when {
    days == null || days < 0L || days == 0L -> ""
    else -> "LEFT"
}

/** Compact badge / chip copy for exam countdown. */
fun examBadgeLabel(days: Long?): String = when {
    days == null -> "Set exam date"
    days < 0 -> "Exam passed"
    days == 0L -> "Exam today!"
    days == 1L -> "1 day left"
    days > 999 -> "999+ days left"
    else -> "$days days left"
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

/** JS `getDay()`: Sun=0 … Sat=6 — matches [StudyPlan.offDays] used by OffDayPicker. */
fun jsDayOfWeek(date: LocalDate): Int =
    when (date.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }

/**
 * Sun-first calendar grid for [yearMonth] (aligned with [jsDayOfWeek] / off-days).
 * `null` entries pad before the first day and after the last day of the month.
 */
fun monthCalendarSlots(yearMonth: YearMonth): List<LocalDate?> {
    val lead = jsDayOfWeek(yearMonth.atDay(1))
    val dim = yearMonth.lengthOfMonth()
    val out = mutableListOf<LocalDate?>()
    repeat(lead) { out.add(null) }
    for (d in 1..dim) {
        out.add(yearMonth.atDay(d))
    }
    while (out.size % 7 != 0) {
        out.add(null)
    }
    return out
}

/** Next schedulable date after [fromDateIso] (exclusive start+1), skipping off-days (TS `findNextAvailableDate`). */
fun findNextAvailablePlannedDateIso(fromDateIso: String, offDays: List<Int>): String {
    val base = parsePlannerDate(fromDateIso) ?: LocalDate.now(ZoneId.systemDefault())
    val start = base.plusDays(1)
    val off = offDays.toSet()
    var cursor = start
    repeat(366) {
        if (jsDayOfWeek(cursor) !in off) return cursor.toString()
        cursor = cursor.plusDays(1)
    }
    return start.toString()
}

data class BulkChapterParsed(val chapterName: String, val topics: List<String>)

data class BulkSubjectParsed(val subjectName: String, val chapters: List<BulkChapterParsed>)

fun parseBulkSubjectsFromTxt(text: String): Result<List<BulkSubjectParsed>> = runCatching {
    data class MutableChapter(val name: String, val topics: MutableList<String> = mutableListOf())
    data class MutableSubject(val name: String, val chapters: LinkedHashMap<String, MutableChapter> = LinkedHashMap())

    val subjects = LinkedHashMap<String, MutableSubject>()
    val seenTopicKeys = mutableSetOf<String>()

    text.split("\r?\n".toRegex()).forEachIndexed { index, rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty()) return@forEachIndexed
        val parts = line.split(">").map { it.trim() }
        require(parts.size == 3 && parts.all { it.isNotEmpty() }) {
            "Line ${index + 1}: use the format Subject > Chapter > Topic"
        }
        val (subjectName, chapterName, topicName) = parts
        val subjectKey = subjectName.lowercase(Locale.US)
        val subject = subjects.getOrPut(subjectKey) { MutableSubject(subjectName) }
        val chapterKey = chapterName.lowercase(Locale.US)
        val chapter = subject.chapters.getOrPut(chapterKey) { MutableChapter(chapterName) }
        if (seenTopicKeys.add("$subjectKey::$chapterKey::${topicName.lowercase(Locale.US)}")) {
            chapter.topics += topicName
        }
    }

    require(subjects.isNotEmpty()) {
        "No syllabus content found. Add a line like Maths > Algebra > Linear Equations."
    }

    subjects.values.map { s ->
        BulkSubjectParsed(s.name, s.chapters.values.map { BulkChapterParsed(it.name, it.topics.toList()) })
    }
}

fun countBulkSubjectsTopics(groups: List<BulkSubjectParsed>): Int =
    groups.sumOf { subject ->
        subject.chapters.sumOf { it.topics.size }
    }

fun countBulkSubjectsChapters(groups: List<BulkSubjectParsed>): Int =
    groups.sumOf { it.chapters.size }
