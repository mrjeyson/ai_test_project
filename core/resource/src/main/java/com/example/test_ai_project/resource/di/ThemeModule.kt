package com.example.test_ai_project.resource.di

import com.example.test_ai_project.resource.theme.DefaultThemeService
import com.example.test_ai_project.resource.theme.ThemeService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the theme contract to the stored-preference implementation.
 *
 * The design system's own binding, and the only one it has. Unlike a feature's data module
 * this is not `runtimeOnly` anywhere — every consumer already compiles against this module
 * — but the shape is the same: callers see [ThemeService] and never the `Context` behind it.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class ThemeModule {

    @Binds
    @Singleton
    internal abstract fun bindsThemeService(implementation: DefaultThemeService): ThemeService
}
