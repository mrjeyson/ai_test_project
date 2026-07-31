package com.example.test_ai_project.network.di

import com.example.test_ai_project.network.BuildConfig
import com.example.test_ai_project.network.plugin.OpenWeatherAuth
import com.example.test_ai_project.network.plugin.RedactingLogger
import com.example.test_ai_project.network.plugin.TmdbAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import kotlinx.serialization.json.Json

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
     * One engine, shared by all three clients below.
     *
     * `HttpClient(engine)` borrows an engine rather than owning one, which is what makes the
     * sharing safe: three `HttpClient(OkHttp)` calls would each spin up their own dispatcher,
     * connection pool and thread set, for three hosts this app talks to a handful of times a
     * session. Sharing one is also what the OkHttp version of this module did, for that reason.
     *
     * Nothing closes it. These are process-lifetime singletons and a closed engine cannot be
     * reopened, so the only thing an explicit shutdown could achieve is a dead HTTP stack in a
     * still-running app.
     */
    @Provides
    @Singleton
    fun providesHttpClientEngine(): HttpClientEngine = OkHttp.create()

    /**
     * The log sink, wrapped so URL credentials never reach it — see [RedactingLogger]. That
     * wrapping is the entire reason this is a binding and not a literal inside [httpClient].
     */
    @Provides
    @Singleton
    fun providesHttpLogger(): Logger = RedactingLogger()

    /**
     * A client of its own, not just a second base URL: the auth plugin has to run for TMDB and
     * must never run for anything else. Everything the three share is in [httpClient], so what
     * is written here is only ever what differs.
     */
    @Provides
    @Singleton
    @Tmdb
    fun providesTmdbHttpClient(
        engine: HttpClientEngine,
        json: Json,
        logger: Logger,
    ): HttpClient = httpClient(engine, json, logger, TMDB_BASE_URL) {
        install(TmdbAuth) {
            accessToken = BuildConfig.TMDB_ACCESS_TOKEN
            apiKey = BuildConfig.TMDB_API_KEY
        }
    }

    /**
     * The one client with no auth plugin at all.
     *
     * The contrast with the other two is the point: Aladhan needs no credential, so nothing is
     * installed here that could attach one. Only the base URL differs from a bare client.
     */
    @Provides
    @Singleton
    @Aladhan
    fun providesAladhanHttpClient(
        engine: HttpClientEngine,
        json: Json,
        logger: Logger,
    ): HttpClient = httpClient(engine, json, logger, ALADHAN_BASE_URL)

    @Provides
    @Singleton
    @OpenWeather
    fun providesOpenWeatherHttpClient(
        engine: HttpClientEngine,
        json: Json,
        logger: Logger,
    ): HttpClient = httpClient(engine, json, logger, OPEN_WEATHER_BASE_URL) {
        install(OpenWeatherAuth) {
            apiKey = BuildConfig.OPENWEATHER_API_KEY
        }
    }

    /**
     * Everything the three clients have in common.
     *
     * `expectSuccess` is the line worth pausing on. Ktor hands a non-2xx response back as an
     * ordinary value where Retrofit threw — and quietly parsing an error page as though it were
     * a movie list is precisely the failure this app cannot have. Turning it on restores the
     * throw, as `ResponseException`, which is what `safeApiCall` and the feature-level 401
     * handling are written against.
     *
     * [baseUrl] must end in `/`. Ktor concatenates a relative request path onto the base's own
     * segments after dropping its last one, so `…/3` without the slash plus `movie/popular`
     * would resolve to `/movie/popular` and lose the API version.
     *
     * `internal` rather than private so the tests can build a client through this exact function
     * with a `MockEngine` in place of the engine. A test that re-declared the configuration would
     * only prove its own copy right, and the thing most worth pinning down here — how a relative
     * path merges onto [baseUrl] — lives in the configuration and nowhere else.
     */
    internal fun httpClient(
        engine: HttpClientEngine,
        json: Json,
        logger: Logger,
        baseUrl: String,
        configure: HttpClientConfig<*>.() -> Unit = {},
    ): HttpClient = HttpClient(engine) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            this.logger = logger
            level = if (BuildConfig.LOG_HTTP_BODIES) LogLevel.ALL else LogLevel.NONE
            // The bearer token, on both legs. The credentials that travel as query parameters
            // are scrubbed by the logger itself, where no install order can betray them.
            sanitizeHeader { header -> header.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }

        defaultRequest {
            url(baseUrl)
        }

        configure()
    }

    // `internal`, for the same reason as [httpClient]: the endpoint tests assert the full URL a
    // call resolves to, and they have to start from the base URL the app actually ships.
    internal const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    internal const val ALADHAN_BASE_URL = "https://api.aladhan.com/"
    internal const val OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/"
}
