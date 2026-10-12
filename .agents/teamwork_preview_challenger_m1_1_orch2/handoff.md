# Milestone 1 Challenge Report: Challenger 1

**Verdict**: `REQUEST_CHANGES`

---

## 1. Observation

### 1.1 Inspected Files and Lines
1. `app/src/main/java/com/safarparmar/app/feature/youtube/model/ChannelIdentity.kt`:
   - Line 32-33: `val hasHandle: Boolean get() = !verifiedHandle.isNullOrBlank()`
   - Line 54-60: `normalizeHandle` uses `trimmed.trimStart('@').trim()`, preserving internal spaces and ignoring fullwidth symbols (`\uFF20`).
   - Line 77-87: `from(handle, displayName)` normalizes whatever string is passed to `handle`, prepending `@` even if a display name with spaces is passed.
2. `app/src/main/java/com/safarparmar/app/feature/youtube/model/VideoFingerprint.kt`:
   - Line 23-29: `key` uses `!mediaId.isNullOrBlank() -> "media:${mediaId.trim()}"`, accepting literal `"null"` and `"undefined"` strings as valid media identifiers.
   - Line 26-27: `"content:$normalizedTitle|$channelCanonicalKey"` uses raw pipe `|` delimiter without escaping.
3. `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`:
   - Line 30-32: `@PrimaryKey @ColumnInfo(name = "canonicalKey") val canonicalKey: String`.
   - Line 54-65: Secondary constructor sets `channelHandle = channelHandle?.trim()?.takeIf { it.isNotEmpty() }`, preserving `@` even when canonical key computation ignores it.
4. `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt`:
   - Line 18-22: `getChannelByCanonicalKey` and `getPolicyByCanonicalKey` query `WHERE canonicalKey = :canonicalKey` without case collation.
   - Line 48-49: `deleteByHandle` only queries `WHERE channelHandle = :channelHandle COLLATE NOCASE`. There is no `deleteByName`.
5. `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`:
   - Line 23-45: `getPolicy(identity)` checks `getPolicyByCanonicalKey(canonicalKey)` first. If an entity was originally saved by display name (`name:khan academy`) and later reclassified via handle (`handle:@khanacademy`), querying by display name returns the stale `name:` policy.
   - Line 46-59: `savePolicy(identity, policy)` inserts directly by `canonicalKey` without reconciling or deleting any existing row under the counterpart key format (`name:` vs `handle:`).
   - Line 150-168: `deleteChannel(channelHandle: String)` tries to delete by `handle:$normalizedHandle` and `name:$normalizedName`. If the channel name has spaces (`"Khan Academy"`), `normalizeHandle("Khan Academy")` produces `"@khan academy"` and fails to delete `"handle:@khanacademy"`.
   - Line 170-180: `deleteChannel(identity: ChannelIdentity)` deletes by `identity.canonicalKey` and `identity.verifiedHandle`. If `identity` has a handle, it does NOT delete the counterpart `name:<displayName>` entity. If `identity` has no handle, it does NOT delete the counterpart `handle:<handle>` entity.
   - Line 112-129: `getChannel(channelHandle: String)` never queries `dao.getChannelByCanonicalKey`.

### 1.2 Empirical Test Execution
Created empirical test suite `app/src/test/java/com/safarparmar/app/feature/youtube/Milestone1Challenger1AdversarialTest.kt` containing 11 adversarial tests reproducing every failure mode.

Executed command:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.Milestone1Challenger1AdversarialTest"
```
Verbatim execution result:
```
> Task :app:testQaDebugUnitTest
BUILD SUCCESSFUL in 28s
36 actionable tasks: 1 executed, 35 up-to-date
```
HTML test report at `app/build/reports/tests/testQaDebugUnitTest/classes/com.safarparmar.app.feature.youtube.Milestone1Challenger1AdversarialTest.html`:
- Tests: 11
- Failures: 0
- Ignored: 0
- Duration: 0.007s
- Success rate: 100%

Full test suite execution:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest
```
Verbatim result:
```
BUILD SUCCESSFUL in 19s
36 actionable tasks: 1 executed, 35 up-to-date
```
Total tests passed: 296 tests across the application.

---

## 2. Logic Chain

### 2.1 Premise 1: Dual-Identity Divergence on Handle Association (CRITICAL DEFECT)
- **Observation**:
  - In `YouTubeChannelEntity`, `canonicalKey` is `@PrimaryKey`.
  - For display-name-only channels, `canonicalKey = "name:khan academy"`.
  - For handle-identified channels, `canonicalKey = "handle:@khanacademy"`.
  - In `YouTubeChannelRepositoryImpl.savePolicy(identity, policy)` (lines 48-58):
    When a channel is initially classified on a video watch screen where only the display name `"Khan Academy"` is present, it is saved as `name:khan academy` with policy `DISTRACTING`.
    When the user navigates to the channel header page or a screen where handle `@khanacademy` is detected, and updates the classification to `PRODUCTIVE`, `savePolicy` executes:
    `insertOrUpdate(YouTubeChannelEntity(canonicalKey = "handle:@khanacademy", policy = PRODUCTIVE, ...))`.
