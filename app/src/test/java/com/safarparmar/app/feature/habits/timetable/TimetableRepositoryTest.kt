package com.safarparmar.app.feature.habits.timetable

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate

class TimetableRepositoryTest {
    @Test fun repeatRulesRespectStartDateWeekendsAndWeekday() {
        val monday = LocalDate.of(2026, 10, 26)
        val session = TimetableSession(title = "Physics", date = monday.toString())
        assertTrue(session.occursOn(monday))
        assertFalse(session.occursOn(monday.plusDays(1)))
        SessionRepeat.entries.forEach { repeat -> assertFalse(session.copy(repeat = repeat).occursOn(monday.minusDays(1))) }
        assertTrue(session.copy(repeat = SessionRepeat.DAILY).occursOn(monday.plusDays(6)))
        assertFalse(session.copy(repeat = SessionRepeat.WEEKDAYS).occursOn(monday.plusDays(5)))
        assertTrue(session.copy(repeat = SessionRepeat.WEEKDAYS).occursOn(monday.plusDays(4)))
        assertTrue(session.copy(repeat = SessionRepeat.WEEKLY).occursOn(monday.plusWeeks(4)))
        assertFalse(session.copy(repeat = SessionRepeat.WEEKLY).occursOn(monday.plusDays(8)))
    }

    @Test fun remindersHandleMidnightFarFutureAndDstWallTime() {
        val india = java.time.ZoneId.of("Asia/Kolkata")
        val evening = java.time.ZonedDateTime.of(2026, 10, 29, 23, 50, 0, 0, india)
        val early = TimetableSession(title = "Read", date = "2026-10-30", startMinute = 5, endMinute = 35, reminderMinutes = 10)
        val next = nextTimetableReminder(early, evening)!!
        assertEquals(LocalDate.of(2026, 10, 30), next.first)
        assertEquals(evening.plusMinutes(5), next.second)
        assertNull(nextTimetableReminder(early, evening.plusHours(1)))
        assertEquals(LocalDate.of(2030, 1, 1), nextTimetableReminder(early.copy(date = "2030-01-01"), evening)!!.first)
        val ny = java.time.ZoneId.of("America/New_York")
        val dstMorning = java.time.ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, ny)
        val study = early.copy(date = "2026-03-08", startMinute = 540, endMinute = 600, reminderMinutes = 0)
        assertEquals(9, nextTimetableReminder(study, dstMorning)!!.second.hour)
        assertEquals(java.time.ZoneOffset.ofHours(-4), nextTimetableReminder(study, dstMorning)!!.second.offset)
    }

    @Test fun rejectsInvalidOrOvernightSessions() {
        listOf(TimetableSession(title = ""), TimetableSession(title = "Study", startMinute = 600, endMinute = 600),
            TimetableSession(title = "Study", startMinute = 1380, endMinute = 60), TimetableSession(title = "Study", color = 5),
            TimetableSession(title = "Study", reminderMinutes = 7)).forEach {
            assertTrue(runCatching { it.validate() }.isFailure)
        }
        TimetableSession(title = "Study", startMinute = 0, endMinute = 1439).validate()
    }

    @Test fun accountIsolationCompletionUndoEditAndDiskReload() = runBlocking {
        val directory = Files.createTempDirectory("timetable-test").toFile()
        val file = directory.resolve("test.preferences_pb")
        val owner = MutableStateFlow<String?>("student-a")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { file }
        val scheduled = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        val repository = TimetableRepository(store, owner, { user, _ -> scheduled += user }, { _, id -> cancelled += id })
        val today = LocalDate.now()
        val session = TimetableSession(title = "Physics", date = today.toString(), trackAsHabit = true)
        try {
            repository.save(session)
            repository.toggle(session.id, today)
            assertTrue(repository.sessions.first().single().isDone(today))
            // A stale editor must not overwrite completion changes.
            repository.save(session.copy(title = "Physics revision"))
            assertTrue(repository.sessions.first().single().isDone(today))
            repository.toggle(session.id, today)
            assertFalse(repository.sessions.first().single().isDone(today))
            assertTrue(runCatching { repository.toggle(session.id, today.plusDays(1)) }.isFailure)
            owner.value = "student-b"
            assertTrue(repository.sessions.first().isEmpty())
            repository.save(session.copy(id = "b", title = "Maths"))
            owner.value = null
            assertTrue(repository.sessions.first().isEmpty())
            assertTrue(runCatching { repository.save(session) }.isFailure)
            owner.value = "student-a"
            assertEquals("Physics revision", repository.sessions.first().single().title)
            scope.cancel(); scope.coroutineContext[Job]!!.join()
            val nextScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val reopened = TimetableRepository(PreferenceDataStoreFactory.create(scope = nextScope) { file }, owner, { _, _ -> }, { _, _ -> })
                assertEquals("Physics revision", reopened.sessions.first().single().title)
                reopened.delete(session.id)
                assertTrue(reopened.sessions.first().isEmpty())
                owner.value = "student-b"
                assertEquals("Maths", reopened.sessions.first().single().title)
            } finally { nextScope.cancel(); nextScope.coroutineContext[Job]!!.join() }
            assertEquals(listOf("student-a", "student-a", "student-b"), scheduled)
        } finally { scope.cancel(); directory.deleteRecursively() }
    }
}
