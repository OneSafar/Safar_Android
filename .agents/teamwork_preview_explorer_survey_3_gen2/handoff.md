# Architectural Survey Handoff Report: YouTubePolicyEngine & Test Infrastructure

**Agent**: Survey Explorer 3 (Gen 2)  
**Date**: 2026-09-11  
**Project**: Safar Android (`com.safarparmar.app`)  
**Working Directory**: `.agents/teamwork_preview_explorer_survey_3_gen2/`  
**Authoritative Reference**: `.agents/ORIGINAL_REQUEST.md`

---

## 1. Observation

### 1.1 Test Infrastructure & Dependencies in Gradle
From inspection of `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
hilt = "2.51.1"
room = "2.6.1"
coroutines = "1.9.0"
turbine = "1.1.0"
junit = "4.13.2"
mockk = "1.13.12"
androidxJunit = "1.2.1"
espresso = "3.6.1"

[libraries]
junit = { group = "junit", name = "junit", version = "4.13.2" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version = "1.2.1" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version = "3.6.1" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

From inspection of `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/build.gradle.kts` (lines 191–194, 273–281):
```kotlin
testOptions {
    unitTests.isReturnDefaultValues = true
}

// ...
debugImplementation(libs.androidx.ui.tooling)
debugImplementation(libs.androidx.ui.test.manifest)
testImplementation(libs.junit)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
testImplementation(libs.mockk)
androidTestImplementation(libs.androidx.junit)
androidTestImplementation(libs.androidx.espresso.core)
```

**Key dependency observations:**
1. **Unit Test Framework**: **JUnit 4** (`junit:junit:4.13.2`), NOT JUnit 5.
2. **Coroutines Testing**: `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0` is available.
3. **Flow Testing**: `app.cash.turbine:turbine:1.1.0` is available and used in the codebase.
4. **Mocking Library**: `io.mockk:mockk:1.13.12` is available and used.
5. **Robolectric**: NOT declared in `libs.versions.toml` or `app/build.gradle.kts`.
6. **Room Testing**: `androidx.room:room-testing` is NOT currently declared in `libs.versions.toml` or `app/build.gradle.kts`.
7. **Compose UI Testing**: Only `debugImplementation(libs.androidx.ui.test.manifest)` is included; `androidx.compose.ui:ui-test-junit4` is NOT present.

### 1.2 Local JVM Environment & Test Runner Execution
When running `./gradlew` directly, `/usr/bin/java` fails with:
`The operation couldn’t be completed. Unable to locate a Java Runtime.`

However, OpenJDK 17 is installed on the host at:
`/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home`

Verifying test execution via terminal command:
```bash
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home"
./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.util.SafeApiCallTest"
```
**Result**: `BUILD SUCCESSFUL in 32s` (36 actionable tasks: 24 executed, 12 up-to-date).

### 1.3 Existing Test Suite Inventory & Conventions
- **Unit Tests Location**: `app/src/test/java/com/safarparmar/app/`
  - Total 23 existing test classes across:
    - `com.safarparmar.app.feature.live.LiveSessionViewModelTest`
    - `com.safarparmar.app.ui.auth.AuthViewModelTest`
    - `com.safarparmar.app.ui.ekagra.EkagraTimeAccountingTest`
    - `com.safarparmar.app.ui.studyplanner.logic.StudyPlannerProgressTest`
    - `com.safarparmar.app.notifications.QuietHoursTest`
- **Instrumentation Tests Location**: `app/src/androidTest` **does not exist** on disk. All automated tests in the repository are pure JVM unit tests.
- **Test Conventions Observed**:
  - `MainDispatcherRule` (`app/src/test/java/com/safarparmar/app/ui/auth/MainDispatcherRule.kt`) uses `StandardTestDispatcher()` to manage `Dispatchers.Main` in coroutine unit tests.
  - Test functions use Kotlin backtick naming (e.g. ``fun `login requires email and password`()``).
  - Repositories are tested using Fakes (e.g. `FakeAuthRepository`, `FakeRepo` implementing interface contracts) or MockK (`mockk(relaxed = true)`).
  - Flows are tested using Turbine `flow.test { awaitItem() ... }`.

### 1.4 Room Database Implementation Status in Codebase
Grep search for `@Database` and `@Dao` across `app/src/main`:
- `grep_search "@Database"`: **No results found**.
- `grep_search "@Dao"`: **No results found**.
- Directory `app/src/main/java/com/safarparmar/app/data/local/`:
  - Contains `SafarDataStore.kt`, `PersistentCookieStore.kt`, `TimerAlertStyle.kt`.
  - **No Room database, no DAOs, and no entities currently exist.**
- Gradle dependencies:
  - `libs.room.runtime` (2.6.1), `libs.room.ktx` (2.6.1), and `ksp(libs.room.compiler)` (2.6.1) are already configured in `app/build.gradle.kts`.
  - This means Room can be implemented without changing dependencies or adding plugins; schema migration is starting from version 1.

### 1.5 Compose Overlay & Lifecycle Patterns in Codebase
Inspection of `app/src/main/java/com/safarparmar/app/ui/ekagra/TimerBubbleOverlay.kt` (lines 429–456):
- WindowManager overlay uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` (or `TYPE_ACCESSIBILITY_OVERLAY` when created from an `AccessibilityService`).
- Uses custom `BubbleLifecycleOwner` implementing `LifecycleOwner` and `SavedStateRegistryOwner`.
- Binds Compose View via:
  - `setViewTreeLifecycleOwner(lo)`
  - `setViewTreeSavedStateRegistryOwner(lo)`
