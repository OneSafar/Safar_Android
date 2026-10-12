# Architectural Survey Report: Project Structure, Room Database, DI, and Build Setup

## 1. Observation

### 1.1 Project Structure & Multi-Module Layout
- File: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/settings.gradle.kts`
  - Lines 25-26:
    ```kotlin
    rootProject.name = "safar"
    include(":app")
    ```
  - Direct observation: The project is a **single-module** Android application. Only `:app` is included. There are no submodules or dynamic feature modules.

### 1.2 Gradle Build, Tooling, SDK & Jetpack Compose Versions
- File: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/gradle/wrapper/gradle-wrapper.properties`
  - Line 3: `distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip` (Gradle version **8.13**)
- File: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/gradle/libs.versions.toml`
  - Lines 2-13:
    ```toml
    agp = "8.13.2"
    kotlin = "2.0.21"
    ksp = "2.0.21-1.0.27"
    hilt = "2.51.1"
    hiltNavigationCompose = "1.2.0"
    coreKtx = "1.13.1"
    coreSplashscreen = "1.0.1"
    lifecycleRuntimeKtx = "2.8.6"
    activityCompose = "1.9.2"
    composeBom = "2025.06.01"
    navigationCompose = "2.9.4"
    room = "2.6.1"
    ```
  - Line 27: `material3 = "1.3.1"`
  - Lines 85-89:
    ```toml
    android-application = { id = "com.android.application", version.ref = "agp" }
    kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
    kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
    hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
    ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
    ```
- File: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/build.gradle.kts`
  - Lines 70-71:
    ```kotlin
    namespace = "com.safarparmar.app"
    compileSdk = 36
    ```
  - Lines 96-98:
    ```kotlin
    defaultConfig {
        applicationId = "com.safarparmar.app"
        minSdk = 26
        targetSdk = 36
    ```
  - Lines 112-133: Product Flavors:
    - Flavor Dimension: `env`
    - Flavors: `qa` (`applicationIdSuffix = ".qa"`, `versionNameSuffix = "-qa"`) and `prod` (`applicationId = "com.safarparmar.app"`).
  - Lines 177-184:
    ```kotlin
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    ```
  - Lines 186-189:
    ```kotlin
    buildFeatures {
        compose = true
        buildConfig = true
    }
    ```
  - Lines 191-193:
    ```kotlin
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    ```
  - Lines 275-281: Test dependencies:
    ```kotlin
    testImplementation(libs.junit) // 4.13.2
    testImplementation(libs.kotlinx.coroutines.test) // 1.9.0
    testImplementation(libs.turbine) // 1.1.0
    testImplementation(libs.mockk) // 1.13.12
    androidTestImplementation(libs.androidx.junit) // 1.2.1
    androidTestImplementation(libs.androidx.espresso.core) // 3.6.1
    ```
  - File: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/gradle/gradle-daemon-jvm.properties`
    - Lines 12-13: `toolchainVendor=JETBRAINS`, `toolchainVersion=21`.
  - Machine Java Runtime:
    - The system `/usr/bin/java` is not configured, but Android Studio JBR (OpenJDK 21.0.10) is present at:
      `/Applications/Android Studio.app/Contents/jbr/Contents/Home`

### 1.3 Room Database Implementation Status
- File: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/build.gradle.kts`
  - Lines 239-241:
    ```kotlin
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    ```
- Tool searches:
  - `grep_search` for `@Database`: **0 results** across `app/src`.
  - `grep_search` for `@Dao`: **0 results** across `app/src`.
  - `grep_search` for `@Entity`: **0 results** across `app/src`.
  - `grep_search` for `RoomDatabase`: **0 results** in source code.
  - `find_by_name` for `*schema*`: **0 results**.
  - `ksp` argument for `room.schemaLocation`: **Not configured** in `app/build.gradle.kts`.
- Existing local storage implementations:
  - `com.safarparmar.app.data.local.SafarDataStore`: Uses Jetpack `DataStore<Preferences>` and `EncryptedSharedPreferences` for user session tokens and settings.
  - `com.safarparmar.app.ui.ekagra.EkagraPendingSessionSaveStore`: Uses standard `SharedPreferences` with JSON serialization.
- Direct observation: **Room 2.6.1 dependencies and KSP compiler are already declared and active in Gradle, but no Room database class, DAO, or Entity exists in the codebase yet.**

