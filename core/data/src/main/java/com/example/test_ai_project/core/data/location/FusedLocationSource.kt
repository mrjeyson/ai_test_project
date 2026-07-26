package com.example.test_ai_project.core.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The platform's fused location provider, wrapped in suspending functions.
 *
 * The wrapper earns its place twice over: it is the only class in the app that knows Play
 * services exist, and it converts Play's `Task` callbacks into coroutines that actually
 * cancel — a fix request left running after the map is closed keeps the GPS warm and the
 * battery draining.
 *
 * Permission is *not* checked here. The caller is the repository, which turns the resulting
 * [SecurityException] into a domain exception; checking here as well would mean two places
 * that can disagree about what "granted" means, and the platform's own answer is the one
 * that counts.
 */
@Singleton
class FusedLocationSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    /**
     * Asks for a fix now, waiting for one if the device has none cached.
     *
     * [Priority.PRIORITY_BALANCED_POWER_ACCURACY] rather than high accuracy: the map centres
     * on a marker at street zoom, where the difference between 20 m and 5 m is invisible and
     * the difference in power draw is not.
     *
     * Returns null when the platform declines to produce one — location services off, or
     * airplane mode with no cached fix.
     *
     * @throws SecurityException if the location permission is not granted.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        // Propagates coroutine cancellation into Play services, so leaving the screen
        // actually stops the request rather than orphaning it.
        continuation.invokeOnCancellation { cancellation.cancel() }

        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
            .addOnSuccessListener { location -> continuation.resume(location) }
            .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }

    /**
     * The fix Play services already had, without waiting for a new one.
     *
     * The fallback for [currentLocation] returning null: a stale system fix is still a
     * better answer than none, and it costs nothing to ask for.
     *
     * @throws SecurityException if the location permission is not granted.
     */
    @SuppressLint("MissingPermission")
    suspend fun lastLocation(): Location? = suspendCancellableCoroutine { continuation ->
        client.lastLocation
            .addOnSuccessListener { location -> continuation.resume(location) }
            .addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
}