- Compose root uses standard Jetpack Compose Material 3 components.

---

## 2. Logic Chain

### 2.1 State Machine & Policy Engine Architecture (`YouTubePolicyEngine`)

#### 2.1.1 State Representation
The state machine MUST model 5 required explicit states as specified in `ORIGINAL_REQUEST.md` (R2):
```kotlin
package com.safarparmar.app.feature.youtube.policy

sealed interface PolicyState {
    /**
     * Initial transient state entered on ANY video identity change.
     * Content is blocked/obscured (fail-closed) while metadata is evaluated.
     */
    data class Verifying(
        val identity: VideoIdentity? = null
    ) : PolicyState

    /**
     * Displayed when a video belongs to a channel with no saved classification.
     * Overlay displays classification prompt allowing user to choose PRODUCTIVE or DISTRACTING.
     */
    data class AskUser(
        val identity: VideoIdentity,
        val channelHandle: String
    ) : PolicyState

    /**
     * Video is allowed to play. Overlay is dismissed.
     * Occurs when channel is PRODUCTIVE, or during an ad within a verified PRODUCTIVE session.
     */
    data class Allowed(
        val identity: VideoIdentity,
        val channelHandle: String
    ) : PolicyState

    /**
     * Video is blocked from playing. Overlay covers screen.
     * Occurs when channel is DISTRACTING, or during an ad within a verified DISTRACTING session.
     */
    data class Blocked(
        val identity: VideoIdentity,
        val channelHandle: String,
        val reason: BlockReason = BlockReason.DISTRACTING_CHANNEL
    ) : PolicyState

    /**
     * Shorts format is detected. Overlay covers screen.
     * Evaluated and blocked INDEPENDENTLY of channel classification.
     */
    data class ShortsBlocked(
        val identity: VideoIdentity,
        val reason: BlockReason = BlockReason.SHORTS
    ) : PolicyState
}

enum class BlockReason {
    DISTRACTING_CHANNEL,
    SHORTS,
    UNVERIFIED
}
```

#### 2.1.2 Input Data Models and Events
```kotlin
data class VideoIdentity(
    val videoId: String? = null,
    val title: String = "",
    val channelHandle: String = "", // Canonical identifier e.g. "@veritasium"
    val channelName: String = "",
    val isShorts: Boolean = false,
    val isAd: Boolean = false,
) {
    fun isSameVideo(other: VideoIdentity?): Boolean {
        if (other == null) return false
        if (videoId != null && other.videoId != null) return videoId == other.videoId
        return title == other.title && channelHandle == other.channelHandle && isShorts == other.isShorts
    }
}

sealed interface PolicyEvent {
    data class VideoDetected(val identity: VideoIdentity) : PolicyEvent
    data class UserClassified(val channelHandle: String, val policy: ChannelPolicy) : PolicyEvent
    data object PlaybackExited : PolicyEvent
}
```

#### 2.1.3 Core Invariants and Logic Rules
1. **Invariant 1: Default to `Verifying` on Video Identity Change (Fail-Closed)**
   - When `VideoDetected(newIdentity)` is received, if `!newIdentity.isSameVideo(currentIdentity)`:
     - The state machine immediately transitions to `PolicyState.Verifying(newIdentity)`.
     - It MUST emit `Verifying` before triggering or completing Room database lookups.
     - Content is never temporarily displayed or left unblocked during this window.

