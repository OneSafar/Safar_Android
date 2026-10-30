# Exam Planner: v5 design within v4 adaptive layouts

Status: implemented in v4 source on 28 October 2026. See the delivery record below for the completed source changes and the verification limit requested during implementation.

## Intent and boundaries

Use the current working files in Safar_Android_v5 as the Exam Planner visual reference. Implement in Safar_Android_v4. Preserve v4's tablet placements and navigation structure. The repeated v4 reference in request item 6 is interpreted in light of items 1–3 as **v5 visual design + v4 tablet structure**.

The adaptive audit concerns v4 only. v5 is read only as a design/source reference. Existing uncommitted and untracked work in both projects is part of the baseline; do not reset it or replace entire directories.

## Findings from v4

### Adaptation exists and responds to configuration

- `app/src/main/java/com/safarparmar/app/ui/adaptive/SafarAdaptiveState.kt` reads `LocalConfiguration.current`, derives width classes at 600dp and 840dp, and keys remembered state on size, orientation, and tablet status. It does not cache the initial device type forever.
- `MainActivity.kt` installs `ProvideSafarAdaptive`, calls `applyOrientationPolicy()` on startup and configuration changes, and keeps ordinary phones portrait while allowing tablets to rotate.
- The main manifest declares the activity resizable and handles screen size, smallest screen size, screen layout, and orientation changes. This supports live configuration updates; it does not by itself prove correct rendered behavior.
- `SafarDrawerScaffold.kt` changes between a modal drawer and a navigation rail.
- `StudyPlannerScreen.kt`, standalone `SyllabusSubjectsScreen.kt`, and `PlannerPracticeStage.kt` put planner destinations at the top for landscape tablets with window width at least 840dp; smaller/portrait layouts retain bottom navigation.
- v4 has dedicated `TabletPlannerOverview`, `TabletPlanHomeLayout`, and `TabletPlannerCalendar` implementations. Syllabus also has a wide layout.
- Calendar month and selected day are remembered above the layout branch using `rememberSaveable(plan.id)`. Selected plan/section state is owned by the ViewModel. These are useful foundations for retaining context during layout changes.

### Seamless switching is not yet established

1. **Mixed sizing policies.** `isWideScreen` is `widthClass != Compact || isTablet`. When the tablet flag remains true in a compact configuration, shared screens can still request a wide layout. Pane eligibility must depend on usable space; device identity alone cannot authorize a multi-pane layout.
2. **Window width and content width differ.** Planner top tabs use configuration width >=840dp. Overview and Calendar require local content width >=840dp; Today requires window width >=840dp and content width >=760dp. The rail consumes 104dp collapsed or 254.8dp expanded. Thus a landscape tablet can retain top navigation while content collapses, and expanding the rail can change content layout. Some difference is appropriate, but it needs an explicit policy and coverage.
3. **Orientation rules need transition checks.** Activity classification also uses display metrics, whereas Compose classification uses configuration only. `applyOrientationPolicy()` also returns early for explicit landscape requests. Verify fold/unfold, split-screen, and transitions out of requested landscape without changing normal-phone behavior unintentionally.
4. **Branch-owned UI state can reset.** The tablet exam preview selection and independent scroll states live inside their layout branches. `rememberSaveable` inside a removed branch alone does not guarantee restoration when that branch returns. Hoist or explicitly retain the state needed across transitions.
5. **State preservation is partial.** ViewModel state normally survives size changes and activity recreation, but the main planner explicitly persists only the selected plan ID through `SavedStateHandle`. Do not claim selected section or all transient dialogs survive process death without separate work and tests.
6. **Tests do not prove live adaptation.** Existing `SafarAdaptiveLayoutTest` checks profile text and generic bottom-bar spacing at fixed widths. It does not exercise the adaptive provider, planner branches, rail expansion, or a compact → expanded → compact transition. Prior tablet QA notes report builds/tests, and explicitly leave visual review outstanding.

