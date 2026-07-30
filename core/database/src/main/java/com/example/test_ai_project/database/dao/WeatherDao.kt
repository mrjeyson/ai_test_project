package com.example.test_ai_project.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.test_ai_project.database.entity.CurrentWeatherEntity
import com.example.test_ai_project.database.entity.DailyForecastEntity
import com.example.test_ai_project.database.entity.HourlyForecastEntity
import com.example.test_ai_project.database.entity.WeatherSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    /**
     * The whole cached snapshot, as a stream.
     *
     * Null until a first fetch has ever succeeded — on a cold start with no network, that is
     * until there is one.
     *
     * `@Transaction` is what makes this correct rather than merely convenient: Room runs the
     * parent query and both relation queries inside one transaction, so a concurrent
     * [replaceSnapshot] cannot be observed half-applied. Room also invalidates the flow when
     * *any* of the three tables changes, so a forecast-only write still reaches the screen.
     */
    @Transaction
    @Query("SELECT * FROM weather_current WHERE id = ${CurrentWeatherEntity.SINGLETON_ID}")
    fun observeSnapshot(): Flow<WeatherSnapshotEntity?>

    /**
     * The same snapshot, read once — for the refresh path, which needs to know how old and
     * how far away the cache is before deciding whether to spend a request on it.
     *
     * Only the current row, not the relations: the freshness decision reads coordinates and
     * a timestamp, and pulling forty forecast rows to answer it would be work thrown away on
     * every check.
     */
    @Query("SELECT * FROM weather_current WHERE id = ${CurrentWeatherEntity.SINGLETON_ID}")
    suspend fun current(): CurrentWeatherEntity?

    /**
     * Swaps the cache for a newly fetched snapshot, atomically.
     *
     * Delete-then-insert rather than upsert for the two forecast tables, and the difference
     * is load-bearing: their keys are timestamps and dates, so yesterday's rows do not
     * collide with today's and an upsert would leave them behind forever — a table that
     * grows without bound and a "Next 24h" strip that starts in the past. The current row is
     * a singleton and genuinely does want an upsert.
     *
     * `@Transaction` so the screen never observes the gap between the delete and the insert,
     * which would otherwise flicker the forecast sections empty on every refresh.
     */
    @Transaction
    suspend fun replaceSnapshot(
        current: CurrentWeatherEntity,
        hourly: List<HourlyForecastEntity>,
        daily: List<DailyForecastEntity>,
    ) {
        deleteHourly()
        deleteDaily()
        upsertCurrent(current)
        insertHourly(hourly)
        insertDaily(daily)
    }

    @Upsert
    suspend fun upsertCurrent(current: CurrentWeatherEntity)

    @Upsert
    suspend fun insertHourly(hourly: List<HourlyForecastEntity>)

    @Upsert
    suspend fun insertDaily(daily: List<DailyForecastEntity>)

    @Query("DELETE FROM weather_hourly")
    suspend fun deleteHourly()

    @Query("DELETE FROM weather_daily")
    suspend fun deleteDaily()
}
