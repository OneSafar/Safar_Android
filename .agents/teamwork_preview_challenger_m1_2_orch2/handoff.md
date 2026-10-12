# Milestone 1 Adversarial Challenge Report

**Verdict**: `REQUEST_CHANGES`

---

## 1. Observation

### 1.1 Test Suite & Verification Execution
- Executed empirical test suite: `app/src/test/java/com/safarparmar/app/feature/youtube/Milestone1AdversarialChallengeTest.kt`
- Command:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.Milestone1AdversarialChallengeTest"
  ```
- Verbatim XML execution result from `app/build/test-results/testQaDebugUnitTest/TEST-com.safarparmar.app.feature.youtube.Milestone1AdversarialChallengeTest.xml`:
  ```xml
  <testsuite name="com.safarparmar.app.feature.youtube.Milestone1AdversarialChallengeTest" tests="10" skipped="0" failures="0" errors="0" timestamp="2026-09-11T16:12:47.568Z" hostname="Shashanks-MacBook-Air.local" time="0.195">
  ```
  Standard output captured from test execution:
  ```
  Entity created with '@': channelHandle=@, canonicalKey=name:khan academy
  ChannelIdentity.from('Khan Academy', 'Khan Academy'): handle=@khan academy, canonicalKey=handle:@khan academy
  Policy when queried with handle: PRODUCTIVE
  Policy when queried with name: DISTRACTING
  Concurrent table entries count: 2
  Keys: [name:concurrent chan, handle:@concurrent_chan]
  getChannelPolicy('Khan Academy'): PRODUCTIVE
  getChannelPolicy('  khan academy  '): PRODUCTIVE
  Stored entity: canonicalKey=handle:@khan academy, channelHandle=@khan academy, displayName=Khan Academy
  DAO Table keys after handle association: [name:khan academy, handle:@khanacademy]
  Has duplicates in storage: true (count = 2)
  Policy resolved for nameOnlyIdentity: PRODUCTIVE (expected DISTRACTING, got PRODUCTIVE)
  Remaining entities after deleteChannel(handleIdentity): [name:khan academy]
  Policy for nameOnlyIdentity after delete: PRODUCTIVE
  DAO Table after deleteChannel('Khan Academy'): [handle:@khanacademy]
  normalizeHandle('Khan Academy') -> '@khan academy'
  ```

### 1.2 Observed Code Defects in Inspected Files
1. **Duplicate Entities & Stale Lookup on Display-Name to Handle Association**:
   - In `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`:
     - Line 30-32: `@PrimaryKey @ColumnInfo(name = "canonicalKey") val canonicalKey: String`
   - In `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`:
     - Lines 46-59 (`savePolicy`): Inserts `YouTubeChannelEntity` under `identity.canonicalKey`. When saving an identity with only `displayName` (`"Khan Academy"`), `canonicalKey` is `"name:khan academy"`. When later saving with verified handle (`"@khanacademy"`), `canonicalKey` is `"handle:@khanacademy"`.
     - Observation: `savePolicy` does not clean up or link pre-existing `name:<normalizedDisplayName>` entities. Both entities remain in storage concurrently (`DAO Table keys after handle association: [name:khan academy, handle:@khanacademy]`).
     - Lines 23-29 (`getPolicy`): When a subsequent video only exposes the display name ("Khan Academy"), `identity.canonicalKey` is `"name:khan academy"`. Line 27 retrieves the stale record and returns `PRODUCTIVE` even though the user reclassified the channel to `DISTRACTING` under its handle (`Policy resolved for nameOnlyIdentity: PRODUCTIVE (expected DISTRACTING, got PRODUCTIVE)`).
     - Lines 170-180 (`deleteChannel(identity: ChannelIdentity)`): When deleting by handle identity (`ChannelIdentity("@khanacademy", "Khan Academy")`), only `canonicalKey` (`handle:@khanacademy`) and `handle` (`@khanacademy`) are deleted. The orphan row `name:khan academy` remains in storage (`Remaining entities after deleteChannel(handleIdentity): [name:khan academy]`). A subsequent check via `getPolicy(nameOnlyIdentity)` continues returning the deleted channel's policy.

2. **Synthetic Fake Handle Creation via `normalizeHandle`**:
   - In `app/src/main/java/com/safarparmar/app/feature/youtube/model/ChannelIdentity.kt`:
     - Lines 54-60:
       ```kotlin
       fun normalizeHandle(handle: String): String {
           val trimmed = handle.trim()
           if (trimmed.isEmpty()) return ""
           val stripped = trimmed.trimStart('@').trim()
           if (stripped.isEmpty()) return ""
           return "@${stripped.lowercase()}"
       }
       ```
     - Line 77-87 (`ChannelIdentity.from(handle, displayName)`): Calls `normalizeHandle(handle)`. If caller passes a display name with spaces (e.g. `"Khan Academy"`), `normalizeHandle` returns `"@khan academy"`.
     - In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt` line 136:
       `val currentHandle = current?.channelHandle?.trim()?.ifEmpty { current?.channelName?.trim() }`
       When `channelHandle` is empty, `currentHandle` falls back to `channelName`. It passes this to `repository.saveChannelPolicy(currentHandle, ...)`.
     - In `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt` line 87:
       `cleanHandle = channelHandle.trim().takeIf { it.isNotEmpty() }?.let { ChannelIdentity.normalizeHandle(it) }`
       This produces `@khan academy` and stores it into Room as `channelHandle = "@khan academy"` and `canonicalKey = "handle:@khan academy"`.
     - Observation: A string containing spaces prepended with `@` is created and persisted as a verified handle in Room (`Stored entity: canonicalKey=handle:@khan academy, channelHandle=@khan academy, displayName=Khan Academy`).

