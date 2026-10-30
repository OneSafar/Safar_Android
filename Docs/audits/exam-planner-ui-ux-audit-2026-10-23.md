<!-- Hallmark · pre-emit critique: Philosophy 5 · Hierarchy 5 · Execution 4 · Specificity 5 · Restraint 5 · Variety 4 -->
# Exam Planner: UI and UX audit

Reviewed 23 October 2026 against source revision `9f7c20f0` and the current working tree. Scope: presentation, discovery, screen hierarchy, navigation, interaction, typography, colors, and motion. Scheduling, weighting, completion, revision, persistence, and premium rules are outside the proposed changes.

This is a source-level audit of the active screens wired through `ui/studyplanner/screens/StudyPlannerScreen.kt`. No new device run or user study was performed. Layout order, list limits, rendered controls, paths and color constants are confirmed from source. First-screen crowding, precise hit areas, large-font clipping and Back edge cases require runtime validation. No production files were edited for this audit.

## Recommendation

Keep every live feature and the existing planning engine. Organize the experience around three student questions: **What should I study now? Where can I change my plan? Am I on track?** Give frequent actions visible labels; keep advanced controls beside the content they change. Use the approved tutorial design's warm color family and rounded surfaces, with a quieter working area for tasks, dates and syllabus lists.

## Findings, ranked by impact

### Critical visibility gaps: restore information first

1. **Calendar cells do not show the available work or rest-day information.** `CalendarDayChip` receives `items` and `isOff`, but renders the day number, today/selection state and exam marker only. This prevents the month view from communicating workload and rest days. Add a compact workload indicator/count, a rest-day treatment and a clear selected-day agenda. Avoid placing four competing dots on every cell; reveal the detailed statuses in the selected day's summary.
   Evidence: [CalendarTab.kt:425](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/CalendarTab.kt:425), [caller:989](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/CalendarTab.kt:989).

2. **The completed list stops at 15 topics without a continuation.** It shows the full total but renders `completedTopics.take(15)`. Keep a short preview only if it includes **View all completed topics**. A full lazy list with search/filters also preserves every completion without changing its storage.
   Evidence: [PlanTabScreen.kt:774](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/plan/PlanTabScreen.kt:774).

3. **Subject progress stops at four subjects.** The chart and its detail selection use `subjects.take(4)` without an all-subject destination. Show all subjects as comparable labeled bars, or provide an explicit **All subjects** route from a four-subject preview. Keep the current weighted progress calculation.
   Evidence: [InsightsMomentumWidgets.kt:715](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/InsightsMomentumWidgets.kt:715).

### Major usability issues: make actions and location clear

4. **Navigation labels and context are ambiguous.** “Plan” opens the exam collection; “Home” opens a selected exam's work. Revision is a sixth section outside the five bottom items, and its indicator index falls back to the first item while none is selected. Rename the existing five items to **Exams / Today / Syllabus / Calendar / Progress**. Display the selected exam consistently. Treat Revision as a labeled child destination with its parent tab selected and a clear Back action.
   Evidence: [StudyPlannerModels.kt:361](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/domain/model/studyplanner/StudyPlannerModels.kt:361), [StudyPlannerScreen.kt:1563](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/StudyPlannerScreen.kt:1563).

5. **Daily actions have the same visibility as rare maintenance actions.** “Focus with Ekagra” is inside topic overflow; plan settings are behind a 30dp three-dot control. Expose **Focus** on today's topic rows and a labeled **Plan settings** action in the exam context. Keep edit, replace and remove in overflow. The Focus action should use the same existing topic-linked Ekagra navigation, without automatically starting a service.
   Evidence: [PlanHomeRedesign.kt:124](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/plan/PlanHomeRedesign.kt:124), [topic actions:594](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/plan/PlanHomeRedesign.kt:594).

6. **The visual order delays the user's work.** The Exams screen puts the Parmar poster and GK5 entry above New Plan, drafts and exam cards. Today puts its header, hero, four counters, recurring checklist and tabs before the agenda. The source order is confirmed; exact below-fold placement depends on the device. Put the active exam and current tasks first; move related trackers into a compact **Study tools** section. Reduce the exam summary to a compact strip on Today and reserve detailed metrics for Progress.
   Evidence: [StudyPlannerScreen.kt:920](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/StudyPlannerScreen.kt:920), [PlanTabScreen.kt:553](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/plan/PlanTabScreen.kt:553).

