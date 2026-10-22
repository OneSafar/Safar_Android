# Titli falsely reports a busy screen

The shared practice launcher registered references to local `start` and `lessons` functions. Compose retained the function references from the initial loading frame, so the readiness callback could report ready while the launch callbacks still used `eligible = false`. Tapping Titli then showed “Finish or close the current activity” on an idle screen.

Replaced those references with explicit callbacks reading `rememberUpdatedState(eligible)` at tap time. Genuine busy-flow restrictions are unchanged. The fix applies to all ten features using `ScreenPracticeEntry`, including Study Circle, the Nishtha guides, and Exam Planner.

`ScreenPracticeLaunchTest` uses the real Compose recomposer and guide registry without Android UI. It checks all ten features through loading → ready → busy, invokes retained start and lesson-picker callbacks, and verifies disposal removes registrations. A temporary copy using the old local-function-reference pattern failed at the first feature; the corrected implementation passed. The temporary reproducer is not included in app sources. Its expected failure is recorded in `Outputs/logs/titli-launch-fix-2026-10-20/before-fix-reproduction.xml`.

Validation runs in `/tmp/safar-habit-checks-20261020` to avoid shared build-output conflicts. No emulator/device or APK/AAB packaging task is used.

Final validation: 775 JVM tests across 131 suites passed with zero failures/errors/skips; full instrumentation-source compilation and debug/release compilation including Hilt passed. Release lint passed with zero errors (906 warnings and 8 hints remain). All 912 app source files match the validated isolated copy. Full invocation/output: `Outputs/logs/titli-launch-fix-2026-10-20/verified-checks.log`.
