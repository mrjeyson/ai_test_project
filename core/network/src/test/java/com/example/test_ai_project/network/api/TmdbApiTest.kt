package com.example.test_ai_project.network.api

import com.example.test_ai_project.network.di.NetworkModule
import com.example.test_ai_project.network.testing.MockBackend
import com.example.test_ai_project.network.testing.respondJson
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Building a request in code rather than declaring it in annotations means the request is a thing
 * that can be got wrong, so it is a thing worth asserting: the path is relative, so it lands
 * *under* the base URL's `/3` rather than replacing it, and every parameter the provider needs is
 * actually attached.
 */
class TmdbApiTest {

    @Test
    fun `popular movies resolves under the versioned base path`() = runTest {
        val backend = MockBackend { respondJson(POPULAR_PAGE) }
        val api = TmdbApi(backend.client(NetworkModule.TMDB_BASE_URL))

        api.getPopularMovies(page = 3)

        assertThat(backend.request.url.toString())
            .isEqualTo("https://api.themoviedb.org/3/movie/popular?page=3&language=en-US")
    }

    @Test
    fun `language is overridable and defaults to en-US`() = runTest {
        val backend = MockBackend { respondJson(POPULAR_PAGE) }
        val api = TmdbApi(backend.client(NetworkModule.TMDB_BASE_URL))

        api.getPopularMovies(page = 1, language = "uz-UZ")

        assertThat(backend.request.url.parameters["language"]).isEqualTo("uz-UZ")
    }

    @Test
    fun `the response body is deserialized into the page DTO`() = runTest {
        val backend = MockBackend { respondJson(POPULAR_PAGE) }
        val api = TmdbApi(backend.client(NetworkModule.TMDB_BASE_URL))

        val page = api.getPopularMovies(page = 3)

        assertThat(page.page).isEqualTo(3)
        assertThat(page.totalPages).isEqualTo(41_000)
        assertThat(page.results.single().title).isEqualTo("Arrival")
    }

    /**
     * A field the DTOs do not model, and one they model as absent. Both have to survive: the
     * `Json` in [NetworkModule.providesJson] is what stops a shipped client crashing on the day
     * TMDB adds a key, and this is the test that fails if that configuration ever stops reaching
     * the content-negotiation plugin.
     */
    @Test
    fun `unknown keys and omitted fields do not fail the call`() = runTest {
        val backend = MockBackend { respondJson(POPULAR_PAGE_WITH_SURPRISES) }
        val api = TmdbApi(backend.client(NetworkModule.TMDB_BASE_URL))

        val movie = api.getPopularMovies(page = 1).results.single()

        assertThat(movie.id).isEqualTo(329_865L)
        assertThat(movie.posterPath).isNull()
        assertThat(movie.voteAverage).isEqualTo(0.0)
    }

    private companion object {
        const val POPULAR_PAGE = """
            {
              "page": 3,
              "results": [
                {"id": 329865, "title": "Arrival", "vote_average": 7.6}
              ],
              "total_pages": 41000,
              "total_results": 820000
            }
        """

        const val POPULAR_PAGE_WITH_SURPRISES = """
            {
              "page": 1,
              "results": [
                {"id": 329865, "title": "Arrival", "a_field_tmdb_added_later": true}
              ],
              "total_pages": 1,
              "total_results": 1
            }
        """
    }
}
