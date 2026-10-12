# Parmar Toppers Batch localization

Android V5 implementation, 6 October 2026. Languages: English (`values`), Hindi (`values-hi`) and Hinglish (`values-b+hi+Latn`). Each has a dedicated `toppers_batch.xml`. The accompanying `toppers-batch-localization-review.csv` lists every string/plural quantity, its three translations and direct consumers.

## Coverage checklist

| Surface | Covered text and behavior | Main resource consumers |
| --- | --- | --- |
| Tracker entry | Title and subtitle; promotional artwork retained | PlannerEntryComponents |
| Shell and navigation | Page titles, rail/bottom tabs, back descriptions, loading, Premium/unavailable/error gates, retry and notices | ToppersBatchScreen, BatchSection |
| Today and focused subject | Overall progress, subject chips, next lecture, completed-today disclosure, due revisions, empty and locked states | ToppersBatchDashboard |
| Study modes | Official/personal labels, instructions, explanation dialogs, disclosure states and accessibility | StudyModePanel, BatchStudyMode |
| Personal plan | Add, watch/revision tasks, progress, completion, remove/confirm, reminder, dates and missed plans | PersonalPlanPanel |
| Library | Subject choices, lecture/backlog/revision tabs, week/page navigation, date strip, counts, completion actions and empty states | BatchWeeklyAgenda, LectureWeekBrowser, LibraryFilter |
| Lecture details and sources | Menus, watch/completion/revision dates, live availability, YouTube/Academy links and link failures | ToppersBatchActions |
| Editing and management | Rename/add, colours, course links, deletion/confirmation, restoration and empty deleted-item lists | ToppersBatchActions, ToppersBatchScreen |
| Watch/revision planning | Suggested/repeated dates, bookmark, optional reminder, notification status, date validation, save/remove/cancel | StudyPlanDialog |
| Prior progress and rescheduling | Selection/review, known/unknown completion date, explanations, preview, unavailable release warning and validation | StudyPlanDialog |
| Progress | Whole-course and subject totals, remaining/released lectures, revision totals, charts, legends and activity summaries | ToppersBatchScreen, BatchReleaseProgressPanel, ToppersBatchProgressChart |
| Calendar | Month/list, weekdays, selected dates, event categories, revision menus, milestones, next classes and important dates | BatchStudentCalendar, BatchEventKind, BatchMilestone |
| Completion feedback | Celebration, retained encouragement, banner, Undo, close and confirmation | BatchCompletionFeedback |
| Accessibility | Field labels, pane titles, action descriptions, expandable states and progress summaries | All feature UI consumers |
| Failures and success messages | API codes, known legacy validation/network messages, partial-save conflict, HTTP/generic fallbacks and deferred success messages | BatchLocalization, ToppersBatchViewModel |

## Translation and compatibility policy

- English retains nonblank server-managed copy. Hindi/Hinglish always use local UI resources. The existing legacy English “Today’s Watch List” normalization remains in the central resolver.
- Standard subject names resolve by stable subject key. English preserves supplied standard names; personal/custom names remain unchanged. Official topic/lecture titles remain catalogue content.
- Notices retain resource IDs and raw arguments. Dates and subject names resolve when displayed, including after configuration changes. Unknown errors show localized fallbacks rather than raw exception/server text. The partial-save warning retains its distinct meaning.
- Enum names, payload/action keys, ISO dates, India timezone calculations, reminder `HH:mm` values and saved study-mode identities are unchanged. Repository error text remains intact for older-server fallback detection.
- Milestone filters use `BatchMilestone` identity, independently of labels. Date/weekday/time/percentage display follows the selected language; Hinglish uses English date names.
- At narrow widths or larger font scales, subject cards use a single column and Library subject selection occupies its own row to keep translated labels readable.
- Server push text and embedded promotional poster text are excluded from this Android screen pass.

## Validation

Read-only gate: `python3 scripts/audit-toppers-localization.py`. It checks key/type/quantity parity, placeholders, literal percent signs, duplicates, nonempty values, Hinglish script, resource references and unlocalized feature UI literals. Its documented exceptions are protocol/date/animation/test identifiers. The general localization review script now reads plurals and indexes `R.plurals` consumers too.

Tracker unit tests cover deferred notices, API/legacy error classification, date arguments, milestone filtering, study modes, workflow, save/remove, compatibility, completion/Undo and release projections. Device coverage includes locale overrides, format/plural behavior, configuration switching with the same ViewModel, narrow/light and wide/dark/large-text screens, planning/import/reschedule/details dialogs and text-overflow checks.

The normal instrumentation compilation includes unrelated broken habit tests (removed reminder/sync/migration APIs). Tracker device verification uses a temporary Gradle init script selecting only the tracker instrumentation source directory; the repository’s build configuration and habit tests are not changed.

### Verified results

- 445 feature strings and 26 plural resources in each language; parity, formatting and hardcoded-text gate passed.
- Production release Kotlin compilation/resource processing passed (`:app:compileProdReleaseKotlin`).
- 113 tracker unit tests passed (`:app:testProdDebugUnitTest --tests 'com.safarparmar.app.feature.toppersbatch.*'`).
- 21 tracker device tests passed on Resizable_Experimental, Android 17: 12 locale/layout/resource cases, 5 Today plan flow cases, 3 opening cases and 1 picker-theme case.
- Locale/layout cases checked English, Hindi and Hinglish at 320dp/light/normal text and 768dp/dark/1.6x text. They visited Today, Library, Calendar, released-progress/charts, watch planning, prior-progress import, rescheduling and lecture details; tested configuration changes using the same ViewModel; and checked rendered text for ellipsis/max-line overflow.
- Source whitespace checks passed. All tests used fixture APIs rather than production account writes.

These checks verify translated Compose semantics and layout metrics. They do not establish real push-delivery localization (excluded), a full spoken TalkBack session, or the entire unrelated app test suite. The existing full calendar layout suite has an older tile-height contract and was not used as this localization gate.

48 fixture-screen captures are saved under `output/parmar-translations/toppers-translations` in the parent workspace (eight views × three languages × two configurations). Representative Hindi Today/Library, Hinglish watch/import dialogs and Hindi dark Calendar captures were visually inspected. These are component-test fixtures; they do not represent an authenticated production session or the full app shell. The capture-only rerun passed all 12 locale/resource/layout cases.
