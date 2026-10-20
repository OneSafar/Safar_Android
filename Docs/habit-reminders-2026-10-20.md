# Habit reminder implementation — 20 October 2026

The reminder blocker was a missing implementation: both tracked scheduler/receiver files were empty. The old instrumentation test also depended on the legacy Room habit database, which production now uses only for one-time imports. Simply adding that old DAO constructor would have made a test compile while leaving actual server-backed habits without reminders.

## Implemented behavior

- Habit creation and editing now expose an optional reminder switch and time input. The time is sent through the existing repository and server mutation contract. Turning the switch off saves `null`. New labels and notification text have English, Hindi and Hinglish resources.
- Every confirmed full server snapshot updates a durable, account-scoped reminder projection. The backend remains authoritative; the cache never saves or mutates a habit independently. Failed/unconfirmed network writes do not publish guessed reminder changes.
- An unchanged server response republishes the previously confirmed projection. This restores reminders after logout/login or local cache loss even when the server version has not changed.
- Startup/login refreshes confirmed habits in an application background coroutine. Existing reminders therefore do not require opening Habit Tracker after an upgrade. Offline delivery uses the last confirmed projection.
- Explicit, immutable PendingIntents identify alarms by account and remote habit ID. Android inexact alarms survive ordinary app process death. The receiver also restores scheduling after reboot, app replacement, wall-clock changes and time-zone changes.
- Delivery checks the signed-in account, SAFAR notification opt-in, Android notification permission/channel, current dated recurrence, completion status, reminder time and snapshot token. A stale, early, duplicate or previous-day alarm cannot post a reminder.
- A delivered date is persisted per account/habit. Delivery schedules the next eligible occurrence, including when today's notification is suppressed by completion or OS notification settings. Edits, disabled reminders, archive and deletion replace/cancel alarms appropriately. Logout/account changes cancel old alarms and notifications and clear old account cache data.
- Notification taps open Habit Tracker. Notifications have separate account/habit tags, so different reminders do not overwrite each other.
- Scheduling follows local wall-clock time, including DST gaps/overlaps. It requires no additional exact-alarm permission. Android Doze/battery saving can delay inexact delivery, which the form explains. Android user force-stop prevents background delivery until the app is reopened; this is distinct from ordinary process death.

No foreground service, continuously running timer or database schema change was added for reminders. Existing Ekagra recovery/accounting code is unchanged.

## Validation

- `testProdDebugUnitTest`: **774 passed**, zero failures, errors or skips across 130 suites. This includes **33 new regressions**: 29 reminder policy/cache/orchestration tests, three server-integration tests and one notification-route test.
- `compileProdDebugAndroidTestKotlin`: **passed with the full source set**, including `HabitReminderInstrumentedTest`. No source exclusion or temporary test filter is used.
- Production debug Kotlin, Java and Hilt dependency-injection compilation: **passed**.
- Production release Kotlin, Java and Hilt dependency-injection compilation: **passed**.
- Full `lintProdRelease`: **passed**, zero errors/fatal findings, **906 warnings and eight hints**. No lint baseline, error suppression or disabled rule was added. Warnings are not represented as resolved. Three new `UseKtx` style warnings concern checked synchronous preference commits; the checked result reports disk-write failures, and the write must be durable before scheduling/delivery state is acknowledged.
- `git diff --check`: passed. All existing `Outputs/*.apk` and `Outputs/*.aab` hashes remain unchanged. The isolated validation tree contains no generated APK/AAB.

`verified-checks.log` records the final invocation after the notification-settings Intent was made safe for wrapped popup contexts. A new tutorial locale read found by the first isolated lint pass was also changed to Compose’s observable locale; its formatting behavior is preserved and now follows language changes.

No emulator/device is opened by this task; instrumentation tests are compiled only. No APK or AAB is rebuilt for this request.

Checks run from `/tmp/safar-habit-checks-20261020`, an isolated copy whose app sources and build configuration are hashed and compared with the shared checkout. Concurrent builds in the shared checkout first caused incremental-cache conflicts; simultaneous debug/release compilation also exhausted the original compiler heap. The successful JVM/instrumentation compile uses one worker and a temporary 6 GiB in-process compiler heap. These are invocation flags, not app/build-configuration changes. Fresh isolated compilation also cleared the intermediate Hilt annotation-cache failure. Earlier failed attempts remain in the logs.

Invocation (no assemble, APK packaging, app bundle, signing or connected-test task):

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew \
  :app:testProdDebugUnitTest :app:compileProdDebugAndroidTestKotlin :app:hiltJavaCompileProdRelease :app:lintProdRelease \
  --continue --max-workers=1 --no-daemon -Pkotlin.compiler.execution.strategy=in-process \
  -Dorg.gradle.jvmargs='-Xmx6144m -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8' \
  -x :app:copyApksToOutputs --console=plain
```

The updated instrumentation case seeds confirmed reminder data instead of Room and retains actual notification, duplicate suppression, completion, account-isolation and next-alarm assertions. It also recreates the cache/scheduler to exercise durable state. This case still requires a device run by the user.

JVM regression coverage includes daily/weekly recurrence, dated schedule revisions and start dates, completion/undo, invalid times, future completed occurrences, stale/duplicate/previous-day/early delivery, DST and time-zone changes, cache recovery/corruption/commit failures, account changes/logout, permission revocation/channel blocking, notification posting and rescheduling, and confirmed versus failed server writes.

The existing APK/AAB files predate these reminder changes and therefore do not include them. Artifact SHA-256 values were captured before the changes and are checked after validation. The isolated validation directory must contain no APK/AAB output.

Logs: `Outputs/logs/habit-reminders-2026-10-20/`.

## Suggested manual device checks after a future build

1. Enable a habit reminder for a few minutes ahead; save, reopen the form and verify its time. Check the notification opens Habit Tracker.
2. Complete the habit before its reminder. Confirm no notification that day and the next scheduled day still has a reminder. Undo completion before the due time and check it is restored.
3. Change the reminder time/recurrence, disable it, archive or delete the habit. Check the old alarm cannot notify.
4. Kill the app process without force-stopping it, reboot, and change the device time zone. Check recovery and chosen local time.
5. Sign out, switch accounts, then sign back into the original account. Confirm old-account notifications disappear and original reminders return.
6. Deny Android notifications, disable the study-reminder channel, and turn off SAFAR notifications. Confirm these settings are respected; re-enable them and check future delivery.

The broader UI/lint repair explanation is in `Docs/validation-fixes-2026-10-20.md`. Concurrent tutorial work in the shared checkout caused intermediate compilation errors; those are kept in the logs rather than counted as successful validation. A missing `isLightBackground` import in Study Circle was added to unblock compilation without changing its behavior. The new Exam Planner practice calendar now uses `LocalLocale.current.platformLocale`, matching the earlier locale fixes.
