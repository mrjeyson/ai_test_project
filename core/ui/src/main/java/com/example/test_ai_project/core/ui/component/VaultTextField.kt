package com.example.test_ai_project.core.ui.component

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.test_ai_project.core.ui.theme.AppTheme

/**
 * The single-line input used across SecureVault.
 *
 * Built on [BasicTextField] rather than `OutlinedTextField`: the design calls for a 52dp
 * box with a 10dp radius and no floating label, and Material's outlined field enforces a
 * 56dp minimum plus label slot that has to be fought on every usage.
 */
@Composable
fun VaultTextField(
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
    val shape = RoundedCornerShape(FieldCornerRadius)
    val borderColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .background(color = MaterialTheme.colorScheme.surface, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
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
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
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

private val FieldHeight = 52.dp
private val FieldCornerRadius = 10.dp

@Preview(showBackground = true)
@Composable
private fun VaultTextFieldPreview() {
    AppTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            VaultTextField(
                value = "",
                onValueChange = {},
                placeholder = "dd/mm/yyyy",
            )
        }
    }
}
