package com.safarparmar.app.feature.toppersbatch

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BatchSubjectColourSaveTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = mockk<ToppersBatchApi>()
    private val subject = BatchSubject(id = "english", key = "english", name = "English", color = "#E11D48", defaultColor = "#E11D48")
    private val original = BatchOverview(subjects = listOf(subject))
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun refreshStartedDuringSaveCannotRestoreTheOldCardColour() = runTest(dispatcher) {
        var reads = 0
        coEvery { api.overview() } coAnswers {
            if (reads++ > 0) delay(100)
            Response.success(original)
        }
        coEvery { api.editSubject(subject.id, any()) } coAnswers {
            delay(50)
            Response.success(subject.copy(color = "#1D4ED8"))
        }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        advanceUntilIdle()
        vm.subjectColor(subject, "#1D4ED8")
        runCurrent()
        vm.refresh()
        advanceUntilIdle()
        assertEquals("#1D4ED8", vm.state.value.overview!!.subjects.first().color)
    }
    @Test fun appliesTheColourConfirmedByTheBackend() = runTest(dispatcher) {
        coEvery { api.overview() } returns Response.success(original)
        coEvery { api.editSubject(subject.id, any()) } returns Response.success(subject.copy(color = "#1D4ED8"))
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        advanceUntilIdle()
        vm.subjectColor(subject, "#1d4ed8")
        advanceUntilIdle()
        assertEquals("#1D4ED8", vm.state.value.overview!!.subjects.first().color)
    }

    @Test fun resettingToDefaultAlsoRejectsAnOlderRefresh() = runTest(dispatcher) {
        var reads = 0
        coEvery { api.overview() } coAnswers {
            if (reads++ > 0) delay(100)
            Response.success(original)
        }
        coEvery { api.editSubject(subject.id, any()) } coAnswers {
            delay(50)
            Response.success(subject.copy(color = null))
        }
        val vm = ToppersBatchViewModel(ToppersBatchRepository(api))
        advanceUntilIdle()
        vm.subjectColor(subject, null)
        runCurrent()
        vm.refresh()
        advanceUntilIdle()
        assertEquals(null, vm.state.value.overview!!.subjects.first().color)
        assertEquals(subject.defaultColor, vm.state.value.overview!!.subjects.first().defaultColor)
    }

}
