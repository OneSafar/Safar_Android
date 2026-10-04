package com.safarparmar.app.feature.toppersbatch

import com.google.gson.Gson
import com.safarparmar.app.util.Resource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class ToppersBatchWireTest {
    @Test fun decodesStatusAndNestedSyllabusUsingServerFieldNames() {
        val gson = Gson()
        val status = gson.fromJson("{\"available\":true,\"enabled\":true}", BatchStatus::class.java)
        assertTrue(status.available)
        assertTrue(status.enabled)
        val batch = gson.fromJson("""{
            "subjects":[{"id":"english","key":"english","name":"English"}],
            "lectures":[{"id":"lecture-1","subjectId":"english","subjectKey":"english","displayTopic":"Nouns"}],
            "progress":{"bySubject":[{"subjectId":"english","completed":0,"total":1}]},
            "watchList":[{"subjectId":"english","nextLectureId":"lecture-1"}]
        }""", BatchOverview::class.java).officialOnly()
        assertEquals("Nouns", batch.lectures.single().displayTopic)
        assertEquals(1, batch.progress.total)
        assertEquals("lecture-1", batch.watchList.single().nextLectureId)
        assertTrue(batch.chapters.isEmpty())
    }

    @Test fun invalidOverviewShowsAnErrorInsteadOfThrowingDuringOpening() = runTest {
        val api = mockk<ToppersBatchApi>()
        val invalid = Gson().fromJson("{\"subjects\":null,\"progress\":null}", BatchOverview::class.java)
        coEvery { api.overview() } returns Response.success(invalid)
        assertTrue(ToppersBatchRepository(api, mockk()).overview() is Resource.Error)
    }
}
