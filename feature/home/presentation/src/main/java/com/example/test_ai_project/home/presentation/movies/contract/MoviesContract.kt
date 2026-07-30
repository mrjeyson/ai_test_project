package com.example.test_ai_project.home.presentation.movies.contract

import androidx.annotation.StringRes
import com.example.test_ai_project.home.domain.model.Movie
import com.example.test_ai_project.home.domain.model.MoviePage
import com.example.test_ai_project.resource.base.UiEvent
import com.example.test_ai_project.resource.base.UiState

/**
 * Everything the Movies page renders.
 *
 * A data class with flags, not the sealed `Loading | Success | Error` hierarchy the other
 * screens use — and that is the whole point of an offline-first screen. Those states are
 * mutually exclusive; these are not. The normal case here is *cached movies* and *a fetch
 * in flight* and *the last fetch having failed*, all at once, and a sealed hierarchy could
 * only express that by throwing away two of the three.
 */
data class MoviesState(
    val page: Int = MoviePage.FIRST_PAGE,
    val totalPages: Int = 0,
    val movies: List<Movie> = emptyList(),
    /** A fetch is in flight. Cached content stays on screen while it runs. */
    val isLoading: Boolean = false,
    /** Non-null when the last fetch failed. Advisory: content is still whatever is cached. */
    @param:StringRes val messageRes: Int? = null,
) : UiState {
    /** Nothing cached and nothing in flight — the only genuinely empty state. */
    val isEmpty: Boolean get() = movies.isEmpty() && !isLoading

    /** No cache to fall back on, so the spinner gets the whole screen rather than a strip. */
    val isInitialLoad: Boolean get() = movies.isEmpty() && isLoading

    /** Below two pages there is nothing to navigate between. */
    val isPagingVisible: Boolean get() = totalPages > 1
}

sealed interface MoviesEvent : UiEvent {
    /** A page number tapped in the page control. */
    data class PageSelected(val page: Int) : MoviesEvent

    /** Refetch the current page, bypassing the cache's freshness window. */
    data object RefreshRequested : MoviesEvent

    data object RetryRequested : MoviesEvent

    data object MessageDismissed : MoviesEvent
}
