package com.safarparmar.app.feature.youtubestudyv2

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YoutubeStudyV2Module {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): YoutubeStudyV2Database = Room
        .databaseBuilder(context, YoutubeStudyV2Database::class.java, YoutubeStudyV2Database.NAME)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
        .fallbackToDestructiveMigration()
        .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS youtube_v2_visit (
                    visitKey TEXT NOT NULL PRIMARY KEY,
                    channelId TEXT,
                    exactHandle TEXT,
                    displayName TEXT NOT NULL,
                    visitCount INTEGER NOT NULL,
                    lastVisitedAtMs INTEGER NOT NULL,
                    lastLookupAtMs INTEGER NOT NULL,
                    status TEXT NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS youtube_v2_visit")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE youtube_v2_identity ADD COLUMN categories TEXT NOT NULL DEFAULT 'education'")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 4 guessed that every legacy identity was educational.
            // Invalidate the old guessed education categories.
            db.execSQL("UPDATE youtube_v2_identity SET categories = 'unclassified'")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE youtube_v2_identity_new (
                    channelId TEXT NOT NULL PRIMARY KEY,
                    handle TEXT NOT NULL,
                    displayName TEXT NOT NULL,
                    thumbnailUrl TEXT,
                    resolvedAtMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO youtube_v2_identity_new
                    (channelId, handle, displayName, thumbnailUrl, resolvedAtMs)
                SELECT channelId, handle, displayName, thumbnailUrl, resolvedAtMs
                FROM youtube_v2_identity
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE youtube_v2_identity")
            db.execSQL("ALTER TABLE youtube_v2_identity_new RENAME TO youtube_v2_identity")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS youtube_v2_classification (
                    channelId TEXT NOT NULL PRIMARY KEY,
                    classification TEXT NOT NULL,
                    updatedAtMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT OR REPLACE INTO youtube_v2_classification (channelId, classification, updatedAtMs)
                SELECT channelId, 'productive', addedAtMs FROM youtube_v2_allowlist
                """.trimIndent(),
            )
        }
    }

    internal val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Preserve legacy category rows for manual recovery; unverified IDs
            // cannot be evaluated or reused as identities by version 8.
            db.execSQL("DELETE FROM youtube_v2_alias WHERE channelId LIKE 'handle:%' OR channelId LIKE 'display:%' OR channelId LIKE 'ephemeral:%'")
            db.execSQL("DELETE FROM youtube_v2_identity WHERE channelId LIKE 'handle:%' OR channelId LIKE 'display:%' OR channelId LIKE 'ephemeral:%'")
            db.execSQL("DELETE FROM youtube_v2_allowlist WHERE channelId LIKE 'handle:%' OR channelId LIKE 'display:%' OR channelId LIKE 'ephemeral:%'")
        }
    }

    @Provides
    @Singleton
    fun dao(database: YoutubeStudyV2Database): YoutubeStudyV2Dao = database.dao()

    @Provides
    @Singleton
    fun repository(
        database: YoutubeStudyV2Database,
        dao: YoutubeStudyV2Dao,
    ): YoutubeStudyV2Repository = YoutubeStudyV2Repository(database, dao)
}
