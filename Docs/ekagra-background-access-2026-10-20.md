# Shared Kavach/Ekagra background-running access

Ekagra now asks for SAFAR’s existing app-wide Kavach battery/background-running access when a study session starts without it. Existing users receive the prompt on their first affected session after updating: the flow checks actual OS permission and does not depend on a fresh-install flag or old Kavach/tutorial onboarding preferences.

## Behavior

- Focus, stopwatch, Pomodoro and recovered running study sessions share the prompt. Viewing Ekagra without starting a study session, or running a break, does not trigger it.
- If Kavach already obtained the Android battery-optimization exemption, Ekagra skips the request unless Android separately reports that the app is background-restricted.
- The prompt reuses Kavach’s settings helper: direct exemption request, Xiaomi battery settings, per-app Android battery settings, exemption list, then app details as supported by the phone. If the exemption already exists, the direct request is skipped so an explicitly restricted app can open the relevant settings instead.
- Returning to SAFAR reads the actual Android exemption/restriction state. Opening settings or getting an Activity result is never treated as a grant. If access remains missing, the explanation stays available, with Unrestricted/No restrictions instructions.
- “Not now” leaves the study session running. The prompt does not repeat on pause/resume of the same session, but a new session asks again if access is still missing or was revoked. Dialog/settings state is retained through recreation.
- If no supported settings activity opens, manual Android Settings → Apps → SAFAR → Battery instructions remain visible.
- The timer keeps running during the request. No change was made to TimerService, process-death accounting, saving, personal analytics or leaderboard/presence rules. Existing recovery remains necessary: a battery exemption reduces interruptions but does not prevent every Android/OEM process termination or a user force-stop.
- Android requires the user’s consent. The app cannot grant this permission silently. No new manifest permission was needed; Kavach already declares the battery-optimization request permission.
- English, Hindi and Hinglish strings are included. V4 tutorials stay enabled; V5 tutorials stay hidden.

## Validation

The source/resource/test changes are identical in V4 and V5. Validation uses `/tmp/safar-habit-checks-20261020` to avoid shared build-output conflicts. No emulator/device, APK build or AAB build is used.

Regression tests cover existing installations, already-granted Kavach access, no prompt while browsing/breaking, missing session identity, pause/resume, denial and retry on the next session, settings return without grant, successful grant, ordinary Activity resume, session end while settings is open, permission revocation, state recreation, the shared exemption/restriction truth table, and the Android 26 versus Android 28+ restriction API.

Final check results and logs are recorded in `Outputs/logs/ekagra-background-access-2026-10-20/`.


Final results in **both V5 and V4 configurations**:

- **790 JVM tests passed**, zero failures/errors/skips, including 15 new background-access regressions.
- Debug/release Kotlin and Java compilation, Hilt compilation, and full instrumentation-source compilation passed.
- Full release lint passed with **zero errors**, 906 warnings and eight hints. No lint rule was disabled and no suppression/baseline was added.
- All **924 app source/resource/test files** match between V4, V5 and the validated copy (excluding ignored local metadata such as Finder .DS_Store files). The two configurations were validated sequentially with their actual build files: V5 keeps tutorials hidden and V4 enables them.
- `git diff --check -- app` passed. Existing APK/AAB hashes are unchanged and the validation tree contains no generated APK/AAB. No device or emulator was used.

The first lint pass caught an API-guard shape it could not verify. The final implementation uses an explicit Android 28+ guard with an annotated API-specific reader; Android 26/27 avoid that call. Both corrected full runs succeeded. Logs retain the first failure as diagnostic history.
