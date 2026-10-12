# Challenger Report: Milestone 2 — Fail-Closed Policy Engine & Verifying Overlay

**Verdict**: `APPROVE`  
**Overall Risk Assessment**: `LOW`

---

## 1. Observation

### 1.1 Direct Source Code Observations
1. **Generation Tokens & Concurrency Protection**:
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt:49-51`:
     ```kotlin
     private val currentGeneration = AtomicLong(0L)
     private var activeLookupJob: Job? = null
     ```
   - In `YouTubePolicyEngine.kt:87-88`:
     ```kotlin
     activeLookupJob?.cancel()
     val gen = currentGeneration.incrementAndGet()
     ```
   - In `YouTubePolicyEngine.kt:113-114`:
     ```kotlin
     if (gen != currentGeneration.get()) return
     ```
   - In `YouTubePolicyEngine.kt:177-178`:
     ```kotlin
     // Discard stale DB lookup if generation token changed while suspending
     if (gen != currentGeneration.get()) return
     ```
   - Every lookup job launched by `onVideoDetected` receives a monotonically increasing token `gen`. Pending jobs are cancelled, and if an asynchronous coroutine completes while suspended during Room I/O, `if (gen != currentGeneration.get()) return` discards the stale DB lookup result before state mutation can occur.

2. **Ad Policy Retention vs Video Switch**:
   - In `YouTubePolicyEngine.kt:93-98`:
     ```kotlin
     // When switching to a new non-ad video, or explicit video switch, clear verified session policy
     if (!identity.isAd || identity.isNewVideoSwitch) {
         lastVerifiedPolicy = null
         lastVerifiedChannel = null
         lastVerifiedFingerprint = null
     }
     ```
   - In `YouTubePolicyEngine.kt:127-136`:
     ```kotlin
     val isNewSwitch = identity.isNewVideoSwitch ||
         (identity.explicitFingerprint != null &&
             lastVerifiedFingerprint != null &&
             identity.explicitFingerprint.key.isNotEmpty() &&
             identity.explicitFingerprint.key != lastVerifiedFingerprint?.key)

     if (isNewSwitch || lastVerifiedPolicy == null) {
         // Pre-roll ad on new video switch or cold start: fail-closed, stay in Verifying
         return
     }
     ```
   - Mid-roll ads during an active session retain `Allowed` (if productive) or `Blocked` (if distracting) without querying Room or exposing advertiser handles.
   - Pre-roll ads on video switch (`isNewVideoSwitch == true` or distinct `explicitFingerprint`) or cold start (`lastVerifiedPolicy == null`) remain fail-closed in `PolicyState.Verifying`.

3. **Touch Consumption Invariant**:
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlay.kt:33-41`:
     ```kotlin
     Box(
         modifier = Modifier
             .fillMaxSize()
             .background(Color(0xF50D131A))
             .pointerInput(Unit) {
                 awaitPointerEventScope {
                     while (true) {
                         awaitPointerEvent().changes.forEach { it.consume() }
                     }
                 }
             },
         contentAlignment = Alignment.Center,
     )
     ```
   - The root full-screen `Box` consumes pointer events via `awaitPointerEvent().changes.forEach { it.consume() }`. Because `WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE` is omitted, the `TYPE_ACCESSIBILITY_OVERLAY` window receives touch events and marks them consumed, preventing any touch passthrough to underlying video player controls during `Verifying`, `Blocked`, and `ShortsBlocked`.

4. **Fail-Closed Service Routing**:
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt:134-137`:
     ```kotlin
     is PolicyState.Verifying -> overlayManager.showVerifying(
         channelName = state.identity?.channelName.orEmpty(),
         title = state.identity?.title.orEmpty(),
     )
     ```
   - In `YouTubeGuardService.kt:132`:
     ```kotlin
     is PolicyState.Allowed -> overlayManager.hide()
     ```
   - Every video identity change immediately emits `PolicyState.Verifying` synchronously before any asynchronous lookup begins, causing the service to immediately display `OverlayState.Verifying`.

### 1.2 Independent Empirical Test Execution
Created adversarial test suite in `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineChallengerTest.kt` (17 tests) and ran the full unit test suite:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest
```
Result:
- **Total Tests Executed**: 355 tests
- **Failures**: 0
- **Errors**: 0
- **Skipped / Ignored**: 0
- **Status**: `BUILD SUCCESSFUL`

