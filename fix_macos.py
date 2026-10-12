import re

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

# Let's replace MacOSControlCard definition completely
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

new_card = """@Composable
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
}"""
content = replace_function(content, "MacOSControlCard", new_card)

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)

