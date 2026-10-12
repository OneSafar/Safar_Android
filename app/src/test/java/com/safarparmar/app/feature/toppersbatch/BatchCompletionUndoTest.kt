package com.safarparmar.app.feature.toppersbatch

import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BatchCompletionUndoTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = mockk<ToppersBatchApi>()
    private val day = ToppersBatchViewModel.indiaDay()
    private val row = BatchLecture(id = "e1", subjectId = "english", subjectKey = "english", studyPlannedFor = day)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun vm(): ToppersBatchViewModel {
        coEvery { api.overview() } returns Response.success(BatchOverview(
            subjects = listOf(BatchSubject(id = "english", key = "english")), lectures = listOf(row)))
        coEvery { api.lectureAction(row.id, "complete", any()) } returns Response.success(LectureResult(row.copy(completedAt = "2026-10-05T12:00:00Z")))
        return ToppersBatchViewModel(ToppersBatchRepository(api))
    }
    @Test fun bannerUndoRestoresPendingTaskAndKeepsItsPlannedDate() = runTest(dispatcher) {
        val vm = vm()
        coEvery { api.lectureAction(row.id, "uncomplete", any()) } returns Response.success(LectureResult(row))
        runCurrent()
        vm.toggleDone(row)
        runCurrent()
        val feedback = vm.state.value.completionFeedback!!
        vm.undoCompletion(feedback)
        vm.undoCompletion(feedback)
        runCurrent()
        assertNull(vm.state.value.overview!!.lectures.single().completedAt)
        assertEquals(day, vm.state.value.overview!!.lectures.single().studyPlannedFor)
        assertEquals(1, vm.state.value.overview!!.todayPlan(day).total)
        assertEquals(0, vm.state.value.overview!!.todayPlan(day).completed)
        coVerify(exactly = 1) { api.lectureAction(row.id, "uncomplete", any()) }
        advanceUntilIdle()
    }
    @Test fun failedUndoPreservesCompletionAndAllowsRetry() = runTest(dispatcher) {
        val vm = vm()
        coEvery { api.lectureAction(row.id, "uncomplete", any()) } throws IOException()
        runCurrent()
        vm.toggleDone(row)
        runCurrent()
        val feedback = vm.state.value.completionFeedback!!
        vm.undoCompletion(feedback)
        runCurrent()
        assertNotNull(vm.state.value.overview!!.lectures.single().completedAt)
        assertNotNull(vm.state.value.error)
        assertEquals(feedback, vm.state.value.completionFeedback)
        coEvery { api.lectureAction(row.id, "uncomplete", any()) } returns Response.success(LectureResult(row))
        vm.undoCompletion(feedback)
        runCurrent()
        assertNull(vm.state.value.overview!!.lectures.single().completedAt)
        advanceUntilIdle()
    }
    @Test fun obsoleteBannerDoesNotUndoANewerCompletion() = runTest(dispatcher) {
        val vm = vm()
        runCurrent()
        vm.toggleDone(row)
        runCurrent()
        vm.undoCompletion(vm.state.value.completionFeedback!!.copy(event = -1))
        runCurrent()
        coVerify(exactly = 0) { api.lectureAction(row.id, "uncomplete", any()) }
        assertNotNull(vm.state.value.overview!!.lectures.single().completedAt)
        advanceUntilIdle()
    }
}
