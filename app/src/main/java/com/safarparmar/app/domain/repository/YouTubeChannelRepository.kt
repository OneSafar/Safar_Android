package com.safarparmar.app.domain.repository

import com.safarparmar.app.data.local.entity.ChannelPolicy
import com.safarparmar.app.data.local.entity.YouTubeChannelEntity
import com.safarparmar.app.feature.youtube.model.ChannelIdentity
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for YouTube channel classifications.
 */
interface YouTubeChannelRepository {

    /**
     * Retrieves the policy for the given [ChannelIdentity], or null if unclassified.
     */
    suspend fun getPolicy(identity: ChannelIdentity): ChannelPolicy? =
        if (!identity.verifiedHandle.isNullOrBlank()) {
            getChannelPolicy(identity.verifiedHandle)
        } else if (identity.displayName.isNotBlank()) {
            getChannelPolicy(identity.displayName)
        } else {
            null
        }

    /**
     * Persists or updates the policy classification for a [ChannelIdentity].
     * Never generates synthetic fake handles.
     */
    suspend fun savePolicy(identity: ChannelIdentity, policy: ChannelPolicy) {
        saveChannelPolicy(
            channelHandle = identity.verifiedHandle.orEmpty(),
            channelName = identity.displayName,
            policy = policy,
        )
    }

    /**
     * Retrieves the policy for the given channel handle, or null if unclassified.
     */
    suspend fun getPolicyByHandle(handle: String): ChannelPolicy? = getChannelPolicy(handle)

    /**
     * Retrieves the policy for the given channel handle, or null if unclassified.
     */
    suspend fun getChannelPolicy(channelHandle: String): ChannelPolicy?

    /**
     * Persists or updates the policy classification for a channel handle.
     * Never fabricates synthetic handles when channelHandle is empty.
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
     * Retrieves the full entity record for a channel handle or name, or null if unclassified.
     */
    suspend fun getChannel(channelHandle: String): YouTubeChannelEntity?

    /**
     * Retrieves the full entity record for a [ChannelIdentity], or null if unclassified.
     */
    suspend fun getChannelByIdentity(identity: ChannelIdentity): YouTubeChannelEntity? =
        if (!identity.verifiedHandle.isNullOrBlank()) {
            getChannel(identity.verifiedHandle)
        } else if (identity.displayName.isNotBlank()) {
            getChannel(identity.displayName)
        } else {
            null
        }

    /**
     * Deletes a channel record by handle.
     */
    suspend fun deleteChannel(channelHandle: String)

    /**
     * Deletes a channel record by [ChannelIdentity].
     */
    suspend fun deleteChannel(identity: ChannelIdentity) {
        if (!identity.verifiedHandle.isNullOrBlank()) {
            deleteChannel(identity.verifiedHandle)
        } else if (identity.displayName.isNotBlank()) {
            deleteChannel(identity.displayName)
        }
    }
}