Verdict: v4 has a reactive adaptive foundation, but a guarantee of seamless switching would be premature. Resolve and test these gaps before treating the redesign as complete.

## Layout contract

Use available window dimensions for app navigation, and actual remaining content constraints for individual pane layouts. Keep one shared planner policy for top versus bottom destinations, consumed by the main route, standalone Syllabus, and tutorial practice. Do not require every pane to use the same numerical threshold: derive pane eligibility from its minimum usable widths, gutter, and available height.

| Situation | Intended behavior |
| --- | --- |
| Ordinary compact phone | v5 phone presentation, bottom planner navigation, single primary content column |
| Tablet portrait / medium window | Preserve v4's current navigation placement; use wide content only where it fits |
| Spacious landscape tablet | Preserve v4 top planner navigation and existing pane relationships; apply v5 styling |
| Narrow split-screen / folded window | Collapse panes to fit the current window while retaining selected exam and task context |
| Rail expands or window shrinks | Re-evaluate pane space without losing selection, edits, or accessible actions |

Preserve the current portrait-tablet navigation behavior unless later explicitly changed. The current source moves planner destinations to the top specifically in sufficiently wide tablet landscape.

## Design mapping

| Area | Bring from current v5 source | Preserve from current v4 |
| --- | --- | --- |
| Theme | Warm Clarity light/dark palette, typography, opaque surfaces, rounded shapes, button treatment | Scoped theme ownership and shared component compatibility |
| Planner navigation | Destination labels, selection styling, revision-parent selection behavior | Top placement on eligible tablets, bottom placement on phones, app rail |
| Exams | Exam cards, action styling, empty/loading states, clear new-plan and saved-syllabus actions | Existing exam-list/preview columns and tablet banner/shortcut positions |
| Today | Summary/task styling, visible Focus, labeled settings, revision/missed/upcoming access | Horizontal tablet summary and 67/33 agenda/checklist split, independent scrolling, finish-day action |
| Syllabus | Search/organize presentation, accessible actions, completion targets | Existing tablet subject/detail relationship and standalone route |
| Calendar | Workload/status indicators, legend, Today shortcut, themed day actions | Existing tablet calendar/detail pane placement and compact day sheet |
| Progress | Forecast-first presentation, readable summaries, all subjects | Fit within v4 shell; responsive sections rather than stretching phone cards |
| Creation and secondary flows | Three creation phases, source/style choices, previews, settings, dialogs, revision/rescheduling styles | Existing callbacks, validation, scheduling, persistence, premium rules |

Use the actual v5 implementation as authoritative where its older design notes differ. For example, `PlannerWarmClarityTheme.kt` currently uses `MulishFontFamily`; the older v5 plan mentions Hanken Grotesk.

v4 has a planner-specific black outline policy and separate `ProgressTrack` token. Merge deliberately: adopting v5's visual styling changes the neutral outline treatment, but removing v4 symbols wholesale can break tablet callers. Also replace the tablet overview's hard-coded coral/teal/purple fills and foregrounds with semantic theme tokens, or it will retain the old design despite a new theme wrapper.

## Implementation sequence

### 1. Capture the v4 baseline and close adaptive gaps

- Record existing phone, portrait-tablet, and landscape-tablet planner screenshots in both themes before changing presentation.
- Extract a testable sizing policy within `ui/adaptive/SafarAdaptiveState.kt`, with an additive planner policy file if needed. Audit all `isWideScreen` consumers before changing its shared meaning; preserve unrelated redesigns.
- Review `MainActivity.kt` orientation classification only where needed for resize/fold transitions; keep normal-phone behavior.
- Apply shared navigation decisions in main planner, standalone Syllabus, and practice host. Give Today, Exams, Calendar, and Syllabus explicit local pane-fit checks.
- Hoist persistent selection and scroll state above alternative layout branches as needed. Switching layout must not issue plan mutations or clear edits.
- Add boundary and live-transition tests before importing visual changes.

### 2. Introduce scoped v5 design primitives

