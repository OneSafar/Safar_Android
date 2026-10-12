# Architectural Survey & Handoff Report: Pillar 5 & Test Suite Architecture (Phases 9 & 10 + Build/Test Suite Verification)

**Agent**: Survey Explorer 3 (`teamwork_preview_explorer_survey_3_orch2`)  
**Date**: 2026-09-11  
**Project**: Safar Android (`com.safarparmar.app`)  
**Working Directory**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_3_orch2`  
**Authoritative Reference**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md` (specifically 15-phase plan under `## 2026-09-11T15:25:10Z`)

---

## 1. Observation

### 1.1 Service Lifecycle & Foreground Exit Detection (`YouTubeGuardService.kt` & XML Config)

#### 1.1.1 Event Filtering Bottleneck (`packageNames` Restriction)
In `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/src/main/res/xml/youtube_guard_service.xml` (lines 4–11):
```xml
    android:accessibilityEventTypes="typeWindowContentChanged|typeWindowStateChanged|typeViewScrolled"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagRetrieveInteractiveWindows|flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="true"
    android:description="@string/youtube_guard_service_description"
    android:notificationTimeout="300"
    android:packageNames="com.google.android.youtube"
    android:settingsActivity="com.safarparmar.app.MainActivity" />
```
And in `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt` (lines 44–54):
```kotlin
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            packageNames = arrayOf(YOUTUBE_PACKAGE)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = EVENT_DEBOUNCE_MS
        }
```
**Observation**: When `packageNames` is declared as `com.google.android.youtube` in XML and code, Android's system `AccessibilityManagerService` filters events at the OS level. The service **never** receives `TYPE_WINDOW_STATE_CHANGED` events when the user presses Home, taps Recent Apps, opens the Notification shade, or launches another app (e.g. Launcher, Settings, Browser).

#### 1.1.2 Broken Exit Handler in `onAccessibilityEvent`
In `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt` (lines 63–67):
```kotlin
        // If an event occurs outside YouTube, hide the overlay and stop verification
        if (event.packageName?.toString() != YOUTUBE_PACKAGE) {
            return
        }
```
**Observation**: Even if Android delivered an event from outside YouTube, the implementation does the exact opposite of its comment: it executes an early `return` without calling `overlayManager.hide()`, without calling `policyEngine.onPlaybackStopped()`, and without cancelling any running detection jobs. Consequently, any active overlay remains permanently displayed over external applications.

#### 1.1.3 Untracked Asynchronous Detection Coroutines
In `YouTubeGuardService.kt` (lines 68–100):
```kotlin
        serviceScope.launch(Dispatchers.Default) {
            val root = rootInActiveWindow
            if (root == null) {
                Log.d(TAG, "rootInActiveWindow is null — skipping event")
                return@launch
            }
            ...
```
**Observation**: The coroutine is launched directly on `serviceScope` without storing a `Job` reference. When rapid accessibility events occur, multiple concurrent parsing coroutines run simultaneously on `Dispatchers.Default`. There is no handle to cancel pending or in-flight detection when the user exits YouTube.

#### 1.1.4 Overlay Suppression During `Verifying` State
In `YouTubeGuardService.kt` (lines 134–136):
```kotlin
            // Verifying is an internal ~2ms DB check state — do not flash overlay
            is PolicyState.Verifying -> overlayManager.hide()
```
**Observation**: When `PolicyState.Verifying` is entered, the service explicitly hides the overlay instead of displaying a touch-consuming blocking overlay. This directly violates Phase 1 / Requirement R1 ("Introduce a full-screen, touch-consuming OverlayState.Verifying overlay... Ensure newly opened or changed YouTube videos never become interactable before channel identity and policy are resolved").

---

### 1.2 Service Lifecycle & Callback Leaks (`YouTubeNotificationListener.kt`)

In `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeNotificationListener.kt`:

