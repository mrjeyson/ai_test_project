package com.example.test_ai_project.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The cached film itself, keyed by its TMDB id and stored exactly once however many pages
 * or lists it turns up in.
 *
 * Note that the *raw* poster path is stored, not a URL: the CDN host and the width variant
 * are rendering decisions, and baking them into the cache would mean a schema migration to
 * change a thumbnail size.
 */
@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val releaseDate: String?,
)

/**
 * Which films sit on which page, and in what order.
 *
 * Split from [MovieEntity] rather than adding a `page` column to it, because the two facts
 * have different lifetimes and different cardinality: a film is one row forever, while its
 * position in the popular list changes daily and the same film can appear on two pages
 * mid-reshuffle. Folding them together would make one of those cases corrupt the other.
 */
@Entity(
    tableName = "movie_page_entries",
    primaryKeys = ["page", "position"],
    indices = [Index("movieId")],
)
data class MoviePageEntryEntity(
    val page: Int,
    /** Index within the page, preserving the order the API returned. */
    val position: Int,
    val movieId: Long,
    /** When this page was last written, used to decide whether a refetch is due. */
    val fetchedAtEpochMillis: Long,
)

/**
 * Catalogue-wide facts — currently just how many pages exist.
 *
 * A single row, pinned to [SINGLETON_ID]. Cached rather than held in memory so the page
 * control can be drawn on a cold, offline start.
 */
@Entity(tableName = "movie_catalog")
data class MovieCatalogEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val totalPages: Int,
    val totalResults: Int,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
