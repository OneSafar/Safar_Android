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
        GROUP BY alias.normalizedAlias HAVING COUNT(DISTINCT alias.channelId) = 1
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

    @Query("DELETE FROM youtube_v2_alias WHERE aliasType = 'handle' AND normalizedAlias = :handle")
    suspend fun deleteHandleAlias(handle: String)

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
    version = 8,
    exportSchema = false,
)
abstract class YoutubeStudyV2Database : RoomDatabase() {
    abstract fun dao(): YoutubeStudyV2Dao

    companion object { const val NAME = "youtube_study_v2.db" }
}

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

enum class YoutubeV2RuntimeDecision { ALLOW, BLOCK }

data class YoutubeV2RuntimeEvaluation(
    val decision: YoutubeV2RuntimeDecision,
    val channelId: String?,
    val classification: YoutubeChannelClassification,
)

class YoutubeStudyV2Repository(
    private val database: YoutubeStudyV2Database,
    private val dao: YoutubeStudyV2Dao,
) {
    val allowedChannels: Flow<List<YoutubeV2IdentityEntity>> = dao.observeAllowed()
    val visitedChannels: Flow<List<YoutubeV2IdentityEntity>> = dao.observeIdentities()
    val classifications: Flow<List<YoutubeV2ClassificationEntity>> = dao.observeClassifications()

    suspend fun resolveAndAllow(reference: String): Result<YoutubeV2Resolution> = runCatching {
        val entered = reference.trim().replace(Regex("\\s+"), " ")
        require(entered.length in 2..120) { "Enter the channel name or @handle shown by YouTube." }
        val handle = entered.takeIf { it.startsWith('@') }?.let(::normalizeHandle)
        if (handle != null) require(HANDLE.matches(handle)) { "Enter the complete @handle shown by YouTube." }
        val display = handle?.let { entered } ?: entered
        val normalized = handle ?: normalizeDisplay(display)
        val entity = YoutubeV2IdentityEntity(
            channelId = "accessibility:${if (handle != null) "handle" else "display"}:$normalized",
            handle = handle.orEmpty(),
            displayName = display,
            thumbnailUrl = null,
            resolvedAtMs = System.currentTimeMillis(),
        )
        saveAccessibilityIdentity(entity, handle, display.takeIf { handle == null })
        setClassification(entity.channelId, YoutubeChannelClassification.PRODUCTIVE)
        YoutubeV2Resolution(channel = entity, source = "accessibility")
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
        val handle = normalizeHandle(dto.handle)
        require(HANDLE.matches(handle)) { "Enter a valid channel handle." }
        val entity = YoutubeV2IdentityEntity(
            channelId = dto.channelId,
            handle = handle,
            displayName = dto.displayName.trim(),
            thumbnailUrl = dto.thumbnailUrl,
            resolvedAtMs = System.currentTimeMillis(),
        )
        database.withTransaction {
            dao.upsertIdentity(entity)
            dao.deleteAliases(entity.channelId)
            dao.deleteHandleAlias(handle)
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

    private suspend fun saveAccessibilityIdentity(
        entity: YoutubeV2IdentityEntity,
        handle: String?,
        displayName: String?,
    ) {
        val aliases = buildList {
            handle?.let { add(YoutubeV2AliasEntity("handle", it, entity.channelId, it)) }
            displayName?.let {
                add(YoutubeV2AliasEntity("display", normalizeDisplay(it), entity.channelId, it))
            }
        }
        database.withTransaction {
            dao.upsertIdentity(entity)
            dao.deleteAliases(entity.channelId)
            if (handle != null) dao.deleteHandleAlias(handle)
            dao.upsertAliases(aliases)
        }
    }

    suspend fun decide(exactHandle: String?, exactDisplayName: String?): YoutubeV2RuntimeDecision {
        return evaluate(exactHandle, exactDisplayName).decision
    }

    suspend fun evaluate(
        exactHandle: String?,
        exactDisplayName: String?,
        exactChannelId: String? = null,
    ): YoutubeV2RuntimeEvaluation {
        val handleId = exactHandle?.let(::normalizeHandle)?.takeIf(HANDLE::matches)
            ?.let { dao.channelIdForHandle(it) }
        val displayIds = exactDisplayName?.let(::normalizeDisplay)?.takeIf { it.isNotBlank() }
            ?.let { dao.channelIdsForDisplay(it) }.orEmpty()
        val matchedIds = (listOfNotNull(exactChannelId, handleId) + displayIds).distinct()
        val allowedId = matchedIds.firstOrNull { dao.isAllowed(it) }
        var classifiedMatch: Pair<String, YoutubeChannelClassification>? = null
        if (allowedId == null) {
            for (channelId in matchedIds) {
                val classification = dao.classificationForChannelId(channelId)
                    ?.classification
                    ?.let(YoutubeChannelClassification::fromWire)
                if (classification == YoutubeChannelClassification.DISTRACTING) {
                    classifiedMatch = channelId to classification
                    break
                }
            }
        }
        val matchedId = allowedId ?: classifiedMatch?.first ?: matchedIds.singleOrNull()
        val classification = when {
            allowedId != null -> YoutubeChannelClassification.PRODUCTIVE
            classifiedMatch != null -> classifiedMatch.second
            else -> YoutubeChannelClassification.OTHERS
        }
        return YoutubeV2RuntimeEvaluation(
            decision = if (allowedId != null) YoutubeV2RuntimeDecision.ALLOW else YoutubeV2RuntimeDecision.BLOCK,
            channelId = matchedId,
            classification = classification,
        )
    }

    suspend fun evaluateChannelId(channelId: String?): YoutubeV2RuntimeEvaluation {
        val verifiedId = channelId
        val classification = verifiedId
            ?.let { dao.classificationForChannelId(it)?.classification }
            ?.let(YoutubeChannelClassification::fromWire)
            ?: YoutubeChannelClassification.OTHERS
        return YoutubeV2RuntimeEvaluation(
            if (classification == YoutubeChannelClassification.PRODUCTIVE) YoutubeV2RuntimeDecision.ALLOW
            else YoutubeV2RuntimeDecision.BLOCK,
            verifiedId,
            classification,
        )
    }

    companion object {
        private val CHANNEL_ID = Regex("^UC[A-Za-z0-9_-]{22}$")
        private val HANDLE = Regex("^@[\\p{L}\\p{N}_.-]{3,30}$")
        fun normalizeHandle(value: String): String = "@" + value.trim().removePrefix("@").lowercase()
        fun normalizeDisplay(value: String): String = value.trim().replace(Regex("\\s+"), " ").lowercase()
    }
}
