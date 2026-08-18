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
