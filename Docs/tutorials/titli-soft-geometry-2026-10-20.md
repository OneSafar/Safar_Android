# Approved Soft Geometry tutorial design

Applied the selected `Docs/design/titli-mockups/03-soft-geometry.png` direction natively across the shared feature-tutorial system:

- Blush canvas, white rounded lesson cards, plum typography/actions, peach icon tiles, mint completion badges, and an explicit matching dark palette.
- Shared `TutorialLessonCard` with lesson-specific icons, unchanged replay/completion state, and chevrons for uncompleted lessons. Ekagra, Goals, and all ten shared practice features use it.
- Solid coral wave, yellow arch, lilac rounded block, sage leaves and plum/pink accents replace the prior line-art families. Four arrangements share the approved palette and are assigned consistently by feature. Decoration remains below measured menu content and does not occupy layout space.
- Header Help/Close controls use soft circular surfaces; Titli remains its existing Kotlin-drawn black/pink butterfly.
- Global Titli feature directory, introduction, help, invitations, busy explanations, spotlight bubble, and information dialog use the shared scoped visual style.

`TutorialVisualStyle` is scoped to tutorial chrome. The active native feature stage retains its existing theme, bounds, demo state, backdrop, and bottom navigation. Only the lesson-selection state gets the new menu canvas. Spotlight movement, target registration, input isolation, navigation, persistence, and safe-practice actions are unchanged. No bitmap assets or new dependencies were added.

Validation is source-only in `/tmp/safar-habit-checks-20261020`. Contrast tests cover text, secondary copy, action labels and icon/completion tiles in both modes; artwork tests cover the four arrangements and opaque filled colors. Device visual and TalkBack review remain manual. No APK/AAB or emulator run is performed.

Final verification passed: 20 relevant JVM tests (zero failures, errors or skips), debug/release Kotlin compilation, instrumentation-test source compilation, and release lint (zero errors; 906 warnings and 8 hints). All 927 source/resource/test files match the isolated validation checkout. `git diff --check` passed. Full output: `Outputs/logs/titli-soft-geometry-2026-10-20/verified-checks.log`. Instrumentation tests were compiled but not run on a device.