3. **Invalid Handle Symbol Persisted by Secondary Constructor**:
   - In `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`:
     - Lines 54-65:
       ```kotlin
       @Ignore
       constructor(
           channelHandle: String?,
           channelName: String = "",
           policy: ChannelPolicy,
           updatedAt: Long = System.currentTimeMillis(),
       ) : this(
           canonicalKey = computeCanonicalKey(channelHandle, channelName),
           channelHandle = channelHandle?.trim()?.takeIf { it.isNotEmpty() },
           displayName = channelName.trim(),
           policy = policy,
           updatedAt = updatedAt,
       )
       ```
     - Observation: `channelHandle = channelHandle?.trim()?.takeIf { it.isNotEmpty() }` does not validate or normalize `channelHandle`. When passing `"@"`, `computeCanonicalKey` returns `"name:khan academy"`, but `entity.channelHandle` stores `"@"` (`Entity created with '@': channelHandle=@, canonicalKey=name:khan academy`).

4. **Deletion Defect for Backward-Compatible Callers**:
   - In `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`:
     - Lines 150-169 (`deleteChannel(channelHandle: String)`):
       When existing callers pass a channel name to `deleteChannel("Khan Academy")`, it only deletes `handle:@khan academy` and `name:khan academy`. If the channel was persisted with its verified handle (`handle:@khanacademy`), calling `deleteChannel("Khan Academy")` does not delete it (`DAO Table after deleteChannel('Khan Academy'): [handle:@khanacademy]`).

---

## 2. Logic Chain

1. **Premise 1 (State Consistency & Identity Linking)**:
   - A YouTube channel is a single physical entity regardless of whether the client initially observes its display name or its handle.
   - When a channel is classified first as `PRODUCTIVE` by display name (`"Khan Academy"`), and later re-classified as `DISTRACTING` when its handle (`"@khanacademy"`) is detected:
     - `YouTubeChannelEntity` uses `canonicalKey` as its `@PrimaryKey`.
     - Storing under `"name:khan academy"` and `"handle:@khanacademy"` produces two separate records in the database with opposing policies.
     - Lookups for `ChannelIdentity(null, "Khan Academy")` match the stale `"name:khan academy"` record first, returning `PRODUCTIVE` and bypassing user blocking.
     - Deleting `ChannelIdentity("@khanacademy", "Khan Academy")` leaves the `"name:khan academy"` record in the database, meaning the channel cannot be removed.
   - **Conclusion**: The repository must reconcile display-name entries when a verified handle is saved or deleted for the same display name.

