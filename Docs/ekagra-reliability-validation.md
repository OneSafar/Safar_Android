# Ekagra reliability validation

## Behavior

- Four active focus hours trigger the check-in; its hidden 30-minute response window remains unchanged. Personal timers keep running after expiry; ranked credit remains backend-controlled.
- Removing the Recent Apps task leaves the foreground timer running.
- Notification End & save (including old Reset actions) queues recorded time. Stale actions cannot operate on a different session.
- An actual process restart finalizes the last durable focus checkpoint as interrupted. It does not resume the timer or add time spent while the process was dead.
- Room serializes checkpoints, finalization, and upload acknowledgements. Active-to-pending is transactional. Five-second checkpoints are possible while Android allows execution; this is not a guarantee that Android runs code every five seconds during sleep.
- Completed and interrupted sessions stay visible while offline with a waiting-to-sync label. Retry preserves the same client identity. Account ownership is checked in the app and in the updated backend.
- Exact alarm access is optional. Without access, Android's inexact fallback may deliver late. Notification permission, channel settings, force-stop, and OEM restrictions remain under the device's control.

## Deployment order

Deploy the targeted SAFAR backend changes before distributing the Android update. They add account checks and source-session identity/end-reason fields for History correlation; existing clients remain compatible. No leaderboard policy or production interval changes are part of this patch.

The Android version remains 63 / 1.6.63. A built bundle at this version is a verification artifact, not a new Play upload if version 63 was already used.

## Automated coverage

- Room instrumentation: process recreation, same-process reuse, duplicate finalization, late checkpoints, upload acknowledgement, account isolation, failed final write and retry, legacy migration, and interrupted Pomodoro/manual breaks.
- Service instrumentation: Recent Apps callback, four-hour check-in, expiry, stale notification action, End & save, Stopwatch, and both Pomodoro styles. Test-only monotonic anchor changes simulate elapsed time; production timing constants are unchanged.
- Unit tests: pending History display, duplicate suppression through client/server identity, break exclusion, existing focus accounting/Pomodoro/presence rules.
- Backend tests: account mismatch, exact interrupted duration, retry deduplication, History correlation, and existing ranked-attendance behavior.

## Local verification record

- Android Studio JBR used for Kotlin compilation and Gradle verification.
- 359 Android JVM unit tests passed; 12 instrumented tests passed on the Android 17 emulator.
- 29 targeted backend API/policy tests passed, including History projection and account mismatch checks.
- SAFAR TypeScript checking (`npx tsc --noEmit`) passed.
- Release packaging completed and jarsigner verified the bundle signature. The user subsequently requested no final bundle; this artifact is unused and no additional bundle build or upload is planned.
- Two additional emulator tests passed for alarm registration with exact access denied and for device-sound/vibration channel configuration. Exact-access-granted delivery and physical sound/vibration remain unverified.
- Samsung/Oppo overnight tests and actual device sound/vibration checks: not run; those phones are not connected to this workspace.

## Required physical-device checks before public rollout

These cannot be replaced by emulator tests. Record device model, Android/One UI/ColorOS version, SAFAR version, battery mode, notification permission, alarm access, and connectivity.

1. Run a real five-hour focus timer on the affected Samsung and on Oppo, including screen-off charging overnight. Record the four-hour alert, expiry at four hours thirty minutes if unanswered, five-hour completion, saved seconds, and backend record.
2. Repeat with Sound and Vibration; confirm the device's default notification sound or vibration is used. Check exact-alarm allowed and denied behavior; the denied case may be delayed.
3. During active focus, swipe Recent Apps, pause/resume through the notification, and return to SAFAR. The same session should remain active with correct elapsed time.
4. Force-stop or reboot after recording focus time. Reopen offline: one interrupted entry should show the last checkpoint, with no overnight time added. Reconnect: it should upload once and lose the pending label.
5. Complete a session offline, reopen the app, and reconnect. Verify no missing or duplicate History entry. Test logout/login into a different account before reconnecting; the original account's session must remain isolated.
6. Run Traditional and Custom Pomodoro with auto-start enabled and disabled, including an interruption during a break. Only completed/recorded focus time belongs in the focus total.

Keep this build in internal testing until these device results pass. The exact cause of the original Samsung incident remains unconfirmed without its diagnostics.
