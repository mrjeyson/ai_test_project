package com.example.test_ai_project.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the bootstrap sequence and reports progress; it does not know that a splash
 * screen is what renders it, and it never navigates — reaching [SplashUiState.Ready] is
 * the only signal the UI gets.
 *
 * Each stage is held on screen for at least [STAGE_MIN_DURATION_MILLIS]. Without a floor,
 * a warm start would flash the wordmark for two frames, which reads as a glitch rather
 * than as launching.
 */
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(
        SplashUiState.Initializing(stage = BootstrapStage.entries.first(), progress = 0f),
    )
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { bootstrap() }
    }

    private suspend fun bootstrap() {
        val stages = BootstrapStage.entries
        stages.forEachIndexed { index, stage ->
            _uiState.value = SplashUiState.Initializing(
                stage = stage,
                progress = index.toFloat() / stages.size,
            )

            // Where real work belongs: open the Room database, unlock the keystore,
            // restore the session. Each is a suspend call awaited here, so the caption
            // above always names the step actually running.
            delay(STAGE_MIN_DURATION_MILLIS)

            _uiState.value = SplashUiState.Initializing(
                stage = stage,
                progress = (index + 1).toFloat() / stages.size,
            )
        }

        _uiState.value = SplashUiState.Ready
    }

    internal companion object {
        const val STAGE_MIN_DURATION_MILLIS = 600L
    }
}
