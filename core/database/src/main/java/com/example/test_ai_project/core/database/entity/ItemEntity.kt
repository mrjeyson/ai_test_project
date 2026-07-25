package com.example.test_ai_project.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room's view of an item. Mapped to the domain model in `:core:data`. */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String,
)
