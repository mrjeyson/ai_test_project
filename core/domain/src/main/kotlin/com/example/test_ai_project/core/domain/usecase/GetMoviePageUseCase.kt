package com.example.test_ai_project.core.domain.usecase

import com.example.test_ai_project.core.domain.repository.MovieRepository
import com.example.test_ai_project.core.model.MoviePage
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Observes one page of the catalogue from the local cache.
 *
 * Thin by design — the ordering that matters here is the server's, reproduced by the
 * cache, so there is nothing for this to re-sort.
 */
class GetMoviePageUseCase @Inject constructor(
    private val movieRepository: MovieRepository,
) {
    operator fun invoke(page: Int): Flow<MoviePage> = movieRepository.observePage(page)
}
