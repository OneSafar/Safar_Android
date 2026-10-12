# Task Assignment: Milestone 1 Worker (Iteration 3)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it3_orch2`

## Objective
Fix the display name normalization gap in `YouTubeChannelRepositoryImpl.kt`:
In `YouTubeChannelRepositoryImpl.kt`:
Lines 28-34 (`getPolicy`), lines 61-93 (`savePolicy`), and lines 243-264 (`deleteChannel`):
Currently uses `identity.displayName.trim()`.
Replace with `ChannelIdentity.normalizeDisplayName(identity.displayName)`:
1. When saving `displayName`, store `ChannelIdentity.normalizeDisplayName(identity.displayName)` in `YouTubeChannelEntity.displayName`.
2. When querying `youTubeChannelDao.getChannelByName(...)`, pass `ChannelIdentity.normalizeDisplayName(name)`.
3. In `savePolicy`, `getPolicy`, and `deleteChannel`, consistently use `ChannelIdentity.normalizeDisplayName(...)`.
4. Also check `getAllChannels()` and fallback queries to ensure uncollapsed whitespace in display names does not cause query mismatch in SQLite.

## Test Verification
Run:
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`
Ensure `Milestone1Iteration2EmpiricalChallengeTest` and all 316 unit tests pass 100% (316/316 passed, 0 failures).

## Mandatory Reading
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md`
- Reviewer It2 Report: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_reviewer_m1_it2_1_orch2/handoff.md`

## Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task.

## Output Requirements
Write report to `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it3_orch2/handoff.md`.

## 2026-09-11T18:05:02Z
You are Milestone 1 Worker (Iteration 3). Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it3_orch2.
Read your instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it3_orch2/DISPATCH.md and the specification in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md.
Also read Reviewer It2 report at /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_reviewer_m1_it2_1_orch2/handoff.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work.

Fix display name normalization in YouTubeChannelRepositoryImpl.kt:
- Use ChannelIdentity.normalizeDisplayName(identity.displayName) in getPolicy, savePolicy, and deleteChannel.
- Store normalized display name in YouTubeChannelEntity.displayName.
- Run `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest` and ensure 100% pass rate (316/316).

Write report to /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it3_orch2/handoff.md and send a message when done.
