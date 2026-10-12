# Original User Request

## 2026-09-11T12:52:54Z

Build a YouTube content blocker using an AccessibilityService and MediaSessionManager that categorizes channels as productive or distracting, blocks Shorts entirely, and uses a determinist policy engine with a Compose accessibility overlay.

Working directory: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android
Integrity mode: development

## Requirements

### R1. Core Detection Service
Create an AccessibilityService (`YouTubeGuardService`) that watches `com.google.android.youtube` to identify the current screen, video title, channel name/handle, and whether it's an ad or Shorts. Combine this with a MediaSessionManager listener (`YouTubeNotificationListener`) to catch fullscreen identity changes.

### R2. State Machine & Policy Engine
Implement a deterministic state machine (`YouTubePolicyEngine`) that manages states (Verifying, AskUser, Allowed, Blocked, ShortsBlocked). The system MUST default to 'Verifying' on any video identity change, never temporarily allowing unknown content.

### R3. Persistence
Store channel classifications (PRODUCTIVE, DISTRACTING) using a Room database entity (`YouTubeChannelEntity`).

### R4. Blocking Overlay
Present a blocking screen via `TYPE_ACCESSIBILITY_OVERLAY` using Compose. This overlay must allow the user to classify unknown channels, inform them when a channel is blocked, and block Shorts entirely.

## Acceptance Criteria

### Detection & State Machine
- [ ] Unit tests for `YouTubePolicyEngine` verify that a new video identity always transitions to 'Verifying' before resolving to 'Allowed' or 'Blocked'.
- [ ] Shorts are evaluated and blocked independently of the channel policy.
- [ ] Ad transitions retain the previous verified channel's policy instead of evaluating the ad's metadata.

### System Integration
- [ ] The Room database successfully saves and retrieves `ChannelPolicy` (PRODUCTIVE/DISTRACTING).
- [ ] The blocking UI is implemented as an Accessibility Overlay (`TYPE_ACCESSIBILITY_OVERLAY`) using Jetpack Compose, avoiding background activity launch restrictions.

## 2026-09-11T15:25:10Z

Execute the YouTube Channel Blocker fixes plan across all 15 phases to achieve reliable, fail-closed blocking across normal videos, suggested videos, autoplay, fullscreen, channel pages, ads, and Shorts.

Working directory: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android
Integrity mode: development

## Requirements

### R1. Fail-Closed Verifying Overlay & State Machine (Phases 1, 6, 13)
- Introduce a full-screen, touch-consuming `OverlayState.Verifying` overlay using `TYPE_ACCESSIBILITY_OVERLAY` whenever policy state is `PolicyState.Verifying`.
- Ensure newly opened or changed YouTube videos never become interactable before channel identity and policy are resolved.
- Fullscreen switches, suggested video clicks, and autoplay must immediately enter `Verifying` overlay state.

### R2. Canonical Channel Identity & Fingerprinting (Phases 2, 5, 14)
- Model `ChannelIdentity` with `verifiedHandle: String?` (only if real @handle detected, never synthesized or fabricated) and `displayName: String`.
- Compute canonical keys: `handle:<handle>` or `name:<normalized_display_name>`.
- Model `VideoFingerprint` prioritizing `mediaId` if present, else normalized title + canonical channel key. Deduplicate MediaSession via fingerprint, not raw title.
- Update Room repository and entity to operate on `ChannelIdentity` without fake handles.

### R3. Structural Subtree Parsing (Phases 3, 7)
- Stop flattening the entire accessibility tree into a global search.
- Implement screen-aware structural parsing (`YouTubeUiParser`) locating specific subtrees: watch metadata, current player, channel header, shorts player.
- Block Shorts based on structural reel/player signals and bottom navigation click interception (`TYPE_VIEW_CLICKED`), not generic global text search.

### R4. Unified Detection / Fusion Layer & Event Serialization (Phases 4, 8, 11, 12, 15)
- Create `YouTubeContentDetector` fusing Accessibility and MediaSession inputs into `CanonicalYouTubeContent` with explicit `DetectionConfidence` scoring.
- Serialize detection events via an event channel/stream before updating `PolicyEngine`.
- Handle ads properly: retain existing verified policy if ad appears during a known video; if playback fingerprint changed with an ad, stay in `Verifying`.
- Add stale lookup / cancellation protection via generation tokens and active job cancellation.

### R5. Lifecycle & Robustness (Phases 9, 10)
- Detect when YouTube leaves foreground (via global window state changes) and immediately dismiss the overlay and cancel transient detection.
- Make `NotificationListener` coroutine scope recreatable across disconnect/reconnect and clean up old `MediaController.Callback`s.

## Acceptance Criteria

### Verification & Policy Guardrails
- [ ] Transitioning to any new video fingerprint immediately triggers `OverlayState.Verifying` before resolving to `Allowed`, `AskUser`, or `Blocked`.
- [ ] Channel identity never generates or persists synthetic `@handles` from display names.
- [ ] Suggested video or comment section text outside the current watch/player subtree does not pollute the detected channel identity.
- [ ] Shorts are detected structurally and blocked immediately (including view click interception on navigation tabs) without relying on channel policy.
- [ ] Advertisements do not adopt advertiser info as the channel identity, and switching videos into an ad enters `Verifying`.
- [ ] Stale Room lookups or out-of-order events from previous videos cannot overwrite the policy state of the current video.
- [ ] Leaving YouTube (switching apps / pressing Home) immediately hides the overlay and cancels active detection.
- [ ] Automated unit test suites covering the state machine, fusion detector, parser, and fingerprinting compile and pass.
