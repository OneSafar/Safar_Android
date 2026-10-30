# Habit Tracker latency and correctness audit — 27 October 2026

> Repairs were implemented later on 27 October. See [implementation notes](habit-tracker-latency-fixes-2026-10-27.md). The test results below belong to the original audit; tests were not rerun for the repairs, at the user’s request.

The user confirmed the affected app is version **81**. The screenshot shows the form locked in “Saving…” with reminder creation disabled. This audit inspected the active V5 Android implementation and SAFAR backend, added controlled reproductions, and ran the relevant tests. No application behavior, production records, deployment, or release was changed.

## Conclusion

The principal problem is the **save completion contract**. Android keeps the form in Saving until it finishes all of these steps:

```text
wait for the shared refresh/write mutex
  → POST mutation (server commits)
  → GET complete account snapshot, including completion/schedule history
  → fetch additional pages if needed
  → rebuild the Android projection
  → write the full reminder cache and reconcile alarms
  → publish the projection
  → return to the form and close it
```

A server acknowledgement alone does not complete the save. A slow background refresh delays the start; a slow snapshot or reminder operation delays the end. A failure after the commit reports a failed save even though the backend has the habit. Controlled tests reproduced these behaviors with the actual Android store and repository.

This is a sufficient explanation for the reported long Saving state, but **the exact slow stage on the user's phone is not identified**: no affected-device timing trace, ANR trace, or authenticated production request was available. Network/store operations run on `Dispatchers.IO` and most repository calculations run on `Dispatchers.Default`; this audit does not establish a main-thread ANR during creation. The modal disables interaction, which itself makes a pending asynchronous save feel like an app freeze.

The older Gson/R8 batch serialization defect is already fixed in current source. The existing 22 October investigation records that the packaged V81 models were checked. It should not be used as the primary diagnosis for this V81 report. The installed APK itself was not extracted or compared byte-for-byte.

## Findings, in repair order

### 1. P1 — Accepted writes remain blocked on full read-back and reminder work

**Evidence:** [HabitRemoteStore.send](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/data/HabitRemoteStore.kt:145) verifies acknowledgements, then calls `applySnapshot(readSnapshot(owner, null))` before returning. [applySnapshot](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/data/HabitRemoteStore.kt:115) awaits `reminders.replace` before publishing any of the new projection.

**Effect:** Create, edit, completion/undo, archive, and delete all incur a write plus a full account history fetch and local reminder work. Even a create with reminders disabled takes this path. If the GET fails after a successful POST, the form reports “Unable to save habit” and still displays the old projection. Reminder cache or scheduler failure produces the same misleading outcome.

**Reproduced:** Four new Android tests separately hold the post-write GET, fail that GET, hold reminder replacement with the new habit's reminder disabled, and throw from reminder replacement. In each failure case the fake server contains the accepted habit while Android reports failure and has not published it.

**Repair:** Distinguish accepted, unconfirmed, and refresh-failed outcomes. Prefer an authoritative changed-record response (plus version) so an acknowledged mutation can update the projection without another full download. Publish accepted data before independent reminder work; queue/retry reminder reconciliation separately. Preserve account checks and stable mutation IDs. Do not simply close the form before the server accepts the mutation.

### 2. P1 — No total save deadline, and the modal locks the user inside the wait

**Evidence:** [Form submit/dismiss](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/ui/HabitFormBottomSheet.kt:106) sets `busy` before submission; dismiss returns while busy, and form controls/close are disabled. There is no bounded operation deadline. The shared [OkHttp configuration](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/di/NetworkModule.kt:140) sets 30-second connect/read/write timeouts but no `callTimeout`. Those are not one total deadline covering mutex wait, multiple HTTP calls, pagination, auth refresh, and local work.

**Effect:** Saving can last substantially longer than 30 seconds. A slow stream can keep a read alive; a queued operation has not even started its network timeout. The form gives no stage, elapsed time, or recovery action while waiting.

**Repair:** Establish a total operation budget, bounded queue wait, and explicit timeout/uncertain-save state. Preserve the operation identity so retry first reconciles an ambiguous server result. Handle deadline expiry distinctly: coroutine timeout cancellation must not merely follow the existing generic `CancellationException` rethrow and leave a still-mounted form busy. Allow leaving the form while a ViewModel-owned operation tracks the result, or provide a safe cancellation/recovery action.

