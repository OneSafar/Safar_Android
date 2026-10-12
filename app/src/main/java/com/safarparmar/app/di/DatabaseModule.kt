package com.safarparmar.app.di

import android.content.Context
import androidx.room.Room
import com.safarparmar.app.data.local.SafarDatabase
import com.safarparmar.app.data.local.dao.YouTubeChannelDao
import com.safarparmar.app.data.repository.YouTubeChannelRepositoryImpl
import com.safarparmar.app.domain.repository.YouTubeChannelRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSafarDatabase(
        @ApplicationContext context: Context,
    ): SafarDatabase {
        return Room.databaseBuilder(
            context,
            SafarDatabase::class.java,
            SafarDatabase.DATABASE_NAME,
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideYouTubeChannelDao(database: SafarDatabase): YouTubeChannelDao {
        return database.youTubeChannelDao()
    }

    @Provides
    @Singleton
    fun provideYouTubeChannelRepository(
        dao: YouTubeChannelDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): YouTubeChannelRepository {
        return YouTubeChannelRepositoryImpl(dao, ioDispatcher)
    }
}
