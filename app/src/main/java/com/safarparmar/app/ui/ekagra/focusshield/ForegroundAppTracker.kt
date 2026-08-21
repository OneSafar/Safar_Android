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

    fun onForeground(packageName: String?) {
        if (!packageName.isNullOrBlank()) currentPackage = packageName
    }

    fun onBackground(packageName: String?) {
        if (!packageName.isNullOrBlank() && packageName == currentPackage) {
            currentPackage = null
        }
    }
}
