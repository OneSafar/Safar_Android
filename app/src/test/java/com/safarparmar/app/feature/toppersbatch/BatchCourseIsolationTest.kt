package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.util.Resource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceUntilIdle
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BatchCourseIsolationTest {
    @Test fun navigationArgumentSelectsTheGkRepository() = runTest {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        try {
            val toppersApi = mockk<ToppersBatchApi>()
            val gkApi = mockk<ToppersBatchApi>()
            val factory = mockk<BatchApiFactory>()
            io.mockk.every { factory.forCourse(GK5_COURSE_ID) } returns gkApi
            coEvery { gkApi.overview() } returns Response.success(BatchOverview(course = BatchCourse(id = GK5_COURSE_ID)))
            val vm = ToppersBatchViewModel(ToppersBatchRepository(toppersApi, factory = factory),
                androidx.lifecycle.SavedStateHandle(mapOf("batch" to "gk-5")))
            advanceUntilIdle()
            assertTrue(vm.isGk5)
            assertEquals(GK5_COURSE_ID, vm.state.value.overview!!.course.id)
            io.mockk.coVerify(exactly = 0) { toppersApi.overview() }
        } finally { kotlinx.coroutines.Dispatchers.resetMain() }
    }

    @Test fun batchApisUseDistinctPathsForReadsAndMutations() = runTest {
        val paths = mutableListOf<String>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            paths += chain.request().url.encodedPath
            okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("{}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val factory = BatchApiFactory(Retrofit.Builder().baseUrl("https://example.test/api/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build())
        val toppers = factory.forCourse(TOPPERS_COURSE_ID)
        val gk = factory.forCourse(GK5_COURSE_ID)
        toppers.overview()
        gk.overview()
        gk.lectureAction("gk1", "complete")
        toppers.lectureAction("top1", "complete")
        assertEquals(listOf("/api/plans/toppers-batch/v2/", "/api/plans/gk-5/v2/",
            "/api/plans/gk-5/v2/lectures/gk1/complete", "/api/plans/toppers-batch/v2/lectures/top1/complete"), paths)
    }
    @Test fun gkRepositoryRejectsAnOverviewForAnotherCourse() = runTest {
        val api = mockk<ToppersBatchApi>()
        val factory = mockk<BatchApiFactory>()
        io.mockk.every { factory.forCourse(GK5_COURSE_ID) } returns api
        val repository = ToppersBatchRepository(api, factory = factory).forCourse(GK5_COURSE_ID)
        coEvery { api.overview() } returns Response.success(BatchOverview(course = BatchCourse(id = TOPPERS_COURSE_ID)))
        assertTrue(repository.overview() is Resource.Error)
        coEvery { api.overview() } returns Response.success(BatchOverview(course = BatchCourse(id = GK5_COURSE_ID)))
        assertTrue(repository.overview() is Resource.Success)
    }
}
