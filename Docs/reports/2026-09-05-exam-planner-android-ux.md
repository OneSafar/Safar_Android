# Android exam planner UX implementation

Scope: `Safar_Android` development codebase only. `SAFAR_2` has no remaining changes from this task. Existing scheduling APIs, calculations, storage and capabilities are retained.

## Student-facing changes

- Today is the default destination. A valid previously active exam is restored; multiple exams without a valid selection open My exams. Navigation is Today, Schedule, Syllabus, Progress, with revision reachable from Schedule.
- Setup leads from syllabus selection to date/workload and schedule preview. Study order and difficulty remain in Customize plan. Daily routine no longer blocks confirmation.
- Setup uses actual exam date, available study days and syllabus count for an explicitly suggested target. Removed hard-coded 60-day estimates and claims that a target had been adjusted when it had not. Preview shows the actual last scheduled day.
- Today foregrounds the daily checklist, completion count and exam countdown. Global syllabus progress and optional routine are secondary.
- A checkbox completes a topic directly; Revise is a separate action. Revision checkboxes complete a session, and completed sessions remain visible after the next review date advances. Undo uses the original review appointment.
- Stop for today shows the number moving to Missed. Stopped work remains discoverable even after its date is cleared. Stopping early cannot produce a completion celebration; unfinished work stays in the day total. Existing undo remains available.
- Missed-work recovery exposes rescheduling and preserves existing capacity/fit results.
- Syllabus editing opens actual reorder controls. Move up/down are available from menus and accessibility actions; changed order exposes Update schedule.
- Revision's empty state can select a topic and open its existing review-date editor.
- Rest days have selectable, wrapping day labels. Task checkboxes and overflow targets are larger. Removed setup About buttons and shortened study-mode labels.

## Verification

Verified on a separate clean checkout at commit `8bfd3255` with only the planner changes applied:

- `:app:testQaDebugUnitTest --tests 'com.safarparmar.app.ui.studyplanner.*'`: 35 tests, zero failures.
- `:app:assembleQaDebug`: successful.
- Eight new regression cases cover active-exam selection, legacy nullable review data, same-day and late review completion, checked-row persistence, future work exclusion, missed-work classification, and stopping early.
- Offline emulator fixture rendered the real Today, date/workload, and revision composables in light/dark scenarios. Checked direct topic completion, the revision picker, and the corrected stopped state (1 of 2 complete, 1 missed, no completion celebration). The fixture is temporary verification code, not part of the application changes; no account or backend writes were used.
- `git diff --check`: clean.

The full development checkout still fails compilation in existing YouTube/Kavach work (including KavachAnalyticsRepository, YoutubeStudyV2AccessibilityService, KavachBlockOverlay, and QuickUnlockActionReceiver). No planner compiler errors remain. Those unrelated edits were preserved.

## Remaining validation before release

- Exercise authenticated creation, persistence, schedule recovery and Undo against the development backend after the unrelated build errors are resolved. Offline visual checks do not prove backend integration.
- Test with students: create a plan, complete one topic, schedule a revision, stop early, and find missed work without coaching. Compare first completion and return usage with the current baseline; retention improvement is not yet measured.
- Production retention instrumentation and the website implementation are deferred. No new telemetry pipeline was introduced.
- The app's existing global theme overrides font scale/density; this task has not changed that app-wide behavior. Dedicated large-font and tablet accessibility validation remains necessary.

## Emulator captures

Offline sample data, real planner composables, without the app navigation shell:

- [Today](planner-ux-2026-09-05/today.png)
- [Stopped for today](planner-ux-2026-09-05/stopped.png)
- [Date and workload](planner-ux-2026-09-05/setup.png)
- [Revision in dark theme](planner-ux-2026-09-05/revision-dark.png)
