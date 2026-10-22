# YouTube Focus and Kavach mechanism audit — 16 October 2026

## Current behavior — coexistence with an explicit whole-app override

The latest user request supersedes the earlier rule that enabling Kavach globally switched YouTube Focus off. **Both features can remain on together:** Kavach blocks the other selected apps, while YouTube Focus manages channels and Shorts inside YouTube.

| Action | Current result |
| --- | --- |
| Enable YouTube Focus | Confirm accessibility permission and the live monitor connection, then persist YouTube removal from Kavach’s block list. Other selected apps stay selected and protected. |
| Enable Kavach or start its timer/profile | Preserve YouTube Focus. YouTube stays excluded while Focus manages it; Kavach protects the other selected apps. |
| Tap the managed YouTube row, its toggle, or the picker status card | Explain that both features can coexist and blocking the whole YouTube app would also stop lessons. No selection or mode changes occur just by opening the dialog. |
| Choose **Keep YouTube Focus**, tap outside, or press Back in the dialog | Close the explanation without selecting YouTube, saving the picker, or activating Kavach. During an explicit save, dialog actions wait for its result. |
| Choose **Turn off Focus & select YouTube** | Persist the explicit YouTube selection; if Kavach is already protecting apps, confirm its updated protection before disabling Focus. Failure preserves the previous choice and displays an error. This choice does not automatically enable a disabled Kavach or start an Ekagra timer. YouTube is blocked when Kavach is active. |
| Choose **Select All** while Focus is on | Select the other apps, keep YouTube excluded, and show an explanation of the exclusion. The student can keep Focus or explicitly choose the whole-app override. |
| Turn YouTube Focus off normally | Keep YouTube deselected; it is not silently added back to Kavach. |

The picker now shows a shield/info status card and a **Managed by YouTube Focus** badge on the YouTube row. Its decision dialog uses the installed Composables UI `AlertDialog`, `Button`, `Icon`, and `Text`, with the existing SAFAR theme. English, Hindi, and Hinglish explain the same behavior.

## Fixes implemented — 16 October 2026

All seven findings below have been addressed in the current worktree. The original audit is retained below as a record of the failures before these fixes.

1. **Safe Kavach activation and coexistence:** validates the complete app/permission configuration and preserves YouTube Focus during ordinary Kavach activation, timer starts, and profile changes. A rejected start, startup timeout, or canceled switch also preserves Focus. Only the explicit whole-app override in the picker replaces Focus with a YouTube block; active Kavach protection must confirm that update first. Timer protection still starts with Ekagra.
2. **Safe Focus handoff:** requires accessibility permission and an actual monitor connection before removing YouTube from the saved block list. Lost permission or a newer mode action during persistence rolls the selection back. Activation failures appear in the existing Focus message area.
3. **Picker navigation:** ordinary Back/Save and app edits only save selections. Auto-activation is reserved for a pending initial Kavach enable request. Immediate selection state prevents stale rows from restoring old choices.
4. **Channel revocation:** a missing cached rule is checked against authoritative Room data; deleted or reclassified channels lose floating/fullscreen authorization. Freshly added channels are preserved when the first Room emission is old.
5. **Quick Unlock media:** both notification paths now use package-scoped unlock, strict mode, origin, and expiry rules before pausing media. Notification suppression remains independent.
6. **Truthful status:** accessibility teardown stops the guard. Invalidated queued starts cannot revive a stale protection notification, and guard starts require enabled Focus and a live connection.
7. **Watch monitoring:** confirmed videos now enter the monitoring state and reuse unchanged applied decisions. New videos and rule changes still trigger evaluation; Add-to-productive establishes the same verified state.

Additional integration checks cover last-action ordering, connection delay, permission loss during persistence, edited app lists during initial loading, and service acknowledgements after cancellation. YouTube remains deselected when Focus is switched off; it is not silently restored to Kavach's list.

### Verification of the seven fixes before the coexistence UI follow-up

- `:app:testQaDebugUnitTest --offline --console=plain`: **656 tests passed, zero failures**. This includes **222 focused YouTube Focus/Kavach tests**.
- The original three failing audit scenarios now have passing regression coverage: missing accessibility, empty Always On configuration, and rejected Kavach service startup.
- Service wiring tests exercise the actual confirmed-watch callback, feedback interruption, and queued foreground-service startup. Helper tests cover channel revocation and Quick Unlock policy.
- `git diff --check` passed.
- No emulator or physical device was used. Android/device-specific verification remains with the user.
- Full test log: `/private/tmp/safar-seven-fixes-full-tests.log`.

