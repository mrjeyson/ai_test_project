package com.example.test_ai_project.auth.presentation.faceverification.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.resource.theme.VaultTealLight

/**
 * The scrim with an oval hole, plus the corner brackets.
 *
 * One even-odd [Path] rather than a scrim plus a blend-mode cut-out: even-odd needs no
 * offscreen compositing layer, which matters on a surface that redraws with the preview.
 */
@Composable
internal fun FaceFrameOverlay(
    isAligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val frameColor by animateColorAsState(
        targetValue = if (isAligned) VaultTealLight else Color.White.copy(alpha = 0.32f),
        label = "faceFrameColor",
    )
    val bracketColor = if (isAligned) VaultTealLight else VaultTealLight.copy(alpha = 0.55f)

    Canvas(modifier = modifier) {
        val ovalWidth = size.width * OVAL_WIDTH_FRACTION
        val ovalHeight = size.height * OVAL_HEIGHT_FRACTION
        val oval = Rect(
            offset = Offset(
                x = (size.width - ovalWidth) / 2f,
                y = size.height * OVAL_TOP_FRACTION,
            ),
            size = Size(ovalWidth, ovalHeight),
        )

        val scrim = Path().apply {
            addRect(Rect(Offset.Zero, size))
            addOval(oval)
            fillType = PathFillType.EvenOdd
        }
        drawPath(path = scrim, color = Color.Black.copy(alpha = SCRIM_ALPHA))

        drawOval(
            color = frameColor,
            topLeft = oval.topLeft,
            size = oval.size,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Four L-shaped brackets on the oval's bounding box, as in the design.
        val armLength = ovalWidth * BRACKET_ARM_FRACTION
        val strokeWidth = 3.dp.toPx()
        listOf(
            Triple(oval.left, oval.top, 1f to 1f),
            Triple(oval.right, oval.top, -1f to 1f),
            Triple(oval.left, oval.bottom, 1f to -1f),
            Triple(oval.right, oval.bottom, -1f to -1f),
        ).forEach { (x, y, direction) ->
            val (horizontal, vertical) = direction
            drawLine(
                color = bracketColor,
                start = Offset(x, y),
                end = Offset(x + armLength * horizontal, y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = bracketColor,
                start = Offset(x, y),
                end = Offset(x, y + armLength * vertical),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val OVAL_WIDTH_FRACTION = 0.72f
private const val OVAL_HEIGHT_FRACTION = 0.42f
private const val OVAL_TOP_FRACTION = 0.24f
private const val BRACKET_ARM_FRACTION = 0.12f
private const val SCRIM_ALPHA = 0.74f
