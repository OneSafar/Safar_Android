import re

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Replace PremiumStatusSection
premium_old = r'''@Composable\s+private fun PremiumStatusSection\(.*?(?=@Composable)'''
premium_new = r'''@Composable
private fun PremiumStatusSection(
    isPremiumActive: Boolean,
    onExplorePremium: () -> Unit,
    onRestoreStatus: () -> Unit,
) {
    val isDark = LocalPlannerIsDarkTheme.current
    val statusTitle = stringResource(if (isPremiumActive) R.string.settings_premium_active else R.string.settings_plus_plan)
    val statusSubtitle = stringResource(if (isPremiumActive) R.string.settings_premium_unlocked else R.string.settings_free_active)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExplorePremium)
            .padding(vertical = 8.dp, horizontal = 14.dp),
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
}

'''
content = re.sub(premium_old, premium_new, content, flags=re.DOTALL)


# Replace SettingsSwitchRow
switch_old = r'''@Composable\s+private fun SettingsSwitchRow\(.*?(?=@Composable)'''
switch_new = r'''@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    val isDark = LocalPlannerIsDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 14.dp),
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
}

'''
content = re.sub(switch_old, switch_new, content, flags=re.DOTALL)


# Replace SettingsNavigationRow
nav_old = r'''@Composable\s+private fun SettingsNavigationRow\(.*?(?=@Composable)'''
nav_new = r'''@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color? = null,
    iconBg: Color? = null
) {
    val isDark = LocalPlannerIsDarkTheme.current
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
            .padding(vertical = 8.dp, horizontal = 14.dp),
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
}

'''
content = re.sub(nav_old, nav_new, content, flags=re.DOTALL)


# Replace PermissionRow
perm_old = r'''@Composable\s+private fun PermissionRow\(.*?(?=@Composable)'''
perm_new = r'''@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    icon: ImageVector = androidx.compose.material.icons.Icons.Default.Security
) {
    val isDark = LocalPlannerIsDarkTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGrantClick)
            .padding(vertical = 8.dp, horizontal = 14.dp),
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
}

'''
content = re.sub(perm_old, perm_new, content, flags=re.DOTALL)

# Add missing Icons.Rounded imports if needed
if "import androidx.compose.material.icons.rounded.WorkspacePremium" not in content:
    content = content.replace("import androidx.compose.material.icons.Icons", "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.rounded.WorkspacePremium")


with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
