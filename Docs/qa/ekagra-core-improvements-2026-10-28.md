# Ekagra core improvements — implementation and release gates

## Scope and data contract

Implemented in `Safar_Android_v4`. The new Room database `ekagra-history-cache.db` is a disposable, account-scoped read cache. It has no connection to the upload queue. A complete non-null successful analytics result is persisted before journal acknowledgement reconciliation; failed cache writes retain journal visibility. Server totals, rounding, session IDs, timestamps and associations remain authoritative. An empty successful response replaces the previous snapshot; a failed or null response does not.

The existing journal remains database version 1. Its schema, migration marker, legacy preference import and upload protocol are unchanged. Its reconciliation method now accepts the fetching account explicitly, preventing a queued response from reconciling another account after a switch. No migration, reset, backfill or re-upload of saved sessions is introduced.

Confirmed title/goal edits patch the read cache. Confirmed deletions persist identity suppression, including known client aliases, to prevent delayed server echoes or journal rows from resurrecting a deleted item. Aggregate values remain unchanged and are marked stale pending refresh. Existing users need one successful refresh to populate their cache.

## Review boundaries

1. History: `EkagraHistoryStore`, repository confirmation hooks, ViewModel read/merge integration, history status text, localized resources and Gson model field preservation.
2. Notifications and End: OS RemoteViews chronometer, publication generation checks, terminal local-write barrier, notification removal acknowledgement, started-service shutdown and account-switch serialization.
3. Timing and overlay: one pure elapsed-time reducer for ticks/alarms/recovery, scheduled completion boundaries, elapsed break records, aligned whole-second recovery checkpoints, and mode-aware floating labels with attachment/removal diagnostics.
4. Verification: cache/upgrade fixtures, reconciliation account isolation, service chronometer/cleanup tests, delayed automatic Pomodoro tests and process-death regression adjustments for separately saved break records.

These boundaries describe reviewable file groups. No commits or pull requests have been created; unrelated existing working-tree changes must not be bundled into these reviews.

## Verification

The QA flavor uses `com.safarparmar.app.qa` and synthetic accounts with isolated/in-memory fixtures. Final full-suite and process-kill verification runs used the fresh `SafarEkagraDisposableQa` emulator. Audit logs and interruption results are retained in the parent workspace under `.local-audits/ekagra-core-2026-10-28/`. The process harness refused to run against a pre-existing emulator QA installation because credentials were present; its guard was retained and no account data was cleared.

Build and JVM command:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew \
  :app:testQaDebugUnitTest :app:assembleQaDebug :app:assembleQaDebugAndroidTest \
  -x :app:copyApksToOutputs --console=plain
```

The existing APK-copy convenience task triggers a Gradle implicit-dependency validation error when assembling app and instrumentation together; it was excluded. APKs are produced in their standard `app/build/outputs/apk` locations. No build-script changes were made for this unrelated issue.

Verified on the API 37 arm64 emulator:

- QA app and instrumentation APK builds passed.
- All 820 JVM tests passed, including 200 Ekagra tests.
- All 58 Ekagra instrumentation cases passed together on a newly created, credential-free disposable emulator: history cache, journal, service lifecycle, alarms and existing tutorial UI.
- All 14 service tests in the final combined run passed, including a simulated terminal-write disk failure: foreground protection remains until retry commits, followed by one final record and foreground removal. The final regression also checks that Retry completes End without requiring another End tap and prevents a new Start during pending cleanup.
- All seven real OS process-kill cases passed on that disposable emulator: focus, focus completion, stopwatch, paused, manual break, completed manual break and automatic Pomodoro chaining. Original focus identities and recorded progress were preserved; completed breaks were saved separately. An explicit force-stop and an actual emulator reboot also passed: each retained exactly 720 recorded focus seconds and restored paused, without crediting the stopped interval.

Exact-alarm registration also passed in separate runs with access explicitly granted and explicitly denied.

The upgrade fixture compares journal payloads and metadata byte for byte across active, pending, acknowledged, synced and zero-duration records, alongside legacy preference values. The cache cold-open test closes and reopens an actual separate on-disk Room database. Service tests assert elapsed anchors/count direction, no per-second reconstruction, no ongoing notification after End and zero foreground service type after cleanup.

## Pending release gates

- Android 26 physical/emulator run and Android 13 OPPO run; the USB phone is currently disconnected and only an API 37 emulator image is available locally.
- Physical overlay permission-denied/attach/remove checks, OEM foreground cleanup, device idle/exact-alarm fallback and reboot checks.
- Upgrade from an actual prior signed-in install using isolated QA copies. Synthetic upgrade-preservation fixtures have passed.
- Review and QA sign-off before staged rollout. No release has been published.

Use existing `EkagraDiagnostics` records and journal save errors to monitor save/recovery failures, duplicate identities, notification failures and stale notifications during staging. Block expansion on any lost duration, changed association, duplicate upload or ongoing notification after a successfully finalized End.

## Rollback

Disable/revert the new history read/presentation integration while retaining `ekagra-history-cache.db` as unused data. Never clear or downgrade `ekagra-journal.db`, migration markers, legacy backups or backend records. Previous code can read the unchanged recovery payload and journal schema; new reducer result fields are transient and are not serialized. Existing saved IDs and durations must not be rewritten during rollback. Timer/notification fixes can be reviewed independently of cache presentation.
