package com.example.test_ai_project.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Aladhan's `/timings` envelope.
 *
 * The provider wraps every response in `{code, status, data}` and reports failures inside
 * a 200, so `code` is modelled rather than relying on the HTTP status alone.
 */
@Serializable
data class PrayerTimingsResponseDto(
    val code: Int,
    val status: String,
    val data: PrayerTimingsDataDto,
)

@Serializable
data class PrayerTimingsDataDto(
    val timings: PrayerTimingsDto,
    val meta: PrayerMetaDto,
)

/**
 * The prayer times themselves, as local wall-clock strings.
 *
 * Only the five obligatory prayers are modelled. Aladhan also returns Sunrise, Sunset,
 * Imsak, Midnight and the two thirds of the night; `ignoreUnknownKeys` drops them, which
 * keeps this type the same shape as the [com.example.test_ai_project.core.model.Prayer]
 * enum it becomes.
 *
 * The `@SerialName`s are capitalised because Aladhan's keys are, and the field names are
 * not — the JSON spelling is the provider's business and stops at this file.
 *
 * Values are `"HH:mm"`, sometimes suffixed with the zone abbreviation (`"16:55 (BST)"`)
 * depending on the query. Parsing that is the data layer's problem, not this type's:
 * modelling it as a String is what lets the mapper handle both spellings in one place.
 */
@Serializable
data class PrayerTimingsDto(
    @SerialName("Fajr") val fajr: String,
    @SerialName("Dhuhr") val dhuhr: String,
    @SerialName("Asr") val asr: String,
    @SerialName("Maghrib") val maghrib: String,
    @SerialName("Isha") val isha: String,
)

/**
 * [timezone] is the one field that makes the timings above usable.
 *
 * They are wall-clock times for the requested coordinates, so turning them into instants
 * needs the zone *of that place* — which is not necessarily the device's, and is never
 * safe to assume.
 */
@Serializable
data class PrayerMetaDto(
    val timezone: String,
)
