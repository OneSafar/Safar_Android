package com.safar.app.domain.repository

import com.safar.app.domain.model.*
import com.safar.app.util.Resource

interface MehfilRepository {
    suspend fun getSandesh(): Resource<Pair<Sandesh?, List<Sandesh>>>
    suspend fun reactSandesh(id: String): Resource<Unit>
    suspend fun getSandeshComments(id: String, page: Int): Resource<List<Comment>>
    suspend fun postSandeshComment(id: String, content: String): Resource<Unit>
    suspend fun getComments(thoughtId: String, page: Int): Resource<List<Comment>>
    suspend fun postComment(thoughtId: String, content: String): Resource<Unit>
    suspend fun savePost(thoughtId: String): Resource<Unit>
    suspend fun unsavePost(thoughtId: String): Resource<Unit>
    suspend fun getActivity(): Resource<List<ActivityItem>>
    suspend fun getSavedPosts(page: Int): Resource<List<MehfilPost>>
}