7. **Creation choices sometimes behave differently from their labels.** “Build it myself” first opens saved syllabi. Selecting Deep Focus/Balanced immediately opens ordering; Mixed Bag opens a picker. Make the source choices explicit: **Use a template / Use a saved syllabus / Create a syllabus**. Let study-style cards select in place, with separate **Customize order** or **Choose priority subjects** actions. Show three user-facing phases: **Syllabus → Study routine → Review**; retain every conditional substep inside these phases.
   Evidence: [ChoosePathStep.kt:72](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/create/steps/ChoosePathStep.kt:72), [PlanSettingsStep.kt:145](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/create/steps/PlanSettingsStep.kt:145), [CreatePlanScreen.kt:170](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/create/CreatePlanScreen.kt:170).

8. **Useful controls depend on discovering gestures.** Syllabus reordering uses whole-row long-press drag; preview renaming uses long-press, with an empty normal click. Add **Organize** mode with visible handles, and accessible Move up/Move down alternatives. Give preview topics an explicit edit action. Preserve the same order and rename callbacks.
   Evidence: [SyllabusRedesign.kt:599](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/SyllabusRedesign.kt:599), [PlanPreviewStep.kt:594](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/create/steps/PlanPreviewStep.kt:594), [DeepFocusOrderStep.kt:355](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/create/steps/DeepFocusOrderStep.kt:355).

9. **Several custom controls and essential labels are too compact.** Today uses an 18dp completion Box, 28dp overflow, 13.5sp topic names and 11sp context. The completion control lacks explicit checked-state/checkbox semantics. Use 48dp explicit interaction containers, 22–24dp artwork, a semantic checkbox, 16sp topic names and 13–14sp context. Source dimensions are layout bounds; Compose may expand touch handling, so actual hit areas and overlap need device checks.
   Evidence: [PlanHomeRedesign.kt:537](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/plan/PlanHomeRedesign.kt:537), [SyllabusRedesign.kt:1089](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/SyllabusRedesign.kt:1089).

10. **Some accent text does not have enough contrast.** The Ekagra history badge uses white 10sp text on `#FF8A65`: calculated contrast is **2.31:1**. The same orange text on the planner cream is approximately **2.21:1**. Default coral `#E0654B` on cream is approximately **3.27:1**. Use separate dark accent-text tokens, dark ink on pale badges, and solid primary fills. These are calculations from source colors, not phone pixel samples; translucent controls need composited-state checks too.
   Evidence: [InsightsTab.kt:369](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/InsightsTab.kt:369), [badge:410](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/InsightsTab.kt:410), [StudyPlannerPalette.kt:48](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/components/StudyPlannerPalette.kt:48).

11. **Progress requires interpretation and scrolling before answering the main question.** The order is overall ring, exam/daily metrics, streak, speed, finish forecast, subjects, revisions, then focus history. Put **Am I on track?** and the existing forecast first. Follow with concise syllabus progress, pace and consistency. Show all subjects; keep revision and real focus time easy to reach. Charts should supplement labeled values. Calendar agenda rows should offer **Open topic**, so reading a topic there does not require finding it again elsewhere.
   Evidence: [InsightsTab.kt:269](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/InsightsTab.kt:269), [CalendarTab.kt:664](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/CalendarTab.kt:664).

### Minor polish issues: refine after access is fixed

12. **Repeated reveal animation delays reading.** Progress counts reset to zero on re-entry; rings and sections restart staggered animations. Keep final numbers visible on revisit, animate changes from their previous values, and use short transitions. Standard Compose animation may already honor system animation scale; this audit does not establish that reduced-motion support is broken.
   Evidence: [InsightsMomentumWidgets.kt:158](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/InsightsMomentumWidgets.kt:158).

13. **The visual language varies between magazine hairlines, glass buttons, large rings and multiple accent systems.** Use one role-based set of surfaces, type sizes, radii and action styles throughout. Keep subject identity, plan origin and task status distinct. Avoid applying decorative tutorial artwork behind worklists or calendar information.
   Evidence: [StudyPlannerPalette.kt:48](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/components/StudyPlannerPalette.kt:48), [PlannerGlass.kt:82](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/components/PlannerGlass.kt:82), [PlanHomeRedesign.kt:168](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/plan/PlanHomeRedesign.kt:168).

14. **Long titles have hidden recovery behavior.** The syllabus exam heading clips its name and becomes tappable to reveal the full name only after overflow is detected. Use a two-line heading or ellipsis with a predictable detail action. Important names should be readable without discovering an unmarked gesture.
   Evidence: [SyllabusRedesign.kt:140](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/screens/SyllabusRedesign.kt:140).

## Proposed navigation and screen arrangement

Keep five primary destinations, with unchanged data/actions behind them:

