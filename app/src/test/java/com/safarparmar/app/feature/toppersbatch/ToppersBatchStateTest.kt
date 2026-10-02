package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ToppersBatchStateTest {
    private val official = BatchSubject(id = "parmar", key = "english", name = "English")
    private val personal = BatchSubject(id = "own", key = "custom:own", name = "My course")
    private val officialLecture = BatchLecture(id = "a", subjectId = "parmar", subjectKey = "english",
        scheduledFor = "2026-09-28", backlogAddedAt = "2026-09-28T00:00:00Z")
    private val personalLecture = BatchLecture(id = "b", subjectId = "own", subjectKey = "custom:own")
    private val overview = BatchOverview(subjects = listOf(official, personal),
        lectures = listOf(officialLecture, personalLecture))

    @Test fun watchListAdvancesPerSubjectAndUndoRestoresThePreviousLecture() {
        val reasoning = BatchSubject(id = "reasoning", key = "reasoning", name = "Reasoning")
        val gk = BatchSubject(id = "gk", key = "gk", name = "GK")
        val first = BatchLecture(id = "english-1", subjectId = official.id, subjectKey = "english", order = 1)
        val second = first.copy(id = "english-2", order = 2, lectureNumber = 2)
        val reasoningFirst = first.copy(id = "reasoning-1", subjectId = reasoning.id, subjectKey = "reasoning")
        val batch = BatchOverview(subjects = listOf(official, reasoning, gk),
            lectures = listOf(first, second, reasoningFirst))
        val after = batch.withLecture(first.copy(completedAt = "2026-09-30T10:00:00Z"), "2026-09-30")
        assertEquals("english-2", after.watchList.first { it.subjectId == official.id }.nextLectureId)
        assertEquals("reasoning-1", after.watchList.first { it.subjectId == reasoning.id }.nextLectureId)
        assertEquals(null, after.watchList.first { it.subjectId == gk.id }.nextLectureId)
        assertEquals("english-1", after.withLecture(first, "2026-09-30")
            .watchList.first { it.subjectId == official.id }.nextLectureId)
    }

    @Test fun completingAndUndoingOlderLectureUpdatesCountsImmediately() {
        val today = "2026-09-29"
        val done = overview.withLecture(officialLecture.copy(completedAt = "2026-09-29T08:00:00Z", backlogResolvedAt = "2026-09-29T08:00:00Z"), today)
        assertEquals(1, done.progress.completed)
        assertEquals(0, done.progress.backlog)
        assertEquals(0, done.progress.behind)
        val undone = done.withLecture(officialLecture, today)
        assertEquals(0, undone.progress.completed)
        assertEquals(1, undone.progress.backlog)
        assertEquals(1, undone.progress.behind)
        assertEquals(2, undone.progress.total)
    }

    @Test fun personalCourseAndOfficialDatesRemainDistinct() {
        assertFalse(officialLecture.subjectKey.startsWith("custom:"))
        assertTrue(personalLecture.subjectKey.startsWith("custom:"))
        assertTrue(officialLecture.olderWork("2026-09-29"))
        assertFalse(personalLecture.olderWork("2026-09-29"))
        val doneOwn = overview.withLecture(personalLecture.copy(completedAt = "2026-09-29T10:00:00Z"), "2026-09-29")
        assertEquals(1, doneOwn.progress.bySubject.first { it.subjectId == "own" }.completed)
        assertEquals(0, doneOwn.progress.bySubject.first { it.subjectId == "parmar" }.completed)
    }

    @Test fun officialTrackerExcludesSavedPersonalCoursesFromEveryView() {
        val mixed = overview.copy(
            progress = BatchProgress(bySubject = listOf(
                SubjectProgress(subjectId = "parmar", completed = 1, total = 3, backlog = 1),
                SubjectProgress(subjectId = "own", completed = 2, total = 2),
            )),
            removedSubjects = listOf(official, personal),
            removedLectures = listOf(officialLecture, personalLecture),
            chapters = listOf(BatchChapter(id = "official-chapter", subjectId = "parmar"),
                BatchChapter(id = "personal-chapter", subjectId = "own")),
        ).officialOnly()
        assertEquals(listOf("parmar"), mixed.subjects.map { it.id })
        assertEquals(listOf("a"), mixed.lectures.map { it.id })
        assertEquals(listOf("parmar"), mixed.removedSubjects.map { it.id })
        assertEquals(listOf("a"), mixed.removedLectures.map { it.id })
        assertEquals(listOf("official-chapter"), mixed.chapters.map { it.id })
        assertEquals(1, mixed.progress.completed)
        assertEquals(3, mixed.progress.total)
        assertEquals(1, mixed.progress.backlog)
        assertEquals(listOf("a"), BatchToday(lectures = listOf(officialLecture, personalLecture),
            backlogPick = personalLecture).officialOnly().lectures.map { it.id })
        assertEquals(null, BatchToday(backlogPick = personalLecture).officialOnly().backlogPick)
        val calendar = BatchCalendar(days = mapOf("2026-09-29" to listOf(officialLecture, personalLecture)),
            classes = mapOf("2026-09-30" to listOf(personalLecture))).officialOnly()
        assertEquals(listOf("a"), calendar.days["2026-09-29"]?.map { it.id })
        assertTrue(calendar.classes.isEmpty())
    }

    @Test fun completionGraphUsesIndiaDayAndOnlySelectedLectures() {
        val selected = listOf(
            officialLecture.copy(id = "first", completedAt = "2026-09-28T20:00:00Z"),
            officialLecture.copy(id = "second", completedAt = "2026-09-29T10:00:00Z"),
            officialLecture.copy(id = "old", completedAt = "2026-09-21T10:00:00Z"),
        )
        val days = weeklyCompletions(selected, LocalDate.parse("2026-09-29"))
        assertEquals(7, days.size)
        assertEquals(LocalDate.parse("2026-09-23"), days.first().date)
        assertEquals(2, days.last().count)
        assertEquals(2, days.sumOf { it.count })
        assertEquals(0, weeklyCompletions(listOf(personalLecture), LocalDate.parse("2026-09-29")).sumOf { it.count })
    }

    @Test fun activityGridKeepsThirtyFiveOrderedDaysAndCountsCompletionDay() {
        val days = completionActivity(
            listOf(
                officialLecture.copy(id = "first", completedAt = "2026-09-28T20:00:00Z"),
                officialLecture.copy(id = "second", completedAt = "2026-09-29T10:00:00Z"),
                officialLecture.copy(id = "before-range", completedAt = "2026-08-23T10:00:00Z"),
                officialLecture.copy(id = "unfinished", completedAt = null),
            ),
            LocalDate.parse("2026-10-04"),
            35,
        )
        assertEquals(35, days.size)
        assertEquals(LocalDate.parse("2026-08-31"), days.first().date)
        assertEquals(LocalDate.parse("2026-10-04"), days.last().date)
        assertEquals(2, days.first { it.date == LocalDate.parse("2026-09-29") }.count)
        assertEquals(2, days.sumOf { it.count })
    }

    @Test fun completionTrendIncludesEarlierWorkAndUpdatesOnCompletionDay() {
        val trend = completionTrend(
            listOf(
                officialLecture.copy(id = "older", completedAt = "2026-09-01T10:00:00Z"),
                officialLecture.copy(id = "yesterday", completedAt = "2026-09-28T10:00:00Z"),
                officialLecture.copy(id = "today", completedAt = "2026-09-28T20:00:00Z"),
            ),
            LocalDate.parse("2026-09-29"),
        )
        assertEquals(14, trend.size)
        assertEquals(1, trend.first().completed)
        assertEquals(2, trend[12].completed)
        assertEquals(3, trend.last().completed)
        assertTrue(trend.zipWithNext().all { (left, right) -> right.completed >= left.completed })
    }
}
