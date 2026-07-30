package com.example.test_ai_project.auth.presentation.login.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.example.test_ai_project.auth.presentation.login.screen.LoginScreen
import kotlinx.serialization.Serializable

/**
 * The screen owns its route and its registration function; the root graph decides *where*
 * a successful authentication leads by supplying [onAuthenticated], so login never names
 * another feature.
 */
object LoginRoutes {

    @Serializable
    data object Login
}

fun NavController.navigateToLogin(navOptions: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route = LoginRoutes.Login, builder = navOptions)
}

fun NavGraphBuilder.loginScreen(onAuthenticated: () -> Unit) {
    composable<LoginRoutes.Login> {
        LoginScreen(onAuthenticated = onAuthenticated)
    }
}
