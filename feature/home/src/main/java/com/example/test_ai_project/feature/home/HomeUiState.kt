package com.example.test_ai_project.feature.home

import com.example.test_ai_project.core.model.Item

/**
 * Everything the Home screen needs to render, and nothing else.
 *
 * A sealed hierarchy rather than a single class with nullable fields, so the
 * composable's `when` is exhaustive and impossible states cannot be expressed.
 */
sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class Success(val items: List<Item>) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
