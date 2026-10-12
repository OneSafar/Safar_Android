# Project: YouTube Channel Blocker 15-Phase Fixes Plan

## Architecture Overview
The YouTube Channel Blocker enforces fail-closed distraction management across YouTube on Android. It operates via:
1. **Core Models & Persistence**: `ChannelIdentity` (clean canonical keys, no fake handles), `VideoFingerprint`, and Room entity `YouTubeChannelEntity`.
2. **Deterministic Policy State Machine**: `YouTubePolicyEngine` with fail-closed `Verifying` state, generation tokens for cancellation, ad policy retention, and user classification.
3. **Accessibility Overlay**: `YouTubeGuardOverlayManager` with Compose overlay consuming pointer inputs during `Verifying`, `Blocked`, and `ShortsBlocked`.
4. **Structural Subtree Parser**: `YouTubeUiParser` isolating watch metadata, player, shorts reel, and channel header subtrees while ignoring suggestions and comments.
5. **Unified Content Detector**: `YouTubeContentDetector` fusing Accessibility and MediaSession events with confidence scoring and an internal serialized channel.
6. **Lifecycle & Foreground Manager**: `YouTubeForegroundTracker` and robust `YouTubeNotificationListener` with clean coroutine lifecycle and callback cleanup.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Fail-Closed Verifying Overlay (Phase 1) | `OverlayState.Verifying` overlay via `TYPE_ACCESSIBILITY_OVERLAY`, touch-consuming pointer blocker | M2 | R1 |
| 2 | Canonical Channel Identity (Phase 2) | `ChannelIdentity` with nullable real `verifiedHandle`, `displayName`, and canonical key | M1 | R2 |
| 3 | Structural Subtree Parsing (Phase 3) | `YouTubeUiParser` extracting isolated subtrees, ignoring suggestions and comments | M3 | R3 |
| 4 | Unified Fusion Layer (Phase 4) | `YouTubeContentDetector` fusing Accessibility & MediaSession with `DetectionConfidence` | M4 | R4 |
| 5 | Video Fingerprinting (Phase 5) | `VideoFingerprint` prioritizing mediaId, then title + channel canonical key | M1 | R2 |
| 6 | Fullscreen & Autoplay Transitions (Phase 6) | Automatic transition to `Verifying` upon video identity/fingerprint change | M2 | R1 |
| 7 | Structural Shorts & Click Interception (Phase 7) | Structural reel detection + bottom nav / card click interception (`TYPE_VIEW_CLICKED`) | M3 | R3 |
| 8 | Event Stream Serialization (Phase 8) | Serialized `Channel<DetectionEvent>` sequential event pipeline | M4 | R4 |
| 9 | Foreground Exit & Overlay Dismissal (Phase 9) | Detect leaving YouTube via window state; dismiss overlay & cancel active detection | M5 | R5 |
| 10 | Service Lifecycle & Leak Prevention (Phase 10) | Recreatable coroutine scope, `onDestroy()` teardown, listener cleanup in `NotificationListener` | M5 | R5 |
| 11 | Ad Policy Retention & Switch Security (Phase 11) | Retain policy during ad of same video; enter Verifying if video changed; ignore advertiser | M2 | R4 |
| 12 | Stale Lookup Protection (Phase 12) | Monotonic generation tokens (`AtomicLong`) and active job cancellation | M2 | R4 |
| 13 | Verifying Edge Cases & Touch Consumption (Phase 13) | Rapid video switching, cold launch, mini-player edge cases with pointer consumption | M2 | R1 |
| 14 | Room Persistence Without Fake Handles (Phase 14) | Update `YouTubeChannelEntity` & DAO with `canonicalKey` primary key, no synthetic handles | M1 | R2 |
| 15 | Suggested Videos & Channel Pages (Phase 15) | Suggested video click interception & channel profile header subtree parsing | M3 | R3 / R4 |

