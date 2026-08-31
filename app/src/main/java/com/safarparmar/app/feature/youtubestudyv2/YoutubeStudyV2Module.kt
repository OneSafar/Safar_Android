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
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YoutubeStudyV2Module {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): YoutubeStudyV2Database = Room
        .databaseBuilder(context, YoutubeStudyV2Database::class.java, YoutubeStudyV2Database.NAME)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

    @Provides
    @Singleton
    fun dao(database: YoutubeStudyV2Database): YoutubeStudyV2Dao = database.dao()

    @Provides
    @Singleton
    fun api(retrofit: Retrofit): YoutubeStudyV2Api = retrofit.create(YoutubeStudyV2Api::class.java)

    @Provides
    @Singleton
    fun repository(
        database: YoutubeStudyV2Database,
        dao: YoutubeStudyV2Dao,
        api: YoutubeStudyV2Api,
    ): YoutubeStudyV2Repository = YoutubeStudyV2Repository(database, dao, api)
}
