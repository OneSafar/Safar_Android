# V5 validation fixes — 20 October 2026

Follow-up: the Habit reminder blocker described below is now implemented in source. **774 JVM tests pass**, and the **complete instrumentation source set compiles without exclusions**. See [Habit reminder implementation](habit-reminders-2026-10-20.md) for the implementation and current checks. The APK/AAB files listed in this earlier report have not been rebuilt for that follow-up and do not contain the new reminder implementation.

All **83 reported lint errors are resolved**, with full production release lint passing at zero errors/fatal findings. **741 JVM tests pass**, including four popup state regressions. Production debug/release compilation, compilation of the repaired UI tests and signed release APK/AAB builds succeeded.

The source causes of the **19 reported UI failures have been addressed**. **Those UI tests have not been rerun**, and the APK has not been installed or launched: the user requested that emulator/device verification be left to them. A passing compilation/JVM result is not a claim that all 19 UI cases now pass on a device.

## What the 19 UI failures meant

| Cases | Cause | Fix and preserved behavior |
| --- | --- | --- |
| 16 dashboard layout cases | One outdated assertion was repeated across screen sizes, font scales and themes. It expected the personal-plan heading even when the dashboard was in Official Schedule mode. | The test now selects the heading for the active mode. Its assertions for readable text, layout overflow, mode controls, subject actions and personal/official planning controls remain in place. Dashboard behavior was not rewritten. |
| One popup bridge case | The test treated `LocalView` as the `ComposeView` attached to the root. Compose actually exposes its inner `AndroidComposeView`. | The assertion now checks the actual container hierarchy. The test still requires an attached bridge, unchanged Row spacing, exactly one added root child, and removal on dismissal. |
| One popup draft case | Replacing a scaled popup view could dispose native dialog slots before their draft providers were saved. Additionally, Compose can recreate a native dialog's composition when exact density changes even within the same rounded Android dpi bucket. | Outer popup state is captured before disposal. Each native content slot has its own saved-state provider retained outside the window being replaced. This preserves edited fields across both kinds of replacement. Native Back/dismiss behavior and isolated popup resource scaling are retained. |
| One Kavach enable case | The fixture claimed Usage Access and overlay permission were granted, while the emulator's actual grants were missing. On resume, production code correctly refreshed the real OS permissions and displayed the permission sheet. | The test now grants the two required app operations for its duration, verifies them and restores their prior modes in `finally`. Production permission checks stay intact. Battery exemption remains optional. This setup has been compiled but not executed in this run. |

The four new JVM popup regressions verify outer/slot draft capture before provider removal, Activity saved-state capture, protection from an old window disposal overwriting newer state, and preservation through independent window-composition replacements at densities 1.6, 1.6002 and 2.4. The last uses Compose runtime with an applier that rejects UI nodes; it creates no Android window and uses no emulator.

## What the 83 lint errors meant

| Errors | Finding | Correction |
| --- | --- | --- |
| 35 | `NonObservableLocale`: reading the process default locale could leave date/day/month formatting in the old language after an in-app language change. | Formatting now reads Compose's observable `LocalLocale.current.platformLocale`. Locale reads needed by parsing/formatting lambdas are captured in composition first. Date ranges, duration calculations and time zones are retained. |
| 37 | `LocalContextGetResourceValueCall`: configuration changes did not reliably refresh strings read through `LocalContext`. | String lookups use observable `LocalResources`. Long-lived callbacks/effects read an updated resource reference through `rememberUpdatedState`; changing language does not restart saves, payment handling or permission flows. |
| 9 | `MissingTranslation`: nine banner, YouTube setup and detection-report strings lacked Hindi translations. | Added the missing Hindi resources, retaining the same resource identifiers and product actions. |
| 1 | `ContextCastToActivity`: directly casting a context can fail when Android or the app supplies a context wrapper. | Navigation safely unwraps `ContextWrapper` to the actual `MainActivity`, preserving notification and Ekagra/PiP handling. No Activity library upgrade was needed. |
| 1 | `SuspiciousIndentation`: a localized-string declaration in a Toppers Batch date row had misleading indentation. | Corrected indentation; no control flow changed. |

