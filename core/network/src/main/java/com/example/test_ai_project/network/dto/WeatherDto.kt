package com.example.test_ai_project.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenWeatherMap's `/data/2.5/weather` response — conditions at one point, right now.
 *
 * A small subset of what the provider sends. `ignoreUnknownKeys` drops the rest: `base`,
 * `cod`, the rain/snow volumes, the cloud percentage and the sea-level pressures are all
 * either redundant with what is modelled here or not on the design.
 */
@Serializable
data class CurrentWeatherResponseDto(
    /**
     * The provider always sends at least one entry, but the field is a list and a client that
     * assumes otherwise crashes on the day that changes. The mapper reads the first and
     * tolerates none.
     */
    val weather: List<WeatherDescriptionDto> = emptyList(),
    val main: WeatherMainDto,
    val wind: WeatherWindDto? = null,
    /**
     * Metres, capped at 10000 by the provider. Absent rather than zero when not reported,
     * which is why this is nullable and not defaulted.
     */
    val visibility: Int? = null,
    /** Seconds to add to UTC for the location's local clock. The provider sends no zone name. */
    val timezone: Int,
    /** The provider's own name for the point, e.g. "Reykjavík". Empty for some coordinates. */
    val name: String = "",
    val sys: WeatherSysDto? = null,
)

/**
 * One condition group.
 *
 * [id] is what the mapper reads, not [main]. The numeric codes are documented, grouped by
 * hundreds (2xx thunderstorm, 3xx drizzle, 5xx rain, 6xx snow, 7xx atmosphere, 800 clear,
 * 80x clouds), and stable. [main] is an English word that the provider is free to reword and
 * that would need re-matching if the request ever asked for another language.
 */
@Serializable
data class WeatherDescriptionDto(
    val id: Int,
    val main: String = "",
    /** Lower-case prose, e.g. "broken clouds". Localised by the request's `lang`. */
    val description: String = "",
    /**
     * e.g. `04n`. Only the trailing `d`/`n` is read — it is the provider's own verdict on
     * whether the sun is up at that point, which is cheaper and more reliable than deriving
     * it from a sunrise time and a clock.
     */
    val icon: String = "",
)

@Serializable
data class WeatherMainDto(
    val temp: Double,
    @SerialName("feels_like") val feelsLike: Double,
    /**
     * Not the day's range. On the current-conditions endpoint these describe the spread of
     * readings across the reporting area at this instant, and for most locations they simply
     * equal [temp]. Kept only as a last-resort fallback for when the forecast call is the one
     * that failed; the day's real high and low are derived from the forecast.
     */
    @SerialName("temp_min") val tempMin: Double? = null,
    @SerialName("temp_max") val tempMax: Double? = null,
    val humidity: Int = 0,
)

@Serializable
data class WeatherWindDto(
    /** Metres per second under `units=metric`. The screen converts to km/h. */
    val speed: Double = 0.0,
)

@Serializable
data class WeatherSysDto(
    /** ISO 3166 alpha-2, e.g. "IS". Joined to the city name for the location line. */
    val country: String? = null,
)

/**
 * OpenWeatherMap's `/data/2.5/forecast` response — five days in three-hour steps.
 *
 * This endpoint, rather than One Call 3.0, because it is on the free tier: One Call needs a
 * separate subscription and a card on file, and a build that cannot be run without one is
 * not a build this project can ship. The cost is resolution and reach — three-hour steps and
 * five days, so the daily list is five rows and not seven.
 */
@Serializable
data class ForecastResponseDto(
    val list: List<ForecastEntryDto> = emptyList(),
    val city: ForecastCityDto? = null,
)

@Serializable
data class ForecastEntryDto(
    /** Start of the three-hour step, in epoch *seconds* UTC. */
    val dt: Long,
    val main: WeatherMainDto,
    val weather: List<WeatherDescriptionDto> = emptyList(),
)

/**
 * The forecast's own view of the queried place.
 *
 * Carries the same two facts as the current-conditions response — a name and a UTC offset —
 * and the mapper prefers *this* offset, because it is the one the [ForecastEntryDto.dt]
 * values in the same payload have to be bucketed against.
 */
@Serializable
data class ForecastCityDto(
    val name: String = "",
    val country: String? = null,
    val timezone: Int? = null,
)
