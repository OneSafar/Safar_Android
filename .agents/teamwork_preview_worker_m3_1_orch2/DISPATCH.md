# Task Assignment: Milestone 3 Worker (orchestrator_2)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m3_1_orch2`

## Scope: Milestone 3 (Phases 3, 7, 15)
1. **Structural Subtree Parsing (Phase 3)**:
   - Create `app/src/main/java/com/safarparmar/app/feature/youtube/detector/YouTubeUiParser.kt`:
     - Determine `ScreenType`: `WATCH`, `SHORTS`, `CHANNEL_PAGE`, `FEED`, `SEARCH`, `UNKNOWN`.
     - Implement screen-aware subtree extraction:
       - `extractWatchSubtree`: identifies player container and watch metadata layout. Extracts title, channel name, and verified `@handle` (only real `@handle`, never synthesized).
       - Exclude suggestions container (`related_videos`, `compact_video_renderer`, `watch_list`) and comments container (`comments_entry_point_layout`, `comment_thread`) so suggestion titles and comment `@mentions` never pollute active channel identity.
       - `extractShortsSubtree`: locates reel container / reel player controls.
       - `extractChannelHeaderSubtree`: parses channel profile header (channel title and verified handle) for channel pages.
     - Deprecate or replace `YouTubeSnapshotParser.kt` and remove any legacy synthetic fake handle generation (e.g. `@${channelName}`).

2. **Structural Shorts Detection & Click Interception (Phase 7)**:
   - In `app/src/main/res/xml/youtube_guard_service.xml`:
     - Add `typeViewClicked` to `android:accessibilityEventTypes`.
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`:
     - Add `AccessibilityEvent.TYPE_VIEW_CLICKED` to `serviceInfo.eventTypes`.
     - In `onAccessibilityEvent`:
       - Handle `AccessibilityEvent.TYPE_VIEW_CLICKED`:
         - If clicked node represents bottom navigation Shorts tab (e.g., view id containing `tab_shorts` or contentDescription "Shorts") or a Shorts shelf item:
           - Immediately trigger `overlayManager.showShortsBlocked()` and notify `policyEngine.onShortsDetected()` to prevent Shorts audio/video leak before the screen inflates.

3. **Suggested Video Click Interception & Channel Pages (Phase 15)**:
   - In `YouTubeGuardService.kt`:
     - When `TYPE_VIEW_CLICKED` occurs on a suggested video card (`compact_video_renderer`, `watch_next` item), immediately transition to `OverlayState.Verifying` so touch events are consumed while the next video loads.
   - In `YouTubeUiParser.kt`:
     - When on a channel profile page (`ScreenType.CHANNEL_PAGE`), extract `ChannelHeaderSubtree` and produce `CanonicalYouTubeContent` marked `isChannelPage = true`.
     - When `isChannelPage = true`, `YouTubePolicyEngine` resolves policy for the creator and blocks the channel page if `DISTRACTING`.

4. **Unit Tests & Verification**:
   - Create `app/src/test/java/com/safarparmar/app/feature/youtube/detector/YouTubeUiParserTest.kt`:
     - Test that suggested video titles and comment authors in the accessibility tree do NOT pollute active channel identity.
     - Test Shorts reel subtree detection.
     - Test Channel profile page header extraction.
   - Update `YouTubeGuardServiceTest.kt` to verify `TYPE_VIEW_CLICKED` on Shorts navigation immediately blocks Shorts.
   - Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`
   - Ensure all tests pass 100%.

## Mandatory Reading
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md` (specifically R3 & R4 under `## 2026-09-11T15:25:10Z`)
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/orchestrator_2/PROJECT.md`
- Survey Explorer 2 Report: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2/handoff.md`

## Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work.

## Write Ownership
- `app/src/main/java/com/safarparmar/app/feature/youtube/detector/`
- `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`
- `app/src/main/res/xml/youtube_guard_service.xml`
- `app/src/test/java/com/safarparmar/app/feature/youtube/detector/`

## Output Requirements
Write report to `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m3_1_orch2/handoff.md`.

## 2026-09-11T18:42:48Z
You are Milestone 3 Worker. Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m3_1_orch2.
Read your instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m3_1_orch2/DISPATCH.md and the specification in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md.
Also read Survey Explorer 2 report in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2/handoff.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Implement Milestone 3 (Phases 3, 7, 15):
1. Create YouTubeUiParser.kt with structural subtree parsing:
   - ScreenType: WATCH, SHORTS, CHANNEL_PAGE, FEED, SEARCH, UNKNOWN.
   - Extract WatchMetadataSubtree, PlayerSubtree, ShortsPlayerSubtree, ChannelHeaderSubtree.
   - Exclude suggested videos container and comment threads from active watch identity.
   - Remove legacy synthetic fake handle generation from parser.
2. In youtube_guard_service.xml and YouTubeGuardService.kt:
   - Subscribe to TYPE_VIEW_CLICKED.
   - Intercept clicks on bottom nav Shorts tab or Shorts shelf cards: immediately trigger showShortsBlocked() and onShortsDetected().
   - Intercept clicks on suggested video cards: immediately showVerifying().
   - Channel profile pages: extract ChannelHeaderSubtree and apply channel blocking if DISTRACTING.
3. Unit tests:
   - YouTubeUiParserTest.kt testing subtree isolation (suggestions/comments do not pollute watch identity), Shorts reel detection, and channel header extraction.
   - YouTubeGuardServiceTest.kt verifying TYPE_VIEW_CLICKED interception.
4. Run `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest` and ensure all tests pass 100%.

Write your report to /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m3_1_orch2/handoff.md and send a message when done.
