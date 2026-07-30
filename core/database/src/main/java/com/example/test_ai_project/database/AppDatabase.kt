package com.example.test_ai_project.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.example.test_ai_project.database.dao.LocationDao
import com.example.test_ai_project.database.dao.MovieDao
import com.example.test_ai_project.database.dao.PrayerTimesDao
import com.example.test_ai_project.database.dao.WeatherDao
import com.example.test_ai_project.database.entity.CurrentWeatherEntity
import com.example.test_ai_project.database.entity.DailyForecastEntity
import com.example.test_ai_project.database.entity.HourlyForecastEntity
import com.example.test_ai_project.database.entity.LastKnownLocationEntity
import com.example.test_ai_project.database.entity.MapCameraEntity
import com.example.test_ai_project.database.entity.MovieCatalogEntity
import com.example.test_ai_project.database.entity.MovieEntity
import com.example.test_ai_project.database.entity.MoviePageEntryEntity
import com.example.test_ai_project.database.entity.PrayerDayEntity

/**
 * Version 2 adds the movie cache; version 3 adds the map's last-known location and saved
 * camera; version 4 adds the cached prayer days; version 5 adds the cached weather snapshot.
 *
 * Every step is an [AutoMigration]: each only creates tables, and Room generates the DDL
 * from the exported schemas. That is strictly safer than a hand-written `CREATE TABLE`,
 * which has to match Room's own generated statement character for character or fail schema
 * validation later, at runtime, on a device.
 *
 * A destructive fallback would also have "worked" — everything in here is re-fetchable —
 * but it would wipe the cache on every upgrade, which is precisely the offline behaviour
 * the movie list, the map, the prayer schedule and the weather tab exist to guarantee. Two
 * of these are *not* cheaply re-fetchable: the location row needs a permission grant, a
 * working fix and a user who is currently outdoors, and the prayer days are what the tab
 * falls back to when there is no network to re-fetch them from at all.
 *
 * Version 6 drops `items`, the scaffolding table the project was generated with. It is the
 * one step that removes rather than adds, so it is the one step that needs an
 * [AutoMigrationSpec] — [DropItemsTable] below — to tell Room the table is going away on
 * purpose rather than by accident.
 */
@Database(
    entities = [
        MovieEntity::class,
        MoviePageEntryEntity::class,
        MovieCatalogEntity::class,
        LastKnownLocationEntity::class,
        MapCameraEntity::class,
        PrayerDayEntity::class,
        CurrentWeatherEntity::class,
        HourlyForecastEntity::class,
        DailyForecastEntity::class,
    ],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = AppDatabase.DropItemsTable::class),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao
    abstract fun locationDao(): LocationDao
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun weatherDao(): WeatherDao

    /**
     * Without this, Room sees a table in schema 5 that is absent from schema 6 and refuses
     * to generate the migration rather than guess that the data is meant to be discarded.
     */
    @DeleteTable(tableName = "items")
    class DropItemsTable : AutoMigrationSpec
}