2. **Premise 2 (Strict Ban on Fake Handles)**:
   - Requirement R2 and Milestone 1 specification mandate that no synthetic fake handles may be generated or persisted anywhere.
   - Real YouTube handles cannot contain spaces (`[a-zA-Z0-9._-]`).
   - `ChannelIdentity.normalizeHandle` blindly prepends `@` to any string with text, turning `"Khan Academy"` into `"@khan academy"`.
   - `YouTubePolicyEngine` passes `channelName` into `channelHandle` when no handle is present, causing `YouTubeChannelRepositoryImpl.saveChannelPolicy` to persist `"@khan academy"` as a `channelHandle` in Room.
   - **Conclusion**: `normalizeHandle` must validate handle syntax (e.g. reject spaces and special characters), and `saveChannelPolicy` must treat invalid handles (or handles with spaces) as display names rather than verified handles.

3. **Premise 3 (Secondary Constructor Consistency)**:
   - `YouTubeChannelEntity` secondary constructor assigns `channelHandle = channelHandle?.trim()?.takeIf { it.isNotEmpty() }`.
   - If `channelHandle = "@"`, it stores `"@"` as the handle instead of `null`.
   - **Conclusion**: `channelHandle` in the secondary constructor must be normalized via `ChannelIdentity.normalizeHandle(channelHandle).takeIf { it.isNotEmpty() }`.

4. **Premise 4 (Backward Compatibility)**:
   - Existing callers invoke `deleteChannel(channelHandle: String)` with channel names.
   - When a channel is stored under its canonical handle, `deleteChannel(channelName)` fails to locate or delete the channel by name.
   - **Conclusion**: `deleteChannel` must look up existing entities by `displayName` and delete their canonical keys.

---

## 3. Caveats

- **No Caveats**: All findings were empirically tested and reproduced using a dedicated test suite (`Milestone1AdversarialChallengeTest.kt`) executed via Gradle.
- The standard test suite (`:app:testQaDebugUnitTest`) compiles and passes cleanly alongside our adversarial suite.

---

## 4. Conclusion

**Verdict**: `REQUEST_CHANGES`

Milestone 1 cannot be approved in its current state due to 4 actionable defects:
1. **Critical**: Storing channels by display name and later by verified handle creates duplicate database records, stale policy resolution, and un-deletable orphan records.
2. **High**: `ChannelIdentity.normalizeHandle` allows display names with spaces to become fake handles (e.g. `"@khan academy"`), which are persisted to Room by `saveChannelPolicy`.
3. **Medium**: `YouTubeChannelEntity` secondary constructor allows invalid handle strings like `"@"` to be persisted as non-null `channelHandle`.
4. **High**: `deleteChannel(channelHandle: String)` fails to delete channels stored by handle when queried with their display name.

### Required Changes for Worker:
1. In `YouTubeChannelRepositoryImpl.savePolicy` and `saveChannelPolicy`:
   - When saving an identity with a verified handle and a non-empty `displayName`, remove any pre-existing orphan entry with `canonicalKey = "name:${normalizeDisplayName(displayName)}"`.
   - When saving an identity with only `displayName`, if an entity with matching `displayName` already exists with a verified handle, update that entity's policy rather than creating a disconnected `name:` record.
2. In `YouTubeChannelRepositoryImpl.deleteChannel`:
   - Delete both the `canonicalKey` and any associated `name:${normalizeDisplayName(displayName)}` or handle records.
   - When deleting by string `channelHandle`, if `getChannelByName(trimmed)` finds an entity, delete that entity's `canonicalKey`.
3. In `ChannelIdentity.normalizeHandle`:
   - Validate handle characters (e.g., must match `^[a-zA-Z0-9._-]+$` after stripping `@`). If the string contains spaces or invalid characters, return `""`.
4. In `YouTubeChannelEntity` secondary constructor:
   - Normalize `channelHandle` using `ChannelIdentity.normalizeHandle(channelHandle).takeIf { it.isNotEmpty() }` so invalid handles like `"@"` become `null`.

---

## 5. Verification Method

To independently verify these findings:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.Milestone1AdversarialChallengeTest"
```
Inspect the output in `app/build/test-results/testQaDebugUnitTest/TEST-com.safarparmar.app.feature.youtube.Milestone1AdversarialChallengeTest.xml`.

### Invalidation Conditions:
This challenge report is invalidated if:
1. Saving a channel by display name and subsequently by handle does not produce duplicate entities in storage.
2. Querying `getPolicy` after associating a handle returns the updated policy rather than the stale display-name policy.
3. Passing a display name with spaces to `normalizeHandle` returns `""` instead of prepending `@`.
4. `deleteChannel("Khan Academy")` removes the channel even if persisted under `handle:@khanacademy`.
