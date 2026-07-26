package com.example.test_ai_project.core.network.di

import com.example.test_ai_project.core.network.BuildConfig
import com.example.test_ai_project.core.network.api.ItemApi
import com.example.test_ai_project.core.network.api.TmdbApi
import com.example.test_ai_project.core.network.interceptor.TmdbAuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun providesJson(): Json = Json {
        // Tolerate fields the app does not model yet, so a backend addition
        // cannot crash a shipped client.
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * Shared by both clients below.
     *
     * `redactHeader` is not decoration: the level is BODY in debug builds, and without it
     * the TMDB bearer token would be written to logcat on every request.
     */
    @Provides
    @Singleton
    fun providesHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.LOG_HTTP_BODIES) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    fun providesRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesItemApi(retrofit: Retrofit): ItemApi = retrofit.create(ItemApi::class.java)

    /**
     * A separate client, not just a second base URL: the auth interceptor has to run for
     * TMDB and must never run for anything else.
     *
     * Logging is installed *before* auth, and the order is load-bearing. Application
     * interceptors log the request as it reaches them, so running logging first means it
     * never sees the credential at all — neither the bearer header nor, more importantly,
     * the v3 `api_key` query parameter, which `redactHeader` cannot hide because it is
     * part of the URL. The response is still logged in full either way.
     */
    @Provides
    @Singleton
    @Tmdb
    fun providesTmdbOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(
            TmdbAuthInterceptor(
                accessToken = BuildConfig.TMDB_ACCESS_TOKEN,
                apiKey = BuildConfig.TMDB_API_KEY,
            ),
        )
        .build()

    @Provides
    @Singleton
    @Tmdb
    fun providesTmdbRetrofit(
        @Tmdb okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(TMDB_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesTmdbApi(@Tmdb retrofit: Retrofit): TmdbApi = retrofit.create(TmdbApi::class.java)

    // Placeholder endpoint — swap for the real base URL when the backend exists.
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val JSON_MEDIA_TYPE = "application/json"
}
