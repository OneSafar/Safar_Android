# Task Assignment: Milestone 1 Worker (orchestrator_2)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_orch2`

## Scope: Milestone 1 (Phases 2, 5, 14)
1. **Canonical Channel Identity (Phase 2)**:
   - Create `app/src/main/java/com/safarparmar/app/feature/youtube/model/ChannelIdentity.kt`:
     - `verifiedHandle: String? = null` (ONLY real `@handle` detected, null otherwise).
     - `displayName: String = ""`.
     - `canonicalKey: String` computed as:
       - If `!verifiedHandle.isNullOrBlank()` -> `"handle:${normalizeHandle(verifiedHandle)}"`
       - Else if `displayName.isNotBlank()` -> `"name:${normalizeDisplayName(displayName)}"`
       - Else `""`
     - Normalization helpers: `normalizeHandle` ensures lowercase with leading `@`. `normalizeDisplayName` ensures trimmed, lowercase, collapsed spaces.
     - Strictly BAN all synthetic fake handle generation.

2. **Video Fingerprinting (Phase 5)**:
   - Create `app/src/main/java/com/safarparmar/app/feature/youtube/model/VideoFingerprint.kt`:
     - `mediaId: String? = null`
     - `normalizedTitle: String = ""`
     - `channelCanonicalKey: String = ""`
     - `key: String`:
       - If `!mediaId.isNullOrBlank()` -> `"media:$mediaId"`
       - Else if `normalizedTitle.isNotBlank() || channelCanonicalKey.isNotBlank()` -> `"content:$normalizedTitle|$channelCanonicalKey"`
       - Else `""`
   - Create `app/src/main/java/com/safarparmar/app/feature/youtube/model/CanonicalYouTubeContent.kt` and `DetectionConfidence.kt`:
     - `DetectionConfidence` enum: `HIGH`, `MEDIUM`, `LOW`, `UNKNOWN`.
     - `CanonicalYouTubeContent`: `fingerprint: VideoFingerprint`, `channelIdentity: ChannelIdentity`, `title: String`, `isShorts: Boolean`, `isAd: Boolean`, `isChannelPage: Boolean`, `confidence: DetectionConfidence`.

3. **Room Persistence Without Fake Handles (Phase 14)**:
   - Update `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`:
     - `@PrimaryKey @ColumnInfo(name = "canonicalKey") val canonicalKey: String`
     - `@ColumnInfo(name = "channelHandle", collate = ColumnInfo.NOCASE) val channelHandle: String? = null`
     - `@ColumnInfo(name = "displayName") val displayName: String = ""`
     - `@ColumnInfo(name = "policy") val policy: ChannelPolicy`
     - `@ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()`
   - Update `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt`:
     - `getChannelByCanonicalKey(canonicalKey: String): YouTubeChannelEntity?`
     - `getPolicyByCanonicalKey(canonicalKey: String): ChannelPolicy?`
     - `getChannelByHandle(channelHandle: String): YouTubeChannelEntity?`
     - `getPolicyByHandle(channelHandle: String): ChannelPolicy?`
     - Retain backward compatible queries where appropriate.
   - Update `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepository.kt` & `YouTubeChannelRepositoryImpl.kt`:
     - Methods to get/save policy by `ChannelIdentity`:
       - `suspend fun getPolicy(identity: ChannelIdentity): ChannelPolicy?`
       - `suspend fun savePolicy(identity: ChannelIdentity, policy: ChannelPolicy)`
     - Fallback / compatibility methods `getPolicyByHandle(handle: String)` and `saveChannel(handle, name, policy)`.
     - DO NOT fabricate fake handles in repository!
   - Update `app/src/main/java/com/safarparmar/app/data/local/SafarDatabase.kt` if entity fields change (note: `fallbackToDestructiveMigration()` is active in `DatabaseModule.kt`).

4. **Unit Tests & Verification**:
   - Update unit tests in `app/src/test/java/com/safarparmar/app/feature/youtube/` and `app/src/test/java/com/safarparmar/app/data/`:
     - Test `ChannelIdentity` canonical key generation with real handle, display name only, and edge cases.
     - Test `VideoFingerprint` generation with mediaId, title, and channel key.
     - Test `YouTubeChannelRepositoryImpl` and persistence tests (`YouTubePersistenceAdversarialTest.kt`, etc.) verifying that saving by display name works without fake handles and case-insensitive handle lookup succeeds.
   - Run compilation and tests:
     `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`

## Mandatory Reading
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md` (read the entire file, especially latest request `## 2026-09-11T15:25:10Z`)
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/orchestrator_2/PROJECT.md`
- Survey Explorer 1 Report: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_1_orch2/handoff.md`

## Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Write Ownership
Exclusive write access to:
- `app/src/main/java/com/safarparmar/app/feature/youtube/model/` (new files)
- `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`
- `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt`
- `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepository.kt`
- `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`
- `app/src/test/java/com/safarparmar/app/data/` and new unit tests for models

## Output Requirements
Produce `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_orch2/handoff.md` with:
- Files modified and created.
- Build and test execution output.
- Verification that all unit tests pass cleanly.

## 2026-09-11T15:32:21Z
You are Milestone 1 Worker. Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_orch2.
Read your instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_orch2/DISPATCH.md and read the mandatory specification in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md.
Also read the project architecture in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/orchestrator_2/PROJECT.md and the survey report in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_survey_1_orch2/handoff.md.

Implement Milestone 1 (Phases 2, 5, 14):
- Models: ChannelIdentity, VideoFingerprint, CanonicalYouTubeContent, DetectionConfidence
- Room Entity: YouTubeChannelEntity (canonicalKey primary key, verifiedHandle nullable, displayName)
- Dao & Repository: YouTubeChannelDao, YouTubeChannelRepository, YouTubeChannelRepositoryImpl
- Tests: Add/update unit tests for models and repository, verify persistence without fake handles.
- Build & Test Verification: Run `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest` and ensure all tests compile and pass.

