package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class TodayPlanModelTest {
    private val day = "2026-10-05"
    private val subject = BatchSubject(id = "english", key = "english", name = "English")
    private fun row(id: String, date: String? = day) = BatchLecture(id = id, subjectId = subject.id,
        subjectKey = subject.key, studyPlannedFor = date, displayTopic = id)
    private fun overview(vararg rows: BatchLecture) = BatchOverview(subjects = listOf(subject), lectures = rows.toList())

    @Test fun completingAndUndoingPreservePlannedTotal() {
        val first = row("first")
        val data = overview(first, row("second"))
        val done = data.withLecture(first.copy(completedAt = "2026-10-05T10:00:00Z"), day)
        assertEquals(2, done.todayPlan(day).total)
        assertEquals(1, done.todayPlan(day).completed)
        assertEquals("second", done.todayPlan(day).tasks.first().lecture.id)
        val undo = done.withLecture(first, day).todayPlan(day)
        assertEquals(2, undo.total)
        assertEquals(0, undo.completed)
    }

    @Test fun unplannedAndFutureCompletionsNeverInflateTodaysPlan() {
        val data = overview(row("planned"), row("extra", null).copy(completedAt = "2026-10-05T10:00:00Z"),
            row("future", "2026-10-06").copy(completedAt = "2026-10-05T10:00:00Z"))
        assertEquals(1, data.todayPlan(day).total)
        assertEquals(0, data.todayPlan(day).completed)
    }

    @Test fun importedProgressOnlyFulfilsAnExplicitPlan() {
        val imported = row("imported", null).copy(completedAt = "2026-10-05T10:00:00Z", completionDateUnknown = true)
        assertEquals(0, overview(imported).todayPlan(day).total)
        assertEquals(1, overview(imported.copy(studyPlannedFor = day)).todayPlan(day).completed)
        assertEquals(0, overview(imported).projectStudyWorkflow(day).second.completedToday.size)
    }

    @Test fun revisionCountsUseScheduledSessionsRatherThanActualCompletionDay() {
        val review = row("review", null).copy(completedAt = "2026-10-01T10:00:00Z", revisionSessions = listOf(
            ReviewSession("2026-10-04", "2026-10-05T08:00:00Z"),
            ReviewSession(day), ReviewSession("2026-10-06")))
        val data = overview(row("watch"), review)
        assertEquals(2, data.todayPlan(day).total)
        assertEquals(1, data.todayPlan(day).lectures)
        assertEquals(1, data.todayPlan(day).revisions)
        assertEquals(0, data.todayPlan(day).completed)
        val revised = review.copy(revisionSessions = review.sessions.map {
            if (it.date == day) it.copy(completedAt = "2026-10-05T10:00:00Z") else it
        })
        val completed = data.withLecture(revised, day).todayPlan(day)
        assertEquals(2, completed.total)
        assertEquals(1, completed.completed)
    }

    @Test fun earlierPendingRevisionIsFlaggedWithoutCountingItAsTodaysTask() {
        val review = row("review", null).copy(completedAt = "saved", revisionSessions = listOf(
            ReviewSession("2026-10-04"), ReviewSession(day)))
        val task = overview(review).todayPlan(day).tasks.single()
        assertEquals(1, task.sessionIndex)
        assertEquals("2026-10-04", task.earlierRevisionDate)
        assertFalse(task.completed)
    }

    @Test fun singleDateAndLegacyRevisionPlansRemainSupported() {
        val review = row("review", null).copy(completedAt = "saved", revisionDate = day, revisionCompletedAt = "done")
        assertEquals(1, overview(review).todayPlan(day).completed)
        val legacy = review.copy(revisionCompletedAt = null, legacyRevisionSessions = listOf(ReviewSession(day)))
        assertEquals(0, overview(legacy).todayPlan(day).completed)
        assertTrue(overview(review.copy(completedAt = null)).todayPlan(day).tasks.isEmpty())
    }

    @Test fun removingReschedulingAndDayRolloverChangeTheTaskSet() {
        val lecture = row("watch")
        val data = overview(lecture)
        assertEquals(0, data.withLecture(lecture.copy(studyPlannedFor = null), day).todayPlan(day).total)
        val tomorrow = data.withLecture(lecture.copy(studyPlannedFor = "2026-10-06"), day)
        assertEquals(0, tomorrow.todayPlan(day).total)
        assertEquals(1, tomorrow.todayPlan("2026-10-06").total)
        assertEquals(1, data.projectStudyWorkflow("2026-10-06").second.missed.size)
    }

    @Test fun pickerExcludesLockedDoneHiddenAndAlreadyPlannedLectures() {
        val data = overview(row("available", null).copy(order = 2), row("overdue", "2026-10-04").copy(order = 1),
            row("today"), row("future", "2026-10-06"), row("done", null).copy(completedAt = "saved"),
            row("locked", null).copy(isAvailable = false), row("hidden", null).copy(subjectId = "hidden"))
        assertEquals(listOf("overdue", "available"), data.planningCandidates(day).map { it.id })
    }

    @Test fun disabledSubjectsAndEmptyPlansProduceNoTasks() {
        assertEquals(0, BatchOverview().todayPlan(day).total)
        assertEquals(0, overview(row("watch")).copy(subjects = listOf(subject.copy(enabled = false))).todayPlan(day).total)
    }

    @Test fun nextSuggestionsAdvanceAfterPlanningWithoutRequiringCompletion() {
        val first = row("first", null).copy(order = 1)
        val second = row("second", null).copy(order = 2)
        val data = overview(second, first)
        assertEquals("first", data.nextPlanningLectures(day)[subject.id]?.id)
        val planned = data.withLecture(first.copy(studyPlannedFor = day), day)
        assertEquals("second", planned.nextPlanningLectures(day)[subject.id]?.id)
        assertEquals(1, planned.todayPlan(day).total)
        val both = planned.withLecture(second.copy(studyPlannedFor = day), day)
        assertTrue(both.nextPlanningLectures(day).isEmpty())
        assertEquals(2, both.todayPlan(day).total)
    }
    @Test fun subjectGroupsHaveConsistentSubjectOrderAndNumericLectureOrder() {
        val maths = BatchSubject(id = "maths", key = "mathematics", name = "Maths")
        val reasoning = BatchSubject(id = "reasoning", key = "reasoning", name = "Reasoning")
        val gk = BatchSubject(id = "gk", key = "gk", name = "GK/GS")
        val data = BatchOverview(subjects = listOf(gk, maths, reasoning, subject), lectures = listOf(
            row("math10").copy(subjectId = maths.id, lectureNumber = 10, order = 1),
            row("reason2").copy(subjectId = reasoning.id, lectureNumber = 2, order = 1),
            row("math2").copy(subjectId = maths.id, lectureNumber = 2, order = 9, studyReminderTime = "21:00"),
            row("reason1").copy(subjectId = reasoning.id, lectureNumber = 1, order = 9, studyReminderTime = "22:00"),
            row("english").copy(lectureNumber = 1)))
        assertEquals(listOf("english", "reasoning", "maths", "gk"), data.planningSubjects().map { it.id })
        val groups = data.todayPlan(day).subjectGroups(data.planningSubjects(), completed = false)
        assertEquals(listOf("english", "reasoning", "maths"), groups.map { it.subject.id })
        assertEquals(listOf("reason1", "reason2"), groups[1].tasks.map { it.lecture.id })
        assertEquals(listOf("math2", "math10"), groups[2].tasks.map { it.lecture.id })
    }

    @Test fun completedGroupsRetainTotalsAndUndoReturnsTaskToItsPendingSubject() {
        val first = row("first").copy(lectureNumber = 1, completedAt = "2026-10-05T10:00:00Z")
        val data = overview(first, row("second").copy(lectureNumber = 2))
        val plan = data.todayPlan(day)
        assertEquals(listOf("second"), plan.subjectGroups(data.planningSubjects(), false).single().tasks.map { it.lecture.id })
        assertEquals(listOf("first"), plan.subjectGroups(data.planningSubjects(), true).single().tasks.map { it.lecture.id })
        val undo = data.withLecture(first.copy(completedAt = null), day).todayPlan(day)
        assertEquals(2, undo.total)
        assertEquals(listOf("first", "second"), undo.subjectGroups(data.planningSubjects(), false).single().tasks.map { it.lecture.id })
        assertTrue(undo.subjectGroups(data.planningSubjects(), true).isEmpty())
    }

    @Test fun suggestionsUseTheLowestEligibleLectureNumberAndIgnoreExistingPlans() {
        val data = overview(row("lecture10", null).copy(lectureNumber = 10, order = 1),
            row("lecture2", null).copy(lectureNumber = 2, order = 99), row("planned1").copy(lectureNumber = 1))
        assertEquals("lecture2", data.nextPlanningLectures(day)[subject.id]?.id)
        assertEquals(listOf("lecture2", "lecture10"), data.planningCandidates(day).map { it.id })
    }

}
