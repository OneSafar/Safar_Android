package com.safarparmar.app.ui.ekagra.focusshield

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppsLoaderTest {

    @Test
    fun `excludes safar package and sorts labels`() {
        val result = InstalledAppsLoader.toBlockedAppInfos(
            apps = listOf(
                InstalledAppsLoader.InstalledAppRecord("com.youtube", "YouTube", null),
                InstalledAppsLoader.InstalledAppRecord("com.safarparmar.app", "SAFAR", null),
                InstalledAppsLoader.InstalledAppRecord("com.alpha", "Alpha", null),
                InstalledAppsLoader.InstalledAppRecord("com.youtube", "YouTube Duplicate", null),
            ),
            ownPackageName = "com.safarparmar.app",
        )

        assertEquals(listOf("Alpha", "YouTube"), result.map { it.appName })
        assertEquals(listOf("com.alpha", "com.youtube"), result.map { it.packageName })
    }

    @Test
    fun `excludes launcher packages from blockable apps`() {
        val result = InstalledAppsLoader.toBlockedAppInfos(
            apps = listOf(
                InstalledAppsLoader.InstalledAppRecord("com.miui.home", "System Launcher", null),
                InstalledAppsLoader.InstalledAppRecord("com.whatsapp", "WhatsApp", null),
            ),
            ownPackageName = "com.safarparmar.app",
            excludedPackages = setOf("com.miui.home"),
        )

        assertEquals(listOf("WhatsApp"), result.map { it.appName })
        assertEquals(listOf("com.whatsapp"), result.map { it.packageName })
    }
}
