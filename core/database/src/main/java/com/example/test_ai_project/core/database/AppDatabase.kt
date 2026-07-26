package com.example.test_ai_project.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.test_ai_project.core.database.dao.ItemDao
import com.example.test_ai_project.core.database.dao.MovieDao
import com.example.test_ai_project.core.database.entity.ItemEntity
import com.example.test_ai_project.core.database.entity.MovieCatalogEntity
import com.example.test_ai_project.core.database.entity.MovieEntity
import com.example.test_ai_project.core.database.entity.MoviePageEntryEntity

/**
 * Version 2 adds the movie cache.
 *
 * The 1 → 2 step is an [AutoMigration]: it only creates tables, and Room generates the DDL
 * from the two exported schemas. That is strictly safer than a hand-written `CREATE TABLE`,
 * which has to match Room's own generated statement character for character or fail schema
 * validation later, at runtime, on a device.
 *
 * A destructive fallback would also have "worked" — everything in here is re-fetchable —
 * but it would wipe the cache on every upgrade, which is precisely the offline behaviour
 * the movie list exists to guarantee.
 */
@Database(
    entities = [
        ItemEntity::class,
        MovieEntity::class,
        MoviePageEntryEntity::class,
        MovieCatalogEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun movieDao(): MovieDao
}
