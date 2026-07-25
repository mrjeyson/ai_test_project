package com.example.test_ai_project.core.data.mapper

import com.example.test_ai_project.core.database.entity.ItemEntity
import com.example.test_ai_project.core.model.Item
import com.example.test_ai_project.core.network.dto.ItemDto

/**
 * Translation between the three representations of an item.
 *
 * These are `internal` on purpose: nothing outside `:core:data` should need to know
 * that `ItemEntity` or `ItemDto` exist.
 */
internal fun ItemEntity.toDomain(): Item = Item(
    id = id,
    name = name,
    description = description,
)

internal fun ItemDto.toEntity(): ItemEntity = ItemEntity(
    id = id,
    name = name,
    description = description,
)
