# Comprehensive Survey & Architectural Analysis Report: Pillars 3 & 4
## YouTube Content Blocker 15-Phase Fixes Plan

**Author**: Survey Explorer 2 (Orchestrator 2)  
**Date**: 2026-09-11  
**Working Directory**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2`  
**Target Path**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_orch2/handoff.md`  
**Codebase Scope**: `app/src/main/java/com/safarparmar/app/feature/youtube/`  
**Pillars Focus**: Pillar 3 (Phases 3, 7) & Pillar 4 (Phases 4, 8, 11, 12, 15)

---

## 1. Observation

### 1.1 Current Parser Implementation (`YouTubeSnapshotParser.kt`)
File path: `app/src/main/java/com/safarparmar/app/feature/youtube/detector/YouTubeSnapshotParser.kt`

- **Global Tree Flattening Destroys Hierarchy (Lines 64–66 & 125–136)**:
  ```kotlin
  val allNodes = mutableListOf<AccessibilityNodeInfo>()
  collectNodes(root, allNodes)
  ```
  `collectNodes` traverses every descendant node from `rootInActiveWindow` and places copies into a flat list `allNodes`. This completely erases screen structure, spatial boundaries, and parent-child encapsulation.
- **Unbounded Global Queries Cause Pollution from Feed, Comments & Suggestions (Lines 72–74 & 138–210)**:
  ```kotlin
  var title = extractByIds(allNodes, TITLE_IDS)
  var channelName = extractByIds(allNodes, CHANNEL_NAME_IDS)
  var channelHandle = extractChannelHandle(allNodes)
  ```
  `extractByIds`, `extractChannelHandle`, and `extractChannelNameFromDescriptions` linearly iterate across `allNodes`. If the user scrolls below the video player, `allNodes` contains:
  1. Suggested video list (`watch_list`, `related_videos`, `compact_video_renderer`): Contains suggestions with their own `video_title` and `channel_name` view IDs.
  2. Comments section (`comments_entry_point_layout`, `comment_thread`): Contains user comments with `@handle` references (e.g. `@friend checkout this timestamp`).
  3. Feed cards: Video cards with "Subscribe to..." content descriptions.
  Because the search is global, the first or last matching node in traversal order is grabbed, causing suggested videos or comment authors to corrupt the active video identity.
- **Synthesizing Fake Handles Violates Integrity Guarantees (Lines 81–88)**:
  ```kotlin
  // Synthesize handle from channelName if no explicit @handle was present
  if (channelHandle.isBlank() && channelName.isNotBlank()) {
      channelHandle = if (channelName.startsWith("@")) {
          channelName.lowercase()
      } else {
          "@${channelName.trim().replace("\\s+".toRegex(), "").lowercase()}"
      }
  }
  ```
  This creates artificial `@handles` from channel display names (e.g., "Veritasium" becomes `@veritasium`, "Khan Academy" becomes `@khanacademy`), which violates Requirement R2: *"Channel identity never generates or persists synthetic `@handles` from display names."*
- **Post-Hoc Shorts Detection without Click Interception (Lines 215–252)**:
  Shorts detection relies solely on DOM scanning after the Shorts layout has already rendered:
  ```kotlin
  private fun detectShorts(nodes: List<AccessibilityNodeInfo>): Boolean { ... }
  ```
  There is no anticipation or click interception when the user taps the Shorts navigation icon or a Shorts shelf item.
- **In-Stream Ad Detection Unscoped from Player (Lines 258–276)**:
  `detectInStreamAd` iterates through `allNodes` looking for `IN_STREAM_AD_IDS` or `"Ad ·"`. If a sponsored card or feed ad is present anywhere in the tree outside the player, it can falsely trigger ad mode.

---

