# BRIEFING — 2026-09-11T15:33:00Z

## Mission
Investigate Pillars 3 & 4 (Phases 3, 4, 7, 8, 11, 12, 15) of YouTube enforcement architecture: Structural Subtree Parsing & Shorts Interception, Unified Detection / Fusion Layer & Event Serialization, Ad Policy Retention & Stale Lookup Protection.

## 🔒 My Identity
- Archetype: explorer
- Roles: investigation, synthesis
- Working directory: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2
- Original parent: 1d3a3e93-6ed8-4bde-88be-e498610f2665
- Milestone: Orchestrator 2 Survey - Pillars 3 & 4 Investigation

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Scope: Pillars 3 & 4 (Phases 3, 4, 7, 8, 11, 12, 15)
- All metadata and reports in working directory

## Current Parent
- Conversation ID: 1d3a3e93-6ed8-4bde-88be-e498610f2665
- Updated: 2026-09-11T15:33:00Z

## Investigation State
- **Explored paths**:
  - `app/src/main/java/com/safarparmar/app/feature/youtube/detector/YouTubeSnapshotParser.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/detector/YouTubeSnapshot.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeNotificationListener.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/policy/VideoIdentity.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/policy/PolicyState.kt`
  - `app/src/main/res/xml/youtube_guard_service.xml`
  - `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt`
  - `app/src/test/java/com/safarparmar/app/feature/youtube/e2e/YouTubeGuardE2ETest.kt`
- **Key findings**:
  - Phase 3: `collectNodes` collapses tree hierarchy, globally searching all nodes and polluting channel identity from suggestions/comments; synthesizes fake `@handles`. Replace with structural `YouTubeUiParser` isolating `WatchMetadataSubtree`, `PlayerSubtree`, `ShortsPlayerSubtree`, and `ChannelHeaderSubtree`.
  - Phase 7: `TYPE_VIEW_CLICKED` missing in service registration. Add `TYPE_VIEW_CLICKED` to intercept bottom nav Shorts tab and Shorts shelf cards immediately before reel player mounts.
  - Phase 4: `YouTubeGuardService` and `YouTubeNotificationListener` operate independently without fusion. Introduce `YouTubeContentDetector` fusing UI snapshots and MediaSession metadata into `CanonicalYouTubeContent` with `DetectionConfidence`.
  - Phase 8: Multiple concurrent coroutines launched per accessibility event without synchronization. Implement `Channel<DetectionEvent>` sequential event pipeline.
  - Phase 11: Ad policy engine check does not verify video fingerprint continuity; pre-roll ads on distracting videos inherit productive policy if preceded by productive video. Fix: ad on changed fingerprint must stay in `Verifying`; advertiser metadata never leaks into channel identity.
  - Phase 12: Cancelled coroutines do not protect against race conditions or out-of-order Room returns. Introduce atomic `generationToken` guarding state transitions and cancelling prior in-flight jobs.
  - Phase 15: Intercept suggested video card clicks on `TYPE_VIEW_CLICKED` to enter `Verifying` immediately; support channel profile pages via `ChannelHeaderSubtree`.
- **Unexplored areas**: None within Pillars 3 & 4.

## Key Decisions Made
- Completed comprehensive gap analysis and target architectural design across Phases 3, 4, 7, 8, 11, 12, 15.
- Defined target class models: `CanonicalYouTubeContent`, `ChannelIdentity`, `VideoFingerprint`, `ScreenType`, `YouTubeUiParser`, `YouTubeContentDetector`.
- Documented full handoff report in `handoff.md`.

## Artifact Index
- handoff.md — Comprehensive survey and analysis report for Pillars 3 & 4
- DISPATCH.md — Task assignment and instructions
- progress.md — Liveness and task execution log
