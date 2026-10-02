with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

def replace_between(content, start_marker, end_marker, replacement):
    start_idx = content.find(start_marker)
    if start_idx == -1: return content
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
btn_end = "    }\n}\n"
# wait, there might be multiple occurrences of `    }\n}\n`
# I should be more precise
