package com.example.test_ai_project.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.test_ai_project.core.database.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<ItemEntity?>

    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    /**
     * Swaps the cache contents atomically, so collectors of [observeAll] never
     * observe an empty list in the middle of a refresh.
     */
    @Transaction
    suspend fun replaceAll(items: List<ItemEntity>) {
        deleteAll()
        upsertAll(items)
    }
}
