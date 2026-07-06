package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.PlanProgress
import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudyPlan
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
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

fun StudyPlan.rollup(): PlanProgress {
    val topics = flattenTopics().map { it.topic }
    val total = topics.size
    val done = topics.count { it.status == TopicStatus.DONE }
    val revision = topics.count { it.status == TopicStatus.REVISION_NEEDED }
    val percent = if (total == 0) 0 else ((done.toFloat() / total) * 100).roundToInt()
    return progress ?: PlanProgress(
        totalTopics = total,
        doneTopics = done,
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

const val BULK_IMPORT_PLACEHOLDER_NAME = "Untitled"

fun isBulkPlaceholderChapter(chapter: StudyChapter): Boolean =
    chapter.name.trim().equals(BULK_IMPORT_PLACEHOLDER_NAME, ignoreCase = true)

private fun normalizeBulkTopicToken(input: String): String =
    input.trim()

private fun splitBulkTopicTokens(input: String, allowCommaSeparated: Boolean): List<String> {
    val normalized = normalizeBulkTopicToken(input)
    if (normalized.isBlank()) return emptyList()
    val tokens = if (allowCommaSeparated) normalized.split(",") else listOf(normalized)
    return tokens.map { it.trim() }.filter { it.isNotBlank() }
}

fun extractBulkTopicsFromSyllabusCode(text: String): String {
    val seen = mutableSetOf<String>()
    val topics = text.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith(">") }
        .map { normalizeBulkTopicToken(it) }
        .filter { it.isNotBlank() }
        .filter { seen.add(it.lowercase(Locale.US)) }
        .toList()
    return topics.joinToString("\n")
}

/**
 * Student-readable manual import: `Subject:`, `Chapter:`/`Unit:`, `Topic:`, and
 * comma-separated topic lists after `Topics:`.
 */
fun parseBulkSubjectsFromTxt(text: String): Result<List<BulkSubjectParsed>> = runCatching {
    val rawLines = text.split("\r?\n".toRegex())
    val subjectIndexByKey = mutableMapOf<String, Int>()
    val chapterIndexBySubjectKey = mutableMapOf<String, MutableMap<String, Int>>()
    val topicSeenByChapter = mutableMapOf<String, MutableSet<String>>()
    val subjects = mutableListOf<Pair<String, MutableList<Pair<String, MutableList<String>>>>>()

    fun ensureSubject(name: String): Int {
        val normalizedName = name.trim().ifBlank { BULK_IMPORT_PLACEHOLDER_NAME }
        val subjectKey = normalizedName.lowercase(Locale.US)
        subjectIndexByKey[subjectKey]?.let { return it }
        subjects += normalizedName to mutableListOf()
        val nextIndex = subjects.lastIndex
        subjectIndexByKey[subjectKey] = nextIndex
        chapterIndexBySubjectKey[subjectKey] = mutableMapOf()
        return nextIndex
    }

    fun ensureChapter(subjectIndex: Int?, name: String): Int {
        val resolvedSubjectIndex = subjectIndex ?: ensureSubject(BULK_IMPORT_PLACEHOLDER_NAME)
        val normalizedName = name.trim().ifBlank { BULK_IMPORT_PLACEHOLDER_NAME }
        val subjectName = subjects[resolvedSubjectIndex].first
        val subjectKey = subjectName.lowercase(Locale.US)
        val chapterMap = chapterIndexBySubjectKey[subjectKey] ?: error("Could not track chapter structure.")
        val chapterKey = normalizedName.lowercase(Locale.US)
        chapterMap[chapterKey]?.let { return it }
        subjects[resolvedSubjectIndex].second += normalizedName to mutableListOf()
        val nextIndex = subjects[resolvedSubjectIndex].second.lastIndex
        chapterMap[chapterKey] = nextIndex
        topicSeenByChapter.getOrPut("$subjectKey::$chapterKey") { mutableSetOf() }
        return nextIndex
    }

    fun addTopic(subjectIndex: Int?, chapterIndex: Int?, topicRaw: String, allowCommaSeparated: Boolean = false) {
        val resolvedSubjectIndex = subjectIndex ?: ensureSubject(BULK_IMPORT_PLACEHOLDER_NAME)
        val resolvedChapterIndex = chapterIndex ?: ensureChapter(resolvedSubjectIndex, BULK_IMPORT_PLACEHOLDER_NAME)
        val subjectName = subjects[resolvedSubjectIndex].first
        val chapterName = subjects[resolvedSubjectIndex].second[resolvedChapterIndex].first
        val chapterKey = "${subjectName.lowercase(Locale.US)}::${chapterName.lowercase(Locale.US)}"
        val seen = topicSeenByChapter.getOrPut(chapterKey) { mutableSetOf() }
        splitBulkTopicTokens(topicRaw, allowCommaSeparated).forEach { topic ->
            val tk = topic.lowercase(Locale.US)
            if (tk in seen) return@forEach
            seen += tk
            subjects[resolvedSubjectIndex].second[resolvedChapterIndex].second += topic
        }
    }

    var activeSubjectIndex: Int? = null
    var activeChapterIndex: Int? = null

    val oldSymbolHeading = Regex("""^[-_>]\s+.*$""")
    val subjectHeading = Regex("""^(?:subject|sub|विषय)\s*[:：\-]\s*(.*)$""", RegexOption.IGNORE_CASE)
    val chapterHeading = Regex("""^(?:chapter|chap|unit|lesson|अध्याय)\s*[:：\-]\s*(.*)$""", RegexOption.IGNORE_CASE)
    val labeledTopicHeading = Regex("""^(?:topic|topics|टॉपिक)\s*[:：\-]\s*(.*)$""", RegexOption.IGNORE_CASE)

    for (index in rawLines.indices) {
        val line = rawLines[index].trim()
        if (line.isEmpty()) continue
        require(!oldSymbolHeading.matches(line)) {
            "Use labels instead: Subject:, Chapter:, and Topic:."
        }

        val sm = subjectHeading.matchEntire(line)
        if (sm != null) {
            activeSubjectIndex = ensureSubject(sm.groupValues[1])
            activeChapterIndex = null
            continue
        }
        val cm = chapterHeading.matchEntire(line)
        if (cm != null) {
            if (activeSubjectIndex == null) activeSubjectIndex = ensureSubject(BULK_IMPORT_PLACEHOLDER_NAME)
            activeChapterIndex = ensureChapter(activeSubjectIndex, cm.groupValues[1])
            continue
        }
        val ltm = labeledTopicHeading.matchEntire(line)
        if (ltm != null) {
            if (activeSubjectIndex == null) activeSubjectIndex = ensureSubject(BULK_IMPORT_PLACEHOLDER_NAME)
            if (activeChapterIndex == null) activeChapterIndex = ensureChapter(activeSubjectIndex, BULK_IMPORT_PLACEHOLDER_NAME)
            addTopic(activeSubjectIndex, activeChapterIndex, ltm.groupValues[1], allowCommaSeparated = true)
            continue
        }
        if (activeSubjectIndex == null) activeSubjectIndex = ensureSubject(BULK_IMPORT_PLACEHOLDER_NAME)
        if (activeChapterIndex == null) activeChapterIndex = ensureChapter(activeSubjectIndex, BULK_IMPORT_PLACEHOLDER_NAME)
        addTopic(activeSubjectIndex, activeChapterIndex, line)
    }

    require(subjects.isNotEmpty()) { "No syllabus content found." }

    subjects.map { (subjectName, chapters) ->
        BulkSubjectParsed(
            subjectName = subjectName,
            chapters = chapters.map { (chapterName, topics) ->
                BulkChapterParsed(chapterName = chapterName, topics = topics.toList())
            },
        )
    }
}

fun countBulkSubjectsTopics(groups: List<BulkSubjectParsed>): Int =
    groups.sumOf { subject ->
        subject.chapters.sumOf { it.topics.size }
    }

fun countBulkSubjectsChapters(groups: List<BulkSubjectParsed>): Int =
    groups.sumOf { it.chapters.size }
