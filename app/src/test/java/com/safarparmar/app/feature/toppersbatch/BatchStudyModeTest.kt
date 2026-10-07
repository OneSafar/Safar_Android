package com.safarparmar.app.feature.toppersbatch

import org.junit.Assert.*
import org.junit.Test

class BatchStudyModeTest {
    private val day = "2026-10-05"
    private val subject = BatchSubject(id = "e", key = "english", name = "English")
    private val first = BatchLecture(id = "e1", subjectId = "e", subjectKey = "english", order = 1,
        lectureNumber = 1, scheduledFor = "2026-10-04", studyPlannedFor = "2026-10-09")
    private val second = first.copy(id = "e2", order = 2, lectureNumber = 2, scheduledFor = day, studyPlannedFor = "2026-10-06")
    private val third = first.copy(id = "e3", order = 3, lectureNumber = 3, scheduledFor = "2026-10-06", studyPlannedFor = null)
    private val batch = BatchOverview(subjects = listOf(subject), lectures = listOf(first, second, third))

    @Test fun officialModeUsesOfficialDatesRegardlessOfPersonalDatesOrMissedHistory() {
        assertEquals("e2", batch.nextStudyLecture("e", BatchStudyMode.OFFICIAL, day)?.id)
        assertEquals(listOf("e2", "e3"), batch.officialLecturesFrom(day).map { it.id })
        assertEquals("e3", batch.withLecture(second.copy(completedAt = "2026-10-05T12:00:00Z"), day)
            .nextStudyLecture("e", BatchStudyMode.OFFICIAL, day)?.id)
    }

    @Test fun ownPaceUsesPlannedDateOrderRatherThanLectureNumberOrOfficialSchedule() {
        assertEquals("e2", batch.nextStudyLecture("e", BatchStudyMode.PERSONAL, day)?.id)
        val done = batch.withLecture(second.copy(completedAt = "2026-10-05T12:00:00Z"), day)
        assertEquals("e1", done.nextStudyLecture("e", BatchStudyMode.PERSONAL, day)?.id)
    }

    @Test fun missedPersonalDateRemainsNextUntilCompletedOrRescheduled() {
        val missed = batch.withLecture(first.copy(studyPlannedFor = "2026-10-03"), day)
        assertEquals("e1", missed.nextStudyLecture("e", BatchStudyMode.PERSONAL, day)?.id)
    }

    @Test fun personalModeDoesNotInventAPlanForAnUnassignedLecture() {
        val unplanned = batch.copy(lectures = listOf(third))
        assertNull(unplanned.nextStudyLecture("e", BatchStudyMode.PERSONAL, day))
        assertEquals("e3", unplanned.nextPlanningLectures("2026-10-07")["e"]?.id)
    }

    @Test fun disabledSubjectsAndInvalidDatesAreExcluded() {
        assertNull(batch.copy(subjects = listOf(subject.copy(enabled = false)))
            .nextStudyLecture("e", BatchStudyMode.OFFICIAL, day))
        assertNull(batch.copy(subjects = listOf(subject.copy(enabled = false)))
            .nextStudyLecture("e", BatchStudyMode.PERSONAL, day))
        val invalid = batch.copy(lectures = listOf(first.copy(scheduledFor = "bad", studyPlannedFor = "bad")))
        assertNull(invalid.nextStudyLecture("e", BatchStudyMode.OFFICIAL, day))
        assertNull(invalid.nextStudyLecture("e", BatchStudyMode.PERSONAL, day))
    }

    @Test fun officialCalendarHidesCustomDatesButRetainsClassesCompletionAndRevisions() {
        val row = first.copy(completedAt = "2026-10-05T12:00:00Z", revisionDate = "2026-10-07")
        val overview = batch.copy(lectures = listOf(row), studyPlan = BatchStudyPlan(day, "2026-11-01"))
        val events = overview.calendarEvents(BatchStudyMode.OFFICIAL)
        assertFalse(events.any { it.kind == BatchEventKind.PLANNED })
        assertFalse(events.any { it.milestone == BatchMilestone.PLAN_START || it.milestone == BatchMilestone.TARGET_FINISH })
        assertTrue(events.any { it.kind == BatchEventKind.CLASS && it.date == "2026-10-04" })
        assertTrue(events.any { it.kind == BatchEventKind.DONE && it.date == day })
        assertTrue(events.any { it.kind == BatchEventKind.REVISION })
        assertTrue(overview.calendarEvents(BatchStudyMode.PERSONAL).any { it.kind == BatchEventKind.PLANNED })
        assertEquals("2026-10-09", overview.lectures.single().studyPlannedFor)
    }

    @Test fun existingCustomDatesDefaultToOwnPaceAndNewStudentsDefaultToOfficial() {
        assertEquals(BatchStudyMode.PERSONAL, batch.defaultStudyMode())
        assertEquals(BatchStudyMode.OFFICIAL, batch.copy(lectures = listOf(third)).defaultStudyMode())
    }

    @Test fun officialDatesRollForwardWithoutChangingCompletionHistory() {
        assertEquals("e2", batch.nextStudyLecture("e", BatchStudyMode.OFFICIAL, day)?.id)
        assertEquals("e3", batch.nextStudyLecture("e", BatchStudyMode.OFFICIAL, "2026-10-06")?.id)
        assertNull(batch.nextStudyLecture("e", BatchStudyMode.OFFICIAL, "2026-10-07"))
        assertEquals("2026-10-09", batch.lectures.first().studyPlannedFor)
    }
}
