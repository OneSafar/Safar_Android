package com.safarparmar.app.ui.ekagra.focusshield

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.os.Build
import android.view.KeyEvent
import com.safarparmar.app.BuildConfig

/** Stops media owned by a blocked app, including playback continuing in system PiP. */
object BlockedMediaEnforcer {

    fun stop(context: Context, packageName: String) {
        if (packageName.isBlank() || packageName == context.packageName) return

        // 1. Strip audio focus (causes YouTube/Shorts and video players to immediately pause playback)
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .build()
                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(
                        null,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                    )
                }

                // Dispatch media pause & stop key events
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP))
            }
        }.onFailure {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("KavachMedia", "Unable to request audio focus / pause media: ${it.message}")
            }
        }

        // 2. Kill background / PiP process if possible
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.killBackgroundProcesses(packageName)
        }

        // 3. MediaSessionManager (if Notification Listener access is granted)
        if (FocusShieldPermissionHelper.hasNotificationListenerAccess(context)) {
            val listener = ComponentName(context, FocusShieldNotificationListenerService::class.java)
            val manager = context.getSystemService(MediaSessionManager::class.java)
            runCatching {
                manager?.getActiveSessions(listener)
                    ?.filter { it.packageName == packageName }
                    ?.forEach { controller ->
                        controller.transportControls.pause()
                        controller.transportControls.stop()
                    }
            }.onFailure {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("KavachMedia", "Unable to stop $packageName media via MediaSessionManager: ${it.message}")
                }
            }
        }
    }
}
