package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.PremiumFeaturesResponse
import com.safarparmar.app.data.remote.dto.PremiumStatusResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface PremiumApi {
    @GET("premium/features")
    suspend fun getFeatures(): Response<PremiumFeaturesResponse>

    @GET("premium/status")
    suspend fun getStatus(): Response<PremiumStatusResponse>

    @POST("premium/start-trial")
    suspend fun startTrial(): Response<PremiumStatusResponse>
}
