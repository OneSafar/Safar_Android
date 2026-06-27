package com.safarparmar.app.data.repository

import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.PremiumApi
import com.safarparmar.app.data.remote.dto.PremiumFeaturesResponse
import com.safarparmar.app.data.remote.dto.PremiumStatusResponse
import com.safarparmar.app.domain.model.PremiumFeatureAccess
import com.safarparmar.app.domain.model.PremiumStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumRepository @Inject constructor(
    private val api: PremiumApi,
    private val dataStore: SafarDataStore,
) {
    val cachedStatus: Flow<PremiumStatus> = combine(
        dataStore.userEmail,
        dataStore.isPremium,
        dataStore.premiumPlanType,
        dataStore.premiumExpiresAt,
        dataStore.premiumFeatureMehfilDm,
        dataStore.premiumFeatureStudyPlannerInsights,
        dataStore.premiumFeatureNishthaAnalytics,
        dataStore.premiumFeatureFocusAnalytics,
    ) { values ->
        val userEmail = values[0] as String?
        val isPremium = values[1] as Boolean
        val planType = values[2] as String?
        val expiresAt = values[3] as String?
        val mehfilDm = values[4] as Boolean
        val studyPlannerInsights = values[5] as Boolean
        val nishthaAnalytics = values[6] as Boolean
        val focusAnalytics = values[7] as Boolean
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
        ).withDeveloperPremiumOverride(userEmail)
    }

    suspend fun refreshStatus(): Result<PremiumStatus> = runCatching {
        val userEmail = dataStore.userEmail.firstOrNull()
        if (userEmail.isDeveloperPremiumEmail()) {
            return@runCatching developerPremiumStatus()
        }

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
        status.withDeveloperPremiumOverride(userEmail)
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
        return status.withDeveloperPremiumOverride(dataStore.userEmail.firstOrNull())
    }
}

private val developerPremiumEmails = setOf(
    "steve123@example.com",
)

private fun String?.isDeveloperPremiumEmail(): Boolean =
    this?.trim()?.lowercase() in developerPremiumEmails

private fun developerPremiumStatus(): PremiumStatus = PremiumStatus(
    isPremium = true,
    planType = "developer_full_access",
    expiresAt = "2099-12-31T23:59:59Z",
    features = PremiumFeatureAccess(
        mehfilDm = true,
        studyPlannerInsights = true,
        nishthaAnalytics = true,
        focusAnalytics = true,
    ),
)

private fun PremiumStatus.withDeveloperPremiumOverride(email: String?): PremiumStatus {
    if (!email.isDeveloperPremiumEmail()) return this
    return developerPremiumStatus()
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
