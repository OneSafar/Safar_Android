package com.safarparmar.app.feature.youtubestudyv2

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.safarparmar.app.R
import com.safarparmar.app.notifications.NotificationDeepLinkHandler
import com.safarparmar.app.notifications.SafarNotificationChannels

/** Process-lifetime support for the user-enabled accessibility monitor. */
class YoutubeStudyV2GuardService : Service() {
    private var isForegroundStarted = false
    private var isActiveStatusShown = false

    override fun onCreate() {
        super.onCreate()
        // Acknowledge every accepted foreground-service start immediately, including
        // one invalidated while Android was creating the service. A neutral status
        // avoids claiming protection before onStartCommand validates the request.
        if (!promote(active = false)) stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isForegroundStarted && !promote(active = false)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val enabled = YoutubeStudyV2Preferences.isEnabled(this)
        val connected = YoutubeStudyV2AccessibilityService.isConnected
        if (!startRequests.canRun(
                request = intent?.getLongExtra(EXTRA_START_REQUEST, -1L) ?: -1L,
                enabled = enabled,
                connected = connected,
            )
        ) {
            // An older command must not tear down a newer, valid foreground start.
            if (!enabled || !connected || !isActiveStatusShown) {
                if (isActiveStatusShown && !promote(active = false)) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                // A newer start may make stopSelfResult return false. Keep its
                // foreground acknowledgement until that command or onDestroy.
                stopSelfResult(startId)
            }
            return START_NOT_STICKY
        }
        if (!promote()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeForegroundNotification()
        super.onDestroy()
    }

    private fun removeForegroundNotification() {
        if (isForegroundStarted) {
            runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
            isForegroundStarted = false
            isActiveStatusShown = false
        }
    }

    private fun promote(active: Boolean = true): Boolean {
        if (isForegroundStarted && isActiveStatusShown == active) return true
        return runCatching {
            SafarNotificationChannels.ensureYoutubeStudyV2StatusChannel(this)
            val pendingIntent = android.app.PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                NotificationDeepLinkHandler.activityIntent(this, "safar://youtube_study_v2"),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = try {
                NotificationCompat.Builder(this, SafarNotificationChannels.YOUTUBE_STUDY_V2_STATUS)
                    .setSmallIcon(R.drawable.ic_safar_notification_sparkle)
                    .setContentTitle(getString(if (active) R.string.youtube_focus_is_on else R.string.kavach_youtube_focus))
                    .setContentText(getString(if (active) R.string.youtube_focus_protection_active else R.string.common_loading))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setSilent(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
            } catch (t: Throwable) {
                NotificationCompat.Builder(this, SafarNotificationChannels.YOUTUBE_STUDY_V2_STATUS)
                    .setSmallIcon(R.drawable.ic_safar_notification_sparkle)
                    .setContentTitle(getString(if (active) R.string.youtube_focus_is_on else R.string.kavach_youtube_focus))
                    .setContentText(getString(if (active) R.string.youtube_focus_protection_active else R.string.common_loading))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .build()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundStarted = true
            isActiveStatusShown = active
            true
        }.getOrElse { error ->
            android.util.Log.e("YoutubeStudyV2", "Failed to promote guard service", error)
            false
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2122
        private const val EXTRA_START_REQUEST = "youtube_focus_guard_start_request"
        private val startRequests = YoutubeGuardStartRequests()

        fun start(context: Context) {
            val appContext = context.applicationContext
            // A permission toggle can precede the actual accessibility connection.
            // The connection callback retries once protection can really run.
            if (!YoutubeStudyV2Preferences.isEnabled(appContext) ||
                !YoutubeStudyV2AccessibilityService.isConnected
            ) return
            // Do not start foreground service from background on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !com.safarparmar.app.util.AppForegroundTracker.isAppInForeground
            ) {
                android.util.Log.w("YoutubeStudyV2", "Deferred guard service start: app in background")
                return
            }

            val intent = Intent(appContext, YoutubeStudyV2GuardService::class.java)
                .putExtra(EXTRA_START_REQUEST, startRequests.requestStart())
            runCatching {
                SafarNotificationChannels.ensureYoutubeStudyV2StatusChannel(appContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appContext.startForegroundService(intent)
                else appContext.startService(intent)
            }.onFailure { error ->
                android.util.Log.e("YoutubeStudyV2", "Failed to start guard service", error)
            }
        }

        fun stop(context: Context) {
            // Invalidate queued start commands before asking Android to stop the service.
            startRequests.invalidate()
            context.stopService(Intent(context, YoutubeStudyV2GuardService::class.java))
        }
    }
}
