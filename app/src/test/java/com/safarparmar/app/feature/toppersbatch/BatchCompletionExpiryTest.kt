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
class BatchCompletionExpiryTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun bannerExpiresWithoutDismissingANewerCompletion() = runTest(dispatcher) {
        val api = mockk<ToppersBatchApi>()
        val first = BatchLecture(id = "first", subjectId = "english", subjectKey = "english", displayTopic = "Nouns")
        val second = first.copy(id = "second", displayTopic = "Verbs")
        coEvery { api.overview() } returns Response.success(BatchOverview(
            subjects = listOf(BatchSubject(id = "english", key = "english")), lectures = listOf(first, second)))
        coEvery { api.lectureAction(any(), "complete", any()) } coAnswers {
            val row = if (firstArg<String>() == first.id) first else second
            Response.success(LectureResult(row.copy(completedAt = "2026-10-05T12:00:00Z")))
        }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        runCurrent()
        vm.toggleDone(first)
        runCurrent()
        assertEquals(first.id, vm.state.value.completionFeedback?.lectureId)
        advanceTimeBy(3000)
        runCurrent()
        vm.toggleDone(second)
        runCurrent()
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(second.id, vm.state.value.completionFeedback?.lectureId)
        advanceTimeBy(7000)
        runCurrent()
        assertNull(vm.state.value.completionFeedback)
        assertEquals(2, vm.state.value.overview!!.progress.completed)
    }
}
