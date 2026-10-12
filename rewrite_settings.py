import re

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# 1. Update SafarDrawerScaffold container color to the clean F7F8FA (for light)
# In SettingsScreen, find SafarDrawerScaffold containerColor
content = re.sub(
    r'containerColor = SafarSemanticColors\.plannerBackground\(\),',
    r'containerColor = if (isDarkTheme) Color(0xFF111111) else Color(0xFFF7F8FA),',
    content
)

# 2. Remove all PlanHairline calls between sections.
# They look like: PlanHairline(alpha = 0.5f) or PlanHairline(alpha = 0.4f)
# We will leave the ones INSIDE sections (like inside NotificationsSection) to be replaced with our new GroupDivider.
# Actually, let's just replace all PlanHairline(...) inside the file with GroupDivider()
content = re.sub(r'PlanHairline\([^)]*\)', r'GroupDivider()', content)
# Wait, between sections we don't want GroupDivider either!
# Let's see: StaggeredSettingsEntranceBox(index = x) is preceded by PlanHairline(alpha = 0.5f).
content = re.sub(r'GroupDivider\(\)\s*Staggered', r'Staggered', content)

# 3. Update SettingsSheetSection
sheet_section_old = r'''@Composable
private fun SettingsSheetSection\(
    title: String,
    content: @Composable \(\) -> Unit,
\) \{
    Column\(
        modifier = Modifier\.fillMaxWidth\(\),
        verticalArrangement = Arrangement\.spacedBy\(10\.dp\),
    \) \{
        Text\(
            text = title,
            fontSize = 14\.sp,
            fontWeight = FontWeight\.Bold,
            color = PlannerFlatColors\.TextDark,
        \)
        content\(\)
    \}
\}'''

sheet_section_new = r'''@Composable
private fun SettingsSheetSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val isDark = LocalPlannerIsDarkTheme.current
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
}

@Composable
private fun GroupDivider() {
    val isDark = LocalPlannerIsDarkTheme.current
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
'''

content = re.sub(sheet_section_old, sheet_section_new, content)

with open('app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
