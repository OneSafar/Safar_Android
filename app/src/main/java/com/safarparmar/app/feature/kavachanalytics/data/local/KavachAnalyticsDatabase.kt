package com.safarparmar.app.feature.kavachanalytics.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppClassificationEntity::class,
        RawUsageIntervalEntity::class,
        KavachSessionEntity::class,
        KavachEventEntity::class,
        DailyAppAggregateEntity::class,
        DayCoverageEntity::class,
        KavachMetaEntity::class,
        ProtectionWindowEntity::class,
        YoutubeChannelEntity::class,
        YoutubeViewingIntervalEntity::class,
        YoutubeOpenIntervalEntity::class,
        YoutubeDailyAggregateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class KavachAnalyticsDatabase : RoomDatabase() {
    abstract fun dao(): KavachAnalyticsDao

    companion object {
        const val NAME = "kavach_analytics.db"

        /**
         * Adds protection windows. Written as a real migration rather than letting
         * the destructive fallback run: analytics is re-collectable in principle,
         * but only for the few days the OS still holds — anything older would be
         * gone for good, and a student would watch their history vanish.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `kavach_protection_window` (
                        `id` TEXT NOT NULL,
                        `startMs` INTEGER NOT NULL,
                        `endMs` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `localDate` TEXT NOT NULL,
                        `isOpen` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kavach_protection_window_startMs` ON `kavach_protection_window` (`startMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kavach_protection_window_localDate` ON `kavach_protection_window` (`localDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kavach_protection_window_source` ON `kavach_protection_window` (`source`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `youtube_channel` (`channelKey` TEXT NOT NULL, `displayName` TEXT NOT NULL, `isProductive` INTEGER NOT NULL, `lastSeenAtMs` INTEGER NOT NULL, PRIMARY KEY(`channelKey`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_youtube_channel_lastSeenAtMs` ON `youtube_channel` (`lastSeenAtMs`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `youtube_viewing_interval` (`id` TEXT NOT NULL, `startMs` INTEGER NOT NULL, `endMs` INTEGER NOT NULL, `localDate` TEXT NOT NULL, `channelKey` TEXT, `category` TEXT NOT NULL, `isShorts` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_youtube_viewing_interval_localDate` ON `youtube_viewing_interval` (`localDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_youtube_viewing_interval_startMs` ON `youtube_viewing_interval` (`startMs`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `youtube_open_interval` (`id` INTEGER NOT NULL, `startedAtMs` INTEGER NOT NULL, `heartbeatAtMs` INTEGER NOT NULL, `channelKey` TEXT, `category` TEXT NOT NULL, `isShorts` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `youtube_daily_aggregate` (`localDate` TEXT NOT NULL, `productiveSeconds` INTEGER NOT NULL, `distractingSeconds` INTEGER NOT NULL, `shortsSeconds` INTEGER NOT NULL, `unidentifiedSeconds` INTEGER NOT NULL, `protectedProductiveSeconds` INTEGER NOT NULL, `protectedDistractingSeconds` INTEGER NOT NULL, `protectedShortsSeconds` INTEGER NOT NULL, `protectedUnidentifiedSeconds` INTEGER NOT NULL, `coverage` TEXT NOT NULL, `updatedAtMs` INTEGER NOT NULL, `synced` INTEGER NOT NULL, PRIMARY KEY(`localDate`))")
            }
        }
    }
}
