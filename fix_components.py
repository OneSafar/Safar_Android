import re

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

def replace_between(content, start_marker, end_marker, replacement):
    start_idx = content.find(start_marker)
    if start_idx == -1: return content
    
    # We want the FIRST end_marker AFTER start_idx
    end_idx = content.find(end_marker, start_idx)
    if end_idx == -1: return content
    
    return content[:start_idx] + replacement + content[end_idx + len(end_marker):]

# 1. MacOSControlCard
card_start = "@Composable\nprivate fun MacOSControlCard("
card_end = "    }\n}\n"
card_new = """@Composable
private fun MacOSControlCard(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color(0xFFE2E8F0)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}
"""
text = replace_between(text, card_start, card_end, card_new)

# 2. MacOSControlButton
btn_start = "@Composable\nprivate fun MacOSControlButton("
# Look for the ending of MacOSControlButton. It ends right before MacOSButton
btn_end = "    }\n}\n"
# Actually, the previous function ends with `        }\n    }\n}\n`
# Let's just use string replace for MacOSControlButton since it's exactly between `private fun MacOSControlButton(` and `@Composable\nprivate fun MacOSButton(`
# Let's extract it carefully
btn_full_start = text.find("@Composable\nprivate fun MacOSControlButton(")
btn_full_end = text.find("@Composable\nprivate fun MacOSButton(", btn_full_start)

if btn_full_start != -1 and btn_full_end != -1:
    btn_new = """@Composable
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                if (iconVector != null) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else if (iconLetter != null) {
                    Text(
                        text = iconLetter,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

"""
    text = text[:btn_full_start] + btn_new + text[btn_full_end:]

# 3. MacOSButton
macbtn_start = text.find("@Composable\nprivate fun MacOSButton(")
macbtn_end = text.find("    }\n}\n", macbtn_start) + 6

macbtn_new = """@Composable
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
        modifier = modifier.fillMaxWidth().height(50.dp),
        style = com.composables.ui.components.ButtonStyle.Primary,
    ) {
        if (icon != null) {
            com.composables.ui.components.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        com.composables.ui.components.Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}
"""
text = text[:macbtn_start] + macbtn_new + text[macbtn_end:]

# Replace SafarButton in CelebrationDialog
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
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp,
                        )
                    }"""
text = text.replace(safar_button_old, safar_button_new)

# Second SafarButton
safar_button_old2 = """                com.safarparmar.app.ui.components.SafarButton(
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
                        text = stringResource(R.string.dashboard_lets_begin),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                    )
                }"""
safar_button_new2 = """                com.composables.ui.components.Button(
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
                        text = stringResource(R.string.dashboard_lets_begin),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                    )
                }"""
text = text.replace(safar_button_old2, safar_button_new2)

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)