2. **Invariant 2: Shorts Evaluation Independence**
   - If `identity.isShorts == true`:
     - After transitioning to `Verifying`, the state machine transitions to `PolicyState.ShortsBlocked(identity)`.
     - Channel policy is **bypassed entirely**. Even if `@veritasium` is saved as `ChannelPolicy.PRODUCTIVE`, a Short from `@veritasium` transitions to `ShortsBlocked`.
     - Unclassified Shorts do NOT transition to `AskUser`; they are blocked immediately as `ShortsBlocked`.

3. **Invariant 3: Ad Transitions Retain Previous Verified Channel's Policy**
   - In YouTube, pre-roll, mid-roll, or post-roll ads trigger accessibility/metadata events carrying advertiser info (e.g. ad sponsor handle `@geico` or ad video title).
   - If `identity.isAd == true`:
     - The state machine checks `lastVerifiedChannelPolicy`:
       - If `lastVerifiedChannelPolicy == ChannelPolicy.PRODUCTIVE`: State remains / transitions to `PolicyState.Allowed`.
       - If `lastVerifiedChannelPolicy == ChannelPolicy.DISTRACTING`: State remains / transitions to `PolicyState.Blocked`.
     - Crucially: **The ad's channel metadata is NOT looked up in the database, and does NOT trigger `AskUser`.**
     - If no previous verified channel policy exists (e.g. cold launch directly into an ad): defaults fail-closed to `Verifying` or `Blocked` until primary video content is identified.

4. **Invariant 4: Channel Classification Resolution**
   - For a regular video (`!isShorts && !isAd`):
     - Extract `channelHandle`.
     - Look up policy in `YouTubeChannelRepository`:
       - `ChannelPolicy.PRODUCTIVE` -> Emit `PolicyState.Allowed(identity, channelHandle)`; record `lastVerifiedChannelPolicy = PRODUCTIVE`.
       - `ChannelPolicy.DISTRACTING` -> Emit `PolicyState.Blocked(identity, channelHandle)`; record `lastVerifiedChannelPolicy = DISTRACTING`.
       - `null` (not yet classified) -> Emit `PolicyState.AskUser(identity, channelHandle)`.
     - When `UserClassified(handle, policy)` is received:
       - Persist `YouTubeChannelEntity` into Room DB.
       - Update `lastVerifiedChannelPolicy = policy`.
       - Emit `Allowed` (if `PRODUCTIVE`) or `Blocked` (if `DISTRACTING`).

#### 2.1.4 State Transition Table

| Current State | Event | Condition | Next State | Side Effects |
| :--- | :--- | :--- | :--- | :--- |
| Any | `VideoDetected(id)` | `id.isShorts == true` | `Verifying` -> `ShortsBlocked` | Log Shorts block |
| Any | `VideoDetected(id)` | `id.isAd == true` && `lastVerified == PRODUCTIVE` | `Allowed` | Retain playback |
| Any | `VideoDetected(id)` | `id.isAd == true` && `lastVerified == DISTRACTING` | `Blocked` | Retain block |
| Any | `VideoDetected(id)` | `id.isAd == true` && `lastVerified == null` | `Verifying` | Fail-closed |
| Any | `VideoDetected(id)` | `!isShorts && !isAd` && channel is `PRODUCTIVE` | `Verifying` -> `Allowed` | Update `lastVerified = PRODUCTIVE` |
| Any | `VideoDetected(id)` | `!isShorts && !isAd` && channel is `DISTRACTING` | `Verifying` -> `Blocked` | Update `lastVerified = DISTRACTING` |
| Any | `VideoDetected(id)` | `!isShorts && !isAd` && channel is unclassified | `Verifying` -> `AskUser` | Display classify overlay |
| `AskUser` | `UserClassified(handle, PRODUCTIVE)` | handle matches | `Allowed` | Save to Room, set `lastVerified = PRODUCTIVE` |
| `AskUser` | `UserClassified(handle, DISTRACTING)` | handle matches | `Blocked` | Save to Room, set `lastVerified = DISTRACTING` |
| Any | `PlaybackExited` | YouTube closed or stopped | `Verifying` / Dismissed | Reset active session |

---

### 2.2 Persistence Layer Blueprint (Room Database & DAO)

Because no Room database currently exists in `app/src/main`, the following clean, self-contained architecture must be created:

#### 2.2.1 Entity: `YouTubeChannelEntity.kt`
Path: `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`
```kotlin
package com.safarparmar.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChannelPolicy {
    PRODUCTIVE,
    DISTRACTING
}

@Entity(tableName = "youtube_channels")
data class YouTubeChannelEntity(
    @PrimaryKey
    @ColumnInfo(name = "channel_handle")
    val channelHandle: String,

    @ColumnInfo(name = "channel_name")
    val channelName: String = "",

    @ColumnInfo(name = "policy")
    val policy: ChannelPolicy,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### 2.2.2 Type Converter: `ChannelPolicyConverter.kt`
Path: `app/src/main/java/com/safarparmar/app/data/local/converter/ChannelPolicyConverter.kt`
```kotlin
package com.safarparmar.app.data.local.converter

