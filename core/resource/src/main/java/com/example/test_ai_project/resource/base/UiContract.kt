package com.example.test_ai_project.resource.base

/**
 * The three halves of a screen's contract. Each screen declares its own trio in a
 * `contract/` package, which is what makes a screen readable without opening its
 * ViewModel: the state is everything rendered, the events are everything the user can do,
 * and the effects are everything that happens once and is not part of the state.
 */

/** Everything a screen renders. Always a data class, so `copy` is the only way to change it. */
interface UiState

/** Something the user did. Handled by the ViewModel's `onEvent`. */
interface UiEvent

/**
 * A one-shot side effect: navigation, a toast, a keyboard dismissal.
 *
 * Deliberately not part of [UiState] — a navigation stored in state replays on the next
 * recomposition or configuration change, which is how a screen ends up navigating twice.
 */
interface UiEffect

/** For a screen with no user input, so its contract does not need an empty sealed class. */
data object NoEvent : UiEvent

/** For a screen that never emits an effect. */
data object NoEffect : UiEffect
