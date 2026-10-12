package com.safarparmar.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.safarparmar.app.feature.youtube.model.ChannelIdentity

/**
 * Room entity representing a persisted YouTube channel classification.
 *
 * Uses [canonicalKey] as the primary key, eliminating the need for synthetic fake handles
 * and supporting channels identified purely by display name or by verified handle.
 *
 * @property canonicalKey Canonical identity key (e.g. "handle:@veritasium" or "name:khan academy"), primary key.
 * @property channelHandle Real YouTube handle (e.g. "@Veritasium"), nullable if not detected.
 * @property displayName Display name of the channel.
 * @property policy Classification policy (PRODUCTIVE or DISTRACTING).
 * @property updatedAt Epoch timestamp of last update.
 */
@Entity(
    tableName = "youtube_channels",
    indices = [
        Index(value = ["channelHandle"], unique = false),
        Index(value = ["displayName"], unique = false),
    ],
)
data class YouTubeChannelEntity(
    @PrimaryKey
    @ColumnInfo(name = "canonicalKey")
    val canonicalKey: String,
    @ColumnInfo(name = "channelHandle", collate = ColumnInfo.NOCASE)
    val channelHandle: String? = null,
    @ColumnInfo(name = "displayName")
    val displayName: String = "",
    @ColumnInfo(name = "policy")
    val policy: ChannelPolicy,
    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),
) {
    @get:Ignore
    val channelName: String
        get() = displayName

    @get:Ignore
    val verifiedHandle: String?
        get() = channelHandle

    /**
     * Backward-compatible secondary constructor.
     */
    @Ignore
    constructor(
        channelHandle: String?,
        channelName: String = "",
        policy: ChannelPolicy,
        updatedAt: Long = System.currentTimeMillis(),
    ) : this(
        canonicalKey = computeCanonicalKey(channelHandle, channelName),
        channelHandle = channelHandle?.let { ChannelIdentity.normalizeHandle(it) }?.takeIf { it.isNotEmpty() },
        displayName = channelName.trim().replace(Regex("\\s+"), " "),
        policy = policy,
        updatedAt = updatedAt,
    )

    fun toIdentity(): ChannelIdentity {
        return ChannelIdentity(
            verifiedHandle = channelHandle,
            displayName = displayName,
        )
    }

    companion object {
        fun computeCanonicalKey(handle: String?, displayName: String): String {
            val normHandle = handle?.let { ChannelIdentity.normalizeHandle(it) }?.takeIf { it.isNotEmpty() }
            if (normHandle != null) return "handle:$normHandle"
            val normName = ChannelIdentity.normalizeDisplayName(displayName).takeIf { it.isNotEmpty() }
            if (normName != null) return "name:$normName"
            return ""
        }

        fun fromIdentity(
            identity: ChannelIdentity,
            policy: ChannelPolicy,
            updatedAt: Long = System.currentTimeMillis(),
        ): YouTubeChannelEntity {
            return YouTubeChannelEntity(
                canonicalKey = identity.canonicalKey,
                channelHandle = identity.verifiedHandle,
                displayName = identity.displayName.trim().replace(Regex("\\s+"), " "),
                policy = policy,
                updatedAt = updatedAt,
            )
        }
    }
}