- **Divergence**:
  - `insertOrUpdate` replaces by primary key. Because `"name:khan academy"` and `"handle:@khanacademy"` are different primary keys, BOTH rows now exist in `youtube_channels`.
- **Consequence**:
  - When the user watches another video from Khan Academy where only display name is available, `getPolicy(ChannelIdentity(null, "Khan Academy"))` queries `dao.getPolicyByCanonicalKey("name:khan academy")` on line 27.
  - It immediately returns `DISTRACTING` (the stale initial policy). The user's explicit update to `PRODUCTIVE` is completely bypassed.
  - Furthermore, `getAllChannels()` lists duplicate rows for the same channel with contradictory policies.
- **Empirical Confirmation**:
  - Verified in `Milestone1Challenger1AdversarialTest.empirical - updating channel policy from display-name to handle leaves stale policy in DAO`.

### 2.2 Premise 2: Zombie Entity Survival on Deletion (HIGH DEFECT)
- **Observation**:
  - In `YouTubeChannelRepositoryImpl.deleteChannel(identity: ChannelIdentity)` (lines 170-180):
    ```kotlin
    val canonicalKey = identity.canonicalKey
    if (canonicalKey.isNotEmpty()) {
        youTubeChannelDao.deleteByCanonicalKey(canonicalKey)
    }
    if (!identity.verifiedHandle.isNullOrBlank()) {
        val norm = ChannelIdentity.normalizeHandle(identity.verifiedHandle)
        youTubeChannelDao.deleteByHandle(norm)
    }
    ```
- **Divergence**:
  - If a channel was previously saved under both `name:` and `handle:`, deleting by `ChannelIdentity("@veritasium", "Veritasium")` only deletes `handle:@veritasium` and `@veritasium`.
  - The row `name:veritasium` remains in the database.
  - Next time a video is played without handle, the old policy is resurrected from the zombie `name:veritasium` row.
- **Empirical Confirmation**:
  - Verified in `Milestone1Challenger1AdversarialTest.empirical - deleting channel by handle leaves zombie display-name record in DAO`.

### 2.3 Premise 3: Handle Deletion Fails When Name Contains Spaces (MEDIUM DEFECT)
- **Observation**:
  - In `YouTubeChannelRepositoryImpl.deleteChannel(channelHandle: String)` (lines 155-168):
    When passed `"Khan Academy"` (a display name), `normalizeHandle("Khan Academy")` produces `"@khan academy"`.
    It calls `deleteByHandle("@khan academy")` and `deleteByCanonicalKey("handle:@khan academy")`.
    The actual handle in the database is `@khanacademy` (without spaces).
    The handle row in the database is untouched and survives deletion.
- **Empirical Confirmation**:
  - Verified in `Milestone1Challenger1AdversarialTest.empirical - deleteChannel with channel name fails to delete handle-based record`.

### 2.4 Premise 4: Inconsistency in `ChannelIdentity.hasHandle` (MEDIUM DEFECT)
- **Observation**:
  - In `ChannelIdentity.kt` line 33: `val hasHandle: Boolean get() = !verifiedHandle.isNullOrBlank()`.
  - If `verifiedHandle = "@"`, `hasHandle` returns `true`.
  - However, `normalizeHandle("@")` returns `""`, causing `canonicalKey` to fall back to `displayName`.
  - Thus `hasHandle == true` even when `verifiedHandle` contains no valid handle identifier characters.
- **Empirical Confirmation**:
  - Verified in `Milestone1Challenger1AdversarialTest.empirical - hasHandle inconsistency with canonicalKey when handle is bare at-symbol`.

### 2.5 Premise 5: `normalizeHandle` Allows Spaces and Emojis/Unicode Malformations (MEDIUM DEFECT)
- **Observation**:
  - In `ChannelIdentity.normalizeHandle` (lines 54-60):
    It only trims leading and trailing whitespace and strips leading `@`. It does NOT reject or strip internal spaces or sanitize characters.
    `normalizeHandle("  @channel with spaces  ")` returns `"@channel with spaces"`.
    Real YouTube handles only support `[a-zA-Z0-9._-]`. If an accessibility parser extracts text with trailing metadata (e.g. `@creator \u2022 1M views`), `normalizeHandle` creates a malformed canonical key.
    Furthermore, fullwidth commercial at `\uFF20` (`＠`) is not stripped by `trimStart('@')`, yielding `"@＠channel"`.
- **Empirical Confirmation**:
  - Verified in `Milestone1Challenger1AdversarialTest.empirical - normalizeHandle allows spaces and creates invalid YouTube handle` and `empirical - normalizeHandle with fullwidth at-symbol fails to strip and produces double at-sign`.

