package com.example.test_ai_project.auth.presentation.faceverification.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.example.test_ai_project.auth.presentation.faceverification.screen.FaceVerificationScreen
import kotlinx.serialization.Serializable

/**
 * The screen owns its route and its registration function; the root graph decides what a
 * verified face — or a cancellation — leads to.
 */
object FaceVerificationRoutes {

    @Serializable
    data object FaceVerification
}

fun NavController.navigateToFaceVerification(
    navOptions: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route = FaceVerificationRoutes.FaceVerification, builder = navOptions)
}

fun NavGraphBuilder.faceVerificationScreen(
    onVerified: () -> Unit,
    onCancel: () -> Unit,
) {
    composable<FaceVerificationRoutes.FaceVerification> {
        FaceVerificationScreen(onVerified = onVerified, onCancel = onCancel)
    }
}
