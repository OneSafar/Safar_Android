# Ekagra session-loss investigation — 17 October 2026

The subsequent [process-death recovery fix](ekagra-process-death-recovery-2026-10-17.md) supersedes the initial checkpoint-only recovery limitation and release status below. The production evidence and counts in this report remain the original read-only snapshot.

## Read-only production evidence

Android was examined first. Production data was read without calling application startup, running migrations, changing records, repairing durations, or deploying anything. The report is based on a snapshot completed at **17/10/2026, 10:57:43 IST** (2026-10-17T05:27:43.123Z). Counts may change as delayed offline sessions arrive. No credentials or other users' contact details belong in this report.

### Reported account

The account supplied by the user has one active Android production device registration reporting **1.6.76**, last seen **17 October 2026 at 04:04:06 IST**. This is device-token metadata; versions are not stored on individual focus sessions. Manufacturer, device model, Android version, battery restrictions, and process-exit reasons are not present in the inspected session or device records.

For **17 October 2026 IST**, four saved focus records explain the roughly 12-minute total:

| Started (IST) | Durable end/checkpoint (IST) | Planned minutes | Actual saved focus | End reason | Insert time inferred from Mongo ObjectId (IST) |
| --- | --- | ---: | --- | --- | --- |
| 04:06:19 | 04:15:21 | 60 | 7m 29s | interrupted | 04:20:19 |
| 04:27:44 | 04:27:52 | 60 | 0m 01s | user-ended | 04:27:52 |
| 04:28:13 | 04:28:25 | 25 | 0m 11s | interrupted | 04:47:02 |
| 04:47:07 | 04:52:10 | 25 | 5m 03s | interrupted | 04:53:44 |

The exact saved total is **764 seconds (12 minutes 44 seconds)**, matching the supplied History screenshot, including all four row durations and labels. Three records are explicitly interrupted; one is explicitly user-ended. There is **no complete two-hour record** for this account on this date in the inspected canonical collection. The client submitted 60-minute and 25-minute plans with short recorded fragments; those records cannot reconstruct the missing portion of the reported study period.

The interrupted entries were inserted after their stored checkpoint times: approximately **4m 58s**, **18m 36s**, and **1m 34s** later respectively. Mongo ObjectId creation times have one-second precision and are insertion-time evidence, not an explicit application upload timestamp. The route writes the client's endedAt into created_at and completed_at; those fields alone cannot show recovery/upload delay.

For the 11-second entry, ranked-session telemetry was last updated at **04:34:25 IST**, about six minutes after the stored local end/checkpoint at **04:28:25 IST**. The ranked record was not closed. This shows later server-side session activity relative to the durable local checkpoint; it does not identify the lifecycle event, prove the process was alive continuously, or establish how much focus time should be restored.

All four sessions have local-* source identities and a recorded **max_ranked_seconds of 16,200 (4h 30m)**. The associated ranked records contain no credited intervals and attendanceMs is zero. **There is no 12-minute server limit in these records.** Canonical saved actual seconds are already short on receipt; personal saved duration and ranked attendance are separate data. The current save endpoint validates and stores submitted seconds rather than reducing a valid two-hour submission to 12 minutes. No historical hours were added based only on planned duration or wall-clock range.

The same account has **265 canonical focus records** in the broader window, including **7 explicitly interrupted records**, all within the recent window, and **249 records marked completed**. Of the recent interrupted records, several end after approximately five minutes (302, 303, 303, and 320 seconds), including two on 13 October at 04:37 and 04:59 IST. This supports a repeated interruption pattern. A process-kill cause or OEM-specific cause remains unproven without device diagnostics. No matching account feedback or ekagra_mode_sessions record was found.

## Other accounts: scope of the symptom

These queries use focus_sessions with session_type = focus and created_at at or after the stated cutoff. All end reasons below are explicit values; null/absent end reasons are counted separately. The broader window starts **17 September 2026 00:00 IST** (2026-09-16T18:30:00.000Z); the recent window starts **10 October 2026 00:00 IST** (2026-10-09T18:30:00.000Z). Both end at the audit snapshot. They are calendar cutoffs, not exact rolling windows.

