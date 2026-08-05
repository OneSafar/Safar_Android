package com.safarparmar.app.feature.kavachanalytics.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppClassificationEntity::class,
        RawUsageIntervalEntity::class,
        KavachSessionEntity::class,
        KavachEventEntity::class,
        DailyAppAggregateEntity::class,
        DayCoverageEntity::class,
        KavachMetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class KavachAnalyticsDatabase : RoomDatabase() {
    abstract fun dao(): KavachAnalyticsDao

    companion object {
        const val NAME = "kavach_analytics.db"
    }
}
