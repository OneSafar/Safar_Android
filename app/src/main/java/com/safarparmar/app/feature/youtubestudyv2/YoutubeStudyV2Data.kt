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
        LIMIT 1
        """,
    )
    suspend fun channelIdForHandle(handle: String): String?

    @Query(
        """
        SELECT DISTINCT alias.channelId FROM youtube_v2_alias AS alias
        WHERE alias.aliasType = 'handle' AND alias.normalizedAlias = :handle
        """,
    )
    suspend fun channelIdsForHandle(handle: String): List<String>

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

    @Query("SELECT * FROM youtube_v2_identity")
    suspend fun allIdentities(): List<YoutubeV2IdentityEntity>

    @Query("SELECT * FROM youtube_v2_classification")
    suspend fun allClassifications(): List<YoutubeV2ClassificationEntity>

    @Query("SELECT * FROM youtube_v2_allowlist")
    suspend fun allAllowed(): List<YoutubeV2AllowlistEntity>

    @Query("SELECT * FROM youtube_v2_alias")
    suspend fun allAliases(): List<YoutubeV2AliasEntity>

}

@Database(
    entities = [
        YoutubeV2IdentityEntity::class,
        YoutubeV2AliasEntity::class,
        YoutubeV2AllowlistEntity::class,
        YoutubeV2ClassificationEntity::class,
    ],
    version = 9,
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
    private val backup: YoutubeAllowlistBackupStore? = null,
) {

    val allowedChannels: Flow<List<YoutubeV2IdentityEntity>> = dao.observeAllowed()
    val visitedChannels: Flow<List<YoutubeV2IdentityEntity>> = dao.observeIdentities()
    val classifications: Flow<List<YoutubeV2ClassificationEntity>> = dao.observeClassifications()

    suspend fun resolveAndAllow(
        reference: String,
        pairedDisplayName: String? = null,
    ): Result<YoutubeV2Resolution> = runCatching {
        val entered = YoutubeChannelReference.canonical(reference)
        require(entered.length in 2..120) { "Enter the channel name or @handle shown by YouTube." }
        val paired = pairedDisplayName?.let(YoutubeChannelReference::clean)?.takeIf { it.isNotBlank() }
        val handle = entered.takeIf { it.startsWith('@') }?.let(::normalizeHandle)
            ?: paired?.takeIf { it.startsWith('@') }?.let(::normalizeHandle)
            ?: (if (HANDLE.matches("@$entered")) normalizeHandle("@$entered") else null)
        val display = paired?.takeUnless { it.startsWith('@') }
            ?: entered.removePrefix("@").trim()
        val normalized = handle ?: normalizeDisplay(display)
        val entity = YoutubeV2IdentityEntity(
            channelId = "accessibility:${if (handle != null) "handle" else "display"}:$normalized",
            handle = handle.orEmpty(),
            displayName = display.ifBlank { entered },
            thumbnailUrl = null,
            resolvedAtMs = System.currentTimeMillis(),
        )
        saveAccessibilityIdentity(entity, handle, display)
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
        backupNow()
    }

    suspend fun deleteChannel(channelId: String) {
        database.withTransaction {
            dao.removeAllowed(channelId)
            dao.deleteClassification(channelId)
            dao.deleteAliases(channelId)
            dao.deleteIdentity(channelId)
        }
        backupNow()
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
        val normalizedHandle = handle?.let(::normalizeHandle)?.takeIf(HANDLE::matches)
        val normalizedDisplay = displayName?.let(::normalizeDisplay)?.takeIf { it.isNotBlank() }
        val candidateDisplay = normalizedHandle?.removePrefix("@")?.let(::normalizeDisplay)
        val candidateHandle = normalizedDisplay?.replace(" ", "")?.let { "@$it" }?.takeIf(HANDLE::matches)

        val aliases = buildList {
            normalizedHandle?.let {
                add(YoutubeV2AliasEntity("handle", it, entity.channelId, entity.displayName.ifBlank { it }))
            }
            candidateHandle?.takeIf { it != normalizedHandle }?.let {
                add(YoutubeV2AliasEntity("handle", it, entity.channelId, entity.displayName.ifBlank { it }))
            }
            normalizedDisplay?.let {
                add(YoutubeV2AliasEntity("display", it, entity.channelId, entity.displayName))
            }
            candidateDisplay?.takeIf { it != normalizedDisplay }?.let {
                add(YoutubeV2AliasEntity("display", it, entity.channelId, entity.displayName))
            }
        }.distinctBy { it.aliasType to it.normalizedAlias }

        database.withTransaction {
            dao.upsertIdentity(entity)
            dao.deleteAliases(entity.channelId)
            normalizedHandle?.let { dao.deleteHandleAlias(it) }
            candidateHandle?.let { dao.deleteHandleAlias(it) }
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
        val normHandle = exactHandle?.let(::normalizeHandle)?.takeIf(HANDLE::matches)
        val normDisplay = exactDisplayName?.let(::normalizeDisplay)?.takeIf { it.isNotBlank() }

        val candidateDisplay = normHandle?.removePrefix("@")?.let(::normalizeDisplay)
        val candidateHandle = normDisplay?.replace(" ", "")?.let { "@$it" }?.takeIf(HANDLE::matches)

        val handleCandidates = listOfNotNull(normHandle, candidateHandle).distinct()
        val displayCandidates = listOfNotNull(normDisplay, candidateDisplay).distinct()

        val handleIds = handleCandidates.flatMap { h ->
            val list = dao.channelIdsForHandle(h)
            if (list.isNotEmpty()) list else listOfNotNull(dao.channelIdForHandle(h))
        }
        val displayIds = displayCandidates.flatMap { d ->
            dao.channelIdsForDisplay(d)
        }

        val matchedIds = (listOfNotNull(exactChannelId) + handleIds + displayIds).distinct()
        val allowedId = matchedIds.firstOrNull { dao.isAllowed(it) }
        if (allowedId != null) {
            learnPairedAliases(allowedId, normHandle, normDisplay, exactDisplayName, exactChannelId)
        }
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

    /**
     * A watch page can show the uploader's @handle and display name together.
     * When one of them identifies an allowed channel, remember the other one,
     * so a later read that only shows the other value still matches.
     * Example: "@EZVAL789" is allowed and the owner row says "EZVAL".
     * Only exact runtime values are learned, never derived candidates. A value
     * is learned only when no other channel already claims it.
     */
    private suspend fun learnPairedAliases(
        allowedId: String,
        normHandle: String?,
        normDisplay: String?,
        rawDisplay: String?,
        exactChannelId: String?,
    ) {
        if (normHandle == null || normDisplay == null) return
        try {
            val exactHandleIds = dao.channelIdsForHandle(normHandle)
                .ifEmpty { listOfNotNull(dao.channelIdForHandle(normHandle)) }
                .distinct()
            val exactDisplayIds = dao.channelIdsForDisplay(normDisplay).distinct()
            val strongMatch = exactChannelId == allowedId || allowedId in exactHandleIds
            val learned = buildList {
                if (strongMatch && exactDisplayIds.isEmpty()) {
                    add(
                        YoutubeV2AliasEntity(
                            "display",
                            normDisplay,
                            allowedId,
                            rawDisplay?.trim()?.replace(Regex("\\s+"), " ").orEmpty().ifBlank { normDisplay },
                        ),
                    )
                }
                if (exactHandleIds.isEmpty() && exactDisplayIds == listOf(allowedId)) {
                    add(YoutubeV2AliasEntity("handle", normHandle, allowedId, normHandle))
                }
            }
            if (learned.isNotEmpty()) {
                dao.upsertAliases(learned)
                backupNow()
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Learning is best effort. It never changes the current decision.
        }
    }

    /** Copies every channel decision to the backup file. Best effort; never fails a change. */
    private suspend fun backupNow() {
        val store = backup ?: return
        try {
            val snapshot = YoutubeAllowlistSnapshot.from(
                dao.allIdentities(),
                dao.allClassifications(),
                dao.allAllowed(),
                dao.allAliases(),
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { store.save(snapshot) }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A failed backup leaves the previous copy in place.
        }
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
        fun normalizeHandle(value: String): String =
            "@" + YoutubeChannelReference.clean(value).removePrefix("@").lowercase()
        fun normalizeDisplay(value: String): String = YoutubeChannelReference.clean(value).lowercase()
    }
}
