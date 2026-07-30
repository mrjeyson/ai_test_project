package com.example.test_ai_project.home.domain.model

/**
 * A single position fix for the device.
 *
 * [capturedAtEpochMillis] is part of the model rather than an implementation detail of the
 * cache: a fix that is displayed offline is displayed *stale*, and the screen can only say
 * so honestly if it knows when the fix was taken.
 *
 * [accuracyMeters] is nullable because a provider genuinely may not report one — an absent
 * radius and a radius of zero mean opposite things, and collapsing them would draw a
 * pin-sharp circle around a coarse network fix.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    /** Radius of 68% confidence, in metres. */
    val accuracyMeters: Float?,
    val capturedAtEpochMillis: Long,
)
