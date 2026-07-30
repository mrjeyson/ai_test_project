package com.example.test_ai_project.home.data.mapper

import com.example.test_ai_project.database.entity.CurrentWeatherEntity
import com.example.test_ai_project.database.entity.DailyForecastEntity
import com.example.test_ai_project.database.entity.HourlyForecastEntity
import com.example.test_ai_project.database.entity.WeatherSnapshotEntity
import com.example.test_ai_project.home.domain.model.CalendarDate
import com.example.test_ai_project.home.domain.model.UserLocation
import com.example.test_ai_project.home.domain.model.WeatherCondition
import com.example.test_ai_project.network.dto.CurrentWeatherResponseDto
import com.example.test_ai_project.network.dto.ForecastCityDto
import com.example.test_ai_project.network.dto.ForecastEntryDto
import com.example.test_ai_project.network.dto.ForecastResponseDto
import com.example.test_ai_project.network.dto.WeatherDescriptionDto
import com.example.test_ai_project.network.dto.WeatherMainDto
import com.example.test_ai_project.network.dto.WeatherSysDto
import com.example.test_ai_project.network.dto.WeatherWindDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mapper is where the provider's units, codes and time zone stop travelling, so it is where the
 * subtle mistakes live: a day bucketed against UTC instead of the location, a "high" taken from a
 * field that does not mean what it looks like, an enum that throws on an unrecognised string.
 */
class WeatherMappersTest {

    @Test
    fun `condition ids map to groups, with clear and clouds split at 800`() {
        val cases = mapOf(
            210 to WeatherCondition.Thunderstorm,
            302 to WeatherCondition.Drizzle,
            502 to WeatherCondition.Rain,
            601 to WeatherCondition.Snow,
            741 to WeatherCondition.Mist,
            800 to WeatherCondition.Clear,
            // The one off-by-one that matters: 800 is exactly clear and everything above it in the
            // eight-hundreds is cloud cover, so a range check written as `in 800..899` would call a
            // fully overcast sky clear.
            804 to WeatherCondition.Clouds,
            // A group this build does not know. Mapped rather than thrown on, because the provider
            // is free to add one at any time.
            999 to WeatherCondition.Unknown,
        )

        cases.forEach { (id, expected) ->
            val mapped = mapWeather(
                current = currentDto(conditionId = id),
                forecast = forecastDto(entries = emptyList()),
                location = Fix,
                placeName = "Reykjavík, IS",
                fetchedAtEpochMillis = seconds(MidnightUtc + 13 * 3600),
            )
            assertEquals("id $id", expected.name, mapped.current.condition)
        }
    }

    @Test
    fun `the day-night flag comes from the icon suffix`() {
        val night = mapWeather(
            current = currentDto(icon = "04n"),
            forecast = forecastDto(entries = emptyList()),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc),
        )
        assertTrue(night.current.isNight)