## Milestones
| # | Milestone | Scope | Dependencies | Status |
|---|-----------|-------|--------------|--------|
| M1 | Models, Identity & Room Persistence | Phases 2, 5, 14: `ChannelIdentity`, `VideoFingerprint`, `CanonicalYouTubeContent`, `YouTubeChannelEntity`, `YouTubeChannelDao`, `YouTubeChannelRepository` | None | DONE |
| M2 | Fail-Closed Policy Engine & Verifying Overlay | Phases 1, 6, 11, 12, 13: `OverlayState.Verifying`, pointer input blocker, `YouTubePolicyEngine` fail-closed transitions, generation tokens, ad policy retention | M1 | DONE |
| M3 | Structural Subtree Parsing & Shorts Interception | Phases 3, 7, 15: `YouTubeUiParser`, subtree isolation, shorts reel detection, `TYPE_VIEW_CLICKED` interception, channel profile parsing | M1 | PLANNED |
| M4 | Unified Fusion Layer & Event Serialization | Phases 4, 8: `YouTubeContentDetector`, Accessibility + MediaSession fusion, `DetectionConfidence`, sequential event channel | M1, M2, M3 | PLANNED |
| M5 | Lifecycle Robustness & Full Verification | Phases 9, 10: `YouTubeForegroundTracker`, window monitoring, `YouTubeNotificationListener` scope/leak fixes, full test suite pass | M1, M2, M3, M4 | PLANNED |

## Interface Contracts
### ChannelIdentity & Canonical Keys
```kotlin
data class ChannelIdentity(
    val verifiedHandle: String? = null, // Real @handle only, null otherwise
    val displayName: String = "",
) {
    val canonicalKey: String
        get() = when {
            !verifiedHandle.isNullOrBlank() -> "handle:${normalizeHandle(verifiedHandle)}"
            displayName.isNotBlank() -> "name:${normalizeDisplayName(displayName)}"
            else -> ""
        }
}
```

### VideoFingerprint
```kotlin
data class VideoFingerprint(
    val mediaId: String? = null,
    val normalizedTitle: String = "",
    val channelCanonicalKey: String = "",
) {
    val key: String
        get() = when {
            !mediaId.isNullOrBlank() -> "media:$mediaId"
            normalizedTitle.isNotBlank() || channelCanonicalKey.isNotBlank() -> "content:$normalizedTitle|$channelCanonicalKey"
            else -> ""
        }
}
```

### OverlayState & Manager
```kotlin
sealed interface OverlayState {
    data object Hidden : OverlayState
    data class Verifying(val channelName: String = "", val title: String = "") : OverlayState
    data class Blocked(val channelName: String, val title: String, val reason: BlockReason = BlockReason.CHANNEL_DISTRACTING) : OverlayState
    data class ShortsBlocked(val message: String = "YouTube Shorts are blocked to preserve focus") : OverlayState
    data class AskUser(val channelName: String, val channelHandle: String, val videoTitle: String, val isAd: Boolean = false) : OverlayState
}
```

### Unified Content Detector
```kotlin
data class CanonicalYouTubeContent(
    val fingerprint: VideoFingerprint,
    val channelIdentity: ChannelIdentity,
    val title: String,
    val isShorts: Boolean,
    val isAd: Boolean,
    val isChannelPage: Boolean,
    val confidence: DetectionConfidence,
)
```

## Code Layout
- Models: `app/src/main/java/com/safarparmar/app/feature/youtube/model/`
- Policy: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/`
- Detector & Parser: `app/src/main/java/com/safarparmar/app/feature/youtube/detector/`
- Overlay: `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/`
- Services & Lifecycle: `app/src/main/java/com/safarparmar/app/feature/youtube/service/`
- Data Layer: `app/src/main/java/com/safarparmar/app/data/local/` and `com/safarparmar/app/data/repository/`
- Unit Tests: `app/src/test/java/com/safarparmar/app/feature/youtube/` and `app/src/test/java/com/safarparmar/app/data/`
