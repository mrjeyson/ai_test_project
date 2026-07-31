package com.example.test_ai_project.network.api

import com.example.test_ai_project.network.di.Tmdb
import com.example.test_ai_project.network.dto.MoviePageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The TMDB endpoints the app uses.
 *
 * A class over an [HttpClient] rather than an annotated interface: Ktor builds requests in
 * ordinary code instead of generating them from reflection, so each endpoint is a suspending
 * function that says what it sends. The signatures keep the shape a Retrofit interface gave —
 * one suspending call per endpoint, returning a DTO — and no Ktor type appears in any of them,
 * so a caller still sees nothing but the provider's data.
 *
 * Paths are relative and have to stay that way. The base URL is attached by `defaultRequest`
 * on the injected client, and Ktor merges only a relative path onto it: a leading `/` would
 * discard the base's own path segments and a full URL would discard the host.
 *
 * Authentication is not expressed here: the credential is attached by a client plugin, so no
 * call site can forget it and no signature carries it.
 */
@Singleton
class TmdbApi @Inject constructor(
    @Tmdb private val client: HttpClient,
) {

    /**
     * One page of the popular-movies list, 20 titles per page.
     *
     * TMDB refuses [page] above 500 with a 400, whatever `total_pages` claims — see
     * [MAX_PAGE], which callers clamp against.
     */
    suspend fun getPopularMovies(
        page: Int,
        language: String = DEFAULT_LANGUAGE,
    ): MoviePageDto = client.get("movie/popular") {
        parameter("page", page)
        parameter("language", language)
    }.body()

    companion object {
        /** TMDB's hard ceiling on the `page` parameter for list endpoints. */
        const val MAX_PAGE = 500

        const val DEFAULT_LANGUAGE = "en-US"
    }
}
