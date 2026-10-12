import re

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Add icons to SettingsSwitchRow calls
content = re.sub(
    r'(SettingsSwitchRow\(\s*title = stringResource\(R\.string\.settings_timer_updates\),.*?onCheckedChange =.*?)(,)?(\s*\))',
    r'\1,\n                icon = androidx.compose.material.icons.Icons.Default.Timer\n            )',
    content, flags=re.DOTALL
)

content = re.sub(
    r'(SettingsSwitchRow\(\s*title = stringResource\(R\.string\.settings_daily_reminder\),.*?onCheckedChange =.*?)(,)?(\s*\))',
    r'\1,\n                icon = androidx.compose.material.icons.Icons.Default.Schedule\n            )',
    content, flags=re.DOTALL
)

content = re.sub(
    r'(SettingsSwitchRow\(\s*title = stringResource\(R\.string\.settings_streak_warning\),.*?onCheckedChange =.*?)(,)?(\s*\))',
    r'\1,\n                icon = androidx.compose.material.icons.Icons.Default.LocalFireDepartment\n            )',
    content, flags=re.DOTALL
)

content = re.sub(
    r'(SettingsSwitchRow\(\s*title = stringResource\(R\.string\.settings_course_updates\),.*?onCheckedChange =.*?)(,)?(\s*\))',
    r'\1,\n                icon = androidx.compose.material.icons.Icons.Default.School\n            )',
    content, flags=re.DOTALL
)

content = re.sub(
    r'(SettingsSwitchRow\(\s*title = stringResource\(R\.string\.settings_mehfil_replies\),.*?onCheckedChange =.*?)(,)?(\s*\))',
    r'\1,\n                icon = androidx.compose.material.icons.Icons.Default.Forum\n            )',
    content, flags=re.DOTALL
)

# And add icons to PermissionRow calls
content = re.sub(
    r'(PermissionRow\(\s*title = stringResource\(R\.string\.settings_usage_access\),.*?onGrantClick =.*?\}\s*,?)(\s*\))',
    r'\1,\n            icon = androidx.compose.material.icons.Icons.Default.QueryStats\n        )',
    content, flags=re.DOTALL
)

content = re.sub(
    r'(PermissionRow\(\s*title = stringResource\(R\.string\.settings_display_over_apps\),.*?onGrantClick =.*?\}\s*,?)(\s*\))',
    r'\1,\n            icon = androidx.compose.material.icons.Icons.Default.Layers\n        )',
    content, flags=re.DOTALL
)

content = re.sub(
    r'(PermissionRow\(\s*title = stringResource\(R\.string\.settings_system_notifications\),.*?onGrantClick =.*?\}\s*,?)(\s*\))',
    r'\1,\n                icon = androidx.compose.material.icons.Icons.Default.Notifications\n            )',
    content, flags=re.DOTALL
)

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)

