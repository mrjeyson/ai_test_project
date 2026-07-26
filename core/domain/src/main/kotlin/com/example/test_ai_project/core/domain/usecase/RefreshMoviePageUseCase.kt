package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.MovieRepository
import com.example.test_ai_project.core.model.MoviePage
import javax.inject.Inject

/**
 * Brings one page of the catalogue up to date.
 *
 * @throws java.io.IOException if the device is offline or the request fails. Callers are
 *   expected to catch it and keep showing the cache.
 */
class RefreshMoviePageUseCase @Inject constructor(
    private val movieRepository: MovieRepository,
) {
    suspend operator fun invoke(page: Int, force: Boolean = false) {
        // Guards the lower bound only. The provider's ceiling is the provider's business:
        // the repository already reports a clamped `totalPages`, so a page control built
        // from it cannot produce a number the provider would reject.
        movieRepository.refreshPage(
            page = page.coerceAtLeast(MoviePage.FIRST_PAGE),
            force = force,
        )
    }
}