import androidx.room.TypeConverter
import com.safarparmar.app.data.local.entity.ChannelPolicy

class ChannelPolicyConverter {
    @TypeConverter
    fun fromPolicy(policy: ChannelPolicy?): String? = policy?.name

    @TypeConverter
    fun toPolicy(name: String?): ChannelPolicy? = name?.let {
        runCatching { ChannelPolicy.valueOf(it) }.getOrNull()
    }
}
```

#### 2.2.3 DAO: `YouTubeChannelDao.kt`
Path: `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt`
```kotlin
package com.safarparmar.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface YouTubeChannelDao {
    @Query("SELECT * FROM youtube_channels WHERE channel_handle = :handle LIMIT 1")
    suspend fun getChannelByHandle(handle: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE channel_handle = :handle LIMIT 1")
    suspend fun getPolicyByHandle(handle: String): ChannelPolicy?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(channel: YouTubeChannelEntity)

    @Query("SELECT * FROM youtube_channels ORDER BY updated_at DESC")
    fun getAllChannels(): Flow<List<YouTubeChannelEntity>>

    @Query("DELETE FROM youtube_channels WHERE channel_handle = :handle")
    suspend fun deleteByHandle(handle: String)
}
```

#### 2.2.4 Room Database: `SafarDatabase.kt`
Path: `app/src/main/java/com/safarparmar/app/data/local/SafarDatabase.kt`
```kotlin
package com.safarparmar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.safarparmar.app.data.local.converter.ChannelPolicyConverter
import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity

@Database(
    entities = [YouTubeChannelEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ChannelPolicyConverter::class)
abstract class SafarDatabase : RoomDatabase() {
    abstract fun youTubeChannelDao(): YouTubeChannelDao
}
```

#### 2.2.5 Hilt DI Module: `DatabaseModule.kt`
Path: `app/src/main/java/com/safarparmar/app/di/DatabaseModule.kt`
```kotlin
package com.safarparmar.app.di

import android.content.Context
import androidx.room.Room
import com.safarparmar.app.data.local.SafarDatabase
import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideSafarDatabase(@ApplicationContext context: Context): SafarDatabase {
        return Room.databaseBuilder(
            context,
            SafarDatabase::class.java,
            "safar_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideYouTubeChannelDao(database: SafarDatabase): YouTubeChannelDao {
        return database.youTubeChannelDao()
    }
}
```

---

### 2.3 Repository Contract & Clean Architecture Separation
To ensure `YouTubePolicyEngine` remains a pure Kotlin class easily testable on the JVM without mocking Room SQLite internals, define:
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

---

### 2.4 State Machine Implementation Blueprint (`YouTubePolicyEngine`)
Path: `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`

```kotlin
package com.safarparmar.app.feature.youtube.policy

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubePolicyEngine @Inject constructor(
    private val repository: YouTubeChannelRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<PolicyState>(PolicyState.Verifying())
    val state: StateFlow<PolicyState> = _state.asStateFlow()

    private var currentIdentity: VideoIdentity? = null
    private var lastVerifiedPolicy: ChannelPolicy? = null
    private var lastVerifiedChannel: String? = null

    /**
     * Called whenever video information is extracted from Accessibility or MediaSession.
     */
    fun onVideoDetected(identity: VideoIdentity) {
        // If identity is identical to current, no-op
        if (identity.isSameVideo(currentIdentity)) {
            return
        }

        currentIdentity = identity

        // INVARIANT 1: Default to 'Verifying' immediately on ANY video identity change
        _state.value = PolicyState.Verifying(identity)

        scope.launch {
            resolvePolicy(identity)
        }
    }

    private suspend fun resolvePolicy(identity: VideoIdentity) {
        // INVARIANT 2: Shorts are evaluated and blocked independently of channel policy
        if (identity.isShorts) {
            _state.value = PolicyState.ShortsBlocked(identity, BlockReason.SHORTS)
            return
        }

        // INVARIANT 3: Ad transitions retain previous verified channel's policy
        if (identity.isAd) {
            when (lastVerifiedPolicy) {
                ChannelPolicy.PRODUCTIVE -> {
                    _state.value = PolicyState.Allowed(identity, lastVerifiedChannel ?: "")
                }
                ChannelPolicy.DISTRACTING -> {
                    _state.value = PolicyState.Blocked(
                        identity = identity,
                        channelHandle = lastVerifiedChannel ?: "",
                        reason = BlockReason.DISTRACTING_CHANNEL
                    )
                }
                null -> {
                    // No prior verified video in this session; fail-closed
                    _state.value = PolicyState.Blocked(
                        identity = identity,
                        channelHandle = "",
                        reason = BlockReason.UNVERIFIED
                    )
                }
            }
            return
        }

        // Standard Video: Look up classification in database
        val handle = identity.channelHandle.trim()
        if (handle.isEmpty()) {
            // Cannot verify without channel identity; remain in Verifying or AskUser
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
                // Unknown channel -> prompt user via Compose overlay
                _state.value = PolicyState.AskUser(identity, handle)
            }
        }
    }

    fun onUserClassified(channelHandle: String, policy: ChannelPolicy) {
        val activeIdentity = currentIdentity ?: return
        scope.launch {
            repository.saveChannelPolicy(
                channelHandle = channelHandle,
                channelName = activeIdentity.channelName,
                policy = policy
            )
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

    fun onPlaybackStopped() {
        currentIdentity = null
        lastVerifiedPolicy = null
        lastVerifiedChannel = null
        _state.value = PolicyState.Verifying(null)
    }
}
```

---

### 2.5 Comprehensive Test Blueprint

#### 2.5.1 Unit Tests for `YouTubePolicyEngine`
Path: `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt`

This test suite directly exercises every acceptance criterion from `ORIGINAL_REQUEST.md`:
```kotlin
package com.safarparmar.app.feature.youtube.policy

import app.cash.turbine.test
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import com.safarparmar.app.ui.auth.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class YouTubePolicyEngineTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    // ── Acceptance Criterion 1: Identity Change Always Transitions to Verifying ──

    @Test
    fun `new video identity always transitions to Verifying before resolving to Allowed`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository().apply {
            saveDirect("@veritasium", ChannelPolicy.PRODUCTIVE)
        }
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            // Initial state
            assertTrue(awaitItem() is PolicyState.Verifying)

            engine.onVideoDetected(
                VideoIdentity(title = "Science Video", channelHandle = "@veritasium", isShorts = false, isAd = false)
            )

            // Step 1: Must emit Verifying
            val first = awaitItem()
            assertTrue("Expected Verifying on new identity, got $first", first is PolicyState.Verifying)

            // Step 2: Resolves to Allowed
            val second = awaitItem()
            assertTrue("Expected Allowed after resolution, got $second", second is PolicyState.Allowed)
            assertEquals("@veritasium", (second as PolicyState.Allowed).channelHandle)
        }
    }

    @Test
    fun `new video identity always transitions to Verifying before resolving to Blocked`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository().apply {
            saveDirect("@distracting_channel", ChannelPolicy.DISTRACTING)
        }
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            assertTrue(awaitItem() is PolicyState.Verifying)

            engine.onVideoDetected(
                VideoIdentity(title = "Distracting Video", channelHandle = "@distracting_channel", isShorts = false, isAd = false)
            )

            val first = awaitItem()
            assertTrue("Expected Verifying first, got $first", first is PolicyState.Verifying)

            val second = awaitItem()
            assertTrue("Expected Blocked after resolution, got $second", second is PolicyState.Blocked)
            assertEquals("@distracting_channel", (second as PolicyState.Blocked).channelHandle)
        }
    }

    @Test
    fun `consecutive allowed videos must re-enter Verifying on each video transition`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository().apply {
            saveDirect("@khanacademy", ChannelPolicy.PRODUCTIVE)
            saveDirect("@mitocw", ChannelPolicy.PRODUCTIVE)
        }
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Video 1
            engine.onVideoDetected(VideoIdentity(title = "Math 101", channelHandle = "@khanacademy"))
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Video 2: Different video identity
            engine.onVideoDetected(VideoIdentity(title = "Physics 101", channelHandle = "@mitocw"))
            val verifying = awaitItem()
            assertTrue("Must re-enter Verifying on video transition", verifying is PolicyState.Verifying)
            val allowed = awaitItem()
            assertTrue("Resolves to Allowed for second channel", allowed is PolicyState.Allowed)
        }
    }

    // ── Acceptance Criterion 2: Shorts Blocked Independently of Channel Policy ──

    @Test
    fun `shorts are blocked with ShortsBlocked even if channel is PRODUCTIVE`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository().apply {
            saveDirect("@veritasium", ChannelPolicy.PRODUCTIVE)
        }
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            assertTrue(awaitItem() is PolicyState.Verifying)

            engine.onVideoDetected(
                VideoIdentity(
                    title = "Short Video Clip",
                    channelHandle = "@veritasium",
                    isShorts = true,
                    isAd = false
                )
            )

            assertTrue(awaitItem() is PolicyState.Verifying)
            val state = awaitItem()
            assertTrue("Shorts must be blocked regardless of channel classification", state is PolicyState.ShortsBlocked)
        }
    }

    @Test
    fun `shorts from unclassified channel are blocked immediately without prompting user`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository() // empty
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            assertTrue(awaitItem() is PolicyState.Verifying)

            engine.onVideoDetected(
                VideoIdentity(title = "Viral Reel", channelHandle = "@unknown_creator", isShorts = true, isAd = false)
            )

            assertTrue(awaitItem() is PolicyState.Verifying)
            val state = awaitItem()
            assertTrue("Shorts must not transition to AskUser", state is PolicyState.ShortsBlocked)
        }
    }

    // ── Acceptance Criterion 3: Ad Transitions Retain Previous Verified Policy ──

    @Test
    fun `ad transition during PRODUCTIVE video retains Allowed state without evaluating ad channel`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository().apply {
            saveDirect("@veritasium", ChannelPolicy.PRODUCTIVE)
            saveDirect("@sponsor_ad", ChannelPolicy.DISTRACTING) // Even if ad sponsor handle is marked DISTRACTING
        }
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Step 1: Main productive video starts
            engine.onVideoDetected(
                VideoIdentity(title = "Educational Video", channelHandle = "@veritasium", isShorts = false, isAd = false)
            )
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Allowed)

            // Step 2: Mid-roll ad begins
            engine.onVideoDetected(
                VideoIdentity(title = "Sponsored Ad", channelHandle = "@sponsor_ad", isShorts = false, isAd = true)
            )
            assertTrue(awaitItem() is PolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Ad during productive session must retain Allowed policy", adState is PolicyState.Allowed)
        }
    }

    @Test
    fun `ad transition during DISTRACTING video retains Blocked state`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository().apply {
            saveDirect("@gaming_hub", ChannelPolicy.DISTRACTING)
            saveDirect("@ad_company", ChannelPolicy.PRODUCTIVE)
        }
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            assertTrue(awaitItem() is PolicyState.Verifying)

            // Distracting video
            engine.onVideoDetected(VideoIdentity(title = "Game Clip", channelHandle = "@gaming_hub", isShorts = false, isAd = false))
            assertTrue(awaitItem() is PolicyState.Verifying)
            assertTrue(awaitItem() is PolicyState.Blocked)

            // Ad plays
            engine.onVideoDetected(VideoIdentity(title = "Insurance Ad", channelHandle = "@ad_company", isShorts = false, isAd = true))
            assertTrue(awaitItem() is PolicyState.Verifying)
            val adState = awaitItem()
            assertTrue("Ad on blocked video must retain Blocked policy", adState is PolicyState.Blocked)
        }
    }

    // ── User Classification Flow ──

    @Test
    fun `unclassified video prompts AskUser and user selection updates state and saves to repository`() = runTest {
        val fakeRepo = FakeYouTubeChannelRepository()
        val engine = YouTubePolicyEngine(fakeRepo, TestScope(dispatcherRule.testDispatcher))

        engine.state.test {
            assertTrue(awaitItem() is PolicyState.Verifying)

            engine.onVideoDetected(
                VideoIdentity(title = "New Tutorial", channelHandle = "@new_teacher", isShorts = false, isAd = false)
            )
            assertTrue(awaitItem() is PolicyState.Verifying)
            val askState = awaitItem()
            assertTrue(askState is PolicyState.AskUser)
            assertEquals("@new_teacher", (askState as PolicyState.AskUser).channelHandle)

            // User taps "Productive" in Compose overlay
            engine.onUserClassified("@new_teacher", ChannelPolicy.PRODUCTIVE)

            val allowedState = awaitItem()
            assertTrue(allowedState is PolicyState.Allowed)
            assertEquals(ChannelPolicy.PRODUCTIVE, fakeRepo.getChannelPolicy("@new_teacher"))
        }
    }
}

