package com.example.test_ai_project.network.di

import com.example.test_ai_project.network.BuildConfig
import com.example.test_ai_project.network.api.AladhanApi
import com.example.test_ai_project.network.api.OpenWeatherApi
import com.example.test_ai_project.network.api.TmdbApi
import com.example.test_ai_project.network.interceptor.OpenWeatherAuthInterceptor
import com.example.test_ai_project.network.interceptor.TmdbAuthInterceptor
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
     * Shared by every client below.
     *
     * Neither redaction is decoration. The level is BODY in debug builds, so without
     * `redactHeader` the TMDB bearer token would be written to logcat on every request.
     *
     * `redactQueryParams` covers the two credentials that travel in the URL instead —
     * OpenWeatherMap's `appid` and TMDB's v3 `api_key`. Installing this interceptor *before*
     * the auth interceptors keeps them out of the request line, because an application
     * interceptor logs the request as it arrives and the key is attached after it. That is not
     * enough on its own, and the gap is easy to miss: the response is logged from
     * `response.request.url`, which is the URL that actually went out — with the credential on
     * it. Ordering hides the key going out; only redaction hides it coming back.
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
            redactQueryParams("appid", "api_key")
        }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * A separate client, not just a second base URL: the auth interceptor has to run for
     * TMDB and must never run for anything else.
     *
     * Logging is installed *before* auth, and the order is load-bearing: application
     * interceptors log the request as it reaches them, so logging never sees the credential on
     * the way out. It does see it on the way back, though — the response is logged from the
     * URL that actually went out — so the v3 `api_key` is redacted by name on the shared
     * interceptor rather than left to ordering.
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

    /**
     * A third base URL, on the *unauthenticated* client.
     *
     * The contrast with the TMDB instance above is the point: Aladhan needs no credential,
     * so it takes the plain client and never comes near the auth interceptor. Only the base
     * URL differs from the default Retrofit, which is why this shares everything else.
     */
    @Provides
    @Singleton
    @Aladhan
    fun providesAladhanRetrofit(
        okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(ALADHAN_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesAladhanApi(@Aladhan retrofit: Retrofit): AladhanApi =
        retrofit.create(AladhanApi::class.java)

    @Provides
    @Singleton
    @OpenWeather
    fun providesOpenWeatherOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(OpenWeatherAuthInterceptor(apiKey = BuildConfig.OPENWEATHER_API_KEY))
        .build()

    @Provides
    @Singleton
    @OpenWeather
    fun providesOpenWeatherRetrofit(
        @OpenWeather okHttpClient: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(OPEN_WEATHER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providesOpenWeatherApi(@OpenWeather retrofit: Retrofit): OpenWeatherApi =
        retrofit.create(OpenWeatherApi::class.java)

    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val ALADHAN_BASE_URL = "https://api.aladhan.com/"
    private const val OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/"
    private const val JSON_MEDIA_TYPE = "application/json"
}
