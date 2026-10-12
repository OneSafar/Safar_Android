# Handoff Report: Milestone 1 — Adversarial Challenge of Deterministic Policy Engine

**Challenger**: Milestone 1 Challenger (`teamwork_preview_challenger_m1_1`)  
**Scope**: Adversarial Stress Testing & Empirical Verification of `YouTubePolicyEngine`  
**Verdict**: **CHALLENGE_FAILED**  
**Date**: 2026-09-11  
**Project Root**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android`  

---

## Challenge Summary

**Overall Risk Assessment**: **CRITICAL**

The policy engine implementation exhibits two critical invariant violations during user classification flows (`onUserClassified`):
1. **Critical Bypass**: YouTube Shorts can be transitioned to `PolicyState.Allowed` if `onUserClassified` is invoked for the channel while a Short is active. This directly violates Invariant 2 ("Shorts are evaluated and blocked independently of the channel policy" / "Shorts can never be allowed under any permutation of channel policies or metadata").
2. **State Corruption**: Invoking `onUserClassified` when playback is stopped (`currentIdentity == null`) causes the engine to transition into `PolicyState.Allowed` for a synthetic phantom `VideoIdentity`, corrupting the fail-closed state of the machine.

Both bugs were reproduced empirically with automated tests in `YouTubePolicyEngineTest.kt` executed via Gradle.

---

## 1. Observation

### 1.1 Code Under Audit
In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt` lines 132–167:
```kotlin
    fun onUserClassified(channelHandle: String, policy: ChannelPolicy) {
        val current = currentIdentity

        // Only update active state if the classification corresponds to the current video
        val currentHandle = current?.channelHandle?.trim()?.ifEmpty { current?.channelName?.trim() }
        val matchesCurrent = current != null && (
            channelHandle.equals(currentHandle, ignoreCase = true) ||
            channelHandle.removePrefix("@").equals(currentHandle?.removePrefix("@"), ignoreCase = true) ||
            channelHandle.equals(current.channelName.trim(), ignoreCase = true)
        )

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            repository.saveChannelPolicy(
                channelHandle = channelHandle,
                channelName = if (matchesCurrent) current?.channelName ?: "" else "",
                policy = policy,
            )
        }

        if (current == null || matchesCurrent) {
            val activeIdentity = current ?: VideoIdentity(channelHandle = channelHandle)
            activeJob?.cancel()

            lastVerifiedPolicy = policy
            lastVerifiedChannel = channelHandle

            when (policy) {
                ChannelPolicy.PRODUCTIVE -> {
                    _state.value = PolicyState.Allowed(activeIdentity, channelHandle)
                }
                ChannelPolicy.DISTRACTING -> {
                    _state.value = PolicyState.Blocked(activeIdentity, channelHandle, BlockReason.DISTRACTING_CHANNEL)
                }
            }
        }
    }
```

### 1.2 Empirical Execution Command & Failure Logs
Execution command:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest"
```

Verbatim terminal failure output:
```
> Task :app:testQaDebugUnitTest

YouTubePolicyEngineTest > adversarial - onUserClassified for current Shorts channel never transitions state to Allowed FAILED
    java.lang.AssertionError at Assert.java:89
        Caused by: java.lang.AssertionError at YouTubePolicyEngineTest.kt:870

YouTubePolicyEngineTest > adversarial - onUserClassified when playback stopped does not emit Allowed FAILED
    java.lang.AssertionError at Assert.java:89
        Caused by: java.lang.AssertionError at YouTubePolicyEngineTest.kt:900

35 tests completed, 2 failed

