package com.safarparmar.app.ui.studyplanner.logic

import com.safarparmar.app.domain.model.studyplanner.StudyChapter
import com.safarparmar.app.domain.model.studyplanner.StudySubject
import com.safarparmar.app.domain.model.studyplanner.StudyTopic

/** One chapter or topic that matched, with where it lives so the result can say "Subject · Chapter". */
data class SyllabusSearchHit(
    val subject: StudySubject,
    val chapter: StudyChapter,
    /** Null for a chapter-name match. */
    val topic: StudyTopic? = null,
)

data class SyllabusSearchResults(
    val subjects: List<StudySubject>,
    val chapters: List<SyllabusSearchHit>,
    val topics: List<SyllabusSearchHit>,
    /** True when [topics] was cut at the limit. */
    val topicsTruncated: Boolean,
) {
    val isEmpty: Boolean get() = subjects.isEmpty() && chapters.isEmpty() && topics.isEmpty()
}

/**
 * Flat syllabus search: every match is listed at its own level instead of only
 * filtering subjects, so typing "Syllogism" shows that topic directly.
 */
fun List<StudySubject>.searchSyllabus(query: String, topicLimit: Int = 50): SyllabusSearchResults {
    val q = query.trim()
    if (q.isEmpty()) return SyllabusSearchResults(emptyList(), emptyList(), emptyList(), false)
    val subjects = filter { it.name.contains(q, ignoreCase = true) }
    val chapters = flatMap { subject ->
        subject.chapters.filter { it.name.contains(q, ignoreCase = true) }.map { SyllabusSearchHit(subject, it) }
    }
    val allTopics = asSequence().flatMap { subject ->
        subject.chapters.asSequence().flatMap { chapter ->
            chapter.topics.asSequence().filter { it.name.contains(q, ignoreCase = true) }
                .map { SyllabusSearchHit(subject, chapter, it) }
        }
    }.take(topicLimit + 1).toList()
    return SyllabusSearchResults(subjects, chapters, allTopics.take(topicLimit), allTopics.size > topicLimit)
}
