import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt", "r") as f:
    content = f.read()

# 1. In setKavachProfile(), for FOCUSED/STANDARD:
old_standard = """                com.safarparmar.app.ui.launch.AppUsageMode.FOCUSED,
                com.safarparmar.app.ui.launch.AppUsageMode.STANDARD -> {
                    dataStore.setFocusShieldEnabled(true)
                    dataStore.setFocusShieldStrictMode(false)
                    dataStore.setFocusShieldAlwaysOnMode(false)
                    KavachAlwaysOnService.stop(appContext)
                }"""
new_standard = """                com.safarparmar.app.ui.launch.AppUsageMode.FOCUSED,
                com.safarparmar.app.ui.launch.AppUsageMode.STANDARD -> {
                    dataStore.setFocusShieldEnabled(true)
                    dataStore.setFocusShieldStrictMode(false)
                    dataStore.setFocusShieldAlwaysOnMode(false)
                    startKavachService()
                }"""
content = content.replace(old_standard, new_standard)

# 2. Rename startAlwaysOnService() to startKavachService()
content = content.replace("startAlwaysOnService", "startKavachService")

# 3. Rename restoreAlwaysOnIfEnabled() to restoreKavachIfEnabled()
content = content.replace("restoreAlwaysOnIfEnabled", "restoreKavachIfEnabled")

# Update restoreKavachIfEnabled to check only enabled
old_restore = """            if (dataStore.focusShieldAlwaysOnMode.first() &&
                dataStore.focusShieldEnabled.first()
            ) {"""
new_restore = """            if (dataStore.focusShieldEnabled.first()) {"""
content = content.replace(old_restore, new_restore)

# Remove quick-unlock-clearing logic that was specific to Always On
# Specifically, in startKavachService:
# ShieldPrefs.applyEmergencyUnlock(appContext, graceUntilMs = 0L)
# QuickUnlockNotification.cancel(appContext)

content = re.sub(r"[ \t]*// Drop any quick-unlock window still running from a previous Normal-Mode[\s\S]*?QuickUnlockNotification\.cancel\(appContext\)\n", "", content)

# 4. In activateForSession(), it should just record analytics if active.
# Let's check what it does currently.
with open("app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt", "w") as f:
    f.write(content)
