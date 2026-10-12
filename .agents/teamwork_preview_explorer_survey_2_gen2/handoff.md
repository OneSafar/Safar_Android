# Architectural Survey Report: YouTubeGuardService, MediaSessionManager & Compose Accessibility Overlay

**Author**: Survey Explorer 2 (Gen 2)  
**Date**: 2026-09-11  
**Target Path**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_gen2/handoff.md`  
**Working Directory**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_2_gen2`  
**Project Root**: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android`  

---

## 1. Observation

### 1.1 AndroidManifest.xml: Permissions and Existing Services
From inspection of `app/src/main/AndroidManifest.xml` (lines 5–15, lines 71–105):
- **Existing Permissions Declared**:
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
  <uses-permission android:name="android.permission.VIBRATE" />
  <uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
  <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
  ```
- **Existing Services Declared**:
  1. `SafarFirebaseMessagingService` (`.notifications.SafarFirebaseMessagingService`, exported=false, `com.google.firebase.MESSAGING_EVENT`)
  2. `TimerService` (`.ui.ekagra.TimerService`, foregroundServiceType="mediaPlayback", exported=false)
  3. `AppLocalesMetadataHolderService` (`androidx.appcompat.app.AppLocalesMetadataHolderService`, enabled=false, exported=false)
- **Service Declarations Currently Missing**:
  - `YouTubeGuardService` (AccessibilityService) is not declared.
  - `YouTubeNotificationListener` (NotificationListenerService) is not declared.
- **Historical Git Evidence for Service Declarations**:
  In Git commit `aca5b502` (`feat: add YouTube Study Mode V2...`), the accessibility service was declared as:
  ```xml
  <service
      android:name=".feature.youtubestudyv2.YoutubeStudyV2AccessibilityService"
      android:exported="true"
      android:label="@string/youtube_study_v2_accessibility_label"
      android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
      <intent-filter>
          <action android:name="android.accessibilityservice.AccessibilityService" />
      </intent-filter>
      <meta-data
          android:name="android.accessibilityservice"
          android:resource="@xml/youtube_study_v2_accessibility_service" />
  </service>
  ```
  In Git commit `db9130b9` (`UI tweaks and Ekagra gradient solid background updates`), the notification listener was declared as:
  ```xml
  <service
      android:name=".ui.ekagra.focusshield.FocusShieldNotificationListenerService"
      android:exported="true"
      android:label="@string/focus_shield_notification_listener_label"
      android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
      <intent-filter>
          <action android:name="android.service.notification.NotificationListenerService" />
      </intent-filter>
  </service>
  ```

---

