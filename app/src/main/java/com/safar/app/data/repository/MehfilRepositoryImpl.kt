package com.safar.app.data.repository

import com.safar.app.data.remote.api.MehfilApi
import com.safar.app.data.remote.api.ThoughtsApi
import com.safar.app.data.remote.dto.*
import com.safar.app.domain.model.*
import com.safar.app.domain.repository.MehfilRepository
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MehfilRepositoryImpl @Inject constructor(
    private val mehfilApi: MehfilApi,
    private val thoughtsApi: ThoughtsApi,
) : MehfilRepository {

    override suspend fun getSandesh(): Resource<Pair<Sandesh?, List<Sandesh>>> {
        val r = safeApiCall { mehfilApi.getSandesh() }
        return when (r) {
            is Resource.Success -> Resource.Success(Pair(r.data.sandesh?.toDomain(), r.data.sandeshes?.map { it.toDomain() } ?: emptyList()))
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun reactSandesh(id: String): Resource<Unit> {
        val r = safeApiCall { mehfilApi.reactSandesh(id) }
        return when (r) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getSandeshComments(id: String, page: Int): Resource<List<Comment>> {
        val r = safeApiCall { mehfilApi.getSandeshComments(id, page) }
        return when (r) {
            is Resource.Success -> Resource.Success(r.data.comments?.map { it.toDomain() } ?: emptyList())
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun postSandeshComment(id: String, content: String): Resource<Unit> {
        val r = safeApiCall { mehfilApi.postSandeshComment(id, CommentRequest(content = content)) }
        return when (r) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getComments(thoughtId: String, page: Int): Resource<List<Comment>> {
        val r = safeApiCall { thoughtsApi.getComments(thoughtId, page) }
        return when (r) {
            is Resource.Success -> Resource.Success(r.data.comments?.map { it.toDomain() } ?: emptyList())
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun postComment(thoughtId: String, content: String): Resource<Unit> {
        val r = safeApiCall { thoughtsApi.postComment(CommentRequest(thoughtId = thoughtId, content = content)) }
        return when (r) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun savePost(thoughtId: String): Resource<Unit> {
        val r = safeApiCall { thoughtsApi.savePost(SaveRequest(thoughtId)) }
        return when (r) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun unsavePost(thoughtId: String): Resource<Unit> {
        val r = safeApiCall { thoughtsApi.unsavePost(thoughtId) }
        return when (r) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getActivity(): Resource<List<ActivityItem>> {
        val r = safeApiCall { mehfilApi.getActivity() }
        return when (r) {
            is Resource.Success -> Resource.Success(r.data.items?.map { it.toDomain() } ?: emptyList())
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getSavedPosts(page: Int): Resource<List<MehfilPost>> {
        val r = safeApiCall { mehfilApi.getSavedPosts(page) }
        return when (r) {
            is Resource.Success -> Resource.Success(r.data.posts?.map { it.toDomain() } ?: emptyList())
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    private fun SandeshDto.toDomain() = Sandesh(id = id ?: mongoId ?: "", content = content ?: "", createdAt = createdAt ?: "", reactionCount = reactionCount ?: 0, commentCount = commentCount ?: 0, linkMeta = linkMeta?.let { LinkMeta(it.title ?: "", it.description ?: "", it.image ?: "", it.url ?: "") }, imageUrl = imageUrl ?: "")
    private fun MehfilPostDto.toDomain(): MehfilPost {
        val resolvedName   = authorName?.takeIf { it.isNotBlank() }
            ?: author?.name?.takeIf { it.isNotBlank() }
            ?: "Anonymous"
        val resolvedAvatar = authorAvatar ?: author?.avatar
        val resolvedUserId = userId ?: author?.id ?: ""
        val displayName    = if (isAnonymous == true) "Anonymous" else resolvedName
        val resolvedSpace  = space?.takeIf { it.isNotBlank() } ?: category ?: ""
        val resolvedReactions = relatableCount ?: reactionCount ?: 0
        val resolvedComments  = commentsCount  ?: commentCount  ?: 0
        val resolvedLiked     = hasReacted ?: userLiked ?: false
        return MehfilPost(
            id            = id ?: mongoId ?: "",
            content       = content ?: "",
            space         = resolvedSpace,
            authorName    = displayName,
            authorAvatar  = resolvedAvatar,
            userId        = resolvedUserId,
            createdAt     = createdAt ?: "",
            reactionCount = resolvedReactions,
            commentCount  = resolvedComments,
            userLiked     = resolvedLiked,
        )
    }
    private fun CommentDto.toDomain() = Comment(id = id ?: "", content = content ?: "", authorName = authorName ?: "Anonymous", createdAt = createdAt ?: "")
    private fun ActivityItemDto.toDomain() = ActivityItem(type = type ?: "", createdAt = createdAt ?: "", thoughtId = thoughtId ?: "", comment = comment, thoughtContent = thought?.content ?: "", thoughtAuthor = thought?.authorName ?: "", thoughtCategory = thought?.category ?: "")
}
