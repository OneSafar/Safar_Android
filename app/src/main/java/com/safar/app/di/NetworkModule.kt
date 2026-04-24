package com.safar.app.di

import com.safar.app.BuildConfig
import com.safar.app.data.remote.api.*
import com.safar.app.data.remote.socket.MehfilSocketManager
import com.safar.app.util.AuthInterceptor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun provideCookieManager(): CookieManager =
        CookieManager().also { it.setCookiePolicy(CookiePolicy.ACCEPT_ALL) }

    @Singleton
    @Provides
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, cookieManager: CookieManager): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Singleton
    @Provides
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton fun provideAuthApi(r: Retrofit): AuthApi = r.create(AuthApi::class.java)
    @Provides @Singleton fun provideHomeApi(r: Retrofit): HomeApi = r.create(HomeApi::class.java)
    @Provides @Singleton fun provideNishthaApi(r: Retrofit): NishthaApi = r.create(NishthaApi::class.java)
    @Provides @Singleton fun provideJournalApi(r: Retrofit): JournalApi = r.create(JournalApi::class.java)
    @Provides @Singleton fun provideFocusApi(r: Retrofit): FocusApi = r.create(FocusApi::class.java)
    @Provides @Singleton fun provideMehfilApi(r: Retrofit): MehfilApi = r.create(MehfilApi::class.java)
    @Provides @Singleton fun provideThoughtsApi(r: Retrofit): ThoughtsApi = r.create(ThoughtsApi::class.java)
    @Provides @Singleton fun provideGson(): Gson = Gson()
    @Provides @Singleton fun provideMehfilSocketManager(gson: Gson): MehfilSocketManager = MehfilSocketManager(gson)
}
