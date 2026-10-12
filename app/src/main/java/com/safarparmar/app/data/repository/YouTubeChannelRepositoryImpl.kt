package com.safarparmar.app.data.repository

import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
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

    override suspend fun getPolicy(identity: ChannelIdentity): ChannelPolicy? =
        withContext(ioDispatcher) {
            val canonicalKey = identity.canonicalKey
            if (canonicalKey.isNotEmpty()) {
                // If identity has no verified handle, check if an existing entity with this display name has a handle
                if (!identity.hasHandle && identity.displayName.isNotBlank()) {
                    val cleanName = identity.displayName.trim().replace(Regex("\\s+"), " ")
                    val normName = ChannelIdentity.normalizeDisplayName(identity.displayName)
                    val existing = youTubeChannelDao.getChannelByName(cleanName)
                        ?: (if (normName != cleanName) youTubeChannelDao.getChannelByName(normName) else null)
                        ?: youTubeChannelDao.getChannelByName(identity.displayName.trim())
                    if (existing != null && !existing.channelHandle.isNullOrBlank()) {
                        return@withContext existing.policy
                    }
                }
                val policy = youTubeChannelDao.getPolicyByCanonicalKey(canonicalKey)
                if (policy != null) return@withContext policy
            }
            // Fallback checks
            if (!identity.verifiedHandle.isNullOrBlank()) {
                val normHandle = ChannelIdentity.normalizeHandle(identity.verifiedHandle)
                if (normHandle.isNotEmpty()) {
                    val policy = youTubeChannelDao.getPolicyByHandle(normHandle)
                        ?: youTubeChannelDao.getPolicyByHandle(identity.verifiedHandle.trim())
                    if (policy != null) return@withContext policy
                }
            }
            if (identity.displayName.isNotBlank()) {
                val cleanName = identity.displayName.trim().replace(Regex("\\s+"), " ")
                val normName = ChannelIdentity.normalizeDisplayName(identity.displayName)
                val policy = youTubeChannelDao.getPolicyByName(cleanName)
                    ?: youTubeChannelDao.getChannelByName(cleanName)?.policy
                    ?: (if (normName != cleanName) {
                        youTubeChannelDao.getPolicyByName(normName)
                            ?: youTubeChannelDao.getChannelByName(normName)?.policy
                    } else null)
                    ?: youTubeChannelDao.getPolicyByName(identity.displayName.trim())
                    ?: youTubeChannelDao.getChannelByName(identity.displayName.trim())?.policy
                if (policy != null) return@withContext policy
            }
            null
        }

    override suspend fun savePolicy(identity: ChannelIdentity, policy: ChannelPolicy) =
        withContext(ioDispatcher) {
            val canonicalKey = identity.canonicalKey
            if (canonicalKey.isEmpty()) return@withContext

            val cleanName = identity.displayName.trim().replace(Regex("\\s+"), " ")
            val normName = ChannelIdentity.normalizeDisplayName(identity.displayName)

            // If identity has only displayName (no verified handle), check if an entity with this displayName already exists with a verified handle
            if (!identity.hasHandle && cleanName.isNotEmpty()) {
                val existing = youTubeChannelDao.getChannelByName(cleanName)
                    ?: (if (normName != cleanName) youTubeChannelDao.getChannelByName(normName) else null)
                    ?: youTubeChannelDao.getChannelByName(identity.displayName.trim())
                if (existing != null && !existing.channelHandle.isNullOrBlank()) {
                    val updated = existing.copy(
                        policy = policy,
                        updatedAt = System.currentTimeMillis(),
                    )
                    youTubeChannelDao.insertOrUpdate(updated)
                    val obsoleteNameKey = "name:$normName"
                    if (existing.canonicalKey != obsoleteNameKey) {
                        youTubeChannelDao.deleteByCanonicalKey(obsoleteNameKey)
                    }
                    return@withContext
                }
            }

            // If saving with verified handle and displayName, remove any obsolete name-only row
            if (identity.hasHandle && normName.isNotEmpty()) {
                val obsoleteNameKey = "name:$normName"
                youTubeChannelDao.deleteByCanonicalKey(obsoleteNameKey)
            }

            val entity = YouTubeChannelEntity(
                canonicalKey = canonicalKey,
                channelHandle = identity.verifiedHandle?.let { ChannelIdentity.normalizeHandle(it) }?.takeIf { it.isNotEmpty() },
                displayName = cleanName,
                policy = policy,
                updatedAt = System.currentTimeMillis(),
            )
            youTubeChannelDao.insertOrUpdate(entity)
        }

    override suspend fun getPolicyByHandle(handle: String): ChannelPolicy? = getChannelPolicy(handle)

    override suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy? =
        withContext(ioDispatcher) {
            val trimmed = channelHandle.trim()
            if (trimmed.isEmpty()) return@withContext null

            if (trimmed.startsWith("handle:") || trimmed.startsWith("name:")) {
                val byCanonical = youTubeChannelDao.getPolicyByCanonicalKey(trimmed)
                if (byCanonical != null) return@withContext byCanonical
            }

            val normalizedHandle = ChannelIdentity.normalizeHandle(trimmed)
            if (normalizedHandle.isNotEmpty()) {
                val policyByHandle = youTubeChannelDao.getPolicyByHandle(normalizedHandle)
                    ?: youTubeChannelDao.getPolicyByHandle(trimmed)
                if (policyByHandle != null) return@withContext policyByHandle
            }

            val cleanName = trimmed.replace(Regex("\\s+"), " ")
            val normName = ChannelIdentity.normalizeDisplayName(trimmed)
            val policyByName = youTubeChannelDao.getChannelByName(cleanName)?.policy
                ?: youTubeChannelDao.getPolicyByName(cleanName)
                ?: (if (normName != cleanName) {
                    youTubeChannelDao.getChannelByName(normName)?.policy
                        ?: youTubeChannelDao.getPolicyByName(normName)
                } else null)
                ?: youTubeChannelDao.getChannelByName(trimmed)?.policy
                ?: youTubeChannelDao.getPolicyByName(trimmed)
            if (policyByName != null) return@withContext policyByName

            null
        }

    override suspend fun saveChannelPolicy(
        channelHandle: String,
        channelName: String,
        policy: ChannelPolicy,
    ) = withContext(ioDispatcher) {
        val cleanHandle = channelHandle.trim().takeIf { it.isNotEmpty() }
            ?.let { ChannelIdentity.normalizeHandle(it) }?.takeIf { it.isNotEmpty() }
        val cleanName = channelName.trim().replace(Regex("\\s+"), " ").ifEmpty {
            if (cleanHandle == null) channelHandle.trim().replace(Regex("\\s+"), " ") else ""
        }

        // Build genuine ChannelIdentity — DO NOT fabricate fake handles!
        val identity = ChannelIdentity(
            verifiedHandle = cleanHandle,
            displayName = cleanName,
        )

        if (identity.canonicalKey.isEmpty()) return@withContext

        savePolicy(identity, policy)
    }

    override fun getAllChannels(): Flow<List<YouTubeChannelEntity>> {
        return youTubeChannelDao.getAllChannels().flowOn(ioDispatcher)
    }

    override suspend fun getChannel(channelHandle: String): YouTubeChannelEntity? =
        withContext(ioDispatcher) {
            val trimmed = channelHandle.trim()
            if (trimmed.isEmpty()) return@withContext null

            if (trimmed.startsWith("handle:") || trimmed.startsWith("name:")) {
                val byCanonical = youTubeChannelDao.getChannelByCanonicalKey(trimmed)
                if (byCanonical != null) return@withContext byCanonical
            }

            val normalizedHandle = ChannelIdentity.normalizeHandle(trimmed)
            if (normalizedHandle.isNotEmpty()) {
                val byHandle = youTubeChannelDao.getChannelByHandle(normalizedHandle)
                    ?: youTubeChannelDao.getChannelByHandle(trimmed)
                if (byHandle != null) return@withContext byHandle
            }

            val cleanName = trimmed.replace(Regex("\\s+"), " ")
            val normName = ChannelIdentity.normalizeDisplayName(trimmed)
            val byName = youTubeChannelDao.getChannelByName(cleanName)
                ?: (if (normName != cleanName) youTubeChannelDao.getChannelByName(normName) else null)
                ?: youTubeChannelDao.getChannelByName(trimmed)
            if (byName != null) return@withContext byName

            null
        }

    override suspend fun getChannelByIdentity(identity: ChannelIdentity): YouTubeChannelEntity? =
        withContext(ioDispatcher) {
            val canonicalKey = identity.canonicalKey
            if (canonicalKey.isNotEmpty()) {
                val channel = youTubeChannelDao.getChannelByCanonicalKey(canonicalKey)
                if (channel != null) return@withContext channel
            }
            if (!identity.verifiedHandle.isNullOrBlank()) {
                val normHandle = ChannelIdentity.normalizeHandle(identity.verifiedHandle)
                if (normHandle.isNotEmpty()) {
                    val channel = youTubeChannelDao.getChannelByHandle(normHandle)
                        ?: youTubeChannelDao.getChannelByHandle(identity.verifiedHandle.trim())
                    if (channel != null) return@withContext channel
                }
            }
            if (identity.displayName.isNotBlank()) {
                val cleanName = identity.displayName.trim().replace(Regex("\\s+"), " ")
                val normName = ChannelIdentity.normalizeDisplayName(identity.displayName)
                val channel = youTubeChannelDao.getChannelByName(cleanName)
                    ?: (if (normName != cleanName) youTubeChannelDao.getChannelByName(normName) else null)
                    ?: youTubeChannelDao.getChannelByName(identity.displayName.trim())
                if (channel != null) return@withContext channel
            }
            null
        }

    override suspend fun deleteChannel(channelHandle: String) =
        withContext(ioDispatcher) {
            val trimmed = channelHandle.trim()
            if (trimmed.isEmpty()) return@withContext

            val cleanName = trimmed.replace(Regex("\\s+"), " ")
            val normName = ChannelIdentity.normalizeDisplayName(trimmed)

            // 1. Direct canonical key deletion if input is a canonical key
            if (trimmed.startsWith("handle:") || trimmed.startsWith("name:")) {
                youTubeChannelDao.deleteByCanonicalKey(trimmed)
            }

            // 2. Check if an entity exists by canonicalKey, name, or handle
            val existing = (if (trimmed.startsWith("handle:") || trimmed.startsWith("name:")) youTubeChannelDao.getChannelByCanonicalKey(trimmed) else null)
                ?: youTubeChannelDao.getChannelByName(cleanName)
                ?: (if (normName != cleanName) youTubeChannelDao.getChannelByName(normName) else null)
                ?: youTubeChannelDao.getChannelByName(trimmed)
                ?: youTubeChannelDao.getChannelByHandle(trimmed)
                ?: youTubeChannelDao.getChannelByHandle(ChannelIdentity.normalizeHandle(trimmed).takeIf { it.isNotEmpty() } ?: "")

            if (existing != null) {
                youTubeChannelDao.deleteByCanonicalKey(existing.canonicalKey)
                existing.channelHandle?.let {
                    youTubeChannelDao.deleteByHandle(it)
                    val normH = ChannelIdentity.normalizeHandle(it)
                    if (normH.isNotEmpty()) {
                        youTubeChannelDao.deleteByCanonicalKey("handle:$normH")
                    }
                }
                if (existing.displayName.isNotBlank()) {
                    val existingNormName = ChannelIdentity.normalizeDisplayName(existing.displayName)
                    if (existingNormName.isNotEmpty()) {
                        youTubeChannelDao.deleteByCanonicalKey("name:$existingNormName")
                    }
                    val existingCleanName = existing.displayName.trim().replace(Regex("\\s+"), " ")
                    youTubeChannelDao.deleteByName(existingCleanName)
                    if (existingNormName != existingCleanName) {
                        youTubeChannelDao.deleteByName(existingNormName)
                    }
                    if (existing.displayName != existingCleanName) {
                        youTubeChannelDao.deleteByName(existing.displayName)
                    }
                }
            }

            // 3. Fallback handle deletion
            val normalizedHandle = ChannelIdentity.normalizeHandle(trimmed)
            if (normalizedHandle.isNotEmpty()) {
                youTubeChannelDao.deleteByCanonicalKey("handle:$normalizedHandle")
                youTubeChannelDao.deleteByHandle(normalizedHandle)
            }
            youTubeChannelDao.deleteByHandle(trimmed)

            // 4. Fallback name deletion
            if (normName.isNotEmpty()) {
                youTubeChannelDao.deleteByCanonicalKey("name:$normName")
            }
            youTubeChannelDao.deleteByName(cleanName)
            if (normName != cleanName) {
                youTubeChannelDao.deleteByName(normName)
            }
            if (trimmed != cleanName) {
                youTubeChannelDao.deleteByName(trimmed)
            }
        }

    override suspend fun deleteChannel(identity: ChannelIdentity) =
        withContext(ioDispatcher) {
            val cleanName = identity.displayName.trim().replace(Regex("\\s+"), " ")
            val normName = ChannelIdentity.normalizeDisplayName(identity.displayName)
            if (cleanName.isNotEmpty()) {
                val existing = youTubeChannelDao.getChannelByName(cleanName)
                    ?: (if (normName != cleanName) youTubeChannelDao.getChannelByName(normName) else null)
                    ?: youTubeChannelDao.getChannelByName(identity.displayName.trim())
                if (existing != null) {
                    youTubeChannelDao.deleteByCanonicalKey(existing.canonicalKey)
                    existing.channelHandle?.let {
                        youTubeChannelDao.deleteByHandle(it)
                        val normH = ChannelIdentity.normalizeHandle(it)
                        if (normH.isNotEmpty()) {
                            youTubeChannelDao.deleteByCanonicalKey("handle:$normH")
                        }
                    }
                }
                if (normName.isNotEmpty()) {
                    youTubeChannelDao.deleteByCanonicalKey("name:$normName")
                }
                youTubeChannelDao.deleteByName(cleanName)
                if (normName != cleanName) {
                    youTubeChannelDao.deleteByName(normName)
                }
                if (identity.displayName.trim() != cleanName) {
                    youTubeChannelDao.deleteByName(identity.displayName.trim())
                }
            }

            val canonicalKey = identity.canonicalKey
            if (canonicalKey.isNotEmpty()) {
                youTubeChannelDao.deleteByCanonicalKey(canonicalKey)
            }
            if (normName.isNotEmpty()) {
                youTubeChannelDao.deleteByCanonicalKey("name:$normName")
            }
            if (!identity.verifiedHandle.isNullOrBlank()) {
                val norm = ChannelIdentity.normalizeHandle(identity.verifiedHandle)
                if (norm.isNotEmpty()) {
                    youTubeChannelDao.deleteByCanonicalKey("handle:$norm")
                    youTubeChannelDao.deleteByHandle(norm)
                }
                youTubeChannelDao.deleteByHandle(identity.verifiedHandle.trim())
            }
        }
}
