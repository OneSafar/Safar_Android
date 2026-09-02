package com.safarparmar.app.feature.youtubestudyv2

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query as RetrofitQuery

@Entity(tableName = "youtube_v2_identity")
data class YoutubeV2IdentityEntity(
    @androidx.room.PrimaryKey val channelId: String,
    val handle: String,
    val displayName: String,
    val thumbnailUrl: String?,
    val resolvedAtMs: Long,
)

@Entity(
    tableName = "youtube_v2_alias",
    primaryKeys = ["aliasType", "normalizedAlias", "channelId"],
)
data class YoutubeV2AliasEntity(
    val aliasType: String,
    val normalizedAlias: String,
    val channelId: String,
    val displayValue: String,
)

@Entity(tableName = "youtube_v2_allowlist")
data class YoutubeV2AllowlistEntity(
    @androidx.room.PrimaryKey val channelId: String,
    val addedAtMs: Long,
)

enum class YoutubeChannelClassification(val wire: String) {
    OTHERS("others"), PRODUCTIVE("productive"), DISTRACTING("distracting");

    companion object {
        fun fromWire(value: String?): YoutubeChannelClassification =
            entries.firstOrNull { it.wire == value } ?: OTHERS
    }
}

@Entity(tableName = "youtube_v2_classification")
data class YoutubeV2ClassificationEntity(
    @androidx.room.PrimaryKey val channelId: String,
    val classification: String,
    val updatedAtMs: Long,
)