- Add `ui/studyplanner/components/PlannerWarmClarityTheme.kt` from v5 and merge its integration into `StudyPlannerPalette.kt`, `PlannerGlass.kt`, `PlannerBlur.kt`, `PlannerDialog.kt`, and `TopicEffortBars.kt`.
- Merge the planner-only background behavior in `ui/theme/SafarSemanticColors.kt`.
- Keep compatibility for current v4 tablet consumers and Composables UI theme integration. Shared components must keep existing behavior outside the planner scope.
- Wrap the main planner, standalone Syllabus, creation flow, and practice host consistently.

### 3. Merge screen presentation while keeping v4 structure

- Update `screens/StudyPlannerScreen.kt` and `PlannerTopNavigation.kt` together so destination meaning and selected state agree. Merge display labels in `domain/model/studyplanner/StudyPlannerModels.kt` without changing enum identities.
- Merge `screens/PlannerEntryComponents.kt`, `plan/PlanHomeRedesign.kt`, `plan/PlanTabV2Components.kt`, and the relevant `PlanTabScreen.kt` sections. Retain and restyle `TabletPlannerOverview.kt` and `TabletPlanHomeLayout.kt`.
- Merge `screens/SyllabusRedesign.kt`, `SyllabusSubjectsScreen.kt`, and `ChapterTopicsSheet.kt`, keeping tablet pane ownership and navigation hooks.
- Merge `screens/CalendarTab.kt`, `InsightsTab.kt`, and `InsightsMomentumWidgets.kt`; restyle and extend `TabletPlannerCalendar.kt` with the same status/Today behavior.
- Merge relevant revision card/schedule/rest-day presentation in `plan/` without altering scheduling or completion mechanics.

### 4. Complete creation, localization, and tutorials

- Add `create/PlannerCreationPhases.kt`; merge `CreatePlanScreen.kt` and the changed presentation in creation steps individually, preserving v4 edits.
- Merge v5 planner string resources in English, Hindi, and Hinglish, plus relevant tutorial resources. Preserve v4 tablet strings; verify IDs and placeholders.
- Update `screens/PlannerPracticeStage.kt` to reflect both layouts with isolated sample data. Keep existing tutorial targets and actions valid.
- Import only relevant v5 planner tests, then adapt them to the merged implementation.

No repository, API, scheduling engine, data migration, or broad navigation-framework replacement is planned. Business-state code changes should be limited to demonstrated presentation/state-retention requirements.

## Validation and acceptance

Run v4 checks only. Prior v5 validation is supporting history, not proof for this merge.

1. JVM tests: sizing boundaries at 599/600 and 839/840dp; local pane thresholds immediately below/at/above fit; narrow window with tablet identity; expanded/collapsed rail; revision navigation and existing planner logic.
2. Compose tests: one composition transitions compact → tablet landscape → compact; assert exactly one planner navigation control set, expected pane visibility, and retained selected exam, destination, subject, calendar month/day, and unfinished input. Exercise the standalone route and practice host too.
3. Device/emulator checks: actual resize/split-screen, rotation, and fold/unfold where supported. Include 320/360/412dp phone windows, medium/portrait tablets, and 1024/1280dp landscape windows; test both rail widths and constrained height/IME. Record the actual window/content sizes rather than relying on device names.
4. Visual checks: light/dark, enlarged fonts, long exam/topic names, empty/loading/populated data, TalkBack focus, keyboard access, system insets. Capture representative screenshots for comparison.
5. Flow regression: create/edit plan, select exam, complete topic, start Focus, rename/delete with existing recovery behavior, reschedule, revision return navigation, saved syllabi, premium gating, and tutorials.
6. Build: run the applicable v4 QA variant's Kotlin compilation, planner unit tests, instrumentation-source compilation, and lint; execute adaptive instrumentation tests on available devices. Record any unavailable runtime coverage explicitly.

Acceptance requires v5's phone presentation, v5's visual language in v4's tablet arrangement, correct live expansion/collapse, preserved user context, and evidence for both layouts. Compilation alone is insufficient.

## Deliverable boundaries for the next implementation pass

