package com.example.test_ai_project.resource.component

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.test_ai_project.resource.theme.AppTextStyle

/**
 * The only text primitive screens use.
 *
 * Wrapping Material's `Text` is what makes [AppTextStyle] enforceable: a screen names a
 * role and cannot reach past it into `MaterialTheme.typography`, so type stays a closed
 * set instead of drifting one `titleLarge` at a time.
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: AppTextStyle = AppTextStyle.Body,
    color: Color = LocalContentColor.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.textStyle,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}
