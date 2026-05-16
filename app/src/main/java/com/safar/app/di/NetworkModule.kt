package com.safar.app.di

import android.content.Context
import com.safar.app.BuildConfig
import com.safar.app.data.remote.api.*
import com.safar.app.data.remote.socket.MehfilSocketManager
import com.safar.app.util.AuthInterceptor
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
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

    // 5 MB shared HTTP cache for safe-to-cache GETs. Endpoints opt in by
    // declaring `@Headers("X-Cache-Max-Age: <seconds>")` on their Retrofit
    // method; a network interceptor below strips that marker before the
    // request leaves the device and rewrites the response with a matching
    // `Cache-Control: public, max-age=<seconds>` so OkHttp's on-disk cache
    // can serve repeats. Endpoints without the marker fall through
    // unchanged, so this is strictly additive — no audited endpoint can
    // start serving stale data unintentionally.
    @Singleton
    @Provides
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, HTTP_CACHE_SIZE_BYTES)
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        cookieManager: CookieManager,
        cache: Cache,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .cache(cache)
            .addInterceptor(authInterceptor)
            // Per-call timeout override: any request that sets the X-Timeout-Seconds header
            // (stripped before sending) gets its connect/read/write timeouts bumped to that
            // value. Used for slow AI calls such as the Study Planner syllabus import, where
            // the Railway-hosted agent + Groq inference can take well over 30s on a cold start.
            .addInterceptor okhttp@{ chain ->
                val original = chain.request()
                val timeoutHeader = original.header(TIMEOUT_HEADER)
                if (timeoutHeader.isNullOrBlank()) {
                    return@okhttp chain.proceed(original)
                }
                val seconds = timeoutHeader.toIntOrNull()?.coerceIn(1, 600)
                    ?: return@okhttp chain.proceed(original.newBuilder().removeHeader(TIMEOUT_HEADER).build())
                val stripped = original.newBuilder().removeHeader(TIMEOUT_HEADER).build()
                chain.withConnectTimeout(seconds, TimeUnit.SECONDS)
                    .withReadTimeout(seconds, TimeUnit.SECONDS)
                    .withWriteTimeout(seconds, TimeUnit.SECONDS)
                    .proceed(stripped)
            }
            // Cache-rewrite network interceptor: when a request carries the
            // X-Cache-Max-Age marker, strip it before hitting the wire and
            // rewrite the response Cache-Control so OkHttp's cache will
            // store and reuse it for the requested TTL. Runs as a network
            // interceptor (not application) so the cache layer above sees
            // the rewritten headers.
            .addNetworkInterceptor cache@{ chain ->
                val original = chain.request()
                val maxAge = original.header(CACHE_MAX_AGE_HEADER)?.toIntOrNull()?.coerceIn(1, 86_400)
                if (maxAge == null) {
                    return@cache chain.proceed(original)
                }
                val stripped = original.newBuilder().removeHeader(CACHE_MAX_AGE_HEADER).build()
                val response = chain.proceed(stripped)
                if (!response.isSuccessful) return@cache response
                response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, max-age=$maxAge")
                    .build()
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private const val TIMEOUT_HEADER = "X-Timeout-Seconds"
    private const val CACHE_MAX_AGE_HEADER = "X-Cache-Max-Age"
    private const val HTTP_CACHE_SIZE_BYTES = 5L * 1024L * 1024L

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
    @Provides @Singleton fun provideNotificationApi(r: Retrofit): NotificationApi = r.create(NotificationApi::class.java)
    @Provides @Singleton fun providePlannerApi(r: Retrofit): PlannerApi = r.create(PlannerApi::class.java)
    @Provides @Singleton fun provideGson(): Gson = Gson()
    @Provides @Singleton fun provideMehfilSocketManager(gson: Gson): MehfilSocketManager = MehfilSocketManager(gson)
}
