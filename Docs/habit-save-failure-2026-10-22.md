# Habit save failure — 22 October 2026

Found a deterministic release serialization defect that explains the reported failure across phones and accounts. Fixed in the current V5 source. No APK was rebuilt, published or installed for this investigation; existing installations still need an updated build.

Follow-up on 22 October: the user requested version 81. Signed App Bundle `Outputs/Safar-prod-release-v1.6.81-81.aab` (1.6.81, versionCode 81) is now built. Full release lint, bundletool validation and signature verification passed; the signing certificate matches version 80. Inspection of the actual packaged DEX confirms both batch models, their constructors and required fields are retained. The R8 mapping is saved alongside the bundle. See `Outputs/logs/release-1.6.81/verification.json` for checks and checksum. No store upload or device test was performed.

## Cause and evidence

`HabitApi.kt` did not protect `HabitBatchRequest` or `HabitBatchResponse` from R8. Gson accesses these models through reflection. Other habit response models already used `@Keep`.

The preserved release mappings show:

- V77: `HabitBatchRequest -> aw2`, `java.util.List mutations -> a` in `Outputs/Safar-v5-1.6.77-mapping.txt`, line 646553.
- V80: `HabitBatchRequest -> iy2`, `java.util.List mutations -> a` in `Outputs/Safar-prod-release-v1.6.80-80-mapping.txt.gz`.
- `HabitBatchResponse` is absent from the V80 mapping; the corresponding existing build's `usage.txt` lists the removed model and its fields.

Consequently, the release request envelope uses `{"a":[...]}` instead of `{"mutations":[...]}`. The backend strictly requires `mutations` and rejects `a` with HTTP 400 `invalid_habit_mutations` before writing to the database (`SAFAR/server/routes/habits.ts`, lines 46–48). This affects the shared batch API used for creates and other habit mutations.

The form catches save exceptions and shows the reported generic “Unable to save habit. Please try again.” (`HabitFormBottomSheet.kt`). The error message does not distinguish invalid requests from connectivity or initial loading failures.

An isolated R8 probe using the actual pre-fix compiled app DTOs reproduced the malformed request and an additional failure parsing the acknowledgement response: Gson could not instantiate the optimized response model. The identical probe passed using the fixed compiled DTOs.

## Source fix

- Added `@Keep` to both batch models to retain the types, fields and constructors.
- Declared explicit Gson wire names for `mutations`, `version` and `acknowledged`.
- Added a regression test that changes Gson's fallback field naming and checks both production models. Strengthened the HTTP contract test to assert the exact top-level request envelope and response version.

No backend validation was relaxed, no database schema changed, and no broad package keep rule was added. The fix is limited to V5, the current project; older workspace copies were not modified.

This behavior is also documented in the [official Gson troubleshooting guide](https://google.github.io/gson/Troubleshooting.html#android-app-not-working-in-release-mode-random-property-names).

## Verification

- `:app:testProdDebugUnitTest --tests 'com.safarparmar.app.feature.habits.*'`: **81 passed**, zero failures, errors or skips, across 12 suites.
- Backend `vitest run server/routes/__tests__/habits.test.ts server/services/__tests__/habit-sync.test.ts`: **12 passed** using an isolated in-memory database.
- Direct backend schema check: a valid create under `mutations` passes; the same create under `a` fails with missing `mutations` and unrecognized key `a`.
- Actual R8 before/after probe: old request key is `a` and response parsing throws; fixed request key is `mutations`, response version and acknowledgements parse correctly, `contract=PASS`.
- `git diff --check`: passed for the changes.

The probe uses the installed SDK R8 9.3.16 in JVM/classfile mode with the project's keep rules and the standard AndroidX `@Keep` rule. The preserved app release mappings were generated with R8 9.1.31. This verifies the focused serialization behavior under real optimization; it is not a rebuilt full-app release or an on-device test. The probe mutation IDs are fixtures, not a production request.

Logs, small probe input/output JARs, mappings, source and rules are retained under `Outputs/logs/habit-save-2026-10-22/`. `old-probe.log` and `fixed-probe.log` show the before/after result. The JARs are diagnostic JVM fixtures, not installable APKs.

## Production scope and remaining verification

Read-only public checks returned HTTP 200 from `/health/ready` and the expected HTTP 401 from unauthenticated `/api/habits/sync`. These confirm service readiness and route presence, not authenticated habit persistence or a deployed source revision.

An older standalone MongoDB transaction incompatibility was fixed in backend source on 14 October (`24e73f0`); old documents requiring a replica set are stale. No claim is made here that the currently deployed revision was inspected.

The student's exact APK version and exception trace were not supplied. The inspected release artifacts contain a sufficient, reproducible cause for the complaint. A rebuilt release should receive an authenticated save/read-back smoke test before distribution. No production user records, emails, app-store releases or deployments were changed.
