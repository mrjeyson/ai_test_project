package com.example.test_ai_project.home.data.service

import com.example.test_ai_project.home.data.mapper.toDomain
import com.example.test_ai_project.home.data.mapper.toEntity
import com.example.test_ai_project.database.dao.MovieDao
import com.example.test_ai_project.database.entity.MovieCatalogEntity
import com.example.test_ai_project.database.entity.MovieEntity
import com.example.test_ai_project.database.entity.MoviePageEntryEntity
import com.example.test_ai_project.home.domain.exception.MovieServiceNotConfiguredException
import com.example.test_ai_project.home.domain.service.MovieService
import com.example.test_ai_project.home.domain.service.TimeProvider
import com.example.test_ai_project.home.domain.model.MoviePage
import com.example.test_ai_project.network.api.TmdbApi
import com.example.test_ai_project.network.interceptor.TmdbNotConfiguredException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Offline-first, in the strict sense: Room is the only thing the UI ever reads from, and
 * the network is a process that writes into Room. There is no code path where a failed
 * request produces an empty screen — the screen is rendering the cache either way.
 */
@Singleton
class DefaultMovieService @Inject constructor(
    private val movieDao: MovieDao,
    private val tmdbApi: TmdbApi,
    private val timeProvider: TimeProvider,
) : MovieService {

    override fun observePage(page: Int): Flow<MoviePage> =
        combine(
            movieDao.observePage(page),
            movieDao.observeCatalog(),
        ) { entities, catalog ->
            MoviePage(
                page = page,
                // Zero until the first successful fetch — which the UI reads as "no page
                // control yet", the honest answer on a cold offline start.
                totalPages = catalog?.totalPages ?: 0,
                movies = entities.map(MovieEntity::toDomain),
            )
        }

    override suspend fun refreshPage(page: Int, force: Boolean) = withContext(Dispatchers.IO) {
        @Suppress("NAME_SHADOWING")
        val page = page.coerceAtLeast(MoviePage.FIRST_PAGE)
        if (!force && isFresh(page)) return@withContext

        // Translated at the layer boundary. This module is the only one that knows the
        // provider is TMDB, so it is the only one that can turn the provider's exception
        // into something the domain — and the screen above it — can act on.
        val response = try {
            tmdbApi.getPopularMovies(page = page)
        } catch (notConfigured: TmdbNotConfiguredException) {
            throw MovieServiceNotConfiguredException(
                message = notConfigured.message.orEmpty(),
                cause = notConfigured,
            )
        }
        val fetchedAt = timeProvider.nowEpochMillis()

        movieDao.replacePage(
            page = page,
            movies = response.results.map { it.toEntity() },
            entries = response.results.mapIndexed { index, movie ->
                MoviePageEntryEntity(
                    page = page,
                    position = index,
                    movieId = movie.id,
                    fetchedAtEpochMillis = fetchedAt,
                )
            },
            catalog = MovieCatalogEntity(
                // Clamped here, at the one place that knows which provider answered.
                // TMDB reports tens of thousands of pages and then rejects any request
                // past 500, so the unclamped number would build a page control whose
                // later half only produces errors.
                totalPages = response.totalPages.coerceAtMost(TmdbApi.MAX_PAGE),
                totalResults = response.totalResults,
            ),
        )
    }

    /**
     * A page already fetched inside [CACHE_TTL] is left alone.
     *
     * Without this, revisiting a page — or just switching away from the tab and back —
     * would re-download it, which is slow on a poor connection and pointless on a list
     * that turns over daily.
     */
    private suspend fun isFresh(page: Int): Boolean {
        val fetchedAt = movieDao.pageFetchedAt(page) ?: return false
        return timeProvider.nowEpochMillis() - fetchedAt < CACHE_TTL.inWholeMilliseconds
    }

    private companion object {
        /**
         * Long enough that ordinary browsing never refetches, short enough that the list
         * is not visibly stale a day later. The popular list itself moves slowly.
         */
        val CACHE_TTL = 6.hours
    }
}
