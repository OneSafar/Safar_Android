package com.safarparmar.app.feature.toppersbatch

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class TodayPlanSaveTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = mockk<ToppersBatchApi>()
    private val day = ToppersBatchViewModel.indiaDay()
    private val lecture = BatchLecture(id = "lecture", subjectId = "english", subjectKey = "english", planVersion = 2)
    private fun overview(row: BatchLecture = lecture) = BatchOverview(
        subjects = listOf(BatchSubject(id = "english", key = "english", name = "English")), lectures = listOf(row))
    private val body get() = mapOf<String, Any>("kind" to "watch", "dates" to listOf(day))
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun confirmedSaveIsVisibleBeforeDismissEvenIfRefreshFails() = runTest(dispatcher) {
        var loads = 0
        coEvery { api.overview() } coAnswers { if (loads++ == 0) Response.success(overview()) else throw IOException() }
        val saved = lecture.copy(studyPlannedFor = day, planVersion = 3)
        coEvery { api.lectureAction(lecture.id, "plan", any()) } returns Response.success(LectureResult(saved))
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        var visibleAtDismiss = false
        vm.savePlan(lecture, body) { visibleAtDismiss = vm.state.value.overview!!.todayPlan(day).total == 1 }
        advanceUntilIdle()
        assertTrue(visibleAtDismiss)
        assertEquals(saved, vm.state.value.overview!!.lectures.single())
        assertNotNull(vm.state.value.error)
        assertEquals(BatchNotice(com.safarparmar.app.R.string.toppers_batch_lecture_planned_for_count, BatchDateArgument(day)), vm.state.value.message)
        coVerify(exactly = 1) { api.lectureAction(lecture.id, "plan", body + ("version" to 2)) }
    }

    @Test fun repeatedSaveTapsSendOnlyOneMutation() = runTest(dispatcher) {
        coEvery { api.overview() } returns Response.success(overview())
        coEvery { api.lectureAction(lecture.id, "plan", any()) } coAnswers {
            delay(1000)
            Response.success(LectureResult(lecture.copy(studyPlannedFor = day, planVersion = 3)))
        }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        var dismissals = 0
        vm.savePlan(lecture, body) { dismissals++ }
        vm.savePlan(lecture, body) { dismissals++ }
        assertTrue(lecture.id in vm.state.value.busyIds)
        advanceUntilIdle()
        assertEquals(1, dismissals)
        assertTrue(vm.state.value.busyIds.isEmpty())
        coVerify(exactly = 1) { api.lectureAction(lecture.id, "plan", any()) }
    }

    @Test fun failedSaveKeepsTheDialogOpenAndDoesNotInventAPlan() = runTest(dispatcher) {
        coEvery { api.overview() } returns Response.success(overview())
        coEvery { api.lectureAction(lecture.id, "plan", any()) } throws IOException()
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        var dismissed = false
        vm.savePlan(lecture, body) { dismissed = true }
        advanceUntilIdle()
        assertFalse(dismissed)
        assertEquals(0, vm.state.value.overview!!.todayPlan(day).total)
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.busyIds.isEmpty())
    }

    @Test fun conflictRefreshesTheVersionAndRetryUsesTheLatestLecture() = runTest(dispatcher) {
        var loads = 0
        coEvery { api.overview() } coAnswers { Response.success(overview(if (loads++ == 0) lecture else lecture.copy(planVersion = 4))) }
        coEvery { api.lectureAction(lecture.id, "plan", any()) } returns Response.error(409,
            """{"code":"PLAN_CONFLICT","message":"Plan changed. Refresh and try again."}""".toResponseBody("application/json".toMediaType()))
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        var dismissed = false
        vm.savePlan(lecture, body) { dismissed = true }
        advanceUntilIdle()
        assertFalse(dismissed)
        assertEquals(4, vm.state.value.overview!!.lectures.single().planVersion)
        coEvery { api.lectureAction(lecture.id, "plan", any()) } returns Response.success(LectureResult(lecture.copy(studyPlannedFor = day, planVersion = 5)))
        vm.savePlan(vm.state.value.overview!!.lectures.single(), body) { dismissed = true }
        advanceUntilIdle()
        assertTrue(dismissed)
        coVerify(exactly = 1) { api.lectureAction(lecture.id, "plan", body + ("version" to 4)) }
    }

    @Test fun oneTapAddsTodayAndAStaleSecondTapDoesNotResave() = runTest(dispatcher) {
        var server = lecture
        coEvery { api.overview() } coAnswers { Response.success(overview(server)) }
        coEvery { api.lectureAction(lecture.id, "plan", any()) } coAnswers {
            server = server.copy(studyPlannedFor = day, planVersion = 3)
            Response.success(LectureResult(server))
        }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        vm.selectStudyMode(BatchStudyMode.PERSONAL)
        vm.addToToday(lecture)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.overview!!.todayPlan(day).total)
        vm.addToToday(lecture)
        advanceUntilIdle()
        coVerify(exactly = 1) { api.lectureAction(lecture.id, "plan", body + ("version" to 2)) }
    }

    @Test fun quickAddPreservesFuturePlansAndIgnoresDoneAndLockedLectures() = runTest(dispatcher) {
        val rows = listOf(lecture.copy(studyPlannedFor = java.time.LocalDate.parse(day).plusDays(1).toString()),
            lecture.copy(id = "done", completedAt = "saved"), lecture.copy(id = "locked", isAvailable = false))
        coEvery { api.overview() } returns Response.success(overview().copy(lectures = rows))
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        vm.selectStudyMode(BatchStudyMode.PERSONAL)
        rows.forEach(vm::addToToday)
        advanceUntilIdle()
        coVerify(exactly = 0) { api.lectureAction(any(), any(), any()) }
    }
}
