package com.example.test_ai_project.core.data.repository

import android.location.Location
import com.example.test_ai_project.core.common.dispatcher.AppDispatcher
import com.example.test_ai_project.core.common.dispatcher.Dispatcher
import com.example.test_ai_project.core.data.location.GeocoderPlaceNameSource
import com.example.test_ai_project.core.data.mapper.mapWeather
import com.example.test_ai_project.core.data.mapper.toDomain
import com.example.test_ai_project.core.database.dao.WeatherDao
import com.example.test_ai_project.core.database.entity.CurrentWeatherEntity
import com.example.test_ai_project.core.domain.exception.WeatherKeyRejectedException
import com.example.test_ai_project.core.domain.exception.WeatherServiceNotConfiguredException
import com.example.test_ai_project.core.domain.repository.WeatherRepository
import com.example.test_ai_project.core.domain.time.TimeProvider
import com.example.test_ai_project.core.model.UserLocation
import com.example.test_ai_project.core.model.WeatherSnapshot
import com.example.test_ai_project.core.network.api.OpenWeatherApi
import com.example.test_ai_project.core.network.dto.CurrentWeatherResponseDto
import com.example.test_ai_project.core.network.dto.ForecastResponseDto
import com.example.test_ai_project.core.network.interceptor.OpenWeatherNotConfiguredException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Offline-first, in the same strict sense as [MovieRepositoryImpl]: Room is the only thing the
 * UI ever reads from, and the network is a process that writes into Room. There is no code path
 * where a failed request produces an empty screen.
 *
 * What differs from the other caches here is the *meaning* of a stale row. A cached movie list
 * or prayer timetable stays correct; a cached temperature only stays the last one known. So the
 * freshness window is minutes rather than hours, and the snapshot carries the timestamp the
 * screen needs to say how old it is.
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherDao: WeatherDao,
    private val openWeatherApi: OpenWeatherApi,
    private val placeNameSource: GeocoderPlaceNameSource,
    private val timeProvider: TimeProvider,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : WeatherRepository {

    override fun observeWeather(): Flow<WeatherSnapshot?> =
        weatherDao.observeSnapshot().map { entity -> entity?.toDomain() }

    override suspend fun refresh(location: UserLocation, force: Boolean) =
        withContext(ioDispatcher) {
            if (!force && isFresh(location)) return@withContext

            // Concurrently, and both awaited before anything is written. The two calls are
            // independent, so running them in series would double the time the user waits on a
            // slow connection for no benefit — and `coroutineScope` means a failure in either
            // cancels the other and propagates, which is exactly the required behaviour: half a
            // snapshot must never reach the cache.
            val (current, forecast) = coroutineScope {
                val currentDeferred = async { fetchCurrent(location) }
                val forecastDeferred = async { fetchForecast(location) }
                currentDeferred.await() to forecastDeferred.await()
            }

            val mapped = mapWeather(
                current = current,
                forecast = forecast,
                location = location,
                placeName = resolvePlaceName(current, forecast, location),
                fetchedAtEpochMillis = timeProvider.nowEpochMillis(),
            )

            weatherDao.replaceSnapshot(
                current = mapped.current,
                hourly = mapped.hourly,
                daily = mapped.daily,
            )
        }

    /**
     * The provider's own name for the place, e.g. "Reykjavík, IS".
     *
     * Preferred over a reverse geocode because it arrives in a response already being made, and
     * because it is the name the readings are attributed to. The [GeocoderPlaceNameSource]
     * fallback covers the coordinates the provider names as an empty string — open water, and
     * points its city list does not reach.
     *
     * Best-effort throughout: a null name is a complete snapshot with coordinates on screen
     * instead of a label, which is a far better outcome than failing a refresh over a caption.
     */
    private suspend fun resolvePlaceName(
        current: CurrentWeatherResponseDto,
        forecast: ForecastResponseDto,
        location: UserLocation,
    ): String? {
        val name = current.name.ifBlank { forecast.city?.name.orEmpty() }
        if (name.isBlank()) {
            return placeNameSource.placeName(location.latitude, location.longitude)
        }

        val country = current.sys?.country ?: forecast.city?.country
        return if (country.isNullOrBlank()) name else "$name, $country"
    }

    private suspend fun fetchCurrent(location: UserLocation): CurrentWeatherResponseDto =
        translatingConfigErrors {
            openWeatherApi.getCurrentWeather(
                latitude = location.latitude,
                longitude = location.longitude,
                language = requestLanguage(),
            )
        }

    private suspend fun fetchForecast(location: UserLocation): ForecastResponseDto =
        translatingConfigErrors {
            openWeatherApi.getForecast(
                latitude = location.latitude,
                longitude = location.longitude,
                language = requestLanguage(),
            )
        }

    /**
     * Translated at the layer boundary. This module is the only one that knows the provider is
     * OpenWeatherMap, so it is the only one that can turn the provider's exceptions into
     * something the domain — and the screen above it — can act on.
     *
     * The 401 is worth singling out. Retrofit raises [HttpException] for any non-2xx, which is a
     * `RuntimeException` and so lands in the UI's catch-all "the service returned an error" —
     * wording that sends the reader to check their connection when the answer is their key. A
     * rejected key is the single most likely first-run failure here, because OpenWeatherMap issues
     * keys inactive and takes a while to switch them on.
     */
    private inline fun <T> translatingConfigErrors(request: () -> T): T =
        try {
            request()
        } catch (notConfigured: OpenWeatherNotConfiguredException) {
            throw WeatherServiceNotConfiguredException(
                message = notConfigured.message.orEmpty(),
                cause = notConfigured,
            )
        } catch (http: HttpException) {
            if (http.code() != HTTP_UNAUTHORIZED) throw http
            throw WeatherKeyRejectedException(
                message = "OpenWeatherMap rejected the API key in this build.",
                cause = http,
            )
        }

    /**
     * The language the provider should describe conditions in.
     *
     * Only the description text is affected — every code and number in the response is
     * language-independent, which is why the mapper reads condition *ids* and not the English
     * `main` field. Read from the process default rather than injected: it is the device
     * setting, and a cache written in one language is simply rewritten on the next refresh
     * after the user changes it.
     */
    private fun requestLanguage(): String = Locale.getDefault().language

    /**
     * Whether the cache can answer without a request.
     *
     * Two conditions, and both have to hold. Age is the one that matters most often —
     * conditions move within the hour, and the provider updates no faster than [MAX_AGE]
     * anyway, so a request inside that window would spend a call from the free quota to be
     * told the same thing.
     *
     * Distance matters because the readings belong to a place. The threshold is tighter than the
     * prayer cache's ten kilometres: prayer times shift by seconds over that distance, whereas
     * crossing five kilometres in coastal or mountain terrain genuinely changes the weather —
     * and unlike a prayer time, being wrong here is visible out of the window.
     */
    private suspend fun isFresh(location: UserLocation): Boolean {
        val cached = weatherDao.current() ?: return false
        val age = timeProvider.nowEpochMillis() - cached.fetchedAtEpochMillis
        // A negative age means the clock moved backwards since the write — a manual change or a
        // time-zone-less device correcting itself. Treated as stale rather than as freshness
        // reaching into the future.
        if (age !in 0..MAX_AGE.inWholeMilliseconds) return false
        return cached.distanceTo(location) < RELOCATION_THRESHOLD_METERS
    }

    private fun CurrentWeatherEntity.distanceTo(location: UserLocation): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            latitude,
            longitude,
            location.latitude,
            location.longitude,
            results,
        )
        return results[0]
    }

    private companion object {
        /**
         * The provider refreshes its own model roughly every ten minutes, so anything shorter
         * buys nothing but quota. Fifteen leaves headroom without letting the screen show a
         * reading old enough to contradict the sky.
         */
        val MAX_AGE = 15.minutes

        const val RELOCATION_THRESHOLD_METERS = 5_000f

        /** The provider's answer for both an absent key and one it has not activated yet. */
        const val HTTP_UNAUTHORIZED = 401
    }
}