| Destination | First visible content | Secondary access, still clearly labeled |
|---|---|---|
| **Exams** | Active exam/continue, New exam plan, other exams | Saved syllabi/drafts; compact Parmar and GK5 Study tools; rename/delete on exam row; refresh |
| **Today** | Selected exam, today's date, clear task count, next topic with Focus | Revision due, missed and no-date alerts with counts; Add topic; Daily routine; upcoming/completed lists; Finish day |
| **Syllabus** | Search, subject list with progress, Add subject | Subject → Chapter → Topics; Add at each level; Organize; Change plan; contextual rebuild prompt |
| **Calendar** | Month, Today shortcut, visible workload/rest/exam markers | Selected-day agenda; Change date; Open topic; Revision and missed links above or beside agenda |
| **Progress** | Existing forecast and on-track status, relevant plan action | Syllabus and routine progress separately; pace/consistency; every subject; revision summary; linked focus time |

A consistent selected-exam control provides context on Today/Syllabus/Calendar/Progress. Switching an exam should retain the chosen destination where valid. Without an active exam, show a clear exam picker/empty state rather than repeated error snackbars. Back behavior and selection must follow the visible hierarchy; save per-tab scroll and calendar month when moving between screens.

**Revision** remains a child destination reached from a visible `Revision · N due` action on Today and Calendar, plus its Progress summary. Keep the parent bottom tab selected. Group revision work into Due / Missed / Upcoming / Completed views while preserving custom/spaced dates and all existing session controls. These are list presentation groupings, not new scheduling rules.

## Proposed Today hierarchy

1. Compact selected-exam row: exam name, date/countdown, labeled Plan settings.
2. Heading: Today, date and actual completed/total count.
3. Only when relevant: missed/no-date warning and a revision-due shortcut, each opening its correct existing list.
4. Today's study topics, with visible Focus and an accessible completion control. Show topic name, subject/chapter context and revision status.
5. Add topic → choose existing syllabus topic or one-off topic using current flows.
6. Daily routine summary with completed/total count and Expand/Manage. Do not mix routine completion with syllabus coverage.
7. Upcoming/Completed access and Finish day using existing confirmation/undo behavior. Finish day must not obstruct scrolling or falsely look like completing all syllabus work.

Detailed completion percentages and decorative analytics move to Progress. Suggested extra study remains available after the day's work is completed, as it is now.

## Feature-preservation map

| Existing capability | Proposed home |
|---|---|
| New plan, templates/category selection, custom syllabus, saved syllabus/drafts | Exams → New exam plan; explicit source choices |
| Custom subject/chapter/topic creation, edit/delete and autosave | Syllabus creation/editor and corresponding live hierarchy |
| Exam title/type/date, topics per day, rest days | Labeled Plan settings → Exam and Study routine groups |
| Deep Focus, Balanced, Mixed Bag, subject priority and ordering | Study routine → Study style; visible customization action |
| Chapter effort/difficulty, topic size, optional even build | Relevant chapter/topic editor and creation substep; explain units; preserve current calculations |
| Preview week/day schedule, topic rename, capacity/coverage notices | Creation → Review with explicit edit/settings links |
| Recurring daily topics/checklist and logging | Today → Daily routine with Manage; existing optional setup flow |
| Complete/uncomplete topic; completion/revision choice | Topic checkbox/detail; existing prompt and state transitions |
| Focus with topic-linked Ekagra | Visible Focus on active Today topics; also in topic detail |
| Add from syllabus/custom, replace, remove from today | Add topic plus topic overflow/detail |
| Missed topics, unscheduled topics, assign dates | Labeled counted recovery links on Today; corresponding Calendar/Syllabus access |
| Individual date changes, pulling/replacing topics | Topic detail and selected-day agenda using existing actions |
| Rebuild/reschedule/distribute, keep-existing-date options | Change plan / Plan tools with the same existing options and confirmations |
| Subject/chapter/topic ordering and mark-all controls | Syllabus → Organize/context menu; accessible move alternatives |
| Revision custom/spaced dates, session checklist and schedule editing | Revision child screen; visible due summaries |
| Weighted coverage, routine progress, countdown, daily load | Concise Progress summary with separately labeled metrics |
| Pace, consistency/streak, completion forecast | Progress → On track summary and expandable explanations |
| Every subject's progress | Progress → Subjects with a complete list |
| Revision metrics and linked Ekagra history | Progress → Revision / Focus time detail |
| PDF export | Plan settings → Plan tools → Export PDF |
| Rename/delete exam | Exams row options; preserve confirmation/undo where currently supported |
| Parmar tracker and GK5 | Exams → visible compact Study tools area |
| Refresh, loading/error/empty states, premium access, Titli, feedback/undo | Consistent screen chrome and existing entry points; do not change access rules |

Two dormant paths need a product decision before any redesign exposes them: a Paste Syllabus implementation has no normal source-choice entry, and PlanSettingsSheet receives onReset without rendering a reset action. Do not restore either merely because code exists.

## Visual system

Use the accepted Soft Geometry direction in a restrained study-tool layout:

