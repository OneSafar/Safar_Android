package com.safarparmar.app.feature.live.data

import com.safarparmar.app.feature.live.model.LiveSession
import com.safarparmar.app.feature.live.model.toDomain
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSessionRepository @Inject constructor(
    private val api: LiveSessionApi,
) : LiveSessionRepositoryContract {
    override suspend fun listByCourse(courseId: String, status: String?): Resource<List<LiveSession>> =
        safeApiCall {
            api.listLiveSessions(
                courseId = courseId.takeIf { it.isNotBlank() },
                status = status,
            )
        }
            .map { dto -> dto.liveSessions.orEmpty().map { it.toDomain() } }

    override suspend fun getById(id: String): Resource<LiveSession> =
        safeApiCall { api.getLiveSession(id) }
            .map { dto ->
                dto.liveSession?.toDomain()
                    ?: throw IllegalStateException("Live session payload missing")
            }
}

interface LiveSessionRepositoryContract {
    suspend fun listByCourse(courseId: String, status: String?): Resource<List<LiveSession>>
    suspend fun getById(id: String): Resource<LiveSession>
}

private fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> {
        try {
            Resource.Success(transform(data))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Mapping error", 500)
        }
    }
    is Resource.Error -> Resource.Error(message, code)
    is Resource.Loading -> Resource.Loading()
}

