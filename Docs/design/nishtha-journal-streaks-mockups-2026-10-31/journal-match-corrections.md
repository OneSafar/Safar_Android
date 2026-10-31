# Journal corrections after device feedback

The library Button default is Secondary, accounting for the blue Answer prompt and New Entry actions. Journal now explicitly selects ButtonStyle.Primary for primary actions and supplies the rounded shapes from the approved mockup. No library or global theme code changed.

Journal typography now uses the project's bundled Poppins regular/medium/semibold/bold files. The global PoppinsFontFamily alias uses a downloadable Hanken Grotesk family with only one declared weight, so it was unsuitable for matching the supplied visual. The local font is applied to Journal headings, prompt, entry cards, actions, toolbar title, and tab labels while Journal is active.

Added a stroke quill tile, lighter peach prompt surface, orange prompt eyebrow, pale rounded pager tiles and localized dynamic page counter. Two recent entries appear before View all; real data and all seven prompts are retained. Entry cards have alternating peach/lilac notebook tiles, stronger titles and a documented-library overflow with the existing Open entry action. Today/Yesterday labels are used only when the real IST timestamp qualifies; older dates retain their original formatting. No fake edit/delete actions were added.

Validation was source-only: XML, locale parity, bundled font existence, imports, whitespace and preserved save wiring. No compilation, builds, tests or device inspection were run. Manual visual confirmation remains with the user. No Home, backend or Streaks-content changes were made for this correction.
