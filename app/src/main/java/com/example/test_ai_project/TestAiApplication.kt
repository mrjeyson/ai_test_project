package com.example.test_ai_project

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt's root. Every `@InstallIn(SingletonComponent::class)` module across all
 * modules is aggregated into the graph generated from this class.
 */
@HiltAndroidApp
class TestAiApplication : Application()