        val day = mapWeather(
            current = currentDto(icon = "04d"),
            forecast = forecastDto(entries = emptyList()),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc),
        )
        assertFalse(day.current.isNight)
    }

    @Test
    fun `forecast steps are bucketed by the location's date, not by UTC`() {
        // UTC-5. The first step is 02:00 UTC on the 26th, which is 21:00 on the 25th where the
        // reading belongs — so bucketing against UTC would file it under the wrong day and shift
        // every row of the daily list.
        val offset = -5 * 3600

        val mapped = mapWeather(
            current = currentDto(),
            forecast = forecastDto(
                timezone = offset,
                entries = listOf(
                    forecastEntry(atSecond = MidnightUtc + 2 * 3600, temp = 4.0),
                    forecastEntry(atSecond = MidnightUtc + 10 * 3600, temp = 6.0),
                ),
            ),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc + 2 * 3600),
        )

        assertEquals(offset, mapped.current.zoneOffsetSeconds)
        assertEquals(listOf("2026-07-25", "2026-07-26"), mapped.daily.map { it.date }.sorted())
    }

    @Test
    fun `the forecast's own offset wins over the current-conditions one`() {
        // They disagree here on purpose. The `dt` values live in the forecast payload, so they have
        // to be bucketed against the offset that came with them.
        val mapped = mapWeather(
            current = currentDto(timezone = 0),
            forecast = forecastDto(
                timezone = -5 * 3600,
                entries = listOf(forecastEntry(atSecond = MidnightUtc + 2 * 3600, temp = 4.0)),
            ),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc + 2 * 3600),
        )

        assertEquals(-5 * 3600, mapped.current.zoneOffsetSeconds)
        assertEquals(listOf("2026-07-25"), mapped.daily.map { it.date })
    }

    @Test
    fun `today's high and low come from the forecast, folding in the current reading`() {
        val mapped = mapWeather(
            // temp_min and temp_max here describe the spread across the reporting area at this
            // instant, not across the day. Trusting them would produce "High 9° Low 9°".
            current = currentDto(temp = 9.0, tempMin = 9.0, tempMax = 9.0),
            forecast = forecastDto(
                entries = listOf(
                    forecastEntry(atSecond = MidnightUtc + 15 * 3600, temp = 5.0),
                    forecastEntry(atSecond = MidnightUtc + 18 * 3600, temp = 2.0),
                ),
            ),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc + 13 * 3600),
        )

        // The current reading is warmer than anything left in the day, and it is real — so it is
        // the high. Without folding it in, the header would show a high below the temperature
        // printed directly above it.
        assertEquals(9.0, mapped.current.highCelsius, 0.001)
        assertEquals(2.0, mapped.current.lowCelsius, 0.001)

        // And the daily row for today has to agree with the header, or the same page contradicts
        // itself twice over.
        val today = mapped.daily.single { it.date == "2026-07-26" }
        assertEquals(9.0, today.highCelsius, 0.001)
        assertEquals(2.0, today.lowCelsius, 0.001)
    }

    @Test
    fun `an empty forecast collapses the range onto the current reading`() {
        val mapped = mapWeather(
            current = currentDto(temp = 3.0, tempMin = 1.0, tempMax = 5.0),
            forecast = forecastDto(entries = emptyList()),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc + 13 * 3600),
        )

        // "High 3° Low 3°" is honest about knowing one temperature. The provider's 1°/5° would
        // dress the same ignorance up as a range.
        assertEquals(3.0, mapped.current.highCelsius, 0.001)
        assertEquals(3.0, mapped.current.lowCelsius, 0.001)
        assertTrue(mapped.daily.isEmpty())
    }

    @Test
    fun `a day is labelled by its midday condition, not its most frequent one`() {
        val mapped = mapWeather(
            current = currentDto(),
            forecast = forecastDto(
                entries = listOf(
                    forecastEntry(MidnightUtc + 3 * 3600, temp = 1.0, conditionId = 800),
                    forecastEntry(MidnightUtc + 6 * 3600, temp = 1.0, conditionId = 800),
                    forecastEntry(MidnightUtc + 9 * 3600, temp = 1.0, conditionId = 800),
                    forecastEntry(MidnightUtc + 12 * 3600, temp = 1.0, conditionId = 502),
                    forecastEntry(MidnightUtc + 21 * 3600, temp = 1.0, conditionId = 800),
                ),
            ),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc + 13 * 3600),
        )

        // Four of the five steps are clear, so the modal condition is Clear — and labelling a wet
        // afternoon "clear" because it was fine overnight is exactly the failure this avoids.
        assertEquals(
            WeatherCondition.Rain.name,
            mapped.daily.single { it.date == "2026-07-26" }.condition,
        )
    }

    @Test
    fun `hourly steps keep epoch millis, converted from the provider's seconds`() {
        val mapped = mapWeather(
            current = currentDto(),
            forecast = forecastDto(
                entries = listOf(forecastEntry(MidnightUtc + 15 * 3600, temp = 5.0)),
            ),
            location = Fix,
            placeName = null,
            fetchedAtEpochMillis = seconds(MidnightUtc + 13 * 3600),
        )

        assertEquals(seconds(MidnightUtc + 15 * 3600), mapped.hourly.single().startEpochMillis)
    }

    @Test
    fun `reading back sorts both forecast lists chronologically`() {
        // Room's @Relation has no ORDER BY, so this is the order rows can genuinely arrive in.
        val entity = WeatherSnapshotEntity(
            current = currentEntity(),
            hourly = listOf(
                HourlyForecastEntity(seconds(MidnightUtc + 18 * 3600), temperatureCelsius = 3.0, condition = "Clear", isNight = true),
                HourlyForecastEntity(seconds(MidnightUtc + 6 * 3600), temperatureCelsius = 1.0, condition = "Clear", isNight = false),
                HourlyForecastEntity(seconds(MidnightUtc + 12 * 3600), temperatureCelsius = 2.0, condition = "Rain", isNight = false),
            ),
            daily = listOf(
                DailyForecastEntity("2026-07-28", highCelsius = 4.0, lowCelsius = 0.0, condition = "Clear"),
                DailyForecastEntity("2026-07-26", highCelsius = 1.0, lowCelsius = -6.0, condition = "Snow"),
                DailyForecastEntity("2026-07-27", highCelsius = 2.0, lowCelsius = -3.0, condition = "Clouds"),
            ),
        )

        val snapshot = entity.toDomain()

        assertEquals(
            listOf(1.0, 2.0, 3.0),
            snapshot.hourly.map { it.temperatureCelsius },
        )
        assertEquals(
            listOf(CalendarDate(2026, 7, 26), CalendarDate(2026, 7, 27), CalendarDate(2026, 7, 28)),
            snapshot.daily.map { it.date },
        )
    }

    @Test
    fun `an unrecognised stored condition reads back as Unknown rather than throwing`() {
        // The row came off this device's own disk, written by a build that may have known a group
        // this one does not. Losing an icon is the right cost; crashing the offline tab is not.
        val snapshot = WeatherSnapshotEntity(
            current = currentEntity(condition = "Tornado"),
            hourly = emptyList(),
            daily = emptyList(),
        ).toDomain()

        assertEquals(WeatherCondition.Unknown, snapshot.current.condition)
    }

    @Test
    fun `the 24-hour window excludes steps already past and stops at the edge`() {
        val now = seconds(MidnightUtc + 12 * 3600)
        val snapshot = WeatherSnapshotEntity(
            current = currentEntity(),
            hourly = listOf(
                // Three hours ago — already happened.
                HourlyForecastEntity(seconds(MidnightUtc + 9 * 3600), temperatureCelsius = 1.0, condition = "Clear", isNight = false),
                // Exactly now: not "after now", so it is out. The strip's leading column is
                // synthesised from current conditions instead.
                HourlyForecastEntity(now, temperatureCelsius = 2.0, condition = "Clear", isNight = false),
                HourlyForecastEntity(seconds(MidnightUtc + 15 * 3600), temperatureCelsius = 3.0, condition = "Clear", isNight = false),
                // Exactly 24 hours out — inclusive, so it stays.
                HourlyForecastEntity(seconds(MidnightUtc + 36 * 3600), temperatureCelsius = 4.0, condition = "Clear", isNight = false),
                // One step beyond the window.
                HourlyForecastEntity(seconds(MidnightUtc + 39 * 3600), temperatureCelsius = 5.0, condition = "Clear", isNight = false),
            ),
            daily = emptyList(),
        ).toDomain()

        assertEquals(
            listOf(3.0, 4.0),
            snapshot.hourlyWithin(now, withinMillis = 24 * 3_600_000L)
                .map { it.temperatureCelsius },
        )
    }

    private fun currentDto(
        temp: Double = -2.0,
        tempMin: Double? = null,
        tempMax: Double? = null,
        conditionId: Int = 803,
        icon: String = "04d",
        timezone: Int = 0,
    ) = CurrentWeatherResponseDto(
        weather = listOf(
            WeatherDescriptionDto(
                id = conditionId,
                main = "Clouds",
                description = "broken clouds",
                icon = icon,
            ),
        ),
        main = WeatherMainDto(
            temp = temp,
            feelsLike = temp - 6.0,
            tempMin = tempMin,
            tempMax = tempMax,
            humidity = 78,
        ),
        wind = WeatherWindDto(speed = 3.9),
        visibility = 12_000,
        timezone = timezone,
        name = "Reykjavík",
        sys = WeatherSysDto(country = "IS"),
    )

    private fun forecastDto(
        entries: List<ForecastEntryDto>,
        timezone: Int? = 0,
    ) = ForecastResponseDto(
        list = entries,
        city = ForecastCityDto(name = "Reykjavík", country = "IS", timezone = timezone),
    )

    private fun forecastEntry(
        atSecond: Long,
        temp: Double,
        conditionId: Int = 601,
    ) = ForecastEntryDto(
        dt = atSecond,
        main = WeatherMainDto(temp = temp, feelsLike = temp - 5.0, humidity = 80),
        weather = listOf(
            WeatherDescriptionDto(id = conditionId, main = "Snow", description = "light snow", icon = "13d"),
        ),
    )

    private fun currentEntity(condition: String = "Clouds") = CurrentWeatherEntity(
        latitude = 64.1466,
        longitude = -21.9426,
        placeName = "Reykjavík, IS",
        zoneOffsetSeconds = 0,
        temperatureCelsius = -2.0,
        feelsLikeCelsius = -8.0,
        highCelsius = 1.0,
        lowCelsius = -5.0,
        condition = condition,
        description = "broken clouds",
        isNight = false,
        windMetresPerSecond = 3.9,
        humidityPercent = 78,
        visibilityMetres = 12_000,
        fetchedAtEpochMillis = seconds(MidnightUtc),
    )

    private companion object {
        /** 2026-07-26T00:00:00Z, in epoch seconds. */
        const val MidnightUtc = 1_785_024_000L

        fun seconds(value: Long): Long = value * 1_000L

        val Fix = UserLocation(
            latitude = 64.1466,
            longitude = -21.9426,
            accuracyMeters = 30f,
            capturedAtEpochMillis = 1_785_024_000_000L,
        )
    }
}