#### 1.2.1 Irrecoverable Coroutine Scope Cancellation
Lines 33, 74–80:
```kotlin
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    ...
    override fun onListenerDisconnected() {
        mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        activeController?.unregisterCallback(mediaCallback)
        activeController = null
        listenerScope.cancel()
        super.onListenerDisconnected()
    }
```
**Observation**: `listenerScope` is initialized once as a final property (`val`). Calling `listenerScope.cancel()` in `onListenerDisconnected()` permanently cancels its underlying `SupervisorJob`. In Android, `NotificationListenerService` can be reconnected by the system without recreating the service process (e.g. toggling Notification Access in Settings, temporary system resource reclamation, Bluetooth reconnects). When `onListenerConnected()` is invoked on the existing instance, `listenerScope` remains cancelled; all subsequent `listenerScope.launch { ... }` calls in `onPlaybackStateChanged` (line 50) and `handleMetadataChange` (line 126) silently fail to launch.

#### 1.2.2 Missing `onDestroy()` Teardown & Callback Leaks
In lines 57–80:
- `sessionListener` is registered via `mediaSessionManager?.addOnActiveSessionsChangedListener(sessionListener, cn)` in `onListenerConnected()` (line 68).
- `activeController?.registerCallback(mediaCallback)` is registered in `updateActiveController` (line 99).
- `onDestroy()` is **NOT overridden** in `YouTubeNotificationListener`.
**Observation**: Android services can be destroyed directly by the system via `onDestroy()` without `onListenerDisconnected()` being invoked. When this happens:
1. `sessionListener` remains registered in `MediaSessionManager`, retaining a strong reference to the destroyed service instance (Memory Leak).
2. `mediaCallback` remains registered with the active `MediaController`, leaking the callback and service.
3. Multiple calls to `onListenerConnected()` without an intervening `onListenerDisconnected()` register duplicate `sessionListener` instances.

#### 1.2.3 Stale `lastTitle` Cache Across Sessions
Lines 36, 110–113:
```kotlin
    private var lastTitle: String = ""
    ...
    // Avoid duplicate processing of the same title
    if (title == lastTitle) return
    lastTitle = title
```
**Observation**: `lastTitle` is never reset when `activeController` becomes null, when playback stops (`onPlaybackStopped()`), or when the listener disconnects. If a user was watching "Lecture 1", exits YouTube, and later reopens "Lecture 1", the metadata change event is completely dropped because `title == lastTitle` evaluates to true, preventing detection.

---

### 1.3 Overlay Lifecycle & Window Management (`YouTubeGuardOverlayManager.kt` & `YouTubeGuardOverlay.kt`)

In `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlayManager.kt` (lines 176–200):
```kotlin
class SimpleLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun start() {
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
```
**Observation**:
1. `SimpleLifecycleOwner` transitions directly from `INITIALIZED` to `RESUMED`, skipping standard `CREATED` and `STARTED` lifecycle stages expected by AndroidX Compose trees.
2. In `YouTubeGuardOverlay.kt` (lines 24–44), `OverlayState` only defines `Hidden`, `Blocked`, `ShortsBlocked`, and `AskUser`. `OverlayState.Verifying` does not exist in production code.
3. In `YouTubeGuardOverlayManager.kt`, there is no `showVerifying(...)` method.
4. WindowManager layout parameters use `FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN` (lines 156–157). When touch-consuming verification is required (Phase 1), `FLAG_NOT_TOUCHABLE` must not be added, and the full-screen view must consume touch events.

---

### 1.4 Test Suite Survey & Build Verification

#### 1.4.1 Build and Compilation Command Execution
Command verified in local shell:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest
```
**Result**:
- Exit code: `0` (`BUILD SUCCESSFUL in 21s`)
- Tasks executed: 36 actionable tasks (24 executed, 12 up-to-date)
- Zero build warnings or compilation errors preventing execution.
- Specific YouTube package execution:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.*"
```
**Result**: `BUILD SUCCESSFUL in 1s` (0 failures, 114 tests passed).

