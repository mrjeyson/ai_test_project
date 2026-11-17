package com.example.test_ai_project.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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






















