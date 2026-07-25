package com.example.test_ai_project.core.domain.usecase

import app.cash.turbine.test
import com.example.test_ai_project.core.domain.repository.ItemRepository
import com.example.test_ai_project.core.model.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Runs on the JVM with no Robolectric and no instrumentation — the payoff for
 * keeping `:core:domain` free of Android dependencies.
 */
class GetItemsUseCaseTest {

    private val repository = FakeItemRepository()
    private val getItems = GetItemsUseCase(repository)

    @Test
    fun `emits items sorted by name, case-insensitively`() = runTest {
        repository.setItems(
            listOf(
                item(id = 1, name = "banana"),
                item(id = 2, name = "Apple"),
                item(id = 3, name = "cherry"),
            ),
        )

        getItems().test {
            assertEquals(listOf("Apple", "banana", "cherry"), awaitItem().map { it.name })
        }
    }

    @Test
    fun `re-emits when the underlying cache changes`() = runTest {
        repository.setItems(listOf(item(id = 1, name = "First")))

        getItems().test {
            assertEquals(1, awaitItem().size)

            repository.setItems(
                listOf(item(id = 1, name = "First"), item(id = 2, name = "Second")),
            )

            assertEquals(2, awaitItem().size)
        }
    }

    private fun item(id: Long, name: String) =
        Item(id = id, name = name, description = "")
}

private class FakeItemRepository : ItemRepository {

    private val items = MutableStateFlow<List<Item>>(emptyList())
    var refreshCount = 0
        private set

    fun setItems(value: List<Item>) {
        items.value = value
    }

    override fun observeItems(): Flow<List<Item>> = items

    override fun observeItem(id: Long): Flow<Item?> =
        items.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun refreshItems() {
        refreshCount++
    }
}
