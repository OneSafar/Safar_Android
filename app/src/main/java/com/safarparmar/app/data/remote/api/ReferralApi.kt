package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.AttributeInstallRequest
import com.safarparmar.app.data.remote.dto.AttributeInstallResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ReferralApi {
    @POST("referrals/attribute")
    suspend fun attributeInstall(
        @Body request: AttributeInstallRequest
    ): Response<AttributeInstallResponse>
}