### 3. P1 — Background refresh and user actions share a lock across network and disk work

**Evidence:** [refresh](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/data/HabitRemoteStore.kt:60) and [write](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/data/HabitRemoteStore.kt:158) hold the same mutex for their entire operations. [HabitScreen](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/ui/HabitScreen.kt:83) polls after each refresh plus up to 15 seconds. [Application startup](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/SafarApplication.kt:98) also refreshes on login/startup. Retry can launch additional refreshes. Legacy import runs inside this same refresh critical section, including sequential batches and final read-back.

**Effect:** A create tapped during a slow poll waits before its POST begins. Rapid checkbox taps launch more writes, each with its own full read-back. Since rows do not expose an in-flight state, repeated taps can carry the same old `currentlyDone` value and submit repeated “done” operations rather than reflecting the intended sequence.

**Reproduced:** The Android diagnostic suite verifies repeated taps using the same displayed completion state generate two separate completion batches. Lock contention itself is established by the shared critical sections; no production queue duration was measured.

**Repair:** Coalesce refreshes, avoid queued redundant polls, and serialize writes intentionally without allowing background reads to monopolize the save pipeline. Track pending operations by habit/date, disable or coalesce that cell while pending, and resolve desired state against the current model. Any lock refactor must retain snapshot version and account isolation guarantees.

### 4. P2 — Create retry protection disappears after process restart

**Evidence:** [createIds/retryBatches](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/data/HabitRemoteStore.kt:39) are in-memory maps. Only the legacy import queue is durable. The reminder cache stores confirmed snapshots, not ordinary pending mutation identities.

**Reproduced:** The server accepts a create and its response is lost; constructing a new store simulates a process restart. Retrying the same form creates a different habit ID and produces two identical habits. Same-process retries are already covered by existing tests and generally retain the identity.

**Repair:** Persist the pending operation ID and immutable payload until its outcome is reconciled, or persist a backend operation token. Bind it to a specific form operation and account. Do not deduplicate all intentional same-name habits globally. Changing form details after an uncertain create also needs an explicit resolution path.

### 5. P2 — Every unchanged poll still rewrites the reminder snapshot and reconciles all habits

**Evidence:** The unchanged branch of [refresh](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/data/HabitRemoteStore.kt:75) calls reminder replacement. [Scheduler.replace](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/reminders/HabitReminderScheduler.kt:55) serializes/persists all nondeleted records, then reads them back and visits each habit to schedule/cancel alarms and notifications. [HabitReminderStore](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/reminders/HabitReminderStore.kt:32) uses synchronous `SharedPreferences.commit()`. These records include completion and schedule history, even for habits without reminders.

**Effect:** Successful unchanged polling is not a cheap no-op. It creates repeated serialization, disk, and Binder work while the remote-store mutex is held. The reported habit having its reminder disabled does not bypass this work. This runs off the UI thread, but extends save queueing and consumes resources.

**Repair:** Separate cache/alarm restoration at startup/settings changes from normal polling. Track reconciliation by snapshot version and relevant reminder changes. Store only the history needed to determine reminders and reconcile changed alarms. Keep a durable recovery mechanism rather than removing persistence.

### 6. P2 — Backend operations scale with the whole account history, even for one checkbox

**Evidence:** [atomicState](/Users/shashankkumar/Documents/SAFAR_PARENT/SAFAR/server/services/habit-sync.ts:85) fetches the entire state document. [readHabitSnapshot](/Users/shashankkumar/Documents/SAFAR_PARENT/SAFAR/server/services/habit-sync.ts:120) checks `knownVersion` only after that fetch. [applyHabitBatch](/Users/shashankkumar/Documents/SAFAR_PARENT/SAFAR/server/services/habit-sync.ts:183) clones all habits and replaces the entire `habits` array for a single completion. Pagination limits habit count, not nested completion history; every page re-fetches the same state document.

**Reproduced:** Two backend diagnostic tests verify an unchanged poll uses `findOne` without a projection and a single completion's update includes an unrelated habit in the rewritten array.

