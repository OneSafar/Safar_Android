# Handoff Report: Milestone 1 — Persistence Layer (Room Database & Repository)

## 1. Observation

### 1.1 Gradle & Environment Configuration
- In `gradle/libs.versions.toml`:
  - Line 2: `agp = "8.13.2"`
  - Line 3: `kotlin = "2.0.21"`
  - Line 4: `ksp = "2.0.21-1.0.27"`
  - Line 5: `hilt = "2.51.1"`
  - Line 13: `room = "2.6.1"`
  - Lines 50-52:
    ```toml
    room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
    room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
    room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
    ```
- In `app/build.gradle.kts`:
  - Lines 1-9: KSP and Kotlin plugins are applied (`alias(libs.plugins.ksp)`, `alias(libs.plugins.kotlin.android)`).
  - Lines 177-185: `JavaVersion.VERSION_17` and `jvmTarget = "17"`.
  - Lines 239-241:
    ```kotlin
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    ```
- Gradle verification command:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew testQaDebugUnitTest
  ```
  Result:
  ```
  BUILD SUCCESSFUL in 1s
  36 actionable tasks: 1 executed, 35 up-to-date
  ```
  KSP tasks (`:app:kspQaDebugKotlin`, `:app:kspQaDebugUnitTestKotlin`) executed cleanly.

### 1.2 Current Persistence Codebase
- Directory `app/src/main/java/com/safarparmar/app/data/local` currently contains:
  - `PersistentCookieStore.kt`
  - `SafarDataStore.kt`
  - `TimerAlertStyle.kt`
- Grep search for `@Entity` and `@Database` returned 0 results: no existing Room databases or entities currently exist in `com.safarparmar.app`.
- In `app/src/main/java/com/safarparmar/app/di/DispatcherModule.kt`:
  `@IoDispatcher` qualifier is declared and provides `Dispatchers.IO`.
- In `app/src/main/java/com/safarparmar/app/di/RepositoryModule.kt`:
  Repositories are bound using `@Binds @Singleton abstract fun bind...`.

### 1.3 Contract Requirements from `ORIGINAL_REQUEST.md` & `PROJECT.md`
- `ORIGINAL_REQUEST.md` (§R3):
  "Store channel classifications (PRODUCTIVE, DISTRACTING) using a Room database entity (`YouTubeChannelEntity`)."
- `PROJECT.md` (lines 63-75):
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

## 2. Logic Chain

1. **Room & KSP Compatibility**:
   - `ksp = "2.0.21-1.0.27"` and `room = "2.6.1"` are already configured in `app/build.gradle.kts`.
   - When using Room KSP without schema directory configuration, Room issues a compiler warning/error if `exportSchema` is left as default (`true`). Setting `exportSchema = false` in `@Database(version = 1, exportSchema = false)` guarantees clean compilation without requiring a dummy schema directory or build script modifications.
2. **Entity & Enum Structure**:
   - `ChannelPolicy` enum with values `PRODUCTIVE` and `DISTRACTING` directly fulfills R3 and Acceptance Criteria §System Integration.
   - `YouTubeChannelEntity` requires `channelHandle: String` as the `@PrimaryKey` because YouTube channel handles (e.g. `@Veritasium`) uniquely identify channels in YouTube URLs and DOM nodes.
   - Additional fields `channelName: String = ""` and `updatedAt: Long = System.currentTimeMillis()` support display formatting and MRU/LRU sorting in the UI.
3. **TypeConverter Strategy**:
   - `ChannelPolicyConverter` converts `ChannelPolicy?` to/from `String?` using `enum.name` and safe `enumValueOf` parsing.
   - Registering `@TypeConverters(ChannelPolicyConverter::class)` on `SafarDatabase` applies it across all DAOs and queries.
4. **DAO & Query Robustness**:
   - YouTube accessibility events may yield handles with varying casing (e.g. `@MKBHD` vs `@mkbhd`) or without the `@` prefix.
   - Using SQLite `COLLATE NOCASE` in DAO queries (`WHERE channelHandle = :channelHandle COLLATE NOCASE`) makes channel lookup case-insensitive.
   - Providing `getChannelByName` as a fallback query handles instances where the accessibility service can only capture the display title/channel name instead of the handle.
   - Reactive `getAllChannels(): Flow<List<YouTubeChannelEntity>>` emits real-time updates to UI consumers.
5. **Dependency Injection**:
   - Creating `DatabaseModule` in `com.safarparmar.app.di` as an `object` providing `SafarDatabase`, `YouTubeChannelDao`, and `YouTubeChannelRepository`.
   - Using `.fallbackToDestructiveMigration()` in `Room.databaseBuilder()` prevents runtime crashes during development iterations.
   - Injecting `@IoDispatcher` ensures all DB operations run off the main thread.

---

## 3. Caveats

1. **Schema Export**: `exportSchema = false` is used to keep the build self-contained without needing schema files committed. When preparing for production releases with future migrations (e.g. version 2+), `exportSchema = true` with `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` can be added to track migrations.
2. **Alternative DI Binding**: `DatabaseModule` provides `YouTubeChannelRepository` directly. If the implementer prefers standard `@Binds` in `RepositoryModule.kt`, `provideYouTubeChannelRepository` can be omitted from `DatabaseModule`, and `@Binds @Singleton abstract fun bindYouTubeChannelRepository(impl: YouTubeChannelRepositoryImpl): YouTubeChannelRepository` can be placed in `RepositoryModule.kt`. Both are valid and documented below.
3. **Environment**: Execution of Gradle requires `JAVA_HOME` pointing to a valid JDK (such as `/Applications/Android Studio.app/Contents/jbr/Contents/Home` or Homebrew `openjdk@17`).

---

## 4. Conclusion & Implementation Specifications

All 8 components are specified below with their exact package names, imports, class definitions, and target file paths.

### Component 1: `ChannelPolicy.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/data/local/entity/ChannelPolicy.kt`
- **Package**: `com.safarparmar.app.data.local.entity`

```kotlin
package com.safarparmar.app.data.local.entity

