package com.example.test_ai_project.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.test_ai_project.database.entity.MovieCatalogEntity
import com.example.test_ai_project.database.entity.MovieEntity
import com.example.test_ai_project.database.entity.MoviePageEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    /**
     * The films on [page], in the order the API returned them.
     *
     * Ordering by the join table's `position` and not by anything on the film itself is
     * the point: "popular" is an order the server decides, and the cache has to reproduce
     * it rather than invent one.
     */
    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_page_entries ON movie_page_entries.movieId = movies.id
        WHERE movie_page_entries.page = :page
        ORDER BY movie_page_entries.position ASC
        """,
    )
    fun observePage(page: Int): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movie_catalog WHERE id = ${MovieCatalogEntity.SINGLETON_ID}")
    fun observeCatalog(): Flow<MovieCatalogEntity?>

    /** Null when [page] has never been cached. Drives the refetch decision. */
    @Query("SELECT MIN(fetchedAtEpochMillis) FROM movie_page_entries WHERE page = :page")
    suspend fun pageFetchedAt(page: Int): Long?

    @Upsert
    suspend fun upsertMovies(movies: List<MovieEntity>)

    @Upsert
    suspend fun upsertPageEntries(entries: List<MoviePageEntryEntity>)

    @Upsert
    suspend fun upsertCatalog(catalog: MovieCatalogEntity)

    @Query("DELETE FROM movie_page_entries WHERE page = :page")
    suspend fun deletePageEntries(page: Int)

    /**
     * Replaces one page's contents atomically.
     *
     * Only that page's *entries* are deleted, never the films: another cached page may
     * reference the same film, and the shared [MovieEntity] rows are upserted so a refetch
     * updates ratings in place instead of churning rows.
     *
     * The transaction is what stops a collector of [observePage] seeing the gap between
     * the delete and the insert.
     */
    @Transaction
    suspend fun replacePage(
        page: Int,
        movies: List<MovieEntity>,
        entries: List<MoviePageEntryEntity>,
        catalog: MovieCatalogEntity,
    ) {
        upsertMovies(movies)
        deletePageEntries(page)
        upsertPageEntries(entries)
        upsertCatalog(catalog)
    }
}
