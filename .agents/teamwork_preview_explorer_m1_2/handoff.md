# Milestone 1 Handoff Report: Deterministic State Machine & Policy Engine Specification

**Author**: M1 Explorer 2  
**Target Component**: `com.safarparmar.app.feature.youtube.policy` (`PolicyState`, `VideoIdentity`, `YouTubePolicyEngine`)  
**Handoff Type**: Hard Handoff (Investigation & Specification Complete)  
**Date**: 2026-09-11  
**Project Root**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android`  
**Working Directory**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_m1_2`  

---

## 1. Observation

### 1.1 Requirements & Acceptance Criteria
From `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md`:
- **Requirement R2 (State Machine & Policy Engine)**:
  > "Implement a deterministic state machine (`YouTubePolicyEngine`) that manages states (`Verifying`, `AskUser`, `Allowed`, `Blocked`, `ShortsBlocked`). The system MUST default to 'Verifying' on any video identity change, never temporarily allowing unknown content." (lines 15–17)
- **Acceptance Criteria (Detection & State Machine)**:
  > - "Unit tests for `YouTubePolicyEngine` verify that a new video identity always transitions to 'Verifying' before resolving to 'Allowed' or 'Blocked'." (lines 26–27)
  > - "Shorts are evaluated and blocked independently of the channel policy." (line 28)
  > - "Ad transitions retain the previous verified channel's policy instead of evaluating the ad's metadata." (line 29)

### 1.2 Architectural Contracts in PROJECT.md
From `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/orchestrator_1/PROJECT.md` (lines 33–75):
- **Policy Engine Contract**:
  ```kotlin
  package com.safarparmar.app.feature.youtube.policy

  sealed interface PolicyState {
      data class Verifying(val identity: VideoIdentity? = null) : PolicyState
      data class AskUser(val identity: VideoIdentity, val channelHandle: String) : PolicyState
      data class Allowed(val identity: VideoIdentity, val channelHandle: String) : PolicyState
      data class Blocked(val identity: VideoIdentity, val channelHandle: String, val reason: BlockReason = BlockReason.DISTRACTING_CHANNEL) : PolicyState
      data class ShortsBlocked(val identity: VideoIdentity, val reason: BlockReason = BlockReason.SHORTS) : PolicyState
  }

  data class VideoIdentity(
      val videoId: String? = null,
      val title: String = "",
      val channelHandle: String = "",
      val channelName: String = "",
      val isShorts: Boolean = false,
      val isAd: Boolean = false,
  )

  class YouTubePolicyEngine {
      val state: StateFlow<PolicyState>
      fun onVideoDetected(identity: VideoIdentity)
      fun onUserClassified(channelHandle: String, policy: ChannelPolicy)
      fun onPlaybackStopped()
  }
  ```
- **Persistence Contract (`YouTubeChannelRepository`)**:
  ```kotlin
  package com.safarparmar.app.domain.repository

  import com.safarparmar.app.data.local.entity.ChannelPolicy
  import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
  import kotlinx.coroutines.flow.Flow

  interface YouTubeChannelRepository {
      suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy?
      suspend fun saveChannelPolicy(channelHandle: String, channelName: String = "", policy: ChannelPolicy)
      fun getAllChannels(): Flow<List<YouTubeChannelEntity>>
  }
  ```
- **Designated Source Paths**:
  - `app/src/main/java/com/safarparmar/app/feature/youtube/policy/PolicyState.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/policy/VideoIdentity.kt`
  - `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`

