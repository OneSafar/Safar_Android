# Ekagra process-death recovery — 17 October 2026

This follow-up supersedes the checkpoint-only recovery policy in the initial [session-loss investigation](ekagra-session-loss-2026-10-17.md). The user explicitly requested continuation across process death. The production audit remains read-only; no historical durations or account records were altered.

## Confirmed failure and scope

TimerService returned START_STICKY but its new process never restored the timer clock, session metadata, or foreground state. Journal initialization finalized a previous-process active row using its last recorded seconds. The deadline receiver also finalized that checkpoint when no live service existed. Consequently, an OS restart could turn a running timer into an idle service and a short, permanently finalized history fragment. This applies to every owner using those code paths; there is no account-specific condition.

No automatic five- or twelve-minute end was found in the Android timer. Historical records do not contain Shelly's process-exit reason, so the particular OS/OEM event that killed or stopped her process remains unknown. A read-only attempt to retrieve the production app's Crashlytics report using the already configured Firebase CLI account returned HTTP 403 (caller lacks permission); no Firebase settings or issue states were changed. The code correction addresses a demonstrated failure after a process exit; it does not establish that all 1,223 recent interrupted users share one OS kill cause. The ten other accounts with a two-hour plan and an interrupted 11–13 minute save, plus separate Stopwatch complaints, are covered by the same generic recovery logic going forward.

## Resulting behavior

- Active focus progress and the entire local recovery clock are committed atomically in the same owner/session Room row, without a database schema migration. Workers and History no longer finalize a resumable active row.
- A same-boot running timer advances from elapsedRealtime and boot identity, independent of wall-clock changes, ticker execution, network availability, or restart delay. A countdown finishes at its original boundary and cannot earn more than the selected duration.
- Sticky restart promotes the foreground service before asynchronous storage reads. Account recovery blocks Start/configuration until restoration completes. A five-minute recovery alarm supplements the timer deadline; idle-mode/inexact alarm delay cannot shorten the durable timer clock.
- Automatic Pomodoro recovery follows the saved schedule and counts only focus periods. Manual breaks restore their suspended focus paused when the break ends. Paused sessions earn no missing-process gap. Stopwatch preserves elapsed seconds.
- Reboot/unknown boot identity restores the checkpoint paused, with no credit for powered-off time. Where Android supplies a relevant explicit user-stop exit reason, reopening also restores paused. The app does not override an explicit system Stop with an alarm restart. Older Android versions without process-exit history cannot provide that attribution.
- Session ID, account, title, goal and planner-topic metadata survive recovery. Screen recreation attaches to that identity; a premature Start tap cannot mint a separate screen-only ID. Final upload stays idempotent. Rank attendance is still backend-controlled and is not replayed as offline attendance.
- Ending an empty timer writes a terminal tombstone so a restart or delayed checkpoint cannot resurrect it. A failed older checkpoint cannot replace a newer recovery anchor; a durable sequence also protects pause/resume transitions occurring in the same monotonic millisecond.
- The silent study service uses specialUse on Android 14+ and explicitly survives task removal. It no longer depends on a mediaPlayback declaration for a silent timer. The subtype explains its user-started timer and persistent controls.

## Diagnostics and remaining OS limits

Recovery now correlates ApplicationExitInfo with the active session's checkpoint and the app's main process. It retains 64 local diagnostic events and reports relevant exits as deduplicated Crashlytics nonfatals, including reason, timestamp, status, PID, importance, device/build, battery restrictions and exact-alarm access. Foreground-start and timer-coroutine failures also produce explicit diagnostics. A failed display coroutine is restarted by the timer alarm instead of silently leaving a nonfunctional ticker. Compatibility preference-write failures no longer kill the process or permanently stop their writer; Room remains authoritative.

Android and OEM software can still terminate a process, deny a background foreground-service start, or force-stop an application. Accurate same-boot recovery does not require continuous execution. The app cannot continue executing while Android keeps a force-stopped package stopped, and reboot intentionally pauses the session. Exact/inexact alarm permission remains optional; no broad battery exemption or silent audio loop was introduced.

Android's documented behavior informed the lifecycle handling: [START_STICKY](https://developer.android.com/reference/android/app/Service#START_STICKY), [user-initiated foreground-service stops](https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping), and [foreground-service types](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use). The new specialUse subtype must be included in the Play Console foreground-service declaration when this update is released.

