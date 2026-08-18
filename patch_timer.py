import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "r") as f:
    content = f.read()

# Remove setFocusShieldConfig, enableFocusShieldForSession, disableFocusShieldForSession, activateFocusShieldFromSettingsIfNeeded
# Remove startFocusShieldMonitor, stopFocusShieldMonitor, monitorForegroundForBlocking, launchBlockScreen, currentForegroundPackage, shouldHideForPackage, syncFocusShieldState, handleFocusShieldBlockedIntent, showFocusShieldBlockedNotification, showFocusShieldActiveNotification, showFocusShieldFailedNotification, labelForPackage

methods_to_remove = [
    r"    fun setFocusShieldConfig\([\s\S]*?    }\n\n",
    r"    fun enableFocusShieldForSession\(\) \{[\s\S]*?    }\n\n",
    r"    fun disableFocusShieldForSession\(force: Boolean = false\) \{[\s\S]*?    }\n\n",
    r"    private fun activateFocusShieldFromSettingsIfNeeded\(\) \{[\s\S]*?    }\n\n",
    r"    private fun startFocusShieldMonitor\(\) \{[\s\S]*?    }\n\n",
    r"    private fun stopFocusShieldMonitor\(\) \{[\s\S]*?    }\n\n",
    r"    private fun monitorForegroundForBlocking\(\) \{[\s\S]*?    }\n\n",
    r"    private fun launchBlockScreen\(blockedPackage: String\) \{[\s\S]*?    }\n\n",
    r"    private fun currentForegroundPackage\(\): String\? \{[\s\S]*?    }\n\n",
    r"    private fun shouldHideForPackage\(packageName: String\): Boolean =[\s\S]*?            packageName == \"com\.android\.settings\"\n\n",
    r"    private suspend fun syncFocusShieldState\(\) \{[\s\S]*?    }\n\n",
    r"    private fun handleFocusShieldBlockedIntent\(intent: Intent\?\) \{[\s\S]*?    }\n\n",
    r"    private fun showFocusShieldBlockedNotification\(blockedPackage: String\) \{[\s\S]*?    }\n\n",
    r"    private fun showFocusShieldActiveNotification\(\) \{[\s\S]*?    }\n\n",
    r"    private fun showFocusShieldFailedNotification\(reason: String\?\) \{[\s\S]*?    }\n\n",
    r"    private fun labelForPackage\(packageName: String\): String \{[\s\S]*?    }\n\n",
]

for pattern in methods_to_remove:
    content = re.sub(pattern, "", content)

# Remove fields:
content = re.sub(r"    private var shieldMonitorJob: Job\? = null\n", "", content)
content = re.sub(r"    @Volatile private var youtubeContentOwnsBlocking: Boolean = false\n", "", content)
content = re.sub(r"    // ── Foreground-app blocking \(Usage access \+ overlay; no Accessibility\) ─────\n    private var lastBlockedPackage: String\? = null\n    private var lastBlockedAt: Long = 0L\n    /\*\* Package for which we already counted one distraction this foreground visit\. \*/\n    private var countedDistractionPackage: String\? = null\n    /\*\* True while a quick unlock is in its grace window, so its end can be detected\. \*/\n    private var quickUnlockWasActive: Boolean = false\n", "", content)

# Remove constants FOREGROUND_POLL_MS, SHIELD_SYNC_INTERVAL_MS, FOREGROUND_LOOKBACK_MS, BLOCK_DEBOUNCE_MS
content = re.sub(r"        // KAVACH foreground-app polling \(replaces the former Accessibility event stream\)\.\n        private const val FOREGROUND_POLL_MS = 500L\n        private const val SHIELD_SYNC_INTERVAL_MS = 1_500L\n        private const val FOREGROUND_LOOKBACK_MS = 2_000L\n        private const val BLOCK_DEBOUNCE_MS = 750L\n", "", content)

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "w") as f:
    f.write(content)
