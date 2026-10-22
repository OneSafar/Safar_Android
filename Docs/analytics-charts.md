# Analytics charts

The installed compose-analytics-visualization skill is at ~/.codex/skills/compose-analytics-visualization/SKILL.md.

Lecture completion, weekly habit rhythm, and habit consistency use the app-owned SafarAnalyticsChart adapter and Vico 3.1.0 (https://github.com/patrykandpatrick/vico/tree/v3.1.0). The upstream 3.3.1 release requires Compose 1.12 and AGP 9.1; the workspace used AGP 8.13.2 when compatibility was checked (the toolchain is being updated independently). No upstream source was vendored. Vico is Copyright 2026 Patryk Goworowski and Patrick Michalik, licensed under Apache 2.0; see licenses/Vico-Apache-2.0.txt.

Lecture totals preserve cumulative history and India-time completion dates. Habit consistency includes only scheduled dates in the last 18 days, with actual day offsets to preserve elapsed-time spacing. Weekly rhythm includes zero-completion scheduled days in weekday averages. Empty history displays an explanation instead of fabricated values. Charts use theme-aware labels and tooltips, fixed 0–100% habit axes, touch markers, and the app's reduced-motion policy.
