import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraScreen.kt", "r") as f:
    content = f.read()

# Replace references to focusShieldActive from timerService with shieldState.sessionActive or shieldState.isEnabled
content = re.sub(r"val fallbackFocusShieldActive = remember \{ MutableStateFlow\(false\) \}\n", "", content)
content = re.sub(r"[ \t]*val focusShieldActive by \(timerService\?\.focusShieldActive  \?: fallbackFocusShieldActive\)\.collectAsStateWithLifecycle\(\)\n", "", content)

# Replace 'focusShieldActive' with 'shieldState.isEnabled' in EkagraScreen
content = re.sub(r"focusShieldActive", "shieldState.isEnabled", content)

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraScreen.kt", "w") as f:
    f.write(content)
