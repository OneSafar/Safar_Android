import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "r") as f:
    text = f.read()

# Remove the property from TimerService.kt
text = re.sub(r"[ \t]*private val _focusShieldActive  = kotlinx\.coroutines\.flow\.MutableStateFlow\(false\)\n", "", text)
text = re.sub(r"[ \t]*val focusShieldActive: kotlinx\.coroutines\.flow\.StateFlow<Boolean> = _focusShieldActive\n", "", text)
text = re.sub(r"[ \t]*private fun focusShieldRepo\(\): FocusShieldRepository =[\s\S]*?\.focusShieldRepository\(\)\n", "", text)

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "w") as f:
    f.write(text)
