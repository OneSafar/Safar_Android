package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context

/** Notification suppression continues during Quick Unlock, but playback is permitted. */
internal suspend fun pauseKavachNotificationMediaIfBlocked(
    context: Context,
    packageName: String,
    strictMode: suspend () -> Boolean,
    isStillBlocked: () -> Boolean,
) {
    // Always On does not write the timer's ShieldPrefs strict flag. Read the current
    // setting, then recheck protection and the grace window after the suspended read.
    val strict = strictMode()
    if (isStillBlocked() && !allowsKavachQuickUnlock(context, packageName, strict)) {
        BlockedMediaEnforcer.stop(context, packageName)
    }
}