## Validation

- 733 JVM tests pass, including 25 new recovery-clock tests and four process-exit correlation tests.
- All 34 Ekagra service/Room/alarm instrumentation tests pass on the Android 17 emulator: eight service tests, 24 journal tests and two alarm tests. No tests failed or were skipped in that selected suite.
- All nine normal-process SIGKILL recovery cases pass. Each keeps the original client identity and produces exactly one final record. The two-hour countdown completes at exactly 7,200 seconds. Paused/manual-break cases preserve 720 focus seconds; a Pomodoro with 54 pre-kill focus seconds recovers 66 focus seconds after real downtime across its break. Goal and planner-topic associations remain intact.
- Separate real force-stop and reboot checks pass and restore paused. The reboot check retains 721 seconds from a 720-second seed: the live ticker durably recorded one additional second between seeding and reboot. Powered-off time was not credited.
- An additional two-process instrumentation check passes for stale-alarm rejection followed by recovery and exactly-once save.
- Autonomous foreground restart was observed before reopening for Focus and Stopwatch, with new live foreground PIDs and specialUse service state. Exact-alarm access was denied and the app had no battery-optimization exemption on this emulator.
- Repeated rapid SIGKILLs trigger Android service-restart backoff. A later test observed the service queued for a delayed restart in dumpsys. The full nine-case persistence matrix therefore uses --skip-background-check to verify recovery across that delay; it does not claim immediate autonomous restart after every repeated kill. A five-minute alarm complements the permitted service restart, while the durable clock preserves time independently.
- The merged production release manifest was checked: the offline emulator probe Activity is absent, TimerService uses specialUse, and stopWithTask is false. No release APK/bundle was assembled.
- The actual-kill probe is debug-only, refuses non-emulators, online devices and real credentials, and uses synthetic account IDs. It starts a normal app service, finishes its Activity, turns the screen off, kills the process with SIGKILL, and observes a different foreground PID before reopening. A separate instrumentation probe also verifies identity, exact finalization and stale-alarm rejection.
- Instrumentation alone is insufficient to prove autonomous sticky restart: killing an instrumented process also causes Android's test runner to tear down its services. The normal-process probe avoids that testing artifact.
- Production interval constants remain unchanged. Long pre-kill progress is simulated by moving test-only monotonic anchors; the gap and SIGKILL/reboot are real OS actions. A successful synthetic two-hour test is not a physical two-hour endurance run.

Reproduction scripts: `scripts/debug/ekagra-background-restart.py` and `scripts/debug/ekagra-process-death.py`. Logs and machine-readable results are under `Outputs/logs/ekagra-process-death-2026-10-17/`. The usual instrumentation build remains blocked by unrelated Habit migration/reminder test references; the temporary Ekagra-only source-set init script described in the initial report isolates these tests without changing the repository's build configuration. The debug build also excludes the existing copyApksToOutputs finalizer when packaging app and test APKs together, avoiding its undeclared task dependency.

| Evidence | Result artifact |
| --- | --- |
| Nine normal-process kill cases | kill-recovery-matrix.json |
| Autonomous Focus restart | native-focus.json and native-focus-focus-services.log |
| Autonomous Focus/Stopwatch observations before later restart backoff | native-kill-matrix.json and its service dumps |
| Explicit stop | force-stop.json |
| Actual emulator reboot | reboot.json |
| Stale alarm plus two-process recovery | stale-alarm-and-recovery.json |

All case durations and PID evidence are synthetic QA data. The scripts never log in or use production credentials. Debug-only probes use actual Service lifecycle calls and Room; they are not shipped in the release manifest.

## Release and historical data

The changes are local source changes and debug verification builds. No release was uploaded or deployed; the app version remains 76 / 1.6.76. Existing installed clients need an update containing this code. Session fragments already finalized by older versions lack the monotonic/boot recovery state, so they cannot be expanded into historical study hours from plans alone. Such older checkpoints retain conservative checkpoint-only migration.

A physical-device 04:00–06:00 screen-off run on the affected phone, and Samsung/Oppo battery-management validation, remain necessary to establish that device's kill behavior. The diagnostics in this change make the next interruption attributable instead of merely recording a short fragment.