@Dao
interface YoutubeStudyV2Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIdentity(identity: YoutubeV2IdentityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAliases(aliases: List<YoutubeV2AliasEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun allow(entity: YoutubeV2AllowlistEntity)

    @Query("DELETE FROM youtube_v2_allowlist WHERE channelId = :channelId")
    suspend fun removeAllowed(channelId: String)

    @Query("DELETE FROM youtube_v2_identity WHERE channelId = :channelId")
    suspend fun deleteIdentity(channelId: String)

    @Query("DELETE FROM youtube_v2_alias WHERE channelId = :channelId")
    suspend fun deleteAliases(channelId: String)

    @Query("DELETE FROM youtube_v2_classification WHERE channelId = :channelId")
    suspend fun deleteClassification(channelId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassification(entity: YoutubeV2ClassificationEntity)

    @Query("SELECT * FROM youtube_v2_classification")
    fun observeClassifications(): Flow<List<YoutubeV2ClassificationEntity>>

    @Query("SELECT * FROM youtube_v2_classification WHERE channelId = :channelId LIMIT 1")
    suspend fun classificationForChannelId(channelId: String): YoutubeV2ClassificationEntity?

    @Query(
        """
        SELECT identity.* FROM youtube_v2_identity AS identity
        INNER JOIN youtube_v2_allowlist AS allowed ON allowed.channelId = identity.channelId
        ORDER BY identity.displayName COLLATE NOCASE
        """,
    )
    fun observeAllowed(): Flow<List<YoutubeV2IdentityEntity>>

    @Query("SELECT * FROM youtube_v2_identity ORDER BY resolvedAtMs DESC")
    fun observeIdentities(): Flow<List<YoutubeV2IdentityEntity>>

    @Query("SELECT * FROM youtube_v2_identity WHERE channelId = :channelId LIMIT 1")
    suspend fun identityForChannelId(channelId: String): YoutubeV2IdentityEntity?

    @Query(
        """
        SELECT alias.channelId FROM youtube_v2_alias AS alias
        WHERE alias.aliasType = 'handle' AND alias.normalizedAlias = :handle
        LIMIT 1
        """,
    )
    suspend fun channelIdForHandle(handle: String): String?

    @Query(
        """
        SELECT DISTINCT alias.channelId FROM youtube_v2_alias AS alias
        WHERE alias.aliasType = 'display' AND alias.normalizedAlias = :display
        """,
    )
    suspend fun channelIdsForDisplay(display: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM youtube_v2_allowlist WHERE channelId = :channelId)")
    suspend fun isAllowed(channelId: String): Boolean

}

@Database(
    entities = [
        YoutubeV2IdentityEntity::class,
        YoutubeV2AliasEntity::class,
        YoutubeV2AllowlistEntity::class,
        YoutubeV2ClassificationEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class YoutubeStudyV2Database : RoomDatabase() {
    abstract fun dao(): YoutubeStudyV2Dao

    companion object { const val NAME = "youtube_study_v2.db" }
}

data class ResolveYoutubeChannelRequest(val reference: String)
data class DiscoverYoutubeHandleRequest(val handle: String)

data class ResolveYoutubeChannelResponse(val channel: ResolvedYoutubeChannelDto)
data class AvailableYoutubeChannelsResponse(val channels: List<ResolvedYoutubeChannelDto>)

data class ResolvedYoutubeChannelDto(
    val channelId: String,
    val handle: String,
    val displayName: String,
    val thumbnailUrl: String? = null,
    val source: String? = null,
)

data class YoutubeV2Resolution(
    val channel: YoutubeV2IdentityEntity,
    val source: String,
)

interface YoutubeStudyV2Api {
    @GET("youtube-study-v2/available")
    suspend fun available(@RetrofitQuery("limit") limit: Int = 100): Response<AvailableYoutubeChannelsResponse>

    @POST("youtube-study-v2/resolve")
    suspend fun resolve(@Body request: ResolveYoutubeChannelRequest): Response<ResolveYoutubeChannelResponse>

    @POST("youtube-study-v2/discover")
    suspend fun discover(@Body request: DiscoverYoutubeHandleRequest): Response<ResolveYoutubeChannelResponse>
}

enum class YoutubeV2RuntimeDecision { ALLOW, BLOCK }

data class YoutubeV2RuntimeEvaluation(
    val decision: YoutubeV2RuntimeDecision,
    val channelId: String?,
    val classification: YoutubeChannelClassification,
)

class YoutubeStudyV2Repository(
    private val database: YoutubeStudyV2Database,
    private val dao: YoutubeStudyV2Dao,
    private val api: YoutubeStudyV2Api,
) {
    val allowedChannels: Flow<List<YoutubeV2IdentityEntity>> = dao.observeAllowed()
    val visitedChannels: Flow<List<YoutubeV2IdentityEntity>> = dao.observeIdentities()
    val classifications: Flow<List<YoutubeV2ClassificationEntity>> = dao.observeClassifications()

    suspend fun resolveAndAllow(reference: String): Result<YoutubeV2Resolution> = runCatching {
        val response = api.resolve(ResolveYoutubeChannelRequest(reference.trim()))
        if (!response.isSuccessful) {
            val message = response.errorBody()?.string()?.let(::extractServerMessage)
                ?: "Could not verify this YouTube channel (${response.code()})."
            error(message)
        }
        val dto = response.body()?.channel ?: error("The resolver returned no channel.")
        require(CHANNEL_ID.matches(dto.channelId)) { "The resolver returned an invalid Channel ID." }
        val handle = normalizeHandle(dto.handle)
        require(HANDLE.matches(handle)) { "The resolver returned an invalid channel handle." }
        val entity = saveIdentity(dto)
        setClassification(entity.channelId, YoutubeChannelClassification.PRODUCTIVE)
        YoutubeV2Resolution(
            channel = entity,
            source = dto.source?.lowercase()?.takeIf { it == "cache" || it == "youtube" } ?: "unknown",
        )
    }

    suspend fun availableChannels(): Result<List<ResolvedYoutubeChannelDto>> = runCatching {
        val response = api.available()
        if (!response.isSuccessful) {
            val message = response.errorBody()?.string()?.let(::extractServerMessage)
                ?: "Could not load available channels (${response.code()})."
            error(message)
        }
        val channelsList = response.body()?.channels ?: error("The server returned no available-channel list.")
        channelsList.forEach { dto ->
            if (CHANNEL_ID.matches(dto.channelId)) {
                runCatching {
                    val handle = normalizeHandle(dto.handle)
                    if (HANDLE.matches(handle)) {
                        saveIdentity(dto)
                    }
                }
            }
        }
        channelsList
    }

    suspend fun setAvailableProductive(
        dto: ResolvedYoutubeChannelDto,
        productive: Boolean,
    ): Result<YoutubeV2IdentityEntity> = runCatching {
        val entity = saveIdentity(dto)
        setClassification(
            entity.channelId,
            if (productive) YoutubeChannelClassification.PRODUCTIVE else YoutubeChannelClassification.DISTRACTING,
        )
        entity
    }

    suspend fun setAvailableClassification(
        dto: ResolvedYoutubeChannelDto,
        classification: YoutubeChannelClassification,
    ): Result<YoutubeV2IdentityEntity> = runCatching {
        val entity = saveIdentity(dto)
        setClassification(entity.channelId, classification)
        entity
    }

    suspend fun setProductive(channelId: String, productive: Boolean) {
        setClassification(
            channelId,
            if (productive) YoutubeChannelClassification.PRODUCTIVE else YoutubeChannelClassification.DISTRACTING,
        )
    }

    suspend fun setClassification(channelId: String, classification: YoutubeChannelClassification) {
        database.withTransaction {
            dao.upsertClassification(
                YoutubeV2ClassificationEntity(channelId, classification.wire, System.currentTimeMillis()),
            )
            if (classification == YoutubeChannelClassification.PRODUCTIVE) {
                dao.allow(YoutubeV2AllowlistEntity(channelId, System.currentTimeMillis()))
            } else {
                dao.removeAllowed(channelId)
            }
        }
    }

    suspend fun deleteChannel(channelId: String) {
        database.withTransaction {
            dao.removeAllowed(channelId)
            dao.deleteClassification(channelId)
            dao.deleteAliases(channelId)
            dao.deleteIdentity(channelId)
        }
    }

    private suspend fun saveIdentity(
        dto: ResolvedYoutubeChannelDto,
    ): YoutubeV2IdentityEntity {
        require(CHANNEL_ID.matches(dto.channelId)) { "The resolver returned an invalid Channel ID." }
        val handle = normalizeHandle(dto.handle)
        require(HANDLE.matches(handle)) { "The resolver returned an invalid channel handle." }
        val entity = YoutubeV2IdentityEntity(
            channelId = dto.channelId,
            handle = handle,
            displayName = dto.displayName.trim(),
            thumbnailUrl = dto.thumbnailUrl,
            resolvedAtMs = System.currentTimeMillis(),
        )
        database.withTransaction {
            dao.upsertIdentity(entity)
            dao.upsertAliases(
                listOf(
                    YoutubeV2AliasEntity("handle", handle, entity.channelId, handle),
                    // Runtime may use this only as an exact, single-result bridge
                    // to the Channel ID. Duplicate display aliases fail closed.
                    YoutubeV2AliasEntity("display", normalizeDisplay(entity.displayName), entity.channelId, entity.displayName),
                ),
            )
        }
        return entity
    }

    suspend fun decide(exactHandle: String?, exactDisplayName: String?): YoutubeV2RuntimeDecision {
        return evaluate(exactHandle, exactDisplayName).decision
    }

    suspend fun evaluate(exactHandle: String?, exactDisplayName: String?): YoutubeV2RuntimeEvaluation {
        val normalizedHandle = exactHandle
            ?.let(::normalizeHandle)
            ?.takeIf(HANDLE::matches)
        val normalizedDisplay = exactDisplayName
                ?.let(::normalizeDisplay)
                ?.takeIf { it.isNotBlank() }
        val channelId = normalizedHandle?.let { dao.channelIdForHandle(it) }
            ?: normalizedDisplay?.let { display -> dao.channelIdsForDisplay(display).singleOrNull() }

        // 1. Individually allowed channel
        val classification = channelId
            ?.let { dao.classificationForChannelId(it)?.classification }
            ?.let(YoutubeChannelClassification::fromWire)
            ?: YoutubeChannelClassification.OTHERS
        val decision = if (channelId != null && dao.isAllowed(channelId)) {
            YoutubeV2RuntimeDecision.ALLOW
        } else {
            YoutubeV2RuntimeDecision.BLOCK
        }
        return YoutubeV2RuntimeEvaluation(decision, channelId, classification)
    }

    /**
     * Registers a newly observed exact handle in SAFAR's shared MongoDB-backed
     * catalogue. This never changes the user's productive allowlist.
     */
    suspend fun registerDiscoveredHandle(
        exactHandle: String?,
        displayName: String? = null,
    ): Result<YoutubeV2IdentityEntity?> = runCatching {
        android.util.Log.d("YTCM", "📡 registerDiscoveredHandle: input=$exactHandle display=$displayName")
        val validHandle = exactHandle
            ?.let(::normalizeHandle)
            ?.takeIf(HANDLE::matches)

        if (validHandle != null) {
            android.util.Log.d("YTCM", "📡 registerDiscoveredHandle: checking local DB for exact handle $validHandle")
            dao.channelIdForHandle(validHandle)?.let { channelId ->
                val cached = dao.identityForChannelId(channelId)
                if (cached != null) {
                    android.util.Log.d("YTCM", "✅ registerDiscoveredHandle: found in local DB $cached")
                    return@runCatching cached
                }
            }

            android.util.Log.d("YTCM", "🌐 registerDiscoveredHandle: calling api.discover for $validHandle")
            val response = runCatching { api.discover(DiscoverYoutubeHandleRequest(validHandle)) }.getOrNull()
            if (response != null && response.isSuccessful) {
                val dto = response.body()?.channel
                if (dto != null && CHANNEL_ID.matches(dto.channelId)) {
                    android.util.Log.d("YTCM", "✅ registerDiscoveredHandle: api.discover resolved $dto")
                    return@runCatching saveIdentity(dto)
                }
            }

            // Local fallback for exact handle:
            val fallbackChannelId = "handle:" + validHandle.removePrefix("@")
            val fallbackEntity = YoutubeV2IdentityEntity(
                channelId = fallbackChannelId,
                handle = validHandle,
                displayName = displayName?.trim().takeUnless { it.isNullOrBlank() } ?: validHandle,
                thumbnailUrl = null,
                resolvedAtMs = System.currentTimeMillis(),
            )
            database.withTransaction {
                dao.upsertIdentity(fallbackEntity)
                dao.upsertAliases(listOf(YoutubeV2AliasEntity("handle", validHandle, fallbackEntity.channelId, validHandle)))
                if (!displayName.isNullOrBlank()) {
                    dao.upsertAliases(listOf(YoutubeV2AliasEntity("display", normalizeDisplay(displayName), fallbackEntity.channelId, displayName.trim())))
                }
            }
            android.util.Log.d("YTCM", "✅ registerDiscoveredHandle: created handle fallback entity $fallbackEntity")
            return@runCatching fallbackEntity
        }

        // Exact handle is null — channel only exposed display name (e.g. Prime Video India, Netflix India)
        if (!displayName.isNullOrBlank()) {
            val normalized = normalizeDisplay(displayName)
            val channelIds = dao.channelIdsForDisplay(normalized)
            if (channelIds.size == 1) {
                val cached = dao.identityForChannelId(channelIds.first())
                if (cached != null) {
                    android.util.Log.d("YTCM", "✅ registerDiscoveredHandle: found in local DB by display $cached")
                    return@runCatching cached
                }
            }

            // Directly create a local entity with the real display name without guessing arbitrary handles
            val cleanSlug = normalized.replace(Regex("[^a-z0-9_]"), "_").take(40).trim('_')
            val displayChannelId = "display:" + cleanSlug.ifBlank { "unknown" }
            val displayHandle = "@" + cleanSlug.replace("_", "").take(30).ifBlank { "channel" }
            val displayEntity = YoutubeV2IdentityEntity(
                channelId = displayChannelId,
                handle = displayHandle,
                displayName = displayName.trim(),
                thumbnailUrl = null,
                resolvedAtMs = System.currentTimeMillis(),
            )
            database.withTransaction {
                dao.upsertIdentity(displayEntity)
                dao.upsertAliases(listOf(YoutubeV2AliasEntity("display", normalized, displayEntity.channelId, displayName.trim())))
            }
            android.util.Log.d("YTCM", "✅ registerDiscoveredHandle: created display-name entity $displayEntity")
            return@runCatching displayEntity
        }

        android.util.Log.e("YTCM", "❌ registerDiscoveredHandle: handle and display both null or blank")
        null
    }.also { result ->
        result.exceptionOrNull()?.let { e ->
            android.util.Log.e("YTCM", "💥 registerDiscoveredHandle THREW exception: ${e.message}", e)
        }
    }

    companion object {
        private val CHANNEL_ID = Regex("^UC[A-Za-z0-9_-]{22}$")
        private val HANDLE = Regex("^@[\\p{L}\\p{N}_.-]{3,30}$")
        fun normalizeHandle(value: String): String = "@" + value.trim().removePrefix("@").lowercase()
        fun normalizeDisplay(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase()

        private fun extractServerMessage(body: String): String? = Regex("\\\"message\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .find(body)?.groupValues?.getOrNull(1)?.replace("\\n", " ")
    }
}
