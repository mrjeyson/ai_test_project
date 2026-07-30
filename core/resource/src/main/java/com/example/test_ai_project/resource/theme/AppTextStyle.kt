package com.example.test_ai_project.resource.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle

/**
 * The closed set of type roles a screen may ask for.
 *
 * Screens name a role — [Wordmark], [FieldLabel] — rather than reaching into
 * `MaterialTheme.typography`, so the mapping from role to Material slot lives in exactly
 * one place and a re-skin does not require finding every `titleMedium` in the app.
 */
enum class AppTextStyle {
    /** The product wordmark. */
    Wordmark,

    /** Section headings. */
    Title,

    /** Default body copy. */
    Body,

    /** Body copy in a denser context — cards, list rows. */
    BodySmall,

    /** Helper text, captions and field errors. */
    Caption,

    /** Button and link labels. */
    Label,

    /** Uppercase field labels and chips. */
    LabelSmall,

    /** Status captions over dark surfaces. */
    Status,
    ;

    val textStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = when (this) {
            Wordmark -> MaterialTheme.typography.headlineSmall
            Title -> MaterialTheme.typography.titleMedium
            Body -> MaterialTheme.typography.bodyLarge
            BodySmall -> MaterialTheme.typography.bodyMedium
            Caption -> MaterialTheme.typography.bodySmall
            Label -> MaterialTheme.typography.labelLarge
            LabelSmall -> MaterialTheme.typography.labelMedium
            Status -> MaterialTheme.typography.labelSmall
        }
}
