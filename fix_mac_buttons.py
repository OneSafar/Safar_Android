import re

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

def replace_function(content, func_name, new_code):
    pattern = r'@Composable\s+(?:private\s+)?fun\s+' + func_name + r'\s*\('
    match = re.search(pattern, content)
    if not match:
        return content
    start_idx = match.start()
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
    return content

new_control_button = """@Composable
private fun MacOSControlButton(
    iconVector: ImageVector? = null,
    iconLetter: String? = null,
    title: String,
    subtitle: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = Color(0xFF0A84FF),
    onClick: () -> Unit = {}
) {
    val textColor = DashboardFlatColors.onGlassText(isDarkTheme)
    val subtitleColor = DashboardFlatColors.onGlassMuted(isDarkTheme)

    Surface(
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (iconVector != null) {
                    Icon(iconVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else if (iconLetter != null) {
                    Text(
                        text = iconLetter,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}"""

new_macos_button = """@Composable
private fun MacOSButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    buttonColor: Color = Color(0xFF0A84FF),
) {
    com.composables.ui.components.Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        style = com.composables.ui.components.ButtonStyle.Primary,
    ) {
        if (icon != null) {
            com.composables.ui.components.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        com.composables.ui.components.Text(text)
    }
}"""

content = replace_function(content, "MacOSControlButton", new_control_button)
content = replace_function(content, "MacOSButton", new_macos_button)

# Also fix the SafarButton that's directly used in CelebrationDialog
# It is inside CelebrationDialog, I can just regex replace it or let's use a simpler text replace
safar_button_old = """                    com.safarparmar.app.ui.components.SafarButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonBg,
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dashboard_awesome),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp,
                        )
                    }"""
                    
safar_button_new = """                    com.composables.ui.components.Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        style = com.composables.ui.components.ButtonStyle.Primary,
                    ) {
                        com.composables.ui.components.Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        com.composables.ui.components.Text(
                            text = stringResource(R.string.dashboard_awesome),
                        )
                    }"""
content = content.replace(safar_button_old, safar_button_new)

# Similarly for the SafarButton in MonthlyCard
# Wait, let's just find them and replace them.

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)

