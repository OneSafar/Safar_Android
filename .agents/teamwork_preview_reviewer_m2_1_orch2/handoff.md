# Milestone 2 Review & Adversarial Challenge Report

## Review Summary

**Verdict**: **REQUEST_CHANGES**

Milestone 2 implementation exhibits commendable architecture: genuine Room database lookups, generation tokens with active job cancellation to protect against race conditions, fail-closed `Verifying` overlay state with pointer-event consumption, and clean distinction between mid-roll ad policy retention and pre-roll ad switches. Zero integrity violations were found (no hardcoded test hacks, no facade implementations, all 338 unit tests pass genuinely).

However, an adversarial trace identified a **Major Correctness Defect in `YouTubeGuardService.kt:134-137`**: `PolicyState.Verifying` unconditionally routes to `overlayManager.showVerifying(...)` even when `state.identity == null`. Because `YouTubePolicyEngine` initializes to `PolicyState.Verifying(null)` and resets to `PolicyState.Verifying(null)` on `onPlaybackStopped()`, the service attaches a full-screen, touch-consuming overlay on cold service start and re-shows it whenever video playback stops, locking the user out of the YouTube Home/browse feed.

---

## Findings

### [Major] Finding 1: Unconditional `showVerifying` on `PolicyState.Verifying(null)` Locks Out Home / Browse Feed

- **What**: When `YouTubePolicyEngine.state` is `PolicyState.Verifying(identity = null)` (the initial engine state on cold start and the state set by `onPlaybackStopped()`), `YouTubeGuardService.handlePolicyState()` unconditionally calls `overlayManager.showVerifying("", "")`. This attaches a full-screen, touch-consuming blocking overlay over the YouTube Home and Search feeds when no video is playing.
- **Where**: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt:134-137`
  ```kotlin
  is PolicyState.Verifying -> overlayManager.showVerifying(
      channelName = state.identity?.channelName.orEmpty(),
      title = state.identity?.title.orEmpty(),
  )
  ```
- **Why**: The guard overlay should ONLY cover the screen when an active video or Shorts is being verified (`state.identity != null`). When `state.identity == null`, YouTube is idle (e.g. on home feed, library, search, or video closed). Showing the overlay with `Modifier.pointerInput` at that time consumes all touches and makes the YouTube app completely unusable before any video is even selected. Furthermore, in `YouTubeGuardService.kt:92-93`, when the user is on the home feed, the service calls `policyEngine.onPlaybackStopped()` followed by `overlayManager.hide()`, but `onPlaybackStopped()` emits `PolicyState.Verifying(null)`, which immediately triggers `handlePolicyState()` to re-invoke `overlayManager.showVerifying("", "")`, neutralizing the `hide()` call.
- **Suggestion**: Gate `showVerifying` on `state.identity != null`. When `state.identity == null`, invoke `overlayManager.hide()`:
  ```kotlin
  is PolicyState.Verifying -> {
      if (state.identity != null) {
          overlayManager.showVerifying(
              channelName = state.identity.channelName,
              title = state.identity.title,
          )
      } else {
          overlayManager.hide()
      }
  }
  ```

### [Minor] Finding 2: `AskUserContent` Lacks an Explicit Exit/Back Button

- **Where**: `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlay.kt:199-260`
- **What**: `BlockedContent` and `ShortsBlockedContent` both provide explicit "Go Back" / "Exit Shorts" buttons bound to `state.onBack`, whereas `AskUserContent` only offers "Distracting" and "Productive" options.
- **Why**: While `OverlayState.AskUser` contains `val onBack: () -> Unit = {}`, `AskUserContent` does not render a button for users who do not wish to classify the channel right now and prefer to navigate away.
- **Suggestion**: Add a secondary "Cancel / Go Back" text button calling `state.onBack`.

### [Minor] Finding 3: Misleading Comment Regarding External Package Events in `YouTubeGuardService`

- **Where**: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt:63-66`
- **What**: Code states:
  ```kotlin
  // If an event occurs outside YouTube, hide the overlay and stop verification
  if (event.packageName?.toString() != YOUTUBE_PACKAGE) {
      return
  }
  ```
- **Why**: The code only returns early; it does not hide the overlay or cancel detection as the comment claims. (Note: Foreground lifecycle detection is slated for Milestone 5).
- **Suggestion**: Update comment or align with Milestone 5 plan.

---

## Adversarial Challenge & Stress-Testing

### Challenge Summary
**Overall risk assessment**: **MEDIUM** (Functional blocker on idle home feed due to Finding 1; state machine and concurrency logic are otherwise high quality and robust).

### Stress Test Scenarios & Results

1. **Scenario: Cold App Launch / Home Feed Navigation**
   - *Attack*: Accessibility service connects while user is on YouTube Home feed. `policyEngine.state` emits initial `PolicyState.Verifying(null)`.
   - *Expected*: No overlay attached; user can interact with home feed.
   - *Observed*: Overlay attaches with `Verifying("", "")` and blocks all touches.
   - *Result*: **FAIL** (See Finding 1).

