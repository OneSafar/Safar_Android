import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "r") as f:
    text = f.read()

text = text.replace("kavachActive = _focusShieldActive.value,", "kavachActive = FocusShieldRepository.ShieldPrefs.isActive(this),")
text = text.replace("shieldEnabled = _focusShieldActive.value || FocusShieldRepository.ShieldPrefs.isActive(this),", "shieldEnabled = FocusShieldRepository.ShieldPrefs.isActive(this),")

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "w") as f:
    f.write(text)
