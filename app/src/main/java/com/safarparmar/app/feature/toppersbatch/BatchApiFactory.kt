package com.safarparmar.app.feature.toppersbatch

import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

internal const val TOPPERS_COURSE_ID = "toppers-batch-2026"
internal const val GK5_COURSE_ID = "parmar-gk-5-2026"

/** Immutable API instances: an in-flight action always stays in its batch. */
@Singleton
class BatchApiFactory @Inject constructor(private val retrofit: Retrofit) {
    private val toppers by lazy { create("toppers-batch") }
    private val gk5 by lazy { create("gk-5") }
    fun forCourse(courseId: String): ToppersBatchApi = when (courseId) {
        TOPPERS_COURSE_ID -> toppers
        GK5_COURSE_ID -> gk5
        else -> error("Unknown batch")
    }
    private fun create(batch: String): ToppersBatchApi = retrofit.newBuilder()
        .baseUrl(retrofit.baseUrl().newBuilder().addPathSegments("plans/$batch/v2/").build())
        .build().create(ToppersBatchApi::class.java)
}
