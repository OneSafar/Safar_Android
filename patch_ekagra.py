import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraScreen.kt", "r") as f:
    content = f.read()

# Remove setFocusShieldConfig and enableFocusShieldForSession calls
content = re.sub(r"[ \t]*timerService\?\.setFocusShieldConfig\(shieldState\.blockedPackages\)\n?", "", content)
content = re.sub(r"[ \t]*timerService\?\.enableFocusShieldForSession\(\)\n?", "", content)

# Remove the surrounding if block if it becomes empty
content = re.sub(r"[ \t]*if \(\(mode == TimerMode\.FOCUS \|\| mode == TimerMode\.STOPWATCH\) && shieldState\.isEnabled && shieldState\.blockedPackages\.isNotEmpty\(\)\) \{\n[ \t]*\}\n", "", content)

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraScreen.kt", "w") as f:
    f.write(content)