/**
 * Classification policy for a YouTube channel.
 * PRODUCTIVE: Content is permitted during focus sessions.
 * DISTRACTING: Content is blocked by YouTubeGuardService overlay.
 */
enum class ChannelPolicy {
    PRODUCTIVE,
    DISTRACTING,
}
```

---

### Component 2: `YouTubeChannelEntity.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`
- **Package**: `com.safarparmar.app.data.local.entity`

```kotlin
package com.safarparmar.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted YouTube channel classification.
 *
 * @property channelHandle Unique YouTube handle (e.g. "@Veritasium"), primary key.
 * @property channelName Display name of the channel.
 * @property policy Classification policy (PRODUCTIVE or DISTRACTING).
 * @property updatedAt Epoch timestamp of last update.
 */
@Entity(tableName = "youtube_channels")
data class YouTubeChannelEntity(
    @PrimaryKey
    @ColumnInfo(name = "channelHandle")
    val channelHandle: String,
    @ColumnInfo(name = "channelName")
    val channelName: String = "",
    @ColumnInfo(name = "policy")
    val policy: ChannelPolicy,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
)
```

---

### Component 3: `ChannelPolicyConverter.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/data/local/converter/ChannelPolicyConverter.kt`
- **Package**: `com.safarparmar.app.data.local.converter`

```kotlin
package com.safarparmar.app.data.local.converter

import androidx.room.TypeConverter
import com.safarparmar.app.data.local.entity.ChannelPolicy

/**
 * Room TypeConverter for [ChannelPolicy] enum.
 */
class ChannelPolicyConverter {

    @TypeConverter
    fun fromChannelPolicy(policy: ChannelPolicy?): String? {
        return policy?.name
    }

