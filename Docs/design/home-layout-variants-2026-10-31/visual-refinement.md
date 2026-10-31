# Phone Home visual correction

The supplied device screenshot showed a different font, lighter typography, a left-aligned title, artwork extending behind the carousel, small labels and generic Material tool icons. The global PoppinsFontFamily alias now resolves to Hanken Grotesk. Phone Home now explicitly uses the existing bundled Poppins regular/medium/semibold/bold files locally; global typography is unchanged.

Corrected the title alignment, headline weight/size, tool heading and labels, plum/peach/lilac tones, banner text/button sizes and independent right-hand illustration placement. Hero artwork is clipped at the hero bounds. The video control has a pale circular background; notifications use an outline bell while retaining real unread badges. Eight native vector tool icons replace the generic defaults, including lotus, rounded bars, study-circle people and locked shield.

Carousel timing and real page counts remain intact: the submitted screenshot happens to show the Habit Tracker slide, while the approved image shows YouTube Focus. Both remain reachable. No fake extra pagination dots or fixed unread count were introduced.

Source-only checks verified vector XML, font assets, eight tool entries, destination callbacks, and unchanged tablet Home renderer checksum. No Kotlin compilation, debug build, tests or phone inspection was performed. Device visual verification remains with the user; pixel matching has not been independently verified.
