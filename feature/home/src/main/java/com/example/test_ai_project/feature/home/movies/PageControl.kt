package com.example.test_ai_project.feature.home.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.core.ui.theme.AppTheme
import com.example.test_ai_project.feature.home.R

/**
 * Numbered page navigation: ‹ 1 … 4 5 6 … 500 ›.
 *
 * Rendered entirely from cached state, so it is fully usable offline — every page the user
 * has already visited is one tap away with the radio off.
 */
@Composable
internal fun PageControl(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(
            iconRes = R.drawable.ic_chevron_left,
            descriptionRes = R.string.movies_previous_page,
            enabled = currentPage > 1,
            onClick = { onPageSelected(currentPage - 1) },
        )

        pageSlots(current = currentPage, total = totalPages).forEach { slot ->
            when (slot) {
                is PageSlot.Gap -> Gap()
                is PageSlot.Number -> PageNumber(
                    page = slot.page,
                    isSelected = slot.page == currentPage,
                    onClick = { onPageSelected(slot.page) },
                )
            }
        }

        StepButton(
            iconRes = R.drawable.ic_chevron_right,
            descriptionRes = R.string.movies_next_page,
            enabled = currentPage < totalPages,
            onClick = { onPageSelected(currentPage + 1) },
        )
    }
}

@Composable
private fun StepButton(
    iconRes: Int,
    descriptionRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.size(36.dp)) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(id = descriptionRes),
            tint = if (enabled) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PageNumber(
    page: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(id = R.string.movies_go_to_page, page)

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .defaultMinSize(minWidth = 32.dp, minHeight = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .clickable(enabled = !isSelected, onClick = onClick)
            // Without this a screen reader reads the bare digit. `clearAndSet` replaces
            // the child Text's own semantics rather than appending to them, so the number
            // is announced once, as an action.
            .clearAndSetSemantics {
                contentDescription = description
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = page.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun Gap(modifier: Modifier = Modifier) {
    Text(
        text = "…",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PageControlMiddlePreview() {
    AppTheme {
        PageControl(currentPage = 42, totalPages = 500, onPageSelected = {})
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun PageControlFirstPreview() {
    AppTheme {
        PageControl(currentPage = 1, totalPages = 500, onPageSelected = {})
    }
}
