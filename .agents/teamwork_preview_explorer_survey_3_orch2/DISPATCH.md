# Task Assignment: Survey Explorer 3 (orchestrator_2)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_3_orch2`

## Scope
Pillar 5 & Test Suite Architecture (Phases 9, 10, plus Build & Test verification):
- Lifecycle & Robustness: Detect YouTube leaving foreground via global window state changes (`TYPE_WINDOW_STATE_CHANGED`, window disappearance); immediately dismiss overlay and cancel active detection.
- Service Lifecycle: `YouTubeNotificationListener` recreatable coroutine scope across disconnect/reconnect, proper cleanup of `MediaController.Callback`s to prevent memory leaks.
- Build & Test Suite Survey:
  - Check existing unit tests and E2E tests (`YouTubeGuardE2ETest.kt`).
  - Verify build/test command: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`.
  - Identify missing test cases required for all 15 phases.

## Mandatory Reading
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md` (read the entire file, especially latest request `## 2026-09-11T15:25:10Z`)
- Current service implementations: `YouTubeGuardService`, `YouTubeNotificationListener`, `YouTubeBlockOverlay`, `ComposeServiceLifecycleOwner`, and `AndroidManifest.xml`.
- Existing test files in `app/src/test/java/com/safarparmar/app/feature/youtube/`.

## Output Requirements
Produce `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_3_orch2/handoff.md` with:
- Current service lifecycle, window monitoring, and overlay lifecycle implementation analysis.
- Detailed gap analysis against Phases 9 and 10.
- Current test suite status, build verification command, and recommended test additions.

## 2026-09-11T15:27:46Z
You are Survey Explorer 3. Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_3_orch2.
Read your instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_3_orch2/DISPATCH.md and read the mandatory specification in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md (especially the 15-phase fixes plan under ## 2026-09-11T15:25:10Z).

Your focus is Pillar 5 & Test Suite Architecture (Phases 9, 10 + Build/Test Suite Verification):
- Lifecycle & Robustness (Foreground detection, overlay dismissal, NotificationListener reconnection, callback leak cleanup)
- Survey of existing unit and E2E tests, compilation command, and test coverage gap analysis for all 15 phases.

Explore app/src/main/java/com/safarparmar/app/feature/youtube/ and app/src/test/java/com/safarparmar/app/feature/youtube/.
Write your comprehensive analysis and handoff report to /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_3_orch2/handoff.md. When done, send a message to the caller with your summary and report path.