> Task :app:testQaDebugUnitTest FAILED
```

---

## 2. Logic Chain

### 2.1 Challenge 1: Shorts Allowed Bypass (Critical)
1. **Requirement & Contract**:
   - In `ORIGINAL_REQUEST.md` §Acceptance Criteria: *"Shorts are evaluated and blocked independently of the channel policy."*
   - In `PROJECT.md` §Feature 4: *"Invariant 2: Shorts independent block"*.
   - In dispatch scope: *"Verify that Shorts can never be allowed under any permutation of channel policies or metadata."*
2. **Observation**:
   - In `YouTubePolicyEngine.kt` line 151, `if (current == null || matchesCurrent)` checks only handle/name equality and completely ignores `current.isShorts`.
   - When `policy == ChannelPolicy.PRODUCTIVE`, lines 159–161 unconditionally execute:
     `_state.value = PolicyState.Allowed(activeIdentity, channelHandle)`
     even when `activeIdentity.isShorts == true`.
3. **Attack Scenario**:
   - User has an `AskUser` dialog open from an unclassified video or channel, or swiped into Shorts while an classification prompt was active.
   - User classifies the channel as `PRODUCTIVE`.
   - The active Short transitions directly from `PolicyState.ShortsBlocked` to `PolicyState.Allowed`.
   - The blocking overlay is dismissed, allowing Shorts playback in direct breach of the app's core value proposition.
4. **Empirical Confirmation**:
   - `adversarial - onUserClassified for current Shorts channel never transitions state to Allowed` reproduced the failure:
     `AssertionError: Shorts must NEVER transition to Allowed! Got: Allowed(identity=VideoIdentity(videoId=short_exploit, title=Shorts Video, channelHandle=@productive_creator, channelName=, isShorts=true, isAd=false), channelHandle=@productive_creator)`

### 2.2 Challenge 2: State Machine Corruption on Playback Stopped (High)
1. **Requirement & Contract**:
   - In `ORIGINAL_REQUEST.md` §R2: *"The system MUST default to 'Verifying' on any video identity change, never temporarily allowing unknown content."*
   - In `YouTubePolicyEngine.kt` line 177: `onPlaybackStopped()` resets state to `PolicyState.Verifying(null)`.
2. **Observation**:
   - In `YouTubePolicyEngine.kt` line 151, `if (current == null || matchesCurrent)` evaluates to `true` when `current == null`.
   - Line 152 creates a phantom identity:
     `val activeIdentity = current ?: VideoIdentity(channelHandle = channelHandle)`
   - Line 160 updates `_state.value` to `PolicyState.Allowed(activeIdentity, channelHandle)`.
3. **Attack Scenario**:
   - Video playback stops or user exits YouTube (`onPlaybackStopped()` called, setting `current = null` and state to `Verifying(null)`).
   - A delayed or background `onUserClassified` call arrives.
   - The engine transitions from `Verifying(null)` to `Allowed(VideoIdentity(channelHandle = "@some_channel"), "@some_channel")`.
   - When the user subsequently launches or returns to YouTube, the policy engine is in `Allowed` for a synthetic identity rather than fail-closed `Verifying(null)`.
4. **Empirical Confirmation**:
   - `adversarial - onUserClassified when playback stopped does not emit Allowed` reproduced the failure:
     `AssertionError: Must not transition to Allowed when playback stopped! Got: Allowed(identity=VideoIdentity(videoId=null, title=, channelHandle=@some_channel, channelName=, isShorts=false, isAd=false), channelHandle=@some_channel)`

---

## 3. Stress Test Results Summary

| Test Scenario | Expected Behavior | Actual Behavior | Result |
|---|---|---|---|
| `onUserClassified` on active Shorts | Shorts remain `ShortsBlocked` | Transitions to `Allowed` | **FAIL (Bug 1)** |
| `onUserClassified` when playback stopped | State remains `Verifying(null)`, DB saves policy | Transitions to `Allowed` (phantom identity) | **FAIL (Bug 2)** |
| Shorts with `isAd = true` | Must be blocked as `ShortsBlocked` | Blocked as `ShortsBlocked` | **PASS** |
| Extreme titles (10k chars, SQLi, XSS, emojis, control chars) | Fails closed or resolves safely to valid channel policy | Handled safely without crash | **PASS** |
| SQL injection strings in channel handle | Emits `AskUser` safely without SQLite errors | Safely emits `AskUser` | **PASS** |
| Whitespace-only handles / names | Fails closed in `Verifying` | Fails closed in `Verifying` | **PASS** |
| Slow DB query cancelled by rapid switch to new video | New video policy wins; old slow query does not overwrite | Cancelled properly, new video state intact | **PASS** |
| Rapid sequential video hopping | Emits `Verifying` on every transition | Emits `Verifying` before each resolution | **PASS** |
| Consecutive 3-ad pod during productive video | All ads retain `Allowed` without querying repository | Zero additional DB queries, retains `Allowed` | **PASS** |

---

## 4. Caveats

- **Accessibility Parser Timing**: Real-world DOM rendering delays and accessibility node hierarchy latency will be tested during Milestone 2 (`YouTubeGuardService`).
- **Review Constraint**: Under Teamwork challenger rules, implementation code was not modified. The adversarial tests have been committed to `YouTubePolicyEngineTest.kt` to allow immediate turn-around verification once the worker applies the fix.

---

## 5. Conclusion & Actionable Fix for Worker

### Verdict: **CHALLENGE_FAILED**

The implementation violates Invariant 2 (Shorts bypass) and exhibits state corruption on playback teardown. 

### Actionable Remediation for Worker:
In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`, update `onUserClassified`:
1. Check `current != null && matchesCurrent` (do **NOT** match if `current == null`).
2. If `current.isShorts == true`, do **NOT** transition to `Allowed`; Shorts must remain `PolicyState.ShortsBlocked`.
3. Proposed fix pattern:
   ```kotlin
   fun onUserClassified(channelHandle: String, policy: ChannelPolicy) {
       val current = currentIdentity

       val currentHandle = current?.channelHandle?.trim()?.ifEmpty { current?.channelName?.trim() }
       val matchesCurrent = current != null && (
           channelHandle.equals(currentHandle, ignoreCase = true) ||
           channelHandle.removePrefix("@").equals(currentHandle?.removePrefix("@"), ignoreCase = true) ||
           channelHandle.equals(current.channelName.trim(), ignoreCase = true)
       )

       scope.launch(start = CoroutineStart.UNDISPATCHED) {
           repository.saveChannelPolicy(
               channelHandle = channelHandle,
               channelName = if (matchesCurrent) current?.channelName ?: "" else "",
               policy = policy,
           )
       }

       // Only mutate active state if a video is actively playing and it is NOT a Short
       if (matchesCurrent && current != null) {
           if (current.isShorts) {
               // Invariant 2: Shorts can never be allowed under any channel policy
               return
           }

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
   }
   ```

---

## 6. Verification Method

To verify after the worker applies the fix:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest"
```

Expected output:
```
BUILD SUCCESSFUL
35 tests completed, 0 failed
```
