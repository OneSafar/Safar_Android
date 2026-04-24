package com.safar.app.di

import com.safar.app.data.repository.*
import com.safar.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
    @Binds @Singleton abstract fun bindNishthaRepository(impl: NishthaRepositoryImpl): NishthaRepository
    @Binds @Singleton abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository
    @Binds @Singleton abstract fun bindEkagraRepository(impl: EkagraRepositoryImpl): EkagraRepository
    @Binds @Singleton abstract fun bindMehfilRepository(impl: MehfilRepositoryImpl): MehfilRepository
}
