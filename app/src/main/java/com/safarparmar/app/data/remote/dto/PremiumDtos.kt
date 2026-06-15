package com.safarparmar.app.data.remote.dto

data class PremiumFeaturesResponse(
    val mehfilDm: Boolean = false,
    val studyPlannerInsights: Boolean = false,
    val nishthaAnalytics: Boolean = false,
    val focusAnalytics: Boolean = false,
)

data class PremiumStatusResponse(
    val success: Boolean? = null,
    val isPremium: Boolean = false,
    val premium: Boolean? = null,
    val active: Boolean? = null,
    val planType: String? = null,
    val expiresAt: String? = null,
    val features: PremiumFeaturesResponse? = null,
    val message: String? = null,
)
