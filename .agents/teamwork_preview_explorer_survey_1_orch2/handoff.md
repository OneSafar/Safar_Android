# Handoff Report: Survey Explorer 1 — Pillars 1 & 2 (Phases 1, 2, 5, 6, 13, 14)

## Executive Summary
This survey provides a comprehensive architectural and code-level investigation of Pillars 1 & 2 of the YouTube Distraction Blocker fixes plan (`ORIGINAL_REQUEST.md`).
The current implementation has two critical security/integrity flaws:
1. **Fail-Open Verification Flaw (Phase 1, 6, 13)**: `YouTubeGuardService` explicitly hides the overlay during `PolicyState.Verifying` (`is PolicyState.Verifying -> overlayManager.hide()`, line 135), and `OverlayState` lacks a `Verifying` state altogether. Video content and touches are completely uninhibited while policy lookup is in progress.
2. **Synthetic Fake Handle Flaw (Phase 2, 5, 14)**: Both `YouTubeSnapshotParser` (line 86) and `YouTubeChannelRepositoryImpl` (line 41) fabricate fake handles (`"@${channelName.replace(" ", "").lowercase()}"`) when no real `@handle` exists, and `YouTubeChannelEntity` enforces `channelHandle` as the primary key. This violates Requirement R2 and corrupts channel identity.

All 129 existing unit tests in the project currently pass, but tests either bypassed the overlay verification check or actively enforced the fake handle antipattern (e.g., `YouTubePersistenceAdversarialTest.kt` line 120).

---

## 1. Observation

### 1.1 Existing Code Inventory & Verbatim Findings

#### A. Fail-Closed Verifying Overlay & State Machine (Pillar 1)
- **`app/src/main/java/com/safarparmar/app/feature/youtube/policy/PolicyState.kt`**:
  - Lines 15–57: `PolicyState.Verifying(val identity: VideoIdentity? = null)` is defined as a transient state.
- **`app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt`**:
  - Lines 134–136:
    ```kotlin
    // Verifying is an internal ~2ms DB check state — do not flash overlay
    is PolicyState.Verifying -> overlayManager.hide()
    ```
    **Direct Observation**: The accessibility service explicitly orders the overlay manager to `hide()` during `PolicyState.Verifying`. Any video undergoing verification is completely exposed and interactive.
- **`app/src/main/java/com/safarparmar/app/feature/youtube/overlay/OverlayState.kt` (inside `YouTubeGuardOverlay.kt`)**:
  - Lines 24–44:
    ```kotlin
    sealed interface OverlayState {
        data object Hidden : OverlayState
        data class Blocked(...) : OverlayState
        data class ShortsBlocked(...) : OverlayState
        data class AskUser(...) : OverlayState
    }
    ```
    **Direct Observation**: There is NO `OverlayState.Verifying` definition.
- **`app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlayManager.kt`**:
  - Lines 68–104: Public API exposes `showBlocked()`, `showShortsBlocked()`, `showAskUser()`, and `hide()`. It has no `showVerifying()` method.
  - Lines 152–160: WindowManager layout params use:
    ```kotlin
    val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    )
    ```
- **`app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlay.kt`**:
  - Lines 54–67: The root `Box` does not consume pointer events. If `OverlayState` is not visible or lacks an active click-consuming modifier, touches fall through to YouTube.

#### B. Canonical Channel Identity & Fingerprinting (Pillar 2)
- **`app/src/main/java/com/safarparmar/app/feature/youtube/policy/VideoIdentity.kt`**:
  - Lines 6–28:
    ```kotlin
    data class VideoIdentity(
        val videoId: String? = null,
        val title: String = "",
        val channelHandle: String = "",
        val channelName: String = "",
        val isShorts: Boolean = false,
        val isAd: Boolean = false,
    )
    ```
    Equality in `isSameVideo()` compares raw strings `title == other.title && channelHandle == other.channelHandle`.
- **`app/src/main/java/com/safarparmar/app/feature/youtube/detector/YouTubeSnapshotParser.kt`**:
  - Lines 81–88:
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
    **Direct Observation**: Synthetic fake handles are explicitly generated and prefixed with `@`.
- **`app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeNotificationListener.kt`**:
  - Lines 110–124: Deduplication relies on `if (title == lastTitle) return` (raw title). `VideoIdentity` is created with `channelHandle = ""`.

