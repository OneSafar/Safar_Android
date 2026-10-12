package com.safarparmar.app.data.remote.maintenance

data class MaintenanceInfo(
    val inMaintenance: Boolean = false,
    val title: String = "App Under Maintenance !",
    val message: String = "Check Back Soon......",
    val detail: String? = null,
    val estimatedEndTime: String? = null,
    val isDatabaseOperation: Boolean = true,
    val lastCheckedAt: Long = System.currentTimeMillis(),
)

data class AppUpdateInfo(
    val minimumVersionCode: Int,
    val latestVersionName: String? = null,
    val title: String = "A new Safar update is ready",
    val message: String = "Update Safar to continue using the app.",
    val playStoreUrl: String = "https://play.google.com/store/apps/details?id=com.safarparmar.app",
)
