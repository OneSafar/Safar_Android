# Expressive tutorial-menu artwork

Replaced the single faint Material-accent mandala with four native vector illustrations:

- **Bloom:** overlapping colored flower petals, an inner rosette, a star center, and dotted trim. Used by Goals, Check-in, and Dhyan.
- **Sunburst:** curved alternating rays, a layered center, colored dots, and small stars. Used by Streaks, Batch Tracker, and YouTube's feature mapping.
- **Orbits:** three tilted orbital ribbons, colored satellite dots, and star accents. Used by Ekagra, Study Circle, and Analytics.
- **Mosaic:** a field of multicolored four-petal tiles with contrasting centers and corner stars. Used by Habits, Journal, Mehfil, and Exam Planner.

Assignment is explicit and consistent across visits. The illustration has its own pink, coral, gold, teal, and violet palettes; it does not take its ink from Material's primary color. Dark mode uses brighter ink, while light mode uses deeper color for contrast. Outlines are 90% opaque with 1.8dp strokes; translucent fills add decorative color. A regression checks every outline color against warm-white/white and near-black/navy canvases at a contrast ratio of at least 3:1.

Geometry is cached in Compose paths. Artwork stays below measured lesson controls, disappears when there is insufficient spare space, adds no touch/accessibility nodes, and has no animation or image/library dependency. Spotlight lesson screens and feature backgrounds are unchanged.

Validation uses source-only checks in the isolated `/tmp/safar-habit-checks-20261020` copy. No APK/AAB or emulator run is requested or performed. On-device visual review remains a manual check.

Validation passed: 20 relevant JVM tests, full instrumentation-source compilation, and debug/release Kotlin compilation. Release lint passed with zero errors (906 warnings and eight hints remain); a final compilation/test pass verifies the shared outline-alpha constant used by both drawing and contrast tests. All 926 source files match the isolated checked copy. Logs are in `Outputs/logs/titli-expressive-artwork-2026-10-20/`. No device visual verification was performed.
