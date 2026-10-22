# Habit save release-shrinking regression, 22 October 2026

The actual pre-fix compiled `HabitBatchRequest` and `HabitBatchResponse` were copied from V5 prodRelease class output before the fix. The fixed compiled classes were copied from V5 prodDebug class output after the fix and passing unit checks. Both pairs were processed by the identical isolated R8 release/classfile harness. No app source or configuration was changed by the harness.

- Before: serialized root key is `a`, not `mutations`. Gson cannot instantiate the shrunk acknowledgement DTO and throws `JsonIOException`.
- After: serialized root key is `mutations`; response decodes version `3` and acknowledgement `test-mutation`; contract passes.

Run `python3 run_probe.py` to repeat the negative/positive regression using retained input jars and locally installed dependencies recorded in `paths.json`. Exit 0 means the old input failed as expected and the fixed input passed.

`rules.pro` contains the current app keep rules and two harness additions: keeping the JVM main and applying Android `@Keep` class semantics. Isolated compile-time Kotlin/Compose metadata warnings are ignored. Gson and Kotlin are external library jars, matching their reflection behavior. No network or production data is used.

The existing SDK R8 used here is 9.3.16; the supplied production v80 artifact was built with R8 9.1.31. This harness reproduces the release defect and validates the fixed compiled DTOs, but does not replace an end-to-end release APK test. Independently, v78, v79 and v80 release mappings all rename the request `mutations` field to `a`, while omitting `HabitBatchResponse`; those preserved local release artifacts directly corroborate the root cause.
