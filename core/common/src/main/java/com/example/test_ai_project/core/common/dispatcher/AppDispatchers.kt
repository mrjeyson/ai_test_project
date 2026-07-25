package com.example.test_ai_project.core.common.dispatcher

import javax.inject.Qualifier

/**
 * Marks *which* dispatcher is being injected.
 *
 * Injecting dispatchers instead of referencing [kotlinx.coroutines.Dispatchers]
 * directly is what makes repositories and use cases testable — a test can swap in
 * a `TestDispatcher` without touching production code.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: AppDispatcher)

enum class AppDispatcher {
    Default,
    IO,
}
