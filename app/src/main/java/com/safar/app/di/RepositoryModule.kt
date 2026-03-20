package com.safar.app.di

import com.safar.app.data.repository.*
import com.safar.app.domain.repository.*
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {
    @Provides @Singleton
    fun provideGson(): Gson = Gson()
}