---

## 2. Logic Chain

1. **Stale Lookup & Concurrency Invariance**:
   - *Observation*: Monotonic generation tokens are incremented via `currentGeneration.incrementAndGet()` on every `onVideoDetected`, `onUserClassified`, and `onPlaybackStopped`.
   - *Reasoning*: Suppose Video A (500ms delay) is followed by Video B (10ms delay). Video A is assigned Gen 1; Video B is assigned Gen 2. Video A's coroutine job is cancelled. If Video A finishes suspending after Video B completes, line 178 checks `gen != currentGeneration.get()` (1 != 2) and exits immediately.
   - *Empirical Verification*: Confirmed by `challenger - rapid out-of-order lookup sequence strictly honors newest generation token` and `challenger - rapid flapping between two videos preserves latest token and policy`. Stale lookups never overwrite current video policy.

2. **Ad Boundary Security**:
   - *Observation*: On non-ad videos, `lastVerifiedPolicy` is reset to null immediately. On ad videos, `lastVerifiedPolicy` is inspected.
   - *Reasoning*: A mid-roll ad shares the playback session of the parent video, so retaining `Allowed` or `Blocked` ensures consistent policy without evaluating the advertiser's sponsored handle. If the user clicks a new video, the transition clears `lastVerifiedPolicy`, ensuring any subsequent pre-roll ad cannot inherit the prior video's `Allowed` state and remains in `PolicyState.Verifying`.
   - *Empirical Verification*: Confirmed by `challenger - mid-roll ad on PRODUCTIVE video retains Allowed without advertiser DB lookup`, `challenger - mid-roll ad on DISTRACTING video retains Blocked`, `challenger - ad pod with 3 consecutive ads retains verified policy throughout`, and `challenger - pre-roll ad on video switch with distinct fingerprint stays in Verifying`.

3. **Touch Inaccessibility & User Safety**:
   - *Observation*: The root `Box` in `YouTubeGuardOverlay` spans the full screen (`fillMaxSize`) and attaches a pointer input blocker that consumes all touch events.
   - *Reasoning*: Because the overlay window is `TYPE_ACCESSIBILITY_OVERLAY` without `FLAG_NOT_TOUCHABLE`, touch events are routed directly to the Compose view. Consuming pointer changes prevents the underlying YouTube player or recommendation feed from receiving click or drag gestures while content verification is in progress.
   - *Empirical Verification*: Confirmed by `challenger - overlay manager transitions through all required blocking and verifying states` and code inspection of Compose pointer pass hierarchy.

---

## 3. Adversarial Challenge Analysis

### Challenge 1: Out-of-Order Delayed DB Resolutions
- **Assumption challenged**: Video lookup responses always return in the order requests were dispatched.
- **Attack scenario**: 5 consecutive video hops with reverse-order latency (Video 1 = 300ms, Video 5 = 20ms).
- **Blast radius**: If stale lookups could commit, a productive video could unblock a distracting video or vice-versa.
- **Mitigation verified**: `AtomicLong` generation tokens discard all stale lookups (`gen != currentGeneration.get()`).
- **Result**: PASS.

### Challenge 2: Ad Policy Leakage Across Videos
- **Assumption challenged**: Advertisements only occur mid-stream during verified videos.
- **Attack scenario**: User is watching an Allowed video; switches to a new video that begins with a pre-roll ad.
- **Blast radius**: Distracting content or unclassified channels could play freely under the guise of an ad.
- **Mitigation verified**: Switching to a new video or setting `isNewVideoSwitch = true` or changing `explicitFingerprint` resets `lastVerifiedPolicy = null`, forcing pre-roll ads to remain in `PolicyState.Verifying`.
- **Result**: PASS.

