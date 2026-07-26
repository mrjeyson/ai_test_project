package com.example.test_ai_project.core.model

/**
 * The condition groups the app draws an icon for.
 *
 * A closed set that is deliberately *coarser* than the provider's own vocabulary.
 * OpenWeatherMap distinguishes some fifty conditions — "light intensity shower rain",
 * "sand/dust whirls", "squalls" — and none of that survives being drawn at 24dp. Collapsing
 * them here means the icon lookup in the UI is a `when` with no `else`, and the provider's
 * exact wording still reaches the screen through [CurrentWeather.description].
 *
 * [Unknown] is the one entry that is not a kind of weather. It exists because this enum is
 * mapped from a remote value and the provider may add a group at any time: without it, a new
 * condition code would have to either crash the mapper or silently render as [Clear].
 */
enum class WeatherCondition {
    Clear,
    Clouds,
    Rain,
    Drizzle,
    Thunderstorm,
    Snow,

    /**
     * The provider's "atmosphere" group flattened into one: mist, fog, haze, smoke, dust,
     * sand, ash. They differ in cause and not in what the sky looks like or what the user
     * should do about it.
     */
    Mist,
    Unknown,
}

/**
 * Conditions at one place, right now.
 *
 * Every temperature is Celsius and every measurement is SI — metres per second, metres,
 * percent — because this is the shape the provider is asked for and converting once, at the
 * point of display, is cheaper than tracking which unit a field is in. The screen turns
 * [windMetresPerSecond] into km/h and [visibilityMetres] into km; nothing below the UI has
 * an opinion about that.
 *
 * [highCelsius]/[lowCelsius] are the *day's* range, and are deliberately not the provider's
 * `temp_min`/`temp_max` on the current-conditions endpoint. Those two describe the spread
 * across the reporting area at this instant, not across the day, and for most locations they
 * are simply equal to [temperatureCelsius] — a "High 1° Low 1°" row that looks broken. The
 * data layer derives this range from the forecast instead.
 *
 * [isNight] comes from the provider's own day/night flag rather than being computed from the
 * clock, which would need a sunrise time this endpoint does not usefully give for anywhere
 * but the queried point. It picks a moon over a sun, and nothing more.
 */
data class CurrentWeather(
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val highCelsius: Double,
    val lowCelsius: Double,
    val condition: WeatherCondition,
    /** The provider's own phrasing, e.g. "broken clouds". Already localised by the request. */
    val description: String,
    val isNight: Boolean,
    val windMetresPerSecond: Double,
    val humidityPercent: Int,
    /**
     * Null when the provider omitted it — which it does, rather than reporting zero. A
     * missing reading and "you cannot see your hand" are opposite claims, so they cannot
     * share a representation.
     */
    val visibilityMetres: Int?,
)

/**
 * One step of the short-range forecast.
 *
 * Named "hourly" after the row it fills rather than after its resolution: the free tier
 * answers in three-hour steps, so the strip shows 3-hourly columns. [startEpochMillis] is
 * absolute, so the UI can label it in the *location's* clock without re-deriving an offset.
 */
data class HourlyForecast(
    val startEpochMillis: Long,
    val temperatureCelsius: Double,
    val condition: WeatherCondition,
    val isNight: Boolean,
)

/** One calendar day of the forecast, in the location's own time zone. */
data class DailyForecast(
    val date: CalendarDate,
    val highCelsius: Double,
    val lowCelsius: Double,
    val condition: WeatherCondition,
)

/**
 * Everything the Weather tab renders, for one place, as of one moment.
 *
 * One aggregate rather than three independently cached pieces, because the three are only
 * meaningful together: a current temperature from Reykjavík beside a forecast for London is
 * worse than no forecast at all. They are fetched in one refresh, written in one
 * transaction, and read back through this type — so a partially-updated screen is not
 * representable.
 *
 * [latitude]/[longitude] travel with the snapshot for the same reason they travel with
 * [PrayerDay]: the readings are only valid for where they were taken, and the repository can
 * only notice the user has moved if the cache remembers where it came from.
 *
 * [zoneOffsetSeconds] is an offset rather than an IANA zone id — the difference from
 * [PrayerDay.zoneId] is the provider's, not a choice. OpenWeatherMap reports a shift from
 * UTC in seconds and no zone name, so there is nothing to look a name up by. It is enough to
 * format an instant, and not enough to survive a DST transition mid-forecast; the forecast
 * window is five days, which makes that a twice-a-year, one-hour error on the far end of the
 * daily list rather than something worth inventing a zone database for.
 *
 * [placeName] is nullable because it is a best-effort lookup. A snapshot with no name is
 * still complete and correct; the screen falls back to coordinates.
 */
data class WeatherSnapshot(
    val latitude: Double,
    val longitude: Double,
    val placeName: String?,
    /** Seconds to add to UTC to get the location's local clock. Negative west of Greenwich. */
    val zoneOffsetSeconds: Int,
    val current: CurrentWeather,
    /** Chronological. Empty if only current conditions were ever cached. */
    val hourly: List<HourlyForecast>,
    /** Chronological, starting with today. Empty if only current conditions were cached. */
    val daily: List<DailyForecast>,
    val fetchedAtEpochMillis: Long,
) {
    /**
     * The steps falling in the [withinMillis] after [epochMillis] — what "Next 24h" means.
     *
     * Filtered here rather than at the query, because the cache is written once and read
     * against a clock that keeps moving: the same cached rows are the next 24 hours at
     * 14:00 and the next 21 at 17:00, and a snapshot that had already been trimmed could
     * only get staler.
     */
    fun hourlyWithin(epochMillis: Long, withinMillis: Long): List<HourlyForecast> =
        hourly.filter {
            it.startEpochMillis > epochMillis && it.startEpochMillis <= epochMillis + withinMillis
        }

    /**
     * The date it is at [epochMillis] *where these readings belong* — not where the device is.
     *
     * The distinction is the whole reason this exists rather than the caller reading a clock. It is
     * also why "today" cannot be taken to be the first entry of [daily]: the forecast window opens
     * at the next three-hour step, so a snapshot fetched at 23:50 local has no entry for today at
     * all and its first row is *tomorrow*. Labelling that row "Today" would be wrong for the ten
     * minutes it matters most.
     */
    fun localDateAt(epochMillis: Long): CalendarDate =
        CalendarDate.at(epochMillis, zoneOffsetSeconds)
}
