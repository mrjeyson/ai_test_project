package com.example.test_ai_project.network.api

import com.example.test_ai_project.network.di.OpenWeather
import com.example.test_ai_project.network.dto.CurrentWeatherResponseDto
import com.example.test_ai_project.network.dto.ForecastResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The OpenWeatherMap endpoints the app uses.
 *
 * Two calls rather than one, because the free tier has no combined endpoint: One Call 3.0
 * would answer current conditions, hourly and daily in a single request, and needs a separate
 * paid subscription. These two are on the free plan, and the repository issues them together
 * so the screen never sees one without the other.
 *
 * Authentication is not expressed here. The `appid` parameter is attached by a plugin on this
 * client, so no call site can forget it and no signature carries a credential.
 */
@Singleton
class OpenWeatherApi @Inject constructor(
    @OpenWeather private val client: HttpClient,
) {

    /** Conditions at one point, now. */
    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        language: String,
        units: String = UNITS_METRIC,
    ): CurrentWeatherResponseDto = client.get("data/2.5/weather") {
        parameter("lat", latitude)
        parameter("lon", longitude)
        parameter("units", units)
        parameter("lang", language)
    }.body()

    /**
     * Five days in three-hour steps — forty entries, ordered, starting at the step after now.
     *
     * `cnt` is deliberately not sent. The screen needs 24 hours of the hourly strip and every
     * day of the daily list, so the full window is what gets cached; trimming the request
     * would save a few kilobytes and cost the daily section its last row.
     */
    suspend fun getForecast(
        latitude: Double,
        longitude: Double,
        language: String,
        units: String = UNITS_METRIC,
    ): ForecastResponseDto = client.get("data/2.5/forecast") {
        parameter("lat", latitude)
        parameter("lon", longitude)
        parameter("units", units)
        parameter("lang", language)
    }.body()

    companion object {
        /**
         * Celsius and metres per second.
         *
         * Sent explicitly rather than omitted: the provider's default is Kelvin, which would
         * render as "271°" if the units parameter were ever dropped — wrong in a way that
         * looks like a data bug rather than a configuration one.
         */
        const val UNITS_METRIC = "metric"
    }
}
