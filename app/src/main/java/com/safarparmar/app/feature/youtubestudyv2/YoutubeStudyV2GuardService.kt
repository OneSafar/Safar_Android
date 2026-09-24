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

    override fun onCreate() {
        super.onCreate()
        if (!promote()) stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!promote()) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isForegroundStarted) {
            runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        }
        super.onDestroy()
    }

    private fun promote(): Boolean {
        if (isForegroundStarted) return true
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
                    .setContentTitle(getString(R.string.youtube_focus_is_on))
                    .setContentText(getString(R.string.youtube_focus_protection_active))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setSilent(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
            } catch (t: Throwable) {
                NotificationCompat.Builder(this, SafarNotificationChannels.YOUTUBE_STUDY_V2_STATUS)
                    .setSmallIcon(R.drawable.ic_safar_notification_sparkle)
                    .setContentTitle(getString(R.string.youtube_focus_is_on))
                    .setContentText(getString(R.string.youtube_focus_protection_active))
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
            true
        }.getOrElse { error ->
            android.util.Log.e("YoutubeStudyV2", "Failed to promote guard service", error)
            false
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2122

        fun start(context: Context) {
            val appContext = context.applicationContext
            // Do not start foreground service from background on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !com.safarparmar.app.util.AppForegroundTracker.isAppInForeground
            ) {
                android.util.Log.w("YoutubeStudyV2", "Deferred guard service start: app in background")
                return
            }

            val intent = Intent(appContext, YoutubeStudyV2GuardService::class.java)
            runCatching {
                SafarNotificationChannels.ensureYoutubeStudyV2StatusChannel(appContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appContext.startForegroundService(intent)
                else appContext.startService(intent)
            }.onFailure { error ->
                android.util.Log.e("YoutubeStudyV2", "Failed to start guard service", error)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, YoutubeStudyV2GuardService::class.java))
        }
    }
}
