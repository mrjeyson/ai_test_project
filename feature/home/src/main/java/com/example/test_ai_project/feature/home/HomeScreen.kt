package com.example.test_ai_project.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.test_ai_project.core.model.Item
import com.example.test_ai_project.core.ui.component.ErrorState
import com.example.test_ai_project.core.ui.component.LoadingState
import com.example.test_ai_project.core.ui.theme.AppTheme

/**
 * Stateful entry point: the only place in this file that touches Hilt or the
 * ViewModel. Keeping it separate from [HomeScreen] is what makes the screen
 * previewable and testable without a DI graph.
 */
@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    HomeScreen(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

/** Stateless and side-effect free — driven entirely by its parameters. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Items",
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
            ) {
                Text(text = if (isRefreshing) "Refreshing…" else "Refresh")
            }
        }

        when (uiState) {
            HomeUiState.Loading -> LoadingState()

            is HomeUiState.Error -> ErrorState(
                message = uiState.message,
                onRetry = onRefresh,
            )

            is HomeUiState.Success -> if (uiState.items.isEmpty()) {
                EmptyState(onRefresh = onRefresh)
            } else {
                ItemList(items = uiState.items)
            }
        }
    }
}

@Composable
private fun ItemList(
    items: List<Item>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = items, key = { it.id }) { item ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Nothing cached yet.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(text = "Load items")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenSuccessPreview() {
    AppTheme {
        HomeScreen(
            uiState = HomeUiState.Success(
                items = listOf(
                    Item(id = 1, name = "First item", description = "A cached description."),
                    Item(id = 2, name = "Second item", description = ""),
                ),
            ),
            isRefreshing = false,
            onRefresh = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    AppTheme {
        HomeScreen(
            uiState = HomeUiState.Success(items = emptyList()),
            isRefreshing = false,
            onRefresh = {},
        )
    }
}
