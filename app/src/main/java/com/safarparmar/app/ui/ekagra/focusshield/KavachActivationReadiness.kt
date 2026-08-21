package com.safarparmar.app.ui.ekagra.focusshield

/** One shared, user-facing readiness policy for every Kavach activation path. */
internal object KavachActivationReadiness {
    fun warning(
        blockedPackages: Set<String>,
        hasUsageAccess: Boolean,
        hasOverlayPermission: Boolean,
    ): String? = when {
        blockedPackages.isEmpty() -> "Select at least one app for Kavach to block."
        !hasUsageAccess && !hasOverlayPermission ->
            "Kavach needs Usage Access and Display over other apps."
        !hasUsageAccess -> "Turn on Usage Access for Kavach."
        !hasOverlayPermission -> "Allow Kavach to display over other apps."
        else -> null
    }
}
