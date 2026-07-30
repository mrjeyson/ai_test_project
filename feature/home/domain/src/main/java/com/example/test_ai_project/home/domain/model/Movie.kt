package com.example.test_ai_project.home.domain.model

/**
 * A film, as the app understands one.
 *
 * [posterUrl] is a finished URL rather than the provider's relative path. Which CDN it
 * came from and at what width is a data-layer decision, and keeping it there means this
 * model — and every screen reading it — stays independent of TMDB.
 *
 * [releaseYear] and [posterUrl] are nullable because the source genuinely omits them:
 * a film can be unscheduled, and an obscure one can have no artwork.
 */
data class Movie(
    val id: Long,
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    /** Average user score out of 10. */
    val voteAverage: Double,
    val voteCount: Int,
    val releaseYear: Int?,
)
