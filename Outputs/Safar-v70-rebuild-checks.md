# Version 70 rebuild checks

Signed production bundle: `Safar-prod-release-v1.6.70-70.aab`
Package: `com.safarparmar.app`; version code **70**; version name **1.6.70**.
Size: 21,902,006 bytes.
SHA-256: `6457696f5cbd404862ecd9ef5f28d5f33463d47a9312cbc5644a4e3c7b7cac1a`.

## Reminder test correction

The English timetable changed from 15:00 to 15:30 IST, but two personal-reminder tests still expected the lecture to be available at 15:00. The backend correctly withheld the watch reminder before release. The tests now use the current release time. Added explicit checks that a watch reminder before release is withheld, and a student's later reminder is sent independently of the live time. The identical-live appointment and revision tests pass. No production reminder behavior was changed for these failures.

All 63 scoped backend tests passed across six test files. Committed and pushed as `b2fe338` on `origin/main`.

## Android build

`./gradlew :app:bundleProdRelease` succeeded. Release-fatal lint passed. Bundletool validation and JAR signature verification passed. Manifest version and copied-file checksum were verified.

The prior full-lint findings and unrelated whole-app unit-test problems were not resolved or rechecked in this rebuild. Device notification delivery and manual UI checks were not performed. Backend code is deployed separately and is not contained in the AAB.
