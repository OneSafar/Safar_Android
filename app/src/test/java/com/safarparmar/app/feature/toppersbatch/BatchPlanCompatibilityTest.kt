package com.safarparmar.app.feature.toppersbatch

import com.safarparmar.app.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class BatchPlanCompatibilityTest {
    private val api = mockk<ToppersBatchApi>()
    private val row = BatchLecture(id = "lecture", studyPlannedFor = "2026-10-05")
    private val body = mapOf<String, Any>("kind" to "watch", "dates" to listOf("2026-10-05"), "version" to 2)
    private fun error(code: Int, message: String) = Response.error<LectureResult>(code,
        """{"message":"$message"}""".toResponseBody("application/json".toMediaType()))

    @Test fun olderServerUsesStudyDateAndCachesUnsupportedPlanAction() = runBlocking {
        coEvery { api.lectureAction(row.id, "plan", any()) } returns error(404, "This option is not available")
        coEvery { api.lectureAction(row.id, "study-date", any()) } returns Response.success(LectureResult(row))
        val repository = ToppersBatchRepository(api)
        repeat(2) {
            val result = repository.savePlan(row.id, body)
            assertEquals(row, (result as Resource.Success).data.lecture)
        }
        coVerify(exactly = 1) { api.lectureAction(row.id, "plan", body) }
        coVerify(exactly = 2) { api.lectureAction(row.id, "study-date", mapOf("date" to "2026-10-05")) }
    }

    @Test fun unsupportedRemindersAreNotSilentlyDiscardedAndPlainDateCanBeRetried() = runBlocking {
        coEvery { api.lectureAction(row.id, "plan", any()) } returns error(404, "This option is not available")
        coEvery { api.lectureAction(row.id, "study-date", any()) } returns Response.success(LectureResult(row))
        val repository = ToppersBatchRepository(api)
        val result = repository.savePlan(row.id, body + ("reminderTime" to "19:00"))
        assertTrue(result is Resource.Error)
        assertTrue((result as Resource.Error).message.contains("Turn off Remind me"))
        coVerify(exactly = 0) { api.lectureAction(row.id, "study-date", any()) }
        assertTrue(repository.savePlan(row.id, body) is Resource.Success)
    }

    @Test fun conflictsAndOther404ResponsesNeverDowngradeToAnUnversionedSave() = runBlocking {
        for ((code, message) in listOf(409 to "Plan changed", 404 to "Lecture not found", 403 to "Access denied")) {
            coEvery { api.lectureAction(row.id, "plan", any()) } returns error(code, message)
            assertTrue(ToppersBatchRepository(api).savePlan(row.id, body) is Resource.Error)
        }
        coVerify(exactly = 0) { api.lectureAction(row.id, "study-date", any()) }
    }

    @Test fun removingAWatchDateUsesTheOlderDeleteEndpoint() = runBlocking {
        coEvery { api.lectureAction(row.id, "plan", any()) } returns error(404, "This option is not available")
        coEvery { api.removeLectureAction(row.id, "study-date") } returns Response.success(LectureResult(row.copy(studyPlannedFor = null)))
        val result = ToppersBatchRepository(api).savePlan(row.id, mapOf("kind" to "watch", "remove" to true, "version" to 2))
        assertNull((result as Resource.Success).data.lecture.studyPlannedFor)
    }

    @Test fun unsupportedRevisionAndMultipleWatchDatesDoNotSavePartialPlans() = runBlocking {
        coEvery { api.lectureAction(row.id, "plan", any()) } returns error(404, "This option is not available")
        val repository = ToppersBatchRepository(api)
        assertTrue(repository.savePlan(row.id, body + ("kind" to "revision")) is Resource.Error)
        assertTrue(repository.savePlan(row.id, body + ("dates" to listOf("2026-10-05", "2026-10-06"))) is Resource.Error)
        coVerify(exactly = 0) { api.lectureAction(row.id, "study-date", any()) }
    }
}
