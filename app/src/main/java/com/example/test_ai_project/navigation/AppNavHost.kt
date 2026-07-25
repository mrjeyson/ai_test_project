package com.example.test_ai_project.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.test_ai_project.feature.home.HomeRouteKey
import com.example.test_ai_project.feature.home.homeScreen

/**
 * The app's only job in navigation: pick the start destination and list the
 * feature graphs. Adding a feature means one `include` in settings.gradle.kts,
 * one dependency, and one line here.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = HomeRouteKey,
        modifier = modifier,
    ) {
        homeScreen()
    }
}