No lint baseline, error suppression or disabled lint rule was added. The remaining full lint report has **904 warnings and eight hints**; these are outside the requested 83 errors and have not been represented as resolved.

## Validation completed without a device

- `testProdDebugUnitTest`: **741 passed**, zero failures, errors or skips.
- `lintProdRelease`: **passed**, zero errors or fatal findings.
- `compileProdDebugAndroidTestKotlin`: repaired UI test sources compile using the temporary exclusion described below; UI tests were not executed.
- `assembleProdRelease` and `bundleProdRelease`: **passed**, including R8 shrinking and required release lint.
- AAB validated with bundletool; JAR signature verified against the configured release key.
- An APK generated from this AAB passed signature verification and 16 KiB ZIP/native ELF64 alignment checks. It was not installed or launched.
- Manifest/configuration checks confirm version `1.6.77` (77), tutorials disabled, no debuggable application or debug recovery probe, and the Ekagra foreground service declaration retained.
- Source inventory was unchanged throughout the release build and static artifact verification. `git diff --check` passed.

## Separate pre-existing test-build gap

The old Habit migration test referred to a removed 4→5 migration and DAO method. It now tests the actual production 4→6 migration and verifies preservation of the habit, completions, schedule revisions and unassigned ownership metadata. No production database schema or migration was changed.

At the time of the APK/AAB builds in this report, `HabitReminderInstrumentedTest` could not compile because the tracked scheduler and receiver files were empty. That earlier UI compilation used a temporary exclusion for this one file; it was not deleted or reported as passing. **The follow-up now closes this blocker:** real server-backed reminders are implemented and the normal, unfiltered instrumentation source set compiles. Its device tests remain unexecuted at the user’s request. See [Habit reminder implementation](habit-reminders-2026-10-20.md).

## Builds for the user's verification

- **Installable APK:** `Outputs/Safar-v5-1.6.77-validation-fixed.apk`.
- **Signed App Bundle:** `Outputs/Safar-v5-1.6.77-validation-fixed.aab`.
- The canonical `Outputs/Safar-v5-1.6.77-prod-release.aab` now contains the same fixed bundle, with matching R8 mapping at `Outputs/Safar-v5-1.6.77-mapping.txt`.
- AAB SHA-256: `dc9639f626bfe64c13e48f227fb0eb282751855de7f04c9b5420bd8c62440db0`.
- The previous 19 October artifacts are preserved under `Outputs/archive/2026-10-19/`.

Version remains 1.6.77 (77), since this is a replacement local build. Nothing was uploaded to Play or Crashlytics, and no production user records were changed. Existing unrelated workspace changes remain in the build, as in the previous bundle. No commit or push was performed for this request.

Suggested device checks:

1. Switch Personal/Official dashboard modes; check empty, populated and completed states at small width and larger fonts. Verify lecture navigation, add-to-today, complete and undo.
2. Edit a field in a dialog; change app/font/display scaling and check that the draft remains. Verify close/Back and that the underlying screen's scale is unchanged.
3. Enable Kavach with Usage Access, overlay permission and selected blocked apps while battery optimization remains enabled. Revoke a required permission and confirm the setup prompt still appears. Verify turning Kavach off.
4. Switch English, Hindi and Hinglish and check history dates, calendar headings and feedback messages. Check existing goal saves and history remain intact.
5. Check Ekagra pause/resume, manual breaks and background recovery; the timer-accounting implementation was not modified by these fixes.

Detailed logs, full lint HTML/XML, JVM results, source hashes and static artifact verification are in `Outputs/logs/validation-fixes-2026-10-20/`. `verified-checks.log` is the successful final compile/JVM/lint invocation; earlier logs retain intermediate failures and the superseded invocation.
