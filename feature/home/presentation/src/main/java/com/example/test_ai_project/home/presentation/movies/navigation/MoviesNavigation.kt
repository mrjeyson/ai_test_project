package com.example.test_ai_project.home.presentation.movies.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import com.example.test_ai_project.home.presentation.movies.contract.MoviesEvent
import com.example.test_ai_project.home.presentation.movies.contract.MoviesState
import com.example.test_ai_project.home.presentation.movies.screen.MoviesScreen

/**
 * The Movies tab owns its route and its registration function, mirroring how a feature
 * module registers itself in the app graph. Keeping that shape here means promoting the
 * tab to `:feature:movies` later is a move, not a rewrite.
 */
@Serializable
internal data object MoviesRouteKey

internal fun NavGraphBuilder.moviesScreen() {
    composable<MoviesRouteKey> {
        MoviesScreen()
    }
}
