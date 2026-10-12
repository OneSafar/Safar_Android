import re

with open('app/src/main/java/com/safarparmar/app/ui/premium/PremiumPaywallScreen.kt', 'r') as f:
    content = f.read()

# 1. Add helper composables at the bottom
helpers = """

@Composable
private fun PremiumSectionTitle(title: String) {
    val isDark = LocalPlannerIsDarkTheme.current == true
    Text(
        text = title,
        modifier = Modifier.padding(start = 14.dp, bottom = 6.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isDark) Color(0xFFA0A0A0) else Color(0xFF6B7280)
    )
}

@Composable
private fun PremiumStyleGroup(content: @Composable ColumnScope.() -> Unit) {
    val isDark = LocalPlannerIsDarkTheme.current == true
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

@Composable
private fun GroupDivider() {
    val isDark = LocalPlannerIsDarkTheme.current == true
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp, end = 0.dp),
        thickness = 0.6.dp,
        color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    )
}
"""
if "fun PremiumStyleGroup" not in content:
    content += helpers

# 2. Replace Scaffold containerColor
content = content.replace("containerColor = SafarSemanticColors.plannerBackground(),", "containerColor = if (isDarkTheme) Color(0xFF111111) else Color(0xFFF7F8FA),")
content = content.replace("containerColor = SafarSemanticColors.plannerBackground()", "containerColor = if (isDarkTheme) Color(0xFF111111) else Color(0xFFF7F8FA)")

# 3. Replace all Cards that have CardWhite container with PremiumStyleGroup
# Example:
# Card(
#     modifier = Modifier.fillMaxWidth(),
#     shape = RoundedCornerShape(18.dp),
#     colors = CardDefaults.cardColors(containerColor = PlannerFlatColors.CardWhite),
#     border = ...
# ) {
card_pattern = re.compile(r'Card\(\s*modifier\s*=\s*Modifier\.fillMaxWidth\(\),\s*shape\s*=\s*RoundedCornerShape\([^)]+\),\s*colors\s*=\s*CardDefaults\.cardColors\(containerColor\s*=\s*PlannerFlatColors\.CardWhite\),(?:\s*border\s*=[^,]*,?)?\s*\)\s*\{', re.MULTILINE)
content = card_pattern.sub('PremiumStyleGroup {', content)


# 4. Replace RadioPlanSelector using the block replace method
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

radio_new = """@Composable
private fun RadioPlanSelector(
    plans: List<PremiumPlanOption>,
    selectedPlanId: String,
    isDarkTheme: Boolean = false,
    onSelectPlan: (String) -> Unit,
) {
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)

    PremiumStyleGroup {
        plans.forEachIndexed { index, plan ->
            val selected = selectedPlanId == plan.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) accent.copy(alpha = 0.08f) else Color.Transparent)
                    .clickable { onSelectPlan(plan.id) }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.safarparmar.app.ui.components.SafarRadioIndicator(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = accent,
                        unselectedColor = PlannerFlatColors.TextMuted,
                    ),
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = plan.label,
                            color = PlannerFlatColors.TextDark,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (plan.badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = accent.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = plan.badge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = plan.subtitle,
                        color = PlannerFlatColors.TextMuted,
                        fontSize = 13.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (plan.originalPrice != null) {
                        Text(
                            text = "₹${plan.originalPrice}",
                            color = PlannerFlatColors.TextMuted,
                            fontSize = 12.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                    Text(
                        text = "₹${plan.price}",
                        color = if (selected) accent else PlannerFlatColors.TextDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (index != plans.lastIndex) {
                GroupDivider()
            }
        }
    }
}"""
content = replace_function(content, "RadioPlanSelector", radio_new)

selected_new = """@Composable
private fun SelectedPlanCard(
    plan: PremiumPlanOption,
    currentExpiryText: String?,
    newExpiryText: String?,
    isDarkTheme: Boolean = false,
) {
    val accent = if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF581C87)

    PremiumStyleGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plan.label,
                    color = PlannerFlatColors.TextDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = plan.subtitle,
                    color = PlannerFlatColors.TextMuted,
                    fontSize = 13.sp
                )
                if (newExpiryText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.premium_renews_on_date, newExpiryText),
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = "₹${plan.price}",
                color = PlannerFlatColors.TextDark,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}"""
content = replace_function(content, "SelectedPlanCard", selected_new)

with open('app/src/main/java/com/safarparmar/app/ui/premium/PremiumPaywallScreen.kt', 'w') as f:
    f.write(content)

