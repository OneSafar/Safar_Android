import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt", "r") as f:
    content = f.read()

# Replace activateForSession with a simplified version.
old_activate = """    fun activateForSession(plannedSeconds: Int = 0, isFocusPeriod: Boolean = true) {
        val settings = currentSettings()
        if (!settings.enabled) {
            debugLog("activateForSession skipped: shield not enabled")
            _activationBlockedReason.value = null
            return
        }

        if (settings.packages.isEmpty()) {
            debugLog("activateForSession skipped: no blocked packages")
            _activationBlockedReason.value = null
            return
        }
        // Always On owns blocking outright, and must keep owning it for the whole
        // session. Handing over to timer-bound blocking here is what downgraded the
        // student to Normal Mode mid-session: they got the bottom sheet with a quick
        // unlock button, in the one mode whose entire point is that there is no way
        // out. The timed session is still recorded, so study time is still measured —
        // only the blocking stays where the student put it.
        if (isAlwaysOnMode.value) {
            debugLog("activateForSession deferred: Always On owns blocking")
            _activationBlockedReason.value = null
            if (isFocusPeriod) {
                analyticsRecorder.sessionStarted(strictMode = false, plannedSeconds = plannedSeconds)
            }
            return
        }

        if (!hasRequiredPermissions()) {
            debugLog("activateForSession skipped: required permission missing")
            _activationBlockedReason.value = "A permission KAVACH needs was turned off, so blocking isn't active this session."
            // Keep the flag on the summary rather than quietly reporting a clean
            // session in which nothing was ever actually blocked.
            analyticsRecorder.permissionLost()
            return
        }
        activateBlocking(settings, resetUnlocks = true)
        QuickUnlockNotification.cancel(appContext)
        _activationBlockedReason.value = null
        // Idempotent: a Normal-Mode break deactivates and re-activates blocking, but
        // that is still one Kavach session from the student's point of view.
        if (isFocusPeriod) {
            analyticsRecorder.sessionStarted(strictMode = false, plannedSeconds = plannedSeconds)
        }
        debugLog("activateForSession enabled for ${settings.packages.size} packages")
    }"""

new_activate = """    fun activateForSession(plannedSeconds: Int = 0, isFocusPeriod: Boolean = true) {
        val settings = currentSettings()
        if (!settings.enabled || settings.packages.isEmpty()) {
            _activationBlockedReason.value = null
            return
        }
        
        if (!hasRequiredPermissions()) {
            _activationBlockedReason.value = "A permission KAVACH needs was turned off, so blocking isn't active this session."
            analyticsRecorder.permissionLost()
            return
        }
        
        _activationBlockedReason.value = null
        
        if (isFocusPeriod) {
            analyticsRecorder.sessionStarted(strictMode = false, plannedSeconds = plannedSeconds)
        }
    }"""
content = content.replace(old_activate, new_activate)

# 4. In setEnabled(): When enabling, if packages exist, call startKavachService().
# When disabling, call KavachAlwaysOnService.stop(appContext).

old_set_enabled = """    fun setEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldEnabled(enabled)
            if (!enabled) {
                // Turning Kavach off has to take Always On with it, or blocking would
                // carry on from a screen that says it is switched off.
                dataStore.setFocusShieldAlwaysOnMode(false)
                NotificationShieldPrefs.clear(appContext)
                KavachAlwaysOnService.stop(appContext)
            }
            val settings = currentSettings().copy(enabled = enabled)
            if (!enabled) {
                deactivateSession()
            }
            if (enabled && blockedPackages.value.isNotEmpty()) {
                homeRepository.trackKavachEvent("configured", blockedPackages.value.size)
            }
        }
    }"""

new_set_enabled = """    fun setEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.setFocusShieldEnabled(enabled)
            if (enabled) {
                if (blockedPackages.value.isNotEmpty()) {
                    startKavachService(blockedPackages.value)
                    homeRepository.trackKavachEvent("configured", blockedPackages.value.size)
                }
            } else {
                dataStore.setFocusShieldAlwaysOnMode(false)
                NotificationShieldPrefs.clear(appContext)
                KavachAlwaysOnService.stop(appContext)
                deactivateSession()
            }
        }
    }"""

content = content.replace(old_set_enabled, new_set_enabled)

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt", "w") as f:
    f.write(content)
