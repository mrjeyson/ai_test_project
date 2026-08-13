package com.example.test_ai_project.home.presentation.movies.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.home.domain.model.Movie
import com.example.test_ai_project.resource.component.AppLoadingState
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.home.presentation.R
import com.example.test_ai_project.home.presentation.movies.contract.MoviesEvent
import com.example.test_ai_project.home.presentation.movies.contract.MoviesState
import com.example.test_ai_project.home.presentation.movies.components.MovieCard
import com.example.test_ai_project.home.presentation.movies.components.PageControl
import com.example.test_ai_project.home.presentation.movies.components.pageSlots
import com.example.test_ai_project.home.presentation.movies.viewmodel.MoviesViewModel
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppLoadingState

/**
 * Stateful entry point: the only place in this file that touches Hilt or the ViewModel.
 */
@Composable
internal fun MoviesScreen(
    modifier: Modifier = Modifier,
    viewModel: MoviesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MoviesScreen(
        uiState = uiState,
        onPageSelected = { page -> viewModel.onEvent(MoviesEvent.PageSelected(page)) },
        onRetry = { viewModel.onEvent(MoviesEvent.RetryRequested) },
        onDismissMessage = { viewModel.onEvent(MoviesEvent.MessageDismissed) },
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
internal fun MoviesScreen(
    uiState: MoviesState,
    onPageSelected: (Int) -> Unit,
    onRetry: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    // Changing page replaces the contents of a grid that is scrolled somewhere down its
    // length. Without this the user lands mid-way through page 4 having never seen its
    // first row.
    LaunchedEffect(uiState.page) {
        gridState.scrollToItem(0)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.messageRes != null) {
            MessageBanner(
                messageRes = uiState.messageRes,
                onRetry = onRetry,
                onDismiss = onDismissMessage,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isInitialLoad -> AppLoadingState()

                uiState.isEmpty -> EmptyState(onRetry = onRetry)

                else -> LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items = uiState.movies, key = Movie::id) { movie ->
                        MovieCard(movie = movie)
                    }
                }
            }

            // A strip, not a spinner over the content: there are already cached movies on
            // screen and they stay readable and scrollable while the refetch runs.
            if (uiState.isLoading && uiState.movies.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (uiState.isPagingVisible) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PageControl(
                currentPage = uiState.page,
                totalPages = uiState.totalPages,
                onPageSelected = onPageSelected,
            )
        }
    }
}

/**
 * Advisory, not blocking. The fetch failed but the cache did not, so this sits above the
 * content rather than replacing it.
 */
@Composable
private fun MessageBanner(
    messageRes: Int,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = messageRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(id = ResR.string.movies_retry))
        }
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(id = ResR.string.movies_dismiss))
        }
    }
}

@Composable
private fun EmptyState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tab_movies),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = stringResource(id = ResR.string.movies_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(id = ResR.string.movies_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(id = ResR.string.movies_retry))
        }
    }
}

private const val GRID_COLUMNS = 2

private fun previewMovie(id: Long, title: String, year: Int, rating: Double) = Movie(
    id = id,
    title = title,
    overview = "",
    posterUrl = null,
    backdropUrl = null,
    voteAverage = rating,
    voteCount = 900,
    releaseYear = year,
)

@Preview(showBackground = true)
@Composable
private fun MoviesScreenPreview() {
    AppTheme {
        MoviesScreen(
            uiState = MoviesState(
                page = 3,
                totalPages = 500,
                movies = listOf(
                    previewMovie(1, "Interstellar Horizon", 2024, 7.8),
                    previewMovie(2, "The Silent Protocol", 2024, 6.9),
                    previewMovie(3, "Cipher Origins", 2024, 8.1),
                    previewMovie(4, "The Vault Keeper", 2022, 7.2),
                ),
            ),
            onPageSelected = {},
            onRetry = {},
            onDismissMessage = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoviesScreenOfflinePreview() {
    AppTheme {
        MoviesScreen(
            uiState = MoviesState(
                page = 1,
                totalPages = 500,
                movies = listOf(previewMovie(1, "Interstellar Horizon", 2024, 7.8)),
                messageRes = ResR.string.movies_error_unreachable,
            ),
            onPageSelected = {},
            onRetry = {},
            onDismissMessage = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MoviesScreenEmptyPreview() {
    AppTheme {
        MoviesScreen(
            uiState = MoviesState(),
            onPageSelected = {},
            onRetry = {},
            onDismissMessage = {},
        )
    }
}
