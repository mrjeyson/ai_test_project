package com.example.test_ai_project.home.presentation.movies.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.test_ai_project.home.domain.model.Movie
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.home.presentation.R
import java.util.Locale
import com.example.test_ai_project.home.presentation.movies.contract.MoviesEvent
import com.example.test_ai_project.home.presentation.movies.contract.MoviesState
import com.example.test_ai_project.resource.R as ResR

/**
 * One poster tile: artwork, the offline badge, the title, and the rating and year that
 * make up the card's "basic details".
 */
@Composable
internal fun MovieCard(
    movie: Movie,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // The standard poster ratio. Fixing it here means the grid's rows line up
                // before any image has loaded, so nothing reflows as they arrive.
                .aspectRatio(POSTER_ASPECT_RATIO)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Poster(movie = movie)

            CachedBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }

        Text(
            text = movie.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(
            text = movie.metaLine(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun Poster(movie: Movie, modifier: Modifier = Modifier) {
    if (movie.posterUrl == null) {
        // TMDB genuinely has films with no artwork. A placeholder keeps the grid regular
        // instead of leaving a hole where a tile should be.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_movies),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        return
    }

    AsyncImage(
        model = movie.posterUrl,
        // The title is rendered directly below as text, so describing the poster too would
        // make a screen reader announce the same film twice.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

/**
 * Honest by construction: the grid is rendered from Room, so anything on screen is by
 * definition already on disk and will still be there with the radio off.
 */
@Composable
private fun CachedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_small),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(9.dp),
        )
        Text(
            text = stringResource(id = ResR.string.movies_cached_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * "★ 7.8 · 2024", dropping whichever half the API did not supply.
 *
 * A zero rating means "nobody has voted", not "rated zero", so it is omitted rather than
 * shown — a brand-new film displaying ★ 0.0 reads as a damning review.
 */
@Composable
private fun Movie.metaLine(): String {
    val parts = buildList {
        if (voteCount > 0) {
            add(stringResource(id = ResR.string.movies_rating, formatRating(voteAverage)))
        }
        releaseYear?.let { add(it.toString()) }
    }
    return parts.joinToString(separator = "  ·  ")
        .ifEmpty { stringResource(id = ResR.string.movies_no_details) }
}

/** Locale.US so the decimal separator matches the ★ glyph's fixed one-decimal format. */
private fun formatRating(voteAverage: Double): String =
    String.format(Locale.US, "%.1f", voteAverage)

private const val POSTER_ASPECT_RATIO = 2f / 3f

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun MovieCardPreview() {
    AppTheme {
        Box(modifier = Modifier.background(Color.Transparent).padding(12.dp)) {
            MovieCard(
                movie = Movie(
                    id = 1,
                    title = "Interstellar Horizon",
                    overview = "",
                    posterUrl = null,
                    backdropUrl = null,
                    voteAverage = 7.84,
                    voteCount = 1240,
                    releaseYear = 2024,
                ),
            )
        }
    }
}