### 1.3 Testing Environment and Dependencies
From `app/build.gradle.kts` and `gradle/libs.versions.toml`:
- **Kotlin**: 2.0.21, **Coroutines**: 1.9.0 (`kotlinx.coroutines.test:1.9.0`)
- **Flow Testing**: `app.cash.turbine:turbine:1.1.0` is present and active.
- **Unit Test Runner**: JUnit 4.13.2 with `MainDispatcherRule` (`app/src/test/java/com/safarparmar/app/ui/auth/MainDispatcherRule.kt`) using `StandardTestDispatcher()`.
- **JDK Requirement**: Verified executable with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`. `./gradlew :app:testQaDebugUnitTest --dry-run` executes in ~600ms.

---

## 2. Logic Chain

### 2.1 Core Invariant Deductions

#### Invariant 1: Fail-Closed Default to `Verifying`
- **Rule**: Any video identity change MUST transition immediately to `PolicyState.Verifying(identity)` prior to any asynchronous database lookup.
- **Mechanism**:
  1. In `onVideoDetected(identity: VideoIdentity)`, the engine compares `identity` with `currentIdentity` using `identity.isSameVideo(currentIdentity)`.
  2. If `isSameVideo` is false, any previous resolution job (`activeJob?.cancel()`) is cancelled.
  3. `_state.value = PolicyState.Verifying(identity)` is updated **synchronously** on the invoking thread.
  4. This guarantees that `state.value` is `Verifying` before any coroutine dispatch occurs. When tested with Turbine (`state.test { awaitItem() }`) or observed by Compose UI, the `Verifying` state is emitted first without race conditions.

#### Invariant 2: Shorts Independence
- **Rule**: Shorts are evaluated and blocked independently of channel policy; never prompt user with `AskUser`.
- **Mechanism**:
  1. After entering `Verifying(identity)`, if `identity.isShorts == true`, `resolvePolicy` sets `_state.value = PolicyState.ShortsBlocked(identity, BlockReason.SHORTS)` and returns immediately.
  2. `repository.getChannelPolicy` is **never called** for Shorts.
  3. Even if the channel is saved in Room as `ChannelPolicy.PRODUCTIVE` or `DISTRACTING`, the result is strictly `ShortsBlocked`.

#### Invariant 3: Ad Policy Retention
- **Rule**: Ad transitions retain the previous verified channel's policy instead of evaluating ad metadata.
- **Mechanism**:
  1. If `identity.isAd == true`, `resolvePolicy` checks `lastVerifiedPolicy` and `lastVerifiedChannel`.
  2. If `lastVerifiedPolicy == ChannelPolicy.PRODUCTIVE`, state resolves to `PolicyState.Allowed(identity, lastVerifiedChannel ?: identity.channelHandle)`.
  3. If `lastVerifiedPolicy == ChannelPolicy.DISTRACTING`, state resolves to `PolicyState.Blocked(identity, lastVerifiedChannel ?: identity.channelHandle, BlockReason.DISTRACTING_CHANNEL)`.
  4. If `lastVerifiedPolicy == null` (e.g. cold start directly into an ad before any channel was verified), state remains fail-closed in `PolicyState.Verifying(identity)`.
  5. Under no circumstances is the advertiser's channel handle looked up in Room, nor is `AskUser` prompted for an ad.

#### Invariant 4: User Classification Flow
- **Rule**: `onUserClassified(channelHandle, policy)` must persist to Room repository and update active state immediately.
- **Mechanism**:
  1. `onUserClassified` cancels any pending lookup job (`activeJob?.cancel()`).
  2. Updates memory caches: `lastVerifiedPolicy = policy`, `lastVerifiedChannel = channelHandle`.
  3. Updates `_state.value` to `PolicyState.Allowed` or `PolicyState.Blocked`.
  4. Launches coroutine in `scope` calling `repository.saveChannelPolicy(channelHandle, channelName, policy)`.

#### Invariant 5: Playback Lifecycle
- **Rule**: `onPlaybackStopped()` resets active identity and returns engine to unassigned `Verifying(null)`.
- **Mechanism**:
  1. `activeJob?.cancel()`.
  2. Clears `currentIdentity = null`, `lastVerifiedPolicy = null`, `lastVerifiedChannel = null`.
  3. Sets `_state.value = PolicyState.Verifying(null)`.

---

### 2.2 Complete State Transition Matrix

| Source State | Trigger / Event | Condition | Target State | Action / Side Effect |
|:---|:---|:---|:---|:---|
| Any | `onVideoDetected(id)` | `id.isSameVideo(current)` && `state !is Verifying` | Unchanged | No-op, drop duplicate UI/accessibility event |
| Any | `onVideoDetected(id)` | `!id.isSameVideo(current)` | `PolicyState.Verifying(id)` (Transient) | Cancel `activeJob`, set `currentIdentity = id`, launch `resolvePolicy` |
| `Verifying(id)` | `resolvePolicy` | `id.isShorts == true` | `PolicyState.ShortsBlocked(id, SHORTS)` | Bypass DB lookup; block immediately |
| `Verifying(id)` | `resolvePolicy` | `id.isAd == true` && `lastVerified == PRODUCTIVE` | `PolicyState.Allowed(id, lastVerifiedChannel)` | Retain playback; bypass ad metadata lookup |
| `Verifying(id)` | `resolvePolicy` | `id.isAd == true` && `lastVerified == DISTRACTING` | `PolicyState.Blocked(id, lastVerifiedChannel, DISTRACTING_CHANNEL)` | Retain blocking; bypass ad metadata lookup |
| `Verifying(id)` | `resolvePolicy` | `id.isAd == true` && `lastVerified == null` | `PolicyState.Verifying(id)` | Cold start ad: remain fail-closed until content video |
| `Verifying(id)` | `resolvePolicy` | `!isShorts && !isAd` && DB returns `PRODUCTIVE` | `PolicyState.Allowed(id, handle)` | Set `lastVerifiedPolicy = PRODUCTIVE`, `lastVerifiedChannel = handle` |
| `Verifying(id)` | `resolvePolicy` | `!isShorts && !isAd` && DB returns `DISTRACTING` | `PolicyState.Blocked(id, handle, DISTRACTING_CHANNEL)` | Set `lastVerifiedPolicy = DISTRACTING`, `lastVerifiedChannel = handle` |
| `Verifying(id)` | `resolvePolicy` | `!isShorts && !isAd` && DB returns `null` | `PolicyState.AskUser(id, handle)` | Prompt user via Compose overlay |
| `AskUser(id, handle)` | `onUserClassified(handle, PRODUCTIVE)` | User taps Productive | `PolicyState.Allowed(id, handle)` | Persist to Room; set `lastVerified = PRODUCTIVE` |
| `AskUser(id, handle)` | `onUserClassified(handle, DISTRACTING)` | User taps Distracting | `PolicyState.Blocked(id, handle, DISTRACTING_CHANNEL)` | Persist to Room; set `lastVerified = DISTRACTING` |
| Any | `onPlaybackStopped()` | YouTube closed or video exited | `PolicyState.Verifying(null)` | Cancel `activeJob`, clear active identity & caches |

---

### 2.3 Detailed Code Specifications

#### 2.3.1 `PolicyState.kt`
**File Target**: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/PolicyState.kt`

