# Habit Tracker repairs — 27 October 2026

Implemented in Safar_Android_v5 and SAFAR backend in response to the latency audit. **No tests were executed**, as requested. Compile-only verification is recorded below. No APK/AAB was packaged, installed, uploaded, or deployed; the installed V81 does not receive these source changes automatically.

## Save behavior

Normal saves now follow this path:

```text
journal immutable operation → POST → validate acknowledgement
    → merge authoritative affected records → close form
```

The POST includes `X-Habit-Changes: 1`. The backend optionally returns `previousVersion` and authoritative `habits` for the affected IDs alongside the existing `version` and `acknowledged` fields. Existing clients retain the previous response shape. Android can also work with an older backend by applying acknowledged operations in memory and reconciling through the next poll, without blocking the form on that poll. Deploy the backend before distributing the Android update to use authoritative record responses immediately.

The save coroutine has an **18-second total budget**, including write queueing and pending-operation recovery. Habit HTTP calls have a **15-second call timeout**, with shorter connect/read/write timeouts. The coroutine budget is cooperative; synchronous device storage operations cannot be forcibly interrupted, so the form also remains closable while pending. Busy state is released in `finally`, including cancellation. A timed-out or disconnected request may already have committed: the UI explicitly offers confirmation retry rather than pretending it certainly failed.

## Changes against the audit

| Audit finding | Implemented repair |
| --- | --- |
| 1. Accepted save waits for read-back/reminders | Return affected records from the mutation; merge/publish on acknowledgement. Reminder work is asynchronous and retryable. Projection reconciliation failure cannot turn a server-accepted save into a save failure. |
| 2. Unbounded Saving and locked modal | Habit-specific call timeout and overall save budget; `finally` resets busy; close remains available; uncertain saves keep their original payload and use a confirmation retry. |
| 3. Refresh blocks writes; repeated taps | Separate refresh, write, and projection locks; overlapping refreshes are coalesced. Pending checkbox cells reject repeated taps in Daily/Weekly/Month. The store also suppresses redundant completion writes. Legacy import has an explicit status and rejects new writes promptly while active. |
| 4. Retry identity lost after restart | Durable, account-scoped journal for unconfirmed ordinary operations, written before POST. The create form saves a stable UUID across recreation; retry uses the same remote habit identity. Startup/screen refresh recovers journaled operations. Older uncertain operations are recovered before newer writes, preventing late replay from undoing a newer edit. |
| 5. Unchanged polls rewrite reminders | No reminder rewrite on unchanged polling. Cache retains only active reminder-bearing habits, current/future schedule information, and relevant completions. Changed-only alarm reconciliation, cache equality check, and background retry avoid repeated disk/Binder work. Boot/settings restoration remains available. |
| 6. Whole-history cost | Unchanged polls query version metadata only. Mutations clone and `$set` only affected habit entries, under the existing account version CAS. No full-array rewrite; mutation response transfers affected records rather than triggering a full-account GET. Android reuses converted unaffected records and indexes history. |
| 7. Receipt drain delays acknowledgement | Snapshot and receipt outbox still commit atomically. Coalesced background bulk writes drain receipts after acknowledgement; both outbox and durable receipts prevent replay. Conditional cleanup protects concurrent writers, and account deletion is rechecked. A bounded outbox applies backpressure if receipt persistence remains unavailable. |
| 8. Eager daily UI and export disk IO | Keyed lazy daily rows. CSV file creation/write and FileProvider resolution run on IO; sharing launches on Main. Export range/cell limits avoid unbounded materialization. |
| 9. All Time export depends on heatmap year | Separate account-wide export catalog; earliest date derives from actual selected account habits, including archived history. No hardcoded 2020 start. Selected habits are filtered before per-day materialization. |
| 10. Generic errors and missing stage evidence | Distinct rejection/auth/conflict/uncertain-save messages; Android queue/HTTP/snapshot/projection/reminder/total timing logs; backend auth/account/request, state-read/receipt-lookup/commit, CAS-attempt and receipt-drain timing. Mutation IDs correlate save logs without habit names or tokens. |

Weekly and monthly grids now use dated historical records, preserving archived rows in their historical periods. Archive is unavailable when editing an already archived record. Repository views consume one immutable history snapshot per emission, preventing mixed-version joins, and suppress unchanged data emissions.

## Persistence and concurrency details

- Backend authoritative storage remains the existing atomic account document. No schema migration or replica-set requirement was introduced. Full initial/changed snapshot reads, and the account state read used by mutations, still scale with history. The fixes remove unnecessary unchanged reads, whole-array writes, full-array cloning, and full-account read-back on every Android mutation; they do not claim unlimited account-document capacity or replace the storage architecture.
- A delayed refresh cannot replace a newer acknowledged projection. If a mutation response reveals a gap containing another device's updates, Android retains the last fully covered version so a later snapshot fills that gap instead of falsely claiming full synchronization.
- The pending journal is separate from legacy Room migration and the reminder cache. It stores immutable operation IDs and payloads, not an independently writable history database. Definite 400/401/403/409 rejections retire their journal entry; ambiguous transport/server failures remain recoverable.
- Receipt cleanup failure after a successful save is harmless: the operation is still protected by durable state/outbox, and retry stays idempotent. Reminder failure does not block or fail an accepted save.
- Background receipt draining is coalesced per database/account, uses version-conditional outbox cleanup, and retains undrained receipts on failure/process death. Above the configured backlog bound, writes wait for recovery instead of growing the embedded outbox without limit.
- Account checks remain around network responses and projection publication. Pending entries are account scoped. Existing release serialization keep rules remain in place for the new response fields and pending batch model.

## Verification and handoff

- **Passed:** Android production-debug Kotlin compilation: `./gradlew :app:compileProdDebugKotlin --console=plain`.
- **Passed:** Backend TypeScript compilation without emit: `./node_modules/.bin/tsc --noEmit --pretty false`.
- Test suites, emulator/device runs, slow-network testing, authenticated production writes, load tests, and deployment were left to the user.
- Earlier diagnostic reproductions and affected fixture expectations were updated to describe repaired behavior. **They have not been executed after the repairs.** The prior audit's 87/32 passed counts describe the earlier investigation, not verification of these changes.

The most useful manual checks are create/edit/complete on a slow connection, retry after losing the response, close/reopen and process restart during a save, rapid checkbox taps, reminder failure/notification settings, account switching, archived historical views, and All Time exports containing prior-year archived habits. These are handoff suggestions, not tests claimed to have been performed.
