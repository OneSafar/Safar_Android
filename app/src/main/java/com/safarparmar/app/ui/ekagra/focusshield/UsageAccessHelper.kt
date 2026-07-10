package com.safarparmar.app.ui.ekagra.focusshield

import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.safarparmar.app.BuildConfig

object FocusShieldPermissionHelper {

    private const val TAG = "FocusShieldPermissionHelper"

    fun hasUsageStatsPermission(context: Context): Boolean = try {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * KAVACH no longer uses Android Accessibility Service. Blocking is driven entirely by
     * Usage access (foreground-app detection) plus "Display over other apps" (the block screen).
     * These stubs remain only so legacy callers keep compiling; they always report "not required".
     */
    fun isAccessibilityFeatureEnabled(): Boolean = false

    fun hasAccessibilityService(context: Context): Boolean = false

    /** Display over other apps ("draw overlay") — required to show the KAVACH block screen. */
    fun hasOverlayPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(intent)
        }.getOrElse {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationListenerAccess(context: Context): Boolean {
        val component = ComponentName(context, FocusShieldNotificationListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return runCatching {
                context.getSystemService(NotificationManager::class.java)
                    .isNotificationListenerAccessGranted(component)
            }.getOrDefault(false)
        }

        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabledListeners.split(':').any { it.equals(component.flattenToString(), ignoreCase = true) }
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun requestNotificationListenerRebind(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val component = ComponentName(context, FocusShieldNotificationListenerService::class.java)
        runCatching {
            NotificationListenerService.requestRebind(component)
        }.onFailure {
            debugLog("Notification listener rebind request failed: ${it.javaClass.simpleName}")
        }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, message)
    }
}
