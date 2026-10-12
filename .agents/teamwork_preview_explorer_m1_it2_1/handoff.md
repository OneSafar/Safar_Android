# Handoff Report: Fix Strategy for YouTubePolicyEngine Invariant 2 Violation and Session Lifecycle Leak

**Author**: Milestone 1 Iteration 2 Explorer 1 (`teamwork_preview_explorer_m1_it2_1`)  
**Scope**: Exact Fix Strategy in `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt` and Test Suite Synchronization in `YouTubePolicyEngineTest.kt`  
**Date**: 2026-09-11  
**Handoff Type**: Hard Handoff (Investigation & Synthesis Complete)  
**Project Root**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android`  

---

## 1. Observation

### 1.1 Direct Defect Evidence in Source Code
In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt` (lines 132–167):
```kotlin
132: fun onUserClassified(channelHandle: String, policy: ChannelPolicy) {
133:     val current = currentIdentity
134: 
135:     // Only update active state if the classification corresponds to the current video
136:     val currentHandle = current?.channelHandle?.trim()?.ifEmpty { current?.channelName?.trim() }
137:     val matchesCurrent = current != null && (
138:         channelHandle.equals(currentHandle, ignoreCase = true) ||
139:         channelHandle.removePrefix("@").equals(currentHandle?.removePrefix("@"), ignoreCase = true) ||
140:         channelHandle.equals(current.channelName.trim(), ignoreCase = true)
141:     )
142: 
143:     scope.launch(start = CoroutineStart.UNDISPATCHED) {
144:         repository.saveChannelPolicy(
145:             channelHandle = channelHandle,
146:             channelName = if (matchesCurrent) current?.channelName ?: "" else "",
147:             policy = policy,
148:         )
149:     }
150: 
151:     if (current == null || matchesCurrent) {
152:         val activeIdentity = current ?: VideoIdentity(channelHandle = channelHandle)
153:         activeJob?.cancel()
154: 
155:         lastVerifiedPolicy = policy
156:         lastVerifiedChannel = channelHandle
157: 
158:         when (policy) {
159:             ChannelPolicy.PRODUCTIVE -> {
160:                 _state.value = PolicyState.Allowed(activeIdentity, channelHandle)
161:             }
162:             ChannelPolicy.DISTRACTING -> {
163:                 _state.value = PolicyState.Blocked(activeIdentity, channelHandle, BlockReason.DISTRACTING_CHANNEL)
164:             }
165:         }
166:     }
167: }
```

