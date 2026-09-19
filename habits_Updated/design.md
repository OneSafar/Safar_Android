# Design — Safar Habit Tracker

A locked design system for the Safar Habit Tracker. Every habit tracker component and screen adheres to this system.

## Genre
Editorial / Tactile Desk Journal (Paper Ledger)

## Macrostructure Family
App Page: Workbench / Ledger
- Base: Warm Cream Paper ledger background (`#FFFDF6` light / `#141416` dark)
- Masthead: Left-aligned date diary title with integrated progress stamp (`3/5 Completed`)
- Segmented Control: Tactile inset journal tab strip (Today · Weekly · Monthly)
- Rows: Clean rule-divided ledger items with hairline borders, high-contrast typography, and tactile check targets
- Calendar: Square, proportional planner tiles with tabular date figures and crisp stamp fills

## Theme Tokens
- `--color-paper`:       `#FFFDF6` (Warm Cream Paper)  / Dark: `#141416`
- `--color-paper-2`:     `#F7F3E9` (Muted Parchment)   / Dark: `#1E1F24`
- `--color-surface`:     `#FFFFFF` (Crisp Card Surface)/ Dark: `#23242A`
- `--color-ink`:         `#1E1B16` (Deep Charcoal Ink) / Dark: `#F3F4F6`
- `--color-ink-muted`:   `#696357` (Graphite Pencil)   / Dark: `#9CA3AF`
- `--color-rule`:        `#EBE5D8` (Hairline Divider)  / Dark: `#2E2F38`
- `--color-rule-strong`: `#D6CEBE` (Firm Rule)         / Dark: `#3E404D`
- `--color-accent`:      `#581C87` (Royal Purple - max 3% viewport) / Dark: `#C084FC`
- `--color-accent-bg`:   `#EDE9FE` (Soft Purple Tint)  / Dark: `#2E1065`
- `--color-done`:        `#3F6E50` (Sage Stamp Green)  / Dark: `#10B981`
- `--color-done-bg`:     `#EBF5EE` (Soft Sage Wash)    / Dark: `#052E16`
- `--color-error`:       `#B91C1C` (Muted Crimson)     / Dark: `#EF4444`

## Typography
- Masthead: Bold Roman, -0.3.sp tracking
- Headers: Semi-bold Roman, sentence case, tight tracking
- Day Numbers: Tabular numerals with crisp vertical baseline
- Prohibited: No italic headers, no cartoon doodle banners, no rainbow badge letters

## Spacing & Shapes
- 4-point scale: 4.dp, 8.dp, 12.dp, 16.dp, 24.dp
- Borders: Crisp hairline rules (0.75.dp - 1.dp)
- Corners: 16.dp for sheets, 10.dp for ledger rows, 6.dp for calendar tiles
