package com.safar.app.ui.ekagra.focusshield

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppsLoaderTest {

    @Test
    fun `excludes safar package and sorts labels`() {
        val result = InstalledAppsLoader.toBlockedAppInfos(
            apps = listOf(
                InstalledAppsLoader.InstalledAppRecord("com.youtube", "YouTube", null),
                InstalledAppsLoader.InstalledAppRecord("com.safar.app", "SAFAR", null),
                InstalledAppsLoader.InstalledAppRecord("com.alpha", "Alpha", null),
                InstalledAppsLoader.InstalledAppRecord("com.youtube", "YouTube Duplicate", null),
            ),
            ownPackageName = "com.safar.app",
        )

        assertEquals(listOf("Alpha", "YouTube"), result.map { it.appName })
        assertEquals(listOf("com.alpha", "com.youtube"), result.map { it.packageName })
    }
}
