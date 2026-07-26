package com.example.test_ai_project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.test_ai_project.feature.faceverification.FaceVerificationRouteKey
import com.example.test_ai_project.feature.faceverification.faceVerificationScreen
import com.example.test_ai_project.feature.faceverification.navigateToFaceVerification
import com.example.test_ai_project.feature.home.homeScreen
import com.example.test_ai_project.feature.home.navigateToHome
import com.example.test_ai_project.feature.login.LoginRouteKey
import com.example.test_ai_project.feature.login.loginScreen
import com.example.test_ai_project.feature.login.navigateToLogin
import com.example.test_ai_project.feature.splash.SplashRouteKey
import com.example.test_ai_project.feature.splash.splashScreen

/**
 * The app's only job in navigation: pick the start destination and list the
 * feature graphs. Adding a feature means one `include` in settings.gradle.kts,
 * one dependency, and one line here.
 *
 * Launch order — splash, login, face verification, home — lives here and nowhere else:
 * each feature reports that it is finished and this graph decides what follows.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = SplashRouteKey,
        modifier = modifier,
    ) {
        splashScreen(
            onFinished = {
                navController.navigateToLogin {
                    // Bootstrap must not be replayable: `back` from login exits the app.
                    popUpTo<SplashRouteKey> { inclusive = true }
                }
            },
        )

        loginScreen(
            onAuthenticated = {
                navController.navigateToFaceVerification {
                    popUpTo<LoginRouteKey> { inclusive = true }
                }
            },
        )

        faceVerificationScreen(
            onVerified = {
                navController.navigateToHome {
                    // `back` from home must never land on a passed verification step.
                    popUpTo<FaceVerificationRouteKey> { inclusive = true }
                }
            },
            // Cancelling is a step back in the flow, not an exit: the credentials form is
            // where the user can start again.
            onCancel = {
                navController.navigateToLogin {
                    popUpTo<FaceVerificationRouteKey> { inclusive = true }
                }
            },
        )

        homeScreen()
    }
}
