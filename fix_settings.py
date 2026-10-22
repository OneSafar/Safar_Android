import re

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Fix LocalPlannerIsDarkTheme.current
content = content.replace("val isDark = LocalPlannerIsDarkTheme.current\n", "val isDark = LocalPlannerIsDarkTheme.current == true\n")

# Import missing icons
imports = """import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Layers
"""
content = content.replace("import androidx.compose.material.icons.filled.Security", imports + "import androidx.compose.material.icons.filled.Security")


# Let's fix the syntax errors in PermissionRow and SettingsSwitchRow calls from bad regex
# I will just revert the file to git state, and apply my replacements cleanly!
