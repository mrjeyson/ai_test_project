package com.example.test_ai_project.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One movie as TMDB returns it from a list endpoint.
 *
 * Every field except `id` has a default. TMDB omits rather than nulls a good deal of this
 * — `poster_path` on a film with no artwork, `release_date` on an unscheduled one — and a
 * missing field must degrade the card, not fail the whole page.
 */
@Serializable
data class MovieDto(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String = "",
    @SerialName("overview") val overview: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    // ISO-8601 date, e.g. "2024-03-01". Occasionally an empty string for unreleased films.
    @SerialName("release_date") val releaseDate: String? = null,
)

/** The envelope every paginated TMDB list endpoint returns. */
@Serializable
data class MoviePageDto(
    @SerialName("page") val page: Int = 1,
    @SerialName("results") val results: List<MovieDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
    @SerialName("total_results") val totalResults: Int = 0,
)
