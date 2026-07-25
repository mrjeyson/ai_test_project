package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.ItemRepository
import javax.inject.Inject

/** Pulls the latest items from the remote source into the local cache. */
class RefreshItemsUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
) {
    suspend operator fun invoke() = itemRepository.refreshItems()
}