### 1.2 Verbatim Test Execution Failure
- **Command executed**:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest"
  ```
- **Exit Code**: `1` (`BUILD FAILED`)
- **Verbatim Output**:
  ```
  > Task :app:testQaDebugUnitTest FAILED

  YouTubePolicyEngineTest > adversarial - onUserClassified for current Shorts channel never transitions state to Allowed FAILED
      java.lang.AssertionError at Assert.java:89
          Caused by: java.lang.AssertionError: Shorts must NEVER transition to Allowed! Got: Allowed(identity=VideoIdentity(videoId=short_exploit, title=Shorts Video, channelHandle=@productive_creator, channelName=, isShorts=true, isAd=false), channelHandle=@productive_creator)
          at YouTubePolicyEngineTest.kt:894

  YouTubePolicyEngineTest > adversarial - onUserClassified when playback stopped does not emit Allowed FAILED
      java.lang.AssertionError at Assert.java:89
          Caused by: java.lang.AssertionError: Must not transition to Allowed when playback stopped! Got: Allowed(identity=VideoIdentity(videoId=null, title=, channelHandle=@some_channel, channelName=, isShorts=false, isAd=false), channelHandle=@some_channel)
          at YouTubePolicyEngineTest.kt:918

  35 tests completed, 2 failed
  ```

### 1.3 Audit & Reviewer Findings Correlation
1. **Auditor Report (`teamwork_preview_auditor_m1_1/handoff.md`)**:
   - Verdict: `INTEGRITY VIOLATION`.
   - Check 4 (Build & run) and Check 5 (Output verification) failed due to lines 151–166 in `YouTubePolicyEngine.kt`.
2. **Reviewer 2 Report (`teamwork_preview_reviewer_m1_2/handoff.md`)**:
   - Verdict: `REQUEST_CHANGES`.
   - Finding 1: Invariant 2 broken — Shorts transition to `Allowed` on classification when `policy == PRODUCTIVE`.
   - Finding 2: Session lifecycle leak — `current == null` triggers phantom identity creation and `Allowed` emission.
3. **Challenger 1 Report (`teamwork_preview_challenger_m1_1/handoff.md`)**:
   - Verdict: `CHALLENGE_FAILED`.
   - Added two adversarial unit tests in `YouTubePolicyEngineTest.kt` (lines 869–921) reproducing both failure modes.

### 1.4 Test Suite Anomaly in `YouTubePolicyEngineTest.kt:893`
In `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt` lines 888–896:
```kotlin
888:     // Adversarial probe: user classification event arrives while Shorts is playing
889:     engine.onUserClassified("@productive_creator", ChannelPolicy.PRODUCTIVE)
890: 
891:     // Invariant 2: Shorts can NEVER be allowed under any permutation of channel policies or user classification
892:     val postClassifyState = awaitItem()
893:     assertFalse("Shorts must NEVER transition to Allowed! Got: $postClassifyState", postClassifyState is PolicyState.Allowed)
894:     assertTrue("Shorts must remain ShortsBlocked! Got: $postClassifyState", postClassifyState is PolicyState.ShortsBlocked)
```
- Line 892 calls `val postClassifyState = awaitItem()`.
- Prior to line 889, `_state.value` was already `PolicyState.ShortsBlocked(shorts, BlockReason.SHORTS)` (asserted at lines 886–887).
- In Kotlin coroutines `StateFlow`, `setValue` compares `oldState == newState` (structural equality). If unchanged, collectors are **NOT** notified.
- If `onUserClassified` correctly prevents transitioning to `Allowed` and preserves `ShortsBlocked`, `_state.value` does not change, and `engine.state` emits zero new items.
- In Turbine, calling `awaitItem()` when zero items are emitted results in a coroutine hang followed by:
  `kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1000 ms`.
- Conversely, lines 695 and 908 of the same test file handle identical no-op transitions using `expectNoEvents()` and checking `engine.state.value` directly.

---

## 2. Logic Chain

### 2.1 Issue 1: Invariant 2 Violation in `onUserClassified`
1. **Invariant 2 Contract** (`ORIGINAL_REQUEST.md` §Acceptance Criteria & `PROJECT.md` §Feature 4):
   - "Shorts are evaluated and blocked independently of channel policy."
   - "YouTube Shorts format is detected and blocked independently of channel policy (`PolicyState.ShortsBlocked`)."
   - Shorts must NEVER be allowed under any user classification or channel policy.
2. **Mechanism of Violation**:
   - In `YouTubePolicyEngine.kt` line 151, `if (current == null || matchesCurrent)` checks handle equality against the active identity.
   - If the active video is a Short (`current.isShorts == true`), `matchesCurrent` evaluates to `true`.
   - Lines 158–165 check `when (policy)`. If `policy == ChannelPolicy.PRODUCTIVE`, line 160 unconditionally executes:
     `_state.value = PolicyState.Allowed(activeIdentity, channelHandle)`.
   - This transitions the engine out of `PolicyState.ShortsBlocked` and directly into `PolicyState.Allowed`, permitting Shorts playback in active violation of Invariant 2.
   - Furthermore, line 155 sets `lastVerifiedPolicy = policy`, which corrupts Invariant 3 by allowing subsequent ads to inherit `Allowed` from a Short.
3. **Remediation**:
   - Guard the active state mutation with `!current.isShorts`.
   - If `current.isShorts == true`, `onUserClassified` must persist the channel classification to Room via `repository.saveChannelPolicy`, but must NOT alter `_state.value`, must NOT cancel `activeJob`, and must NOT set `lastVerifiedPolicy`. The state strictly remains `PolicyState.ShortsBlocked`.

### 2.2 Issue 2: Session Lifecycle Leak
1. **Session Lifecycle Contract** (`ORIGINAL_REQUEST.md` §R2 & `PROJECT.md` §Feature 3):
   - When playback stops or the user exits YouTube, `onPlaybackStopped()` transitions the engine into idle fail-closed `PolicyState.Verifying(null)` and clears `currentIdentity = null`.
   - The engine must never spontaneously transition to `Allowed` in the absence of active media playback.
2. **Mechanism of Leak**:
   - Line 151 contains the clause: `if (current == null || matchesCurrent)`.
   - When playback is stopped (`current == null`), this condition evaluates to `true`.
   - Line 152 manufactures a synthetic identity: `val activeIdentity = current ?: VideoIdentity(channelHandle = channelHandle)`.
   - Line 160 transitions `_state.value = PolicyState.Allowed(activeIdentity, channelHandle)`.
   - The engine, while idle with no video playing, enters `Allowed` for a dummy video identity.
3. **Remediation**:
   - Disallow state mutation when `current == null`.
   - When `current == null`, `onUserClassified` must persist the policy to Room via `repository.saveChannelPolicy`, but must NOT alter `_state.value` and must NOT instantiate synthetic `VideoIdentity` objects.

### 2.3 Synthesis of the Exact Engine Guard
Combining the requirements:
- Only mutate active state when:
  1. A video is actively playing (`current != null`), AND
  2. The classified channel matches the active video (`matchesCurrent`), AND
  3. The active video is a regular video, not a Short (`!current.isShorts`).
- Formal expression:
  ```kotlin
  if (current != null && matchesCurrent && !current.isShorts) {
      activeJob?.cancel()
      lastVerifiedPolicy = policy
      lastVerifiedChannel = channelHandle

      when (policy) {
          ChannelPolicy.PRODUCTIVE -> {
              _state.value = PolicyState.Allowed(current, channelHandle)
          }
          ChannelPolicy.DISTRACTING -> {
              _state.value = PolicyState.Blocked(current, channelHandle, BlockReason.DISTRACTING_CHANNEL)
          }
      }
  }
  ```

### 2.4 Resolution of Test Anomaly (`YouTubePolicyEngineTest.kt`)
1. In `adversarial - onUserClassified for current Shorts channel never transitions state to Allowed` (lines 881–896):
   - State before classification: `PolicyState.ShortsBlocked`.
   - With the correct engine fix, calling `onUserClassified` preserves `PolicyState.ShortsBlocked`.
   - Since `_state.value` remains identical, `MutableStateFlow` suppresses identical emissions (distinct-until-changed).
   - Calling `awaitItem()` on line 892 fails due to Turbine timeout.
2. The proper assertion matching Turbine StateFlow testing standards is:
   ```kotlin
   expectNoEvents()
   val postClassifyState = engine.state.value
   assertFalse("Shorts must NEVER transition to Allowed! Got: $postClassifyState", postClassifyState is PolicyState.Allowed)
   assertTrue("Shorts must remain ShortsBlocked! Got: $postClassifyState", postClassifyState is PolicyState.ShortsBlocked)
   ```
   This guarantees that:
   - No illegal transition to `Allowed` occurred (`expectNoEvents()`).
   - The engine remains securely in `ShortsBlocked`.

---

## 3. Caveats

1. **Test Double Divergence in E2E Suite**:
   - As discovered by Challenger 2 (`teamwork_preview_challenger_m1_2/handoff.md`), `YouTubeGuardE2ETest.kt` lines 96–101 lowercases channel handles, whereas `YouTubeChannelRepositoryImpl` does not. While separate from `YouTubePolicyEngine`, the Worker should ensure channel handles passed to `onUserClassified` are normalized cleanly across persistence and state machine boundaries.
2. **Concurrency During Rapid Classification**:
   - `repository.saveChannelPolicy` is launched via `scope.launch(start = CoroutineStart.UNDISPATCHED)`. Because it runs undispatched until its first suspension point, the Room write initiates synchronously on the calling thread before any subsequent UI/state changes.
3. **No Other Caveats**:
   - All state machine paths (Verifying, AskUser, Allowed, Blocked, ShortsBlocked, Ad retention) remain completely deterministic and covered by tests.

---

## 4. Conclusion

### Summary of Fix Strategy
The fix requires two tightly coordinated edits:
1. **In `YouTubePolicyEngine.kt`**: Replace the flawed `if (current == null || matchesCurrent)` block with `if (current != null && matchesCurrent && !current.isShorts)`. Remove synthetic `VideoIdentity` creation.
2. **In `YouTubePolicyEngineTest.kt`**: Update line 892 from `val postClassifyState = awaitItem()` to `expectNoEvents(); val postClassifyState = engine.state.value` to prevent a false-positive Turbine timeout caused by StateFlow conflation.

### Actionable Implementation Diff

#### Target File 1: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`
```diff
--- a/app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt
+++ b/app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt
@@ -151,16 +151,16 @@ class YouTubePolicyEngine(
-        if (current == null || matchesCurrent) {
-            val activeIdentity = current ?: VideoIdentity(channelHandle = channelHandle)
-            activeJob?.cancel()
-
-            lastVerifiedPolicy = policy
-            lastVerifiedChannel = channelHandle
-
-            when (policy) {
-                ChannelPolicy.PRODUCTIVE -> {
-                    _state.value = PolicyState.Allowed(activeIdentity, channelHandle)
-                }
-                ChannelPolicy.DISTRACTING -> {
-                    _state.value = PolicyState.Blocked(activeIdentity, channelHandle, BlockReason.DISTRACTING_CHANNEL)
-                }
-            }
-        }
+        // Only update active state if a video is actively playing, matches this channel, and is NOT a Short
+        if (current != null && matchesCurrent && !current.isShorts) {
+            activeJob?.cancel()
+
+            lastVerifiedPolicy = policy
+            lastVerifiedChannel = channelHandle
+
+            when (policy) {
+                ChannelPolicy.PRODUCTIVE -> {
+                    _state.value = PolicyState.Allowed(current, channelHandle)
+                }
+                ChannelPolicy.DISTRACTING -> {
+                    _state.value = PolicyState.Blocked(current, channelHandle, BlockReason.DISTRACTING_CHANNEL)
+                }
+            }
+        }
     }
```

