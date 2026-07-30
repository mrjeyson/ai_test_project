package com.example.test_ai_project.resource.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * The MVI base every ViewModel in the app extends.
 *
 * State goes out as a [StateFlow] — always has a current value, so a screen can render on
 * first composition without a null check. Effects go out through a [Channel], not a
 * `StateFlow`, and the difference is the whole point: a channel delivers each effect
 * exactly once to exactly one collector, so a rotation does not re-navigate and a
 * re-collection does not re-show a toast.
 *
 * [Channel.BUFFERED] rather than `RENDEZVOUS`, so [sendEffect] never suspends waiting for
 * the screen to be listening — an effect emitted while the UI is backgrounded is held
 * until it comes back rather than blocking the ViewModel.
 */
abstract class BaseViewModel<State : UiState, Event : UiEvent, Effect : UiEffect>(
    initialState: State,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)

    /**
     * `open` for the screens whose state is *derived* rather than reduced.
     *
     * Most ViewModels leave this alone and drive it with [setState]. A screen that folds
     * several cold sources together — a cache, a clock — overrides it with its own
     * `combine(...).stateIn(viewModelScope, WhileSubscribed(...), ...)`, which is what keeps
     * a ticking clock from running while nothing is watching the screen.
     */
    open val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    /** The current state, for the reads that do not want to collect. */
    protected val currentState: State get() = _uiState.value

    /** The only way state changes: `setState { copy(isLoading = true) }`. */
    protected fun setState(reducer: State.() -> State) {
        _uiState.update(reducer)
    }

    /** Emits a one-shot [Effect]. Non-suspending, because [Channel.BUFFERED] accepts it. */
    protected fun sendEffect(effect: Effect) {
        _effects.trySend(effect)
    }

    /**
     * Handles a user [Event]. Screens with no events use [NoEvent] and leave this alone.
     */
    open fun onEvent(event: Event) = Unit
}