### Challenge 3: Stale User Classification on New Video
- **Assumption challenged**: User only clicks classification dialog while the original video is still active.
- **Attack scenario**: Video 1 prompts `AskUser`. User swipes to Video 2 (Distracting) which resolves to `Blocked`. User then taps "Productive" on the old dialog for Video 1.
- **Blast radius**: Video 2 could be erroneously unblocked as `Allowed`.
- **Mitigation verified**: In `onUserClassified`, `matchesCurrent` validates the handle against `currentIdentity`. Since Video 2 does not match Video 1, the repository updates Video 1's policy in Room, but Video 2 remains `Blocked`.
- **Result**: PASS (`challenger - classifying old channel after switching to new video does not alter active video policy`).

### Stress Test Results Summary

| Scenario | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|
| Rapid 5-video switch with reverse DB delays | Only Video 5 policy commits | Only Video 5 commits (`Allowed`) | **PASS** |
| Rapid 2-video flapping with 200ms delay | Latest video state commits | Latest video (`Blocked`) commits | **PASS** |
| Delayed lookup after `onPlaybackStopped` | Remains `Verifying(null)` | Remains `Verifying(null)` | **PASS** |
| Mid-roll ad during Productive video | Retains `Allowed`, DB not queried | Retains `Allowed`, 0 queries | **PASS** |
| Mid-roll ad during Distracting video | Retains `Blocked`, DB not queried | Retains `Blocked`, 0 queries | **PASS** |
| Ad pod (3 consecutive ads) | All 3 retain `Allowed` | All 3 retain `Allowed` | **PASS** |
| Cold start directly into Ad | Stays in `Verifying(identity)` | Stays in `Verifying(identity)` | **PASS** |
| Pre-roll ad with new fingerprint | Stays in `Verifying` | Stays in `Verifying` | **PASS** |
| Pre-roll ad with `isNewVideoSwitch=true` | Stays in `Verifying` | Stays in `Verifying` | **PASS** |
| Sponsored Shorts (`isAd=true`) | Blocked as `ShortsBlocked` | Resolves to `ShortsBlocked` | **PASS** |
| Autoplay: Allowed -> Unknown | `Verifying` -> `AskUser` | `Verifying` -> `AskUser` | **PASS** |
| Autoplay: Allowed -> Distracting | `Verifying` -> `Blocked` | `Verifying` -> `Blocked` | **PASS** |
| Autoplay: Allowed -> Shorts | `Verifying` -> `ShortsBlocked` | `Verifying` -> `ShortsBlocked` | **PASS** |
| OverlayManager lifecycle transitions | Transitions match all states | Exact state transitions observed | **PASS** |
| Canonical identity without handle | Resolves via canonical key without fake handle | Resolved via `name:<channel>` | **PASS** |
| Classification of old video | Persists policy, does not unblock new video | Room updated, state stays `Blocked` | **PASS** |
| Rapid Shorts vs normal video hopping | Shorts always blocked immediately | Shorts blocked on each occurrence | **PASS** |

---

## 4. Caveats

- **Structural Subtree Node Extraction**: Subtree parsing (watch metadata vs comments vs suggested videos) is scheduled for Milestone 3.
- **Unified Fusion Engine & MediaSession Wiring**: Sequential event streaming via Kotlin Channel is scheduled for Milestone 4.
- No other caveats.

---

## 5. Conclusion

**Verdict: `APPROVE`**

Milestone 2 implementation satisfies all security invariants and acceptance criteria:
1. Generation tokens prevent race conditions and stale DB query overwrites during rapid video navigation.
2. Ad policy retention preserves verified policy for mid-roll ads and strictly isolates new video switches into fail-closed `OverlayState.Verifying`.
3. Overlay manager and Compose overlay enforce fail-closed verification and pointer touch consumption.
4. All 355 unit tests (including 17 adversarial stress tests) pass with zero errors.

Milestone 2 is ready for integration into downstream milestones.

---

## 6. Verification Method

To independently verify this evaluation:

1. **Execute All Unit Tests (355 tests)**:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew :app:testQaDebugUnitTest
   ```
   Confirm `BUILD SUCCESSFUL` with 355 passing tests and 0 failures.

2. **Execute Challenger Suite**:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineChallengerTest"
   ```
   Confirm all 17 adversarial tests pass.

3. **Inspect Implementation & Test Files**:
   - Implementation: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`
   - Overlay UI: `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlay.kt`
   - Overlay Manager: `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlayManager.kt`
   - Service: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`
   - Challenger Test: `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineChallengerTest.kt`