### 1.2 Current Accessibility Service Configuration & Event Flow (`YouTubeGuardService.kt`)
File path: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`
Configuration path: `app/src/main/res/xml/youtube_guard_service.xml`

- **Missing `TYPE_VIEW_CLICKED` Subscription (Lines 45–54 & XML Line 4)**:
  ```kotlin
  serviceInfo = AccessibilityServiceInfo().apply {
      eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
          AccessibilityEvent.TYPE_VIEW_SCROLLED
      ...
  ```
  Neither `YouTubeGuardService.onServiceConnected()` nor `res/xml/youtube_guard_service.xml` subscribes to `AccessibilityEvent.TYPE_VIEW_CLICKED`. Consequently:
  - Taps on the bottom navigation "Shorts" tab cannot be intercepted prior to screen switch.
  - Taps on suggested video cards cannot trigger instantaneous transition to `OverlayState.Verifying`.
- **Concurrent, Unserialized Event Handling (Lines 68–99)**:
  ```kotlin
  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
      ...
      serviceScope.launch(Dispatchers.Default) {
          val root = rootInActiveWindow ?: return@launch
          val snapshot: YouTubeSnapshot
          ...
          policyEngine.onVideoDetected(snapshot.toVideoIdentity())
      }
  }
  ```
  Every accessibility event launches an independent coroutine on `Dispatchers.Default`. There is NO event channel, no actor, and no serialization queue. Rapid DOM changes produce concurrent coroutines that race against each other, leading to out-of-order execution where an older snapshot can arrive after a newer one.
- **Hiding Overlay on `PolicyState.Verifying` (Lines 134–135)**:
  ```kotlin
  // Verifying is an internal ~2ms DB check state — do not flash overlay
  is PolicyState.Verifying -> overlayManager.hide()
  ```
  This is an explicit fail-open vulnerability. While the DB query is pending or during slow video loads, the overlay is hidden, allowing the user to view and interact with unverified content.
- **Zero Coordination with MediaSession Service**:
  `YouTubeGuardService` does not communicate with `YouTubeNotificationListener`. Both components independently push conflicting, unsynchronized events into `YouTubePolicyEngine`.

---

### 1.3 Current MediaSession Listener Implementation (`YouTubeNotificationListener.kt`)
File path: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeNotificationListener.kt`

- **Deduplication Solely on Raw Title (Lines 110–112)**:
  ```kotlin
  // Avoid duplicate processing of the same title
  if (title == lastTitle) return
  lastTitle = title
  ```
  If a creator posts two videos with identical or empty titles, or during metadata updates, deduplication fails or suppresses legitimate changes. There is no `VideoFingerprint` utilizing `mediaId`.
- **Bypasses Fusion Layer (Lines 126–128)**:
  ```kotlin
  listenerScope.launch {
      policyEngine.onVideoDetected(identity)
  }
  ```
  Directly invokes `policyEngine.onVideoDetected` with empty `channelHandle`, racing against `YouTubeGuardService`.
- **Coroutine Scope Not Recreatable on Reconnect (Lines 33 & 78)**:
  ```kotlin
  override fun onListenerDisconnected() {
      ...
      listenerScope.cancel()
      super.onListenerDisconnected()
  }
  ```
  Once `listenerScope.cancel()` is called on disconnect, the supervisor job is cancelled permanently. If the service is subsequently reconnected by the system (`onListenerConnected()`), new coroutines launched on `listenerScope` fail immediately without executing.

---

