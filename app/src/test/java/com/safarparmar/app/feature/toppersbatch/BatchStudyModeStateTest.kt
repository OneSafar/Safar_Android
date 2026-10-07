package com.safarparmar.app.feature.toppersbatch

import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BatchStudyModeStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = mockk<ToppersBatchApi>()
    private val day = ToppersBatchViewModel.indiaDay()
    private val row = BatchLecture(id = "e1", subjectId = "e", subjectKey = "english", scheduledFor = day)
    private val batch = BatchOverview(subjects = listOf(BatchSubject(id = "e", key = "english")), lectures = listOf(row))
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun switchingRetainsPlansAndCompletionAndDoesNotMutateLecturesOnTheServer() = runTest(dispatcher) {
        val overview = batch.copy(lectures = listOf(row.copy(studyPlannedFor = day, completedAt = "2026-10-05T12:00:00Z")))
        coEvery { api.overview() } returns Response.success(overview)
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        advanceUntilIdle()
        assertEquals(BatchStudyMode.PERSONAL, vm.state.value.studyMode)
        vm.selectStudyMode(BatchStudyMode.OFFICIAL)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(BatchStudyMode.OFFICIAL, vm.state.value.studyMode)
        assertEquals(overview.lectures, vm.state.value.overview!!.lectures)
        vm.selectStudyMode(BatchStudyMode.PERSONAL)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.overview!!.todayPlan(day).total)
        assertEquals(1, vm.state.value.overview!!.todayPlan(day).completed)
        coVerify(exactly = 0) { api.lectureAction(any(), any(), any()) }
    }

    @Test fun savedModeRestoresAcrossReopeningAndOverridesMigrationDefault() = runTest(dispatcher) {
        val cache = mockk<BatchOverviewCache>(relaxed = true)
        var saved: BatchStudyMode? = null
        coEvery { cache.account() } returns "account-a"
        coEvery { cache.load() } returns null
        coEvery { cache.studyMode() } coAnswers { saved }
        coEvery { cache.saveStudyMode(any()) } coAnswers { saved = firstArg() }
        coEvery { api.overview() } returns Response.success(batch.copy(lectures = listOf(row.copy(studyPlannedFor = day))))
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api, cache = cache))
        advanceUntilIdle()
        vm.selectStudyMode(BatchStudyMode.OFFICIAL)
        advanceUntilIdle()
        val reopened = ToppersBatchViewModel(ToppersBatchRepository(api, cache = cache))
        advanceUntilIdle()
        assertEquals(BatchStudyMode.OFFICIAL, reopened.state.value.studyMode)
        assertEquals(day, reopened.state.value.overview!!.lectures.single().studyPlannedFor)
        coVerify(exactly = 1) { cache.saveStudyMode(BatchStudyMode.OFFICIAL) }
    }

    @Test fun rapidModeChangesPersistTheLatestChoice() = runTest(dispatcher) {
        val cache = mockk<BatchOverviewCache>(relaxed = true)
        var saved: BatchStudyMode? = null
        coEvery { cache.account() } returns "account-a"
        coEvery { cache.load() } returns null
        coEvery { cache.studyMode() } coAnswers { saved }
        coEvery { cache.saveStudyMode(any()) } coAnswers {
            val choice = firstArg<BatchStudyMode>()
            if (choice == BatchStudyMode.PERSONAL) kotlinx.coroutines.delay(100)
            saved = choice
        }
        coEvery { api.overview() } returns Response.success(batch)
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api, cache = cache))
        advanceUntilIdle()
        vm.selectStudyMode(BatchStudyMode.PERSONAL)
        runCurrent()
        vm.selectStudyMode(BatchStudyMode.OFFICIAL)
        advanceUntilIdle()
        assertEquals(BatchStudyMode.OFFICIAL, saved)
        assertEquals(BatchStudyMode.OFFICIAL, vm.state.value.studyMode)
    }

    @Test fun newStudentUsesOfficialNextAndQuickAddRequiresOwnPace() = runTest(dispatcher) {
        coEvery { api.overview() } returns Response.success(batch)
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        advanceUntilIdle()
        assertEquals(BatchStudyMode.OFFICIAL, vm.state.value.studyMode)
        vm.focusTodaySubject("e")
        assertEquals("e1", vm.state.value.focusedLectureId)
        vm.addToToday(row)
        advanceUntilIdle()
        coVerify(exactly = 0) { api.lectureAction(any(), any(), any()) }
        vm.selectStudyMode(BatchStudyMode.PERSONAL)
        vm.focusTodaySubject("e")
        assertNull(vm.state.value.focusedLectureId)
    }
}
