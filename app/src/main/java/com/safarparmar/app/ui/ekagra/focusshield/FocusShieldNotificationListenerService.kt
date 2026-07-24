package com.safarparmar.app.ui.ekagra.focusshield

import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.ui.ekagra.TimerService

class FocusShieldNotificationListenerService : NotificationListenerService() {

    private val mainHandler = Handler(Looper.getMainLooper())

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

        cancelBlockedNotification(notification, "posted")
        mainHandler.postDelayed(
            { suppressActiveBlockedNotifications("posted sweep") },
            SUPPRESSION_SWEEP_DELAY_MS,
        )
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        suppressActiveBlockedNotifications("ranking update")
    }

    private fun suppressActiveBlockedNotifications(reason: String) {
        val alwaysOn = KavachAlwaysOnPrefs.isActive(this)
        if (!NotificationShieldPrefs.isActive(this) && !alwaysOn) return
        if (!TimerService.isKavachNotificationSuppressionActive(this) && !alwaysOn) return

        val blockedPackages = if (alwaysOn) KavachAlwaysOnPrefs.packages(this)
        else NotificationShieldPrefs.packages(this)
        if (blockedPackages.isEmpty()) return

        runCatching {
            activeNotifications
                ?.filter { shouldSuppressPackage(it.packageName, blockedPackages) }
                ?.forEach { cancelBlockedNotification(it, reason) }
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
        val alwaysOn = KavachAlwaysOnPrefs.isActive(this)
        if (!NotificationShieldPrefs.isActive(this) && !alwaysOn) return false
        if (!TimerService.isKavachNotificationSuppressionActive(this) && !alwaysOn) return false
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

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("FocusShieldNotif", message)
    }

    private companion object {
        private const val SUPPRESSION_SWEEP_DELAY_MS = 250L
    }
}
