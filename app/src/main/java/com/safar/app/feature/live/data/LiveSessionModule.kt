package com.safar.app.feature.live.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LiveSessionModule {
    @Binds
    @Singleton
    abstract fun bindLiveSessionRepository(
        impl: LiveSessionRepository
    ): LiveSessionRepositoryContract
}
