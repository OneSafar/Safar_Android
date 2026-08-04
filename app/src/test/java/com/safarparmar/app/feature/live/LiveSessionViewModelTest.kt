package com.safarparmar.app.feature.live

import app.cash.turbine.test
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.socket.MehfilSocketManager
import com.safarparmar.app.feature.live.data.LiveSessionRepositoryContract
import com.safarparmar.app.feature.live.model.LiveSession
import com.safarparmar.app.feature.live.presentation.LiveSessionViewModel
import com.safarparmar.app.ui.auth.MainDispatcherRule
import com.safarparmar.app.util.Resource
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LiveSessionViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `sessions success state`() = runTest {
        val vm = LiveSessionViewModel(
            repository = FakeRepo(Resource.Success(listOf(sampleSession()))),
            socketManager = mockk(relaxed = true),
            dataStore = mockk(relaxed = true),
            socketConnector = mockk(relaxed = true),
        )
        vm.liveSessionsState.test {
            assertTrue(awaitItem().isLoading)
            vm.loadSessions("course-1", "live")
            val next = awaitItem()
            assertFalse(next.isLoading)
            assertEquals(1, next.sessions.size)
        }
    }

    @Test
    fun `sessions error state`() = runTest {
        val vm = LiveSessionViewModel(
            repository = FakeRepo(Resource.Error("forbidden", 403)),
            socketManager = mockk(relaxed = true),
            dataStore = mockk(relaxed = true),
            socketConnector = mockk(relaxed = true),
        )
        vm.liveSessionsState.test {
            awaitItem()
            vm.loadSessions("course-1", "live")
            val next = awaitItem()
            assertEquals("forbidden", next.errorMessage)
        }
    }
}

private class FakeRepo(
    private val listResult: Resource<List<LiveSession>>,
) : LiveSessionRepositoryContract {
    override suspend fun listByCourse(courseId: String, status: String?) = listResult
    override suspend fun getById(id: String): Resource<LiveSession> = Resource.Success(sampleSession())
}

private fun sampleSession() = LiveSession(
    id = "live-1",
    title = "Biology Live",
    description = "Cell chapter",
    courseId = "course-1",
    teacherId = "teacher-1",
    scheduledStartAt = "2026-06-01T10:00:00.000Z",
    scheduledEndAt = "2026-06-01T11:00:00.000Z",
    status = "live",
    youtubeVideoId = "dQw4w9WgXcQ",
    youtubeWatchUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    youtubeEmbedUrl = "https://www.youtube.com/embed/dQw4w9WgXcQ?enablejsapi=1&playsinline=1&rel=0",
    thumbnailUrl = null,
    isChatEnabled = true,
    isRecordingAvailable = false,
    recordingVideoId = null,
    resources = emptyList(),
    canManage = false,
)
