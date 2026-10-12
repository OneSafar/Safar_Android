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

    @Query("SELECT * FROM youtube_channels WHERE canonicalKey = :canonicalKey LIMIT 1")
    suspend fun getChannelByCanonicalKey(canonicalKey: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE canonicalKey = :canonicalKey LIMIT 1")
    suspend fun getPolicyByCanonicalKey(canonicalKey: String): ChannelPolicy?

    @Query("SELECT * FROM youtube_channels WHERE channelHandle = :channelHandle COLLATE NOCASE LIMIT 1")
    suspend fun getChannelByHandle(channelHandle: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE channelHandle = :channelHandle COLLATE NOCASE LIMIT 1")
    suspend fun getPolicyByHandle(channelHandle: String): ChannelPolicy?

    @Query("SELECT * FROM youtube_channels WHERE displayName = :channelName COLLATE NOCASE LIMIT 1")
    suspend fun getChannelByName(channelName: String): YouTubeChannelEntity?

    @Query("SELECT policy FROM youtube_channels WHERE displayName = :channelName COLLATE NOCASE LIMIT 1")
    suspend fun getPolicyByName(channelName: String): ChannelPolicy?

    @Query("SELECT * FROM youtube_channels ORDER BY updatedAt DESC")
    fun getAllChannels(): Flow<List<YouTubeChannelEntity>>

    @Query("SELECT * FROM youtube_channels WHERE policy = :policy ORDER BY updatedAt DESC")
    fun getChannelsByPolicy(policy: ChannelPolicy): Flow<List<YouTubeChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(channel: YouTubeChannelEntity)

    @Query("DELETE FROM youtube_channels WHERE canonicalKey = :canonicalKey")
    suspend fun deleteByCanonicalKey(canonicalKey: String)

    @Query("DELETE FROM youtube_channels WHERE channelHandle = :channelHandle COLLATE NOCASE")
    suspend fun deleteByHandle(channelHandle: String)

    @Query("DELETE FROM youtube_channels WHERE displayName = :channelName COLLATE NOCASE")
    suspend fun deleteByName(channelName: String)

    @Delete
    suspend fun deleteChannel(channel: YouTubeChannelEntity)

    @Query("DELETE FROM youtube_channels")
    suspend fun deleteAll()
}