### 1.4 Dependency Injection & Application Setup
- Framework: Dagger Hilt `2.51.1` via KSP.
- Application class:
  - File: `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/src/main/java/com/safarparmar/app/SafarApplication.kt`
  - Line 29: `@HiltAndroidApp class SafarApplication : Application(), coil.ImageLoaderFactory`
  - Lines 48-53: Injected fields:
    ```kotlin
    @Inject lateinit var readSnapshots: com.safarparmar.app.data.repository.ReadSnapshotStore
    @Inject lateinit var dataStore: SafarDataStore
    @Inject lateinit var notificationTokenRegistrar: NotificationTokenRegistrar
    @Inject lateinit var referralManager: com.safarparmar.app.data.repository.ReferralManager
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
    ```
- Existing Hilt Modules in `/Users/shashankkumar/Documents/SAFAR_PARENT/Safar_Android/app/src/main/java/com/safarparmar/app/di/`:
  - `DispatcherModule.kt`: Provides `@IoDispatcher CoroutineDispatcher = Dispatchers.IO` in `SingletonComponent`.
  - `NetworkModule.kt`: Provides `OkHttpClient`, `Retrofit`, `CookieManager`, `Cache`, `Gson`, and Retrofit API services in `SingletonComponent`.
  - `RepositoryModule.kt`: Abstract `@Module` binding domain repository interfaces to implementations via `@Binds @Singleton`.

### 1.5 Gradle Build and Test Execution
- Command executed:
  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testQaDebugUnitTest --tests "com.safarparmar.app.notifications.QuietHoursTest"`
- Output:
  `BUILD SUCCESSFUL in 1s`
- Command executed:
  `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileQaDebugKotlin`
- Output:
  `BUILD SUCCESSFUL in 668ms`

---

## 2. Logic Chain

1. **Module & Architecture Boundary**:
   - Observation 1.1 proves `:app` is the only module. All new packages for the YouTube blocker (Room database, policy engine, services, overlay) belong inside `:app/src/main/java/com/safarparmar/app/`.
   - Recommended package structure:
     - Room DB & DAOs: `com.safarparmar.app.data.local.db` or `com.safarparmar.app.data.local`
     - Entities: `com.safarparmar.app.data.local.entity`
     - YouTube Feature (policy engine, services, overlay): `com.safarparmar.app.feature.youtube`

2. **Room Database Integration Plan**:
   - Observation 1.3 shows Room dependencies (`room-runtime`, `room-ktx`, `room-compiler` v2.6.1) are already configured in `app/build.gradle.kts`.
   - Because no database exists yet, we can create the initial database cleanly without legacy migration baggage.
   - Proposed Entity:
     ```kotlin
     package com.safarparmar.app.data.local.entity

     import androidx.room.Entity
     import androidx.room.PrimaryKey

     enum class ChannelPolicy {
         PRODUCTIVE,
         DISTRACTING
     }

     @Entity(tableName = "youtube_channels")
     data class YouTubeChannelEntity(
         @PrimaryKey
         val channelId: String, // Normalized handle (e.g. "@veritasium") or channel ID
         val channelName: String,
         val policy: ChannelPolicy,
         val createdAt: Long = System.currentTimeMillis(),
         val updatedAt: Long = System.currentTimeMillis()
     )
     ```
   - Proposed DAO:
     ```kotlin
     package com.safarparmar.app.data.local.dao

     import androidx.room.*
     import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
     import kotlinx.coroutines.flow.Flow

     @Dao
     interface YouTubeChannelDao {
         @Query("SELECT * FROM youtube_channels WHERE channelId = :channelId LIMIT 1")
         suspend fun getChannel(channelId: String): YouTubeChannelEntity?

         @Query("SELECT * FROM youtube_channels WHERE channelName = :channelName COLLATE NOCASE LIMIT 1")
         suspend fun getChannelByName(channelName: String): YouTubeChannelEntity?

         @Query("SELECT * FROM youtube_channels")
         fun getAllChannels(): Flow<List<YouTubeChannelEntity>>

         @Insert(onConflict = OnConflictStrategy.REPLACE)
         suspend fun upsertChannel(channel: YouTubeChannelEntity)

         @Query("DELETE FROM youtube_channels WHERE channelId = :channelId")
         suspend fun deleteChannel(channelId: String)
     }
     ```
   - Proposed Database Class:
     ```kotlin
     package com.safarparmar.app.data.local

     import androidx.room.Database
     import androidx.room.RoomDatabase
     import androidx.room.TypeConverters
     import com.safarparmar.app.data.local.dao.YouTubeChannelDao
     import com.safarparmar.app.data.local.entity.YouTubeChannelEntity

     @Database(
         entities = [YouTubeChannelEntity::class],
         version = 1,
         exportSchema = false
     )
     abstract class SafarDatabase : RoomDatabase() {
         abstract fun youTubeChannelDao(): YouTubeChannelDao
     }
     ```
   - Proposed DI Module:
     Create `DatabaseModule.kt` in `com.safarparmar.app.di`:
     ```kotlin
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
         @Singleton
         fun provideYouTubeChannelDao(database: SafarDatabase): YouTubeChannelDao {
             return database.youTubeChannelDao()
         }
     }
     ```

3. **Dependency Injection Alignment**:
   - Observation 1.4 confirms Hilt is standard in the project.
   - Any service (`YouTubeGuardService`) or policy engine (`YouTubePolicyEngine`) can either inject `YouTubeChannelDao` directly or access it via an injected repository (`YouTubeRepository`).
   - For Android services like `AccessibilityService` that cannot use constructor injection easily, `@AndroidEntryPoint` allows `@Inject lateinit var ...` field injection.

4. **Testing & Tooling Execution**:
   - Observation 1.5 confirms local unit tests run in ~1s using Gradle and OpenJDK 21 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`.
   - Unit tests for the deterministic state machine (`YouTubePolicyEngineTest`) do not need Android dependencies and can execute via standard JUnit 4 + Coroutines Test + Turbine.
   - Room DAO tests can use `Room.inMemoryDatabaseBuilder` with the AndroidX test runner or robolectric if added.