**Effect:** Database transfer, memory allocation, cloning, JSON serialization and writes grow with account history. “Unchanged” saves HTTP response bytes but still reads the full database document. Habit pagination does not bound a large habit's history payload. There are indexes and atomic version checks, so this is not simply a missing-index diagnosis.

**Repair:** Start with a lightweight version-only read for unchanged checks; fetch a version-consistent full state only when required. Add delta/changed-record responses. Evaluate storage partitioning and bounded history reads while preserving standalone-Mongo atomicity, idempotency, and account-deletion protection. This is a confirmed design cost, not proof that a 10-habit account currently saturates MongoDB.

### 7. P2 — Backend acknowledgement waits for receipt persistence after the commit

**Evidence:** [applyHabitBatch](/Users/shashankkumar/Documents/SAFAR_PARENT/SAFAR/server/services/habit-sync.ts:207) atomically commits the snapshot and receipt outbox, then awaits [drainReceiptOutbox](/Users/shashankkumar/Documents/SAFAR_PARENT/SAFAR/server/services/habit-sync.ts:171) before returning. The drain writes each receipt sequentially and clears the outbox. A batch of 100 new mutations therefore adds 100 sequential receipt upserts and a cleanup update after its atomic commit. Recovery may add another drain/re-read cycle.

**Reproduced:** The third backend diagnostic test holds a receipt upsert, observes the already-committed state/version, and confirms the save promise is still pending.

**Effect:** Slow receipt storage prolongs the Android POST even though the accepted write and retry protection are already durable. This particularly increases legacy import latency.

**Repair:** Bulk-write receipts where safe and separate acknowledgement from durable-outbox draining. Any asynchronous drain must be recoverable across process death and coordinate concurrent writers; do not remove the outbox or blindly clear receipts.

### 8. P2 — Additional UI latency: eager daily rows and export file writes on Main

**Evidence:** [TodayView](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/ui/TodayView.kt:90) wraps every daily habit in one lazy-list `item` and uses `forEachIndexed`, so all rows in that card compose/layout together. Weekly cards correctly use keyed lazy `items`. [Export handler](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/ui/insights/HabitInsightsScreen.kt:197) resumes its Compose coroutine after CSV generation and calls [exportAndShare](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/export/HabitExportManager.kt:134), which creates directories and writes the CSV synchronously before opening the share sheet.

**Effect:** Daily rendering can become expensive with many habits. Large exports can block the UI thread during file creation/write. Neither establishes the cause of the creation screenshot with 10 habits.

**Repair:** Use keyed lazy rows with suitable grouping/dividers. Move CSV file creation/write to IO and return to Main to launch sharing. Bound large custom export ranges and compute only selected habits where possible.

### 9. P2 — “All Time” export derives its start from the selected year's habits

**Evidence:** [Insights availableHabits](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/viewmodel/HabitInsightsViewModel.kt:139) comes from `yearHabits`; [generateExportCsv](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/viewmodel/HabitInsightsViewModel.kt:211) uses that list to choose the earliest date for ALL_TIME. The export habit picker uses the same year-scoped list.

**Example:** An account has an archived habit from a previous year and a new current-year habit. From the current-year view, the previous habit is absent from `availableHabits`; ALL_TIME begins at the new habit's creation and excludes the older archived history. Selecting another heatmap year can change the export start. If the year list is empty, the hardcoded 2020 fallback can also exclude older data.

**Repair:** Derive account-wide earliest date and exportable habits from an account-wide snapshot independently of the heatmap year. This is a source-traced correctness finding; no new export integration test was run for this case.

### 10. P2 — Errors hide the stage and evidence needed to diagnose the real device

**Evidence:** [HabitFormBottomSheet](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/ui/HabitFormBottomSheet.kt:127), [write error handling](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/habits/data/HabitRemoteStore.kt:163), and `refreshBackend` turn most exceptions into generic user messages without retaining structured stage timings. There is no habit-specific correlation ID linking a UI operation to POST, GET, reminder reconciliation, and backend database stages. Existing debug BODY logging is not a production latency measurement.

**Effect:** Invalid input, connectivity, auth refresh, write failure, acknowledged-write/read-back failure, and reminder failure can look alike. The current screenshot cannot tell which stage is pending.

