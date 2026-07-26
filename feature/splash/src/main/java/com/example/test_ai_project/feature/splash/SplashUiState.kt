package com.example.test_ai_project.feature.splash

/**
 * Everything the splash screen needs to render, and nothing else.
 *
 * [progress] is carried in state rather than derived in the composable so the bar and
 * the caption can never disagree about how far bootstrap has got.
 */
sealed interface SplashUiState {

    data class Initializing(
        val stage: BootstrapStage,
        val progress: Float,
    ) : SplashUiState

    data object Ready : SplashUiState
}

/**
 * The ordered bootstrap steps. Declaration order *is* execution order, and the enum
 * size is what the progress fraction is measured against — adding a step needs no
 * other change.
 */
enum class BootstrapStage {
    LocalEnvironment,
    SecureStorage,
    Session,
}