#### 1.4.2 Existing YouTube Test Inventory

| Test Class Path | Test Count | Pass/Fail | Scope & Characteristics |
|---|---|---|---|
| `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt` | 35 | 35 Pass, 0 Fail | Pure JVM tests with `FakeYouTubeChannelRepository` and Turbine. Tests `YouTubePolicyEngine` against `VideoIdentity` (title, handle, name, isShorts, isAd). Tests basic Invariants 1, 2, 3, deduplication, and active job cancellation. |
| `app/src/test/java/com/safarparmar/app/feature/youtube/e2e/YouTubeGuardE2ETest.kt` | 79 | 79 Pass, 0 Fail | Pure JVM opaque-box test suite using internal simulator `YouTubeGuardSystemHarness`. Covers Tiers 1–4 across simulated components (`E2EYouTubePolicyEngine`, `E2EInMemoryChannelRepository`, `E2EOverlayController`). |
| `app/src/test/java/com/safarparmar/app/data/local/YouTubePersistenceAdversarialTest.kt` | 15 | 15 Pass, 0 Fail | Tests Room `ChannelPolicyConverter`, mocked `YouTubeChannelDao`, and `YouTubeChannelRepositoryImpl`. Tests handle normalization with synthetic `@` prefixing and concurrency. |
| **Total YouTube Tests** | **129** | **129 Pass, 0 Fail** | **100% Passing** |

#### 1.4.3 Entire Project Test Inventory
Total 26 test suite XML files in `app/build/test-results/testQaDebugUnitTest/`:
- `com.safarparmar.app.data.local.YouTubePersistenceAdversarialTest`: 15 tests
- `com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest`: 35 tests
- `com.safarparmar.app.feature.youtube.e2e.YouTubeGuardE2ETest`: 79 tests
- Core project feature tests (`LiveSessionViewModelTest`, `AuthViewModelTest`, `EkagraTimeAccountingTest`, `StudyPlannerProgressTest`, etc.): 72+ tests
- **Grand Total**: 201+ tests, 0 failures, 0 errors, 0 flaky tests.

---

### 1.5 Detailed Test Coverage Gap Analysis Across All 15 Phases

