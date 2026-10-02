with open('app/src/main/java/com/safarparmar/app/ui/nishtha/NishthaScreen.kt', 'r') as f:
    code = f.read()

# 1. Add imports if needed
if "import androidx.compose.ui.platform.LocalDensity" not in code:
    code = code.replace(
        "import androidx.compose.ui.platform.LocalHapticFeedback",
        "import androidx.compose.ui.platform.LocalHapticFeedback\nimport androidx.compose.ui.platform.LocalDensity\nimport androidx.compose.foundation.interaction.MutableInteractionSource"
    )

# 2. Find NishthaBottomBar and replace it
start_marker = "/**\n * Same macOS glass bottom-bar recipe as Exam Planner [PlannerBottomBar]:"
if start_marker not in code:
    start_marker = "@Composable\nprivate fun NishthaBottomBar("

end_marker = "/** Per-tab accents — same role as PlannerTabAccent on Exam Planner. */"

new_bottom_bar = """/**
 * Modern Motion Bottom Navigation Bar with hardware-accelerated graphicsLayer slide,
 * per-tab accent transitions, and bouncy spring micro-interactions.
 */
@Composable
private fun NishthaBottomBar(
    selected: NishthaTab,
    onSelect: (NishthaTab) -> Unit,
) {
    val tabs = remember { NishthaTab.entries.filter { it != NishthaTab.ANALYTICS } }
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.isLightBackground()
    val isDark = !isLight
    val haptic = LocalHapticFeedback.current

    val isAnalyticsSelected = selected == NishthaTab.ANALYTICS
    val selectedIndex = tabs.indexOf(selected).takeIf { it >= 0 } ?: 0

    // High-performance spring slide animation for the sliding indicator pill
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nishthaMotionTabSlide",
    )

    val currentAccent = nishthaTabAccent(tabs.getOrElse(selectedIndex) { NishthaTab.CHECK_IN }, isDark)
    val animatedAccent by animateColorAsState(
        targetValue = currentAccent,
        animationSpec = tween(250),
        label = "nishthaMotionAccent",
    )

    val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB),
                shape = barShape,
            ),
        color = if (isDark) Color(0xFF161618) else Color.White,
        shape = barShape,
        shadowElevation = 0.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 8.dp),
        ) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val tabCount = tabs.size
            val tabWidthPx = if (tabCount > 0) totalWidthPx / tabCount else 0f
            val density = LocalDensity.current

            // ── Sliding Pill Indicator (Evaluated in draw phase via graphicsLayer) ──
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = if (isAnalyticsSelected) 0f else 1f
                        translationX = tabWidthPx * animatedIndex
                    }
                    .width(with(density) { tabWidthPx.toDp() })
                    .height(48.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(animatedAccent.copy(alpha = if (isDark) 0.18f else 0.12f))
                    .border(
                        width = 1.dp,
                        color = animatedAccent.copy(alpha = if (isDark) 0.35f else 0.25f),
                        shape = RoundedCornerShape(14.dp),
                    ),
            )

            // ── Interactive Tab Items with Motion Scale & Bounce ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selected == tab
                    val tabAccent = nishthaTabAccent(tab, isDark)

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "nishthaTabIconScale_${tab.name}",
                    )

                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            tabAccent
                        } else {
                            if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                        },
                        animationSpec = tween(200),
                        label = "nishthaTabContentColor_${tab.name}",
                    )
                    val label = stringResource(tab.labelRes)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelect(tab)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

"""

s_idx = code.find(start_marker)
e_idx = code.find(end_marker, s_idx)
if s_idx != -1 and e_idx != -1:
    code = code[:s_idx] + new_bottom_bar + code[e_idx:]
    print("SUCCESS: Replaced NishthaBottomBar")
else:
    print(f"FAILED: start={s_idx}, end={e_idx}")

with open('app/src/main/java/com/safarparmar/app/ui/nishtha/NishthaScreen.kt', 'w') as f:
    f.write(code)

