package com.safarparmar.app.feature.habits.di

import android.content.Context
import androidx.room.Room
import com.safarparmar.app.feature.habits.data.HabitDatabase
import com.safarparmar.app.feature.habits.data.MIGRATION_2_3
import com.safarparmar.app.feature.habits.data.MIGRATION_3_4
import com.safarparmar.app.feature.habits.data.MIGRATION_4_6
import com.safarparmar.app.feature.habits.data.MIGRATION_5_6
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
    fun provideHabitApi(retrofit: retrofit2.Retrofit): com.safarparmar.app.feature.habits.data.HabitApi =
        retrofit.newBuilder()
            .apply { converterFactories().clear() }
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(
                com.google.gson.GsonBuilder().serializeNulls().create()))
            .build().create(com.safarparmar.app.feature.habits.data.HabitApi::class.java)


    @Provides
    @Singleton
    // Existing disk database is retained exclusively for the one-time backend import.
    fun provideHabitDatabase(@ApplicationContext context: Context): HabitDatabase =
        Room.databaseBuilder(context, HabitDatabase::class.java, "habits.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_6, MIGRATION_5_6)
            .build()

    @Provides
    @Singleton
    fun provideFeatureAccessManager(defaultManager: com.safarparmar.app.feature.habits.premium.DefaultFeatureAccessManager): com.safarparmar.app.feature.habits.premium.FeatureAccessManager = defaultManager
}
