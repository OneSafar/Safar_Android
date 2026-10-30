# Exam Planner tablet dashboard

The selected reference is `selected-dashboard-reference.png`.

Landscape tablet windows at least 840dp wide use the shared Exam Planner top navigation: Plan, Home, Syllabus, Calendar, Progress. The standalone Syllabus route and tutorial navigation use the same component. Phone and portrait navigation retain their existing presentation.

Home uses a horizontal progress/stat summary, an unassigned-topics warning, and a 67/33 agenda/checklist split with a 20dp gutter. Both lower panes scroll independently. The finish-day action stays below the agenda at a bounded 190–260dp width and at least 48dp height. The existing plan settings, topic actions, scheduling, checklist editing, and completion callbacks remain wired to their original state and actions.

The navigation adapts the Composables UI Tabs documentation using the project's existing dependency: https://composables.com/ui/docs/tabs . No dependency changes were required.

Visual review is intentionally left to the user. No screenshots or device captures were taken.

Manual review suggestions:
- Switch between all five tabs and confirm a consistent top navigation without a bottom navigation bar in tablet landscape.
- Check Home's Today and Done lists, long topic/subject labels, scrolling, and the finish-day action.
- Check daily task add/edit/complete/delete and warning → scheduling.
- Open Syllabus both from its tab and from its standalone route.
- Rotate to portrait and check the existing layout; also review dark mode and larger system text.

Validation passed: `:app:compileQaDebugKotlin`, `:app:assembleQaDebug`, and `:app:testQaDebugUnitTest --tests 'com.safarparmar.app.ui.studyplanner.*'`.
Planner tests: 32; failures: 0; errors: 0; skipped: 0.

## Contrast and navigation update

Neutral dividers, card borders, and control outlines use opaque #000000 within Exam Planner. The feature scope covers the main tabs, standalone Syllabus, creation flow, and their dialogs. Shared consumers outside the planner retain their original styling. Status colors and progress-track colors are preserved.

Top navigation: minimum height 56 → 61.6dp (+10%); tab target minimum height 48 → 52.8dp (+10%); icons 20 → 23dp (+15%); labels 13 → 14.95sp (+15%); title 18 → 20.7sp (+15%). Added 16dp horizontal outer padding and 4dp per-tab horizontal padding, with extra vertical breathing room.

Validation: `./gradlew :app:assembleQaDebug --console=plain` passed. Visual review left to the user. APK: `Safar-Exam-Planner-Contrast-QA.apk`.