#### Target File 2: `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt`
```diff
--- a/app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt
+++ b/app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt
@@ -892,3 +892,4 @@ class YouTubePolicyEngineTest {
             // Invariant 2: Shorts can NEVER be allowed under any permutation of channel policies or user classification
-            val postClassifyState = awaitItem()
+            expectNoEvents()
+            val postClassifyState = engine.state.value
             assertFalse("Shorts must NEVER transition to Allowed! Got: $postClassifyState", postClassifyState is PolicyState.Allowed)
```

A machine-applicable patch file has been generated at:
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_m1_it2_1/youtube_policy_engine_fix.patch`

---

## 5. Verification Method

### 5.1 Independent Test Execution Command
Run the unit test suite targeting `YouTubePolicyEngineTest`:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest"
```

### 5.2 Pass Criteria
- Build Status: `BUILD SUCCESSFUL` (exit code `0`).
- Test Results: `35 tests completed, 0 failed, 0 errors, 0 skipped`.
- Test XML Report: `app/build/test-results/testQaDebugUnitTest/TEST-com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest.xml` containing `failures="0" errors="0"`.

### 5.3 Invalidation Conditions
- **Condition 1**: If `adversarial - onUserClassified for current Shorts channel never transitions state to Allowed` fails with `AssertionError: Shorts must NEVER transition to Allowed`, Invariant 2 is breached.
- **Condition 2**: If the test fails with `TimeoutCancellationException: Timed out waiting for 1000 ms`, line 892 in `YouTubePolicyEngineTest.kt` has not been updated from `awaitItem()` to `expectNoEvents()` and `engine.state.value`.
- **Condition 3**: If `adversarial - onUserClassified when playback stopped does not emit Allowed` fails with `AssertionError: Must not transition to Allowed when playback stopped`, session lifecycle leakage persists.
