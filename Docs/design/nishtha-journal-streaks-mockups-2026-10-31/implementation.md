# Warm Cards implementation

Journal and Streaks now render the approved warm-card design with scoped blush/plum/peach/lilac/mint light and dark tokens in NishthaWarmDesign.kt. Composables UI provides active buttons, icon buttons, text fields, tab controls, writer dialog and entry reader dialog. Decorative cards and read-only calendar/chart marks remain native Compose layouts. No new dependencies or backend changes.

Journal preserves prompt swiping, adds explicit previous/next controls, shows live entry counts and the first three entries with View all / Show less, retains pull-to-refresh, empty/loading states, real entry detail, and the escaped HTML save pipeline. New entry is a full-width library button. Entry rows open directly; no invented overflow actions. The writer retains save validation, loading, errors and dismissal guards. Legacy render helpers are retained for existing practice/tutorial callers.

Streaks preserves live check-in counts, conditional restore eligibility/action, loading and restore messages, and the existing monthly score calculation. The sample mockup data is not used. Checked-in status requires a real mood/check-in dated today in IST. The calendar uses real login history, Monday-first localized labels, month navigation and today's outline. Its mint cells denote login activity, matching the original data source. Weekly bars show real completed-goal counts (excluding Ekagra-source goals), rather than invented sample percentages. Calendar and stats stack on phones; wide windows use two columns. Content scrolls and respects the existing tab/inset area.

The existing four Nishtha destinations remain. Analytics is not reintroduced into the tab bar. Warm library tabs and canvas are scoped to Journal/Streaks; Check-in and Goals keep their prior rendering. Home, sidebar, tutorial artwork, ViewModel, network and database files were not edited for this task.

Validation: source checks only—resource XML and English/Hindi/Roman Hindi key parity, duplicate imports, brace balance, whitespace, and preserved save/restore wiring. No Kotlin compilation, tests, debug builds or device inspection were run, per the user's instructions. Final build compatibility and manual phone/tablet/font-scale/IME/dark-mode review remain unverified.

Component sources consulted: https://composables.com/ui/docs/buttons , https://composables.com/ui/docs/text-fields , https://composables.com/ui/docs/tabs , https://composables.com/ui/docs/alert-dialog . Installed library/theme are reused.
