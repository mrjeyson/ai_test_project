package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.Item
import kotlinx.coroutines.flow.Flow

/**
 * The dependency inversion boundary of the app.
 *
 * Declared here in the domain layer and implemented in `:core:data`, so the
 * arrow of dependency points inward: data knows about domain, never the reverse.
 */
interface ItemRepository {

    /** Emits the locally cached items, and re-emits whenever the cache changes. */
    fun observeItems(): Flow<List<Item>>

    /** Emits the item with [id], or `null` if it is not cached. */
    fun observeItem(id: Long): Flow<Item?>

    /** Fetches from the remote source and replaces the local cache. */
    suspend fun refreshItems()
}
