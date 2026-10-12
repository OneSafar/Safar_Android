# Task Assignment: Milestone 1 Worker (Iteration 2)

## Working Directory
`/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it2_orch2`

## Objective
Remediate the 4 defects identified by Challenger 1 and Challenger 2 in Milestone 1 Gate:

1. **Dual-Entity Synchronization in `YouTubeChannelRepositoryImpl` & `YouTubeChannelDao`**:
   - In `savePolicy(identity: ChannelIdentity, policy: ChannelPolicy)`:
     - When saving an identity with a verified handle (`handle:@handle`) and display name (`displayName`), check if there is an existing entity with `canonicalKey == "name:${normalizeDisplayName(displayName)}"`. If so, remove the obsolete name-only row or ensure lookups for that display name return the updated policy.
   - In `getPolicy(identity: ChannelIdentity)`:
     - If `identity` has only `displayName` (handle is null), query by `canonicalKey` ("name:..."). If null, also fallback to checking if any entity has `displayName = :name` (which might have been saved with a handle). Return its policy.
     - If `identity` has `verifiedHandle`, query by `canonicalKey` ("handle:..."). If null, fallback to display name query if present.
   - In `deleteChannel(identity: ChannelIdentity)`:
     - Delete by `canonicalKey`. Also delete any entity matching `channelHandle` (if handle present) AND any entity matching `displayName` (if displayName present), preventing zombie orphan rows.
   - In `deleteChannel(channelHandleOrName: String)`:
     - Delete by canonicalKey, by handle, and by displayName.

2. **Handle Normalization & Validation (`ChannelIdentity.kt`)**:
   - In `normalizeHandle`:
     - If the input contains whitespace (`it.isWhitespace()`), it is NOT a valid YouTube handle; return `""`.
     - Strip leading standard `@` and fullwidth `＠`, trim. If stripped is empty or contains whitespace or invalid characters, return `""`.
     - Otherwise return `"@${stripped.lowercase()}"`.
   - In `hasHandle`:
     - Return `!verifiedHandle.isNullOrBlank() && normalizeHandle(verifiedHandle).isNotEmpty()`.

3. **`VideoFingerprint` MediaId Validation (`VideoFingerprint.kt`)**:
   - In `VideoFingerprint.create(mediaId, title, channelIdentity)`:
     - If `mediaId.trim().lowercase() == "null"`, treat as null/blank.

4. **Secondary Constructor in `YouTubeChannelEntity.kt`**:
   - Normalize `channelHandle` via `ChannelIdentity.normalizeHandle(it).takeIf { it.isNotEmpty() }`, ensuring raw `"@"` or invalid handles store `null`.

5. **Test Verification**:
   - Run tests:
     `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`
   - Ensure both `Milestone1AdversarialChallengeTest` and `Milestone1Challenger1AdversarialTest` pass 100%.

## Mandatory Reading
- `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md`
- Challenger 1 Report: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_challenger_m1_1_orch2/handoff.md`
- Challenger 2 Report: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_challenger_m1_2_orch2/handoff.md`

## Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work.

## Output Requirements
Write report to `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it2_orch2/handoff.md`.

## 2026-09-11T17:50:22Z
You are Milestone 1 Worker (Iteration 2). Your working directory is /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it2_orch2.
Read your instructions in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it2_orch2/DISPATCH.md and the mandatory specification in /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md.
Also read Challenger 1 report at /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_challenger_m1_1_orch2/handoff.md and Challenger 2 report at /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_challenger_m1_2_orch2/handoff.md.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Remediate the 4 defects:
1. Reconcile dual-entity desynchronization in YouTubeChannelRepositoryImpl & YouTubeChannelDao (clean up obsolete name-only row on handle save, fallback lookup by displayName if entity has handle, comprehensive deletion of both rows).
2. Handle normalization: reject whitespace in handles, strip fullwidth ＠, fix hasHandle.
3. VideoFingerprint: treat literal string "null" as empty/null.
4. YouTubeChannelEntity: normalize handle in secondary constructor.
5. Verify build and tests with `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:testQaDebugUnitTest`.

Write handoff report to /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it2_orch2/handoff.md and send a message when done.
