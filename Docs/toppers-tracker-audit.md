# Toppers Batch tracker audit and implementation

Audit date: 2 October 2026. Scope: Android V5 tracker and its account-backed V2 API.

The user-mentioned `androidTest/.../toppersbatch` folder contains an instrumentation test, not the feature implementation. The UI and state live in `app/src/main/java/com/safarparmar/app/feature/toppersbatch`. Production rollout settings were not changed.

## 1. Does the tracker follow the student or the admin?

It already follows the student for completion, next lecture and personal backlog. Server `personalWatchList` chooses the first unfinished lecture independently in each subject. Backlog contains skipped lectures before the furthest completed lecture and unfinished lectures explicitly added by the student. Finishing a lecture advances only that student's subject; undo restores the next unfinished lecture. Every persisted lecture action is scoped to the authenticated account and course.

Admin/source updates control syllabus content, official class dates, time and lecture links. `syncOfficialDates` updates those fields on student rows without changing `completedAt` or revision history. A new lecture can expand the denominator and change the next unfinished lecture, but an admin upload does not mark anything completed.

The confusing part was the separate `behind` count, which measures unfinished lectures with past official dates. This is a gap against the batch timetable, not evidence that a late joiner missed their personal plan. The Android UI now labels this explicitly as batch context. Personal backlog continues to use the student queue, not elapsed batch days.

New enrollments default to their activation date, with an editable personal start, optional target finish and weekly lecture goal. Existing enrollment creation dates are the fallback; records without a usable start prompt the student to choose it. Students already studying before launch can backdate their personal start and mark lectures they have finished. Editing plan dates does not reset completion history or rewrite official dates.

The batch start date is 28 September 2026, supplied by the user. No individual class release dates were inferred from this date.

## 2. GK schedule

Both pages of `Planner.pdf` were read and visually checked. They contain 25 continuous weekly topic blocks from 5 October 2026 through 28 March 2027:

Polity → Economics → Geography → History → Static GK → Biology → Chemistry → Physics → Revision.

The GK subject now displays the complete schedule preview, actual start/end dates, source month labels, and the current official week. Source month labels intentionally differ from some calendar dates: the January-labelled first week starts 28 December, and the April-labelled revision week is 22–28 March.

These are topic blocks, not 25 lectures. They have no completion checkbox and are excluded from progress, forecasts and personal backlog. GK remains without individual lecture rows until an ordered lecture catalog is supplied. Official individual class dates and times still need verified release metadata.

## 3. Why keep the second tab?

Renamed Lectures to Library and gave it cross-subject capabilities unavailable in Today:

- Search by topic, subject, lecture number, source month or week.
- Filter all, unfinished, personal backlog, pending revision and completed lectures.
- Manage revision plans and backlog across the entire syllabus; revision results sort by next revision date.
- Browse each subject's syllabus and open the GK timetable.

Today remains a focused next-step view: personal next lecture per subject, today's completions, up to three due revision lectures with a link to all revisions, and official classes today. The previous “Coming up today” heading described a personal queue without regard to the day; it now reads “Next at your pace”. Official classes are separate and appear only when supplied by the API for today's India date.

A shared lecture screen is useful in both contexts: Today answers what to study next; Library lets a student locate and manage arbitrary work. Library results use lazy rendering.

## 4. Dates, analytics and insights

Progress now includes:

- Official batch start versus editable personal start.
- First recorded completion, target date, and actual completion date of the current tracked syllabus.
- Recent completion count, active days, current study streak and rolling seven-day lecture goal.
- Lectures due for revision, existing personal backlog, subject progress, activity grid and completion trend.
- Estimated finish for the current syllabus at recent pace.
- Approximate weekly pace needed to meet the target, or a clear missed-target message.

Forecast = today + ceil(remaining lectures × observation days / recent completions). Observation uses up to seven days, shortened for a newly started plan. No forecast is shown without recent completions. Required weekly pace = ceil(remaining lectures × 7 / inclusive days left until target).

All completion dates use Asia/Kolkata, including the calendar API offset. A study streak counts consecutive days with at least one completion; yesterday's streak remains visible before today's first completion. Completion dates reflect recorded Done actions, not inferred watch time. Undo can remove a recorded completion day and an all-syllabus finish date. Adding lectures can reopen syllabus completion. These estimates cover tracked lectures, not the full unpublished GK course or mastery.

Inspiration: [Todoist productivity](https://www.todoist.com/help/todoist/features/use-the-productivity-view-in-todoist-6S63uAa9) provides completion summaries, goals and streaks. [Khan Academy](https://support.khanacademy.org/hc/en-us/articles/28849330897293-Where-can-I-find-my-streaks-and-course-level-information) places streaks and course-level progress in student views. SAFAR uses the relevant activity patterns without claiming assessment-based mastery or inventing study hours.

## Validation and delivery limits

- 28 server tests passed: V2 routes, personal watch list and source catalog tests. New route tests verify account isolation, premium/enrollment access, impossible/future dates, goal limits, target clearing and preserved completion/class dates.
- 13 Kotlin tracker state/math tests passed through an isolated JVM harness using the unchanged pure model/helper implementations extracted from their source files. Coverage includes late joining, undo, India date boundaries, estimates, due revisions, disabled subjects, search/filter results and all GK weeks.
- Server production build passed.
- Full Android QA compile/unit-test task is blocked by existing errors in `TimerBubbleOverlay.kt` (Row parameters) and `NishthaAnalyticsScreen.kt` (Float/Double arguments). No tracker compilation diagnostics were reported. The Gradle unit-test task and on-device visual validation remain unverified.
- Server changes and Android changes are local. The new personal-plan endpoint must be deployed with the app update; no public rollout or deployment was performed.

## Daily GK lectures (3 October 2026)

The student confirmed one lecture per day. The planner's 25 weekly topics now produce 175 daily records, 5 October 2026–28 March 2027, including Sundays. Topic transition weeks retain the planner's combined topic rather than inventing a transition date. Stable date-based identifiers support completion, undo, backlog, revision sessions, rename, colour, deletion and restoration through the existing lecture cards. Scheduled dates enforce the same unlock rule as the other subjects; GK participates in totals, next lectures and calendar classes.

These planner-derived records are persisted on the device by signed-in user and course. They are not sent as fabricated IDs to the server. When the server supplies actual GK lecture records those records take precedence; local progress is not automatically merged into them. The PDF does not supply video URLs. Earlier timetable-only descriptions above are superseded by this confirmed daily schedule.

The student subsequently confirmed seven teaching days for English, Maths and Reasoning too. Existing five-slot topic groups are expanded to seven slots, retaining server IDs and completion history. Additional slots use account/course-scoped device persistence. Consecutive week dates start 28 September for Maths/Reasoning and 5 October for English, aligned to the GK planner's first October week. Supplied server dates take precedence. Overlapping month/last-week labels are retained as source headings; inferred consecutive dates may extend beyond those headings. This is a user-confirmed schedule interpretation, not independently verified daily release metadata.
