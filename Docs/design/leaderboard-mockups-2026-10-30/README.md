<!-- Hallmark · pre-emit critique: P4 H4 E4 S5 R4 V5 -->
# Leaderboard concept mockups

1. Warm podium steps: light card and flat peach/lavender stepped pedestals.
2. Plum stage: plum hero, crowned winner and flat stepped platforms.
3. Portrait podium: stepped portrait cards, gold winner border, plum user summary.

ImageGen concepts only; implementation has not changed. Reference: supplied leaderboard screenshot. Sample names, ranks, periods and durations from screenshot; images illustrative. User summary uses the supplied 2m and rank 2548 without repeating the contradictory unranked message in the screenshot.

Backend contract inspected: WeeklyLeaderboardResponseDto contains period, podiumPeriod, podium, entries, currentUserRank, currentUserEntry, page and totalPages. Designs distinguish previous-week podium from current live list. Final UI must bind all values dynamically, handle absent avatars and missing podium places, null current-user entries, loading/error/empty states, long localized names, and pagination boundaries. No backend changes needed for these concepts. No added XP, trends, streaks or prize metrics.

Target implementation: existing Compose components with Composables UI buttons and theme, Coil avatars and vector shapes/crowns. Raster concept lighting is not a requirement; use flat brand tokens. Runtime sizing and larger fonts need verification after selection.