### 1.4 Current Policy Engine Implementation (`YouTubePolicyEngine.kt`)
File path: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`

- **Ad Policy Vulnerability: Cross-Video Policy Carryover (Lines 76–101)**:
  ```kotlin
  if (identity.isAd) {
      when (lastVerifiedPolicy) {
          ChannelPolicy.PRODUCTIVE -> {
              _state.value = PolicyState.Allowed(
                  identity = identity,
                  channelHandle = lastVerifiedChannel ?: identity.channelHandle,
              )
          }
          ...
  ```
  If the user watches Video A (Productive -> `lastVerifiedPolicy = PRODUCTIVE`), and then navigates to Video B (Distracting or Unclassified) which begins with a pre-roll ad:
  Because `identity.isAd == true`, the engine checks `lastVerifiedPolicy` (still `PRODUCTIVE` from Video A) and immediately marks the ad as `Allowed`! The distracting/unclassified video starts playing under the previous video's productive policy.
- **Advertiser Handle Leakage (Lines 81 & 87)**:
  ```kotlin
  channelHandle = lastVerifiedChannel ?: identity.channelHandle
  ```
  If `lastVerifiedChannel` is not set or if `identity` contains the advertiser's name, advertiser information is adopted as the video identity.
- **Absence of Generation Tokens for Stale DB Lookup Protection (Lines 57–66 & 104–127)**:
  ```kotlin
  activeJob?.cancel()
  currentIdentity = identity
  _state.value = PolicyState.Verifying(identity)
  activeJob = scope.launch {
      resolvePolicy(identity)
  }
  ```
  While `activeJob?.cancel()` cancels the local coroutine, if a repository query `repository.getChannelPolicy(...)` or background task is suspended or in-flight, there is no generation counter. A stale lookup can return and overwrite the state of a newer video.

---

## 2. Logic Chain

### 2.1 Phase 3: Structural Subtree Parsing (`YouTubeUiParser`)
1. **Premise**: In an AccessibilityNodeInfo hierarchy of the YouTube app, different functional sections reside in distinct layout containers:
   - **Active Player Subtree**: Contains video surface, player controls, timebar, and in-stream ad overlays (`skip_ad_button`, `ad_badge`, `ad_countdown`).
   - **Watch Metadata Subtree**: Positioned immediately below the player container in portrait mode. Contains the primary video title (`video_title`, `title`), channel avatar/badge, creator name/handle (`owner_row`, `channel_name`, `author`), and the subscribe button.
   - **Suggestions & Feed Subtree**: Positioned below the metadata and comments. Contains scrollable video cards (`compact_video_renderer`, `related_videos`, `results_list`) with secondary titles, secondary channel names, and thumbnail views.
   - **Comments Subtree**: Contains user comments with arbitrary `@handle` tags.
   - **Shorts Player Subtree**: Vertical full-screen pager containing `reel_watch_player_layout`, `reel_player_page`, and Shorts action buttons.
   - **Channel Header Subtree**: On channel pages, the header container displays the channel banner, title, verified `@handle`, and subscriber count.
2. **Failure of Global Scan**: Flattening the entire tree via `collectNodes` collapses these boundaries. When `extractChannelHandle` searches for `@`, it cannot distinguish between the video creator's handle and an `@handle` in the comment section or in a suggested video card.
3. **Architectural Remedy (`YouTubeUiParser`)**:
   - Determine `ScreenType`: `WATCH`, `SHORTS`, `CHANNEL_PAGE`, `FEED`, `UNKNOWN`.
   - Locate the dedicated subtree:
     - When `ScreenType == WATCH`: Parse video title and creator identity EXCLUSIVELY inside the `WatchMetadataSubtree`.
     - When searching for in-stream ads: Search EXCLUSIVELY inside the `PlayerSubtree`.
     - When `ScreenType == SHORTS`: Search EXCLUSIVELY inside the `ShortsPlayerSubtree`.
     - When `ScreenType == CHANNEL_PAGE`: Search EXCLUSIVELY inside the `ChannelHeaderSubtree`.
   - Completely prune/ignore `related_videos`, `results_list`, and `comments` subtrees during video identity extraction.

---

### 2.2 Phase 7: Shorts Detection & Interception
1. **Premise**: YouTube Shorts autoplay instantly upon opening. If detection waits until the Shorts layout has finished rendering and `TYPE_WINDOW_CONTENT_CHANGED` fires:
   - Several hundred milliseconds elapse between the user tap and policy enforcement.
   - Video frames render and audio plays, violating focus session boundaries.
2. **Interception via `TYPE_VIEW_CLICKED`**:
   - When a user taps the bottom navigation bar "Shorts" tab, or taps a Shorts shelf item on the Home feed, Android delivers an `AccessibilityEvent.TYPE_VIEW_CLICKED`.
   - By inspecting `event.source` and its immediate ancestors:
     - Target matching `contentDescription == "Shorts"` or `viewIdResourceName` matching `shorts_tab` / `reel_tab` / `reel_item` / `shorts_shelf` indicates an impending Shorts playback.
3. **Architectural Remedy**:
   - Register `AccessibilityEvent.TYPE_VIEW_CLICKED` in both `res/xml/youtube_guard_service.xml` and `YouTubeGuardService.onServiceConnected()`.
   - On `TYPE_VIEW_CLICKED`, immediately intercept:
     - Emit an immediate `ShortsClickIntercepted` event to the detector.
     - Transition immediately to `PolicyState.ShortsBlocked` (or fail-closed `PolicyState.Verifying`), presenting the touch-consuming overlay before the reel activity/fragment initializes.
     - If audio is already active, invoke `transportControls.pause()` via MediaSession.
   - Supplement with structural detection in `YouTubeUiParser`: identify `reel_watch_player_layout`, `reel_player_page`, `reel_scrim_shorts`, and reel pager gestures.

---

### 2.3 Phase 4: Unified Detection / Fusion Layer (`YouTubeContentDetector`)
1. **Premise**: AccessibilityService and MediaSessionManager each possess complementary strengths and specific weaknesses:
   - **Accessibility**: Accurately detects visual elements (verified `@handle`, in-stream ad badges, Shorts tabs), but UI nodes disappear during fullscreen playback, autohiding controls, or fast scrolling.
   - **MediaSession**: Authoritatively reports `title`, `artist` (channel display name), and playback state (`PLAYING`, `PAUSED`, `STOPPED`) during fullscreen and background playback, but lacks verified `@handle` and does not distinguish Shorts or ads.
2. **Failure of Independent Channels**: When both services independently invoke `YouTubePolicyEngine.onVideoDetected`, they trigger conflicting state transitions, redundant DB lookups, and race conditions.
3. **Architectural Remedy (`YouTubeContentDetector`)**:
   - Introduce a centralized singleton `YouTubeContentDetector` as the sole bridge between OS events and `YouTubePolicyEngine`.
   - Define canonical output model `CanonicalYouTubeContent`:
     ```kotlin
     data class CanonicalYouTubeContent(
         val fingerprint: VideoFingerprint,
         val channelIdentity: ChannelIdentity,
         val title: String,
         val isShorts: Boolean,
         val isAd: Boolean,
         val confidence: DetectionConfidence,
         val source: DetectionSource,
         val generationToken: Long,
         val timestamp: Long = System.currentTimeMillis(),
     )
     ```
   - **Fusion Rules**:
     - *Full Match (Confidence = HIGH)*: Accessibility extracts title and verified `@handle` from `WatchMetadataSubtree`; MediaSession title and artist match.
     - *Fullscreen Fallback (Confidence = MEDIUM/HIGH)*: Accessibility detects active player subtree without text nodes; MediaSession reports valid title and artist. If prior UI snapshot for this video had `@handle`, fuse them; otherwise use canonical `displayName` key.
     - *Ad Playing*: Player subtree detects in-stream ad badge/skip button (`isAd = true`). Retain underlying video's fingerprint; do NOT overwrite channel identity with advertiser data.
     - *Shorts Detected*: Either click interception or reel DOM signals `isShorts = true`.

---

### 2.4 Phase 8: Event Serialization
1. **Premise**: Accessibility events are asynchronous and high-frequency (a single swipe can produce 10+ content-changed and scroll events in under 100ms).
2. **Failure of Unsynchronized Coroutines**: Spawning unconstrained coroutines on `Dispatchers.Default` for each event produces non-deterministic interleaving. Coroutine A (event at t=0) may complete AFTER Coroutine B (event at t=50ms), causing an obsolete snapshot to overwrite active policy state.
3. **Architectural Remedy**:
   - Inside `YouTubeContentDetector`, implement an event processing channel:
     ```kotlin
     private val eventChannel = Channel<DetectionEvent>(capacity = Channel.BUFFERED)
     ```
   - Launch a dedicated sequential processing loop:
     ```kotlin
     scope.launch(Dispatchers.Default) {
         eventChannel.receiveAsFlow()
             .conflate() // Debounce redundant rapid-fire DOM noise
             .collect { event ->
                 processAndDispatch(event)
             }
     }
     ```
   - High-priority events (e.g. `ShortsClickIntercepted`, video fingerprint change) bypass debouncing and are processed immediately.

---

### 2.5 Phase 11: Ad Policy Retention & Ad Video Switch Protection
1. **Premise**: In-stream advertisements occur in two distinct scenarios:
   - **Scenario A: Mid-Roll Ad during Known Video**: The user is already watching an allowed video; a mid-roll ad interrupts playback.
   - **Scenario B: Pre-Roll Ad on New Video / Video Switch**: The user opens a new video or autoplay advances to a new video that begins immediately with an ad.
2. **Defect Analysis of Current Engine**:
   - Current logic checks `lastVerifiedPolicy` unconditionally whenever `identity.isAd == true`.
   - In Scenario B, if the preceding video was `PRODUCTIVE`, the new video's pre-roll ad is allowed, even if the new video is a distracting channel!
3. **Architectural Remedy**:
   - Track `activeVideoFingerprint` and `lastVerifiedPolicy`.
   - When an ad is detected (`isAd == true`):
     - **Check Fingerprint Continuity**:
       - If `ad.fingerprint == activeVideoFingerprint` (or mid-roll within the established video session):
         Retain `lastVerifiedPolicy` (Productive remains Allowed; Distracting remains Blocked).
       - If `ad.fingerprint != activeVideoFingerprint` (new video started, autoplay into ad, or cold launch directly into an ad):
         MUST enter and remain in `PolicyState.Verifying` fail-closed!
     - **Zero Advertiser Leakage**:
       - An ad event must never produce a `ChannelIdentity` with advertiser name/handle.
       - The repository must NEVER be queried for advertiser names, and advertiser info must never be saved to Room.

---

### 2.6 Phase 12: Stale Protection & Job Cancellation via Generation Tokens
1. **Premise**: Network, disk I/O, and database queries introduce variable latency. A slow Room DB query for Video 1 might take 100ms, while a fast in-memory query for Video 2 takes 2ms.
2. **Defect Analysis**: Merely calling `job.cancel()` is insufficient if the query has already returned its result and is about to update `_state.value`, or if events were queued.
3. **Architectural Remedy**:
   - Introduce an atomic generation token: `private val currentGeneration = AtomicLong(0L)`.
   - On every new video detection or video fingerprint change:
     - Increment generation: `val generationToken = currentGeneration.incrementAndGet()`.
     - Cancel active in-flight policy resolution jobs.
     - Pass `generationToken` through all asynchronous phases:
       ```kotlin
       val policy = repository.getChannelPolicy(key)
       if (generationToken != currentGeneration.get()) {
           Log.d(TAG, "Discarding stale policy lookup for generation $generationToken")
           return
       }
       ```
   - State mutations (`_state.value = ...`) are rejected if the generation token does not match `currentGeneration.get()`.

---

### 2.7 Phase 15: Suggested Videos & Channel Page Interactions
1. **Suggested Videos Interaction**:
   - Structural isolation in `YouTubeUiParser` guarantees suggested videos in `related_videos` cannot pollute the active video's title or creator identity.
   - When the user clicks a suggested video card (`compact_video_renderer`):
     `TYPE_VIEW_CLICKED` intercepts the tap and immediately dispatches an `InitiateVideoSwitch` event to the detector.
     The system enters `PolicyState.Verifying` and activates `OverlayState.Verifying` before the new video loads.
2. **Channel Page Interaction**:
   - When user browses a creator's channel page (`ScreenType.CHANNEL_PAGE`):
     - Screen contains `ChannelHeaderSubtree` with channel banner, avatar, title, and verified handle.
     - No video player is active (`isShorts = false`, `isAd = false`, `playerSubtree == null`).
     - `YouTubeUiParser` extracts `ChannelIdentity` directly from `ChannelHeaderSubtree`.
     - If the channel is classified as `DISTRACTING`, the entire channel page is blocked (`PolicyState.Blocked`).
     - If `PRODUCTIVE`, channel page is allowed (`PolicyState.Allowed`).
     - If unclassified, prompts `PolicyState.AskUser`.

---

## 3. Caveats

1. **YouTube Dynamic DOM & Litho Obfuscation**:
   - Modern versions of YouTube heavily utilize ComponentKit/Litho and Compose, which occasionally omit standard Android view IDs (e.g. `R.id.video_title`).
   - Structural parsing must pair view ID checks (`watch_video_title`, `channel_name`) with robust accessibility hierarchy rules (e.g. content descriptions on Subscribe buttons, subscriber count patterns, and relative node coordinates/hierarchy).
2. **MediaSession Asynchronous Latency**:
   - `MediaController.Callback.onMetadataChanged` may lag UI changes by 100–300ms during initial playback buffer.
   - `YouTubeContentDetector` must maintain fail-closed state (`PolicyState.Verifying`) until both sources reconcile or the confidence threshold is satisfied.
3. **Backward Compatibility with Existing Unit & E2E Tests**:
   - Existing test suites (`YouTubePolicyEngineTest` with 1206 lines, `YouTubeGuardE2ETest` with 1799 lines) use `VideoIdentity(videoId, title, channelHandle, channelName, isShorts, isAd)`.
   - New models (`CanonicalYouTubeContent`, `ChannelIdentity`, `VideoFingerprint`) should either provide clean mapping to `VideoIdentity` or overloaded entry points on `YouTubePolicyEngine` to avoid breaking existing passes.
4. **Touch Consumption on `OverlayState.Verifying`**:
   - To guarantee fail-closed security, `YouTubeGuardOverlayManager` must ensure `OverlayState.Verifying` uses `WindowManager.LayoutParams` without `FLAG_NOT_TOUCHABLE`, so all touches are consumed by the overlay while verifying.

---

## 4. Conclusion & Proposed Architecture

### 4.1 Target Class & Interface Definitions

#### `ChannelIdentity.kt` & `VideoFingerprint.kt`
```kotlin
package com.safarparmar.app.feature.youtube.policy

/**
 * Authoritative representation of a YouTube channel identity.
 * Strictly guarantees verifiedHandle is ONLY populated when a genuine @handle is detected.
 */
data class ChannelIdentity(
    val verifiedHandle: String? = null, // Only true @handle, e.g. "@veritasium". NEVER synthesized.
    val displayName: String = "",       // Display name, e.g. "Veritasium"
) {
    /**
     * Canonical lookup key for Room DB and policy resolution.
     * Format: "handle:<handle>" or "name:<normalized_display_name>"
     */
    val canonicalKey: String get() {
        val handle = verifiedHandle?.trim()?.lowercase()
        if (!handle.isNullOrBlank()) {
            val normalized = if (handle.startsWith("@")) handle else "@$handle"
            return "handle:$normalized"
        }
        val normalizedName = displayName.trim().lowercase().replace("\\s+".toRegex(), " ")
        return if (normalizedName.isNotBlank()) "name:$normalizedName" else ""
    }

    val isResolvable: Boolean get() = canonicalKey.isNotBlank()
}

data class VideoFingerprint(
    val mediaId: String? = null,
    val canonicalChannelKey: String = "",
    val normalizedTitle: String = "",
) {
    fun matches(other: VideoFingerprint?): Boolean {
        if (other == null) return false
        if (!mediaId.isNullOrBlank() && !other.mediaId.isNullOrBlank()) {
            return mediaId == other.mediaId
        }
        return normalizedTitle == other.normalizedTitle &&
            canonicalChannelKey == other.canonicalChannelKey
    }

    companion object {
        val EMPTY = VideoFingerprint()
        fun create(mediaId: String?, title: String, channelKey: String): VideoFingerprint {
            val normTitle = title.trim().lowercase().replace("\\s+".toRegex(), " ")
            return VideoFingerprint(mediaId = mediaId?.trim(), canonicalChannelKey = channelKey, normalizedTitle = normTitle)
        }
    }
}
```

#### `ScreenType.kt` & `CanonicalYouTubeContent.kt`
```kotlin
package com.safarparmar.app.feature.youtube.detector

import com.safarparmar.app.feature.youtube.policy.ChannelIdentity
import com.safarparmar.app.feature.youtube.policy.VideoFingerprint

enum class ScreenType {
    WATCH,
    SHORTS,
    CHANNEL_PAGE,
    FEED,
    UNKNOWN,
}

enum class DetectionConfidence {
    HIGH,       // Verified @handle from UI + title matching MediaSession, or structural watch metadata
    MEDIUM,     // Channel displayName matching, or MediaSession title+artist without UI handle
    LOW,        // Partial metadata during video transition
    UNKNOWN,    // Unparseable / empty tree
}

enum class DetectionSource {
    ACCESSIBILITY_UI,
    MEDIA_SESSION,
    FUSED,
    CLICK_INTERCEPT,
}

data class CanonicalYouTubeContent(
    val fingerprint: VideoFingerprint,
    val channelIdentity: ChannelIdentity,
    val title: String,
    val isShorts: Boolean,
    val isAd: Boolean,
    val confidence: DetectionConfidence,
    val source: DetectionSource,
    val generationToken: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        val EMPTY = CanonicalYouTubeContent(
            fingerprint = VideoFingerprint.EMPTY,
            channelIdentity = ChannelIdentity(),
            title = "",
            isShorts = false,
            isAd = false,
            confidence = DetectionConfidence.UNKNOWN,
            source = DetectionSource.ACCESSIBILITY_UI,
        )
    }
}
```

#### `YouTubeUiParser.kt` (Structural Subtree Parser)
```kotlin
package com.safarparmar.app.feature.youtube.detector

import android.view.accessibility.AccessibilityNodeInfo

data class ParsedUiSnapshot(
    val screenType: ScreenType,
    val title: String = "",
    val channelIdentity: ChannelIdentity = ChannelIdentity(),
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
    val hasActivePlayer: Boolean = false,
    val confidence: DetectionConfidence = DetectionConfidence.UNKNOWN,
)

object YouTubeUiParser {
    private const val TAG = "YouTubeUiParser"
    private const val YT_PKG = "com.google.android.youtube:id/"

    // Structural container IDs
    private val WATCH_METADATA_CONTAINERS = setOf(
        "${YT_PKG}watch_metadata",
        "${YT_PKG}video_owner",
        "${YT_PKG}owner_row",
        "${YT_PKG}watch_header",
        "${YT_PKG}metadata_layout",
    )

    private val PLAYER_CONTAINERS = setOf(
        "${YT_PKG}watch_player",
        "${YT_PKG}movie_player",
        "${YT_PKG}fullscreen_player",
        "${YT_PKG}player_fragment_container",
    )

    private val SHORTS_CONTAINERS = setOf(
        "${YT_PKG}reel_watch_player",
        "${YT_PKG}reel_player_page",
        "${YT_PKG}reel_scrim_shorts",
    )

    private val CHANNEL_HEADER_CONTAINERS = setOf(
        "${YT_PKG}channel_header",
        "${YT_PKG}channel_page_header",
        "${YT_PKG}c3_channel_header",
    )

    // Subtree exclusion list (ignore recommendations and comments)
    private val EXCLUDED_SUBTREES = setOf(
        "${YT_PKG}watch_list",
        "${YT_PKG}related_videos",
        "${YT_PKG}results_list",
        "${YT_PKG}comments_entry_point_layout",
        "${YT_PKG}comment_thread",
    )

    fun parse(root: AccessibilityNodeInfo?): ParsedUiSnapshot {
        if (root == null) return ParsedUiSnapshot(ScreenType.UNKNOWN)

        // 1. Identify ScreenType & Check for Shorts
        val isShorts = detectShortsStructurally(root)
        if (isShorts) {
            return ParsedUiSnapshot(
                screenType = ScreenType.SHORTS,
                isShorts = true,
                confidence = DetectionConfidence.HIGH,
            )
        }

        // 2. Locate Active Player Subtree
        val playerNode = findSubtree(root, PLAYER_CONTAINERS)
        val isAd = if (playerNode != null) detectInStreamAd(playerNode) else false
        val hasActivePlayer = playerNode != null

        // 3. Locate Watch Metadata Subtree
        val metadataNode = findSubtree(root, WATCH_METADATA_CONTAINERS)
        if (metadataNode != null) {
            val title = extractTitleFromSubtree(metadataNode)
            val identity = extractChannelIdentityFromSubtree(metadataNode)
            val confidence = when {
                identity.verifiedHandle != null && title.isNotBlank() -> DetectionConfidence.HIGH
                identity.displayName.isNotBlank() -> DetectionConfidence.MEDIUM
                title.isNotBlank() -> DetectionConfidence.LOW
                else -> DetectionConfidence.UNKNOWN
            }
            return ParsedUiSnapshot(
                screenType = ScreenType.WATCH,
                title = title,
                channelIdentity = identity,
                isShorts = false,
                isAd = isAd,
                hasActivePlayer = hasActivePlayer,
                confidence = confidence,
            )
        }

        // 4. Locate Channel Header Subtree (Channel Profile Pages)
        val channelHeaderNode = findSubtree(root, CHANNEL_HEADER_CONTAINERS)
        if (channelHeaderNode != null) {
            val identity = extractChannelIdentityFromSubtree(channelHeaderNode)
            return ParsedUiSnapshot(
                screenType = ScreenType.CHANNEL_PAGE,
                channelIdentity = identity,
                isShorts = false,
                isAd = false,
                hasActivePlayer = false,
                confidence = if (identity.isResolvable) DetectionConfidence.HIGH else DetectionConfidence.MEDIUM,
            )
        }

        // Fallback for feeds/home/search
        return ParsedUiSnapshot(
            screenType = if (hasActivePlayer) ScreenType.WATCH else ScreenType.FEED,
            isAd = isAd,
            hasActivePlayer = hasActivePlayer,
        )
    }

    private fun findSubtree(node: AccessibilityNodeInfo, targetIds: Set<String>): AccessibilityNodeInfo? {
        val id = node.viewIdResourceName
        if (id in targetIds) return node
        if (id in EXCLUDED_SUBTREES) return null // Prune excluded subtrees immediately

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSubtree(child, targetIds)
            if (result != null) return result
        }
        return null
    }

    private fun detectShortsStructurally(root: AccessibilityNodeInfo): Boolean {
        return findSubtree(root, SHORTS_CONTAINERS) != null
    }

    private fun detectInStreamAd(playerNode: AccessibilityNodeInfo): Boolean {
        // Scoped strictly to PlayerSubtree
        ...
    }

    private fun extractTitleFromSubtree(metadataNode: AccessibilityNodeInfo): String { ... }
    private fun extractChannelIdentityFromSubtree(metadataNode: AccessibilityNodeInfo): ChannelIdentity { ... }
}
```

#### `YouTubeContentDetector.kt` (Unified Detection & Fusion Layer)
```kotlin
package com.safarparmar.app.feature.youtube.detector

