package com.example.test_ai_project.core.data.di

import com.example.test_ai_project.core.data.repository.ItemRepositoryImpl
import com.example.test_ai_project.core.domain.repository.ItemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds domain interfaces to their data-layer implementations.
 *
 * This is the seam that lets a feature be tested against a fake repository, and
 * lets Room/Retrofit be replaced without any consumer changing.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    internal abstract fun bindsItemRepository(
        implementation: ItemRepositoryImpl,
    ): ItemRepository
}