- Warm blush/cream canvas; solid readable cards; deep plum primary actions; peach icon tiles; mint success surfaces. Dark mode uses deep neutral/plum surfaces with light readable ink and pale action accents.
- Reuse the tutorial's semantic visual roles, while checking contrast in actual planner states. Keep existing template-green/custom-orange origin identification as a small labeled badge rather than recoloring every control.
- Green means completed; amber means attention/revision; red means missed/error. Planned/no-date states also require text/icons. Subject colors are stable identity cues, mainly in small dots and charts.
- One main interaction style: solid filled primary, outlined/tonal secondary, quieter maintenance overflow. Essential text uses solid ink, not translucent decorative colors.
- Header/empty-state shapes may carry personality. Keep shapes out of task rows, chart labels and calendar cells.

The current typography identifiers are misleading: `MulishFontFamily` / `PoppinsFontFamily` actually load **Hanken Grotesk**, and `LoraFontFamily` loads **EB Garamond**. Preserve that real app identity rather than adding another font family. Source: [Typography.kt:19](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/theme/Typography.kt:19).

| Role | Proposed size / treatment |
|---|---|
| Screen title | 24–28sp, semibold, Hanken Grotesk |
| Section title | 18–20sp, semibold |
| Topic/primary body | 16sp, 22–24sp line height |
| Subject/chapter/support copy | 13–14sp, readable contrast |
| Button label | 14–16sp, medium/semibold |
| Primary metric | 28–32sp; EB Garamond optional for a single display accent |
| Small chart annotation | 11–12sp only for supplementary content, with detail available |

Use 16–20dp screen gutters, 24dp between different sections and 12–16dp within related groups. Explicit touch targets should be at least 48dp; primary actions 52–56dp high. Allow wrapping and scrolling instead of shrinking text or overriding user font scale. Keep short-window and landscape content reachable. For larger screens, use navigation rail plus list/detail panes rather than stretching a phone layout.

Android's guidance supports familiar three-to-five primary destinations, secondary tabs for sibling content and overflow for less frequent actions: [Layouts and navigation patterns](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns). Its accessibility guidance recommends 48dp focusable touch regions: [Make apps more accessible](https://developer.android.com/guide/topics/ui/accessibility/apps).

## Motion

- Preserve visible numbers when returning to Progress; animate from previous values on data updates.
- Navigation/selection: approximately 150–220ms, with restrained easing; avoid the current low-bouncy whole-bar indicator.
- Expansion: approximately 220–300ms, retaining the current anchor and scroll position.
- Completion: brief check feedback with existing Undo; avoid sudden list jumps during the tap.
- Calendar month changes: consistent directional motion; keep selected date/month stable between tab visits.
- Creation: phase progress and a clear Plan ready state. The current fixed 1400ms success handoff should be reviewed as a presentation delay, without changing generation work.
- Respect system/app reduced-motion settings. Keep Titli's established animation lightweight and away from dense content.

## Implementation boundaries and validation

All changes should wrap the existing `PlannerActions`, ViewModel results, dates, IDs, calculation outputs, revision rules and persistence calls. Presentation extraction should not recalculate the plan in composables. Navigation labels/routes should keep existing deep links working. The later UI implementation must update Titli targets to the final layout, without letting practice invoke real actions.

Suggested order:

1. Restore complete information and explicit accessibility semantics: calendar indicators, all completed topics, all subjects, predictable controls.
2. Clarify labels, selected exam, Revision parenting, visible Focus/settings and content order.
3. Simplify creation presentation and gestures; preserve source/order/rating options.
4. Apply shared typography, color and component roles across live states and sheets.
5. Polish motion and adaptive layouts; align tutorials after the live UI settles.

Validate with more than four subjects, more than fifteen completed topics, many topics on one day, long exam/topic names, saved/manual/template plans, an unset or passed exam date, no active plan, rest days, missed/unscheduled work and every revision type. Test light/dark, 320dp width, short landscape, large fonts and TalkBack. Trace tab/Back behavior, rotation and selected month/scroll retention. Compare callbacks and generated plan outputs before/after to ensure presentation changes do not alter mechanics.

Potential Back behavior edge case to reproduce: opening Revision records an origin; `setSection` does not clear it. Switching another bottom tab while Revision is open may leave stale Back context. This is a source-derived hypothesis, not a reproduced defect. Evidence: [StudyPlannerViewModel.kt:312](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/studyplanner/StudyPlannerViewModel.kt:312).

Acceptance should include students locating Focus, Revision, Plan settings, a completed topic beyond item 15 and subject progress beyond subject four without a tutorial. Collect actual results; this audit does not invent success rates or time savings.

**Audit count: 3 critical visibility gaps · 8 major usability issues · 3 minor polish issues.**