#### C. Room Persistence & Data Layer (Pillar 2 / Phase 14)
- **`app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`**:
  - Lines 16–26:
    ```kotlin
    @Entity(tableName = "youtube_channels")
    data class YouTubeChannelEntity(
        @PrimaryKey
        @ColumnInfo(name = "channelHandle", collate = ColumnInfo.NOCASE)
        val channelHandle: String,
        @ColumnInfo(name = "channelName")
        val channelName: String = "",
        @ColumnInfo(name = "policy")
        val policy: ChannelPolicy,
        @ColumnInfo(name = "updatedAt")
        val updatedAt: Long = System.currentTimeMillis(),
    )
    ```
    **Direct Observation**: `channelHandle` is the `@PrimaryKey`. Without a handle, an entity cannot be stored without fabricating a fake handle.
- **`app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`**:
  - Lines 38–42:
    ```kotlin
    val handleToSave = if (normalized.isNotEmpty()) {
        normalized
    } else {
        "@${channelName.trim().replace("\\s+".toRegex(), "")}"
    }
    ```
    **Direct Observation**: Repository fabricates fake handle `@${channelName...}` if `channelHandle` is empty.
- **`app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt`**:
  - Primary queries are `getChannelByHandle(channelHandle: String)` and `getPolicyByHandle(channelHandle: String)`.
- **`app/src/main/java/com/safarparmar/app/data/local/SafarDatabase.kt`**:
  - Version 1, `exportSchema = false`.
- **`app/src/main/java/com/safarparmar/app/di/DatabaseModule.kt`**:
  - Line 30: `Room.databaseBuilder(...).fallbackToDestructiveMigration().build()`. Schema alterations will auto-migrate cleanly during development without crashing.

### 1.2 Existing Unit Test Verification
- Executed:
  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testProdDebugUnitTest --tests "com.safarparmar.app.feature.youtube.*" --tests "com.safarparmar.app.data.local.YouTubePersistenceAdversarialTest"`
- Results:
  - Total tests: 129
  - Failures: 0
  - Ignored: 0
  - Classes:
    - `YouTubePolicyEngineTest`: 35 tests (100% pass)
    - `YouTubePersistenceAdversarialTest`: 15 tests (100% pass)
    - `YouTubeGuardE2ETest`: 79 tests (100% pass)

---

## 2. Logic Chain

### Phase 1 & Phase 13: Fail-Closed Verifying Overlay & Touch Consumption
1. **Premise**: Invariant 1 dictates that any newly opened or changed YouTube video must never become visible or interactable before its channel identity and policy are resolved.
2. **Direct Observation**: In `YouTubeGuardService.kt:135`, when `PolicyState.Verifying` is active, the service explicitly executes `overlayManager.hide()`.
3. **Inference**: During initial video loading, autoplay switches, or delayed DB lookups, the blocking overlay is removed, allowing the user to view content and tap YouTube controls.
4. **Required Action (Phase 1 & 13)**:
   - Introduce `OverlayState.Verifying(val channelName: String = "", val title: String = "")`.
   - Add `YouTubeGuardOverlayManager.showVerifying(...)`.
   - Route `PolicyState.Verifying -> overlayManager.showVerifying(...)` in `YouTubeGuardService`.
   - Add `VerifyingContent` composable in `YouTubeGuardOverlay.kt` showing a dark, high-opacity backdrop (`#F50D131A`) with an indeterminate loading spinner and verification status text.
   - Consume all touch events in `YouTubeGuardOverlay` via `Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } }` so no touch events can pass through to the YouTube UI underneath.

