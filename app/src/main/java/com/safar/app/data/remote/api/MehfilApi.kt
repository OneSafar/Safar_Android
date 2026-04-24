package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MehfilApi {
    @GET("mehfil/sandesh") suspend fun getSandesh(): Response<SandeshResponse>
    @POST("mehfil/sandesh/{id}/react") suspend fun reactSandesh(@Path("id") id: String): Response<Any>
    @GET("mehfil/sandesh/{id}/comments") suspend fun getSandeshComments(@Path("id") id: String, @Query("page") page: Int = 1): Response<CommentsResponse>
    @POST("mehfil/sandesh/{id}/comments") suspend fun postSandeshComment(@Path("id") id: String, @Body body: CommentRequest): Response<Any>
    @GET("mehfil/activity") suspend fun getActivity(): Response<ActivityResponse>
    @GET("mehfil/saved-posts") suspend fun getSavedPosts(@Query("page") page: Int = 1): Response<SavedPostsResponse>
}

interface ThoughtsApi {
    @GET("mehfil/interactions/comments/{thoughtId}") suspend fun getComments(@Path("thoughtId") id: String, @Query("page") page: Int = 1): Response<CommentsResponse>
    @POST("mehfil/interactions/comments") suspend fun postComment(@Body body: CommentRequest): Response<Any>
    @POST("mehfil/interactions/save") suspend fun savePost(@Body body: SaveRequest): Response<Any>
    @DELETE("mehfil/interactions/save/{thoughtId}") suspend fun unsavePost(@Path("thoughtId") thoughtId: String): Response<Any>
}
