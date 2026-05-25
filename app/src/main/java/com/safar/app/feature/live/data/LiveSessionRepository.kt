package com.safar.app.feature.live.data

import com.safar.app.feature.live.model.LiveSession
import com.safar.app.feature.live.model.EndLiveSessionRequest
import com.safar.app.feature.live.model.StartLiveSessionRequest
import com.safar.app.feature.live.model.toDomain
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSessionRepository @Inject constructor(
    private val api: LiveSessionApi,
) : LiveSessionRepositoryContract {
    override suspend fun listByCourse(courseId: String, status: String?): Resource<List<LiveSession>> =
        safeApiCall { api.listLiveSessions(courseId = courseId, status = status) }
            .map { dto -> dto.liveSessions.orEmpty().map { it.toDomain() } }

    override suspend fun getById(id: String): Resource<LiveSession> =
        safeApiCall { api.getLiveSession(id) }
            .map { dto ->
                dto.liveSession?.toDomain()
                    ?: throw IllegalStateException("Live session payload missing")
            }

    override suspend fun startLiveSession(id: String, youtubeUrl: String): Resource<LiveSession> =
        safeApiCall { api.startLiveSession(id, StartLiveSessionRequest(youtubeUrl)) }
            .map { dto ->
                dto.liveSession?.toDomain()
                    ?: throw IllegalStateException("Live session payload missing")
            }

    override suspend fun endLiveSession(id: String, recordingVideoId: String?): Resource<LiveSession> =
        safeApiCall { api.endLiveSession(id, EndLiveSessionRequest(recordingVideoId)) }
            .map { dto ->
                dto.liveSession?.toDomain()
                    ?: throw IllegalStateException("Live session payload missing")
            }
}

interface LiveSessionRepositoryContract {
    suspend fun listByCourse(courseId: String, status: String?): Resource<List<LiveSession>>
    suspend fun getById(id: String): Resource<LiveSession>
    suspend fun startLiveSession(id: String, youtubeUrl: String): Resource<LiveSession>
    suspend fun endLiveSession(id: String, recordingVideoId: String?): Resource<LiveSession>
}

private fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error -> Resource.Error(message, code)
    is Resource.Loading -> Resource.Loading()
}