### Phase 2: Canonical Channel Identity & Canonical Keys
1. **Premise**: Real YouTube channels may have an explicit handle (e.g. `@Veritasium`) or only a display name (or the handle is not visible on the current screen). Requirement R2 strictly forbids fabricating synthetic handles.
2. **Direct Observation**: In `YouTubeSnapshotParser.kt:86` and `YouTubeChannelRepositoryImpl.kt:41`, empty handles are converted into fake handles like `@KhanAcademy`.
3. **Inference**: Fabricating synthetic `@handles` pollutes the database with unverified handles, causes collisions, and breaks lookup when the real handle is later encountered.
4. **Required Action (Phase 2)**:
   - Create `ChannelIdentity`:
     ```kotlin
     data class ChannelIdentity(
         val verifiedHandle: String? = null, // ONLY real @handle detected from UI; null otherwise
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
   - Strip all synthetic handle generation from `YouTubeSnapshotParser` and repository.

### Phase 5: Video Fingerprinting (`VideoFingerprint`)
1. **Premise**: Deduplication between MediaSession and AccessibilityService must be robust against title variations and missing handles.
2. **Direct Observation**: `VideoIdentity.isSameVideo` uses direct string comparison. `YouTubeNotificationListener` checks `title == lastTitle`.
3. **Inference**: MediaSession events (which provide `title` and `artist/channelName` but no handle) cannot cleanly match AccessibilityService events without a structured fingerprint.
4. **Required Action (Phase 5)**:
   - Create `VideoFingerprint`:
     ```kotlin
     data class VideoFingerprint(
         val mediaId: String? = null,
         val normalizedTitle: String = "",
         val channelCanonicalKey: String = "",
     ) {
         val key: String
             get() = when {
                 !mediaId.isNullOrBlank() -> "media:$mediaId"
                 normalizedTitle.isNotBlank() || channelCanonicalKey.isNotBlank() ->
                     "content:$normalizedTitle|$channelCanonicalKey"
                 else -> ""
             }
     }
     ```
   - Deduplicate in `YouTubeNotificationListener` and `YouTubePolicyEngine` using `fingerprint.key`.

### Phase 6: Autoplay & Fullscreen State Transitions
1. **Premise**: Autoplay switches and fullscreen transitions must immediately re-enter `Verifying` before policy resolution.
2. **Direct Observation**: In `YouTubePolicyEngine.kt:53`, `onVideoDetected` ignores events if `identity.isSameVideo(currentIdentity) && _state.value !is PolicyState.Verifying`.
3. **Inference**: If fingerprint changes (new video started via autoplay or fullscreen change), any pending lookup must be cancelled, `_state.value` must immediately emit `PolicyState.Verifying(identity)`, and `YouTubeGuardService` must immediately show `OverlayState.Verifying`.

### Phase 14: Room Persistence Without Fake Handles
1. **Premise**: Room entity and DAO must store channels by canonical identity without requiring synthetic fake handles.
2. **Direct Observation**: `YouTubeChannelEntity` currently defines `@PrimaryKey val channelHandle: String`.
3. **Inference**: Storing a channel classified only by `displayName` currently requires a fabricated handle as the primary key.
4. **Required Action (Phase 14)**:
   - Redesign `YouTubeChannelEntity`:
     - `@PrimaryKey @ColumnInfo(name = "canonicalKey") val canonicalKey: String` (e.g. `handle:@veritasium` or `name:khan academy`)
     - `@ColumnInfo(name = "verifiedHandle", collate = ColumnInfo.NOCASE) val verifiedHandle: String? = null` (nullable!)
     - `@ColumnInfo(name = "displayName", collate = ColumnInfo.NOCASE) val displayName: String = ""`
     - `@ColumnInfo(name = "policy") val policy: ChannelPolicy`
     - `@ColumnInfo(name = "updatedAt") val updatedAt: Long`
   - Update `YouTubeChannelDao` to query by `canonicalKey`, `verifiedHandle`, or `displayName`.
   - Update `YouTubeChannelRepository` to provide `getPolicy(identity: ChannelIdentity)` and `savePolicy(identity: ChannelIdentity, policy: ChannelPolicy)` without generating fake handles.

---

## 3. Caveats

1. **Subtree Parsing Boundary (Pillar 3)**:
   - This analysis focuses on Pillars 1 & 2. The structural subtree parsing (`YouTubeUiParser`) and view click interception on bottom navigation (`TYPE_VIEW_CLICKED`) are covered by Pillar 3. `YouTubeSnapshotParser` should be adapted or replaced by `YouTubeUiParser` to populate `ChannelIdentity`.
2. **Detection Fusion & Serialization (Pillar 4)**:
   - Fusing Accessibility and MediaSession events via `YouTubeContentDetector` and an event channel is part of Pillar 4. In Pillars 1 & 2, `VideoIdentity` and `YouTubePolicyEngine` must expose the canonical contracts (`ChannelIdentity`, `VideoFingerprint`, `PolicyState.Verifying`) so the fusion layer can directly feed them.
3. **Test Suite Compatibility**:
   - Existing tests in `YouTubePolicyEngineTest` and `YouTubeGuardE2ETest` pass because they use convenience constructors and mock repositories. To prevent breaking existing test suites, `VideoIdentity` should provide a backward-compatible secondary constructor that maps `channelHandle` and `channelName` into `ChannelIdentity`.
   - `YouTubePersistenceAdversarialTest.kt` line 120 currently tests the obsolete fake handle derivation. That specific test must be updated to verify that fake handles are NEVER created.

---

## 4. Conclusion & Concrete Recommendations

### 4.1 Implementation Model Specifications

#### File: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/ChannelIdentity.kt` (New File)
```kotlin
package com.safarparmar.app.feature.youtube.policy

data class ChannelIdentity(
    val verifiedHandle: String? = null,
    val displayName: String = "",
) {
    val canonicalKey: String
        get() = when {
            !verifiedHandle.isNullOrBlank() -> "handle:${normalizeHandle(verifiedHandle)}"
            displayName.isNotBlank() -> "name:${normalizeDisplayName(displayName)}"
            else -> ""
        }

    companion object {
        val EMPTY = ChannelIdentity()

        fun normalizeHandle(handle: String): String {
            val trimmed = handle.trim()
            if (trimmed.isEmpty()) return ""
            val clean = if (trimmed.startsWith("@")) trimmed.substring(1) else trimmed
            return "@${clean.lowercase()}"
        }

        fun normalizeDisplayName(name: String): String {
            return name.trim().lowercase()
        }
    }
}
```

