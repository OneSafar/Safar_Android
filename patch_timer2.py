import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "r") as f:
    content = f.read()

content = re.sub(r"[ \t]*stopFocusShieldMonitor\(\)\n?", "", content)
content = re.sub(r"[ \t]*startFocusShieldMonitor\(\)\n?", "", content)
content = re.sub(r"[ \t]*//.*startFocusShieldMonitor.*\n?", "", content)

# I should also remove references to enableFocusShieldForSession/disableFocusShieldForSession/activateFocusShieldFromSettingsIfNeeded if they exist elsewhere.
content = re.sub(r"[ \t]*enableFocusShieldForSession\(\)\n?", "", content)
content = re.sub(r"[ \t]*disableFocusShieldForSession\(.*\)\n?", "", content)
content = re.sub(r"[ \t]*activateFocusShieldFromSettingsIfNeeded\(\)\n?", "", content)

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "w") as f:
    f.write(content)
