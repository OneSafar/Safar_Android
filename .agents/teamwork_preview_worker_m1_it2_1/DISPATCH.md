## 2026-09-11T13:25:27Z
You are Milestone 1 Iteration 2 Worker.
Your working directory is: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it2_1
The project root is: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android

MANDATORY: Read the authoritative user request at: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/ORIGINAL_REQUEST.md before doing anything else.
Also read the project architecture at: /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/orchestrator_1/PROJECT.md

Read the detailed Milestone 1 Iteration 2 Explorer handoff reports:
- /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_m1_it2_1/handoff.md (Policy engine fix specs & patch)
- /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_m1_it2_2/handoff.md (Room persistence casing fix specs & patch)
- /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_explorer_m1_it2_3/handoff.md (Holistic test verification specs)

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Scope: Milestone 1 Iteration 2 Remediation
You have exclusive write ownership of the following 5 files:
1. `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`
2. `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt`
3. `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`
4. `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`
5. `app/src/test/java/com/safarparmar/app/data/local/YouTubePersistenceAdversarialTest.kt`

Tasks:
1. In `app/src/main/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngine.kt`:
   Replace lines 151–166 with:
   ```kotlin
           if (matchesCurrent && current != null && !current.isShorts) {
               activeJob?.cancel()

               lastVerifiedPolicy = policy
               lastVerifiedChannel = channelHandle

               when (policy) {
                   ChannelPolicy.PRODUCTIVE -> {
                       _state.value = PolicyState.Allowed(current, channelHandle)
                   }
                   ChannelPolicy.DISTRACTING -> {
                       _state.value = PolicyState.Blocked(current, channelHandle, BlockReason.DISTRACTING_CHANNEL)
                   }
               }
           }
   ```
2. In `app/src/test/java/com/safarparmar/app/feature/youtube/policy/YouTubePolicyEngineTest.kt`:
   Update lines 892–896 to handle `StateFlow` conflation:
   ```kotlin
               expectNoEvents()
               val postClassifyState = engine.state.value
               assertFalse("Shorts must NEVER transition to Allowed! Got: $postClassifyState", postClassifyState is PolicyState.Allowed)
               assertTrue("Shorts must remain ShortsBlocked! Got: $postClassifyState", postClassifyState is PolicyState.ShortsBlocked)
   ```
3. In `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`:
   Add `collate = ColumnInfo.NOCASE` on `channelHandle`:
   ```kotlin
       @PrimaryKey
       @ColumnInfo(name = "channelHandle", collate = ColumnInfo.NOCASE)
       val channelHandle: String,
   ```
4. In `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`:
   Update `normalizeHandle`:
   ```kotlin
       private fun normalizeHandle(handle: String): String {
           val trimmed = handle.trim()
           if (trimmed.isEmpty()) return ""
           val withAt = if (trimmed.startsWith("@")) trimmed else "@$trimmed"
           return withAt.lowercase()
       }
   ```
5. In `app/src/test/java/com/safarparmar/app/data/local/YouTubePersistenceAdversarialTest.kt`:
   Update Test #9 to assert the verified fix:
   ```kotlin
       @Test
       fun `verification - repository normalizes handle casing to ensure single primary key and update replacement`() = runTest(testDispatcher) {
           val slots = mutableListOf<YouTubeChannelEntity>()
           coEvery { dao.insertOrUpdate(capture(slots)) } returns Unit

           repository.saveChannelPolicy("@Veritasium", "Veritasium", ChannelPolicy.PRODUCTIVE)
           repository.saveChannelPolicy("@veritasium", "Veritasium", ChannelPolicy.DISTRACTING)

           assertEquals(2, slots.size)
           val firstSavedHandle = slots[0].channelHandle
           val secondSavedHandle = slots[1].channelHandle

           assertEquals("@veritasium", firstSavedHandle)
           assertEquals("@veritasium", secondSavedHandle)
           assertEquals(firstSavedHandle, secondSavedHandle)
       }
   ```
6. Run verification across all 4 pillars:
   ```bash
   export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
   ./gradlew cleanTestQaDebugUnitTest testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.policy.YouTubePolicyEngineTest"
   ./gradlew cleanTestQaDebugUnitTest testQaDebugUnitTest --tests "com.safarparmar.app.data.local.YouTubePersistenceAdversarialTest"
   ./gradlew cleanTestQaDebugUnitTest testQaDebugUnitTest --tests "com.safarparmar.app.feature.youtube.e2e.YouTubeGuardE2ETest"
   ./gradlew cleanTestQaDebugUnitTest testQaDebugUnitTest
   ```
   Verify that 100% of all tests pass (246/246 tests in the project) with 0 regressions.
7. Write your handoff report to:
   /Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/.agents/teamwork_preview_worker_m1_it2_1/handoff.md
   Update your progress.md and notify your orchestrator via send_message when done.
