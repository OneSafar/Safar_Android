package com.safar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the list of user-installed, launchable apps from the PackageManager,
 * excluding SAFAR itself.
 */
@Singleton
class InstalledAppsLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns all launchable, non-system apps excluding this app.
     * Runs on [Dispatchers.IO] since PackageManager queries can be slow.
     */
    suspend fun loadLaunchableApps(): List<BlockedAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

        resolveInfos
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName } // Exclude Safar
            .mapNotNull { pkgName ->
                try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    BlockedAppInfo(
                        packageName = pkgName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = pm.getApplicationIcon(appInfo),
                    )
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
    }
}
