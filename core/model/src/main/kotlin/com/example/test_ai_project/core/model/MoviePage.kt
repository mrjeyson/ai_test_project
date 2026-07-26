package com.example.test_ai_project.core.model

/**
 * One page of the movie catalogue, with enough context to render a page control.
 *
 * [totalPages] travels with the movies rather than being fetched separately because the
 * page control has to work offline too: a cached page number that the UI cannot bound is
 * a page control that cannot be drawn.
 */
data class MoviePage(
    val page: Int,
    val totalPages: Int,
    val movies: List<Movie>,
) {
    companion object {
        const val FIRST_PAGE = 1

        /** What the UI shows before anything has ever been cached. */
        fun empty(page: Int = FIRST_PAGE) = MoviePage(
            page = page,
            totalPages = 0,
            movies = emptyList(),
        )
    }
}
