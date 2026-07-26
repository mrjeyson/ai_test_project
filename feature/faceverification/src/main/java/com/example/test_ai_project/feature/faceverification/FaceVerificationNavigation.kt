package com.example.test_ai_project.feature.faceverification

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The feature owns its route and its registration function; `:app` decides what a
 * verified face — or a cancellation — leads to.
 */
@Serializable
data object FaceVerificationRouteKey

fun NavController.navigateToFaceVerification(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route = FaceVerificationRouteKey, builder = navOptions)
}

fun NavGraphBuilder.faceVerificationScreen(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
) {
    composable<FaceVerificationRouteKey> {
        FaceVerificationRoute(onVerified = onVerified, onCancel = onCancel)
    }
}
