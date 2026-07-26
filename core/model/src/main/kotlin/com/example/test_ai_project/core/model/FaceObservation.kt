package com.example.test_ai_project.core.model

/**
 * A rectangle in `0f..1f` coordinates, relative to the frame it was found in.
 *
 * Normalised on purpose: alignment rules must not depend on the camera's resolution, and
 * a domain layer that spoke in pixels would change meaning on every device.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * One face as seen in one frame — the only thing the domain learns about a camera image.
 *
 * Deliberately not ML Kit's `Face`: keeping the detector's type at the edge is what lets
 * the alignment rules be unit-tested with no camera, no emulator, and no model.
 */
data class FaceObservation(
    val bounds: NormalizedRect,
    /** Left/right head turn, in degrees; 0 is facing the camera. */
    val yawDegrees: Float,
    /** Head tilt in the image plane, in degrees; 0 is upright. */
    val rollDegrees: Float,
)

/** Whether the frame is good enough to verify from. */
sealed interface FaceAlignment {

    data object Aligned : FaceAlignment

    data class Misaligned(val reason: MisalignmentReason) : FaceAlignment
}

enum class MisalignmentReason {
    NoFace,

    /** More than one face: verifying would not know which person it had matched. */
    MultipleFaces,

    TooFar,
    TooClose,
    OffCentre,

    /** Turned or tilted too far for a reliable match. */
    Turned,
}
