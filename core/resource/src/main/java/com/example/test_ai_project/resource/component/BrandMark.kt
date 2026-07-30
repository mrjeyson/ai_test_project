package com.example.test_ai_project.resource.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.resource.R
import com.example.test_ai_project.resource.preview.DevicePreview
import com.example.test_ai_project.resource.theme.AppTheme
import com.example.test_ai_project.resource.theme.VaultInkDeep

/**
 * The shield in its rounded tile. Every surface that shows the brand shows the same mark,
 * so it lives here rather than being redrawn per screen.
 *
 * [tileColor] is a parameter because the tile is not one colour: on dark surfaces it
 * defaults to near-black so the mark reads as a cut-out, while on light surfaces callers
 * pass the brand accent.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tileColor: Color = VaultInkDeep,
) {
    Box(
        modifier = modifier
            .size(size)
            // Radius tracks the tile size so the mark scales without redesigning.
            .background(color = tileColor, shape = RoundedCornerShape(percent = 28)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_shield),
            contentDescription = stringResource(id = R.string.brand_logo_content_description),
            tint = Color.White,
            modifier = Modifier.size(size * SHIELD_RATIO),
        )
    }
}

/** The "SecureVault" wordmark. Owns the product string so callers cannot drift from it. */
@Composable
fun BrandWordmark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Text(
        text = stringResource(id = R.string.brand_name),
        style = MaterialTheme.typography.headlineSmall,
        color = color,
        modifier = modifier,
    )
}

/** Proportion of the tile the glyph occupies, taken from the design. */
private const val SHIELD_RATIO = 0.47f

@DevicePreview
@Composable
private fun BrandMarkOnLightPreview() {
    AppTheme {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            BrandLogo(tileColor = MaterialTheme.colorScheme.primary)
        }
    }
}
