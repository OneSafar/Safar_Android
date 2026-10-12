package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BatchOpeningRequestsTest {
    private val api = mockk<ToppersBatchApi>()
    private fun denied(code: String) = Response.error<BatchOverview>(403,
        """{"code":"$code","message":"Unavailable"}""".toResponseBody("application/json".toMediaType()))
    @Test fun existingStudentNeedsOnlyOneOverviewRequest() = runBlocking {
        coEvery { api.overview() } returns Response.success(BatchOverview())
        assertTrue(ToppersBatchRepository(api).open() is Resource.Success)
        coVerify(exactly = 1) { api.overview() }
        coVerify(exactly = 0) { api.status() }
        coVerify(exactly = 0) { api.activate() }
        coVerify(exactly = 0) { api.today(any()) }
    }
    @Test fun missingEnrollmentActivatesOnce() = runBlocking {
        coEvery { api.overview() } returns denied("PLANNER_V2_NOT_ENABLED")
        coEvery { api.activate() } returns Response.success(BatchOverview())
        assertTrue(ToppersBatchRepository(api).open() is Resource.Success)
        coVerify(exactly = 1) { api.activate() }
        coVerify(exactly = 0) { api.status() }
    }
    @Test fun accessDenialDoesNotAttemptActivation() = runBlocking {
        coEvery { api.overview() } returns denied("PREMIUM_REQUIRED")
        assertTrue(ToppersBatchRepository(api).open() is Resource.Error)
        coVerify(exactly = 0) { api.activate() }
    }
}