Guard startup keeps a neutral loading notification while Android acknowledges foreground-service creation; only a validated enabled and connected request shows protection as active. Ordinary accessibility feedback interruption preserves the bound monitor; true disconnection stops support and removes its status.

### Coexistence UI follow-up checks

- Consulted current official Composables UI documentation for Alert Dialog, Buttons, and Icon via the configured MCP server. The component parameters match those APIs and the project already supplies the required library and theme.
- Reviewed row/toggle, Select All, positive/negative dialog actions, outside dismissal, system Back, toolbar Back, busy persistence, and error handling. Closing the explanation does not call the picker save/activation callback. The explicit override waits for authoritative repository state before showing the selected row or dismissing on success.
- English, Hindi, and Hinglish XML parse successfully and contain the same 16 integration string keys.
- `git diff --check` passed. This follow-up was reviewed without an emulator or device; its build/test results must be recorded separately from the earlier seven-fix run above.

### Coexistence verification

- `:app:testQaDebugUnitTest --offline --console=plain`: **681 tests passed, zero failures or skipped tests** across 119 suites. This compiles the updated app and all three translated resource sets.
- The 33 mode-switching tests cover concurrent activation, explicit whole-app selection, sole-YouTube restart, startup failure/timeout rollback, selection edits during persistence, and cancellation by a newer Focus choice. Protection acknowledgements include the actual loaded package set and activation request so stale monitor callbacks cannot stop Focus.
- The 13 picker tests cover normal saving, explicit override delegation, saved selection updates, and search preservation during loading.
- Eight preference tests exercise real reads/writes against in-memory storage: Kavach starting, stopping, or resetting preserves Study-origin breaks and their notifications; Kavach/legacy breaks still clear correctly. Strict Kavach behavior does not remove Study break options.
- No emulator or physical-device feature testing was performed. Test log: `/private/tmp/safar-coexistence-tests.log`.

---

## Original scope and result (before fixes)

Reviewed the current worktree, including automatic mode switching, persisted app selection, foreground blocking, notification/media enforcement, fullscreen identity, picture-in-picture (PiP), and accessibility lifecycle. Production code was not changed during this audit. No emulator or device was used.

Found **seven actionable issues**, including **three high-priority protection gaps in the new switching logic**. The earlier 177 passing tests do not establish that these integration paths are safe.

## Original findings (before the fixes)

### 1. P1 — Failed Kavach activation turns off working YouTube Focus

Location: [FocusShieldRepository.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt:317); related timer activation at [line 204](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt:204).

Reproduce: select only YouTube in Kavach, then enable YouTube Focus. The new rule removes YouTube, leaving an empty Kavach block list. Select the Always On profile. `setKavachProfile()` disables Focus after checking permissions, before validating that any apps remain selected. `startKavachService()` then rejects the empty list. Neither protection manages YouTube afterward.

The timer activation path similarly disables Focus before starting Kavach and does not restore it when the service start fails. Android background-start restrictions are one possible failure source.

**Recommendation:** validate the complete target configuration and retain/restore the previous mode if target activation fails. This does not require automatically reselecting YouTube.

**Evidence:** two temporary regression assertions failed with the expected protection-state assertions (empty Always On list and rejected timer service start).

### 2. P1 — YouTube is removed before confirming Focus can monitor it

Location: [FocusShieldRepository.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt:59) and [line 72](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt:72).

`setYoutubeFocusEnabled(true)` persists YouTube deselection and sets the enabled preference before checking accessibility. The permission check only controls whether to start the support service; a false result does not prevent or roll back deselection. For an existing setup with accessibility permission revoked, the Kavach-screen Focus toggle can therefore remove a working YouTube app block without a working channel monitor.

**Recommendation:** gate the handoff on required accessibility readiness and preserve the existing Kavach selection when Focus cannot start. Handle later disconnection explicitly so the user can see that protection has stopped.

**Evidence:** a temporary regression assertion with accessibility disabled showed YouTube removed from the saved block list.

### 3. P1 — Merely leaving Apps to Block switches Focus off

Location: [FocusShieldRepository.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldRepository.kt:355); caller [FocusShieldViewModel.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldViewModel.kt:195).

Reproduce: keep Kavach enabled, enable YouTube Focus last, open Apps to Block, and press Back without editing anything. `AppPickerScreen.finishAndReturn()` always saves and enables Kavach. `saveAndEnableShieldIfReady()` calls `setEnabled(true)` even when Kavach was already enabled. The new side effect disables Focus. YouTube remains deselected, so this routine navigation removes its channel protection.

**Recommendation:** separate saving an app list from requesting a new Kavach activation; use the pending activation flag when appropriate.

**Evidence:** complete callback and state trace, including the system Back handler and toolbar Back path.

