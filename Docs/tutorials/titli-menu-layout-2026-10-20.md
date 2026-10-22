# Titli menu and lesson-menu design

- Reused one `TutorialLessonMenu` for Ekagra, Goals, and all shared practice features. The existing feature canvas stays unchanged. A static, cached vector mandala draws only below the measured menu content and only when sufficient empty space exists; it has no touch or accessibility node and no animation.
- Aligned the practice-window title and subtext with the lesson content. Titles use 24sp/30sp semibold text; supporting text is 13sp/18sp with theme-derived contrast. Help and Close retain 48dp targets on the title row. Lesson cards use 18dp rounded corners. Generic practices now say “Sample data” rather than “Sample goals.” The Goals label remains specific to its sample goals.
- The Batch Tracker wrapping title now reserves the measured trailing-action width (including Titli) instead of extending underneath it. Existing navigation and tutorial registration are unchanged.
- Replaced the text-only global Titli AlertDialog with a bounded, rounded native app surface: a small mascot beside the greeting, 56dp icon rows with chevrons, a scrollable feature list, and fixed query/support actions. It uses the installed Composables UI Ghost/Outlined buttons and explicit Material foreground colors, following the Buttons documentation, with no dependency changes. All feature routes and test tags remain the same. Added two short labels in English, Hindi and Hinglish.
- Updated the existing menu routing instrumentation test for support actions now outside the scrolling feature list. Instrumentation sources are compiled only; no emulator or device is used for this task.

Validation uses `/tmp/safar-habit-checks-20261020` with no APK/AAB packaging. Physical visual review of the redesigned menu remains a manual check.

Final checks passed: 18 relevant JVM tutorial tests, debug/release Kotlin compilation, full instrumentation-source compilation, and release lint with zero errors (906 warnings and 8 hints remain). All 924 source files match the isolated validation copy. Log: `Outputs/logs/titli-menu-layout-2026-10-20/verified-checks.log`. No APK/AAB or emulator run was produced.