**Repair:** Record queue wait, POST duration/status, acknowledgement version, snapshot page count/bytes/duration, projection time, reminder time, and total time. Record server auth/account lookup, state read, CAS/retry count, receipt drain, and response timing using the same operation/correlation ID. Avoid logging habit names, tokens, or full payloads.

## Other audit observations

- Daily/weekly/monthly flows stay subscribed together through the main ViewModel even when one tab is visible. Each new projection triggers several independent filters/joins. The expensive repository and insights calculations generally run on Default, which is good; consider a shared immutable indexed projection and distinct data flows rather than rescanning all completions per consumer.
- Archived history remains available through the historical insights/export repository path, but weekly/monthly grids use the active-only repository path. Therefore archiving hides its past grid rows although the backend retains the data. This is a presentation inconsistency to resolve explicitly, not evidence of backend deletion.
- The backend checks account existence and ownership on every request, and auth additionally depends on token-store availability. Mongo pool waits and auth refresh are additional possible latency stages. Their actual production contribution was not measured.
- Legacy migration is durable and read-back protected, but serial migration batches can monopolize the shared lock. Show separate migration progress and disallow/queue creates deliberately during initial migration rather than presenting an opaque Saving state.
- The current backend uses standalone-compatible atomic snapshot writes. Old documentation saying a Mongo replica set is required is stale. No transaction-configuration failure was found in the current implementation.
- Existing owner checks, pagination conflict detection, stable same-process retries, schedule revisions, and the V81 `@Keep`/wire-name serialization fix should be preserved during repair.

## Verification and limits

| Verification | Result |
| --- | --- |
| Android habit unit tests | **87 passed**, 13 suites, zero failures/errors; includes 6 new diagnostic reproductions |
| Backend/routes/web habit tests | **32 passed**, 4 skipped; includes 3 new backend diagnostic reproductions |
| Skipped real-Mongo tests | 3; `HABIT_TEST_MONGODB_URI` was not configured |
| Skipped browser integration | 1; existing opt-in browser test was not enabled |
| Source changes | Two new diagnostic test files and this audit; no production implementation changes |
| Runtime scope | Isolated mocked Android dependencies and in-memory backend database; no authenticated production save, affected-device trace, or on-device latency measurement |

Diagnostic tests assert the **current problematic behavior**, so passing them confirms the reproduction; it does not mean the defects were repaired. Convert them to desired-behavior regression tests during implementation.

Commands:

```sh
# Safar_Android_v5
./gradlew :app:testProdDebugUnitTest --tests 'com.safarparmar.app.feature.habits.*' --console=plain

# SAFAR
npx --no-install vitest run server/routes/__tests__/habits.test.ts server/services/__tests__/habit-sync.test.ts server/services/__tests__/habit-sync-mongodb.integration.test.ts server/services/__tests__/habit-latency-audit.test.ts client/features/habits/__tests__
```

[Android reproductions](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/test/java/com/safarparmar/app/feature/habits/HabitLatencyAuditTest.kt), [backend reproductions](/Users/shashankkumar/Documents/SAFAR_PARENT/SAFAR/server/services/__tests__/habit-latency-audit.test.ts), and [verification logs](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/Outputs/logs/habit-latency-audit-2026-10-27/android-tests.log) are retained in the workspace. The earlier [V81 serialization investigation](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/Docs/habit-save-failure-2026-10-22.md) supplies historical release evidence, not a new production test.

## Recommended implementation sequence

1. Fix the accepted-write versus read-back/reminder outcome boundary; make accepted changes visible immediately from an authoritative result.
2. Add bounded save/queue handling, recoverable operation identities, and per-cell pending/coalescing behavior; coalesce background refreshes.
3. Remove unchanged-poll reminder rewrites; make reminder side effects independent and retryable.
4. Optimize backend version reads and receipt draining, then design delta/history storage changes using representative account sizes and concurrency tests.
5. Fix daily lazy rows, export IO and account-wide export bounds. Add operation-stage telemetry and run a V81-to-fixed-build slow-network smoke test on the affected device.

A release should verify slow GET after accepted POST, lost POST response followed by restart/retry, disk/reminder failure, account switch during each stage, long legacy import, rapid repeated toggles, and background/resume. Measure p50/p95/p99 stage durations and actual frame/ANR traces before attributing the phone's freeze to a particular infrastructure component.
