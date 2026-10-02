import re

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# 1. Fix content in Column
content = content.replace("content = content\n            )", ") {\n                content()\n            }")

# 2. Add missing switch import
if "import com.composables.ui.components.Switch as ComposablesSwitch" not in content:
    content = content.replace("import com.safarparmar.app.R", "import com.safarparmar.app.R\nimport com.composables.ui.components.Switch as ComposablesSwitch")

# 3. Fix isDarkTheme in Dialogs
content = re.sub(
    r'containerColor = if \(isDarkTheme\) Color\(0xFF111111\) else Color\(0xFFF7F8FA\),',
    r'containerColor = if (LocalPlannerIsDarkTheme.current == true) Color(0xFF111111) else Color(0xFFF7F8FA),',
    content
)
# Wait, I DO want isDarkTheme for SafarDrawerScaffold, because it is available there.
# Let's just blindly replace it everywhere, since LocalPlannerIsDarkTheme is valid everywhere in this file.

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