#### File: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/VideoFingerprint.kt` (New File)
```kotlin
package com.safarparmar.app.feature.youtube.policy

data class VideoFingerprint(
    val mediaId: String? = null,
    val normalizedTitle: String = "",
    val channelCanonicalKey: String = "",
) {
    val key: String
        get() = when {
            !mediaId.isNullOrBlank() -> "media:${mediaId.trim()}"
            normalizedTitle.isNotBlank() || channelCanonicalKey.isNotBlank() ->
                "content:${normalizedTitle.trim().lowercase()}|$channelCanonicalKey"
            else -> ""
        }

    companion object {
        val EMPTY = VideoFingerprint()

        fun create(
            mediaId: String? = null,
            title: String = "",
            channelIdentity: ChannelIdentity = ChannelIdentity.EMPTY,
        ): VideoFingerprint {
            return VideoFingerprint(
                mediaId = mediaId?.trim()?.takeIf { it.isNotEmpty() },
                normalizedTitle = title.trim().lowercase(),
                channelCanonicalKey = channelIdentity.canonicalKey,
            )
        }
    }
}
```

#### File: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/VideoIdentity.kt` (Updated)
```kotlin
package com.safarparmar.app.feature.youtube.policy

data class VideoIdentity(
    val videoId: String? = null,
    val title: String = "",
    val channelIdentity: ChannelIdentity = ChannelIdentity.EMPTY,
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
) {
    // Backwards compatibility constructor for existing tests & callers
    constructor(
        videoId: String? = null,
        title: String = "",
        channelHandle: String = "",
        channelName: String = "",
        isShorts: Boolean = false,
        isAd: Boolean = false,
    ) : this(
        videoId = videoId,
        title = title,
        channelIdentity = ChannelIdentity(
            verifiedHandle = channelHandle.trim().takeIf { it.isNotEmpty() },
            displayName = channelName.trim(),
        ),
        isShorts = isShorts,
        isAd = isAd,
    )

    val channelHandle: String get() = channelIdentity.verifiedHandle.orEmpty()
    val channelName: String get() = channelIdentity.displayName

    val fingerprint: VideoFingerprint
        get() = VideoFingerprint.create(
            mediaId = videoId,
            title = title,
            channelIdentity = channelIdentity,
        )

    fun isSameVideo(other: VideoIdentity?): Boolean {
        if (other == null) return false
        if (isShorts != other.isShorts || isAd != other.isAd) return false
        val myKey = fingerprint.key
        val otherKey = other.fingerprint.key
        if (myKey.isNotEmpty() && otherKey.isNotEmpty()) {
            return myKey == otherKey
        }
        return title.equals(other.title, ignoreCase = true) &&
            channelIdentity == other.channelIdentity
    }
}
```

