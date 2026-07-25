package com.example.test_ai_project.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test_ai_project.core.common.result.DataResult
import com.example.test_ai_project.core.common.result.asResult
import com.example.test_ai_project.core.domain.usecase.GetItemsUseCase
import com.example.test_ai_project.core.domain.usecase.RefreshItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Depends only on use cases from `:core:domain` — it has no idea whether the data
 * came from Room, Retrofit, or a fake.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getItems: GetItemsUseCase,
    private val refreshItems: RefreshItemsUseCase,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = getItems()
        .asResult()
        .map { result ->
            when (result) {
                is DataResult.Loading -> HomeUiState.Loading
                is DataResult.Success -> HomeUiState.Success(result.data)
                is DataResult.Error -> HomeUiState.Error(
                    result.throwable.message ?: "Could not load items.",
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            // Survives short-lived config changes without re-querying.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // A failed refresh must not take down the screen — the cached list is
            // still valid, so the error is surfaced separately from [uiState].
            runCatching { refreshItems() }
                .onFailure { _errorMessage.value = it.message ?: "Refresh failed." }
                .onSuccess { _errorMessage.value = null }
            _isRefreshing.value = false
        }
    }

    fun consumeErrorMessage() {
        _errorMessage.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
