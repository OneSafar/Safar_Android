package com.safarparmar.app.feature.habits.premium

import javax.inject.Inject
import javax.inject.Singleton

interface FeatureAccessManager {
    fun hasAccess(feature: PremiumFeature): Boolean
}

@Singleton
class DefaultFeatureAccessManager @Inject constructor() : FeatureAccessManager {
    override fun hasAccess(feature: PremiumFeature): Boolean {
        // Centralized feature access layer.
        // Can easily be connected to user entitlement/billing tier.
        return true
    }
}
