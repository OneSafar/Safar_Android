package com.safarparmar.app.ui.ekagra.focusshield

/**
 * Remembers the latest foreground app between UsageEvents queries.
 *
 * UsageEvents only reports transitions, not a fresh snapshot on every poll. Keeping the
 * last transition prevents Kavach from forgetting an app simply because its resume event
 * fell outside the latest query window.
 */
internal class ForegroundAppTracker {
    var currentPackage: String? = null
        private set
    private var currentActivity: String? = null

    fun onForeground(packageName: String?, activityKey: String? = null) {
        if (!packageName.isNullOrBlank()) {
            currentPackage = packageName
            currentActivity = activityKey
        }
    }

    fun onBackground(packageName: String?, activityKey: String? = null) {
        if (!packageName.isNullOrBlank() && packageName == currentPackage &&
            (activityKey == null || currentActivity == null || activityKey == currentActivity)) {
            currentPackage = null
            currentActivity = null
        }
    }
}