### 1.2 AccessibilityService XML Configuration & Parsing (`YouTubeGuardService`)
From inspection of git history (`git show HEAD:app/src/main/res/xml/youtube_study_v2_accessibility_service.xml`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged|typeWindowContentChanged|typeViewClicked"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagReportViewIds|flagIncludeNotImportantViews"
    android:canRetrieveWindowContent="true"
    android:canPerformGestures="false"
    android:description="@string/youtube_study_v2_accessibility_description"
    android:isAccessibilityTool="false"
    android:notificationTimeout="100"
    android:packageNames="com.google.android.youtube" />
```

Key observations on attributes:
- `android:packageNames="com.google.android.youtube"`: Restricts the OS from dispatching accessibility events from any app other than YouTube, optimizing battery and CPU.
- `android:accessibilityFlags="flagReportViewIds|flagIncludeNotImportantViews"`:
  - `flagReportViewIds` is mandatory; without it, `AccessibilityNodeInfo.viewIdResourceName` returns `null`.
  - `flagIncludeNotImportantViews` enables inspecting container and layout nodes that wrap titles and channel headers.
- `android:accessibilityEventTypes`: `typeWindowStateChanged` (screen/dialog change), `typeWindowsChanged` (window configuration change), `typeWindowContentChanged` (dynamic text/video content changes), `typeViewClicked` (video card and tab clicks).
- `android:notificationTimeout="100"`: 100ms throttle between event deliveries.

From historical parser code (`git show HEAD:app/src/main/java/com/safarparmar/app/feature/youtubestudyv2/YoutubeStudyV2Parser.kt`):
- **YouTube Screen & View Markers**:
  - Watch Player Containers: `watch_player`, `movie_player`, `fullscreen_player`, `reel_watch`
  - Miniplayer containers (to ignore or treat as preview): `modern_miniplayer`, `miniplayer_container`
  - Video Titles: `video_title`, `watch_title`, `title_text`
  - Owner / Channel Rows: `video_owner`, `owner_row`, `channel_row`, `channel_info`
  - Shorts Containers: `reel_watch`, `shorts_player`, `shorts_video`
  - Ad Playback Markers: `visit advertiser`, `skip ad`, `stop ad`, `ad countdown`, `ad badge`
  - Uploader Handle Matching: Regex `@[\\p{L}\\p{N}_.-]{3,30}`
  - Channel Avatar Proximity: ImageViews with width/height in 20dp–88dp, horizontal gap -8dp to 64dp from owner TextView, and shared parent or grandparent containers.

---

### 1.3 MediaSessionManager Integration (`YouTubeNotificationListener`)
- Direct Android Framework API investigation:
  - Class: `android.media.session.MediaSessionManager`
  - Method: `addOnActiveSessionsChangedListener(listener, notificationListenerComponentName)`
  - Constraint: `addOnActiveSessionsChangedListener` requires a non-null `ComponentName` pointing to an enabled `NotificationListenerService`. Passing any other component or calling without an enabled notification listener throws `SecurityException: Only an enabled notification listener may access active sessions`.
- MediaController Metadata Keys:
  - When YouTube plays media, its `MediaController` publishes `MediaMetadata`:
    - `MediaMetadata.METADATA_KEY_TITLE` (String): YouTube video title.
    - `MediaMetadata.METADATA_KEY_ARTIST` (String): YouTube channel / creator name.
    - `MediaMetadata.METADATA_KEY_ALBUM_ARTIST` (String): Secondary channel descriptor.
  - Active Callback: `MediaController.Callback.onMetadataChanged(MediaMetadata?)` fires immediately on video identity changes.
  - Control Callback: `MediaController.transportControls.pause()` can directly pause playback without faking hardware key events.

---

### 1.4 Compose Overlay & WindowManager Implementation (`TYPE_ACCESSIBILITY_OVERLAY`)
From inspection of existing project code (`app/src/main/java/com/safarparmar/app/ui/ekagra/TimerBubbleOverlay.kt`, lines 285–294 and 434–456):
- In `TimerBubbleOverlay.kt`:
  ```kotlin
  private class BubbleLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
      private val lifecycleRegistry = LifecycleRegistry(this)
      private val savedStateController = SavedStateRegistryController.create(this)
      override val lifecycle: Lifecycle get() = lifecycleRegistry
      override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

      fun onCreate() { savedStateController.performRestore(null); lifecycleRegistry.currentState = Lifecycle.State.CREATED }
      fun onResume() { lifecycleRegistry.currentState = Lifecycle.State.RESUMED }
      fun onDestroy() { lifecycleRegistry.currentState = Lifecycle.State.DESTROYED }
  }
  ```
- Also from `app/src/main/java/com/safarparmar/app/performance/MotionPolicy.kt` (lines 59–63):
  ```kotlin
  @Composable
  fun decorativeMotionEnabled(): Boolean {
      val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
      return LocalMotionPolicy.current.decorationsEnabled && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
  }
  ```
  This proves that `SafarTheme` (which calls `ProvideMotionPolicy`) strictly requires `LocalLifecycleOwner.current` to be set and progressed to `Lifecycle.State.RESUMED`, or decorative motions fail and missing owners cause runtime crashes.
- In `KavachBlockOverlay.kt` (lines 135–150):
  The project previously supported switching overlay types:
  ```kotlin
  if (accessibilityOverlay)
      WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
  else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
  ```
- Overlay Window Flags:
  - `FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS` for true edge-to-edge drawing over status bar and navigation bar.
  - Do NOT set `FLAG_NOT_FOCUSABLE` or `FLAG_NOT_TOUCHABLE` so the overlay consumes all touches, blocking interaction with YouTube underneath and allowing user interaction with overlay buttons.

---

## 2. Logic Chain

### 2.1 Manifest & Service Declaration Architecture
1. **Observation 1.1**: The project does not currently declare `YouTubeGuardService` or `YouTubeNotificationListener` in `app/src/main/AndroidManifest.xml`.
2. **Observation 1.1 & 1.3**: `MediaSessionManager.addOnActiveSessionsChangedListener(listener, notificationListenerComponentName)` requires an enabled `NotificationListenerService`.
3. **Deduction**: Both services must be added to `AndroidManifest.xml`:
   - `YouTubeGuardService` must declare `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"` and `android:exported="true"`, with the accessibility action intent-filter and meta-data pointing to `res/xml/youtube_guard_accessibility_service.xml`.
   - `YouTubeNotificationListener` must declare `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"` and `android:exported="true"`, with the notification listener action intent-filter.
   - Neither permission is a `<uses-permission>` request; both are component protection permissions ensuring only the Android system framework can bind to them.