### 2.6 Premise 6: `VideoFingerprint` Accepts String `"null"` / `"undefined"` (LOW/MEDIUM DEFECT)
- **Observation**:
  - In `VideoFingerprint.kt` line 25: `!mediaId.isNullOrBlank() -> "media:${mediaId.trim()}"`.
  - When media framework or JSON serialization outputs `"null"`, the fingerprint becomes `"media:null"`, causing all such videos to collide.
- **Empirical Confirmation**:
  - Verified in `Milestone1Challenger1AdversarialTest.empirical - literal string null mediaId is accepted as valid mediaId`.

### 2.7 Premise 7: `getChannel` Omits Canonical Key Lookup (MEDIUM DEFECT)
- **Observation**:
  - In `YouTubeChannelRepositoryImpl.getChannel(channelHandle: String)` lines 112-129:
    It queries `dao.getChannelByHandle` and `dao.getChannelByName`, but never calls `dao.getChannelByCanonicalKey`.
    Passing a canonical key (e.g. `"handle:@veritasium"` or `"name:khan academy"`) returns `null`.
- **Empirical Confirmation**:
  - Verified in `Milestone1Challenger1AdversarialTest.empirical - getChannel with canonicalKey fails because it only queries handle and name`.

---

## 3. Caveats

- **No Caveats**: All 7 challenge points were directly analyzed against the source code, empirically verified via an executable test suite in `:app:testQaDebugUnitTest`, and corroborated by test output logs.

---

## 4. Conclusion

The worker successfully eliminated synthetic fake handle fabrication and implemented the core `ChannelIdentity`, `VideoFingerprint`, and Room entity models. However, the repository layer and entity lifecycle suffer from a **critical state de-synchronization defect**:
1. When a channel originally classified by display name (`name:`) is subsequently saved with its verified handle (`handle:`), the repository does not reconcile the two rows, resulting in duplicate database rows and **stale policy lookups** when videos from that channel are watched without an exposed handle.
2. Deleting a channel does not cleanly delete both `handle:` and `name:` rows for that channel, leaving **zombie policies** active.
3. Edge case validation in `ChannelIdentity` (`hasHandle`, internal whitespace, fullwidth `@`) and `VideoFingerprint` (`"null"` media IDs) requires refinement.

Therefore, the verdict is **`REQUEST_CHANGES`**.

### Required Action Items for Worker:
1. **Reconcile Canonical Keys on Save**:
   - In `YouTubeChannelRepositoryImpl.savePolicy` and `saveChannelPolicy`:
     - If saving with a verified handle and display name (`handle:<handle>`), check if an existing row with `canonicalKey = "name:${normalizeDisplayName(displayName)}"` exists. If so, delete or migrate that row so only the unified handle row exists.
     - If saving with a display name only, check if an existing row with `displayName = displayName` (or matching handle) already exists. If an existing handle row exists, update its policy rather than creating a disconnected `name:` row.
2. **Reconcile Deletions**:
   - In `deleteChannel(identity: ChannelIdentity)`:
     - Always delete both `identity.canonicalKey` and, if `displayName` is present, `youTubeChannelDao.deleteByCanonicalKey("name:${ChannelIdentity.normalizeDisplayName(displayName)}")`.
   - In `deleteChannel(channelHandle: String)`:
     - Look up `getChannelByName(trimmed)` or `getChannelByHandle(trimmed)` first to find both the handle and display name, and delete all associated records.
3. **Refine Edge Cases**:
   - In `ChannelIdentity.kt`:
     - Fix `hasHandle`: `val hasHandle: Boolean get() = !verifiedHandle.isNullOrBlank() && normalizeHandle(verifiedHandle).isNotEmpty()`.
     - In `normalizeHandle`: reject or strip internal spaces and reject non-handle characters.
   - In `VideoFingerprint.kt`:
     - In `create`: reject literal `"null"` and `"undefined"` media IDs.
   - In `YouTubeChannelRepositoryImpl.getChannel`:
     - Add `youTubeChannelDao.getChannelByCanonicalKey(trimmed)` as the first lookup step.

---

## 5. Verification Method

To independently reproduce and verify all 7 empirical challenges:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.Milestone1Challenger1AdversarialTest"
```

To run the complete test suite:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testQaDebugUnitTest
```

### Invalidation Conditions
This challenge report is invalidated if:
1. Associating a handle with an existing display-name channel updates the display-name policy and leaves only one unified row in the database.
2. Deleting a channel via its handle identity also removes any associated `name:<displayName>` entity.
3. `ChannelIdentity.hasHandle` returns `false` when `verifiedHandle = "@"`.
4. `VideoFingerprint.create(mediaId = "null", ...)` falls back to content key instead of generating `"media:null"`.
