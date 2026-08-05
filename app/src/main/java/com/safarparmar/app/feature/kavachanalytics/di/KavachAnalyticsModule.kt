package com.safarparmar.app.feature.kavachanalytics.di

import android.content.Context
import androidx.room.Room
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachAnalyticsDao
import com.safarparmar.app.feature.kavachanalytics.data.local.KavachAnalyticsDatabase
import com.safarparmar.app.feature.kavachanalytics.data.remote.KavachAnalyticsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KavachAnalyticsModule {

    @Provides
    @Singleton
    fun provideKavachAnalyticsDatabase(
        @ApplicationContext context: Context,
    ): KavachAnalyticsDatabase = Room
        .databaseBuilder(context, KavachAnalyticsDatabase::class.java, KavachAnalyticsDatabase.NAME)
        // Analytics is a derived, re-collectable view of on-device usage, never the
        // source of truth for a student's study record. Rebuilding it beats failing
        // to open the app on a schema change.
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideKavachAnalyticsDao(db: KavachAnalyticsDatabase): KavachAnalyticsDao = db.dao()

    @Provides
    @Singleton
    fun provideKavachAnalyticsApi(retrofit: Retrofit): KavachAnalyticsApi =
        retrofit.create(KavachAnalyticsApi::class.java)
}
