package com.safarparmar.app.feature.habits.di

import android.content.Context
import androidx.room.Room
import com.safarparmar.app.feature.habits.data.HabitDao
import com.safarparmar.app.feature.habits.data.HabitDatabase
import com.safarparmar.app.feature.habits.data.MIGRATION_2_3
import com.safarparmar.app.feature.habits.data.MIGRATION_3_4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HabitModule {

    @Provides
    @Singleton
    fun provideHabitDatabase(@ApplicationContext context: Context): HabitDatabase =
        Room.databaseBuilder(context, HabitDatabase::class.java, "habits.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao = database.habitDao()
}