### 2.2 Dual Detection: AccessibilityService + MediaSessionManager
1. **Observation 1.2 & 1.3**: When a user watches YouTube in standard portrait mode, `YouTubeGuardService` can parse the accessibility tree nodes (`video_title`, `owner_row`, `@handle`, `reel_watch`).
2. **Observation 1.3**: When YouTube transitions to fullscreen or when the player controls autohide after 3 seconds, UI text nodes disappear or become invisible to user. At that exact moment, `MediaSessionManager`'s active session controller still retains authoritative metadata (`METADATA_KEY_TITLE`, `METADATA_KEY_ARTIST`).
3. **Acceptance Criteria R1 & R2**:
   - `YouTubePolicyEngine` must manage state transitions: `Verifying`, `AskUser`, `Allowed`, `Blocked`, `ShortsBlocked`.
   - System MUST default to `Verifying` on any video identity change, never temporarily allowing unknown content.
   - Ad transitions retain the previous verified channel's policy instead of evaluating the ad's metadata.
   - Shorts are evaluated and blocked independently of channel policy.
4. **Deduction**:
   - `YouTubeGuardService` (Accessibility) and `YouTubeNotificationListener` (MediaSession) should report identity events to a shared singleton `YouTubeStateCoordinator` / `YouTubePolicyEngine`.
   - When a video identity changes (via accessibility event or `MediaController.Callback.onMetadataChanged`), the policy engine immediately emits `Verifying`.
   - If `adPlaying` is detected (via `visit advertiser`, `skip ad`, or ad countdown markers), the engine does NOT treat the ad as a new channel; it preserves the previous video's policy.
   - If `reel_watch` or `shorts_player` is detected, the engine transitions immediately to `ShortsBlocked`, regardless of channel.

### 2.3 Compose Blocking Overlay via TYPE_ACCESSIBILITY_OVERLAY
1. **Observation 1.4**: `AccessibilityService` has direct access to `getSystemService(WINDOW_SERVICE) as WindowManager` and can add windows of type `TYPE_ACCESSIBILITY_OVERLAY`.
2. **Observation 1.4**: `TYPE_ACCESSIBILITY_OVERLAY` is granted to accessibility services by the OS without requiring the `SYSTEM_ALERT_WINDOW` permission or launching background activities, bypassing Background Activity Launch (BAL) restrictions.
3. **Observation 1.4**: Jetpack Compose `ComposeView` expects:
   - `ViewTreeLifecycleOwner` (must reach `Lifecycle.State.RESUMED` for `SafarTheme` and `ProvideMotionPolicy`)
   - `ViewTreeViewModelStoreOwner` (for ViewModel resolution and Compose view hierarchy support)
   - `ViewTreeSavedStateRegistryOwner` (for `rememberSaveable` to avoid runtime crashes)