#### File: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/PolicyState.kt` (Updated)
```kotlin
package com.safarparmar.app.feature.youtube.policy

enum class BlockReason {
    DISTRACTING_CHANNEL,
    SHORTS,
    UNVERIFIED,
}

sealed interface PolicyState {
    data class Verifying(
        val identity: VideoIdentity? = null,
    ) : PolicyState

    data class AskUser(
        val identity: VideoIdentity,
        val channelIdentity: ChannelIdentity,
    ) : PolicyState {
        val channelHandle: String get() = channelIdentity.verifiedHandle ?: channelIdentity.displayName
    }

    data class Allowed(
        val identity: VideoIdentity,
        val channelIdentity: ChannelIdentity,
    ) : PolicyState {
        val channelHandle: String get() = channelIdentity.verifiedHandle ?: channelIdentity.displayName
    }

    data class Blocked(
        val identity: VideoIdentity,
        val channelIdentity: ChannelIdentity,
        val reason: BlockReason = BlockReason.DISTRACTING_CHANNEL,
    ) : PolicyState {
        val channelHandle: String get() = channelIdentity.verifiedHandle ?: channelIdentity.displayName
    }

    data class ShortsBlocked(
        val identity: VideoIdentity,
        val reason: BlockReason = BlockReason.SHORTS,
    ) : PolicyState
}
```

#### File: `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlay.kt` (Updated)
1. Add `OverlayState.Verifying`:
```kotlin
sealed interface OverlayState {
    data object Hidden : OverlayState

    data class Verifying(
        val channelName: String = "",
        val title: String = "",
    ) : OverlayState

    data class Blocked(
        val channelName: String,
        val channelHandle: String,
        val reason: BlockReason,
        val onBack: () -> Unit = {},
    ) : OverlayState

    data class ShortsBlocked(
        val onBack: () -> Unit = {},
    ) : OverlayState

    data class AskUser(
        val channelName: String,
        val channelHandle: String,
        val onClassify: (String, ChannelPolicy) -> Unit,
        val onBack: () -> Unit = {},
    ) : OverlayState
}
```
2. Consume all touch events on the root `Box`:
```kotlin
@Composable
fun YouTubeGuardOverlay(state: OverlayState) {
    if (state is OverlayState.Hidden) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF50D131A))
            .pointerInput(Unit) {
                // Invariant: Consume all touches fail-closed across all active overlay states
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when (val s = state) {
            is OverlayState.Verifying -> VerifyingContent(s)
            is OverlayState.Blocked -> BlockedContent(s)
            is OverlayState.ShortsBlocked -> ShortsBlockedContent(s)
            is OverlayState.AskUser -> AskUserContent(s)
            is OverlayState.Hidden -> Unit
        }
    }
}

@Composable
private fun VerifyingContent(state: OverlayState.Verifying) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF42A5F5),
            strokeWidth = 4.dp,
        )
        Text(
            text = "Verifying Content...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
        )
        if (state.channelName.isNotBlank()) {
            Text(
                text = state.channelName,
                fontSize = 14.sp,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center,
            )
        }
    }
}
```

#### File: `app/src/main/java/com/safarparmar/app/feature/youtube/overlay/YouTubeGuardOverlayManager.kt` (Updated)
Add:
```kotlin
fun showVerifying(channelName: String = "", title: String = "") {
    updateState(OverlayState.Verifying(channelName, title))
}
```

#### File: `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt` (Updated)
In `handlePolicyState`:
```kotlin
is PolicyState.Verifying -> overlayManager.showVerifying(
    channelName = state.identity?.channelName.orEmpty(),
    title = state.identity?.title.orEmpty(),
)
```