| Phase | Phase Name | Existing Coverage Status | Concrete Missing Test Cases (Gaps) | Severity / Priority |
|:---|:---|:---|:---|:---|
| **Phase 1** | Fail-Closed Verifying Overlay | Simulated in `YouTubeGuardE2ETest` (E2EOverlayController). Production code has 0 coverage. | 1. `YouTubeGuardOverlayManager` renders `OverlayState.Verifying`.<br>2. `YouTubeGuardService` triggers `showVerifying` on `PolicyState.Verifying`.<br>3. Verifying overlay consumes touch events and prevents background clicks. | **High** |
| **Phase 2** | Canonical Channel Identity | None. Existing tests use raw strings and synthetic `@` handles. | 1. `ChannelIdentity` with `verifiedHandle: String?` (no fake handle).<br>2. Canonical key generation (`handle:<handle>` vs `name:<normalized_name>`).<br>3. Prevention of synthesized handles from display names. | **High** |
| **Phase 3** | Structural Subtree Parsing | Zero tests for `YouTubeSnapshotParser`. | 1. Subtree isolation for watch metadata vs recommendation cards.<br>2. Active video player subtree extraction vs suggested feed.<br>3. Channel header subtree parsing without comment pollution. | **High** |
| **Phase 4** | Unified Detection & Fusion Layer | Class does not exist yet (`YouTubeContentDetector`). Zero tests. | 1. Fusing Accessibility tree and MediaSession metadata into `CanonicalYouTubeContent`.<br>2. Confidence score calculation (`HIGH`, `MEDIUM`, `LOW`, `UNKNOWN`).<br>3. Source conflict resolution (Accessibility handle vs MediaSession title). | **High** |
| **Phase 5** | Video Fingerprinting & Deduplication | Basic title-based dedupe in `YouTubePolicyEngineTest`. | 1. `VideoFingerprint` prioritization of `mediaId`.<br>2. Normalized title + canonical channel key fallback.<br>3. MediaSession deduplication via fingerprint rather than raw title string. | **High** |
| **Phase 6** | Fullscreen & Autoplay Transitions | Simulated in `YouTubeGuardE2ETest` (Scenario 4). | 1. Autoplay next video immediately triggers `Verifying` before resolving.<br>2. Switching orientation / fullscreen transitions re-enter `Verifying`.<br>3. Rapid consecutive autoplay transitions never leak unverified frames. | **Medium** |
| **Phase 7** | Shorts Structural Detection & Nav Interception | Basic policy tests in `YouTubePolicyEngineTest`. Zero parser/service tests. | 1. `TYPE_VIEW_CLICKED` interception on bottom navigation Shorts tab.<br>2. Structural reel player detection without relying on global string search.<br>3. Remix button and reel timebar detection. | **High** |
| **Phase 8** | Event Stream & Concurrency Serialization | Direct synchronous calls in existing classes. Zero event stream tests. | 1. Concurrency serialization via Kotlin Flow / Channel.<br>2. Burst of 20 concurrent accessibility + media events processed in strict FIFO order.<br>3. Buffer overflow and backpressure handling. | **Medium** |
| **Phase 9** | **Foreground Detection & Overlay Dismissal** | **Zero tests in production service.** | 1. `TYPE_WINDOW_STATE_CHANGED` from Launcher dismisses overlay immediately.<br>2. External app switch cancels active detection job.<br>3. Window disappearance / app exit notifies `PolicyEngine.onPlaybackStopped()`.<br>4. IME / SystemUI window events do not inadvertently dismiss overlay while YouTube is active. | **Critical (Pillar 5 Focus)** |
| **Phase 10** | **Service Lifecycle & Leak Cleanup** | **Zero tests for `YouTubeNotificationListener`.** | 1. CoroutineScope is recreatable across `onListenerDisconnected()` and `onListenerConnected()`.<br>2. `MediaController.Callback` is unregistered on active controller change.<br>3. `MediaController.Callback` and `sessionListener` are unregistered in `onDestroy()`.<br>4. Stale `lastTitle` / `lastFingerprint` cache is cleared on disconnect. | **Critical (Pillar 5 Focus)** |
| **Phase 11** | Ad Policy Retention & Video Switch | Good coverage for ad during known video in `YouTubePolicyEngineTest`. | 1. Video switch into an ad with a different fingerprint stays in `Verifying` (fail-closed).<br>2. Cold start into ad with no prior video fails closed.<br>3. Advertiser handle is never stored or looked up in Room. | **Medium** |
| **Phase 12** | Stale Lookup & Generation Tokens | Basic job cancellation in `YouTubePolicyEngine`. Zero generation token tests. | 1. Delayed Room query from generation $N$ cannot overwrite state if generation $N+1$ has already started.<br>2. Generation token increments monotonically on every video transition. | **High** |
| **Phase 13** | Edge Cases: Cold Start & Rapid Switching | Basic tests in `YouTubePolicyEngineTest`. | 1. Cold launch directly into distracting video blocks without flashing Allowed.<br>2. Rapid video switching (10 videos in 100ms) leaves only the final video active.<br>3. Unparseable UI tree fails closed in `Verifying`. | **Medium** |
| **Phase 14** | Persistence without Fake Handles | `YouTubePersistenceAdversarialTest` actively tests legacy fake `@` prepending. | 1. Room entity stores `verifiedHandle: String?` and `displayName: String`.<br>2. Query by canonical key (`handle:<handle>` or `name:<normalized_name>`).<br>3. Updating entity does not fabricate `@` prefix when none exists. | **High** |
| **Phase 15** | Suggested Video Clicks & Channel Pages | None. | 1. Tapping suggested video enters `Verifying` immediately.<br>2. Channel page browsing is recognized as non-playback (`isChannelPage = true`) and does not block channel exploration. | **Medium** |

