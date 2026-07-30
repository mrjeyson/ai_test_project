package com.example.test_ai_project.network.api

import com.example.test_ai_project.network.dto.MoviePageDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * The TMDB endpoints the app uses.
 *
 * Authentication is not expressed here: the bearer token is attached by an interceptor on
 * the TMDB client, so no call site can forget it and no signature carries a credential.
 */
interface TmdbApi {

    /**
     * One page of the popular-movies list, 20 titles per page.
     *
     * TMDB refuses [page] above 500 with a 400, whatever `total_pages` claims — see
     * [MAX_PAGE], which callers clamp against.
     */
    @GET("movie/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int,
        @Query("language") language: String = DEFAULT_LANGUAGE,
    ): MoviePageDto

    companion object {
        /** TMDB's hard ceiling on the `page` parameter for list endpoints. */
        const val MAX_PAGE = 500

        const val DEFAULT_LANGUAGE = "en-US"
    }
}