// ── Test Double: Fake Repository ──

class FakeYouTubeChannelRepository : YouTubeChannelRepository {
    private val channels = mutableMapOf<String, YouTubeChannelEntity>()
    private val flow = MutableStateFlow<List<YouTubeChannelEntity>>(emptyList())

    fun saveDirect(handle: String, policy: ChannelPolicy) {
        channels[handle] = YouTubeChannelEntity(channelHandle = handle, policy = policy)
        flow.value = channels.values.toList()
    }

    override suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy? {
        return channels[channelHandle]?.policy
    }

    override suspend fun saveChannelPolicy(channelHandle: String, channelName: String, policy: ChannelPolicy) {
        channels[channelHandle] = YouTubeChannelEntity(
            channelHandle = channelHandle,
            channelName = channelName,
            policy = policy
        )
        flow.value = channels.values.toList()
    }

    override fun getAllChannels(): Flow<List<YouTubeChannelEntity>> = flow.asStateFlow()
}
```

#### 2.5.2 Unit / Instrumented Tests for `YouTubeChannelDao`
Because `app/build.gradle.kts` configures `androidTestImplementation(libs.androidx.junit)` and `androidTestImplementation(libs.androidx.espresso.core)`, an in-memory Room test can be co-located either as an Android instrumentation test (`app/src/androidTest/...`) or as a JVM unit test using Robolectric.

##### Option A: Room In-Memory Instrumented Test (`app/src/androidTest/...`)
```kotlin
package com.safarparmar.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.safarparmar.app.data.local.SafarDatabase
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YouTubeChannelDaoTest {

    private lateinit var database: SafarDatabase
    private lateinit var dao: YouTubeChannelDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SafarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.youTubeChannelDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveChannelPolicy() = runTest {
        val entity = YouTubeChannelEntity(
            channelHandle = "@veritasium",
            channelName = "Veritasium",
            policy = ChannelPolicy.PRODUCTIVE
        )
        dao.insertOrUpdate(entity)

        val retrieved = dao.getChannelByHandle("@veritasium")
        assertNotNull(retrieved)
        assertEquals("@veritasium", retrieved?.channelHandle)
        assertEquals("Veritasium", retrieved?.channelName)
        assertEquals(ChannelPolicy.PRODUCTIVE, retrieved?.policy)

        val policyOnly = dao.getPolicyByHandle("@veritasium")
        assertEquals(ChannelPolicy.PRODUCTIVE, policyOnly)
    }

    @Test
    fun updateExistingChannelPolicyToDistracting() = runTest {
        val initial = YouTubeChannelEntity(
            channelHandle = "@tech_reviews",
            policy = ChannelPolicy.PRODUCTIVE
        )
        dao.insertOrUpdate(initial)
        assertEquals(ChannelPolicy.PRODUCTIVE, dao.getPolicyByHandle("@tech_reviews"))

        val updated = initial.copy(policy = ChannelPolicy.DISTRACTING)
        dao.insertOrUpdate(updated)
        assertEquals(ChannelPolicy.DISTRACTING, dao.getPolicyByHandle("@tech_reviews"))
    }

    @Test
    fun deleteChannelRemovesFromDatabase() = runTest {
        val entity = YouTubeChannelEntity(
            channelHandle = "@to_delete",
            policy = ChannelPolicy.DISTRACTING
        )
        dao.insertOrUpdate(entity)
        assertEquals(ChannelPolicy.DISTRACTING, dao.getPolicyByHandle("@to_delete"))

        dao.deleteByHandle("@to_delete")
        assertNull(dao.getChannelByHandle("@to_delete"))
        assertNull(dao.getPolicyByHandle("@to_delete"))
    }

    @Test
    fun getAllChannelsFlowEmitsUpdatedList() = runTest {
        dao.insertOrUpdate(YouTubeChannelEntity("@ch1", policy = ChannelPolicy.PRODUCTIVE))
        dao.insertOrUpdate(YouTubeChannelEntity("@ch2", policy = ChannelPolicy.DISTRACTING))

        val list = dao.getAllChannels().first()
        assertEquals(2, list.size)
    }
}
```

##### Option B: Running Room In-Memory Tests on the JVM via Robolectric
If desired to run DAO tests directly in `./gradlew testQaDebugUnitTest` alongside `YouTubePolicyEngineTest` (without launching an Android device/emulator):
- Add `testImplementation("org.robolectric:robolectric:4.13")` to `app/build.gradle.kts`.
- Annotate the test class with `@RunWith(RobolectricTestRunner::class)`.
- Room in-memory SQLite executes natively on Mac/Linux JVM!

---

## 3. Caveats

1. **Accessibility View Hierarchy Latency vs MediaSession Metadata**:
   - In the YouTube app, when a video starts, the accessibility node tree may initially only show the video player view or title, while the channel handle appears a fraction of a second later (or after the description loads).
   - *Mitigation*: The state machine's invariant of defaulting to `Verifying` gracefully handles this latency: the screen remains fail-closed in `Verifying` until the channel handle is extracted.
2. **Channel Handle Extraction Variability**:
   - YouTube sometimes exposes channel handles (e.g. `@mkbhd`) and other times channel display names (e.g. `Marques Brownlee`).
   - *Recommendation*: Channel classification should match against normalized handles first, and fall back to exact channel display names. The `YouTubeChannelEntity.channelHandle` primary key should store the canonical handle (or normalized name if handle is absent).
3. **Room JVM Unit Testing without Robolectric**:
   - Standard Room 2.6 on the pure JVM requires SQLite native bindings. Without Robolectric or `room-testing`, running `Room.inMemoryDatabaseBuilder` in JVM unit tests produces `RuntimeException("Method ... not mocked")`.
   - *Recommendation*: Keep `YouTubePolicyEngineTest` pure Kotlin using `FakeYouTubeChannelRepository` (runs 100% on JVM with zero Android dependencies), and run `YouTubeChannelDaoTest` either via Android instrumented test (`androidTest`) or with Robolectric.
4. **Ad Channel Name in Accessibility Events**:
   - Some YouTube ads expose a channel handle of the advertiser (e.g. `@geico`). Without the ad invariant, the engine would prompt `AskUser` for `@geico`.
   - *Resolution*: The state machine's `isAd` logic explicitly skips channel lookup and retains `lastVerifiedPolicy`.

---

## 4. Conclusion

1. **State Machine (`YouTubePolicyEngine`)**:
   - Must be designed as a pure Kotlin, lifecycle-aware deterministic engine utilizing Kotlin `StateFlow<PolicyState>`.
   - Must encapsulate the 5 required states: `Verifying`, `AskUser`, `Allowed`, `Blocked`, `ShortsBlocked`.
   - Explicitly fulfills the three core invariants:
     1. Always transitions to `Verifying` before resolving to `Allowed` or `Blocked`.
     2. Shorts are always transitioned to `ShortsBlocked` regardless of whether the channel is marked `PRODUCTIVE`.
     3. Ads retain the previous verified channel's policy (`Allowed` or `Blocked`) without evaluating ad metadata or prompting `AskUser`.
2. **Persistence (`YouTubeChannelEntity` & Room)**:
   - The project currently has Room 2.6.1 runtime and compiler configured in `app/build.gradle.kts`, but has zero existing Room databases or DAOs.
   - A new `SafarDatabase` (version 1) with `YouTubeChannelEntity`, `ChannelPolicy` (`PRODUCTIVE`/`DISTRACTING`), `ChannelPolicyConverter`, and `YouTubeChannelDao` can be cleanly integrated via Hilt `DatabaseModule`.
3. **Testing Infrastructure**:
   - Gradle build has JUnit 4, Kotlinx Coroutines Test 1.9.0, Turbine 1.1.0, and MockK 1.13.12 already wired.
   - All tests in the repository are fast JVM unit tests under `app/src/test`.
   - `YouTubePolicyEngine` can be tested 100% on JVM using JUnit 4 + Turbine + Coroutines Test + Fake Repository, executing in seconds via `./gradlew :app:testQaDebugUnitTest`.
   - OpenJDK 17 must be exported (`JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home"`) for CLI gradle executions.

