package com.safarparmar.app.data.repository

import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.PremiumApi
import com.safarparmar.app.data.remote.dto.PremiumFeaturesResponse
import com.safarparmar.app.data.remote.dto.PremiumStatusResponse
import com.safarparmar.app.domain.model.PremiumFeatureAccess
import com.safarparmar.app.domain.model.PremiumStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumRepository @Inject constructor(
    private val api: PremiumApi,
    private val dataStore: SafarDataStore,
) {
    val cachedStatus: Flow<PremiumStatus> = combine(
        dataStore.isPremium,
        dataStore.premiumPlanType,
        dataStore.premiumExpiresAt,
        dataStore.premiumFeatureMehfilDm,
        dataStore.premiumFeatureStudyPlannerInsights,
        dataStore.premiumFeatureNishthaAnalytics,
        dataStore.premiumFeatureFocusAnalytics,
    ) { values ->
        val isPremium = values[0] as Boolean
        val planType = values[1] as String?
        val expiresAt = values[2] as String?
        val mehfilDm = values[3] as Boolean
        val studyPlannerInsights = values[4] as Boolean
        val nishthaAnalytics = values[5] as Boolean
        val focusAnalytics = values[6] as Boolean
        PremiumStatus(
            isPremium = isPremium,
            planType = planType,
            expiresAt = expiresAt,
            features = PremiumFeatureAccess(
                mehfilDm = mehfilDm,
                studyPlannerInsights = studyPlannerInsights,
                nishthaAnalytics = nishthaAnalytics,
                focusAnalytics = focusAnalytics,
            ),
        )
    }

    suspend fun refreshStatus(): Result<PremiumStatus> = runCatching {
        val response = api.getStatus()
        if (!response.isSuccessful) {
            error(response.message().ifBlank { "Could not restore premium status" })
        }
        val body = response.body() ?: error("Premium status response was empty")
        val status = body.toDomain()
        dataStore.setPremiumStatus(
            isPremium = status.isPremium,
            planType = status.planType,
            expiresAt = status.expiresAt,
            mehfilDm = status.features.mehfilDm,
            studyPlannerInsights = status.features.studyPlannerInsights,
            nishthaAnalytics = status.features.nishthaAnalytics,
            focusAnalytics = status.features.focusAnalytics,
        )
        status
    }

    suspend fun cacheVerifiedStatus(response: PremiumStatusResponse?): PremiumStatus? {
        val status = response?.toDomain() ?: return null
        dataStore.setPremiumStatus(
            isPremium = status.isPremium,
            planType = status.planType,
            expiresAt = status.expiresAt,
            mehfilDm = status.features.mehfilDm,
            studyPlannerInsights = status.features.studyPlannerInsights,
            nishthaAnalytics = status.features.nishthaAnalytics,
            focusAnalytics = status.features.focusAnalytics,
        )
        return status
    }
}

private fun PremiumStatusResponse.toDomain(): PremiumStatus {
    val resolvedFeatures = features.toDomain()
    val resolvedPremium = isPremium || premium == true || active == true
    return PremiumStatus(
        isPremium = resolvedPremium,
        planType = planType,
        expiresAt = expiresAt,
        features = resolvedFeatures,
    )
}

private fun PremiumFeaturesResponse?.toDomain(): PremiumFeatureAccess {
    if (this == null) return PremiumFeatureAccess()
    return PremiumFeatureAccess(
        mehfilDm = mehfilDm,
        studyPlannerInsights = studyPlannerInsights,
        nishthaAnalytics = nishthaAnalytics,
        focusAnalytics = focusAnalytics,
    )
}
