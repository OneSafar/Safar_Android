import re

def replace_function(content, func_name, new_code):
    # Find the start of the function: @Composable\nprivate fun FuncName( or similar
    pattern = r'@(?:OptIn\([^)]+\)\s*)?Composable\s+(?:private\s+)?fun\s+' + func_name + r'\s*\('
    match = re.search(pattern, content)
    if not match:
        print(f"Could not find {func_name}")
        return content
    
    start_idx = match.start()
    
    # Find the matching closing brace for the function
    brace_count = 0
    in_function = False
    end_idx = -1
    
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            if not in_function:
                in_function = True
            brace_count += 1
        elif content[i] == '}':
            brace_count -= 1
            if in_function and brace_count == 0:
                end_idx = i + 1
                break
                
    if end_idx != -1:
        return content[:start_idx] + new_code + content[end_idx:]
    else:
        print(f"Could not find end of {func_name}")
        return content


with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# 1. Imports
imports = """
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Layers
"""
content = content.replace("import androidx.compose.material.icons.filled.Security", imports.strip() + "\nimport androidx.compose.material.icons.filled.Security")

# 2. Container color
content = content.replace(
    "containerColor = SafarSemanticColors.plannerBackground(),",
    "containerColor = if (isDarkTheme) Color(0xFF111111) else Color(0xFFF7F8FA),"
)

# 3. Dividers
content = content.replace("PlanHairline(alpha = 0.5f)", "")
content = content.replace("PlanHairline(alpha = 0.4f)", "GroupDivider()")

# 4. Inject specific icons into calls
# Course Updates
content = content.replace(
    "onCheckedChange = { onEvent(SettingsEvent.ToggleCourseUpdates(it)) },",
    "onCheckedChange = { onEvent(SettingsEvent.ToggleCourseUpdates(it)) },\n                icon = Icons.Default.School,"
)
# Timer Updates
content = content.replace(
    "onCheckedChange = { onEvent(SettingsEvent.ToggleFocusTimerNotifications(it)) },",
    "onCheckedChange = { onEvent(SettingsEvent.ToggleFocusTimerNotifications(it)) },\n                icon = Icons.Default.Timer,"
)
# Daily Reminder
content = content.replace(
    "onCheckedChange = { onEvent(SettingsEvent.ToggleDailyStudyReminder(it)) },",
    "onCheckedChange = { onEvent(SettingsEvent.ToggleDailyStudyReminder(it)) },\n                icon = Icons.Default.Schedule,"
)
# Streak Warning
content = content.replace(
    "onCheckedChange = { onEvent(SettingsEvent.ToggleStreakReminder(it)) },",
    "onCheckedChange = { onEvent(SettingsEvent.ToggleStreakReminder(it)) },\n                icon = Icons.Default.LocalFireDepartment,"
)
# Mehfil Replies
content = content.replace(
    "onCheckedChange = { onEvent(SettingsEvent.ToggleCommunityReplies(it)) },",
    "onCheckedChange = { onEvent(SettingsEvent.ToggleCommunityReplies(it)) },\n                icon = Icons.Default.Forum,"
)

# Usage Access
content = content.replace(
    "isGranted = hasUsagePermission,",
    "isGranted = hasUsagePermission,\n            icon = Icons.Default.QueryStats,"
)
# Display over apps
content = content.replace(
    "isGranted = hasOverlayPermission,",
    "isGranted = hasOverlayPermission,\n            icon = Icons.Default.Layers,"
)
# System Notifications
content = content.replace(
    "isGranted = hasNotificationPermission,",
    "isGranted = hasNotificationPermission,\n                icon = Icons.Default.Notifications,"
)

# 5. Functions

# Add GroupDivider and SettingsSquircleIcon at the end of the file
helper_funcs = """

@Composable
private fun GroupDivider() {
    val isDark = LocalPlannerIsDarkTheme.current == true
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp, end = 0.dp),
        thickness = 0.6.dp,
        color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    )
}

@Composable
fun SettingsSquircleIcon(
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}
"""
content += helper_funcs


sheet_new = """@Composable
private fun SettingsSheetSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 14.dp, bottom = 6.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color(0xFFA0A0A0) else Color(0xFF6B7280)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDark) Color(0xFF1E1E1E) else Color.White,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}"""
content = replace_function(content, "SettingsSheetSection", sheet_new)

premium_new = """@Composable
private fun PremiumStatusSection(
    isPremiumActive: Boolean,
    onExplorePremium: () -> Unit,
    onRestoreStatus: () -> Unit,
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    val statusTitle = stringResource(if (isPremiumActive) R.string.settings_premium_active else R.string.settings_plus_plan)
    val statusSubtitle = stringResource(if (isPremiumActive) R.string.settings_premium_unlocked else R.string.settings_free_active)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExplorePremium)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            SettingsSquircleIcon(
                icon = androidx.compose.material.icons.Icons.Rounded.WorkspacePremium,
                iconTint = SafarSemanticColors.brandPurple(),
                backgroundColor = if (isDark) SafarSemanticColors.brandPurple().copy(alpha = 0.15f) else Color(0xFFF3E8FF)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = statusTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = statusSubtitle,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted
                )
            }
        }
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
            contentDescription = null,
            tint = PlannerFlatColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}"""
content = replace_function(content, "PremiumStatusSection", premium_new)

switch_new = """@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                SettingsSquircleIcon(
                    icon = icon,
                    iconTint = if (isDark) Color(0xFFE5E7EB) else Color(0xFF4B5563),
                    backgroundColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF3F4F6)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = PlannerFlatColors.TextMuted
                    )
                }
            }
        }

        ComposablesSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            accessibilityLabel = title,
        )
    }
}"""
content = replace_function(content, "SettingsSwitchRow", switch_new)

nav_new = """@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color? = null,
    iconBg: Color? = null
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    val defaultIconTint = if (isDark) Color(0xFFE5E7EB) else Color(0xFF4B5563)
    val defaultIconBg = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF3F4F6)
    
    val finalIconTint = iconTint ?: defaultIconTint
    val finalIconBg = iconBg ?: defaultIconBg
    
    val isDelete = title.contains("Delete", ignoreCase = true)
    val activeTint = if (isDelete) Color(0xFFEF4444) else finalIconTint
    val activeBg = if (isDelete) Color(0xFFFEE2E2) else finalIconBg
    val activeTitleColor = if (isDelete) Color(0xFFEF4444) else PlannerFlatColors.TextDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            SettingsSquircleIcon(
                icon = icon,
                iconTint = activeTint,
                backgroundColor = if (isDark && isDelete) Color(0xFF5F1717) else activeBg
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeTitleColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted
                )
            }
        }
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.ChevronRight,
            contentDescription = null,
            tint = PlannerFlatColors.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}"""
content = replace_function(content, "SettingsNavigationRow", nav_new)


perm_new = """@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    icon: ImageVector = androidx.compose.material.icons.Icons.Default.Security
) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGrantClick)
            .padding(vertical = 10.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            SettingsSquircleIcon(
                icon = icon,
                iconTint = if (isDark) Color(0xFFE5E7EB) else Color(0xFF4B5563),
                backgroundColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFF3F4F6)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlannerFlatColors.TextDark
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = PlannerFlatColors.TextMuted
                )
            }
        }

        if (isGranted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.settings_granted),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SafarSemanticColors.brandPurple())
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.settings_grant),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafarSemanticColors.brandOnPurple()
                )
            }
        }
    }
}"""
content = replace_function(content, "PermissionRow", perm_new)

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)

