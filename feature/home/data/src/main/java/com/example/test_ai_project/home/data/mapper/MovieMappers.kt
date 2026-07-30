package com.example.test_ai_project.home.data.mapper

import com.example.test_ai_project.database.entity.MovieEntity
import com.example.test_ai_project.home.domain.model.Movie
import com.example.test_ai_project.network.dto.MovieDto

/**
 * Translation between the three representations of a movie.
 *
 * `internal`, like the item mappers: nothing outside `:core:data` should know that
 * `MovieEntity` or `MovieDto` exist — and in particular, nothing outside this module
 * should know what TMDB's image CDN looks like.
 */
internal fun MovieDto.toEntity(): MovieEntity = MovieEntity(
    id = id,
    title = title,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    voteAverage = voteAverage,
    voteCount = voteCount,
    releaseDate = releaseDate,
)

internal fun MovieEntity.toDomain(): Movie = Movie(
    id = id,
    title = title,
    overview = overview,
    posterUrl = posterPath?.toTmdbImageUrl(TmdbImageSize.POSTER),
    backdropUrl = backdropPath?.toTmdbImageUrl(TmdbImageSize.BACKDROP),
    voteAverage = voteAverage,
    voteCount = voteCount,
    releaseYear = releaseDate.toReleaseYear(),
)

/**
 * TMDB gives a date as `yyyy-MM-dd`, and gives an empty string for a film with no
 * announced date.
 *
 * Parsed by taking the leading four characters rather than with a date formatter: the year
 * is all the UI shows, and a formatter here would mean a time zone, a locale, and an
 * exception path for a value this shape does not need any of.
 */
private fun String?.toReleaseYear(): Int? = this
    ?.take(YEAR_LENGTH)
    ?.toIntOrNull()

private const val YEAR_LENGTH = 4

/**
 * Width variants from TMDB's `configuration` endpoint.
 *
 * Hard-coded rather than fetched: these have been stable for the life of the v3 API, and
 * an extra network round trip before the first poster can render is a poor trade for
 * tracking a list that does not change.
 */
private object TmdbImageSize {
    /** Roughly 2× a half-width phone poster, so it stays sharp on xxhdpi. */
    const val POSTER = "w500"
    const val BACKDROP = "w780"
}

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

/** [this] is TMDB's leading-slash relative path, e.g. `/abc123.jpg`. */
private fun String.toTmdbImageUrl(size: String): String = "$TMDB_IMAGE_BASE_URL$size$this"
