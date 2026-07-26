package com.example.test_ai_project.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.test_ai_project.core.database.entity.PrayerDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimesDao {

    /**
     * Today and tomorrow in one query, as a stream.
     *
     * A range rather than two reads because the screen needs both to answer "what is next"
     * across midnight, and two independent flows would let the UI render a moment where one
     * had updated and the other had not.
     *
     * `LIMIT 2` bounds the result even if pruning has fallen behind — the schedule never
     * wants more than the day after next.
     */
    @Query("SELECT * FROM prayer_days WHERE date >= :fromDate ORDER BY date ASC LIMIT 2")
    fun observeFrom(fromDate: String): Flow<List<PrayerDayEntity>>

    /**
     * The same window, read once.
     *
     * A one-shot read, not a [Flow], because the callers are an alarm receiver and a boot
     * receiver: both run outside any UI, do their work, and stop. Observing would keep a
     * database connection alive in a process that is about to be killed.
     */
    @Query("SELECT * FROM prayer_days WHERE date >= :fromDate ORDER BY date ASC LIMIT 2")
    suspend fun daysFrom(fromDate: String): List<PrayerDayEntity>

    /** Null when that date has never been cached. Drives the refetch decision. */
    @Query("SELECT * FROM prayer_days WHERE date = :date")
    suspend fun day(date: String): PrayerDayEntity?

    @Upsert
    suspend fun upsertDays(days: List<PrayerDayEntity>)

    /**
     * Drops days that have already passed.
     *
     * Nothing reads them — the schedule window starts at today — and without this the table
     * would grow by a row a day forever, which is a slow leak rather than a cache.
     */
    @Query("DELETE FROM prayer_days WHERE date < :date")
    suspend fun deleteBefore(date: String)
}
