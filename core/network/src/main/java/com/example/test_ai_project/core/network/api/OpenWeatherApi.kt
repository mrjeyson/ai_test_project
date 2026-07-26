package com.example.test_ai_project.core.network.api

import com.example.test_ai_project.core.network.dto.CurrentWeatherResponseDto
import com.example.test_ai_project.core.network.dto.ForecastResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The OpenWeatherMap endpoints the app uses.
 *
 * Two calls rather than one, because the free tier has no combined endpoint: One Call 3.0
 * would answer current conditions, hourly and daily in a single request, and needs a separate
 * paid subscription. These two are on the free plan, and the repository issues them together
 * so the screen never sees one without the other.
 *
 * Authentication is not expressed here. The `appid` parameter is attached by an interceptor
 * on this client, so no call site can forget it and no signature carries a credential.
 */
interface OpenWeatherApi {

    /** Conditions at one point, now. */
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = UNITS_METRIC,
        @Query("lang") language: String,
    ): CurrentWeatherResponseDto

    /**
     * Five days in three-hour steps — forty entries, ordered, starting at the step after now.
     *
     * `cnt` is deliberately not sent. The screen needs 24 hours of the hourly strip and every
     * day of the daily list, so the full window is what gets cached; trimming the request
     * would save a few kilobytes and cost the daily section its last row.
     */
    @GET("data/2.5/forecast")
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = UNITS_METRIC,
        @Query("lang") language: String,
    ): ForecastResponseDto

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
