package com.safarparmar.app.ui.ekagra.focusshield

import android.app.AppOpsManager
import android.app.NotificationManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.view.accessibility.AccessibilityManager
import com.safarparmar.app.BuildConfig
import com.safarparmar.app.MainActivity
import com.safarparmar.app.feature.youtubestudyv2.YoutubeStudyV2AccessibilityService

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
        // Prefer the per-app Usage Access page when the OEM supports a package URI.
        val perApp = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching { context.startActivity(perApp) }.isSuccess
        if (!opened) {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** KAVACH itself no longer needs accessibility; YouTube Study Mode still does. */
    fun isAccessibilityFeatureEnabled(): Boolean = false

    /**
     * Checks the dedicated YouTube Study Mode service, not merely whether some
     * unrelated accessibility service is enabled. AccessibilityManager is the
     * authoritative API; the secure-setting fallback covers OEMs that return a
     * stale/empty enabled-service list immediately after Settings closes.
     */
    fun hasAccessibilityService(context: Context): Boolean {
        val expected = ComponentName(context, YoutubeStudyV2AccessibilityService::class.java)
        val managerEnabled = runCatching {
            val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .orEmpty()
                .any { info ->
                    val service = info.resolveInfo?.serviceInfo ?: return@any false
                    ComponentName(service.packageName, service.name) == expected
                }
        }.getOrDefault(false)
        if (managerEnabled) return true

        val enabledServices = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        }.getOrNull().orEmpty()
        return enabledServices.split(':').any { flattened ->
            ComponentName.unflattenFromString(flattened.trim()) == expected
        }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Display over other apps ("draw overlay") — required to show the KAVACH block screen. */
    fun hasOverlayPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
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
        val component = ComponentName(context, FocusShieldNotificationListenerService::class.java)
        // API 30+ can open the per-app notification-listener detail page directly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    component.flattenToString(),
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val opened = runCatching { context.startActivity(detail) }.isSuccess
            if (opened) return
        }
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

    /** Whether [target] is currently granted on this device. */
    fun isPermissionGranted(context: Context, target: PermissionTarget): Boolean = when (target) {
        PermissionTarget.USAGE_STATS -> hasUsageStatsPermission(context)
        PermissionTarget.OVERLAY -> hasOverlayPermission(context)
        PermissionTarget.NOTIFICATIONS -> hasNotificationPermission(context)
        PermissionTarget.NOTIFICATION_ACCESS -> hasNotificationListenerAccess(context)
    }

    fun openSettingsFor(context: Context, target: PermissionTarget) {
        when (target) {
            PermissionTarget.USAGE_STATS -> openUsageAccessSettings(context)
            PermissionTarget.OVERLAY -> openOverlaySettings(context)
            PermissionTarget.NOTIFICATION_ACCESS -> openNotificationListenerSettings(context)
            PermissionTarget.NOTIFICATIONS -> Unit // runtime dialog, not a Settings page
        }
    }

    /**
     * Brings SAFAR back to the foreground (typically the Kavach permission screen
     * that launched Settings) after the user enables a system permission toggle.
     */
    fun bringAppToForeground(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK,
            )
        }
        runCatching { context.startActivity(intent) }
            .onFailure { debugLog("bringAppToForeground failed: ${it.message}") }
    }

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d(TAG, message)
    }
}
