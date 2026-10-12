# Project: YouTube Guard

## Architecture
- **Package Namespace**: `com.safarparmar.app`
- **Module**: Single-module Android app (`:app`)
- **Key Subsystems**:
  1. **Persistence & Policy Engine**: Room Database (`SafarDatabase`), DAO (`YouTubeChannelDao`), Entity (`YouTubeChannelEntity`), TypeConverter (`ChannelPolicyConverter`), Repository (`YouTubeChannelRepository`), Deterministic State Machine (`YouTubePolicyEngine`), states (`Verifying`, `AskUser`, `Allowed`, `Blocked`, `ShortsBlocked`), `VideoIdentity` model.
  2. **Detection Services**: `YouTubeGuardService` (`AccessibilityService`) parsing YouTube DOM + `YouTubeNotificationListener` (`MediaSessionManager` active sessions listener) catching fullscreen/playback metadata changes.
  3. **Compose Overlay**: `TYPE_ACCESSIBILITY_OVERLAY` Compose UI (`YouTubeBlockOverlay`) hosted on a custom `ComposeServiceLifecycleOwner` (Lifecycle + ViewModelStore + SavedStateRegistry).

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Channel Policy Entity & DAO | Room entity `YouTubeChannelEntity` with `ChannelPolicy` (PRODUCTIVE, DISTRACTING) and `YouTubeChannelDao` | M1 | ORIGINAL_REQUEST §R3 |
| 2 | Room Database & DI | `SafarDatabase` (v1) with `ChannelPolicyConverter`, Hilt `DatabaseModule`, and `YouTubeChannelRepository` | M1 | ORIGINAL_REQUEST §R3 |
| 3 | Policy State Machine Model | `PolicyState` (Verifying, AskUser, Allowed, Blocked, ShortsBlocked) and `VideoIdentity` model | M1 | ORIGINAL_REQUEST §R2 |
| 4 | Deterministic Transition Logic | Invariant 1: Fail-closed `Verifying` on any identity change; Invariant 2: Shorts independent block; Invariant 3: Ad policy retention | M1 | ORIGINAL_REQUEST §R2, Acceptance Criteria |
| 5 | Accessibility Guard Service | `YouTubeGuardService` watching `com.google.android.youtube` with `flagReportViewIds` and `flagIncludeNotImportantViews` | M2 | ORIGINAL_REQUEST §R1 |
| 6 | MediaSession Fullscreen Detection | `YouTubeNotificationListener` observing active sessions for fullscreen & background metadata changes | M2 | ORIGINAL_REQUEST §R1 |
| 7 | Compose Overlay Lifecycle Support | `ComposeServiceLifecycleOwner` implementing `LifecycleOwner`, `ViewModelStoreOwner`, `SavedStateRegistryOwner` | M3 | ORIGINAL_REQUEST §R4 |
| 8 | Accessibility Compose Overlay UI | `TYPE_ACCESSIBILITY_OVERLAY` Compose UI handling `AskUser`, `Blocked`, and `ShortsBlocked` screens | M3 | ORIGINAL_REQUEST §R4, Acceptance Criteria |
| 9 | E2E Integration & Verification | 100% pass of E2E test suite (Tiers 1-4) & adversarial test hardening (Tier 5) | M4 | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Data Layer & Deterministic Policy Engine | `YouTubeChannelEntity`, `ChannelPolicy`, `ChannelPolicyConverter`, `YouTubeChannelDao`, `SafarDatabase`, `DatabaseModule`, `YouTubeChannelRepository`, `YouTubePolicyEngine`, `PolicyState`, `VideoIdentity`, unit test suite | none | IN_PROGRESS |
| M2 | Detection Services | `YouTubeGuardService`, `res/xml/youtube_guard_accessibility_service.xml`, `YouTubeNotificationListener`, manifest declarations | M1 | PLANNED |
| M3 | Compose Accessibility Overlay | `ComposeServiceLifecycleOwner`, `YouTubeBlockOverlay`, WindowManager `TYPE_ACCESSIBILITY_OVERLAY` integration | M1, M2 | PLANNED |
| M4 | E2E Integration & Coverage Hardening | Pass 100% E2E test suite + adversarial coverage hardening | M1, M2, M3 | PLANNED |

## Interface Contracts
### M1 ↔ M2/M3: Policy Engine Contract
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

### M1: Persistence Contract
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

## Code Layout
- `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt` (M1)
- `app/src/main/java/com/safarparmar/app/data/local/converter/ChannelPolicyConverter.kt` (M1)
- `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt` (M1)
- `app/src/main/java/com/safarparmar/app/data/local/SafarDatabase.kt` (M1)
- `app/src/main/java/com/safarparmar/app/di/DatabaseModule.kt` (M1)
- `app/src/main/java/com/safarparmar/app/domain/repository/YouTubeChannelRepository.kt` (M1)
- `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt` (M1)
- `app/src/main/java/com/safarparmar/app/feature/youtube/policy/PolicyState.kt` (M1)
- `app/src/main/java/com/safarparmar/app/feature/youtube/policy/VideoIdentity.kt` (M1)
- `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt` (M1)
- `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt` (M1)
- `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeGuardService.kt` (M2)
- `app/src/main/java/com/safarparmar/app/feature/youtube/service/YouTubeNotificationListener.kt` (M2)
- `app/src/main/res/xml/youtube_guard_accessibility_service.xml` (M2)
- `app/src/main/AndroidManifest.xml` (M2)
- `app/src/main/java/com/safarparmar/app/feature/youtube/ui/ComposeServiceLifecycleOwner.kt` (M3)
- `app/src/main/java/com/safarparmar/app/feature/youtube/ui/YouTubeBlockOverlay.kt` (M3)
- `app/src/test/java/com/safarparmar/app/feature/youtube/ui/ComposeServiceLifecycleOwnerTest.kt` (M3)
- `app/src/test/java/com/safarparmar/app/feature/youtube/e2e/YouTubeGuardE2ETest.kt` (M4 / E2E Track)
