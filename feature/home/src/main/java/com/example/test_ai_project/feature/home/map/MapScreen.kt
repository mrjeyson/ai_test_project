package com.example.test_ai_project.feature.home.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.test_ai_project.feature.home.R
import com.example.test_ai_project.feature.home.component.TabPlaceholder

@Composable
internal fun MapScreen(modifier: Modifier = Modifier) {
    TabPlaceholder(
        title = stringResource(id = R.string.home_tab_map),
        description = stringResource(id = R.string.home_placeholder_map),
        modifier = modifier,
    )
}
