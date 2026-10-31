# Habit Tracker: V5 sync approach in V4

V4 adopts the habit sync/export implementation from V5 commit `01b93c0b`, retaining V4's presentation, timetable, local custom emojis, and transient completion feedback. V5 already uses this implementation and is unchanged.

## Behavior

- Unconfirmed mutations are journaled per account before transmission and replayed with their original mutation IDs after a lost response or process restart.
- Creation uses a form UUID saved across state restoration, separate from V4's emoji argument.
- Background refreshes do not hold the write lock. Accepted writes publish acknowledged server changes (or the compatible local projection) without a second snapshot request.
- Reminder reconciliation runs independently, retries failures, and avoids rewriting unchanged alarm projections.
- Habit requests have bounded HTTP and coroutine timeouts. Uncertain form saves offer Retry confirmation and lock request fields until confirmation.
- History views consume one immutable projection per emission. Exports include all available habits, filter selected habits before building ranges, validate export size, and write files on IO.

## Validation scope

Focused habit tests cover snapshot-free saves, delayed/broken reminders, lost responses, form identity after restart, multi-device sync, account changes, export boundaries, local emoji persistence, and timetable behavior. Additional port regressions exercise persistent pending-operation replay across recreated stores, saving during a blocked refresh, emoji linkage to a stable create identity, and server changed-record/version handling.

These controlled tests establish which dependencies the save waits for; they do not measure production network latency or guarantee a particular real-device speedup.

Validation completed: `:app:testQaDebugUnitTest --tests 'com.safarparmar.app.feature.habits.*' --offline --console=plain` passed **97 tests**, zero failures/errors/skips. This compiled the QA debug app, generated dependency injection code, and compiled unit tests. `git diff --check` passed. No real-device latency benchmark was performed.
