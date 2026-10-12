with open('app/src/main/java/com/safarparmar/app/ui/nishtha/NishthaScreen.kt', 'r') as f:
    code = f.read()

old_bar = """    Surface(
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
            ) {"""

new_bar = """    Surface(
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
                .height(56.dp)
                .padding(vertical = 4.dp, horizontal = 8.dp),
        ) {
            val totalWidth = maxWidth
            val itemWidth = totalWidth / tabs.size
            val density = LocalDensity.current

            // ── Sliding Pill Indicator (Evaluated in draw phase via graphicsLayer) ──
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = if (isAnalyticsSelected) 0f else 1f
                        translationX = with(density) { (itemWidth * animatedIndex).toPx() }
                    }
                    .width(itemWidth)
                    .fillMaxHeight()
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
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {"""

if old_bar in code:
    code = code.replace(old_bar, new_bar)
    print("SUCCESS: Fixed bottom bar layout and height")
else:
    print("FAILED to find old_bar")

with open('app/src/main/java/com/safarparmar/app/ui/nishtha/NishthaScreen.kt', 'w') as f:
    f.write(code)

