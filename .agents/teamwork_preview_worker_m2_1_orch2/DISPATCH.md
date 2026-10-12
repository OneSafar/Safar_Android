# Task Assignment: Milestone 2 Worker (orchestrator_2)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_1_orch2`

## Scope: Milestone 2 (Phases 1, 6, 11, 12, 13)
1. **OverlayState & Verifying Screen (Phase 1 & 13)**:
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/OverlayState.kt`:
     - Add `data class Verifying(val channelName: String = "", val title: String = "") : OverlayState`
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlayManager.kt`:
     - Add `showVerifying(channelName: String = "", title: String = "")` which sets `_overlayState.value = OverlayState.Verifying(channelName, title)` and ensures the overlay view is attached/visible.
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlay.kt`:
     - Render `VerifyingContent(state: OverlayState.Verifying)` with dark scrim, progress indicator, and verification status text.
     - Add touch-consumption pointer input modifier on the root overlay container:
       `Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } }`
       so that unverified/blocked content CANNOT receive user touch events.
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`:
     - In `observePolicyState()`:
       `is PolicyState.Verifying -> overlayManager.showVerifying(state.identity?.channelName.orEmpty(), state.identity?.title.orEmpty())`
       (Remove the old `overlayManager.hide()` call!).

2. **Policy Engine Fail-Closed Transitions & Generation Tokens (Phases 6, 11, 12, 13)**:
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`:
     - **Generation Tokens (Phase 12)**:
       - Add `private val currentGeneration = AtomicLong(0L)`
       - Maintain `private var activeLookupJob: Job? = null`
       - On any new video or fingerprint change: cancel `activeLookupJob`, increment `val gen = currentGeneration.incrementAndGet()`, and immediately emit `_state.value = PolicyState.Verifying(identity)`.
       - When async DB lookup completes: check `if (gen != currentGeneration.get()) return@launch` (discard stale result).
     - **Autoplay & Video Transitions (Phase 6)**:
       - Always transition to `PolicyState.Verifying` whenever a new video fingerprint is detected.
     - **Ad Policy Retention & Video Switch Security (Phase 11)**:
       - If `identity.isAd == true`:
         - If ad is during the *same* verified video session (same fingerprint or same active channel), retain the last verified policy (`Allowed` or `Blocked`).
         - If ad occurs on a *new* video switch (different fingerprint or cold start without active session), STAY in `PolicyState.Verifying` until the real video content resolves.
         - NEVER query or adopt advertiser channel name / handle into `YouTubeChannelRepository`.
     - **Integration with Models**:
       - Update `YouTubePolicyEngine` to work seamlessly with `ChannelIdentity` and `VideoFingerprint`.

3. **Unit Tests & Verification**:
   - Update and add comprehensive tests in `YouTubePolicyEngineTest.kt`:
     - Stale generation token test: delayed DB lookup must not overwrite newer video policy.
     - Pre-roll ad on video switch stays in `Verifying`; mid-roll ad on same video retains policy.
     - `Verifying` state emitted immediately on every video change.
     - OverlayManager shows verifying overlay during verification.
   - Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`
   - Ensure all tests pass 100%.

## Mandatory Reading
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md`
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/orchestrator_2/PROJECT.md`
- Survey Explorer 1 Report: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_1_orch2/handoff.md`

## Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work.

## Write Ownership
- `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/`
- `app/src/main/java/com/safarparmar/app/feature/youtube/policy/`
- `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`
- `app/src/test/java/com/safarparmar/app/feature/youtube/policy/`

## Output Requirements
Write report to `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_1_orch2/handoff.md`.

## 2026-09-11T18:17:36Z
You are Milestone 2 Worker. Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_1_orch2.
Read your instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_1_orch2/DISPATCH.md and the specification in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md.
Also read the project architecture in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/orchestrator_2/PROJECT.md and survey report in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_1_orch2/handoff.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Implement Milestone 2 (Phases 1, 6, 11, 12, 13):
1. Overlay: Add OverlayState.Verifying, YouTubeGuardOverlayManager.showVerifying, touch-consuming pointerInput blocker on root overlay, and route PolicyState.Verifying -> showVerifying in YouTubeGuardService.
2. State Machine & Policy Engine: In YouTubePolicyEngine, implement generation tokens (AtomicLong) and cancel active lookup jobs on new video; immediate fail-closed transition to Verifying on video fingerprint changes; ad policy retention for same video vs stay in Verifying on new video switch.
3. Integration with ChannelIdentity & VideoFingerprint models.
4. Unit Tests: Update YouTubePolicyEngineTest to test generation tokens, pre-roll ad vs mid-roll ad, fail-closed verifying state flow.
5. Verification: Run `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest` and ensure 100% tests pass.

Write your report to /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m2_1_orch2/handoff.md and send a message when done.

