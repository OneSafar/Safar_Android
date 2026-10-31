# Streaks corrections after device feedback

Active Streaks tiles now use explicit vector icons rendered by Composables UI Icon: outlined flame in a mint tile, bar chart in lilac for monthly health and consistency trend, calendar in peach, and a warning icon in the restore notice. The former monthly icon drawable had no path fill; global drawable files are unchanged.

Added the mint percentage badge, bundled Poppins typography and bold Streaks toolbar title, calendar title/month grouped alongside bordered navigation buttons, more compact calendar row spacing and weekly chart height. Restore remains a genuine eligibility-dependent notice with the existing handler, disabled while restoring. No missed day or sample streak is fabricated. Existing live score calculation, date history and weekly completed-goal counts remain unchanged. Font/tab refinements retain the Journal correction.

Source-only checks verified active icons, XML, locale key parity, brace/import consistency, restore eligibility and loading guards. No builds, Kotlin compilation, tests or device inspection were run. Home, backend and existing feature navigation were not edited.
