package com.safarparmar.app.domain.model

data class PremiumStatus(
    val isPremium: Boolean = false,
    val planType: String? = null,
    val expiresAt: String? = null,
    val features: PremiumFeatureAccess = PremiumFeatureAccess(),
) {
    val hasAnyPaidAccess: Boolean
        get() = isPremium ||
            features.mehfilDm ||
            features.studyPlannerInsights ||
            features.nishthaAnalytics ||
            features.focusAnalytics

    val canUseStudyPlannerInsights: Boolean
        get() = isPremium || features.studyPlannerInsights

    val canUseNishthaAnalytics: Boolean
        get() = isPremium || features.nishthaAnalytics || features.focusAnalytics

    val canUseMehfilDm: Boolean
        get() = isPremium || features.mehfilDm
}

data class PremiumFeatureAccess(
    val mehfilDm: Boolean = false,
    val studyPlannerInsights: Boolean = false,
    val nishthaAnalytics: Boolean = false,
    val focusAnalytics: Boolean = false,
)
