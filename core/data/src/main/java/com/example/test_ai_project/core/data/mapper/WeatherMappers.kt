package com.example.test_ai_project.core.data.mapper

import com.example.test_ai_project.core.database.entity.CurrentWeatherEntity
import com.example.test_ai_project.core.database.entity.DailyForecastEntity
import com.example.test_ai_project.core.database.entity.HourlyForecastEntity
import com.example.test_ai_project.core.database.entity.WeatherSnapshotEntity
import com.example.test_ai_project.core.model.CalendarDate
import com.example.test_ai_project.core.model.CurrentWeather
import com.example.test_ai_project.core.model.DailyForecast
import com.example.test_ai_project.core.model.HourlyForecast
import com.example.test_ai_project.core.model.UserLocation
import com.example.test_ai_project.core.model.WeatherCondition
import com.example.test_ai_project.core.model.WeatherSnapshot
import com.example.test_ai_project.core.network.dto.CurrentWeatherResponseDto
import com.example.test_ai_project.core.network.dto.ForecastEntryDto
import com.example.test_ai_project.core.network.dto.ForecastResponseDto
import com.example.test_ai_project.core.network.dto.WeatherDescriptionDto
import kotlin.math.abs

/** The three tables one fetch produces, kept together so they can only be written together. */
internal class MappedWeather(
    val current: CurrentWeatherEntity,
    val hourly: List<HourlyForecastEntity>,
    val daily: List<DailyForecastEntity>,
)

/**
 * Turns one pair of OpenWeatherMap responses into the rows the Weather tab reads.
 *
 * Both responses, not one each: the day's high and low cannot be derived from current
 * conditions alone (see [CurrentWeatherResponseDto.main]) and the forecast has no
 * "now" — so neither half is mappable on its own, and pretending otherwise would put the
 * seam in the wrong place.
 *
 * This is also where the provider's units and codes stop travelling. Downstream sees a
 * [WeatherCondition], Celsius, and epoch millis; nothing above this file knows that 802 means
 * scattered clouds or that `dt` is in seconds.
 */
internal fun mapWeather(
    current: CurrentWeatherResponseDto,
    forecast: ForecastResponseDto,
    location: UserLocation,
    placeName: String?,
    fetchedAtEpochMillis: Long,
): MappedWeather {
    // The forecast's offset wins when both are present: it is the one the `dt` values in the
    // same payload have to be bucketed against, and a mismatch would put a step in the wrong
    // day.
    val offsetSeconds = forecast.city?.timezone ?: current.timezone

    val buckets = forecast.list.groupBy { it.localDate(offsetSeconds).toIsoString() }

    val todayIso = CalendarDate.at(fetchedAtEpochMillis, offsetSeconds).toIsoString()

    // Folded into today's bucket so the header and the daily row cannot disagree. It also
    // rescues the late-evening case: by 23:00 the forecast has at most one step left for
    // today, and a "high" taken from that alone would be lower than the temperature already
    // on screen.
    val todayTemperatures = buckets[todayIso].orEmpty().map { it.main.temp } + current.main.temp

    val currentEntity = CurrentWeatherEntity(
        latitude = location.latitude,
        longitude = location.longitude,
        placeName = placeName,
        zoneOffsetSeconds = offsetSeconds,
        temperatureCelsius = current.main.temp,
        feelsLikeCelsius = current.main.feelsLike,
        // Never empty — the current temperature is always in the list — so these cannot throw.
        // If the forecast came back with nothing for today, both collapse to the current
        // reading: a "High 2° Low 2°" row is honest about knowing only one temperature, where
        // the provider's own temp_min/temp_max would dress up the same ignorance as a range.
        highCelsius = todayTemperatures.max(),
        lowCelsius = todayTemperatures.min(),
        condition = current.weather.toCondition().name,
        description = current.weather.firstOrNull()?.description.orEmpty(),
        isNight = current.weather.isNight(),
        windMetresPerSecond = current.wind?.speed ?: 0.0,
        humidityPercent = current.main.humidity,
        visibilityMetres = current.visibility,
        fetchedAtEpochMillis = fetchedAtEpochMillis,
    )

    val hourly = forecast.list.map { entry ->
        HourlyForecastEntity(
            startEpochMillis = entry.dt * MILLIS_PER_SECOND,
            temperatureCelsius = entry.main.temp,
            condition = entry.weather.toCondition().name,
            isNight = entry.weather.isNight(),
        )
    }

    val daily = buckets.map { (isoDate, entries) ->
        val temperatures = if (isoDate == todayIso) {
            todayTemperatures
        } else {
            entries.map { it.main.temp }
        }
        DailyForecastEntity(
            date = isoDate,
            highCelsius = temperatures.max(),
            lowCelsius = temperatures.min(),
            condition = entries.representativeCondition(offsetSeconds).name,
        )
    }

    return MappedWeather(current = currentEntity, hourly = hourly, daily = daily)
}