2. **Scenario: Rapid Video Hopping Race Condition (Out-of-Order DB Resolution)**
   - *Attack*: Video 1 (slow 500ms lookup, Allowed) immediately followed 100ms later by Video 2 (fast 0ms lookup, Blocked).
   - *Expected*: Video 2 resolves to Blocked. Slow Video 1 lookup never overwrites state to Allowed.
   - *Observed*: `activeLookupJob?.cancel()` and `currentGeneration.incrementAndGet()` discard the stale generation `gen != currentGeneration.get()`.
   - *Result*: **PASS**.

3. **Scenario: Mid-Roll Ad vs. Pre-Roll Ad Transition**
   - *Attack*: (a) Mid-roll ad begins during productive session; (b) New video switches directly into pre-roll ad.
   - *Expected*: (a) Retains Allowed without querying advertiser handle; (b) Fails closed in Verifying state without inheriting prior video's Allowed policy.
   - *Observed*: `identity.isAd` checks `isNewVideoSwitch || (explicitFingerprint != lastVerifiedFingerprint) || lastVerifiedPolicy == null` and stays fail-closed in Verifying for pre-roll ads, while retaining policy for mid-roll ads.
   - *Result*: **PASS**.

4. **Scenario: Shorts Bypass via Classification / Sponsor**
   - *Attack*: User attempts to classify Shorts channel or Short is marked as an Ad (`isAd = true`).
   - *Expected*: Shorts unconditionally blocked with `PolicyState.ShortsBlocked`. Never emits `Allowed` or `AskUser`.
   - *Observed*: Shorts check precedes ad and DB lookups; `onUserClassified` explicitly guards `!current.isShorts`.
   - *Result*: **PASS**.

5. **Scenario: Touch Leakage through Verifying Scrim**
   - *Attack*: Tap repeatedly on YouTube player controls through the `OverlayState.Verifying` screen.
   - *Expected*: Pointer events completely consumed by the overlay.
   - *Observed*: `Modifier.pointerInput(Unit) { awaitPointerEventScope { while(true) awaitPointerEvent().changes.forEach { it.consume() } } }` on the root Box consumes all pointer changes.
   - *Result*: **PASS**.

---

## Integrity Assessment

- **Hardcoded Test Checks**: Verified clean. No test titles, IDs, or synthetic bypasses exist in production sources.
- **Facade Implementations**: None. `YouTubeGuardOverlayManager` genuinely attaches a `ComposeView` with `SimpleLifecycleOwner` to `WindowManager` via `TYPE_ACCESSIBILITY_OVERLAY`. `YouTubePolicyEngine` executes real coroutine-based Room queries.
- **Bypassed Logic**: None.
- **Verification Integrity**: All 338 unit tests run genuinely and pass.

---

## 5-Component Handoff Report

### 1. Observation
- `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt:134-137`:
  ```kotlin
  is PolicyState.Verifying -> overlayManager.showVerifying(
      channelName = state.identity?.channelName.orEmpty(),
      title = state.identity?.title.orEmpty(),
  )
  ```
- `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt:46`:
  ```kotlin
  private val _state = MutableStateFlow<PolicyState>(PolicyState.Verifying(null))
  ```
- `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt:256`:
  ```kotlin
  _state.value = PolicyState.Verifying(null)
  ```
- Independent test execution output:
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`
  - Total test XML files: 34
  - Total unit tests executed: 338
  - Failures: 0, Errors: 0, Skipped: 0
  - YouTube feature tests: 41 in `YouTubePolicyEngineTest`, 6 in `YouTubeGuardOverlayManagerTest`, 79 in `YouTubeGuardE2ETest`.

### 2. Logic Chain
1. When `YouTubeGuardService` is connected, `observePolicyState()` begins collecting `policyEngine.state`.
2. The initial value is `PolicyState.Verifying(identity = null)`.
3. `handlePolicyState()` receives `PolicyState.Verifying(null)` and calls `overlayManager.showVerifying("", "")`.
4. `overlayManager.showVerifying()` attaches the overlay view (`TYPE_ACCESSIBILITY_OVERLAY`) with full-screen `Color(0xF50D131A)` background and pointer-event consumption.
5. Because the user is on the YouTube Home screen and no video is playing, no metadata arrives to change the policy. The user is locked out by an un-dismissible "Verifying Content..." scrim.
6. When playback stops, `onPlaybackStopped()` sets `_state.value = PolicyState.Verifying(null)`, reproducing the same issue.

### 3. Caveats
- Structural Subtree Parsing (Phase 3 & 7) and MediaSession Fusion Layer (Phase 4 & 8) are scheduled for Milestones 3 & 4.
- Lifecycle foreground tracking via global window state (Phase 9) is scheduled for Milestone 5.

### 4. Conclusion
Milestone 2 delivers solid foundation for fail-closed blocking, generation tokens, ad policy retention, and touch-consuming overlay rendering. However, because `YouTubeGuardService` unconditionally displays the verifying overlay when `state.identity == null`, this causes a severe UX lockout bug on cold starts and home feed browsing.
**Verdict**: **REQUEST_CHANGES** to fix Finding 1.

### 5. Verification Method
1. Inspect `YouTubeGuardService.kt:134-137` and confirm `overlayManager.showVerifying` is only called when `state.identity != null`, and `overlayManager.hide()` is called when `state.identity == null`.
2. Run the test suite:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.*"
   ```
   Confirm all tests compile and pass.