### 4. P2 — Deleting a productive channel can leave its video allowed

Location: [YoutubeStudyV2AccessibilityService.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/youtubestudyv2/YoutubeStudyV2AccessibilityService.kt:174); fallback at [line 1154](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/youtubestudyv2/YoutubeStudyV2AccessibilityService.kt:1154).

Reproduce: allow a channel, start its lesson in PiP, return to SAFAR, and delete it from the productive list. The classification collector retains the previous PRODUCTIVE value when the ID disappears. Floating playback falls back to that value, so the video stays allowed until a later tap or runtime reset. Some fullscreen paths can retain the same stale grant.

**Recommendation:** revoke cached playback authorization when an established channel rule is removed, distinguishing that removal from an initial/loading emission.

**Evidence:** replay of compiled policy classes returned `classification=PRODUCTIVE, blocked=false` after deletion. The hidden fullscreen replay also retained permission.

### 5. P2 — Notification handling pauses media during valid Quick Unlock

Location: [FocusShieldNotificationListenerService.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/FocusShieldNotificationListenerService.kt:27); duplicate sweep at line 58.

Reproduce: with YouTube Focus off, block YouTube using normal Kavach and enable notification access. Take a Kavach Quick Unlock, then press Play. YouTube updates its media notification. The notification listener checks block-list membership and pauses its media, ignoring the active package unlock. Foreground and background enforcement correctly honor that unlock, so the two paths disagree.

**Recommendation:** keep notification cancellation separate from media blocking and apply the shared Quick Unlock policy before pausing playback.

**Evidence:** notification, quick-unlock, and timer lifecycle trace. Timer notification suppression intentionally remains active while the timer is paused.

### 6. P2 — Accessibility disconnection leaves an inaccurate active notification

Location: [YoutubeStudyV2AccessibilityService.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/youtubestudyv2/YoutubeStudyV2AccessibilityService.kt:300).

Turning off the accessibility service unbinds the monitor and removes its callbacks, but does not stop `YoutubeStudyV2GuardService`. The foreground notification continues claiming YouTube Focus protection is active. The guard has no connection-state observer to correct that status.

**Recommendation:** stop/update the support service and notification on accessibility unbind, interruption, and termination as appropriate.

**Evidence:** lifecycle trace from `onUnbind()` through `stopRuntime()` and the guard's notification builder.

### 7. P2 — Confirmed playback never enters the monitoring state

Location: [YoutubeStudyV2AccessibilityService.kt](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/youtubestudyv2/YoutubeStudyV2AccessibilityService.kt:527); the only `acceptStable()` call is at [line 464](/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android_v5/app/src/main/java/com/safarparmar/app/feature/youtubestudyv2/YoutubeStudyV2AccessibilityService.kt:464).

The only service call to `session.acceptStable()` is in the branch for an unconfirmed watch page. Confirmed videos never transition to MONITORING, so `session.isAlreadyEvaluated()` cannot recognize unchanged playback. The same lesson is evaluated again on the one-second watchdog and qualifying content changes, causing unnecessary Room work and analytics updates.

**Recommendation:** wire confirmed stable observations into the session state machine before relying on its unchanged-video fast path.

**Evidence:** policy/session replay remained VIDEO_TAPPED with `skipUnchanged=false`; supplying the missing stable observation produced MONITORING with `skipUnchanged=true`.

## Original validation (before fixes)

- Current production source and tests compiled.
- Existing focused tests: **177 passed**.
- Temporary audit regression tests: **three failed as expected**, exposing findings 1 and 2; their failures were assertion failures, not build or fixture errors.
- PiP/fullscreen and watch-session policy replays independently confirmed findings 4 and 7.
- Findings 3, 5, and 6 were verified by caller/state/lifecycle tracing, without claiming a device reproduction.
- Temporary failing tests were removed from the repository so this audit does not add failing tests to the regular suite. Their source and XML results are retained under `/private/tmp/safar-runtime-audit/`; logs are `/private/tmp/safar-mode-audit-tests.log`.

## Intentional behavior and existing limitation

- Both modes staying enabled while Focus owns YouTube is the current coexistence behavior. The earlier global Kavach-enables/Focus-disables rule is superseded.
- YouTube remaining unchecked after Focus is switched off normally is intentional. The explicit **Turn off Focus & select YouTube** action is the exception that selects it.
- Without optional notification-listener access, Kavach cannot reliably pause package-specific background media. It can still block app interaction with its overlay. The removed global pause fallback avoids interrupting unrelated productive playback; this limitation should be clear in permission guidance.

The original findings above are retained for traceability. Their fixes are summarized at the top of this document; physical-device verification remains separate.
