package com.safarparmar.app.ui.ekagra.focusshield

import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.ui.ekagra.TimerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusShieldNotificationListenerService : NotificationListenerService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val dataStore by lazy { SafarDataStore(applicationContext) }

    override fun onListenerConnected() {
        suppressActiveBlockedNotifications("listener connected")
    }

    override fun onListenerDisconnected() {
        if (NotificationShieldPrefs.isActive(this) || KavachAlwaysOnPrefs.isActive(this)) {
            FocusShieldPermissionHelper.requestNotificationListenerRebind(this)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (!shouldSuppressPackage(notification.packageName)) return

        pauseBlockedMedia(notification.packageName)
        cancelBlockedNotification(notification, "posted")
        mainHandler.postDelayed(
            { suppressActiveBlockedNotifications("posted sweep") },
            SUPPRESSION_SWEEP_DELAY_MS,
        )
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        suppressActiveBlockedNotifications("ranking update")
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        mediaScope.cancel()
        super.onDestroy()
    }

    private fun suppressActiveBlockedNotifications(reason: String) {
        val alwaysOn = KavachAlwaysOnPrefs.isActive(this)
        val timerActive = NotificationShieldPrefs.isActive(this) &&
            TimerService.isKavachNotificationSuppressionActive(this)
        if (!alwaysOn && !timerActive) return

        val blockedPackages = if (alwaysOn) {
            KavachAlwaysOnPrefs.packages(this)
        } else {
            NotificationShieldPrefs.packages(this)
        }
        if (blockedPackages.isEmpty()) return

        runCatching {
            activeNotifications
                ?.filter { shouldSuppressPackage(it.packageName, blockedPackages) }
                ?.forEach {
                    pauseBlockedMedia(it.packageName)
                    cancelBlockedNotification(it, reason)
                }
        }.onFailure {
            debugLog("Notification suppression sweep failed: ${it.javaClass.simpleName}")
        }
    }

    private fun shouldSuppressPackage(
        packageName: String?,
        blockedPackages: Set<String> = if (KavachAlwaysOnPrefs.isActive(this)) {
            KavachAlwaysOnPrefs.packages(this)
        } else {
            NotificationShieldPrefs.packages(this)
        },
    ): Boolean {
        val normalizedPackage = packageName?.takeIf { it.isNotBlank() } ?: return false
        if (normalizedPackage == this.packageName) return false
        if (KavachYoutubePolicy.isManagedByYoutubeFocus(this, normalizedPackage)) return false
        val alwaysOn = KavachAlwaysOnPrefs.isActive(this)
        val timerActive = NotificationShieldPrefs.isActive(this) &&
            TimerService.isKavachNotificationSuppressionActive(this)
        if (!alwaysOn && !timerActive) return false
        return normalizedPackage in blockedPackages
    }

    private fun cancelBlockedNotification(sbn: StatusBarNotification, reason: String) {
        runCatching {
            cancelNotification(sbn.key)
            debugLog("Suppressed notification from ${sbn.packageName} ($reason)")
        }.onFailure {
            debugLog("Notification suppression failed for ${sbn.packageName}: ${it.javaClass.simpleName}")
        }
    }

    private fun pauseBlockedMedia(packageName: String) {
        mediaScope.launch {
            runCatching {
                pauseKavachNotificationMediaIfBlocked(
                    context = this@FocusShieldNotificationListenerService,
                    packageName = packageName,
                    strictMode = { dataStore.focusShieldStrictMode.first() },
                    isStillBlocked = { shouldSuppressPackage(packageName) },
                )
            }.onFailure {
                debugLog("Notification media enforcement failed: ${it.javaClass.simpleName}")
            }
        }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("FocusShieldNotif", message)
    }

    private companion object {
        private const val SUPPRESSION_SWEEP_DELAY_MS = 250L
    }
}
