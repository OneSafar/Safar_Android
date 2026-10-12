# Task Assignment: Milestone 2 Worker (Iteration 2)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_it2_orch2`

## Objective
Remediate the defect in `YouTubeGuardService.kt` identified by Reviewer M2:
1. In `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`:
   In `handlePolicyState()`:
   ```kotlin
   is PolicyState.Verifying -> {
       if (state.identity != null) {
           overlayManager.showVerifying(
               channelName = state.identity.channelName,
               title = state.identity.title,
           )
       } else {
           // No active video undergoing verification (e.g. cold start, playback stopped, home/browse screen)
           overlayManager.hide()
       }
   }
   ```
2. Verify that cold launch / playback stopped does not trap the user with a permanent verifying overlay when no video is active.
3. In `YouTubeGuardOverlayManagerTest.kt` or service tests, add a unit test asserting that `PolicyState.Verifying(identity = null)` hides the overlay, while `PolicyState.Verifying(identity = valid)` shows `OverlayState.Verifying`.
4. Run:
   `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`
   Ensure all 355+ tests pass cleanly.

## Mandatory Reading
- Reviewer M2 Report: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_reviewer_m2_1_orch2/handoff.md`
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md`

## Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results.

## Output Requirements
Write report to `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_it2_orch2/handoff.md`.

## 2026-09-11T18:31:38Z
You are Milestone 2 Worker (Iteration 2). Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_it2_orch2.
Read instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_it2_orch2/DISPATCH.md and Reviewer report in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_reviewer_m2_1_orch2/handoff.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results.

Remediate the defect in YouTubeGuardService.kt:
Gate showVerifying on `state.identity != null`. When `state.identity == null`, call `overlayManager.hide()`.
Add a test verifying this behavior.
Verify with `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`.
Write report to /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_it2_orch2/handoff.md and send a message when done.

