package com.example.test_ai_project.home.domain.service

import com.example.test_ai_project.home.domain.model.MoviePage
import kotlinx.coroutines.flow.Flow

/**
 * The paged movie catalogue.
 *
 * Read and refresh are deliberately separate calls rather than one "load" — the grid is
 * rendered from cache and stays rendered while a refresh runs behind it, which is what
 * makes the tab work with the radio off.
 */
interface MovieService {

    fun observePage(page: Int): Flow<MoviePage>

    /**
     * @param force refetches even when the cached page is still considered fresh, for a
     *   pull-to-refresh the user explicitly asked for.
     */
    suspend fun refreshPage(page: Int, force: Boolean = false)
}
