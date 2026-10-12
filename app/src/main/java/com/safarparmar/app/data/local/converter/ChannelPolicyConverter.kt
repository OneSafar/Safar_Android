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
