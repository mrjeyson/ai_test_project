package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.ItemRepository
import com.example.test_ai_project.core.model.Item
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observes the item list, sorted for presentation.
 *
 * The sort lives here rather than in the ViewModel so every caller gets the same
 * ordering, and rather than in the DAO so it is verifiable without a database.
 */
class GetItemsUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
) {
    operator fun invoke(): Flow<List<Item>> =
        itemRepository.observeItems().map { items ->
            items.sortedBy { it.name.lowercase() }
        }
}