#### File: `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt` (Updated)
```kotlin
package com.safarparmar.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "youtube_channels",
    indices = [
        Index(value = ["verifiedHandle"], unique = false),
        Index(value = ["displayName"], unique = false),
    ]
)
data class YouTubeChannelEntity(
    @PrimaryKey
    @ColumnInfo(name = "canonicalKey")
    val canonicalKey: String,
    @ColumnInfo(name = "verifiedHandle", collate = ColumnInfo.NOCASE)
    val verifiedHandle: String? = null,
    @ColumnInfo(name = "displayName", collate = ColumnInfo.NOCASE)
    val displayName: String = "",
    @ColumnInfo(name = "policy")
    val policy: ChannelPolicy,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
) {
    // Backwards-compatible convenience getters
    val channelHandle: String get() = verifiedHandle.orEmpty()
    val channelName: String get() = displayName
}
```

#### File: `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt` (Updated)
```kotlin
package com.safarparmar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface YouTubeChannelDao {
    @Query("SELECT * FROM youtube_channels WHERE canonicalKey = :canonicalKey LIMIT 1")
    suspend fun getChannelByCanonicalKey(canonicalKey: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE canonicalKey = :canonicalKey LIMIT 1")
    suspend fun getPolicyByCanonicalKey(canonicalKey: String): ChannelPolicy?

    @Query("SELECT * FROM youtube_channels WHERE verifiedHandle = :handle COLLATE NOCASE LIMIT 1")
    suspend fun getChannelByHandle(handle: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE verifiedHandle = :handle COLLATE NOCASE LIMIT 1")
    suspend fun getPolicyByHandle(handle: String): ChannelPolicy?

    @Query("SELECT * FROM youtube_channels WHERE displayName = :displayName COLLATE NOCASE LIMIT 1")
    suspend fun getChannelByName(displayName: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE displayName = :displayName COLLATE NOCASE LIMIT 1")
    suspend fun getPolicyByName(displayName: String): ChannelPolicy?

    @Query("SELECT * FROM youtube_channels ORDER BY updatedAt DESC")
    fun getAllChannels(): Flow<List<YouTubeChannelEntity>>

    @Query("SELECT * FROM youtube_channels WHERE policy = :policy ORDER BY updatedAt DESC")
    fun getChannelsByPolicy(policy: ChannelPolicy): Flow<List<YouTubeChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(channel: YouTubeChannelEntity)

    @Query("DELETE FROM youtube_channels WHERE canonicalKey = :canonicalKey")
    suspend fun deleteByCanonicalKey(canonicalKey: String)

    @Query("DELETE FROM youtube_channels WHERE verifiedHandle = :handle COLLATE NOCASE")
    suspend fun deleteByHandle(handle: String)

    @Delete
    suspend fun deleteChannel(channel: YouTubeChannelEntity)

    @Query("DELETE FROM youtube_channels")
    suspend fun deleteAll()
}
```

#### File: `app/src/main/java/com/safarparmar/app/domain/repository/YouTubeChannelRepository.kt` (Updated)
```kotlin
package com.safarparmar.app.domain.repository

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.feature.youtube.policy.ChannelIdentity
import kotlinx.coroutines.flow.Flow

interface YouTubeChannelRepository {
    suspend fun getPolicy(identity: ChannelIdentity): ChannelPolicy?
    suspend fun savePolicy(identity: ChannelIdentity, policy: ChannelPolicy)

    suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy?
    suspend fun saveChannelPolicy(
        channelHandle: String,
        channelName: String = "",
        policy: ChannelPolicy,
    )

    fun getAllChannels(): Flow<List<YouTubeChannelEntity>>
    suspend fun getChannel(channelHandle: String): YouTubeChannelEntity?
    suspend fun deleteChannel(channelHandle: String)
}
```

---

## 5. Verification Method

### Test Execution Command
```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testProdDebugUnitTest --tests "com.safarparmar.app.feature.youtube.*" --tests "com.safarparmar.app.data.local.YouTubePersistenceAdversarialTest"
```

### Invalidation Conditions
The design is invalidated if:
1. When a new video is detected, `YouTubeGuardService` invokes `overlayManager.hide()` or does not present `OverlayState.Verifying`.
2. Any touch or click penetrates through the `Verifying` overlay window to the YouTube player or recommendations below.
3. A video with only a display name creates an entity in Room with a synthetic `@handle` (e.g. `@khanacademy`).
4. Lookups for `@veritasium` fail if saved as `Veritasium` or vice versa.
5. Autoplay transitions between videos fail to trigger `PolicyState.Verifying` and `OverlayState.Verifying`.