| Pattern | Since 17 September IST | Since 10 October IST |
| --- | ---: | ---: |
| All canonical focus records / users | 187,995 / 7,464 | 38,437 / 3,624 |
| Explicit interrupted records / users | 3,867 / 1,555 | 2,989 / **1,223** |
| Users with at least two explicit interruptions | 657 | 532 |
| Interrupted users with an Android registration | 1,550 | 1,218 |
| Planned 120m, actual 11–13m, explicitly interrupted: records / users | 14 / 14 | **10 / 10** |

The supplied account is part of the 1,223 recent users with an interrupted record. The ten recent users in the narrower 120-minute-to-11–13-minute interrupted pattern are **other accounts**; all ten have Android device registrations. An interrupted record can follow a legitimate force-stop, reboot, or other interruption, so these counts are **investigation candidates, not a proven count of the same defect**.

Among recent interrupted users, 1,210 have 1.6.76 as the latest Android device-token version. This is a current user-level association; it does not prove which version created each historical session, and it is not an estimate of that release's failure rate.

The broader planned-120m/actual-11–13m pattern includes legitimate or ambiguous early endings:

| End classification | Records / users since 17 September | Records / users since 10 October |
| --- | ---: | ---: |
| Explicit interrupted | 14 / 14 | 10 / 10 |
| Explicit user-ended | 25 / 21 | 23 / 19 |
| completed = true, reason null/absent | 102 / 86 | 6 / 6 |
| completed = false, reason null/absent | 17 / 14 | 6 / 4 |

Legacy completed flags without an end reason do not prove natural timer completion. These groups must not be combined into a confirmed affected-user total.

## Independent feedback

The same broader window contains **four related feedback records from three other accounts**:

- 29 September and 4 October: the same person reports that Ekagra session time does not appear in the mobile app.
- 18 September: another person reports that stopwatch sessions stop by themselves and are not registered.
- 18 September: another person reports unsaved time and a discrepancy with leaderboard time.

These support a broader reliability complaint but do not establish identical causes. The current Android token versions for these accounts are 1.6.76, 1.6.59, and 1.6.42 respectively; those are not historical release attribution. No matching support-ticket record was found in the queried time window.

## Reproduction criteria for the read-only audit

- Query focus_sessions where session_type is focus and created_at is at least the chosen ISO cutoff; project only user identity, source identity, plan, actual seconds/minutes, completion flags, end reason, mode, and timing fields.
- Resolve actual seconds from actual_duration_seconds when present; otherwise use actual_duration_minutes, falling back to duration_minutes, multiplied by 60.
- Count explicit end_reason = interrupted separately from user-ended and missing/null reasons. The narrow symptom is planned_duration_minutes = 120 and actual seconds in the inclusive range 660–780.
- Group by user_id for distinct-account and repeat-interruption counts. Correlate device_tokens by user_id and platform = android only as registration evidence. Select the latest last_seen_at token for current version context; never treat this as per-session metadata.
- For the supplied account, inspect the canonical records, matching ranked_focus_sessions by userId plus source_session_id/sessionId, and Mongo ObjectId timestamps to distinguish the stored checkpoint time from insertion time. Read-only collection queries avoid application initialization and its possible index/migration side effects.

Private selected-contact and candidate-ID evidence is stored outside both source repositories in the locally ignored, access-restricted directory /Users/shashankkumar/Documents/SAFAR_PARENT/.local-audits/ekagra-save-2026-10-17/. It is not required to review code changes. No users were contacted.

## Code fixes and verification

### Confirmed Android defects corrected

The foreground timer already measures a running period with SystemClock.elapsedRealtime(), which includes device sleep. Its displayed StateFlow values are updated by a coroutine ticker; that ticker does not necessarily execute while the phone sleeps. Several consumers incorrectly treated the last displayed seconds as current progress:

