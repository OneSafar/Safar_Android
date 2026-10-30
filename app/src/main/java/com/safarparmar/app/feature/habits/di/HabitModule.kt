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
            .client((retrofit.callFactory() as okhttp3.OkHttpClient).newBuilder()
                .callTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val operation = request.header("X-Habit-Operation") ?: java.util.UUID.randomUUID().toString()
                    val started = System.nanoTime()
                    try {
                        val response = chain.proceed(request.newBuilder().header("X-Habit-Operation", operation).build())
                        runCatching { android.util.Log.i("HabitSync", "http operation=${response.header("X-Habit-Operation") ?: operation} method=${request.method} status=${response.code} durationMs=${(System.nanoTime() - started) / 1_000_000} bytes=${response.body?.contentLength()}") }
                        response
                    } catch (e: java.io.IOException) {
                        runCatching { android.util.Log.w("HabitSync", "http operation=$operation failed durationMs=${(System.nanoTime() - started) / 1_000_000}") }
                        throw e
                    }
                }.build())
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
