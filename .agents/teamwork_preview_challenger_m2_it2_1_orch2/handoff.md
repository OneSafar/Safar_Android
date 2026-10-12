# Adversarial Challenger Report: Milestone 2 (Iteration 2)

**Verdict**: `APPROVE`  
**Overall Risk Assessment**: `LOW`

---

## 1. Observation

### 1.1 Remediated Implementation Observations
In `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt:130-146`:
```kotlin
    @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.PRIVATE)
    internal fun handlePolicyState(state: PolicyState) {
        Log.d(TAG, "Policy state → $state")
        when (state) {
            is PolicyState.Allowed -> overlayManager.hide()

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
- In `YouTubeGuardService.kt:90-95`:
```kotlin
            // If no video or Shorts is detected, user is in home feed, library, or search
            if (snapshot.channelHandle.isBlank() && snapshot.channelName.isBlank() && !snapshot.isShorts) {
                Log.d(TAG, "No video or Shorts active — stopping playback policy")
                policyEngine.onPlaybackStopped()
                overlayManager.hide()
                return@launch
            }
```
- In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt:46, 100-101, 256`:
  - `private val _state = MutableStateFlow<PolicyState>(PolicyState.Verifying(null))` (initial state on cold start)
  - `_state.value = PolicyState.Verifying(identity)` (synchronously emitted on active video detection)
  - `_state.value = PolicyState.Verifying(null)` (synchronously emitted on `onPlaybackStopped()`)

### 1.2 Independent Empirical Verification
Authored dedicated adversarial challenge suite in `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubeHomeFeedLockoutChallengerTest.kt` consisting of 18 stress tests covering:
1. Cold start launch transitions (`PolicyState.Verifying(null)`)
2. Repeated idle Home feed events keeping overlay strictly `Hidden`
3. Active video transitions enforcing immediate fail-closed `OverlayState.Verifying` before resolving to `Allowed`, `Blocked`, or `AskUser`
4. Navigation back to Home feed from `Allowed`, `Blocked`, and `ShortsBlocked` immediately clearing overlays
5. Stale asynchronous DB query cancellation/discard preventing delayed popups on the Home feed
6. Rapid flapping (10 cycles of Video -> Home -> Video -> Home) verifying state consistency
7. Pre-roll and mid-roll ad exits to Home feed
8. Empty field identities preserving fail-closed verifying overlay