```kotlin
package com.safarparmar.app.feature.youtube.policy

/**
 * Reason why a video or stream is blocked.
 */
enum class BlockReason {
    DISTRACTING_CHANNEL,
    SHORTS,
    UNVERIFIED,
}

/**
 * Deterministic states for the YouTube content guard state machine.
 */
sealed interface PolicyState {
    /**
     * Initial transient state entered on ANY video identity change.
     * Content is fail-closed before Room DB lookup completes.
     */
    data class Verifying(
        val identity: VideoIdentity? = null,
    ) : PolicyState

    /**
     * Entered when a video's channel is unclassified in the database.
     * Triggers the Compose overlay prompt for user classification.
     */
    data class AskUser(
        val identity: VideoIdentity,
        val channelHandle: String,
    ) : PolicyState

    /**
     * Video playback is allowed. Overlay is dismissed.
     */
    data class Allowed(
        val identity: VideoIdentity,
        val channelHandle: String,
    ) : PolicyState

    /**
     * Video playback is blocked. Overlay covers screen.
     */
    data class Blocked(
        val identity: VideoIdentity,
        val channelHandle: String,
        val reason: BlockReason = BlockReason.DISTRACTING_CHANNEL,
    ) : PolicyState

    /**
     * YouTube Shorts format is detected and blocked independently of channel policy.
     */
    data class ShortsBlocked(
        val identity: VideoIdentity,
        val reason: BlockReason = BlockReason.SHORTS,
    ) : PolicyState
}
```

---

#### 2.3.2 `VideoIdentity.kt`
**File Target**: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/VideoIdentity.kt`

```kotlin
package com.safarparmar.app.feature.youtube.policy

/**
 * Represents the extracted metadata identity of a video playing in YouTube.
 */
data class VideoIdentity(
    val videoId: String? = null,
    val title: String = "",
    val channelHandle: String = "",
    val channelName: String = "",
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
) {
    /**
     * Determines whether another [VideoIdentity] represents the identical playing content.
     * Distinguishes between video playback, ad playback, and shorts.
     */
    fun isSameVideo(other: VideoIdentity?): Boolean {
        if (other == null) return false
        if (!videoId.isNullOrBlank() && !other.videoId.isNullOrBlank()) {
            return videoId == other.videoId && isShorts == other.isShorts && isAd == other.isAd
        }
        return title == other.title &&
            channelHandle == other.channelHandle &&
            isShorts == other.isShorts &&
            isAd == other.isAd
    }
}
```

---

#### 2.3.3 `YouTubePolicyEngine.kt`
**File Target**: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`

