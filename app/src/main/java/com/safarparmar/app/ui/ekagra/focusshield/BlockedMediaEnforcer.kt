package com.safarparmar.app.ui.ekagra.focusshield

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.view.KeyEvent

/** Pauses blocked media without terminating the app or resetting its playback position. */
object BlockedMediaEnforcer {
    fun stop(context: Context, packageName: String) {
        if (packageName.isBlank() || packageName == context.packageName) return

        // Prefer package-scoped controls when notification access is available.
        if (FocusShieldPermissionHelper.hasNotificationListenerAccess(context)) {
            val paused = runCatching {
                val listener = ComponentName(context, FocusShieldNotificationListenerService::class.java)
                val controllers = context.getSystemService(MediaSessionManager::class.java)
                    ?.getActiveSessions(listener)
                    .orEmpty()
                    .filter { it.packageName == packageName }
                controllers.forEach { it.transportControls.pause() }
                controllers.isNotEmpty()
            }.getOrDefault(false)
            if (paused) return
        }

        // Do not hold exclusive audio focus indefinitely or send STOP: a quick
        // unlock should let the student resume the existing player in place.
        runCatching {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            manager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            manager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }
    }
}