Ran full project unit test suite:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest
```
**Results**:
- Total test XML suites: 37
- Total unit tests executed: 383
- Total failures: 0
- Total errors: 0
- Total skipped: 0
- Status: `BUILD SUCCESSFUL`

---

## 2. Logic Chain

1. **Cold Start & Idle Feeds Unblocked**:
   - *Observation*: `YouTubePolicyEngine` initializes with `_state = MutableStateFlow(PolicyState.Verifying(null))`. When `YouTubeGuardService` connects, `handlePolicyState(state)` is triggered with `identity = null`.
   - *Reasoning*: Because line 143 explicitly invokes `overlayManager.hide()` when `state.identity == null`, no window view is attached to `WindowManager` on cold start. The YouTube home feed, subscriptions, and search screens remain 100% interactable without touch interception.
   - *Empirical Verification*: Confirmed by `challenger - cold start initializes in Hidden overlay state with zero lockout on home feed` and `challenger - repeated home feed idle events keep overlay strictly Hidden`.

2. **Active Video Fail-Closed Invariant Maintained**:
   - *Observation*: When a video or Shorts is detected, `policyEngine.onVideoDetected(identity)` is called with a non-null `VideoIdentity`. Line 101 synchronously sets `_state.value = PolicyState.Verifying(identity)`.
   - *Reasoning*: Because `state.identity != null`, line 137 invokes `overlayManager.showVerifying(channelName, title)`. The full-screen overlay attaches with pointer event consumption before any DB lookup coroutine executes.
   - *Empirical Verification*: Confirmed by `challenger - launching productive video enforces Verifying overlay before resolving to Hidden`, `challenger - launching distracting video enforces Verifying overlay before resolving to Blocked`, and `challenger - empty fields in VideoIdentity still enforce fail-closed Verifying overlay`.

3. **Clean Teardown on Return to Home Feed**:
   - *Observation*: When the user exits a video (or Shorts), `onPlaybackStopped()` advances the generation token and emits `PolicyState.Verifying(null)`.
   - *Reasoning*: `handlePolicyState` receives `identity = null` and immediately invokes `overlayManager.hide()`.
   - *Empirical Verification*: Confirmed by `challenger - navigating from Allowed video back to Home feed hides overlay immediately`, `challenger - navigating from Blocked video back to Home feed immediately dismisses blocked overlay`, and `challenger - navigating from Shorts back to Home feed immediately dismisses shorts overlay`.

4. **Stale Query Discard & Race Resistance**:
   - *Observation*: `YouTubePolicyEngine` uses `AtomicLong` generation tokens (`currentGeneration.incrementAndGet()`).
   - *Reasoning*: If a user launches a video with a slow DB query (e.g. 300ms) and navigates back to the Home feed at 50ms, `onPlaybackStopped()` increments `currentGeneration`. When the slow query finishes at 300ms, `if (gen != currentGeneration.get()) return` drops the result.
   - *Empirical Verification*: Confirmed by `challenger - slow DB lookup finishing after user returns to Home feed is discarded and does not hijack screen` and `challenger - slow AskUser DB lookup finishing after playback stopped does not show classification popup on home feed`.

---

## 3. Adversarial Challenge Analysis

### Challenge 1: Home Feed Lockout on Cold Start / Navigation
- **Assumption challenged**: YouTube service cannot distinguish between an unverified video and an idle home feed when both produce `PolicyState.Verifying`.
- **Attack scenario**: Cold app start or closing video; accessibility service receives initial engine state.
- **Blast radius**: Complete app un-usability if overlay covers home feed.
- **Mitigation verified**: Gating `if (state.identity != null)` in `YouTubeGuardService.kt:136` routes `identity == null` directly to `overlayManager.hide()`.
- **Stress Test Result**: `PASS`.

### Challenge 2: Touch Passthrough Window on Active Video Detection
- **Assumption challenged**: Gating `showVerifying` on `state.identity != null` might delay overlay attachment and allow touch leakage into distracting videos.
- **Attack scenario**: User taps on a distracting video from the home feed; taps rapidly during the first 50ms before Room DB lookup completes.
- **Blast radius**: User could interact with distracting video controls or autoplay before verification finishes.
- **Mitigation verified**: `onVideoDetected` sets `_state.value = PolicyState.Verifying(identity)` synchronously BEFORE launching the asynchronous `resolvePolicy` coroutine. `OverlayState.Verifying` with pointer event consumption is immediately displayed.
- **Stress Test Result**: `PASS`.

### Challenge 3: Rapid Video-to-Home Flapping
- **Assumption challenged**: Rapidly alternating between video playback and home feed could lead to desynchronized overlay states or leaked scrims.
- **Attack scenario**: 10 back-to-back cycles of `onVideoDetected` followed by `onPlaybackStopped`.
- **Blast radius**: Lingering blocked overlay on home feed or missing verifying overlay on active video.
- **Mitigation verified**: Every stop transition immediately results in `OverlayState.Hidden`; every video transition immediately enters `OverlayState.Verifying` before resolving.
- **Stress Test Result**: `PASS`.

### Stress Test Results Summary

| Scenario | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|
| Cold launch on Home feed | Overlay remains `Hidden` | `OverlayState.Hidden` | **PASS** |
| Repeated idle home feed scrolling | Overlay remains `Hidden` | `OverlayState.Hidden` | **PASS** |
| Productive video launch (100ms DB delay) | `Verifying` during delay -> `Hidden` | `Verifying` -> `Hidden` | **PASS** |
| Distracting video launch (100ms DB delay) | `Verifying` during delay -> `Blocked` | `Verifying` -> `Blocked` | **PASS** |
| Unknown video launch (50ms DB delay) | `Verifying` during delay -> `AskUser` | `Verifying` -> `AskUser` | **PASS** |
| Allowed video -> Home feed exit | Overlay transitions to `Hidden` | `OverlayState.Hidden` | **PASS** |
| Blocked video -> Home feed exit | Overlay transitions from `Blocked` to `Hidden` | `OverlayState.Hidden` | **PASS** |
| Shorts -> Home feed exit | Overlay transitions from `ShortsBlocked` to `Hidden` | `OverlayState.Hidden` | **PASS** |
| Stale slow Blocked DB query after Home exit | Stale result dropped, overlay stays `Hidden` | `OverlayState.Hidden` | **PASS** |
| Stale slow AskUser DB query after Home exit | Stale result dropped, overlay stays `Hidden` | `OverlayState.Hidden` | **PASS** |
| 10-cycle rapid Video/Home flapping | Strict state synchronization across all 10 cycles | Exact state observed | **PASS** |
| Pre-roll ad -> Home feed exit | Overlay transitions to `Hidden` | `OverlayState.Hidden` | **PASS** |
| Mid-roll ad -> Home feed exit | Overlay transitions to `Hidden` | `OverlayState.Hidden` | **PASS** |
| Direct video-to-video hopping (slow -> fast) | Stale slow query dropped, newest policy commits | `OverlayState.Blocked` | **PASS** |
| User classification on active video -> Home | Allowed -> Hidden, remains Hidden | `OverlayState.Hidden` | **PASS** |
| Blank fields in `VideoIdentity` | Shows fail-closed `Verifying("", "")` | `OverlayState.Verifying` | **PASS** |
| 10 rapid `hide()` calls | Completely idempotent without crashing | `OverlayState.Hidden` | **PASS** |
| Full `handlePolicyState` 9-step matrix | Exact 1:1 state transition matching | Exact states match | **PASS** |

---

## 4. Caveats

- Structural subtree parsing (Phase 3 & 7) and MediaSession fusion stream serialization (Phase 4 & 8) are scheduled for Milestones 3 and 4 per the project roadmap.
- Android system-level `WindowManager` token binding runs on real device runtime; unit tests verify full stateflow emissions, dispatching logic, and MockK/Robolectric test contracts.
- No other caveats.

---

## 5. Conclusion

**Verdict: `APPROVE`**

The defect identified in Finding 1 has been completely and robustly resolved in Milestone 2 Iteration 2:
1. `handlePolicyState` correctly differentiates between idle/home state (`PolicyState.Verifying(identity = null)`) and active video verification (`PolicyState.Verifying(identity != null)`).
2. Cold launches, playback stops, and home feed navigation safely hide the overlay, eliminating user lockout.
3. Active video transitions remain strictly fail-closed, immediately presenting the touch-consuming verifying overlay prior to asynchronous database resolution.
4. Out-of-order delayed database lookups cannot overwrite the idle home feed state.
5. All 383 unit tests across 37 test suites compile cleanly and pass with 0 failures.

---

## 6. Verification Method

To independently reproduce and verify this evaluation:

1. **Run New Adversarial Challenger Suite**:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubeHomeFeedLockoutChallengerTest"
   ```
   Confirm all 18 adversarial tests pass with 0 failures.

2. **Run All YouTube Feature Tests (14 suites, 254 tests)**:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.*"
   ```

3. **Run Full Test Suite (37 suites, 383 tests)**:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew :app:testQaDebugUnitTest
   ```
   Confirm `BUILD SUCCESSFUL` with 383 passing tests.

4. **Inspect Code**:
   - `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt:130-146`
   - `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubeHomeFeedLockoutChallengerTest.kt`