internal fun WeatherSnapshotEntity.toDomain() = WeatherSnapshot(
    latitude = current.latitude,
    longitude = current.longitude,
    placeName = current.placeName,
    zoneOffsetSeconds = current.zoneOffsetSeconds,
    current = CurrentWeather(
        temperatureCelsius = current.temperatureCelsius,
        feelsLikeCelsius = current.feelsLikeCelsius,
        highCelsius = current.highCelsius,
        lowCelsius = current.lowCelsius,
        condition = current.condition.toCondition(),
        description = current.description,
        isNight = current.isNight,
        windMetresPerSecond = current.windMetresPerSecond,
        humidityPercent = current.humidityPercent,
        visibilityMetres = current.visibilityMetres,
    ),
    // Sorted here, not assumed: `@Relation` has no ORDER BY, so the order rows come back in is
    // SQLite's business. Every consumer treats these as chronological, and this is the one
    // place that can guarantee it.
    hourly = hourly.sortedBy { it.startEpochMillis }.map { entity ->
        HourlyForecast(
            startEpochMillis = entity.startEpochMillis,
            temperatureCelsius = entity.temperatureCelsius,
            condition = entity.condition.toCondition(),
            isNight = entity.isNight,
        )
    },
    daily = daily.sortedBy { it.date }.map { entity ->
        DailyForecast(
            date = entity.date.toCalendarDate(),
            highCelsius = entity.highCelsius,
            lowCelsius = entity.lowCelsius,
            condition = entity.condition.toCondition(),
        )
    },
    fetchedAtEpochMillis = current.fetchedAtEpochMillis,
)

/**
 * The condition group, read from the provider's numeric code.
 *
 * The codes are grouped by hundreds and documented as such, which makes this a range check
 * rather than a table of fifty strings. Deliberately not read from the `main` field: that is an
 * English word the provider is free to reword, and it would need re-matching the moment the
 * request asks for another language — which it does, so the prose can be localised.
 *
 * `800` is exactly clear and `80x` above it are cloud cover, so clear is matched before the
 * eight-hundreds range rather than inside it.
 */
private fun List<WeatherDescriptionDto>.toCondition(): WeatherCondition {
    val id = firstOrNull()?.id ?: return WeatherCondition.Unknown
    return when (id) {
        in 200..299 -> WeatherCondition.Thunderstorm
        in 300..399 -> WeatherCondition.Drizzle
        in 500..599 -> WeatherCondition.Rain
        in 600..699 -> WeatherCondition.Snow
        in 700..799 -> WeatherCondition.Mist
        800 -> WeatherCondition.Clear
        in 801..899 -> WeatherCondition.Clouds
        else -> WeatherCondition.Unknown
    }
}

/**
 * The provider's own day/night verdict, taken from the trailing letter of the icon code.
 *
 * Defaults to day when the field is missing or malformed: a sun drawn at night is a cosmetic
 * error, and it is the same thing every other branch of a missing-data path would have to pick
 * anyway.
 */
private fun List<WeatherDescriptionDto>.isNight(): Boolean =
    firstOrNull()?.icon?.endsWith("n") == true

/**
 * The one condition that stands for a whole day.
 *
 * The step nearest local midday, rather than the most frequent one across the day. Over a
 * five-step day the modal condition is usually the overnight one — clear skies at 03:00 — which
 * would label a wet afternoon "clear". Midday is what a person means by "what is Wednesday
 * like".
 */
private fun List<ForecastEntryDto>.representativeCondition(offsetSeconds: Int): WeatherCondition =
    minByOrNull { abs(it.localHour(offsetSeconds) - MIDDAY_HOUR) }
        ?.weather
        ?.toCondition()
        ?: WeatherCondition.Unknown

private fun ForecastEntryDto.localDate(offsetSeconds: Int): CalendarDate =
    CalendarDate.at(dt * MILLIS_PER_SECOND, offsetSeconds)

private fun ForecastEntryDto.localHour(offsetSeconds: Int): Int {
    val localSeconds = dt + offsetSeconds
    // floorMod, not `%`: the remainder of a negative epoch is negative, which would put a
    // pre-1970 step at a negative hour. Unreachable with live data, and one less thing to be
    // wrong about in a fixture.
    return (Math.floorMod(localSeconds, SECONDS_PER_DAY) / SECONDS_PER_HOUR).toInt()
}

/**
 * [WeatherCondition] by name, tolerating anything unrecognised.
 *
 * `valueOf` would throw, and the row it threw on came off this device's own disk — written by a
 * build that may have known a group this one does not, after an install of an older APK. Losing
 * an icon is the right cost; crashing the tab that exists to work offline is not.
 */
private fun String.toCondition(): WeatherCondition =
    WeatherCondition.entries.firstOrNull { it.name == this } ?: WeatherCondition.Unknown

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_HOUR = 3_600L
private const val SECONDS_PER_DAY = 86_400L
private const val MIDDAY_HOUR = 12