- onDestroy finalized the last display value without reconciling the running period. onTaskRemoved also wrote a stale checkpoint. Both now reconcile before saving.
- focusProgressSnapshot was read by the End/organize flow before pause() reconciled elapsed time. Because the journal accepts the first finalization for a client ID, the stale screen snapshot could win against the later accurate service save. A public progress snapshot now reconciles first and includes suspended focus time without crediting break time.
- startBreak copied stale remaining seconds into the suspended focus state. It now reconciles before capturing that state, for both Focus and Pomodoro.
- A duplicate start() call replaced the original monotonic anchor with stale displayed progress. Start is now idempotent while a period is running.

Separately, journal reads or acknowledgement writes could throw into the manual-save coroutine after an HTTP save. Upload/drain/link entry points now return retryable errors for these failures and preserve coroutine cancellation. The manual-save paths no longer perform an extra, unprotected journal read before the worker's authoritative read. Exact seconds and stable client identity remain in the durable queue for a retry.

These are reproduced code defects compatible with loss of recorded progress. The production records do not establish which lifecycle event or defect affected each of the reported fragments. In particular, an abrupt process kill that never invokes onDestroy still recovers only the last durable checkpoint. The existing rule against crediting unobserved process-dead time remains in place; the patch does not invent missing historical hours.

### Automated verification

- All **704 JVM unit tests passed**, including three new sync-failure/cancellation tests.
- **22 Ekagra instrumentation tests passed** on the Android 17 emulator. They include four new service regressions and a new Room acknowledgement-failure regression. The service regressions simulate elapsed time by moving existing monotonic anchors; production timing intervals are unchanged.
- The lifecycle regression freezes the display at 12 minutes of a two-hour timer, advances the live period to its deadline, releases the activity binding, then uses actual Android stopService/onDestroy. The saved record contains **7,200 seconds**, rather than 720. Independent tests cover duplicate Start, snapshot-before-pause, and Focus/Pomodoro manual breaks without break-time credit.
- The Room regression injects an acknowledgement disk failure after an upload and verifies that the same client identity and all 7,200 seconds remain queued; a subsequent acknowledgement keeps the same server identity.
- git diff --check passed.

The normal instrumentation build currently fails while compiling unrelated HabitMigrationInstrumentedTest and HabitReminderInstrumentedTest references (MIGRATION_4_5, getOwnerlessHabits, and reminder classes). Ekagra instrumentation was compiled using a temporary Gradle init script that restricted both androidTest.java and androidTest.kotlin source directories to src/androidTest/java/com/safarparmar/app/ui/ekagra. The repository build configuration and those unrelated tests were not changed. The final run used the emulator with Wi-Fi and mobile data disabled; the tests exercise local persistence and failure handling.

To repeat the isolated run, create a temporary Groovy Gradle init script:

```groovy
allprojects {
    afterEvaluate { p ->
        if (p.path == ':app') {
            def ekagraTests = new File(p.projectDir, 'src/androidTest/java/com/safarparmar/app/ui/ekagra')
            def tests = p.android.sourceSets.getByName('androidTest')
            tests.java.setSrcDirs([ekagraTests])
            tests.kotlin.setSrcDirs([ekagraTests])
        }
    }
}
```

Run :app:testProdDebugUnitTest and :app:connectedProdDebugAndroidTest with that init script, selecting EkagraServiceInstrumentedTest, EkagraJournalInstrumentedTest, and EkagraAlarmInstrumentedTest through android.testInstrumentationRunnerArguments.class.

### Remaining validation and release status

The fixes are local source changes. No release version was bumped, no release bundle was built, and nothing was deployed. The affected phone's process-exit diagnostics and a real 04:00–06:00 screen-off session are still needed to confirm why it stops and whether the patch resolves that device's incident. Emulator clock simulation does not verify OEM battery management. Production session durations have not been changed.
