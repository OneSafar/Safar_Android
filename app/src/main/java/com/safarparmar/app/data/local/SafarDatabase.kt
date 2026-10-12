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
