package com.safarparmar.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AttributeInstallRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("utmSource") val utmSource: String?,
    @SerializedName("utmMedium") val utmMedium: String?,
    @SerializedName("utmCampaign") val utmCampaign: String?,
    @SerializedName("rawReferrer") val rawReferrer: String?
)

data class AttributeInstallResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null
)
