package com.safarparmar.app.ui.ekagra.focusshield

import android.app.AppOpsManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * While [awaiting] is non-null, watches that system permission. The moment it
 * becomes granted (user flipped the Settings toggle), SAFAR is brought back to
 * the foreground so the user lands on the Kavach permission screen again.
 *
 * Usage Access uses [AppOpsManager.startWatchingMode] for a fast callback;
 * Overlay / Notification Listener fall back to short-interval polling.
 */
@Composable
fun AwaitPermissionThenReturnToApp(
    awaiting: PermissionTarget?,
    onReturned: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val mainHandler = Handler(Looper.getMainLooper())

    fun returnToApp() {
        mainHandler.post {
            FocusShieldPermissionHelper.bringAppToForeground(appContext)
            onReturned()
        }
    }

    // Fast path for Usage Access via AppOps callback.
    DisposableEffect(awaiting) {
        if (awaiting != PermissionTarget.USAGE_STATS) {
            return@DisposableEffect onDispose { }
        }
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return@DisposableEffect onDispose { }
        val listener = AppOpsManager.OnOpChangedListener { _, _ ->
            if (FocusShieldPermissionHelper.hasUsageStatsPermission(appContext)) {
                returnToApp()
            }
        }
        runCatching {
            appOps.startWatchingMode(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                appContext.packageName,
                listener,
            )
        }
        onDispose {
            runCatching { appOps.stopWatchingMode(listener) }
        }
    }

    // Polling path for all watched targets (also covers Usage as a backup).
    LaunchedEffect(awaiting) {
        val target = awaiting ?: return@LaunchedEffect
        delay(500)
        while (isActive) {
            val granted = withContext(Dispatchers.Default) {
                FocusShieldPermissionHelper.isPermissionGranted(appContext, target)
            }
            if (granted) {
                returnToApp()
                break
            }
            delay(350)
        }
    }
}
