package com.safarparmar.app.ui.ekagra.focusshield

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.safarparmar.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
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
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /**
     * Returns all launchable, non-system apps excluding this app.
     * Runs on [Dispatchers.IO] since PackageManager queries can be slow.
     */
    suspend fun loadLaunchableApps(): List<BlockedAppInfo> = withContext(dispatcher) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val homePackages = resolveHomePackages(pm)

        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

        toBlockedAppInfos(
            apps = resolveInfos
                .map { it.activityInfo.packageName }
                .distinct()
                .mapNotNull { pkgName ->
                    try {
                        val appInfo = pm.getApplicationInfo(pkgName, 0)
                        InstalledAppRecord(
                            packageName = pkgName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            icon = pm.getApplicationIcon(appInfo),
                        )
                    } catch (_: PackageManager.NameNotFoundException) {
                        null
                    }
                },
            ownPackageName = context.packageName,
            excludedPackages = homePackages,
        )
    }

    companion object {
        internal fun toBlockedAppInfos(
            apps: List<InstalledAppRecord>,
            ownPackageName: String,
            excludedPackages: Set<String> = emptySet(),
        ): List<BlockedAppInfo> =
            apps
                .distinctBy { it.packageName }
                .filter { it.packageName != ownPackageName }
                .filter { it.packageName !in excludedPackages }
                .map {
                    BlockedAppInfo(
                        packageName = it.packageName,
                        appName = it.appName,
                        icon = it.icon,
                    )
                }
                .sortedBy { it.appName.lowercase() }

        private fun resolveHomePackages(pm: PackageManager): Set<String> {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val queried = pm.queryIntentActivities(homeIntent, 0)
                .mapNotNull { it.activityInfo?.packageName }
            val resolved = pm.resolveActivity(homeIntent, 0)?.activityInfo?.packageName
            return (queried + listOfNotNull(resolved) + KNOWN_HOME_PACKAGES).toSet()
        }

        private val KNOWN_HOME_PACKAGES = setOf(
            "com.miui.home",
            "com.mi.android.globallauncher",
            "com.android.launcher",
            "com.android.launcher2",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.vivo.launcher",
            "com.transsion.XOSLauncher",
        )
    }

    internal data class InstalledAppRecord(
        val packageName: String,
        val appName: String,
        val icon: android.graphics.drawable.Drawable?,
    )
}
