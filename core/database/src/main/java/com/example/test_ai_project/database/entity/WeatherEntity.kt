package com.example.test_ai_project.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * The cached current conditions — the row that makes the tab work with no network.
 *
 * A single row pinned to [SINGLETON_ID], like [LastKnownLocationEntity]: the tab shows
 * where the user is, so a second row could only ever be a stale first one. That also makes
 * the write a plain upsert with no key to invent and no pruning query to keep up with.
 *
 * The forecast lives in [HourlyForecastEntity] and [DailyForecastEntity] rather than in
 * columns here, because unlike the five prayers of a day those are genuinely variable in
 * length — the provider returns as many steps as its window covers, and a schema with forty
 * `hour1..hour40` columns would encode a free-tier detail as a table definition.
 *
 * [latitude]/[longitude] are stored so the repository can tell a still-useful snapshot from
 * one belonging to a city the user has since left, and [fetchedAtEpochMillis] so it can tell
 * a fresh one from a stale one. Weather expires in minutes, unlike a prayer timetable, so
 * both checks matter here and only one does there.
 */
@Entity(tableName = "weather_current")
data class CurrentWeatherEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val latitude: Double,
    val longitude: Double,
    val placeName: String?,
    val zoneOffsetSeconds: Int,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val highCelsius: Double,
    val lowCelsius: Double,
    /** [com.example.test_ai_project.core.model.WeatherCondition] by name. */
    val condition: String,
    val description: String,
    val isNight: Boolean,
    val windMetresPerSecond: Double,
    val humidityPercent: Int,
    val visibilityMetres: Int?,
    val fetchedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}

/**
 * One step of the short-range forecast.
 *
 * [ownerId] is always [CurrentWeatherEntity.SINGLETON_ID] and buys exactly one thing: it is
 * the key the [Relation] below joins on, which is what makes the whole snapshot readable in
 * a single `@Transaction` query. Without it the screen would have to combine three
 * independent flows and could render a moment where the current temperature had updated and
 * the forecast had not. A degenerate foreign key is a small price for a read that cannot
 * tear; if the cache ever holds more than one place, this becomes a real key.
 */
@Entity(tableName = "weather_hourly")
data class HourlyForecastEntity(
    @PrimaryKey val startEpochMillis: Long,
    val ownerId: Int = CurrentWeatherEntity.SINGLETON_ID,
    val temperatureCelsius: Double,
    val condition: String,
    val isNight: Boolean,
)

/** One forecast day. Keyed by ISO `yyyy-MM-dd`, which sorts lexicographically. */
@Entity(tableName = "weather_daily")
data class DailyForecastEntity(
    @PrimaryKey val date: String,
    val ownerId: Int = CurrentWeatherEntity.SINGLETON_ID,
    val highCelsius: Double,
    val lowCelsius: Double,
    val condition: String,
)

/**
 * The three tables read as one value.
 *
 * Not an `@Entity` — Room builds it from a `@Transaction` query, so every field comes from
 * the same consistent view of the database.
 *
 * The two lists arrive in whatever order SQLite returns them: `@Relation` has no `ORDER BY`,
 * and relying on primary-key order would be relying on an implementation detail. The mapper
 * sorts them, which is the one place that has to know they are chronological.
 */
data class WeatherSnapshotEntity(
    @Embedded val current: CurrentWeatherEntity,
    @Relation(parentColumn = "id", entityColumn = "ownerId")
    val hourly: List<HourlyForecastEntity>,
    @Relation(parentColumn = "id", entityColumn = "ownerId")
    val daily: List<DailyForecastEntity>,
)
