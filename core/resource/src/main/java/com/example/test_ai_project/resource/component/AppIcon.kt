package com.example.test_ai_project.resource.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.test_ai_project.resource.theme.sizes

/**
 * A drawable at one of the design system's icon sizes.
 *
 * [contentDescription] is required rather than defaulted to null: an icon that carries
 * meaning and has no description is invisible to TalkBack, and making the parameter
 * mandatory forces that decision at every call site. Pass `null` explicitly for icons that
 * are purely decorative.
 */
@Composable
fun AppIcon(
    @DrawableRes id: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = modifier.size(size ?: sizes.icon),
        tint = tint,
    )
}