Modify only v4 files needed above. Keep v5 untouched. Record actual changed files, verification results, screenshots, and remaining limitations. The original planning pass added only this document; the subsequent implementation pass is recorded below.


## Delivery record — 28 October 2026

Implemented the selective source merge in v4. v5 was used as a read-only reference; application source hashes matched the starting snapshot during final source review.

### Adaptive changes

- Compact versus wide shared layouts follow current window width, without the former tablet-identity override. Shared consumers such as Home, drawer/rail, habits, and Syllabus therefore react to a narrow tablet window.
- Planner top navigation is shared across the main route, standalone Syllabus, and practice. It uses a landscape window at least 840dp wide and 480dp high. Portrait retains its existing navigation placement.
- Planner pane fit uses remaining content constraints. Overview/Calendar require 840dp content width; Today requires 760dp. All require 360dp remaining height. Syllabus preserves portrait splits where two 320dp panes fit, with at least 400dp height.
- Ekagra's three existing tablet-landscape gates use the same spacious-landscape window footprint to avoid retaining tablet arrangements in narrow windows.
- Exam preview selection, exam-list/preview scroll state, agenda/checklist scroll state, and the checklist editor are owned above layout alternatives. Calendar month/day and Syllabus selection remain above their branches.

### Design changes

- Imported v5's scoped Warm Clarity light/dark palette, Mulish typography, solid surfaces, rounded controls, and secondary-flow styling. Material and Composables UI palettes share the planner accent within this feature.
- Phones use the v5 Exams, Today, Syllabus, Calendar, and Progress presentation. Today shares one agenda definition across compact and tablet layouts and retains the complete completed-topic list and Focus actions.
- Preserved v4's landscape top tabs, app rail integration, exam list/preview arrangement, horizontal Today summary, 67/33 agenda/checklist arrangement, independent scrolling, and calendar/detail arrangement.
- Restyled v4's tablet-specific cards and primary actions using the new semantic colors; added completion feedback to the existing exam preview slot and Today/legend parity to the tablet calendar.
- Merged creation phases and source/style choices, search/organize controls, localized copy in English/Hindi/Hinglish, and tutorial presentation. Kept the v4 tablet banner and shortcut positions.
- Revision navigation retains the originating destination selection and clears the old return state when selecting another tab.
- Scheduling, repositories, API contracts, persistence, and premium rules were not migrated from v5. Existing presentation callbacks remain wired to v4's actions.

### Verification actually performed

Before the user instructed execution to stop:

- v4 adaptive-stage Kotlin compilation and QA application assembly completed.
- Three new adaptive JVM tests passed: width boundaries, top-navigation conditions, and pane fit after rail/height constraints.
- Instrumentation test sources compiled, but no instrumentation test was executed.
- A combined build hit an existing dependency-order problem in `copyApksToOutputs` while packaging the instrumentation APK. A separate packaging attempt was stopped following the user's instruction.
- The separate `com.safarparmar.app.qa` adaptive-stage package was installed on the USB-connected device. It was not launched. The installed QA artifact predates the design merge and does not show the final design.
- An additional emulator was briefly started, then stopped when the user requested use of the USB-connected device. No further emulator was started.

After the user's instruction “Don't Run anything. I'll inspect it visually”:

- Continued source edits and text/resource review only.
- No further build, unit-test execution, instrumentation execution, app launch, screenshot capture, device interaction, or lint run was performed.
- The final merged design has **not** been compiled or validated on a device. The adaptive unit results describe the earlier adaptive stage, not a completed runtime transition check or final visual regression pass.

Added `SafarAdaptiveStateTest` and `PlannerAdaptiveSwitchingTest`; imported the v5 palette/creation/calendar presentation tests. The live Compose transition test is available for a later authorized run. Actual fold/unfold, rotation, split-screen, visual review, TalkBack, enlarged text, and end-to-end planner flows remain unverified.

No APK containing the final design was generated. Source work is complete; visual/runtime acceptance remains with the user's requested inspection workflow.
