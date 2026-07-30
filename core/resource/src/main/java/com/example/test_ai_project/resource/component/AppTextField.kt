package com.example.test_ai_project.resource.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.resource.theme.AppTextStyle
import com.example.test_ai_project.resource.theme.sizes
import com.example.test_ai_project.resource.theme.spacing

/**
 * The single-line input used across SecureVault.
 *
 * Built on [BasicTextField] rather than `OutlinedTextField`: the design calls for a fixed
 * box height with a small radius and no floating label, and Material's outlined field
 * enforces a 56dp minimum plus a label slot that has to be fought on every usage.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(sizes.radius)
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(sizes.buttonHeight)
            .background(color = MaterialTheme.colorScheme.surface, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = AppTextStyle.Body.textStyle.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    // The placeholder sits behind the field rather than replacing it, so
                    // the cursor is visible on an empty field.
                    if (value.isEmpty()) {
                        AppText(
                            text = placeholder,
                            style = AppTextStyle.Body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                },
            )

            trailing?.invoke()
        }
    }
}