import com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeContentDetector @Inject constructor(
    private val policyEngine: YouTubePolicyEngine,
    private val scope: CoroutineScope,
) {
    private val currentGeneration = AtomicLong(0L)
    private val eventChannel = Channel<DetectionEvent>(capacity = Channel.BUFFERED)

    private var latestUiSnapshot: ParsedUiSnapshot? = null
    private var latestMediaSnapshot: MediaMetadataSnapshot? = null
    private var activeFingerprint: VideoFingerprint = VideoFingerprint.EMPTY

    init {
        scope.launch {
            eventChannel.receiveAsFlow()
                .collect { event ->
                    handleEvent(event)
                }
        }
    }

    fun onAccessibilitySnapshot(snapshot: ParsedUiSnapshot) {
        eventChannel.trySend(DetectionEvent.UiUpdate(snapshot))
    }

    fun onMediaMetadata(metadata: MediaMetadataSnapshot) {
        eventChannel.trySend(DetectionEvent.MediaUpdate(metadata))
    }

    fun onShortsClickIntercepted() {
        val gen = currentGeneration.incrementAndGet()
        // Priority bypass: dispatch immediately
        policyEngine.onShortsIntercepted(gen)
    }

    fun onSuggestedVideoClicked() {
        val gen = currentGeneration.incrementAndGet()
        policyEngine.onVideoSwitchInitiated(gen)
    }

    private fun handleEvent(event: DetectionEvent) {
        val gen = currentGeneration.get()
        val fused = fuse(event, gen)
        if (fused != null) {
            policyEngine.onContentDetected(fused)
        }
    }

    private fun fuse(event: DetectionEvent, generationToken: Long): CanonicalYouTubeContent? {
        ...
    }
}
```

---

## 5. Verification Method

### 5.1 Static Compilation & Gradle Test Execution
Run test suite verifying current passes and baseline:
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testProdDebugUnitTest --tests "com.safarparmar.app.feature.youtube.*"
```

