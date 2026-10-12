package com.safarparmar.app.feature.toppersbatch

import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TodayPlanRemoveTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = mockk<ToppersBatchApi>()
    private val day = ToppersBatchViewModel.indiaDay()
    private val row = BatchLecture(id = "e1", subjectId = "english", subjectKey = "english", studyPlannedFor = day, planVersion = 2)
    private fun overview(lecture: BatchLecture = row) = BatchOverview(
        subjects = listOf(BatchSubject(id = "english", key = "english")), lectures = listOf(lecture))
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun removalUpdatesCountsAndRestoresTheSuggestionWithoutDeletingTheLecture() = runTest(dispatcher) {
        var server = row
        coEvery { api.overview() } coAnswers { Response.success(overview(server)) }
        coEvery { api.lectureAction(row.id, "plan", any()) } coAnswers {
            delay(100)
            server = server.copy(studyPlannedFor = null, studyReminderTime = null, planVersion = 3)
            Response.success(LectureResult(server))
        }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        val task = vm.state.value.overview!!.todayPlan(day).tasks.single()
        vm.removeTodayTask(task)
        vm.removeTodayTask(task)
        advanceUntilIdle()
        val result = vm.state.value.overview!!
        assertEquals(0, result.todayPlan(day).total)
        assertEquals(1, result.lectures.size)
        assertNull(result.lectures.single().completedAt)
        assertEquals(row.id, result.nextPlanningLectures(day)[row.subjectId]?.id)
        coVerify(exactly = 1) { api.lectureAction(row.id, "plan", mapOf("kind" to "watch", "remove" to true, "version" to 2)) }
    }

    @Test fun failedRemovalRetainsARecoverablePlan() = runTest(dispatcher) {
        coEvery { api.overview() } returns Response.success(overview())
        coEvery { api.lectureAction(row.id, "plan", any()) } throws IOException()
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        vm.removeTodayTask(vm.state.value.overview!!.todayPlan(day).tasks.single())
        advanceUntilIdle()
        assertEquals(1, vm.state.value.overview!!.todayPlan(day).total)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.busyIds.isEmpty())
    }

    @Test fun staleRemovalDoesNotClearAFuturePlanOrCompletedWatch() = runTest(dispatcher) {
        var server = row
        coEvery { api.overview() } coAnswers { Response.success(overview(server)) }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        val stale = vm.state.value.overview!!.todayPlan(day).tasks.single()
        server = row.copy(studyPlannedFor = java.time.LocalDate.parse(day).plusDays(1).toString())
        vm.refresh()
        advanceUntilIdle()
        vm.removeTodayTask(stale)
        server = row.copy(completedAt = "2026-10-05T12:00:00Z")
        vm.refresh()
        advanceUntilIdle()
        vm.removeTodayTask(stale)
        advanceUntilIdle()
        coVerify(exactly = 0) { api.lectureAction(any(), any(), any()) }
        assertEquals(server.completedAt, vm.state.value.overview!!.lectures.single().completedAt)
    }

    @Test fun revisionRemovalRetainsCompletedSessionsAndWatchHistory() = runTest(dispatcher) {
        val completed = ReviewSession(java.time.LocalDate.parse(day).minusDays(1).toString(), "2026-10-04T12:00:00Z")
        var server = row.copy(completedAt = "2026-10-03T12:00:00Z", revisionSessions = listOf(completed, ReviewSession(day)))
        coEvery { api.overview() } coAnswers { Response.success(overview(server)) }
        coEvery { api.lectureAction(row.id, "plan", any()) } coAnswers {
            server = server.copy(revisionSessions = listOf(completed), revisionDate = completed.date,
                revisionCompletedAt = completed.completedAt, planVersion = 3)
            Response.success(LectureResult(server))
        }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        vm.removeTodayTask(vm.state.value.overview!!.todayPlan(day).tasks.first { it.kind == "revision" })
        advanceUntilIdle()
        val result = vm.state.value.overview!!
        assertEquals(1, result.todayPlan(day).total) // Retained completed watch assignment.
        assertEquals(1, result.todayPlan(day).completed)
        assertEquals(listOf(completed), result.lectures.single().sessions)
        assertNotNull(result.lectures.single().completedAt)
        coVerify(exactly = 1) { api.lectureAction(row.id, "plan", mapOf("kind" to "revision", "remove" to true, "version" to 2)) }
    }
}
