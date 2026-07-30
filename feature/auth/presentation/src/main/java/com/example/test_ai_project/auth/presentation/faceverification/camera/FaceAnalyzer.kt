package com.example.test_ai_project.auth.presentation.faceverification.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.test_ai_project.auth.domain.model.FaceObservation
import com.example.test_ai_project.auth.domain.model.NormalizedRect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * The edge of the app: turns camera frames into [FaceObservation]s and nothing more.
 *
 * ML Kit's types stop here. Everything above this class reasons about normalised
 * rectangles and angles, which is why the alignment rules can be unit-tested.
 */
internal class FaceAnalyzer(
    private val onFaces: (List<FaceObservation>) -> Unit,
    private val onError: () -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            // FAST over ACCURATE: this runs on every preview frame, and the alignment
            // gate only needs a bounding box and head angles.
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(MIN_FACE_SIZE)
            .build(),
    )

    @OptIn(markerClass = [ExperimentalGetImage::class])
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        // ML Kit reports bounding boxes in the *rotated* image's coordinates, so a quarter
        // turn swaps which sensor dimension normalisation has to divide by. Getting this
        // wrong makes a portrait face look permanently off-centre.
        val isQuarterTurned = rotationDegrees == 90 || rotationDegrees == 270
        val frameWidth = if (isQuarterTurned) mediaImage.height else mediaImage.width
        val frameHeight = if (isQuarterTurned) mediaImage.width else mediaImage.height

        detector.process(InputImage.fromMediaImage(mediaImage, rotationDegrees))
            .addOnSuccessListener { faces ->
                onFaces(faces.map { it.toObservation(frameWidth, frameHeight) })
            }
            .addOnFailureListener { onError() }
            // Always, on both paths: an unclosed ImageProxy stalls the whole pipeline.
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        detector.close()
    }

    private companion object {
        /** Ignore faces smaller than this fraction of the frame — bystanders, mostly. */
        const val MIN_FACE_SIZE = 0.2f
    }
}

/**
 * The front camera preview is mirrored for display but the analyser sees the unmirrored
 * frame. Only symmetric thresholds are applied to x, so the mirroring cancels out and no
 * correction is needed here.
 */
private fun Face.toObservation(frameWidth: Int, frameHeight: Int): FaceObservation {
    val width = frameWidth.toFloat()
    val height = frameHeight.toFloat()

    return FaceObservation(
        bounds = NormalizedRect(
            left = boundingBox.left / width,
            top = boundingBox.top / height,
            right = boundingBox.right / width,
            bottom = boundingBox.bottom / height,
        ),
        yawDegrees = headEulerAngleY,
        rollDegrees = headEulerAngleZ,
    )
}
