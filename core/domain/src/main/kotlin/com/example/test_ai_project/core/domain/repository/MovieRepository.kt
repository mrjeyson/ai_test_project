package com.example.test_ai_project.core.domain.repository

import com.example.test_ai_project.core.model.MoviePage
import kotlinx.coroutines.flow.Flow

/**
 * The movie catalogue, as the rest of the app sees it.
 *
 * The split between [observePage] and [refreshPage] is the offline-first contract, and it
 * is deliberate that only one of them can fail. Reading is a local operation that always
 * succeeds, returning whatever is cached — possibly nothing. Fetching is separate, and a
 * caller that ignores its failure still has a screen full of content.
 */
interface MovieRepository {

    /**
     * The cached contents of [page], re-emitted whenever the cache changes.
     *
     * Never throws and never completes: an uncached page emits an empty [MoviePage] and
     * then fills in if and when [refreshPage] succeeds.
     */
    fun observePage(page: Int): Flow<MoviePage>

    /**
     * Fetches [page] and writes it to the cache.
     *
     * Skips the network when the page is already cached and still fresh, unless [force] is
     * set — which is what a user-initiated refresh passes.
     *
     * @throws java.io.IOException if the device is offline or the request fails.
     */
    suspend fun refreshPage(page: Int, force: Boolean = false)
}