---

## 2. Logic Chain

### 2.1 Logic Chain for Phase 9: Global Window Monitoring, Foreground Exit & Overlay Dismissal

1. **Premise**: Under Android's Accessibility framework, an `AccessibilityService` that specifies `packageNames = ["com.google.android.youtube"]` is only notified of events where `AccessibilityEvent.getPackageName() == "com.google.android.youtube"`. (Directly observed in `youtube_guard_service.xml`:10 and `YouTubeGuardService.kt`:48).
2. **Inference**: When a user presses Home or switches apps, the foreground package becomes `com.google.android.apps.nexuslauncher`, `com.android.launcher3`, or another app. Because of the package filter, `onAccessibilityEvent` receives **zero** notifications of this transition.
3. **Inference**: Even if an event were received, `YouTubeGuardService.kt`:64–66 executes an immediate `return`, failing to dismiss the overlay or stop policy enforcement.
4. **Consequence**: The Compose overlay (`TYPE_ACCESSIBILITY_OVERLAY`) remains rendered on screen on top of the launcher and third-party apps, trapping the user.
5. **Architectural Resolution**:
   - **Step 1**: In `youtube_guard_service.xml` and `YouTubeGuardService.onServiceConnected()`, remove package filtering (`packageNames = null`) to allow reception of `TYPE_WINDOW_STATE_CHANGED` events system-wide.
   - **Step 2**: Introduce a dedicated `YouTubeForegroundTracker` component:
     - On `TYPE_WINDOW_STATE_CHANGED`:
       - If `event.packageName == YOUTUBE_PACKAGE`: Record `isForeground = true`.
       - If `event.packageName != YOUTUBE_PACKAGE`:
         - Check if the event originates from a non-interactive system overlay or IME (e.g. `android.inputmethodservice`, system volume slider, or Safar's own package). If so, ignore.
         - If from another application or launcher:
           - Record `isForeground = false`.
           - **Immediately invoke `overlayManager.hide()`**.
           - **Cancel `activeDetectionJob?.cancel()`**.
           - **Invoke `policyEngine.onPlaybackStopped()`**.
           - **Reset `lastSnapshot = YouTubeSnapshot.EMPTY`**.
   - **Step 3**: On every accessibility event received within YouTube, cancel the previous `activeDetectionJob` before launching the new parsing job, preventing race conditions.

---

### 2.2 Logic Chain for Phase 10: Recreatable Scope & Deterministic Callback Cleanup

1. **Premise**: In Kotlin Coroutines, calling `cancel()` on a `CoroutineScope` permanently cancels its underlying `Job`. Once cancelled, subsequent calls to `scope.launch` do not execute. (Directly observed in `YouTubeNotificationListener.kt`:33, 78).
2. **Premise**: Android's `NotificationListenerService` can experience multiple `onListenerConnected()` and `onListenerDisconnected()` cycles during the life of its host process (e.g. user toggles permissions, system power management rebinds).
3. **Inference**: After the first disconnect, any subsequent reconnection leaves `listenerScope` dead. Future YouTube playback events received via MediaSession will fail to notify `YouTubePolicyEngine`.
4. **Consequence**: Fullscreen fallback detection silently breaks after any notification listener reconnection.
5. **Architectural Resolution**:
   - Manage the CoroutineScope dynamically with lifecycle methods:
     ```kotlin
     private var listenerJob: CompletableJob? = null
     private var listenerScope: CoroutineScope? = null

     private fun ensureScope(): CoroutineScope {
         val scope = listenerScope
         if (scope != null && scope.isActive) return scope
         val job = SupervisorJob()
         listenerJob = job
         val newScope = CoroutineScope(job + Dispatchers.Main.immediate)
         listenerScope = newScope
         return newScope
     }

     private fun cancelScope() {
         listenerJob?.cancel()
         listenerJob = null
         listenerScope = null
     }
     ```
   - In `onListenerConnected()`: Invoke `ensureScope()`.
   - In `onListenerDisconnected()` and `onDestroy()`: Invoke `cancelScope()`.

6. **Premise**: `YouTubeNotificationListener` registers callbacks with `MediaSessionManager` and `MediaController`, but does not implement `onDestroy()` (Observed in `YouTubeNotificationListener.kt`:57–80).
7. **Inference**: If the service process is destroyed by the system without an orderly `onListenerDisconnected()`, strong references to `this` remain inside `MediaSessionManager`, causing an OS service leak.
8. **Architectural Resolution**:
   - Implement a unified `teardown()` method called from both `onListenerDisconnected()` and `onDestroy()`:
     - Unregister `sessionListener` via `mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)`.
     - Guard `sessionListener` registration with a boolean flag `isSessionListenerRegistered` to prevent duplicate registrations on multiple `onListenerConnected()` invocations.
     - Unregister `mediaCallback` from `activeController` and null out `activeController`.
     - Clear `lastTitle = ""` and `lastFingerprint = null` to prevent duplicate suppression bugs when restarting the same video.

---

### 2.3 Logic Chain for Test Suite Architecture & Verification Strategy

1. **Premise**: The existing `YouTubeGuardE2ETest.kt` (79 tests) executes against an opaque-box simulator (`E2EYouTubePolicyEngine`, `E2EOverlayController`, `E2EInMemoryChannelRepository`), and `YouTubePolicyEngineTest.kt` (35 tests) tests legacy `VideoIdentity` strings.
2. **Observation**: Zero unit tests currently exist for:
   - `YouTubeGuardService` (event dispatching, foreground tracking, overlay dismiss).
   - `YouTubeNotificationListener` (reconnect lifecycle, coroutine scope recreation, callback leak prevention).
   - `YouTubeSnapshotParser` / `YouTubeUiParser` (node tree structural subtree parsing).
3. **Inference**: While the simulator proves the architectural invariants work in theory, the real Android service and parser code in `app/src/main/` have critical bugs that are currently masked because they lack direct component unit tests.
4. **Architectural Blueprint for New Test Suites**:
   - **`YouTubeForegroundDetectorTest.kt`**:
     - Unit-test foreground window transition logic using synthetic `AccessibilityEvent`s without requiring a live device.
     - Verify overlay hides immediately upon receiving `TYPE_WINDOW_STATE_CHANGED` for non-YouTube packages.
     - Verify active detection jobs are cancelled immediately upon exit.
   - **`YouTubeNotificationListenerLifecycleTest.kt`**:
     - Unit-test lifecycle reconnection cycles: verify coroutines launch successfully after `onListenerConnected()` -> `onListenerDisconnected()` -> `onListenerConnected()`.
     - Verify `MediaController.Callback` is unregistered upon controller changes and service destruction.
     - Verify duplicate session listener registration is prevented.
     - Verify `lastTitle` / `lastFingerprint` cache is cleared on disconnect.
   - **Integration with Gradle**:
     - All new test suites will execute under the existing fast JVM command:
       `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`

---

## 3. Caveats

1. **JVM Unit Testing of Android Accessibility Services**:
   - `AccessibilityService` and `NotificationListenerService` are abstract framework classes tied to Android system IPC. In standard JVM tests (`testQaDebugUnitTest`), methods like `rootInActiveWindow`, `performGlobalAction`, and `getSystemService` return null unless mocked or run with Robolectric.
   - *Mitigation*: Decouple the core logic into testable pure Kotlin collaborators:
     - Extract foreground detection into a pure `YouTubeForegroundTracker` class.
     - Extract MediaSession session handling into a testable `YouTubeMediaSessionCoordinator`.
     - This allows 100% JVM unit test coverage without device/emulator dependencies.
2. **Global Event Performance & Battery Impact**:
   - Listening to `TYPE_WINDOW_STATE_CHANGED` globally across all applications introduces a potential event stream from every app launch.
   - *Mitigation*: Set `notificationTimeout = 300L` and strictly filter out all non-`TYPE_WINDOW_STATE_CHANGED` events for packages outside YouTube at the very top of `onAccessibilityEvent`. Only YouTube receives `TYPE_WINDOW_CONTENT_CHANGED` and `TYPE_VIEW_SCROLLED` handling.
3. **Adversarial Persistence Test Refactoring**:
   - `YouTubePersistenceAdversarialTest.kt` contains test assertions that mandate synthetic `@` prefix generation (e.g. `saveChannelPolicy prepends at-sign when handle lacks prefix`). When Phase 2 & 14 are implemented, these assertions must be updated to avoid test failures.

---

## 4. Conclusion

1. **Pillar 5 (Phases 9 & 10) Flaws Identified**:
   - **Phase 9 (Foreground Detection)**: Critically broken. Package filtering prevents receiving exit events; exit code returns early without dismissing overlay; active coroutine jobs are untracked and never cancelled.
   - **Phase 10 (Service Lifecycle & Leaks)**: Critically broken. `listenerScope.cancel()` permanently bricks the coroutine scope on disconnect; `onDestroy()` is missing, leaking `sessionListener` and `mediaCallback` in `MediaSessionManager`; `lastTitle` cache is never invalidated.
2. **Build and Test Harness Health**:
   - Gradle build and test execution is 100% operational:
     `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`
   - Runs in ~21s cold, ~1s warm, executing 201+ tests with 0 failures across 26 test suites.
3. **Test Gap Analysis**:
   - A complete 15-phase test gap matrix has been cataloged. The primary vulnerability is that existing YouTube tests are focused on the simulator (`YouTubeGuardE2ETest`) or policy engine (`YouTubePolicyEngineTest`), while production service lifecycle, parser, and detector layers currently have 0 unit tests.
   - Implementing decoupled components (`YouTubeForegroundTracker`, `YouTubeMediaSessionCoordinator`) will allow comprehensive, deterministic JVM testing for Phases 9 & 10.

---

## 5. Verification Method

### 5.1 Verifying Current Full Project Test Suite
Run the standard test runner from repository root:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android
./gradlew :app:testQaDebugUnitTest
```
**Expected Result**: `BUILD SUCCESSFUL`, 201+ tests passing, 0 failures, 0 errors.

### 5.2 Verifying YouTube Feature Tests Specifically
Run only the YouTube package unit and E2E tests:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android
./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.*"
```
**Expected Result**: `BUILD SUCCESSFUL`, 114 tests passing (`YouTubePolicyEngineTest`: 35, `YouTubeGuardE2ETest`: 79).

### 5.3 Verifying YouTube Persistence Adversarial Suite
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
cd /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android
./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.data.local.YouTubePersistenceAdversarialTest"
```
**Expected Result**: `BUILD SUCCESSFUL`, 15 tests passing.

### 5.4 Files to Inspect
- Service lifecycle & foreground detection: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`
- Media session lifecycle & callback cleanup: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeNotificationListener.kt`
- Overlay lifecycle: `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlayManager.kt`
- Accessibility config: `app/src/main/res/xml/youtube_guard_service.xml`
- Existing test suites:
  - `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt`
  - `app/src/test/java/com/safarparmar/app/feature/youtube/e2e/YouTubeGuardE2ETest.kt`
  - `app/src/test/java/com/safarparmar/app/data/local/YouTubePersistenceAdversarialTest.kt`

### 5.5 Invalidation Conditions
This survey and blueprint will be invalidated if:
1. Android OS alters `AccessibilityService` security boundaries preventing multi-package window monitoring without user prompts.
2. AGP or Kotlin version updates modify coroutine test dispatching semantics.
3. YouTube app is rewritten to bypass Android `MediaSession` APIs entirely for fullscreen metadata.
