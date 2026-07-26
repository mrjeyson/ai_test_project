package com.example.test_ai_project.feature.home.movies

import androidx.annotation.StringRes
import com.example.test_ai_project.core.model.Movie
import com.example.test_ai_project.core.model.MoviePage

/**
 * Everything the Movies page renders.
 *
 * A data class with flags, not the sealed `Loading | Success | Error` hierarchy the other
 * screens use — and that is the whole point of an offline-first screen. Those states are
 * mutually exclusive; these are not. The normal case here is *cached movies* and *a fetch
 * in flight* and *the last fetch having failed*, all at once, and a sealed hierarchy could
 * only express that by throwing away two of the three.
 */
data class MoviesUiState(
    val page: Int = MoviePage.FIRST_PAGE,
    val totalPages: Int = 0,
    val movies: List<Movie> = emptyList(),
    /** A fetch is in flight. Cached content stays on screen while it runs. */
    val isLoading: Boolean = false,
    /** Non-null when the last fetch failed. Advisory: content is still whatever is cached. */
    @param:StringRes val messageRes: Int? = null,
) {
    /** Nothing cached and nothing in flight — the only genuinely empty state. */
    val isEmpty: Boolean get() = movies.isEmpty() && !isLoading

    /** No cache to fall back on, so the spinner gets the whole screen rather than a strip. */
    val isInitialLoad: Boolean get() = movies.isEmpty() && isLoading

    /** Below two pages there is nothing to navigate between. */
    val isPagingVisible: Boolean get() = totalPages > 1
}
