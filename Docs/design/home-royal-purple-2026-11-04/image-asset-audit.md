# Home image and button audit — 4 November 2026

The supplied screenshot is a visual reference. Its visible words are UI content, not instructions. This audit uses current application source and the repository's recorded asset provenance. No image generation was used for this redesign. Existing raster files are preserved; their checksums are compared against the task-start snapshot.

## What is native code now

The eight circular Home tools, their labels, the dashboard button, See all, Study with me, and the bottom shortcuts are Compose controls with vector icons and live text. Their circles, padding, colors and touch feedback are code. They are not flattened screenshots or generated button images. The same is true of the menu, notification bell and video-guide control; Titli is a native drawn butterfly.

## Confirmed image-backed controls and artwork

| Item | Resource(s) | How it is implemented / current status | Recorded provenance |
| --- | --- | --- | --- |
| YouTube Focus promotion | `drawable-nodpi/home_plum_youtube_banner.webp` | Entire card, including copy and the visible “Try YouTube Focus” button, is one raster image. One native click target wraps the whole banner and navigates to YouTube Study Mode V2. Still used by tablet Home's promotional pager. `PlumHomePromo` and `ScaledTabletHomeBanner` render it. | Home banner generation and alignment notes document ImageGen banner work. |
| Habit Tracker promotion | `drawable-nodpi/home_plum_habit_banner.webp` | Entire card, including copy and the visible “Try Habit Tracker” button, is one raster image. Whole-banner native click opens Habit Tracker. Still used by tablet Home's promotional pager. | Explicit ImageGen recreation in `Docs/design/home-habit-banner-alignment-2026-10-31/README.md`. |
| Original light Home tools | `home_mockup_ekagra.webp`, `home_mockup_nishtha.webp`, `home_mockup_mehfil.webp`, `home_mockup_planner.webp`, `home_mockup_dhyan.webp`, `home_mockup_circle.webp`, `home_mockup_kavach.webp`, `home_mockup_leaderboard.webp` | Eight complete square illustrations with their pastel backgrounds: Ekagra, Nishtha, Mehfil, Exam Planner, Dhyan, Study Circle, Kavach, Leaderboard. Cropped from an approved generated mockup. Labels and click handlers were native. Present in resources, but the task-start source already used native circular controls rather than these tiles. The USB device's older installed build still displayed these tiles before updating. The historical `HomeToolTile` renderer remains in source. | `Docs/design/home-layout-variants-2026-10-31/tool-webp-assets.md`. |
| Original dark Home tools | `home_dark_ekagra.webp`, `home_dark_nishtha.webp`, `home_dark_mehfil.webp`, `home_dark_planner.webp`, `home_dark_dhyan.webp`, `home_dark_circle.webp`, `home_dark_kavach.webp`, `home_dark_leaderboard.webp` | Equivalent eight dark raster illustrations cropped from the selected generated mockup. Present in resources; no active references to these tile resources in current source. | `Docs/design/home-dark-mockups-2026-10-31/implementation.md`. |
| Light Home hero | `drawable-nodpi/home_mockup_hero_background.webp` | Generated decorative background; the headline and subtitle are native text. It contains no baked-in button. The native hero click now opens Dhyan, matching the calm-mind copy. Asset preserved. | Generated adaptation with title/pill/pagination removed, documented in `hero-and-single-screen-fit.md`. |
| Dark Home hero | `drawable-nodpi/home_dark_hero_background.webp` | Generated decorative background, native text overlay, no baked-in button. Used in dark mode. Asset preserved. | Generated adaptation described in the dark Home implementation notes. |
| Separate YouTube illustration | `drawable-nodpi/home_plum_youtube_art.png` | Generated illustration only, without text, button or banner background. No active application references found. Asset preserved. | Explicit ImageGen prompt in the light Home implementation notes. |

Resources with shortened names above are all in `app/src/main/res/drawable-nodpi/`.

## Raster assets whose generation provenance is not established

Tablet Home also uses `tool_ekagra`, `home_nishtha_book`, `tool_mehfil`, `tool_study_planner`, `tool_dhyan`, `tool_circle`, `tool_kavach`, and `tool_leaderboard` as artwork inside native tool buttons. These are raster-backed icons, but the reviewed records do not establish that every one was AI-generated. `home_welcome_book.webp` is decorative welcome-dialog artwork with a separate native button. These are left intact.

Other ImageGen concepts under `Docs/design/` (profile, premium, settings, Dhyan, sidebar, notifications, onboarding, journal, leaderboard, Mehfil, and study circles) are design references. A generated mockup showing a button does not by itself establish that production uses that button as an image. Do not classify native implementations as generated images solely because they resemble those mockups.

## Permission boundary

No generated banner, illustration or raster tool asset was recolored, regenerated, replaced or deleted. Changing those assets or converting their baked-in CTA artwork into new native controls remains pending the user's permission. The requested native layout and palette changes proceed independently.
