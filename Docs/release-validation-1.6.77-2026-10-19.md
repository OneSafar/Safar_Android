# V5 1.6.77 release bundle — 19 October 2026

Superseded by the [20 October validation fixes](validation-fixes-2026-10-20.md). The original artifacts described below are archived under `Outputs/archive/2026-10-19/`; the canonical output path now points to the newer fixed build. The counts and checksum below remain the historical 19 October results.

The production bundle built and signed successfully. Required release lint, signature verification, bundle validation and the release launch smoke check pass. **Overall validation is not clean:** full lint and the broader UI suite have outstanding findings listed below.

## Changes for this build

- Version name `1.6.77`, version code `77`.
- Release builds force `TITLI_TUTORIALS_ENABLED=false`, even when `-PtitliTutorialsEnabled=true` is supplied for preview work. Default debug builds also keep tutorials off.
- Existing shared gates hide every Titli toolbar/drawer icon, tutorial invitation, replay action, query route and practice entry. The visibility regression now also covers Habits, Toppers Batch, Mehfil and Dhyan entries, alongside Ekagra, Goals and YouTube help.
- Removed two unused Hinglish translations (`support_asked_before`, `support_first_time`) that had no default resource or code reference. These caused the two fatal `ExtraTranslation` lint findings. No lint baseline or severity suppression was added.

The bundle was built from the current `Safar_Android_v5` working tree on `feature/v5-safar-updates`, based on commit `fa7d07dd`. It includes the existing pending workspace changes as well as the changes above; it is not a clean-checkout build of that commit. No new commit, push, store upload or production-data change was performed for this request.

## Artifact verification

| Check | Result |
| --- | --- |
| Bundle | `Outputs/Safar-v5-1.6.77-prod-release.aab` |
| Size | 24,846,238 bytes (23.70 MiB) |
| SHA-256 | `00f1a71a312a3f3cd9beed6ccddbe956862234818e13b99c71df032f794c7705` |
| Package / version | `com.safarparmar.app` / `1.6.77` (77) |
| Signing | JAR signature verified; certificate matches the configured release keystore |
| Certificate SHA-256 | `7F:C5:E6:0E:58:55:CE:8D:D7:A2:75:4C:F0:62:01:1B:AC:D4:7C:F1:48:DE:AE:2D:F8:DB:11:45:C7:1B:B6:2B` |
| Bundle structure | `bundletool 1.18.3 validate` passed |
| Release settings | Tutorials disabled, application not debuggable, debug recovery probe absent |
| Ekagra service manifest | `specialUse`, `stopWithTask=false` |
| Generated universal APK | Signature verification and 16 KiB ZIP alignment passed; all ELF64 native load segments support at least 16 KiB alignment |
| Release launch | Installed the APK generated from this bundle; cold-launched successfully to the signed-out login screen on Android 17; process remained alive with no fatal crash recorded |
| Source consistency | Source inventory remained unchanged during bundle creation and artifact validation |

R8 mapping is retained at `Outputs/Safar-v5-1.6.77-mapping.txt`. Crashlytics mapping upload was explicitly excluded from this local build; retain/upload the matching mapping when releasing. The Play Console foreground-service declaration must cover the Ekagra `specialUse` subtype introduced by the timer recovery fix.

## Checks and outstanding findings

- **737 JVM tests passed**, no failures, errors or skips.
- **Tutorial release visibility regression passed**, including explicit replay callbacks and the newer feature practice entries. A separate release configuration generation with `-PtitliTutorialsEnabled=true` still emitted `TITLI_TUTORIALS_ENABLED=false`.
- **All 34 Ekagra service, journal and alarm instrumented tests passed** in this run.
- **Required `lintVitalProdRelease`, signing validation and `bundleProdRelease` passed.** Release shrinking and R8 compilation completed successfully.
- Full instrumentation ran **148 tests: 124 passed, 19 failed, five skipped**. The skips are tests that require tutorials to be enabled. Outstanding failures:
  - 16 parameterized `TodayPlanLayoutTest` cases expect the text “Planned lectures for today”, which the current dashboard no longer renders. The underlying layout test expectations need review.
  - Two `SafarPopupEnvironmentInstrumentedTest` cases failed their bridge/state assertions.
  - One `KavachProgressiveUiTest` case failed to reach its expected screen state on the emulator.
  These failures remain unresolved; they must not be represented as passing product validation.
- Unmodified full instrumentation compilation is blocked by the existing `HabitMigrationInstrumentedTest` and `HabitReminderInstrumentedTest`, which reference unavailable migration/owner/reminder APIs. A temporary init script excluded only those two source files for the run above. The manual multi-process `EkagraProcessDeathInstrumentedTest` was excluded from automatic execution as its source requires the dedicated kill/recovery harness; that harness was not repeated for this tutorial visibility change.
- **Full `lintProdRelease` still fails with 83 errors**, 904 warnings and eight hints. There are **zero fatal findings** after removal of the obsolete translations. Error categories: 35 `NonObservableLocale`, 37 `LocalContextGetResourceValueCall`, nine `MissingTranslation`, one `ContextCastToActivity`, and one `SuspiciousIndentation`. These predate this request's release gate/resource cleanup and remain outstanding.
- `git diff --check` passed.

The emulator was offline during tests and release smoke verification. No real credentials were entered. The release app was stopped after the smoke check, and emulator Wi-Fi/mobile data were restored.

## Reproduction and evidence

Logs, the source inventory, XML test results, full lint HTML/XML and machine-readable artifact verification are retained under `Outputs/logs/release-1.6.77-2026-10-19/`.

Primary commands (using Android Studio's bundled JBR):

```sh
./gradlew :app:testProdDebugUnitTest :app:connectedProdDebugAndroidTest \
  --continue -I /tmp/safar-release-validation-20261019/instrumentation.gradle \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.safarparmar.app.ui.ekagra.EkagraProcessDeathInstrumentedTest \
  -x :app:copyApksToOutputs --console=plain
./gradlew :app:lintProdRelease --console=plain
./gradlew :app:lintVitalProdRelease :app:validateSigningProdRelease --console=plain
./gradlew :app:bundleProdRelease \
  -x :app:copyApksToOutputs -x :app:uploadCrashlyticsMappingFileProdRelease --console=plain
```

The temporary instrumentation init script applies `exclude('**/HabitMigrationInstrumentedTest.kt', '**/HabitReminderInstrumentedTest.kt')` to both the Android test Java and Kotlin source sets. Repository test source-set configuration was not changed. The existing APK/AAB copy finalizer was excluded, and the final AAB/mapping were copied explicitly to their versioned output names.