4. **Deduction**:
   - Create a `ComposeServiceLifecycleOwner` implementing `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`.
   - Bind it to `ComposeView` using `setViewTreeLifecycleOwner`, `setViewTreeViewModelStoreOwner`, and `setViewTreeSavedStateRegistryOwner`.
   - Configure `ComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)`.
   - Inflate Compose content with `SafarTheme` and a reactive state machine UI:
     - `AskUser`: displays channel name/handle, video title, and [Productive] / [Distracting] buttons.
     - `Blocked`: displays blocked notice, motivating copy, and [Go Home] button.
     - `ShortsBlocked`: displays shorts blocked notice and [Return to Videos] button.

---

## 3. Caveats

1. **Android Studio JDK Requirement**:
   - Running `./gradlew` from the CLI requires setting `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`, as system `/usr/libexec/java_home` does not detect a default JDK.
2. **User Permission Granting**:
   - While `TYPE_ACCESSIBILITY_OVERLAY` does not require `Settings.canDrawOverlays()`, the user MUST explicitly turn on the app's `AccessibilityService` in `Settings -> Accessibility` and `NotificationListenerService` in `Settings -> Special App Access -> Device & app notifications`.
   - The onboarding/settings UI should provide deep links to `Settings.ACTION_ACCESSIBILITY_SETTINGS` and `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`.
3. **YouTube App DOM Variations**:
   - YouTube updates its UI hierarchy periodically. Reliance on exact resource IDs (`video_title`, `owner_row`) should always be paired with heuristic fallbacks (e.g. content descriptions, metadata band regex `@handle`, and `MediaSessionManager` artist metadata).

---

## 4. Conclusion

The architectural design for the YouTube blocker is completely viable within Safar_Android and aligns with existing codebase patterns:

1. **Manifest & Service Config**:
   - Add `YouTubeGuardService` (AccessibilityService) with `res/xml/youtube_guard_accessibility_service.xml` configured for `packageNames="com.google.android.youtube"` and flags `flagReportViewIds|flagIncludeNotImportantViews`.
   - Add `YouTubeNotificationListener` (NotificationListenerService) with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.
2. **MediaSession Integration**:
   - `YouTubeNotificationListener` registers `MediaSessionManager.addOnActiveSessionsChangedListener` with its `ComponentName`.
   - Catches video title (`METADATA_KEY_TITLE`) and channel name (`METADATA_KEY_ARTIST`) in fullscreen mode and when player controls autohide.
   - Provides `transportControls.pause()` for media suppression.
3. **Compose Blocking Overlay**:
   - Uses `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` with `FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS`.
   - Supplies `ComposeServiceLifecycleOwner` (Lifecycle + ViewModelStore + SavedStateRegistry) to `ComposeView` before `setContent`.
   - Hosts `SafarTheme` with Compose UI for `AskUser`, `Blocked`, and `ShortsBlocked` states.

---

## 5. Verification Method

### 5.1 Compilation and Static Verification
1. **Verify Gradle Dry-Run**:
   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testProdDebugUnitTest --dry-run
   ```
2. **Verify Manifest Syntax**:
   Ensure `app/src/main/AndroidManifest.xml` compiles cleanly with the new service declarations and exported flags.

### 5.2 Unit Test Verification Plan
1. **`YouTubePolicyEngineTest`**:
   - Verify initial state on video identity change is always `Verifying`.
   - Verify transition to `AskUser` when channel is unclassified in Room DB.
   - Verify transition to `Allowed` when channel is `ChannelPolicy.PRODUCTIVE`.
   - Verify transition to `Blocked` when channel is `ChannelPolicy.DISTRACTING`.
   - Verify `ShortsBlocked` triggers immediately on Shorts detection, independent of channel policy.
   - Verify Ad events retain the prior verified channel policy.
2. **`ComposeServiceLifecycleOwnerTest`**:
   - Verify lifecycle transitions: `CREATED` -> `RESUMED` -> `DESTROYED`.
   - Verify `SavedStateRegistry` restores cleanly without throwing `IllegalStateException`.
   - Verify `ViewModelStore` clears on destroy without leaks.

### 5.3 Invalidation Conditions
- If Google removes `TYPE_ACCESSIBILITY_OVERLAY` support in future Android versions (it remains supported through Android 15 / API 35 / compileSdk 36).
- If YouTube stops publishing `MediaMetadata` via `MediaSession` (in which case accessibility DOM parsing remains the primary fallback).