### 5.2 Unit Test Verification Plan for Implementers
1. **`YouTubeUiParserTest`**:
   - Verify `WatchMetadataSubtree` extraction excludes suggested videos in `related_videos`.
   - Verify `@handle` references in user comments do NOT pollute video channel identity.
   - Verify fake handles are NEVER synthesized when only `displayName` is present.
   - Verify `ShortsPlayerSubtree` is detected accurately via structural markers.
   - Verify in-stream ads are detected strictly within the `PlayerSubtree`.
2. **`YouTubeContentDetectorTest`**:
   - Verify event serialization processes rapid UI updates sequentially without races.
   - Verify fusion reconciles Accessibility verified `@handle` with MediaSession metadata.
   - Verify fullscreen mode falls back cleanly to MediaSession without emitting playback stopped.
   - Verify `ShortsClickIntercepted` produces immediate `ShortsBlocked` state.
3. **`YouTubePolicyEngineTest`**:
   - Verify generation token discards stale Room DB query results.
   - Verify pre-roll ad on new video fingerprint stays in `PolicyState.Verifying`.
   - Verify mid-roll ad on existing video retains previous verified policy.
   - Verify advertiser name/handle is NEVER saved to repository or adopted as channel identity.

### 5.3 Invalidation Conditions
- Any occurrence of synthetic handle generation (e.g. `@` prepended to display name).
- Any occurrence where an ad on a new video fingerprint evaluates to `Allowed` due to prior video session.
- Any regression where existing 79+ E2E tests fail.

