package com.example.test_ai_project.core.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Turns coordinates into something a person recognises — "London, United Kingdom".
 *
 * Best-effort by design, and every path returns null rather than throwing. The name is
 * decoration on a screen whose actual content is the prayer times: failing the whole
 * refresh because a reverse lookup timed out would trade the feature for the label.
 *
 * The platform [Geocoder] rather than a network call of our own: it is free, needs no key,
 * and on many devices answers from a local dataset when there is no connection at all.
 */
@Singleton
class GeocoderPlaceNameSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Null when the device has no geocoder, the lookup fails, or nothing matches. */
    suspend fun placeName(latitude: Double, longitude: Double): String? {
        // Not every device ships one — Geocoder is backed by an optional platform service,
        // and calling it when absent throws rather than returning empty.
        if (!Geocoder.isPresent()) return null

        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.awaitAddresses(latitude, longitude)
        } else {
            geocoder.blockingAddresses(latitude, longitude)
        }
        return addresses.firstOrNull()?.toPlaceName()
    }

    /**
     * API 33+ has a callback-based lookup, and it is the only one that is not a blocking
     * network call on whatever thread it was invoked from.
     *
     * The annotation is not decoration: the version check lives in the caller, one frame
     * up, which is far enough that lint cannot see it and would otherwise have to be
     * silenced. Declaring the requirement instead keeps the check honest — moving this call
     * somewhere unguarded would still fail the build.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun Geocoder.awaitAddresses(
        latitude: Double,
        longitude: Double,
    ): List<Address> = suspendCancellableCoroutine { continuation ->
        getFromLocation(latitude, longitude, MAX_RESULTS) { addresses ->
            // `resume` once: the platform contract is a single callback, but a
            // double-delivered result would crash the coroutine machinery rather than the
            // lookup, and this is not worth a crash.
            if (continuation.isActive) continuation.resume(addresses)
        }
    }

    /**
     * The pre-33 path. Deprecated upstream precisely because it blocks, which is why the
     * caller runs it on the IO dispatcher.
     */
    @Suppress("DEPRECATION", "SwallowedException")
    private fun Geocoder.blockingAddresses(latitude: Double, longitude: Double): List<Address> =
        try {
            getFromLocation(latitude, longitude, MAX_RESULTS).orEmpty()
        } catch (failure: Exception) {
            // IOException for a failed lookup, IllegalArgumentException for coordinates the
            // service rejects. Both mean the same thing here: no name.
            emptyList()
        }

    /**
     * "City, Country", falling back through progressively coarser fields.
     *
     * [Address.getLocality] is empty for a fix out at sea, in open country, or in a
     * territory the dataset does not divide into towns — common enough that a null-safe
     * chain is cheaper than an occasional bare country name.
     */
    private fun Address.toPlaceName(): String? {
        val place = locality ?: subAdminArea ?: adminArea
        val country = countryName
        return when {
            place != null && country != null -> "$place, $country"
            else -> place ?: country
        }
    }

    private companion object {
        // One is all that is read. Asking for more is a larger response for results that
        // are, by construction, worse matches for the same point.
        const val MAX_RESULTS = 1
    }
}
