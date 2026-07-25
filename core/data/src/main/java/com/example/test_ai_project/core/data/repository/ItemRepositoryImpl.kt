package com.example.test_ai_project.core.data.repository

import com.example.test_ai_project.core.common.dispatcher.AppDispatcher
import com.example.test_ai_project.core.common.dispatcher.Dispatcher
import com.example.test_ai_project.core.data.mapper.toDomain
import com.example.test_ai_project.core.data.mapper.toEntity
import com.example.test_ai_project.core.database.dao.ItemDao
import com.example.test_ai_project.core.database.entity.ItemEntity
import com.example.test_ai_project.core.domain.repository.ItemRepository
import com.example.test_ai_project.core.model.Item
import com.example.test_ai_project.core.network.api.ItemApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Offline-first: reads always come from Room, so the UI has data before — and
 * without — a network call. [refreshItems] is the only path that touches the API.
 */
@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val itemApi: ItemApi,
    @param:Dispatcher(AppDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ItemRepository {

    override fun observeItems(): Flow<List<Item>> =
        itemDao.observeAll().map { entities -> entities.map(ItemEntity::toDomain) }

    override fun observeItem(id: Long): Flow<Item?> =
        itemDao.observeById(id).map { entity -> entity?.toDomain() }

    override suspend fun refreshItems() = withContext(ioDispatcher) {
        val remote = itemApi.getItems()
        itemDao.replaceAll(remote.map { it.toEntity() })
    }
}
