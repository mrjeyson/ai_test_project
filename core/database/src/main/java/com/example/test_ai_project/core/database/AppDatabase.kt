package com.example.test_ai_project.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.test_ai_project.core.database.dao.ItemDao
import com.example.test_ai_project.core.database.dao.LocationDao
import com.example.test_ai_project.core.database.dao.MovieDao
import com.example.test_ai_project.core.database.entity.ItemEntity
import com.example.test_ai_project.core.database.entity.LastKnownLocationEntity
import com.example.test_ai_project.core.database.entity.MapCameraEntity
import com.example.test_ai_project.core.database.entity.MovieCatalogEntity
import com.example.test_ai_project.core.database.entity.MovieEntity
import com.example.test_ai_project.core.database.entity.MoviePageEntryEntity

/**
 * Version 2 adds the movie cache; version 3 adds the map's last-known location and saved
 * camera.
 *
 * Both steps are an [AutoMigration]: each only creates tables, and Room generates the DDL
 * from the exported schemas. That is strictly safer than a hand-written `CREATE TABLE`,
 * which has to match Room's own generated statement character for character or fail schema
 * validation later, at runtime, on a device.
 *
 * A destructive fallback would also have "worked" — everything in here is re-fetchable —
 * but it would wipe the cache on every upgrade, which is precisely the offline behaviour
 * the movie list and the map exist to guarantee. The location row in particular is *not*
 * cheaply re-fetchable: reacquiring it needs a permission grant, a working GPS or network
 * fix, and a user who is currently outdoors.
 */
@Database(
    entities = [
        ItemEntity::class,
        MovieEntity::class,
        MoviePageEntryEntity::class,
        MovieCatalogEntity::class,
        LastKnownLocationEntity::class,
        MapCameraEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun movieDao(): MovieDao
    abstract fun locationDao(): LocationDao
}