---

## 5. Verification Method

### 5.1 Verifying Test Execution
Execute existing project unit tests to confirm JVM test harness viability:
```bash
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home"
cd /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android
./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.util.SafeApiCallTest"
```
**Expected Result**: `BUILD SUCCESSFUL`.

### 5.2 Verifying `YouTubePolicyEngineTest` Once Implemented
Once the implementer writes `YouTubePolicyEngine` and `YouTubePolicyEngineTest`:
```bash
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home"
cd /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android
./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest"
```
**Expected Result**: All tests pass:
- `new video identity always transitions to Verifying before resolving to Allowed`
- `new video identity always transitions to Verifying before resolving to Blocked`
- `shorts are blocked with ShortsBlocked even if channel is PRODUCTIVE`
- `ad transition during PRODUCTIVE video retains Allowed state`
- `ad transition during DISTRACTING video retains Blocked state`
- `unclassified video prompts AskUser`

### 5.3 Invalidation Conditions
This survey and blueprint will be invalidated if:
1. Room version is downgraded below 2.6.0.
2. AGP or Kotlin version changes breaking KSP annotation processing.
3. YouTube app modifies its Shorts or Ad accessibility node structure such that `isShorts` or `isAd` flags cannot be determined prior to passing `VideoIdentity` to the engine.