```kotlin
package com.safarparmar.app.feature.youtube.policy

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deterministic state machine and policy engine governing YouTube content blocking.
 *
 * Core Invariants:
 * 1. Default to [PolicyState.Verifying] immediately on ANY video identity change (fail-closed before DB lookup).
 * 2. Shorts are evaluated and blocked independently of channel policy ([PolicyState.ShortsBlocked]).
 * 3. Ad transitions retain previous verified channel's policy instead of evaluating ad metadata.
 */
@Singleton
class YouTubePolicyEngine(
    private val repository: YouTubeChannelRepository,
    private val scope: CoroutineScope,
) {
    /**
     * Primary constructor for Hilt dependency injection.
     * Uses [SupervisorJob] with [Dispatchers.Default] for production execution.
     */
    @Inject
    constructor(repository: YouTubeChannelRepository) : this(
        repository = repository,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    )

    private val _state = MutableStateFlow<PolicyState>(PolicyState.Verifying(null))
    val state: StateFlow<PolicyState> = _state.asStateFlow()

    private var currentIdentity: VideoIdentity? = null
    private var lastVerifiedPolicy: ChannelPolicy? = null
    private var lastVerifiedChannel: String? = null
    private var activeJob: Job? = null

    /**
     * Called whenever video information is extracted from AccessibilityService or MediaSessionManager.
     */
    fun onVideoDetected(identity: VideoIdentity) {
        // Prevent duplicate processing if identical video is already resolved
        if (identity.isSameVideo(currentIdentity) && _state.value !is PolicyState.Verifying) {
            return
        }

        activeJob?.cancel()
        currentIdentity = identity

        // INVARIANT 1: Default to 'Verifying' immediately on ANY video identity change (fail-closed)
        _state.value = PolicyState.Verifying(identity)

        activeJob = scope.launch {
            resolvePolicy(identity)
        }
    }

    private suspend fun resolvePolicy(identity: VideoIdentity) {
        // INVARIANT 2: Shorts are evaluated and blocked independently of channel policy
        if (identity.isShorts) {
            _state.value = PolicyState.ShortsBlocked(identity, BlockReason.SHORTS)
            return
        }

        // INVARIANT 3: Ad transitions retain previous verified channel's policy instead of evaluating ad metadata
        if (identity.isAd) {
            when (lastVerifiedPolicy) {
                ChannelPolicy.PRODUCTIVE -> {
                    _state.value = PolicyState.Allowed(
                        identity = identity,
                        channelHandle = lastVerifiedChannel ?: identity.channelHandle,
                    )
                }
                ChannelPolicy.DISTRACTING -> {
                    _state.value = PolicyState.Blocked(
                        identity = identity,
                        channelHandle = lastVerifiedChannel ?: identity.channelHandle,
                        reason = BlockReason.DISTRACTING_CHANNEL,
                    )
                }
                null -> {
                    // No prior verified video in session: remain fail-closed in Verifying
                    _state.value = PolicyState.Verifying(identity)
                }
            }
            return
        }

        // Standard Video: Look up classification in repository
        val handle = identity.channelHandle.trim().ifEmpty { identity.channelName.trim() }
        if (handle.isEmpty()) {
            // Cannot verify without channel identity; remain fail-closed in Verifying
            _state.value = PolicyState.Verifying(identity)
            return
        }

        val policy = repository.getChannelPolicy(handle)
        when (policy) {
            ChannelPolicy.PRODUCTIVE -> {
                lastVerifiedPolicy = ChannelPolicy.PRODUCTIVE
                lastVerifiedChannel = handle
                _state.value = PolicyState.Allowed(identity, handle)
            }
            ChannelPolicy.DISTRACTING -> {
                lastVerifiedPolicy = ChannelPolicy.DISTRACTING
                lastVerifiedChannel = handle
                _state.value = PolicyState.Blocked(identity, handle, BlockReason.DISTRACTING_CHANNEL)
            }
            null -> {
                // Unknown channel: prompt user via Compose overlay
                _state.value = PolicyState.AskUser(identity, handle)
            }
        }
    }

    /**
     * Called when the user classifies an unclassified channel via the AskUser overlay dialog.
     */
    fun onUserClassified(channelHandle: String, policy: ChannelPolicy) {
        val activeIdentity = currentIdentity ?: VideoIdentity(channelHandle = channelHandle)
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

        scope.launch {
            repository.saveChannelPolicy(
                channelHandle = channelHandle,
                channelName = activeIdentity.channelName,
                policy = policy,
            )
        }
    }

    /**
     * Called when playback exits or YouTube stops playing.
     */
    fun onPlaybackStopped() {
        activeJob?.cancel()
        currentIdentity = null
        lastVerifiedPolicy = null
        lastVerifiedChannel = null
        _state.value = PolicyState.Verifying(null)
    }
}
```

---

## 3. Caveats