---

## 3. Caveats

1. **Java Runtime Requirement**:
   - On the host machine, running `./gradlew` without specifying `JAVA_HOME` will fail with `Unable to locate a Java Runtime`. All scripts/commands must set:
     `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
2. **Room Schema Export**:
   - Setting `exportSchema = true` on Room `@Database` requires configuring `room.schemaLocation` in KSP arguments in `app/build.gradle.kts`. For version 1, setting `exportSchema = false` avoids compile warnings/errors until migration export is necessary.
3. **Robolectric Absence**:
   - Neither Robolectric nor AndroidX Test Room is in `testImplementation` (only `androidTestImplementation` has `androidx.junit`). Pure unit tests should isolate `YouTubePolicyEngine` logic from Android SDK classes, while Room tests should either use `Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ...)` in instrumentation or test via DAO mock/fake implementations in unit tests.
4. **Play Protect Manifest Considerations**:
   - In `app/src/qa/AndroidManifestStripped.xml`, permissions like `PACKAGE_USAGE_STATS`, `SYSTEM_ALERT_WINDOW`, and `BIND_NOTIFICATION_LISTENER_SERVICE` are conditionally stripped under `-PstripKavach=true`. The YouTube blocker will use `AccessibilityService` (`YouTubeGuardService`) and `TYPE_ACCESSIBILITY_OVERLAY`, which do NOT require `SYSTEM_ALERT_WINDOW` or `PACKAGE_USAGE_STATS`, avoiding the stalkerware permission trio.

---

## 4. Conclusion

- The codebase is a clean single-module Gradle project with AGP `8.13.2`, Kotlin `2.0.21`, Jetpack Compose BOM `2025.06.01`, Hilt `2.51.1`, and Room `2.6.1` dependencies ready in build configuration.
- Room has **no prior schema or classes**, giving us total freedom to define `SafarDatabase`, `YouTubeChannelEntity` (with `ChannelPolicy.PRODUCTIVE` and `ChannelPolicy.DISTRACTING`), and `YouTubeChannelDao`.
- Hilt is set up cleanly with standard `@InstallIn(SingletonComponent::class)` modules, making `DatabaseModule` a seamless addition.
- Build and unit tests execute cleanly and rapidly (< 1s) when using Android Studio's bundled JDK 21.

---

## 5. Verification Method

To verify these findings independently:

1. **Verify Gradle & JDK Environment**:
   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew -version
   ```
   *Expected*: Gradle 8.13 running on JVM 21.0.10.

2. **Verify Compilation**:
   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileQaDebugKotlin
   ```
   *Expected*: `BUILD SUCCESSFUL`.

3. **Verify Unit Test Execution**:
   ```bash
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testQaDebugUnitTest --tests "com.safarparmar.app.notifications.QuietHoursTest"
   ```
   *Expected*: `BUILD SUCCESSFUL` with all tests passing.

4. **Verify Room Dependencies & Absence of Existing Database**:
   ```bash
   rg "@Database" app/src
   rg "libs.room" app/build.gradle.kts
   ```
   *Expected*: 0 matches for `@Database`, 3 matches for `libs.room` dependencies.
