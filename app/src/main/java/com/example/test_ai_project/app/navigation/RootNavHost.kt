package com.example.test_ai_project.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.test_ai_project.auth.presentation.faceverification.navigation.FaceVerificationRoutes
import com.example.test_ai_project.auth.presentation.faceverification.navigation.faceVerificationScreen
import com.example.test_ai_project.auth.presentation.faceverification.navigation.navigateToFaceVerification
import com.example.test_ai_project.home.presentation.home.navigation.HomeRouteKey
import com.example.test_ai_project.home.presentation.home.navigation.homeScreen
import com.example.test_ai_project.home.presentation.home.navigation.navigateToHome
import com.example.test_ai_project.auth.presentation.login.navigation.LoginRoutes
import com.example.test_ai_project.auth.presentation.login.navigation.loginScreen
import com.example.test_ai_project.auth.presentation.login.navigation.navigateToLogin
import com.example.test_ai_project.resource.component.AppToastHost
import com.example.test_ai_project.resource.component.LocalAppToast
import com.example.test_ai_project.resource.component.rememberAppToastState

/**
 * The app's only job in navigation: pick the start destination and list the screen graphs.
 * Adding a feature means one `include` in settings.gradle.kts, one dependency, and one
 * line here.
 *
 * Launch order — login, face verification, home — lives here and nowhere else: each screen
 * reports that it is finished and this graph decides what follows.
 *
 * It also hosts the app's single [AppToastHost]. One host, provided through
 * [LocalAppToast], is what stops two screens stacking two error toasts on top of each
 * other — a screen raises a message and never draws one itself.
 */
@Composable
fun RootNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val toastState = rememberAppToastState()

    CompositionLocalProvider(LocalAppToast provides toastState) {
        Box(modifier = modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = LoginRoutes.Login,
                modifier = Modifier.fillMaxSize(),
            ) {
                loginScreen(
                    onAuthenticated = {
                        navController.navigateToHome {
                            popUpTo<LoginRoutes.Login> { inclusive = true }
                        }
                    },
                )

                faceVerificationScreen(
                    onVerified = {
                        navController.navigateToHome {
                            // `back` from home must never land on a passed verification step.
                            popUpTo<FaceVerificationRoutes.FaceVerification> { inclusive = true }
                        }
                    },
                    // Cancelling is a step back in the flow, not an exit: the credentials
                    // form is where the user can start again.
                    onCancel = {
                        navController.navigateToLogin {
                            popUpTo<FaceVerificationRoutes.FaceVerification> { inclusive = true }
                        }
                    },
                )

                homeScreen(
                    // The mirror image of `onAuthenticated`: home leaves the stack the way
                    // login found it, so `back` from the credentials form exits the app
                    // rather than walking back into a signed-out session.
                    onSignedOut = {
                        navController.navigateToLogin {
                            popUpTo<HomeRouteKey> { inclusive = true }
                        }
                    },
                )
            }

            AppToastHost(
                state = toastState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