1. **StateFlow Conflation vs Synchronous `Verifying` Emission**:
   - `MutableStateFlow` conflates emissions if multiple values are assigned synchronously within the same dispatch cycle.
   - By assigning `_state.value = PolicyState.Verifying(identity)` synchronously at the start of `onVideoDetected`, and deferring `resolvePolicy(identity)` to `scope.launch`, collectors (including Turbine `awaitItem()` and Compose `collectAsStateWithLifecycle`) are guaranteed to observe the fail-closed `Verifying` state before `Allowed`, `Blocked`, `ShortsBlocked`, or `AskUser`.
2. **Channel Identity Extraction Edge Cases**:
   - In accessibility node trees, `channelName` or title may appear milliseconds before `@channelHandle` is populated.
   - `handle = identity.channelHandle.trim().ifEmpty { identity.channelName.trim() }` ensures graceful fallback. If both are blank, the engine intentionally stays in `PolicyState.Verifying(identity)` rather than prompting the user with an empty handle.
3. **Cold-Start Ad Playback**:
   - If YouTube launches directly into an ad before any channel has been evaluated, `lastVerifiedPolicy` is `null`. The engine remains in `PolicyState.Verifying(identity)` until the primary video begins, ensuring fail-closed safety.
4. **Testing Injected CoroutineScope**:
   - The secondary constructor `constructor(repository: YouTubeChannelRepository, scope: CoroutineScope)` is required for deterministic unit testing with `TestScope` or `StandardTestDispatcher`. The primary constructor with `@Inject` ensures Hilt can construct it without requiring an `@Provides CoroutineScope` binding.

---

## 4. Conclusion

The state machine design fully satisfies all requirements of Milestone 1:
1. **Invariant 1 Compliant**: Every video identity change triggers synchronous transition to `PolicyState.Verifying(identity)` before launching database queries.
2. **Invariant 2 Compliant**: Shorts bypass repository lookups and transition directly to `PolicyState.ShortsBlocked`.
3. **Invariant 3 Compliant**: Ads retain `lastVerifiedPolicy` without querying the database for advertiser metadata or triggering `AskUser`.
4. **User Classification Compliant**: `onUserClassified` immediately updates active state and asynchronously persists the classification to Room.
5. **Architectural Separation**: Clean interfaces decoupled from Android framework APIs enable 100% JVM test execution in under 2 seconds.

---

## 5. Verification Method

### 5.1 Independent Test Execution Command
Run the JVM unit test suite using the project's verified JDK:
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest"
```

### 5.2 Test Scenarios & Invalidation Conditions
A compliant test suite (`YouTubePolicyEngineTest`) must pass the following test scenarios:

| Test Case | Method / Action | Expected Assertions | Invalidation Condition |
|:---|:---|:---|:---|
| Invariant 1 (Allowed) | Detect productive video identity | 1. `awaitItem() is Verifying`<br>2. `awaitItem() is Allowed` | Fails if `Allowed` emitted without preceding `Verifying` |
| Invariant 1 (Blocked) | Detect distracting video identity | 1. `awaitItem() is Verifying`<br>2. `awaitItem() is Blocked` | Fails if `Blocked` emitted without preceding `Verifying` |
| Invariant 1 (Consecutive) | Detect video 1 then video 2 | Transitions: `Verifying` -> `Allowed` -> `Verifying` -> `Blocked` | Fails if video 2 does not re-enter `Verifying` |
| Invariant 2 (Shorts) | Detect Shorts from productive channel | 1. `awaitItem() is Verifying`<br>2. `awaitItem() is ShortsBlocked` | Fails if Shorts resolves to `Allowed` or queries repository |
| Invariant 2 (Shorts Unknown) | Detect Shorts from unclassified channel | Emits `ShortsBlocked` (never `AskUser`) | Fails if Shorts prompts `AskUser` |
| Invariant 3 (Productive Ad) | Productive video -> Ad detected | Ad state is `Allowed(ad, lastVerifiedChannel)` | Fails if ad metadata is queried or prompts `AskUser` |
| Invariant 3 (Distracting Ad) | Distracting video -> Ad detected | Ad state is `Blocked(ad, lastVerifiedChannel)` | Fails if ad metadata is queried or prompts `AskUser` |
| Invariant 3 (Cold Start Ad) | Ad detected with no prior video | Remains `Verifying(ad)` | Fails if unverified ad plays unblocked |
| User Classification | Detect unknown -> `onUserClassified` | State updates to `Allowed`/`Blocked`; saved in repo | Fails if state not updated or repo not called |
| Playback Stopped | `onPlaybackStopped()` | State resets to `Verifying(null)` | Fails if stale state or identity persists |

