package com.example.test_ai_project.network.api

import com.example.test_ai_project.network.dto.PrayerTimingsResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The Aladhan prayer-time endpoints the app uses.
 *
 * Chosen over TMDB-style key authentication because it needs none: a build with no
 * credentials configured still shows real prayer times, which matters for a screen whose
 * whole promise is that it keeps working when other things do not.
 *
 * One day per request, rather than the provider's month-at-a-time `/calendar` endpoint.
 * The caller fetches exactly the two days the schedule spans, so a month response would be
 * fifteen times the payload for data that is discarded on the next visit — and the two-day
 * window still has to be requested across a month boundary anyway.
 */
interface AladhanApi {

    /**
     * Prayer times for one date at one place.
     *
     * @param date `DD-MM-YYYY`. The provider's own format, and not negotiable — a path
     *   segment it cannot parse is answered with times for *today* rather than an error,
     *   which is the worst possible failure mode for a cache keyed by date.
     */
    @GET("v1/timings/{date}")
    suspend fun getTimings(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int = METHOD_MUSLIM_WORLD_LEAGUE,
        @Query("school") school: Int = SCHOOL_HANAFI,
    ): PrayerTimingsResponseDto

    companion object {
        /**
         * Muslim World League: an 18° dawn angle and a 17° dusk angle.
         *
         * Sent explicitly rather than omitted. Left out, the provider picks an authority
         * from the coordinates, which means the same device produces different Fajr times
         * either side of a border — and a user who has memorised their local timetable
         * sees the app disagree with it for no visible reason.
         */
        const val METHOD_MUSLIM_WORLD_LEAGUE = 3

        /**
         * The Hanafi position on Asr: shadow length twice the object's, rather than once.
         *
         * A roughly hour-later Asr, and the convention across Central and South Asia and
         * Turkey. It is a school of thought rather than a calculation detail, so it is
         * named here rather than buried as a bare `1`.
         */
        const val SCHOOL_HANAFI = 1
    }
}
