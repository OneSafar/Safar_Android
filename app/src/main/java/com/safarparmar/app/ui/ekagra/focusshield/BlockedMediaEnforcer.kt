package com.safarparmar.app.ui.ekagra.focusshield

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager

/** Pauses blocked media without terminating the app or resetting its playback position. */
object BlockedMediaEnforcer {
    fun stop(context: Context, packageName: String) {
        if (packageName.isBlank() || packageName == context.packageName) return
        if (KavachYoutubePolicy.isManagedByYoutubeFocus(context, packageName)) return

        // Prefer package-scoped controls when notification access is available.
        if (FocusShieldPermissionHelper.hasNotificationListenerAccess(context)) {
            runCatching {
                val listener = ComponentName(context, FocusShieldNotificationListenerService::class.java)
                val controllers = context.getSystemService(MediaSessionManager::class.java)
                    ?.getActiveSessions(listener)
                    .orEmpty()
                    .filter { it.packageName == packageName }
                controllers.forEach { it.transportControls.pause() }
            }
        }

        // A global media key can pause a productive YouTube lesson when the
        // blocked package is a different app. Without its media session, rely on
        // Kavach's app overlay rather than controlling an unrelated player.
    }
}
