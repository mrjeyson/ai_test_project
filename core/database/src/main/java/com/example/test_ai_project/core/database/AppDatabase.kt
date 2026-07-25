package com.example.test_ai_project.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.test_ai_project.core.database.dao.ItemDao
import com.example.test_ai_project.core.database.entity.ItemEntity

@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
