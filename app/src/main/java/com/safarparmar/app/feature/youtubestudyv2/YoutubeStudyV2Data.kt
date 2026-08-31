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
    val categories: String = "education",
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

    @Query(
        """
        SELECT identity.* FROM youtube_v2_identity AS identity
        INNER JOIN youtube_v2_allowlist AS allowed ON allowed.channelId = identity.channelId
        ORDER BY identity.displayName COLLATE NOCASE
        """,
    )
    fun observeAllowed(): Flow<List<YoutubeV2IdentityEntity>>

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
    ],
    version = 4,
    exportSchema = false,
)
abstract class YoutubeStudyV2Database : RoomDatabase() {
    abstract fun dao(): YoutubeStudyV2Dao

    companion object { const val NAME = "youtube_study_v2.db" }
}

data class ResolveYoutubeChannelRequest(val reference: String)

data class ResolveYoutubeChannelResponse(val channel: ResolvedYoutubeChannelDto)
data class AvailableYoutubeChannelsResponse(val channels: List<ResolvedYoutubeChannelDto>)

data class YoutubeCategoryDto(
    val id: String,
    val name: String,
    val description: String,
    val defaultAllowed: Boolean,
)

data class AvailableYoutubeCategoriesResponse(val categories: List<YoutubeCategoryDto>)

data class ResolvedYoutubeChannelDto(
    val channelId: String,
    val handle: String,
    val displayName: String,
    val thumbnailUrl: String? = null,
    val categories: List<String> = emptyList(),
    val source: String? = null,
)

data class YoutubeV2Resolution(
    val channel: YoutubeV2IdentityEntity,
    val source: String,
)

interface YoutubeStudyV2Api {
    @GET("youtube-study-v2/available")
    suspend fun available(@RetrofitQuery("limit") limit: Int = 100): Response<AvailableYoutubeChannelsResponse>

    @GET("youtube-study-v2/categories")
    suspend fun categories(): Response<AvailableYoutubeCategoriesResponse>

    @POST("youtube-study-v2/resolve")
    suspend fun resolve(@Body request: ResolveYoutubeChannelRequest): Response<ResolveYoutubeChannelResponse>
}

enum class YoutubeV2RuntimeDecision { ALLOW, BLOCK }

class YoutubeStudyV2Repository(
    private val database: YoutubeStudyV2Database,
    private val dao: YoutubeStudyV2Dao,
    private val api: YoutubeStudyV2Api,
    private val preferences: YoutubeStudyV2Preferences,
) {
    val allowedChannels: Flow<List<YoutubeV2IdentityEntity>> = dao.observeAllowed()

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
        val entity = saveIdentity(dto, productive = true)
        YoutubeV2Resolution(
            channel = entity,
            source = dto.source?.lowercase()?.takeIf { it == "cache" || it == "youtube" } ?: "unknown",
        )
    }

    suspend fun categories(): Result<List<YoutubeCategoryDto>> = runCatching {
        val response = api.categories()
        if (!response.isSuccessful) {
            val message = response.errorBody()?.string()?.let(::extractServerMessage)
                ?: "Could not load categories (${response.code()})."
            error(message)
        }
        response.body()?.categories ?: error("The server returned no categories.")
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
                        val isAllowed = dao.isAllowed(dto.channelId)
                        saveIdentity(dto, productive = isAllowed)
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
        saveIdentity(dto, productive = productive)
    }

    suspend fun setProductive(channelId: String, productive: Boolean) {
        if (productive) dao.allow(YoutubeV2AllowlistEntity(channelId, System.currentTimeMillis()))
        else dao.removeAllowed(channelId)
    }

    private suspend fun saveIdentity(
        dto: ResolvedYoutubeChannelDto,
        productive: Boolean,
    ): YoutubeV2IdentityEntity {
        require(CHANNEL_ID.matches(dto.channelId)) { "The resolver returned an invalid Channel ID." }
        val handle = normalizeHandle(dto.handle)
        require(HANDLE.matches(handle)) { "The resolver returned an invalid channel handle." }
        val categoriesStr = if (dto.categories.isNotEmpty()) dto.categories.joinToString(",") else "education"
        val entity = YoutubeV2IdentityEntity(
            channelId = dto.channelId,
            handle = handle,
            displayName = dto.displayName.trim(),
            thumbnailUrl = dto.thumbnailUrl,
            categories = categoriesStr,
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
            if (productive) dao.allow(YoutubeV2AllowlistEntity(entity.channelId, System.currentTimeMillis()))
            else dao.removeAllowed(entity.channelId)
        }
        return entity
    }

    suspend fun decide(exactHandle: String?, exactDisplayName: String?): YoutubeV2RuntimeDecision {
        val normalizedHandle = exactHandle
            ?.let(::normalizeHandle)
            ?.takeIf(HANDLE::matches)
        val normalizedDisplay = exactDisplayName
                ?.let(::normalizeDisplay)
                ?.takeIf { it.isNotBlank() }
        val channelId = normalizedHandle?.let { dao.channelIdForHandle(it) }
            ?: normalizedDisplay?.let { display -> dao.channelIdsForDisplay(display).singleOrNull() }

        // 1. Individually allowed channel
        if (channelId != null && dao.isAllowed(channelId)) return YoutubeV2RuntimeDecision.ALLOW

        // 2. Allowed categories check from local Room DB
        val allowedCategories = preferences.allowedCategories.value.map { it.trim().lowercase() }.toSet()
        if (channelId != null) {
            val identity = dao.identityForChannelId(channelId)
            if (identity != null) {
                val channelCategories = identity.categories.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                if (channelCategories.any { allowedCategories.contains(it) }) {
                    return YoutubeV2RuntimeDecision.ALLOW
                }
            }
        }

        // 3. If unlisted channel and categories are active, resolve on the fly using YouTube Data API
        if (channelId == null && normalizedHandle != null && allowedCategories.isNotEmpty()) {
            val resolvedDto = runCatching {
                val res = api.resolve(ResolveYoutubeChannelRequest(normalizedHandle))
                if (res.isSuccessful) res.body()?.channel else null
            }.getOrNull()

            if (resolvedDto != null && CHANNEL_ID.matches(resolvedDto.channelId)) {
                val savedEntity = saveIdentity(resolvedDto, productive = false)
                val channelCategories = savedEntity.categories.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                if (channelCategories.any { allowedCategories.contains(it) }) {
                    return YoutubeV2RuntimeDecision.ALLOW
                }
            }
        }

        return YoutubeV2RuntimeDecision.BLOCK
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
