package com.example.test_ai_project.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One cached day of prayer times — the row that makes the tab work with no network.
 *
 * The five instants are five columns rather than rows in a child table, because the
 * relationship cannot vary: a day has exactly five prayers, and a schema that allows four
 * or six is a schema that has to be validated on every read. Five columns make a partial
 * day unrepresentable, and reduce the whole screen to a single-row query.
 *
 * They are stored as epoch millis rather than "16:55" so that nothing downstream has to
 * re-apply a time zone. [zoneId] is kept anyway, for *formatting* — the instants are
 * absolute, but rendering them as the local clock of the place they belong to needs the
 * zone the API computed them in.
 *
 * [latitude]/[longitude] are stored so the repository can tell a still-valid cached day
 * from one belonging to a city the user has since left. Without them, a cached row is
 * indistinguishable from a correct one.
 */
@Entity(tableName = "prayer_days")
data class PrayerDayEntity(
    /**
     * ISO `yyyy-MM-dd`.
     *
     * A text key rather than three integer columns, because it is the only form that is
     * both a primary key and correctly ordered by a plain `ORDER BY` — which is what the
     * pruning query and the two-day schedule read depend on.
     */
    @PrimaryKey val date: String,
    val latitude: Double,
    val longitude: Double,
    val zoneId: String,
    val locationLabel: String?,
    val fajrEpochMillis: Long,
    val dhuhrEpochMillis: Long,
    val asrEpochMillis: Long,
    val maghribEpochMillis: Long,
    val ishaEpochMillis: Long,
    val fetchedAtEpochMillis: Long,
)