    @TypeConverter
    fun toChannelPolicy(value: String?): ChannelPolicy? {
        if (value.isNullOrBlank()) return null
        return try {
            enumValueOf<ChannelPolicy>(value.trim())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
```

---

### Component 4: `YouTubeChannelDao.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt`
- **Package**: `com.safarparmar.app.data.local.dao`

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

/**
 * Room Data Access Object for YouTube channels and classifications.
 */
@Dao
interface YouTubeChannelDao {

    @Query("SELECT * FROM youtube_channels WHERE channelHandle = :channelHandle COLLATE NOCASE LIMIT 1")
    suspend fun getChannelByHandle(channelHandle: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE channelHandle = :channelHandle COLLATE NOCASE LIMIT 1")
    suspend fun getPolicyByHandle(channelHandle: String): ChannelPolicy?

    @Query("SELECT * FROM youtube_channels WHERE channelName = :channelName COLLATE NOCASE LIMIT 1")
    suspend fun getChannelByName(channelName: String): YouTubeChannelEntity?

    @Query("SELECT * FROM youtube_channels ORDER BY updatedAt DESC")
    fun getAllChannels(): Flow<List<YouTubeChannelEntity>>

    @Query("SELECT * FROM youtube_channels WHERE policy = :policy ORDER BY updatedAt DESC")
    fun getChannelsByPolicy(policy: ChannelPolicy): Flow<List<YouTubeChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(channel: YouTubeChannelEntity)

    @Query("DELETE FROM youtube_channels WHERE channelHandle = :channelHandle COLLATE NOCASE")
    suspend fun deleteByHandle(channelHandle: String)

    @Delete
    suspend fun deleteChannel(channel: YouTubeChannelEntity)

    @Query("DELETE FROM youtube_channels")
    suspend fun deleteAll()
}
```

---

### Component 5: `SafarDatabase.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/data/local/SafarDatabase.kt`
- **Package**: `com.safarparmar.app.data.local`

```kotlin
package com.safarparmar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.safarparmar.app.data.local.converter.ChannelPolicyConverter
import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity

/**
 * Safar Room Database, Version 1.
 */
@Database(
    entities = [
        YouTubeChannelEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(ChannelPolicyConverter::class)
abstract class SafarDatabase : RoomDatabase() {

    abstract fun youTubeChannelDao(): YouTubeChannelDao

    companion object {
        const val DATABASE_NAME = "safar_database"
    }
}
```

---

### Component 6: `DatabaseModule.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/di/DatabaseModule.kt`
- **Package**: `com.safarparmar.app.di`

```kotlin
package com.safarparmar.app.di

import android.content.Context
import androidx.room.Room
import com.safarparmar.app.data.local.SafarDatabase
import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.repository.YouTubeChannelRepositoryImpl
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
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
    fun provideSafarDatabase(
        @ApplicationContext context: Context,
    ): SafarDatabase {
        return Room.databaseBuilder(
            context,
            SafarDatabase::class.java,
            SafarDatabase.DATABASE_NAME,
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideYouTubeChannelDao(database: SafarDatabase): YouTubeChannelDao {
        return database.youTubeChannelDao()
    }

    @Provides
    @Singleton
    fun provideYouTubeChannelRepository(
        dao: YouTubeChannelDao,
    ): YouTubeChannelRepository {
        return YouTubeChannelRepositoryImpl(dao)
    }
}
```

*(Note: If using `RepositoryModule.kt` for `@Binds`, omit `provideYouTubeChannelRepository` from `DatabaseModule.kt` and add `@Binds @Singleton abstract fun bindYouTubeChannelRepository(impl: YouTubeChannelRepositoryImpl): YouTubeChannelRepository` to `RepositoryModule.kt`)*.

---

### Component 7: `YouTubeChannelRepository.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/domain/repository/YouTubeChannelRepository.kt`
- **Package**: `com.safarparmar.app.domain.repository`

```kotlin
package com.safarparmar.app.domain.repository

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for YouTube channel classifications.
 */
interface YouTubeChannelRepository {

    /**
     * Retrieves the policy for the given channel handle, or null if unclassified.
     */
    suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy?

    /**
     * Persists or updates the policy classification for a channel handle.
     */
    suspend fun saveChannelPolicy(
        channelHandle: String,
        channelName: String = "",
        policy: ChannelPolicy,
    )

    /**
     * Observes all classified channels as a reactive Flow.
     */
    fun getAllChannels(): Flow<List<YouTubeChannelEntity>>

    /**
     * Retrieves the full entity record for a channel handle, or null if unclassified.
     */
    suspend fun getChannel(channelHandle: String): YouTubeChannelEntity?

    /**
     * Deletes a channel record by handle.
     */
    suspend fun deleteChannel(channelHandle: String)
}
```

---

### Component 8: `YouTubeChannelRepositoryImpl.kt`
- **Target File**: `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`
- **Package**: `com.safarparmar.app.data.repository`

```kotlin
package com.safarparmar.app.data.repository

import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeChannelRepositoryImpl @Inject constructor(
    private val youTubeChannelDao: YouTubeChannelDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : YouTubeChannelRepository {

    override suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy? =
        withContext(ioDispatcher) {
            val normalized = normalizeHandle(channelHandle)
            if (normalized.isEmpty()) return@withContext null
            youTubeChannelDao.getPolicyByHandle(normalized)
                ?: youTubeChannelDao.getPolicyByHandle(channelHandle.trim())
                ?: youTubeChannelDao.getChannelByName(channelHandle.trim())?.policy
        }

    override suspend fun saveChannelPolicy(
        channelHandle: String,
        channelName: String,
        policy: ChannelPolicy,
    ) = withContext(ioDispatcher) {
        val normalized = normalizeHandle(channelHandle)
        if (normalized.isEmpty() && channelName.isBlank()) return@withContext
        val handleToSave = if (normalized.isNotEmpty()) {
            normalized
        } else {
            "@${channelName.trim().replace("\\s+".toRegex(), "")}"
        }
        val entity = YouTubeChannelEntity(
            channelHandle = handleToSave,
            channelName = channelName.trim(),
            policy = policy,
            updatedAt = System.currentTimeMillis(),
        )
        youTubeChannelDao.insertOrUpdate(entity)
    }

    override fun getAllChannels(): Flow<List<YouTubeChannelEntity>> {
        return youTubeChannelDao.getAllChannels().flowOn(ioDispatcher)
    }

    override suspend fun getChannel(channelHandle: String): YouTubeChannelEntity? =
        withContext(ioDispatcher) {
            val normalized = normalizeHandle(channelHandle)
            if (normalized.isEmpty()) return@withContext null
            youTubeChannelDao.getChannelByHandle(normalized)
                ?: youTubeChannelDao.getChannelByHandle(channelHandle.trim())
                ?: youTubeChannelDao.getChannelByName(channelHandle.trim())
        }

    override suspend fun deleteChannel(channelHandle: String) =
        withContext(ioDispatcher) {
            val normalized = normalizeHandle(channelHandle)
            if (normalized.isNotEmpty()) {
                youTubeChannelDao.deleteByHandle(normalized)
            }
            val trimmed = channelHandle.trim()
            if (trimmed.isNotEmpty() && trimmed != normalized) {
                youTubeChannelDao.deleteByHandle(trimmed)
            }
        }

    private fun normalizeHandle(handle: String): String {
        val trimmed = handle.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("@")) trimmed else "@$trimmed"
    }
}
```

---

## 5. Verification Method

### 5.1 Static Verification
Inspect that all 8 target files match the specified paths and package names:
- `app/src/main/java/com/safarparmar/app/data/local/entity/ChannelPolicy.kt`
- `app/src/main/java/com/safarparmar/app/data/local/entity/YouTubeChannelEntity.kt`
- `app/src/main/java/com/safarparmar/app/data/local/converter/ChannelPolicyConverter.kt`
- `app/src/main/java/com/safarparmar/app/data/local/dao/YouTubeChannelDao.kt`
- `app/src/main/java/com/safarparmar/app/data/local/SafarDatabase.kt`
- `app/src/main/java/com/safarparmar/app/di/DatabaseModule.kt`
- `app/src/main/java/com/safarparmar/app/domain/repository/YouTubeChannelRepository.kt`
- `app/src/main/java/com/safarparmar/app/data/repository/YouTubeChannelRepositoryImpl.kt`

### 5.2 Build & Compilation Verification
Run Gradle build and unit tests with a configured JDK:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew compileQaDebugKotlin compileQaDebugJavaWithJavac kspQaDebugKotlin testQaDebugUnitTest
```
Expected output:
`BUILD SUCCESSFUL` with `kspQaDebugKotlin` generating Room DAO implementations (`YouTubeChannelDao_Impl`) and `SafarDatabase_Impl` without warnings or errors.

### 5.3 Invalidation Conditions
- Changing `exportSchema = true` without setting `room.schemaLocation` in KSP arguments will cause compiler warnings/errors.
- Modifying `ChannelPolicy` values without updating `ChannelPolicyConverter` will cause conversion failures.
